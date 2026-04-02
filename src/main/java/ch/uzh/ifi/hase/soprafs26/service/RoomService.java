package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.entity.Judge;
import ch.uzh.ifi.hase.soprafs26.entity.Room;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.entity.Writer;
import ch.uzh.ifi.hase.soprafs26.repository.RoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional
public class RoomService {

    private final RoomRepository roomRepository;
    private final UserService userService;

    @Autowired
    public RoomService(@Qualifier("roomRepository") RoomRepository roomRepository, UserService userService) {
        this.roomRepository = roomRepository;
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
}