package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.rest.dto.user.UserDeleteDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.user.UserPasswordPutDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.user.UserPutDTO;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.any;

import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.user.UserPostDTO;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * UserControllerTest
 * This is a WebMvcTest which allows to test the UserController i.e. GET/POST
 * request without actually sending them over the network.
 * This tests if the UserController works.
 */
@WebMvcTest(UserController.class)
public class UserControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private UserService userService;

        // --- GET /users ---

	@Test
	public void givenUsers_whenGetUsers_thenReturnJsonArray() throws Exception {
		// given
		User user = new User();
		user.setUsername("firstname@lastname");

		List<User> allUsers = Collections.singletonList(user);

		// this mocks the UserService -> we define above what the userService should
		// return when getUsers() is called
		given(userService.getUsers(anyString())).willReturn(allUsers);

		// when
		MockHttpServletRequestBuilder getRequest = get("/users").contentType(MediaType.APPLICATION_JSON).header("Authorization", "Bearer some-token");

		// then
		mockMvc.perform(getRequest).andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].username", is(user.getUsername())));
	}

        //Test for missing header
        @Test
        public void givenNoAuthorizationHeader_whenGetUsers_thenReturn401() throws Exception {
                // given
                given(userService.getUsers(null))
                        .willThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated"));

                // when
                MockHttpServletRequestBuilder getRequest = get("/users")
                        .contentType(MediaType.APPLICATION_JSON);

                // then
                mockMvc.perform(getRequest)
                        .andExpect(status().isUnauthorized());
        }

        //Test for invalid token
        @Test
        public void givenInvalidToken_whenGetUsers_thenReturn401() throws Exception {
                // given
                given(userService.getUsers(anyString()))
                        .willThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token"));

                // when
                MockHttpServletRequestBuilder getRequest = get("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer invalid-token");

                // then
                mockMvc.perform(getRequest)
                        .andExpect(status().isUnauthorized());
        }

    // --- POST /users (Registration) ---

    @Test
    public void createUser_validInput_201Created() throws Exception {
        // given
        User user = new User();
        user.setId(1L);
        user.setUsername("testUser");
        user.setPassword("password123");
        user.setToken("token-123");
        user.setBio("my bio");

        UserPostDTO userPostDTO = new UserPostDTO();
        userPostDTO.setUsername("testUser");
        userPostDTO.setPassword("password123");

        given(userService.createUser(Mockito.any())).willReturn(user);

        // when
        MockHttpServletRequestBuilder postRequest = post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(userPostDTO));

        // then
        mockMvc.perform(postRequest)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(user.getId().intValue())))
                .andExpect(jsonPath("$.username", is(user.getUsername())))
                .andExpect(jsonPath("$.token", is(user.getToken())))
                .andExpect(jsonPath("$.bio", is(user.getBio())));
    }

    @Test
    public void createUser_emptyFields_400BadRequest() throws Exception {
        // given
        UserPostDTO userPostDTO = new UserPostDTO();
        userPostDTO.setUsername(""); // Blank username

        given(userService.createUser(Mockito.any()))
                .willThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username and password cannot be empty!"));

        // when
        MockHttpServletRequestBuilder postRequest = post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(userPostDTO));

        // then
        mockMvc.perform(postRequest)
                .andExpect(status().isBadRequest());
    }

    @Test
    public void createUser_duplicateUsername_409Conflict() throws Exception {
        // given
        UserPostDTO userPostDTO = new UserPostDTO();
        userPostDTO.setUsername("existingUser");
        userPostDTO.setPassword("password123");

        given(userService.createUser(Mockito.any()))
                .willThrow(new ResponseStatusException(HttpStatus.CONFLICT, "The username provided is not unique."));

        // when
        MockHttpServletRequestBuilder postRequest = post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(userPostDTO));

        // then
        mockMvc.perform(postRequest)
                .andExpect(status().isConflict());
    }

    // --- POST /users/login ---

    @Test
    public void loginUser_validCredentials_200Ok() throws Exception {
        // given
        User user = new User();
        user.setId(1L);
        user.setUsername("testUser");
        user.setPassword("password123");
        user.setToken("login-token");
        user.setBio("my bio");

        UserPostDTO userPostDTO = new UserPostDTO();
        userPostDTO.setUsername("testUser");
        userPostDTO.setPassword("password123");

        given(userService.loginUser(Mockito.any())).willReturn(user);

        // when
        MockHttpServletRequestBuilder postRequest = post("/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(userPostDTO));

        // then
        mockMvc.perform(postRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", is(user.getToken())))
                .andExpect(jsonPath("$.username", is(user.getUsername())));
    }

    @Test
    public void loginUser_invalidCredentials_401Unauthorized() throws Exception {
        // given
        UserPostDTO userPostDTO = new UserPostDTO();
        userPostDTO.setUsername("wrongUser");
        userPostDTO.setPassword("wrongPass");

        given(userService.loginUser(Mockito.any()))
                .willThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password."));

        // when
        MockHttpServletRequestBuilder postRequest = post("/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(userPostDTO));

        // then
        mockMvc.perform(postRequest)
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void loginUser_emptyFields_400BadRequest() throws Exception {
        // given
        UserPostDTO userPostDTO = new UserPostDTO();
        userPostDTO.setUsername(""); // Empty username

        given(userService.loginUser(Mockito.any()))
                .willThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username and password cannot be empty!"));

        // when
        MockHttpServletRequestBuilder postRequest = post("/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(userPostDTO));

        // then
        mockMvc.perform(postRequest)
                .andExpect(status().isBadRequest());
    }

    // --- POST /users/logout ---

    @Test
    public void logoutUser_validToken_204NoContent() throws Exception {
        // given
        MockHttpServletRequestBuilder postRequest = post("/users/logout")
                .header("Authorization", "Bearer some-token");

        // then
        mockMvc.perform(postRequest)
                .andExpect(status().isNoContent());
    }

    @Test
    public void logoutUser_missingToken_204NoContent() throws Exception {
        // given
        // No header is provided at all
        MockHttpServletRequestBuilder postRequest = post("/users/logout");

        // then
        mockMvc.perform(postRequest)
                .andExpect(status().isNoContent());
    }
  
  	@Test
	  public void getUser_validId_returnsUser() throws Exception {
		  //ADD OTHER USER ATTRIBUTES
   		User user = new User();
    	user.setId(1L);
    	//user.setBio("Test Bio");
    	user.setUsername("testUsername");

    	given(userService.findUserFromId(1L)).willReturn(user);
    	given(userService.findUserFromToken(Mockito.any())).willReturn(user);

    
    	MockHttpServletRequestBuilder getRequest = get("/users/1")
        	.contentType(MediaType.APPLICATION_JSON)
        	.header("Authorization", "Bearer testToken");

    
    	mockMvc.perform(getRequest)
        	.andExpect(status().isOk())
        	.andExpect(jsonPath("$.id", is(user.getId().intValue())))
        	.andExpect(jsonPath("$.username", is(user.getUsername())));
        	//.andExpect(jsonPath("$.bio", is(user.getBio())));
	}
  
  	@Test
	  public void getUser_invalidId_returns404() throws Exception {
    
    	given(userService.findUserFromId(Mockito.any()))
        	.willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    	given(userService.findUserFromToken(Mockito.any())).willReturn(new User());

    
    	MockHttpServletRequestBuilder getRequest = get("/users/99")
        	.contentType(MediaType.APPLICATION_JSON)
        	.header("Authorization", "Bearer testToken");

    
    	mockMvc.perform(getRequest)
        	.andExpect(status().isNotFound());
	}
  
  	@Test
	  public void getUser_Unauthorized_returns401() throws Exception {
    
    	given(userService.findUserFromId(Mockito.any()))
        	.willThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized"));
    	given(userService.findUserFromToken(Mockito.any())).willReturn(new User());

    
    	MockHttpServletRequestBuilder getRequest = get("/users/99")
        	.contentType(MediaType.APPLICATION_JSON)
        	.header("Authorization", "Bearer testToken");

    
    	mockMvc.perform(getRequest)
        	.andExpect(status().isUnauthorized());
	}

    @Test
    public void getUser_nonBearerHeader_returnsUser() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setUsername("testUsername");

        given(userService.findUserFromId(1L)).willReturn(user);
        given(userService.findUserFromToken(any())).willReturn(user);

        // Branch: token != null AND DOES NOT start with "Bearer "
        MockHttpServletRequestBuilder getRequest = get("/users/1")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "just-a-raw-token-string");

        mockMvc.perform(getRequest)
                .andExpect(status().isOk());
    }

    // --- 2. Fix "Fully Uncovered" changePassword (PUT) ---

    @Test
    public void changePassword_validInput_200Ok() throws Exception {
        UserPasswordPutDTO passwordDTO = new UserPasswordPutDTO();
        passwordDTO.setCurrentPassword("oldPass");
        passwordDTO.setNewPassword("newPass");

        mockMvc.perform(put("/users/1/password")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(passwordDTO)))
                .andExpect(status().isOk());
    }

    // --- 3. Fix "Fully Uncovered" updateUserBio (PUT) ---

    @Test
    public void updateUserBio_validInput_200Ok() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setBio("Updated Bio");

        UserPutDTO putDTO = new UserPutDTO();
        putDTO.setBio("Updated Bio");

        given(userService.updateUserBio(anyLong(), any(), any())).willReturn(user);

        mockMvc.perform(put("/users/1")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(putDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bio", is("Updated Bio")));
    }

    // --- 4. Fix "Fully Uncovered" deleteUser (DELETE) ---

    @Test
    public void deleteUser_withBody_204NoContent() throws Exception {
        UserDeleteDTO deleteDTO = new UserDeleteDTO();
        deleteDTO.setPassword("pass");

        mockMvc.perform(delete("/users/1")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(deleteDTO)))
                .andExpect(status().isNoContent());
    }

    @Test
    public void deleteUser_noBody_204NoContent() throws Exception {
        // Testing the (required = false) branch of @RequestBody
        mockMvc.perform(delete("/users/1")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isNoContent());
    }

    // --- 1. Fix the Red Block for /results ---

    @Test
    public void getResults_validBearerToken_200Ok() throws Exception {
        // Mock the service to return an empty list or some stories
        given(userService.findAllStories()).willReturn(new ArrayList<>());

        mockMvc.perform(get("/results")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk());
    }

    // --- 2. Fix the Yellow Diamond (Branch Coverage) ---
    // This covers the case where token != null BUT startsWith("Bearer ") is FALSE
    @Test
    public void getResults_nonBearerToken_200Ok() throws Exception {
        given(userService.findAllStories()).willReturn(new ArrayList<>());

        mockMvc.perform(get("/results")
                        .header("Authorization", "just-a-raw-token-without-bearer-prefix"))
                .andExpect(status().isOk());
    }

    // --- 3. Fix the "Token is Null" branch ---
    @Test
    public void getResults_nullToken_200Ok() throws Exception {
        given(userService.findAllStories()).willReturn(new ArrayList<>());

        // Now this will correctly hit the 'if (token == null)' path
        // inside your method and return 200 instead of 400.
        mockMvc.perform(get("/results"))
                .andExpect(status().isOk());
    }

	/**
	 * Helper Method to convert userPostDTO into a JSON string such that the input
	 * can be processed
	 * Input will look like this: {"name": "Test User", "username": "testUsername"}
	 * 
	 * @param object
	 * @return string
	 */
	private String asJsonString(final Object object) {
		try {
			return new ObjectMapper().writeValueAsString(object);
		} catch (JacksonException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					String.format("The request body could not be created.%s", e.toString()));
		}
	}
}
