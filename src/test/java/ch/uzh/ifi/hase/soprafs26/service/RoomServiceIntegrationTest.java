package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.entity.*;
import ch.uzh.ifi.hase.soprafs26.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs26.repository.RoomRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@WebAppConfiguration
@SpringBootTest
public class RoomServiceIntegrationTest {

    @Qualifier("roomRepository")
    @Autowired
    private RoomRepository roomRepository;

    @Qualifier("userRepository")
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private RoomService roomService;

    @Autowired
    private UserService userService;

    private User testUser;
    private String token;

    @BeforeEach
    public void setup() {
        gameRepository.deleteAll();
        roomRepository.deleteAll();
        userRepository.deleteAll();

        // Create a base user for testing authentication
        testUser = new User();
        testUser.setUsername("testLeader");
        testUser.setPassword("password");
        testUser = userService.createUser(testUser);
        token = "Bearer " + testUser.getToken();
    }

    // --- Create Room (POST /rooms) ---

    @Test
    public void createRoom_validInputs_success() {
        Room roomInput = new Room();
        roomInput.setName("IntegrationRoom");

        Room createdRoom = roomService.createRoom(roomInput, token);

        assertNotNull(createdRoom.getId());
        assertEquals("IntegrationRoom", createdRoom.getName());
        assertEquals(testUser.getId(), createdRoom.getLobbyLeader().getId());
        assertEquals(1, createdRoom.getPlayerCount());
    }

    @Test
    public void createRoom_duplicateName_409Conflict() {
        Room firstRoom = new Room();
        firstRoom.setName("DuplicateName");
        roomService.createRoom(firstRoom, token);

        Room secondRoom = new Room();
        secondRoom.setName("DuplicateName");

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> roomService.createRoom(secondRoom, token));
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    @Test
    public void createRoom_invalidToken_401Unauthorized() {
        Room room = new Room();
        room.setName("NoAuthRoom");

        assertThrows(ResponseStatusException.class,
                () -> roomService.createRoom(room, "Bearer invalid-token"));
    }

    // --- Join Room (PUT /rooms/{id}/join) ---

    @Test
    public void joinRoom_validInput_success() {
        Room room = new Room();
        room.setName("JoinableRoom");
        room = roomService.createRoom(room, token);

        // Second user joins
        User secondUser = new User();
        secondUser.setUsername("joiner");
        secondUser.setPassword("pass");
        secondUser = userService.createUser(secondUser);

        Room joinedRoom = roomService.joinRoom(room.getId(), "Bearer " + secondUser.getToken());

        assertEquals(2, joinedRoom.getPlayerCount());
        assertTrue(joinedRoom.getUsers().stream().anyMatch(u -> u.getUsername().equals("joiner")));
    }

    @Test
    public void joinRoom_roomFull_400BadRequest() {
        Room room = new Room();
        room.setName("FullRoom");
        room.setPlayerCount(3); // Database needs this initialized
        room = roomRepository.save(room);

        // FIX: Added password
        User user = new User();
        user.setUsername("latecomer");
        user.setPassword("password123");
        user = userService.createUser(user);

        final Long roomId = room.getId();
        final String userToken = "Bearer " + user.getToken();

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> roomService.joinRoom(roomId, userToken));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    // --- Swap Role (PUT /rooms/{id}/roles) ---

    @Test
    public void swapRole_promoteToWriter_success() {
        Room room = new Room();
        room.setName("RoleRoom");
        room = roomService.createRoom(room, token);

        Room updatedRoom = roomService.swapRole(room.getId(), "WRITER", token);

        assertEquals(1, updatedRoom.getWriters().size());
        assertEquals(0, updatedRoom.getUsers().size());
    }

    @Test
    public void swapRole_notInRoom_400BadRequest() {
        Room room = new Room();
        room.setName("EmptyRoom");
        room.setPlayerCount(0); // FIX: Initialize playerCount to satisfy DB constraint
        room = roomRepository.save(room);

        final Long roomId = room.getId();
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> roomService.swapRole(roomId, "WRITER", token));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    // --- Leave Room (PUT /rooms/{id}/leave) ---

    @Test
    public void leaveRoom_lastUserLeaves_roomDeleted() {
        Room room = new Room();
        room.setName("EphemeralRoom");
        room = roomService.createRoom(room, token);

        roomService.leaveRoom(room.getId(), token);

        assertFalse(roomRepository.findById(room.getId()).isPresent());
    }

    // --- Start Game (POST /rooms/{id}) ---

    @Test
    public void startGame_notLeader_403Forbidden() {
        Room room = new Room();
        room.setName("ForbiddenRoom");
        room = roomService.createRoom(room, token);

        // FIX: Added password
        User nonLeader = new User();
        nonLeader.setUsername("notTheBoss");
        nonLeader.setPassword("password123");
        nonLeader = userService.createUser(nonLeader);

        final String nlToken = "Bearer " + nonLeader.getToken();
        final Long roomId = room.getId();

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> roomService.startGame(roomId, nlToken));
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    @Test
    public void startGame_wrongState_400BadRequest() {
        Room room = new Room();
        room.setName("EmptyGameRoom");
        room = roomService.createRoom(room, token); // Only 1 player, no judge/writers

        final Long roomId = room.getId();
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> roomService.startGame(roomId, token));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    // --- Get Room (GET /rooms/{id}) ---

