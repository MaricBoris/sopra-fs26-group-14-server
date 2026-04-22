package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.constant.GamePhase;
import ch.uzh.ifi.hase.soprafs26.constant.ObjectivePool;
import ch.uzh.ifi.hase.soprafs26.entity.*;
import ch.uzh.ifi.hase.soprafs26.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs26.repository.RoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.security.SecureRandom;


import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

@Service
@Transactional
public class RoomService {

    private final RoomRepository roomRepository;
    private final GameRepository gameRepository;
    private final UserService userService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Autowired
    public RoomService(@Qualifier("roomRepository") RoomRepository roomRepository, @Qualifier("gameRepository") GameRepository gameRepository, UserService userService) {
        this.roomRepository = roomRepository;
        this.gameRepository = gameRepository;
        this.userService = userService;
    }

    public Room createRoom(Room newRoom, String bearerToken) {
        String token = userService.extractToken(bearerToken);
        User creator = userService.findUserFromToken(token);

        Room existingRoom = roomRepository.findByName(newRoom.getName());
        if (existingRoom != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Error: A room with that name already exists");
        }

        newRoom.setLobbyLeader(creator);
        newRoom.getUsers().add(creator);
        newRoom.setPlayerCount(1);

        return roomRepository.save(newRoom);
    }

    public List<Room> getRooms(String bearerToken) {
        userService.findUserFromToken(userService.extractToken(bearerToken));
        return this.roomRepository.findAll();
    }

    public Room joinRoom(Long roomId, String bearerToken) {
        String token = userService.extractToken(bearerToken);
        User user = userService.findUserFromToken(token);

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Error: Room with roomId was not found"));

        boolean isFull = room.getPlayerCount() >= 3;

        boolean alreadyIn = room.getUsers().contains(user);

        if (isFull || alreadyIn) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Error: Wrong state and/or credential exchange");
        }

        room.getUsers().add(user);
        room.setPlayerCount(room.getUsers().size());

