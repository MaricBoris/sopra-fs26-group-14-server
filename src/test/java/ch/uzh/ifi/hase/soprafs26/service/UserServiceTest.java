package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.entity.Story;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.StoryRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs26.rest.dto.user.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

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

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testUsername");
        testUser.setPassword("password123");
        testUser.setToken("some-token");

        Mockito.when(userRepository.save(any())).thenReturn(testUser);
    }

    // --- USER REGISTRATION (createUser) ---

    @Test
    public void createUser_validInputs_201Created() {
        User createdUser = userService.createUser(testUser);

        Mockito.verify(userRepository, Mockito.times(1)).save(any());
        assertEquals(testUser.getId(), createdUser.getId());
        assertNotNull(createdUser.getToken());
    }

    @Test
    public void createUser_emptyUsername_400BadRequest() {
        testUser.setUsername("");
        assertThrows(ResponseStatusException.class, () -> userService.createUser(testUser));
    }

    @Test
    public void createUser_nullUsername_400BadRequest() {
        testUser.setUsername(null);
        assertThrows(ResponseStatusException.class, () -> userService.createUser(testUser));
    }

    @Test
    public void createUser_nullPassword_400BadRequest() {
        testUser.setPassword(null);
        assertThrows(ResponseStatusException.class, () -> userService.createUser(testUser));
    }

    @Test
    public void createUser_duplicateUsername_409Conflict() {
        Mockito.when(userRepository.findByUsername(any())).thenReturn(testUser);
        assertThrows(ResponseStatusException.class, () -> userService.createUser(testUser));
    }

    // --- AUTHENTICATION (loginUser & logoutUser) ---

    @Test
    public void loginUser_validCredentials_200Ok() {
        Mockito.when(userRepository.findByUsername(testUser.getUsername())).thenReturn(testUser);

        User loggedInUser = userService.loginUser(testUser);

        assertNotEquals("some-token", loggedInUser.getToken());
        Mockito.verify(userRepository).save(any());
    }

    @Test
    public void loginUser_emptyPassword_400BadRequest() {
        User loginAttempt = new User();
        loginAttempt.setUsername("testUser");
        loginAttempt.setPassword(null);

        assertThrows(ResponseStatusException.class, () -> userService.loginUser(loginAttempt));
    }

    @Test
    public void loginUser_blankUsername_400BadRequest() {
        User loginAttempt = new User();
        loginAttempt.setUsername("   ");
        loginAttempt.setPassword("password123");

        assertThrows(ResponseStatusException.class, () -> userService.loginUser(loginAttempt));
    }

    @Test
    public void loginUser_wrongPassword_401Unauthorized() {
        User existingUser = new User();
        existingUser.setPassword("REAL_PASSWORD");
        Mockito.when(userRepository.findByUsername(any())).thenReturn(existingUser);

        User loginAttempt = new User();
        loginAttempt.setPassword("WRONG_PASSWORD");

        assertThrows(ResponseStatusException.class, () -> userService.loginUser(loginAttempt));
    }

    @Test
    public void logoutUser_validToken_success() {
        Mockito.when(userRepository.findByToken("valid-token")).thenReturn(testUser);

        userService.logoutUser("Bearer valid-token");

        Mockito.verify(userRepository).save(testUser);
        assertNotEquals("some-token", testUser.getToken());
    }

    @Test
    public void logoutUser_invalidToken_doesNothing() {
        Mockito.when(userRepository.findByToken("invalid-token")).thenReturn(null);
        userService.logoutUser("Bearer invalid-token");
        Mockito.verify(userRepository, Mockito.never()).save(any());
    }

    @Test
    public void logoutUser_notBearerPrefix_returnsEarly() {
        userService.logoutUser("Basic randomtoken");
        Mockito.verify(userRepository, Mockito.never()).findByToken(anyString());
    }

    @Test
    public void logoutUser_nullToken_doesNothing() {
        userService.logoutUser(null);
        Mockito.verify(userRepository, Mockito.never()).findByToken(any());
    }

    // --- DATA RETRIEVAL (getUsers & findUser) ---

    @Test
    public void getUsers_validToken_returnsAllUsers() {
        Mockito.when(userRepository.findByToken("valid-token")).thenReturn(testUser);
        Mockito.when(userRepository.findAll()).thenReturn(Collections.singletonList(testUser));

        List<User> result = userService.getUsers("Bearer valid-token");

        assertEquals(1, result.size());
        Mockito.verify(userRepository).findAll();
    }

    @Test
    public void getUsers_nullToken_401Unauthorized() {
        assertThrows(ResponseStatusException.class, () -> userService.getUsers(null));
    }

    @Test
    public void getUsers_notBearer_401Unauthorized() {
        assertThrows(ResponseStatusException.class, () -> userService.getUsers("Basic token"));
    }

    @Test
    public void getUsers_emptyToken_401Unauthorized() {
        assertThrows(ResponseStatusException.class, () -> userService.getUsers("Bearer "));
    }

    @Test
    public void getUsers_tokenNotFound_401Unauthorized() {
        Mockito.when(userRepository.findByToken(anyString())).thenReturn(null);
        assertThrows(ResponseStatusException.class, () -> userService.getUsers("Bearer fake"));
    }

    @Test
    public void findUserFromId_success() {
        Mockito.when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        User found = userService.findUserFromId(1L);
        assertEquals(testUser.getUsername(), found.getUsername());
    }

    @Test
    public void findUserFromId_notFound_404NotFound() {
        Mockito.when(userRepository.findById(anyLong())).thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class, () -> userService.findUserFromId(99L));
    }

    @Test
    public void findUserFromToken_notFound_401Unauthorized() {
        Mockito.when(userRepository.findByToken(anyString())).thenReturn(null);
        assertThrows(ResponseStatusException.class, () -> userService.findUserFromToken("fake"));
    }

    // --- PROFILE UPDATES (updateBio & changePassword) ---

    @Test
    public void updateUserBio_success() {
        UserPutDTO dto = new UserPutDTO();
        dto.setBio("New Bio");

        Mockito.when(userRepository.findByToken(anyString())).thenReturn(testUser);
        Mockito.when(userRepository.findById(anyLong())).thenReturn(Optional.of(testUser));

        User updated = userService.updateUserBio(1L, dto, "Bearer token");
        assertEquals("New Bio", updated.getBio());
        Mockito.verify(userRepository).save(testUser);
    }

    @Test
    public void changePassword_correctPassword_success() {
        UserPasswordPutDTO dto = new UserPasswordPutDTO();
        dto.setCurrentPassword("password123");
        dto.setNewPassword("brandNewPassword");

        Mockito.when(userRepository.findById(anyLong())).thenReturn(Optional.of(testUser));
        Mockito.when(userRepository.findByToken(anyString())).thenReturn(testUser);

        userService.changePassword(1L, dto, "Bearer token");

        assertEquals("brandNewPassword", testUser.getPassword());
        Mockito.verify(userRepository).save(testUser);
    }

    @Test
    public void changePassword_missingFields_400BadRequest() {
        UserPasswordPutDTO dto = new UserPasswordPutDTO();
        dto.setCurrentPassword("");

        Mockito.when(userRepository.findById(anyLong())).thenReturn(Optional.of(testUser));
        Mockito.when(userRepository.findByToken(anyString())).thenReturn(testUser);

        assertThrows(ResponseStatusException.class, () -> userService.changePassword(1L, dto, "Bearer token"));
    }

    @Test
    public void changePassword_nullNewPassword_400BadRequest() {
        UserPasswordPutDTO dto = new UserPasswordPutDTO();
        dto.setCurrentPassword("password123");
        dto.setNewPassword(null);

        Mockito.when(userRepository.findById(anyLong())).thenReturn(Optional.of(testUser));
        Mockito.when(userRepository.findByToken(anyString())).thenReturn(testUser);

        assertThrows(ResponseStatusException.class, () -> userService.changePassword(1L, dto, "Bearer token"));
    }

    @Test
    public void changePassword_wrongCurrentPassword_401Unauthorized() {
        UserPasswordPutDTO dto = new UserPasswordPutDTO();
        dto.setCurrentPassword("WRONG");
        dto.setNewPassword("NEW");

        Mockito.when(userRepository.findById(anyLong())).thenReturn(Optional.of(testUser));
        Mockito.when(userRepository.findByToken(anyString())).thenReturn(testUser);

        assertThrows(ResponseStatusException.class, () -> userService.changePassword(1L, dto, "Bearer token"));
    }

    // --- DELETION (deleteUser) ---

    @Test
    public void deleteUser_success() {
        UserDeleteDTO dto = new UserDeleteDTO();
        dto.setPassword("password123");

        Mockito.when(userRepository.findByToken(anyString())).thenReturn(testUser);
        Mockito.when(userRepository.findById(anyLong())).thenReturn(Optional.of(testUser));

        userService.deleteUser(1L, dto, "Bearer token");
        Mockito.verify(userRepository).delete(testUser);
    }

    @Test
    public void deleteUser_nullDTO_400BadRequest() {
        Mockito.when(userRepository.findById(anyLong())).thenReturn(Optional.of(testUser));
        Mockito.when(userRepository.findByToken(anyString())).thenReturn(testUser);

        assertThrows(ResponseStatusException.class, () -> userService.deleteUser(1L, null, "Bearer token"));
    }

    @Test
    public void deleteUser_blankPassword_400BadRequest() {
        UserDeleteDTO dto = new UserDeleteDTO();
        dto.setPassword("");

        Mockito.when(userRepository.findById(anyLong())).thenReturn(Optional.of(testUser));
        Mockito.when(userRepository.findByToken(anyString())).thenReturn(testUser);

        assertThrows(ResponseStatusException.class, () -> userService.deleteUser(1L, dto, "Bearer token"));
    }

    @Test
    public void deleteUser_wrongPassword_401Unauthorized() {
        UserDeleteDTO dto = new UserDeleteDTO();
        dto.setPassword("WRONG");

        Mockito.when(userRepository.findById(anyLong())).thenReturn(Optional.of(testUser));
        Mockito.when(userRepository.findByToken(anyString())).thenReturn(testUser);

        assertThrows(ResponseStatusException.class, () -> userService.deleteUser(1L, dto, "Bearer token"));
    }

    // --- STORY MANAGEMENT (findAllStories) ---

    @Test
    public void findAllStories_success() {
        List<Story> mockStories = new ArrayList<>();
        mockStories.add(new Story());
        Mockito.when(storyRepository.findAll()).thenReturn(mockStories);

        List<StoryGetDTO> result = userService.findAllStories();

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    // --- INTERNAL HELPERS (extractToken & checkMatch) ---

    @Test
    public void extractToken_nullToken_throws401() {
        assertThrows(ResponseStatusException.class, () -> userService.extractToken(null));
    }

    @Test
    public void extractToken_invalidPrefix_throws401() {
        assertThrows(ResponseStatusException.class, () -> userService.extractToken("Basic token"));
    }

    @Test
    public void checkUsersMatch_mismatchId_401() {
        User user1 = new User(); user1.setId(1L); user1.setToken("T1");
        User user2 = new User(); user2.setId(2L); user2.setToken("T1");
        assertThrows(ResponseStatusException.class, () -> userService.checkUsersMatch(user1, user2));
    }

    @Test
    public void checkUsersMatch_mismatchToken_401() {
        User user1 = new User(); user1.setId(1L); user1.setToken("T1");
        User user2 = new User(); user2.setId(1L); user2.setToken("T2");
        assertThrows(ResponseStatusException.class, () -> userService.checkUsersMatch(user1, user2));
    }
}