package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.entity.Story;
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
import java.util.List;
import java.util.Random;

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
        game.setTimer(90L);

        List<String> genrePool = new ArrayList<>(List.of("Horror", "Comedy", "Sci-Fi", "Fantasy"));
        Collections.shuffle(genrePool);
        game.getWriters().get(0).setGenre(genrePool.get(0));
        game.getWriters().get(1).setGenre(genrePool.get(1));

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