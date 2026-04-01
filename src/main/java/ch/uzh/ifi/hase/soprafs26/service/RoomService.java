package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.entity.Room;
import ch.uzh.ifi.hase.soprafs26.entity.User;
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

    public List<Room> getRooms() {
        return this.roomRepository.findAll();
    }

    public Room createRoom(Room newRoom, String bearerToken) {
        // 1. Identify Creator & Handle 401 (Spec: "Error: reason... Go to login...")
        // This will use the extractToken/findUserFromToken logic we built earlier
        // Make sure those methods throw the EXACT string from your image.
        String token = userService.extractToken(bearerToken);
        User creator = userService.findUserFromToken(token);

        // 2. Validate Room Name Uniqueness & Handle 409
        Room existingRoom = roomRepository.findByName(newRoom.getName());
        if (existingRoom != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Error: A room with that name already exists"); // Matches 409 Spec
        }

        // 3. Setup and Persist (Spec: Room created)
        newRoom.setLobbyLeader(creator);
        newRoom.getUsers().add(creator);

        return roomRepository.save(newRoom);
    }
}