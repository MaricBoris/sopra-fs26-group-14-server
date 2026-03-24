package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;

public class UserServiceTest {

	@Mock
	private UserRepository userRepository;

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
        Mockito.when(userRepository.save(Mockito.any())).thenReturn(testUser);
    }

    // --- Create User Tests ---

    @Test
    public void createUser_validInputs_201Created() {
        // when
        User createdUser = userService.createUser(testUser);

        // then
        Mockito.verify(userRepository, Mockito.times(1)).save(Mockito.any());

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
        Mockito.when(userRepository.findByUsername(Mockito.any())).thenReturn(testUser);

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
        Mockito.verify(userRepository, Mockito.times(1)).save(Mockito.any());
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
        Mockito.verify(userRepository, Mockito.times(0)).save(Mockito.any());
    }

    @Test
    public void logoutUser_nullToken_doesNothing() {
        // when: Calling logout with a null string
        userService.logoutUser(null);

        // then: The repository should never even be searched
        Mockito.verify(userRepository, Mockito.times(0)).findByToken(Mockito.any());
        Mockito.verify(userRepository, Mockito.times(0)).save(Mockito.any());
    }
}