    @Test
    public void getRoomById_notFound_404NotFound() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> roomService.getRoomById(999L, token));
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    public void getRooms_valid_200Ok() {
        Room r1 = new Room(); r1.setName("R1"); roomService.createRoom(r1, token);

        List<Room> rooms = roomService.getRooms(token);
        assertFalse(rooms.isEmpty());
    }

    // --- Set Timer (PUT /rooms/{id}/timer) ---

    @Test
    public void setTimer_validLeader_success() {
        Room room = new Room();
        room.setName("TimerRoom");
        room = roomService.createRoom(room, token);

        Room updatedRoom = roomService.setTimer(room.getId(), 60L, token);

        assertEquals(60L, updatedRoom.getTimer());
    }

    @Test
    public void setTimer_notLeader_401Unauthorized() {
        Room room = new Room();
        room.setName("TimerRoomNotLeader");
        room = roomService.createRoom(room, token);

        User otherUser = new User();
        otherUser.setUsername("notLeader");
        otherUser.setPassword("password123");
        otherUser = userService.createUser(otherUser);

        final Long roomId = room.getId();
        final String otherToken = "Bearer " + otherUser.getToken();

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> roomService.setTimer(roomId, 60L, otherToken));
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    }

    @Test
    public void setTimer_roomNotFound_404NotFound() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> roomService.setTimer(999L, 60L, token));
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    public void setTimer_persistedCorrectly() {
        Room room = new Room();
        room.setName("TimerPersistRoom");
        room = roomService.createRoom(room, token);

        roomService.setTimer(room.getId(), 45L, token);

        Room fromDb = roomRepository.findById(room.getId()).orElseThrow();
        assertEquals(45L, fromDb.getTimer());
    }

    // --- Set Max Rounds (PUT /rooms/{id}/rounds) ---

    @Test
    public void setMaxRounds_validLeader_success() {
        Room room = new Room();
        room.setName("RoundsRoom");
        room = roomService.createRoom(room, token);

        Room updatedRoom = roomService.setMaxRounds(room.getId(), 6, token);

        assertEquals(6, updatedRoom.getMaxRounds());
    }

    @Test
    public void setMaxRounds_notLeader_401Unauthorized() {
        Room room = new Room();
        room.setName("RoundsRoomNotLeader");
        room = roomService.createRoom(room, token);

        User otherUser = new User();
        otherUser.setUsername("notLeader2");
        otherUser.setPassword("password123");
        otherUser = userService.createUser(otherUser);

        final Long roomId = room.getId();
        final String otherToken = "Bearer " + otherUser.getToken();

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> roomService.setMaxRounds(roomId, 6, otherToken));
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    }

    @Test
    public void setMaxRounds_roomNotFound_404NotFound() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> roomService.setMaxRounds(999L, 6, token));
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    public void setMaxRounds_persistedCorrectly() {
        Room room = new Room();
        room.setName("RoundsPersistRoom");
        room = roomService.createRoom(room, token);

        roomService.setMaxRounds(room.getId(), 8, token);

        Room fromDb = roomRepository.findById(room.getId()).orElseThrow();
        assertEquals(8, fromDb.getMaxRounds());
    }

    // --- Add Chat Message (PUT /rooms/{id}/chat) ---

    @Test
    @Transactional
    public void addChatMessage_success_persistedInDb() {
        Room room = new Room();
        room.setName("ChatRoom");
        room = roomService.createRoom(room, token);

        Room updatedRoom = roomService.addChatMessage(room.getId(), "Hello everyone!", token);

        assertFalse(updatedRoom.getChat().isEmpty());
        assertEquals("Hello everyone!", updatedRoom.getChat().get(0).getMessage());
        assertEquals(testUser.getUsername(), updatedRoom.getChat().get(0).getUsername());

        Room fromDb = roomRepository.findById(room.getId()).orElseThrow();
        assertEquals(1, fromDb.getChat().size());
        assertEquals("Hello everyone!", fromDb.getChat().get(0).getMessage());
    }

    @Test
    public void addChatMessage_success_userIsWriter() {
        Room room = new Room();
        room.setName("WriterChatRoom");
        room = roomService.createRoom(room, token);

        room = roomService.swapRole(room.getId(), "WRITER", token);

        Room updatedRoom = roomService.addChatMessage(room.getId(), "Writer message", token);

        assertEquals(1, updatedRoom.getChat().size());
        assertEquals("Writer message", updatedRoom.getChat().get(0).getMessage());
    }

    @Test
    public void addChatMessage_success_userIsJudge() {
        Room room = new Room();
        room.setName("JudgeChatRoom");
        room = roomService.createRoom(room, token);

        room = roomService.swapRole(room.getId(), "JUDGE", token);

        Room updatedRoom = roomService.addChatMessage(room.getId(), "Judge message", token);

        assertEquals(1, updatedRoom.getChat().size());
        assertEquals("Judge message", updatedRoom.getChat().get(0).getMessage());
    }

    @Test
    public void addChatMessage_userNotParticipant_403Forbidden() {
        Room room = new Room();
        room.setName("PrivateRoom");
        room = roomService.createRoom(room, token);

        User intruder = new User();
        intruder.setUsername("intruder");
        intruder.setPassword("pass123");
        intruder = userService.createUser(intruder);
        String intruderToken = "Bearer " + intruder.getToken();

        final Long roomId = room.getId();
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> roomService.addChatMessage(roomId, "Let me in!", intruderToken));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertEquals("You are not a participant in this room", exception.getReason());
    }

    @Test
    public void addChatMessage_roomNotFound_404NotFound() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> roomService.addChatMessage(999L, "Valid message", token));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("Room not found", exception.getReason());
    }

    @Test
    public void addChatMessage_emptyMessage_400BadRequest() {
        Room room = new Room();
        room.setName("EmptyMsgRoom");
        room = roomService.createRoom(room, token);

        final Long roomId = room.getId();
        // Test null message
        assertThrows(ResponseStatusException.class,
                () -> roomService.addChatMessage(roomId, null, token));

        // Test empty/whitespace message
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> roomService.addChatMessage(roomId, "   ", token));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Message cannot be empty", exception.getReason());
    }
}