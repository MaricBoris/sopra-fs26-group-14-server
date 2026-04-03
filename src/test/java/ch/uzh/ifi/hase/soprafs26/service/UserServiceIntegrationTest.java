package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs26.repository.RoomRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;

@WebAppConfiguration
@SpringBootTest
public class UserServiceIntegrationTest {

    @Qualifier("userRepository")
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Qualifier("roomRepository")
    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private GameRepository gameRepository;

    @BeforeEach
    public void setup() {
        gameRepository.deleteAll();
        roomRepository.deleteAll();
        userRepository.deleteAll();
    }

    // --- Create User (POST /users) ---

    @Test
    public void createUser_validInputs_201Created() {
        User testUser = new User();
        testUser.setUsername("testUsername");
        testUser.setPassword("password123");

        User createdUser = userService.createUser(testUser);

        assertNotNull(createdUser.getId());
        assertEquals(testUser.getUsername(), createdUser.getUsername());
        assertEquals(testUser.getPassword(), createdUser.getPassword());
        assertNotNull(createdUser.getToken());
        assertNotNull(userRepository.findByUsername("testUsername"));
    }

    @Test
    public void createUser_emptyFields_400BadRequest() {
        User badUser = new User();
        badUser.setUsername(""); // isBlank() check

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            userService.createUser(badUser);
        });
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Username and password cannot be empty!", exception.getReason());
    }

    @Test
    public void createUser_duplicateUsername_409Conflict() {
        User firstUser = new User();
        firstUser.setUsername("uniqueUser");
        firstUser.setPassword("password123");
        userService.createUser(firstUser);

        User duplicateUser = new User();
        duplicateUser.setUsername("uniqueUser");
        duplicateUser.setPassword("newPass");

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            userService.createUser(duplicateUser);
        });
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    // --- Login User (POST /users/login) ---

    @Test
    public void loginUser_validCredentials_200Ok() {
        User existingUser = new User();
        existingUser.setUsername("loginUser");
        existingUser.setPassword("securePass");
        existingUser = userService.createUser(existingUser);
        String oldToken = existingUser.getToken();

        User credentials = new User();
        credentials.setUsername("loginUser");
        credentials.setPassword("securePass");
        User loggedInUser = userService.loginUser(credentials);

        assertEquals(existingUser.getId(), loggedInUser.getId());
        assertNotEquals(oldToken, loggedInUser.getToken());
        assertNotNull(loggedInUser.getToken());
    }

    @Test
    public void loginUser_emptyFields_400BadRequest() {
        User loginAttempt = new User();
        loginAttempt.setUsername("user");
        loginAttempt.setPassword(null); // password is null

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            userService.loginUser(loginAttempt);
        });
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Username and password cannot be empty!", exception.getReason());
    }

    @Test
    public void loginUser_wrongCredentials_401Unauthorized() {
        User realUser = new User();
        realUser.setUsername("correctUser");
        realUser.setPassword("correctPass");
        userService.createUser(realUser);

        User wrongPass = new User();
        wrongPass.setUsername("correctUser");
        wrongPass.setPassword("WRONG");

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            userService.loginUser(wrongPass);
        });
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    }

    // --- Logout User (POST /users/logout) ---

    @Test
    public void logoutUser_validToken_204NoContent() {
        User user = new User();
        user.setUsername("logoutUser");
        user.setPassword("pass");
        user = userService.createUser(user);
        String originalToken = user.getToken();

        userService.logoutUser("Bearer " + originalToken);

        User postLogoutUser = userRepository.findById(user.getId()).orElse(null);
        assertNotNull(postLogoutUser);
        assertNotEquals(originalToken, postLogoutUser.getToken());
    }

    @Test
    public void logoutUser_invalidToken_204NoContent() {
        User user = new User();
        user.setUsername("stayLoggedIn");
        user.setPassword("pass");
        user = userService.createUser(user);
        String currentToken = user.getToken();

        userService.logoutUser("Bearer fake-token");

        User foundUser = userRepository.findByUsername("stayLoggedIn");
        assertEquals(currentToken, foundUser.getToken());
    }
}