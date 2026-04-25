package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.entity.*;
import ch.uzh.ifi.hase.soprafs26.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs26.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

public class RoomServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private GameRepository gameRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private RoomService roomService;

    private User testUser;
    private Room testRoom;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testUser");
        testUser.setToken("valid-token");

        testRoom = new Room();
        testRoom.setId(1L);
        testRoom.setName("TestRoom");
        testRoom.setPlayerCount(0);
        testRoom.setUsers(new ArrayList<>());
        testRoom.setWriters(new ArrayList<>());
        testRoom.setJudges(new ArrayList<>());

        given(userService.extractToken(anyString())).willReturn("valid-token");
        given(userService.findUserFromToken("valid-token")).willReturn(testUser);
        given(roomRepository.save(any())).willReturn(testRoom);
    }

    // --- AUTHENTICATION ---

    @Test
    public void anyMethod_invalidToken_401Unauthorized() {
        given(userService.extractToken(anyString())).willReturn("invalid-token");
        given(userService.findUserFromToken("invalid-token"))
                .willThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        assertThrows(ResponseStatusException.class, () -> roomService.getRooms("Bearer invalid-token"));
        assertThrows(ResponseStatusException.class, () -> roomService.createRoom(testRoom, "Bearer invalid-token"));
        assertThrows(ResponseStatusException.class, () -> roomService.joinRoom(1L, "Bearer invalid-token"));
    }

    // --- ROOM CREATION ---

    @Test
    public void createRoom_validInput_success() {
        given(roomRepository.findByName(anyString())).willReturn(null);

        Room createdRoom = roomService.createRoom(testRoom, "Bearer valid-token");

        assertNotNull(createdRoom);
        assertEquals(testUser, createdRoom.getLobbyLeader());
        assertEquals(1, createdRoom.getPlayerCount());
        verify(roomRepository, times(1)).save(any());
    }

    @Test
    public void createRoom_duplicateName_409Conflict() {
        given(roomRepository.findByName(anyString())).willReturn(testRoom);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> roomService.createRoom(testRoom, "Bearer valid-token"));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    // --- ROOM RETRIEVAL ---

    @Test
    public void getRooms_validRequest_200Ok() {
        given(roomRepository.findAll()).willReturn(List.of(testRoom));

        List<Room> result = roomService.getRooms("Bearer valid-token");

        assertEquals(1, result.size());
        assertEquals(testRoom.getName(), result.get(0).getName());
    }

    @Test
    public void getRoomById_validRequest_200Ok() {
        given(roomRepository.findById(anyLong())).willReturn(Optional.of(testRoom));

        Room result = roomService.getRoomById(1L, "Bearer valid-token");

        assertEquals(testRoom.getName(), result.getName());
    }

    @Test
    public void getRoomById_notFound_404NotFound() {
        given(roomRepository.findById(anyLong())).willReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> roomService.getRoomById(1L, "Bearer valid-token"));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    // --- ROOM ENTRY (JOINING) ---

    @Test
    public void joinRoom_validInput_success() {
        given(roomRepository.findById(anyLong())).willReturn(Optional.of(testRoom));

        Room joinedRoom = roomService.joinRoom(1L, "Bearer valid-token");

        assertEquals(1, joinedRoom.getPlayerCount());
        assertTrue(joinedRoom.getUsers().contains(testUser));
    }

    @Test
    public void joinRoom_roomFull_400BadRequest() {
        testRoom.setPlayerCount(3);
        given(roomRepository.findById(anyLong())).willReturn(Optional.of(testRoom));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> roomService.joinRoom(1L, "Bearer valid-token"));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    public void joinRoom_alreadyIn_400BadRequest() {
        testRoom.getUsers().add(testUser);
        given(roomRepository.findById(anyLong())).willReturn(Optional.of(testRoom));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> roomService.joinRoom(1L, "Bearer valid-token"));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    public void joinRoom_notFound_404NotFound() {
        given(roomRepository.findById(anyLong())).willReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> roomService.joinRoom(1L, "Bearer valid-token"));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    // --- ROLE MANAGEMENT (SWAPPING) ---

    @Test
    public void swapRole_toWriter_success() {
        testRoom.getUsers().add(testUser);
        given(roomRepository.findById(anyLong())).willReturn(Optional.of(testRoom));

        Room updatedRoom = roomService.swapRole(1L, "WRITER", "Bearer valid-token");

        assertEquals(1, updatedRoom.getWriters().size());
        assertEquals(0, updatedRoom.getUsers().size());
        assertEquals("testUser", updatedRoom.getWriters().get(0).getUser().getUsername());
    }

    @Test
    public void swapRole_toJudge_success() {
        testRoom.getUsers().add(testUser);
        given(roomRepository.findById(anyLong())).willReturn(Optional.of(testRoom));

        Room updatedRoom = roomService.swapRole(1L, "JUDGE", "Bearer valid-token");

        assertEquals(1, updatedRoom.getJudges().size());
        assertEquals("testUser", updatedRoom.getJudges().get(0).getUser().getUsername());
    }

    @Test
    public void swapRole_currentlyWriter_swapToUser_success() {
        testRoom.getWriters().add(new Writer(testUser));
        given(roomRepository.findById(anyLong())).willReturn(Optional.of(testRoom));

        Room updatedRoom = roomService.swapRole(1L, "USER", "Bearer valid-token");

        assertEquals(1, updatedRoom.getUsers().size());
        assertEquals(0, updatedRoom.getWriters().size());
    }

    @Test
    public void swapRole_notInRoom_400BadRequest() {
        given(roomRepository.findById(anyLong())).willReturn(Optional.of(testRoom));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> roomService.swapRole(1L, "WRITER", "Bearer valid-token"));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    public void swapRole_writerFull_400BadRequest() {
        testRoom.getUsers().add(testUser);

        User writerUser1 = new User();
        writerUser1.setId(10L);
        testRoom.getWriters().add(new Writer(writerUser1));

        User writerUser2 = new User();
        writerUser2.setId(11L);
        testRoom.getWriters().add(new Writer(writerUser2));

        given(roomRepository.findById(anyLong())).willReturn(Optional.of(testRoom));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> roomService.swapRole(1L, "WRITER", "Bearer valid-token"));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3})
    public void startGame_judgeCountValidation(int judgeCount) {
        testRoom.setLobbyLeader(testUser);
        testRoom.getWriters().add(new Writer(new User()));
        testRoom.getWriters().add(new Writer(new User()));

        for (int i = 0; i < judgeCount; i++) {
            User judgeUser = new User();
            judgeUser.setId(100L + i);
            testRoom.getJudges().add(new Judge(judgeUser));
        }

        given(roomRepository.findById(anyLong())).willReturn(Optional.of(testRoom));
        given(gameRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        if (judgeCount == 0) {
            ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                    () -> roomService.startGame(1L, "Bearer valid-token"));
            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
            verify(gameRepository, times(0)).save(any());
        }
        else {
            Game game = roomService.startGame(1L, "Bearer valid-token");

            assertNotNull(game);
            assertEquals(judgeCount, game.getJudges().size());
            verify(gameRepository, times(1)).save(any());
            verify(roomRepository, times(1)).delete(testRoom);
        }
    }

    @Test
    public void swapRole_roomNotFound_404NotFound() {
        given(roomRepository.findById(anyLong())).willReturn(Optional.empty());

        assertThrows(ResponseStatusException.class,
                () -> roomService.swapRole(99L, "WRITER", "Bearer valid-token"));
    }

    // --- ROOM EXIT & LEADERSHIP ---

    @Test
    public void leaveRoom_writerLeaves_success() {
        User leader = new User();
        leader.setId(99L);
        testRoom.setLobbyLeader(leader);
        testRoom.getUsers().add(leader);
        testRoom.getWriters().add(new Writer(testUser));
        testRoom.setPlayerCount(2);

        given(roomRepository.findById(1L)).willReturn(Optional.of(testRoom));

        roomService.leaveRoom(1L, "Bearer valid-token");

        assertEquals(0, testRoom.getWriters().size());
        assertEquals(1, testRoom.getPlayerCount());
        verify(roomRepository, times(1)).save(testRoom);
    }

    @Test
    public void leaveRoom_lastPlayer_deletesRoom() {
        testRoom.getUsers().add(testUser);
        given(roomRepository.findById(anyLong())).willReturn(Optional.of(testRoom));

        roomService.leaveRoom(1L, "Bearer valid-token");

        verify(roomRepository, times(1)).delete(testRoom);
    }

    @Test
    public void leaveRoom_leaderLeaves_promotesNewLeader() {
        User runnerUp = new User();
        runnerUp.setId(2L);
        testRoom.setLobbyLeader(testUser);
        testRoom.getUsers().add(testUser);
        testRoom.getUsers().add(runnerUp);

        given(roomRepository.findById(anyLong())).willReturn(Optional.of(testRoom));

        roomService.leaveRoom(1L, "Bearer valid-token");

        assertEquals(runnerUp, testRoom.getLobbyLeader());
    }

    @Test
    public void leaveRoom_promoteWriter_success() {
        User writerUser = new User();
        writerUser.setId(2L);
        testRoom.getWriters().add(new Writer(writerUser));
        testRoom.setLobbyLeader(testUser);
        testRoom.getUsers().add(testUser);

        given(roomRepository.findById(anyLong())).willReturn(Optional.of(testRoom));

        roomService.leaveRoom(1L, "Bearer valid-token");

        assertEquals(writerUser.getId(), testRoom.getLobbyLeader().getId());
    }

    @Test
    public void leaveRoom_promoteJudge_success() {
        User judgeUser = new User();
        judgeUser.setId(3L);
        testRoom.getJudges().add(new Judge(judgeUser));
        testRoom.setLobbyLeader(testUser);
        testRoom.getUsers().add(testUser);

        given(roomRepository.findById(anyLong())).willReturn(Optional.of(testRoom));

        roomService.leaveRoom(1L, "Bearer valid-token");

        assertEquals(judgeUser.getId(), testRoom.getLobbyLeader().getId());
    }

    @Test
    public void leaveRoom_userNotInRoom_400BadRequest() {
        given(roomRepository.findById(anyLong())).willReturn(Optional.of(testRoom));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> roomService.leaveRoom(1L, "Bearer valid-token"));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    public void leaveRoom_notFound_404NotFound() {
        given(roomRepository.findById(anyLong())).willReturn(Optional.empty());
        assertThrows(ResponseStatusException.class, () -> roomService.leaveRoom(99L, "Bearer valid-token"));
    }

    // --- GAME INITIALIZATION ---

    @Test
    public void startGame_validState_success() {
        testRoom.setLobbyLeader(testUser);
        testRoom.getWriters().add(new Writer(new User()));
        testRoom.getWriters().add(new Writer(new User()));
        testRoom.getJudges().add(new Judge(new User()));

        given(roomRepository.findById(anyLong())).willReturn(Optional.of(testRoom));
        given(gameRepository.save(any())).willReturn(new Game());

        Game game = roomService.startGame(1L, "Bearer valid-token");

        assertNotNull(game);
        verify(gameRepository, times(1)).save(any());
        verify(roomRepository, times(1)).delete(testRoom);
    }

    @Test
    public void startGame_genresAssignedDistinctly_success() {
        testRoom.setLobbyLeader(testUser);
        testRoom.getWriters().add(new Writer(new User()));
        testRoom.getWriters().add(new Writer(new User()));
        testRoom.getJudges().add(new Judge(new User()));

        given(roomRepository.findById(anyLong())).willReturn(Optional.of(testRoom));
        given(gameRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        Game game = roomService.startGame(1L, "Bearer valid-token");

        assertNotEquals(game.getWriters().get(0).getGenre(), game.getWriters().get(1).getGenre());
    }

    @Test
    public void startGame_notLeader_403Forbidden() {
        User differentUser = new User();
        differentUser.setId(99L);
        testRoom.setLobbyLeader(differentUser);

        given(roomRepository.findById(anyLong())).willReturn(Optional.of(testRoom));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> roomService.startGame(1L, "Bearer valid-token"));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    @Test
    public void startGame_wrongPlayerCount_400BadRequest() {
        testRoom.setLobbyLeader(testUser);
        testRoom.getWriters().add(new Writer(new User()));

        given(roomRepository.findById(anyLong())).willReturn(Optional.of(testRoom));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> roomService.startGame(1L, "Bearer valid-token"));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    public void startGame_missingJudge_400BadRequest() {
        testRoom.setLobbyLeader(testUser);
        testRoom.getWriters().add(new Writer(new User()));
        testRoom.getWriters().add(new Writer(new User()));

        given(roomRepository.findById(anyLong())).willReturn(Optional.of(testRoom));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> roomService.startGame(1L, "Bearer valid-token"));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }
}