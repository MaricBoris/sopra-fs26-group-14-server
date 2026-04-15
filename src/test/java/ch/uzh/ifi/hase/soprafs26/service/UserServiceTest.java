package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.entity.Story;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.StoryRepository;
import ch.uzh.ifi.hase.soprafs26.rest.dto.user.StoryGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.user.UserDeleteDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.user.UserPasswordPutDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.user.UserPutDTO;

import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;

public class UserServiceTest {

	@Mock
	private UserRepository userRepository;

    @Mock
    private StoryRepository storyRepository;

	@InjectMocks
	private UserService userService;

	private User testUser;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);

        // given
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testUsername");
        testUser.setPassword("password123");
        testUser.setToken("some-token");

        // default behavior for repository
        Mockito.when(userRepository.save(any())).thenReturn(testUser);
    }

    // --- Create User Tests ---

    @Test
    public void createUser_validInputs_201Created() {
        // when
        User createdUser = userService.createUser(testUser);

        // then
        Mockito.verify(userRepository, Mockito.times(1)).save(any());

        assertEquals(testUser.getId(), createdUser.getId());
        assertEquals(testUser.getUsername(), createdUser.getUsername());
        assertNotNull(createdUser.getToken());
    }

    @Test
    public void createUser_emptyUsername_400BadRequest() {
        // given
        testUser.setUsername("");

        // when/then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> userService.createUser(testUser));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    public void createUser_duplicateUsername_409Conflict() {
        // given -> mock that findByUsername finds an existing user
        Mockito.when(userRepository.findByUsername(any())).thenReturn(testUser);

        // when/then -> attempt to create user with same username
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> userService.createUser(testUser));
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    // --- Login User Tests ---

    @Test
    public void loginUser_validCredentials_200Ok() {
        // given
        Mockito.when(userRepository.findByUsername(testUser.getUsername())).thenReturn(testUser);

        // when
        User loggedInUser = userService.loginUser(testUser);

        // then
        assertNotNull(loggedInUser.getToken());
        assertNotEquals("some-token", loggedInUser.getToken()); // Token was refreshed
        Mockito.verify(userRepository, Mockito.times(1)).save(any());
    }

    @Test
    public void loginUser_emptyPassword_400BadRequest() {
        // given: User with null password
        User loginAttempt = new User();
        loginAttempt.setUsername("testUser");
        loginAttempt.setPassword(null);

        // when/then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            userService.loginUser(loginAttempt);
        });

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Username and password cannot be empty!", exception.getReason());
    }

    @Test
    public void loginUser_wrongPassword_401Unauthorized() {
        // given
        User existingUser = new User();
        existingUser.setUsername("testUsername");
        existingUser.setPassword("REAL_PASSWORD");

        Mockito.when(userRepository.findByUsername("testUsername")).thenReturn(existingUser);

        // when
        User loginAttempt = new User();
        loginAttempt.setUsername("testUsername");
        loginAttempt.setPassword("WRONG_PASSWORD");

        // then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> userService.loginUser(loginAttempt));
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    }

    // --- Logout User Tests ---

    @Test
    public void logoutUser_validToken_204NoContent() {
        // given
        Mockito.when(userRepository.findByToken("valid-token")).thenReturn(testUser);

        // when
        userService.logoutUser("Bearer valid-token");

        // then
        Mockito.verify(userRepository, Mockito.times(1)).save(testUser);
        assertNotEquals("some-token", testUser.getToken()); // Verify token was rotated
    }

    @Test
    public void logoutUser_invalidToken_doesNothing() {
        // given
        Mockito.when(userRepository.findByToken("invalid-token")).thenReturn(null);

        // when
        userService.logoutUser("Bearer invalid-token");

        // then
        Mockito.verify(userRepository, Mockito.times(0)).save(any());
    }

    @Test
    public void logoutUser_nullToken_doesNothing() {
        // when: Calling logout with a null string
        userService.logoutUser(null);

        // then: The repository should never even be searched
        Mockito.verify(userRepository, Mockito.times(0)).findByToken(any());
        Mockito.verify(userRepository, Mockito.times(0)).save(any());
    }

    // --- 1. GET /users (getUsers) Branch Coverage ---

    @Test
    public void getUsers_nullToken_401Unauthorized() {
        assertThrows(ResponseStatusException.class, () -> userService.getUsers(null));
    }

    @Test
    public void getUsers_notBearer_401Unauthorized() {
        assertThrows(ResponseStatusException.class, () -> userService.getUsers("Basic randomtoken"));
    }

    @Test
    public void getUsers_emptyToken_401Unauthorized() {
        // "Bearer " is length 7, so substring(7) is empty
        assertThrows(ResponseStatusException.class, () -> userService.getUsers("Bearer "));
    }

    @Test
    public void getUsers_tokenNotFound_401Unauthorized() {
        Mockito.when(userRepository.findByToken(anyString())).thenReturn(null);
        assertThrows(ResponseStatusException.class, () -> userService.getUsers("Bearer valid-looking-but-fake"));
    }

    @Test
    public void getUsers_validToken_returnsAllUsers() {
        // 1. Setup: Ensure findByToken returns a valid user
        Mockito.when(userRepository.findByToken("valid-token")).thenReturn(testUser);

        // 2. Setup: Mock the final return list
        List<User> allUsers = Collections.singletonList(testUser);
        Mockito.when(userRepository.findAll()).thenReturn(allUsers);

        // 3. Act: Use a properly formatted Bearer token
        List<User> result = userService.getUsers("Bearer valid-token");

        // 4. Assert: This turns the diamond green and the red findAll() green
        assertNotNull(result);
        assertEquals(1, result.size());
        Mockito.verify(userRepository, Mockito.times(1)).findAll();
    }

    // --- 2. findUser Helper Methods ---

    @Test
    public void findUserFromId_success() {
        Mockito.when(userRepository.findById(1L)).thenReturn(java.util.Optional.of(testUser));
        User found = userService.findUserFromId(1L);
        assertEquals(testUser.getUsername(), found.getUsername());
    }

    @Test
    public void findUserFromId_notFound_404() {
        Mockito.when(userRepository.findById(anyLong())).thenReturn(java.util.Optional.empty());
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> userService.findUserFromId(99L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    public void findUserFromToken_notFound_401() {
        Mockito.when(userRepository.findByToken(anyString())).thenReturn(null);
        assertThrows(ResponseStatusException.class, () -> userService.findUserFromToken("fake"));
    }

    // --- 3. checkUsersMatch (The New Code block) ---

    @Test
    public void checkUsersMatch_mismatchId_401() {
        User user1 = new User(); user1.setId(1L); user1.setToken("T1");
        User user2 = new User(); user2.setId(2L); user2.setToken("T1"); // Same token, different ID
        assertThrows(ResponseStatusException.class, () -> userService.checkUsersMatch(user1, user2));
    }

    @Test
    public void checkUsersMatch_mismatchToken_401() {
        User user1 = new User(); user1.setId(1L); user1.setToken("T1");
        User user2 = new User(); user2.setId(1L); user2.setToken("T2"); // Same ID, different token
        assertThrows(ResponseStatusException.class, () -> userService.checkUsersMatch(user1, user2));
    }

    // --- 4. updateUserBio / changePassword / deleteUser ---

    @Test
    public void updateUserBio_success() {
        UserPutDTO dto = new UserPutDTO();
        dto.setBio("New Bio");

        // Mocking the chain: extract -> findFromToken -> findFromId -> checkMatch
        Mockito.when(userRepository.findByToken(anyString())).thenReturn(testUser);
        Mockito.when(userRepository.findById(anyLong())).thenReturn(java.util.Optional.of(testUser));

        User updated = userService.updateUserBio(1L, dto, "Bearer some-token");
        assertEquals("New Bio", updated.getBio());
        Mockito.verify(userRepository).save(any());
    }

    @Test
    public void changePassword_missingFields_400() {
        UserPasswordPutDTO dto = new UserPasswordPutDTO();
        dto.setCurrentPassword(""); // Blank

        Mockito.when(userRepository.findByToken(anyString())).thenReturn(testUser);
        Mockito.when(userRepository.findById(anyLong())).thenReturn(java.util.Optional.of(testUser));

        assertThrows(ResponseStatusException.class, () -> userService.changePassword(1L, dto, "Bearer token"));
    }

    @Test
    public void changePassword_wrongCurrentPassword_401() {
        UserPasswordPutDTO dto = new UserPasswordPutDTO();
        dto.setCurrentPassword("WRONG");
        dto.setNewPassword("NEW");

        Mockito.when(userRepository.findByToken(anyString())).thenReturn(testUser);
        Mockito.when(userRepository.findById(anyLong())).thenReturn(java.util.Optional.of(testUser));

        assertThrows(ResponseStatusException.class, () -> userService.changePassword(1L, dto, "Bearer token"));
    }

    @Test
    public void deleteUser_success() {
        UserDeleteDTO dto = new UserDeleteDTO();
        dto.setPassword("password123");

        Mockito.when(userRepository.findByToken(anyString())).thenReturn(testUser);
        Mockito.when(userRepository.findById(anyLong())).thenReturn(java.util.Optional.of(testUser));

        userService.deleteUser(1L, dto, "Bearer some-token");
        Mockito.verify(userRepository).delete(testUser);
    }

    @Test
    public void deleteUser_wrongPassword_401() {
        UserDeleteDTO dto = new UserDeleteDTO();
        dto.setPassword("WRONG");

        Mockito.when(userRepository.findByToken(anyString())).thenReturn(testUser);
        Mockito.when(userRepository.findById(anyLong())).thenReturn(java.util.Optional.of(testUser));

        assertThrows(ResponseStatusException.class, () -> userService.deleteUser(1L, dto, "Bearer token"));
    }

    @Test
    public void findAllStories_success() {
        // 1. Setup: Mock the repository to return a list of Story entities
        List<Story> mockStories = new java.util.ArrayList<>();
        mockStories.add(new Story());
        Mockito.when(storyRepository.findAll()).thenReturn(mockStories);

        // 2. Act: The service method returns DTOs, so we use List<StoryGetDTO>
        List<StoryGetDTO> result = userService.findAllStories();

        // 3. Assert
        assertNotNull(result);
        // If your service maps them, the size should match
        assertEquals(1, result.size());
    }

    // --- 1. Exhausting the createUser/loginUser validation branches ---

    @Test
    public void createUser_nullUsername_400() {
        testUser.setUsername(null);
        assertThrows(ResponseStatusException.class, () -> userService.createUser(testUser));
    }

    @Test
    public void createUser_nullPassword_400() {
        testUser.setPassword(null);
        assertThrows(ResponseStatusException.class, () -> userService.createUser(testUser));
    }

    @Test
    public void loginUser_blankUsername_400() {
        User loginAttempt = new User();
        loginAttempt.setUsername("   "); // Blank string
        loginAttempt.setPassword("password123");
        assertThrows(ResponseStatusException.class, () -> userService.loginUser(loginAttempt));
    }

    // --- 2. Exhausting changePassword branches ---

    @Test
    public void changePassword_nullNewPassword_400() {
        UserPasswordPutDTO dto = new UserPasswordPutDTO();
        dto.setCurrentPassword("password123");
        dto.setNewPassword(null); // One side of the || check

        Mockito.when(userRepository.findByToken(anyString())).thenReturn(testUser);
        Mockito.when(userRepository.findById(anyLong())).thenReturn(java.util.Optional.of(testUser));

        assertThrows(ResponseStatusException.class, () -> userService.changePassword(1L, dto, "Bearer some-token"));
    }

    // --- 3. Exhausting logoutUser branches ---

    @Test
    public void logoutUser_notBearerPrefix_returnsEarly() {
        // This hits the "else { return; }" branch when token is not null but lacks prefix
        userService.logoutUser("Basic randomtoken");
        Mockito.verify(userRepository, Mockito.times(0)).findByToken(anyString());
    }

    // --- 4. Exhausting deleteUser branches ---

    @Test
    public void deleteUser_nullDTO_400() {
        Mockito.when(userRepository.findByToken(anyString())).thenReturn(testUser);
        Mockito.when(userRepository.findById(anyLong())).thenReturn(java.util.Optional.of(testUser));

        // Passing a literal null instead of a DTO object
        assertThrows(ResponseStatusException.class, () -> userService.deleteUser(1L, null, "Bearer some-token"));
    }

    @Test
    public void deleteUser_blankPassword_400() {
        UserDeleteDTO dto = new UserDeleteDTO();
        dto.setPassword(""); // Triggers the .isBlank() part of the check

        Mockito.when(userRepository.findByToken(anyString())).thenReturn(testUser);
        Mockito.when(userRepository.findById(anyLong())).thenReturn(java.util.Optional.of(testUser));

        assertThrows(ResponseStatusException.class, () -> userService.deleteUser(1L, dto, "Bearer some-token"));
    }

    @Test
    public void changePassword_correctPassword_success() {
        // 1. Setup
        UserPasswordPutDTO dto = new UserPasswordPutDTO();
        dto.setCurrentPassword("password123"); // Matches testUser password
        dto.setNewPassword("brandNewPassword");

        // 2. Mocking the chain
        Mockito.when(userRepository.findById(anyLong())).thenReturn(java.util.Optional.of(testUser));
        Mockito.when(userRepository.findByToken(anyString())).thenReturn(testUser);

        // 3. Act
        userService.changePassword(1L, dto, "Bearer some-token");

        // 4. Assert: This hits the red lines and turns the diamond green
        assertEquals("brandNewPassword", testUser.getPassword());
        Mockito.verify(userRepository, Mockito.times(1)).save(testUser);
    }

    @Test
    public void extractToken_nullToken_throws401() {
        // Branch: bearerToken == null
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> userService.extractToken(null));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    public void extractToken_invalidPrefix_throws401() {
        // Branch: bearerToken != null BUT doesn't start with "Bearer "
        // This turns the yellow diamond green
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> userService.extractToken("Basic some-token"));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }
}