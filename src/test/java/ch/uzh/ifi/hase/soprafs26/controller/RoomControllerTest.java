package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.entity.*;
import ch.uzh.ifi.hase.soprafs26.rest.dto.room.RoomPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.room.RoomRoleDTO;
import ch.uzh.ifi.hase.soprafs26.service.RoomService;
import ch.uzh.ifi.hase.soprafs26.service.UserService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RoomController.class)
public class RoomControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RoomService roomService;

    @MockitoBean
    private UserService userService;

    // --- POST /rooms (Create Room) ---

    @Test
    public void createRoom_validInput_201Created() throws Exception {
        Room room = new Room();
        room.setId(1L);
        room.setName("TestRoom");

        RoomPostDTO roomPostDTO = new RoomPostDTO();
        roomPostDTO.setName("TestRoom");

        given(roomService.createRoom(any(), anyString())).willReturn(room);

        MockHttpServletRequestBuilder postRequest = post("/rooms")
                .header("Authorization", "Bearer token123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(roomPostDTO));

        mockMvc.perform(postRequest)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(room.getId().intValue())))
                .andExpect(jsonPath("$.name", is(room.getName())));
    }

    @Test
    public void createRoom_invalidToken_401Unauthorized() throws Exception {
        RoomPostDTO roomPostDTO = new RoomPostDTO();
        roomPostDTO.setName("TestRoom");

        given(roomService.createRoom(any(), any()))
                .willThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Error: reason<string> Go to login and clear local Storage"));

        MockHttpServletRequestBuilder postRequest = post("/rooms")
                .header("Authorization", "InvalidToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(roomPostDTO));

        mockMvc.perform(postRequest)
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void createRoom_duplicateName_409Conflict() throws Exception {
        RoomPostDTO roomPostDTO = new RoomPostDTO();
        roomPostDTO.setName("ExistingRoom");

        given(roomService.createRoom(any(), anyString()))
                .willThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Error: A room with that name already exists"));

        MockHttpServletRequestBuilder postRequest = post("/rooms")
                .header("Authorization", "Bearer token123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(roomPostDTO));

        mockMvc.perform(postRequest)
                .andExpect(status().isConflict());
    }

    // --- PUT /rooms/{id}/join (Join Room) ---

    @Test
    public void joinRoom_validInput_200Ok() throws Exception {
        Room room = new Room();
        room.setId(1L);
        room.setName("TestRoom");

        given(roomService.joinRoom(anyLong(), anyString())).willReturn(room);

        MockHttpServletRequestBuilder putRequest = put("/rooms/1/join")
                .header("Authorization", "Bearer token123");

        mockMvc.perform(putRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(room.getId().intValue())))
                .andExpect(jsonPath("$.name", is(room.getName())));
    }

    @Test
    public void joinRoom_roomFullOrAlreadyIn_400BadRequest() throws Exception {
        given(roomService.joinRoom(anyLong(), anyString()))
                .willThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Error: Wrong state and/or credential exchange"));

        MockHttpServletRequestBuilder putRequest = put("/rooms/1/join")
                .header("Authorization", "Bearer token123");

        mockMvc.perform(putRequest)
                .andExpect(status().isBadRequest());
    }

    @Test
    public void joinRoom_invalidToken_401Unauthorized() throws Exception {
        given(roomService.joinRoom(anyLong(), any()))
                .willThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Error: reason<string> Go to login and clear local Storage"));

        MockHttpServletRequestBuilder putRequest = put("/rooms/1/join")
                .header("Authorization", "WrongToken");

        mockMvc.perform(putRequest)
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void joinRoom_roomNotFound_404NotFound() throws Exception {
        given(roomService.joinRoom(anyLong(), anyString()))
                .willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Error: Room with roomId was not found"));

        MockHttpServletRequestBuilder putRequest = put("/rooms/99/join")
                .header("Authorization", "Bearer token123");

        mockMvc.perform(putRequest)
                .andExpect(status().isNotFound());
    }

    // --- PUT /rooms/{id}/roles (Swap Role) ---

    @Test
    public void swapRole_validInput_200Ok() throws Exception {
        Room room = new Room();
        room.setId(1L);
        RoomRoleDTO roleDTO = new RoomRoleDTO();
        roleDTO.setRole("WRITER");

        given(roomService.swapRole(anyLong(), anyString(), anyString())).willReturn(room);

        MockHttpServletRequestBuilder putRequest = put("/rooms/1/roles")
                .header("Authorization", "Bearer token123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(roleDTO));

        mockMvc.perform(putRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(room.getId().intValue())));
    }

    @Test
    public void swapRole_notInRoom_400BadRequest() throws Exception {
        RoomRoleDTO roleDTO = new RoomRoleDTO();
        roleDTO.setRole("WRITER");

        given(roomService.swapRole(anyLong(), anyString(), anyString()))
                .willThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Error: You must join the room before selecting a role."));

        MockHttpServletRequestBuilder putRequest = put("/rooms/1/roles")
                .header("Authorization", "Bearer token123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(roleDTO));

        mockMvc.perform(putRequest)
                .andExpect(status().isBadRequest());
    }

    @Test
    public void swapRole_invalidToken_401Unauthorized() throws Exception {
        RoomRoleDTO roleDTO = new RoomRoleDTO();
        roleDTO.setRole("WRITER");

        given(roomService.swapRole(anyLong(), anyString(), any()))
                .willThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Error: reason<string> Go to login and clear local Storage"));

        MockHttpServletRequestBuilder putRequest = put("/rooms/1/roles")
                .header("Authorization", "InvalidToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(roleDTO));

        mockMvc.perform(putRequest)
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void swapRole_roomNotFound_404NotFound() throws Exception {
        RoomRoleDTO roleDTO = new RoomRoleDTO();
        roleDTO.setRole("WRITER");

        given(roomService.swapRole(anyLong(), anyString(), anyString()))
                .willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Error: Room with roomId was not found"));

        MockHttpServletRequestBuilder putRequest = put("/rooms/99/roles")
                .header("Authorization", "Bearer token123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(roleDTO));

        mockMvc.perform(putRequest)
                .andExpect(status().isNotFound());
    }

    // --- POST /rooms/{id} (Start Game) ---

    @Test
    public void startGame_validRequest_201Created() throws Exception {
        Game game = new Game();
        game.setId(1L);

        given(roomService.startGame(anyLong(), anyString())).willReturn(game);

        MockHttpServletRequestBuilder postRequest = post("/rooms/1")
                .header("Authorization", "Bearer token123");

        mockMvc.perform(postRequest)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.gameId", is(game.getId().intValue())));
    }

    @Test
    public void startGame_wrongState_400BadRequest() throws Exception {
        given(roomService.startGame(anyLong(), anyString()))
                .willThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Error: Wrong state and/or credential exchange"));

        MockHttpServletRequestBuilder postRequest = post("/rooms/1")
                .header("Authorization", "Bearer token123");

        mockMvc.perform(postRequest)
                .andExpect(status().isBadRequest());
    }

    @Test
    public void startGame_invalidToken_401Unauthorized() throws Exception {
        given(roomService.startGame(anyLong(), any()))
                .willThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Error: reason<string> Go to login and clear local Storage"));

        MockHttpServletRequestBuilder postRequest = post("/rooms/1")
                .header("Authorization", "InvalidToken");

        mockMvc.perform(postRequest)
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void startGame_notLeader_403Forbidden() throws Exception {
        given(roomService.startGame(anyLong(), anyString()))
                .willThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Error: The Lobby leader has to start the game"));

        MockHttpServletRequestBuilder postRequest = post("/rooms/1")
                .header("Authorization", "Bearer token123");

        mockMvc.perform(postRequest)
                .andExpect(status().isForbidden());
    }

    // --- PUT /rooms/{id}/leave (Leave Room) ---

    @Test
    public void leaveRoom_validRequest_204NoContent() throws Exception {
        MockHttpServletRequestBuilder putRequest = put("/rooms/1/leave")
                .header("Authorization", "Bearer token123");

        mockMvc.perform(putRequest)
                .andExpect(status().isNoContent());
    }

    @Test
    public void leaveRoom_wrongState_400BadRequest() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Error: Wrong state and/or credential exchange"))
                .when(roomService).leaveRoom(anyLong(), anyString());

        MockHttpServletRequestBuilder putRequest = put("/rooms/1/leave")
                .header("Authorization", "Bearer token123");

        mockMvc.perform(putRequest)
                .andExpect(status().isBadRequest());
    }

    @Test
    public void leaveRoom_invalidToken_401Unauthorized() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Error: reason<string> Go to login and clear local Storage"))
                .when(roomService).leaveRoom(anyLong(), any());

        MockHttpServletRequestBuilder putRequest = put("/rooms/1/leave")
                .header("Authorization", "InvalidToken");

        mockMvc.perform(putRequest)
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void leaveRoom_roomNotFound_404NotFound() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Error: Room with roomId was not found"))
                .when(roomService).leaveRoom(anyLong(), anyString());

        MockHttpServletRequestBuilder putRequest = put("/rooms/99/leave")
                .header("Authorization", "Bearer token123");

        mockMvc.perform(putRequest)
                .andExpect(status().isNotFound());
    }

    // --- GET /rooms (getAllRooms) ---

    @Test
    public void getAllRooms_validRequest_200Ok() throws Exception {
        Room room = new Room();
        room.setId(1L);
        room.setName("TestRoom");
        List<Room> allRooms = Collections.singletonList(room);

        given(roomService.getRooms(anyString())).willReturn(allRooms);

        MockHttpServletRequestBuilder getRequest = get("/rooms")
                .header("Authorization", "Bearer token123");

        mockMvc.perform(getRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is(room.getName())));
    }

    @Test
    public void getAllRooms_invalidToken_401Unauthorized() throws Exception {
        given(roomService.getRooms(any()))
                .willThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Error: reason<string> Go to login and clear local Storage"));

        MockHttpServletRequestBuilder getRequest = get("/rooms")
                .header("Authorization", "InvalidToken");

        mockMvc.perform(getRequest)
                .andExpect(status().isUnauthorized());
    }

    // --- GET /rooms/{id} (getRoomById) ---

    @Test
    public void getRoomById_validRequest_200Ok() throws Exception {
        Room room = new Room();
        room.setId(1L);
        room.setName("TestRoom");

        given(roomService.getRoomById(anyLong(), anyString())).willReturn(room);

        MockHttpServletRequestBuilder getRequest = get("/rooms/1")
                .header("Authorization", "Bearer token123");

        mockMvc.perform(getRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(room.getId().intValue())))
                .andExpect(jsonPath("$.name", is(room.getName())));
    }

    @Test
    public void getRoomById_invalidToken_401Unauthorized() throws Exception {
        given(roomService.getRoomById(anyLong(), any()))
                .willThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Error: reason<string> Go to login and clear local Storage"));

        MockHttpServletRequestBuilder getRequest = get("/rooms/1")
                .header("Authorization", "InvalidToken");

        mockMvc.perform(getRequest)
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void getRoomById_roomNotFound_404NotFound() throws Exception {
        given(roomService.getRoomById(anyLong(), anyString()))
                .willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Error: Room with roomId was not found"));

        MockHttpServletRequestBuilder getRequest = get("/rooms/99")
                .header("Authorization", "Bearer token123");

        mockMvc.perform(getRequest)
                .andExpect(status().isNotFound());
    }

    /**
     * Helper Method to convert object into a JSON string
     */
    private String asJsonString(final Object object) {
        try {
            return new ObjectMapper().writeValueAsString(object);
        } catch (JacksonException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format("The request body could not be created.%s", e.toString()));
        }
    }


    @Test
    public void updateTimer_validRequest_200Ok() throws Exception {
        Room room = new Room();
        room.setId(1L);
        room.setName("TestRoom");
        room.setTimer(60L);

        given(roomService.setTimer(anyLong(), any(), anyString())).willReturn(room);

        MockHttpServletRequestBuilder putRequest = put("/rooms/1/timer")
                .header("Authorization", "Bearer token123")
                .contentType(MediaType.APPLICATION_JSON)
                .content("60");

        mockMvc.perform(putRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(room.getId().intValue())));
    }

    @Test
    public void updateTimer_notLeader_401Unauthorized() throws Exception {
        given(roomService.setTimer(anyLong(), any(), anyString()))
                .willThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Error: You are not the lobby leader"));

        MockHttpServletRequestBuilder putRequest = put("/rooms/1/timer")
                .header("Authorization", "Bearer token123")
                .contentType(MediaType.APPLICATION_JSON)
                .content("60");

        mockMvc.perform(putRequest)
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void updateTimer_roomNotFound_404NotFound() throws Exception {
        given(roomService.setTimer(anyLong(), any(), anyString()))
                .willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Error: Room with roomId was not found"));

        MockHttpServletRequestBuilder putRequest = put("/rooms/99/timer")
                .header("Authorization", "Bearer token123")
                .contentType(MediaType.APPLICATION_JSON)
                .content("60");

        mockMvc.perform(putRequest)
                .andExpect(status().isNotFound());
    }


    @Test
    public void updateMaxRounds_validRequest_200Ok() throws Exception {
        Room room = new Room();
        room.setId(1L);
        room.setName("TestRoom");
        room.setMaxRounds(6);

        given(roomService.setMaxRounds(anyLong(), any(int.class), anyString())).willReturn(room);

        MockHttpServletRequestBuilder putRequest = put("/rooms/1/rounds")
                .header("Authorization", "Bearer token123")
                .contentType(MediaType.APPLICATION_JSON)
                .content("6");

        mockMvc.perform(putRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(room.getId().intValue())));
    }

    @Test
    public void updateMaxRounds_notLeader_401Unauthorized() throws Exception {
        given(roomService.setMaxRounds(anyLong(), any(int.class), anyString()))
                .willThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Error: You are not the lobby leader"));

        MockHttpServletRequestBuilder putRequest = put("/rooms/1/rounds")
                .header("Authorization", "Bearer token123")
                .contentType(MediaType.APPLICATION_JSON)
                .content("6");

        mockMvc.perform(putRequest)
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void updateMaxRounds_roomNotFound_404NotFound() throws Exception {
        given(roomService.setMaxRounds(anyLong(), any(int.class), anyString()))
                .willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Error: Room with roomId was not found"));

        MockHttpServletRequestBuilder putRequest = put("/rooms/99/rounds")
                .header("Authorization", "Bearer token123")
                .contentType(MediaType.APPLICATION_JSON)
                .content("6");

        mockMvc.perform(putRequest)
                .andExpect(status().isNotFound());
    }
}