        return roomRepository.save(room);
    }

    public Room swapRole(Long roomId, String targetRole, String bearerToken) {
        String token = userService.extractToken(bearerToken);
        User user = userService.findUserFromToken(token);
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));

        boolean isAlreadyInRoom = room.getUsers().stream().anyMatch(u -> u.getId().equals(user.getId())) ||
                room.getWriters().stream().anyMatch(w -> w.getUser().getId().equals(user.getId())) ||
                room.getJudges().stream().anyMatch(j -> j.getUser().getId().equals(user.getId()));

        if (!isAlreadyInRoom) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Error: You must join the room before selecting a role.");
        }

        room.getUsers().removeIf(u -> u.getId().equals(user.getId()));
        room.getWriters().removeIf(w -> w.getUser().getId().equals(user.getId()));
        room.getJudges().removeIf(j -> j.getUser().getId().equals(user.getId()));

        // 📝 flush deletes before insert to avoid unique constraint violation on user_id
        roomRepository.saveAndFlush(room);

        if ("WRITER".equalsIgnoreCase(targetRole)) {
            if (room.getWriters().size() >= 2) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Error: Wrong state and/or credential exchange");
            }
            room.getWriters().add(new Writer(user));
        }
        else if ("JUDGE".equalsIgnoreCase(targetRole)) {
            if (!room.getJudges().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Error: Wrong state and/or credential exchange");
            }
            room.getJudges().add(new Judge(user));
        }
        else {
            room.getUsers().add(user);
        }

        return roomRepository.save(room);
    }

    public Room getRoomById(Long roomId, String bearerToken) {
        userService.findUserFromToken(userService.extractToken(bearerToken));

        return roomRepository.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Error: Room with roomId was not found"));
    }

    public void leaveRoom(Long roomId, String bearerToken) {
        String token = userService.extractToken(bearerToken);
        User user = userService.findUserFromToken(token);

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Error: Room with roomId was not found"));

        boolean inUsers = room.getUsers().stream().anyMatch(u -> u.getId().equals(user.getId()));
        boolean inWriters = room.getWriters().stream().anyMatch(w -> w.getUser().getId().equals(user.getId()));
        boolean inJudges = room.getJudges().stream().anyMatch(j -> j.getUser().getId().equals(user.getId()));

        if (!inUsers && !inWriters && !inJudges) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Error: Wrong state and/or credential exchange");
        }

        room.getUsers().removeIf(u -> u.getId().equals(user.getId()));
        room.getWriters().removeIf(w -> w.getUser().getId().equals(user.getId()));
        room.getJudges().removeIf(j -> j.getUser().getId().equals(user.getId()));

        room.setPlayerCount(room.getUsers().size() + room.getWriters().size() + room.getJudges().size());

        if (room.getPlayerCount() == 0) {
            roomRepository.delete(room);
            roomRepository.flush();
            return;
        }

        if (room.getLobbyLeader().getId().equals(user.getId())) {
            promoteToLeader(room);
        }

        roomRepository.save(room);
        roomRepository.flush();
    }

    private void promoteToLeader(Room room) {
        if (!room.getUsers().isEmpty()) {
            room.setLobbyLeader(room.getUsers().get(0));
        } else if (!room.getWriters().isEmpty()) {
            room.setLobbyLeader(room.getWriters().get(0).getUser());
        } else if (!room.getJudges().isEmpty()) {
            room.setLobbyLeader(room.getJudges().get(0).getUser());
        }
    }

    public Game startGame(Long roomId, String bearerToken) {
        String token = userService.extractToken(bearerToken);
        User requester = userService.findUserFromToken(token);

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Error: Room with roomId was not found"));

        if (!room.getLobbyLeader().getId().equals(requester.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Error: The Lobby leader has to start the game");
        }
        if (room.getWriters().size() != 2 || room.getJudges().size() != 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Error: Wrong state and/or credential exchange");
        }

        Game game = new Game();
        game.setWriters(new ArrayList<>(room.getWriters()));
        game.setJudges(new ArrayList<>(room.getJudges()));

        game.setPhase(GamePhase.WRITING);
        game.setCurrentRound(1);
        game.setTimer(90L);
        game.setTurnStartedAt(System.currentTimeMillis());
        game.setRoundResolved(false);

        LinkedHashMap<String, String> darkGenres = new LinkedHashMap<>();
        darkGenres.put("Horror", "Scary and threatening.");
        darkGenres.put("Drama", "Serious emotions and conflict.");
        darkGenres.put("Thriller", "Tension, danger, urgency.");
        darkGenres.put("Tragedy", "Things go wrong and don't end well.");
        darkGenres.put("Dystopian Sci-Fi", "A broken, oppressive future.");
        darkGenres.put("Survival", "Characters struggle to stay alive.");
        darkGenres.put("Crime", "Something illegal or morally wrong happens.");
        darkGenres.put("Psychological", "Fear, paranoia or unstable reality.");
        darkGenres.put("Dark Fantasy", "Magic world, but dangerous, cruel or corrupted.");

        LinkedHashMap<String, String> lightGenres = new LinkedHashMap<>();
        lightGenres.put("Comedy", "Funny, happy situations.");
        lightGenres.put("Love Story", "Romance");
        lightGenres.put("Utopian Sci-Fi", "A perfect or ideal future.");
        lightGenres.put("Fairy Tale", "Magical and hopeful.");
        lightGenres.put("Kids / Disney Fantasy", "Magical world that is colorful, safe and positive.");

        List<String> darkNames = new ArrayList<>(darkGenres.keySet());
        List<String> lightNames = new ArrayList<>(lightGenres.keySet());
        Collections.shuffle(darkNames);
        Collections.shuffle(lightNames);
        boolean firstWriterDark = secureRandom.nextBoolean();
        String genre1 = (firstWriterDark ? darkNames : lightNames).get(0);
        String genre2 = (firstWriterDark ? lightNames : darkNames).get(0);
        game.getWriters().get(0).setGenre(genre1);
        game.getWriters().get(0).setGenreDescription(firstWriterDark ? darkGenres.get(genre1) : lightGenres.get(genre1));
        game.getWriters().get(1).setGenre(genre2);
        game.getWriters().get(1).setGenreDescription(firstWriterDark ? lightGenres.get(genre2) : darkGenres.get(genre2));
        
        boolean firstWriterStarts = secureRandom.nextBoolean();
        game.getWriters().get(0).setTurn(firstWriterStarts);
        game.getWriters().get(1).setTurn(!firstWriterStarts);

        long now = System.currentTimeMillis();
        game.getWriters().get(0).setLastSeenAt(now);
        game.getWriters().get(1).setLastSeenAt(now);

        for(Judge j: game.getJudges()){
            j.setLastSeenAt(now);
        }
        Story story = new Story();
        List<String> objectivePool = new ArrayList<>(ObjectivePool.OBJECTIVES);
        Collections.shuffle(objectivePool, secureRandom);
        story.setObjective(objectivePool.get(0));
        game.setStory(story);

        game = gameRepository.save(game);

        room.getWriters().clear();
        room.getJudges().clear();
        room.getUsers().clear();
        roomRepository.delete(room);
        roomRepository.flush();

        return game;
    }
}