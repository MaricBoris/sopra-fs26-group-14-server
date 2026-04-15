package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.user.*;
import ch.uzh.ifi.hase.soprafs26.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    // --- REGISTRATION (POST /users) ---

    @Test
    public void createUser_validInput_201Created() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setUsername("testUser");
        user.setToken("token-123");

        UserPostDTO userPostDTO = new UserPostDTO();
        userPostDTO.setUsername("testUser");
        userPostDTO.setPassword("password123");

        given(userService.createUser(any())).willReturn(user);

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(userPostDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(user.getId().intValue())))
                .andExpect(jsonPath("$.username", is(user.getUsername())));
    }

    @Test
    public void createUser_emptyFields_400BadRequest() throws Exception {
        UserPostDTO userPostDTO = new UserPostDTO();
        userPostDTO.setUsername("");

        given(userService.createUser(any()))
                .willThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username and password cannot be empty!"));

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(userPostDTO)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void createUser_duplicateUsername_409Conflict() throws Exception {
        UserPostDTO userPostDTO = new UserPostDTO();
        userPostDTO.setUsername("existingUser");

        given(userService.createUser(any()))
                .willThrow(new ResponseStatusException(HttpStatus.CONFLICT, "The username provided is not unique."));

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(userPostDTO)))
                .andExpect(status().isConflict());
    }

    // --- AUTHENTICATION (POST /users/login & logout) ---

    @Test
    public void loginUser_validCredentials_200Ok() throws Exception {
        User user = new User();
        user.setUsername("testUser");
        user.setToken("login-token");

        UserPostDTO userPostDTO = new UserPostDTO();
        userPostDTO.setUsername("testUser");
        userPostDTO.setPassword("password123");

        given(userService.loginUser(any())).willReturn(user);

        mockMvc.perform(post("/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(userPostDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", is(user.getToken())));
    }

    @Test
    public void loginUser_invalidCredentials_401Unauthorized() throws Exception {
        UserPostDTO userPostDTO = new UserPostDTO();
        given(userService.loginUser(any()))
                .willThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        mockMvc.perform(post("/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(userPostDTO)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void logoutUser_validToken_204NoContent() throws Exception {
        mockMvc.perform(post("/users/logout")
                        .header("Authorization", "Bearer some-token"))
                .andExpect(status().isNoContent());
    }

    @Test
    public void logoutUser_missingToken_204NoContent() throws Exception {
        mockMvc.perform(post("/users/logout"))
                .andExpect(status().isNoContent());
    }

    // --- USER RETRIEVAL (GET /users & /users/{id}) ---

    @Test
    public void getAllUsers_validRequest_200Ok() throws Exception {
        User user = new User();
        user.setUsername("firstname@lastname");
        List<User> allUsers = Collections.singletonList(user);

        given(userService.getUsers(anyString())).willReturn(allUsers);

        mockMvc.perform(get("/users")
                        .header("Authorization", "Bearer some-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].username", is(user.getUsername())));
    }

    @Test
    public void getAllUsers_missingToken_401Unauthorized() throws Exception {
        given(userService.getUsers(null))
                .willThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated"));

        mockMvc.perform(get("/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void getUser_validId_200Ok() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setUsername("testUsername");

        given(userService.findUserFromId(1L)).willReturn(user);
        given(userService.findUserFromToken(anyString())).willReturn(user);

        mockMvc.perform(get("/users/1")
                        .header("Authorization", "Bearer testToken"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.username", is("testUsername")));
    }

    @Test
    public void getUser_nonBearerHeader_200Ok() throws Exception {
        User user = new User();
        user.setId(1L);
        given(userService.findUserFromId(anyLong())).willReturn(user);

        mockMvc.perform(get("/users/1")
                        .header("Authorization", "raw-token-string"))
                .andExpect(status().isOk());
    }

    @Test
    public void getUser_invalidId_404NotFound() throws Exception {
        given(userService.findUserFromId(anyLong()))
                .willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));

        mockMvc.perform(get("/users/99")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isNotFound());
    }

    // --- USER UPDATES (PUT /users/{id}/password & /users/{id}) ---

    @Test
    public void changePassword_validInput_200Ok() throws Exception {
        UserPasswordPutDTO dto = new UserPasswordPutDTO();
        dto.setCurrentPassword("old");
        dto.setNewPassword("new");

        mockMvc.perform(put("/users/1/password")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(dto)))
                .andExpect(status().isOk());
    }

    @Test
    public void updateUserBio_validInput_200Ok() throws Exception {
        User user = new User();
        user.setBio("New Bio");
        UserPutDTO dto = new UserPutDTO();
        dto.setBio("New Bio");

        given(userService.updateUserBio(anyLong(), any(), any())).willReturn(user);

        mockMvc.perform(put("/users/1")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bio", is("New Bio")));
    }

    // --- ACCOUNT DELETION (DELETE /users/{id}) ---

    @Test
    public void deleteUser_withBody_204NoContent() throws Exception {
        UserDeleteDTO dto = new UserDeleteDTO();
        dto.setPassword("pass");

        mockMvc.perform(delete("/users/1")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(dto)))
                .andExpect(status().isNoContent());
    }

    @Test
    public void deleteUser_noBody_204NoContent() throws Exception {
        mockMvc.perform(delete("/users/1")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isNoContent());
    }

    // --- STORY RESULTS (GET /results) ---

    @Test
    public void getResults_validBearerToken_200Ok() throws Exception {
        given(userService.findAllStories()).willReturn(new ArrayList<>());

        mockMvc.perform(get("/results")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk());
    }

    @Test
    public void getResults_nonBearerToken_200Ok() throws Exception {
        given(userService.findAllStories()).willReturn(new ArrayList<>());

        mockMvc.perform(get("/results")
                        .header("Authorization", "raw-token"))
                .andExpect(status().isOk());
    }

    @Test
    public void getResults_nullToken_200Ok() throws Exception {
        given(userService.findAllStories()).willReturn(new ArrayList<>());

        mockMvc.perform(get("/results"))
                .andExpect(status().isOk());
    }

    private String asJsonString(final Object object) {
        try {
            return new ObjectMapper().writeValueAsString(object);
        } catch (JacksonException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Body creation failed");
        }
    }
}