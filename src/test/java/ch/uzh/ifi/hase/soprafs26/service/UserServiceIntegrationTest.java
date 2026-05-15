package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.entity.Story;
import ch.uzh.ifi.hase.soprafs26.repository.*;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.server.ResponseStatusException;
import ch.uzh.ifi.hase.soprafs26.rest.dto.user.StoryGetDTO;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

    @Autowired
    private StoryRepository storyRepository;

    @Autowired
    private UserStatisticsRepository userStatisticsRepository;

    @Autowired
    private UserAchievementRepository  userAchievementRepository;

    @Autowired
    private GenreMasterRepository genreMasterRepository;

    @BeforeEach
    public void setup() {
        gameRepository.deleteAll();
        roomRepository.deleteAll();
        storyRepository.deleteAll();
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

    

    @Test
    public void findAllStoriesOfUser_userIsWinner_success() {
        User winner = new User();
        winner.setUsername("winner");
        winner.setPassword("password");
        winner.setToken("token1");
        winner = userRepository.save(winner);
        userRepository.flush();
        
        User loser = new User();
        loser.setUsername("loser");
        loser.setPassword("password");
        loser.setToken("token2");
        loser = userRepository.save(loser);
        userRepository.flush();
        
        Story story = new Story();
        story.setWinner(winner);
        story.setLoser(loser);
        story.setJudges(new ArrayList<>());
        story = storyRepository.save(story);
        storyRepository.flush();
        
        List<StoryGetDTO> result = userService.findAllStoriesOfUser(winner.getId());
        
        assertEquals(1, result.size());
    }

    @Test
    public void findAllStoriesOfUser_userIsLoser_success() {
        User winner = new User();
        winner.setUsername("winner");
        winner.setPassword("password");
        winner.setToken("token1");
        winner = userRepository.save(winner);
        userRepository.flush();
        
        User loser = new User();
        loser.setUsername("loser");
        loser.setPassword("password");
        loser.setToken("token2");
        loser = userRepository.save(loser);
        userRepository.flush();
        
        Story story = new Story();
        story.setWinner(winner);
        story.setLoser(loser);
        story.setJudges(new ArrayList<>());
        story = storyRepository.save(story);
        storyRepository.flush();
        
        List<StoryGetDTO> result = userService.findAllStoriesOfUser(loser.getId());
        
        assertEquals(1, result.size());
    }

    @Test
    public void findAllStoriesOfUser_userIsJudge_success() {
        User winner = new User();
        winner.setUsername("winner");
        winner.setPassword("password");
        winner.setToken("token1");
        winner = userRepository.save(winner);
        userRepository.flush();
        
        User loser = new User();
        loser.setUsername("loser");
        loser.setPassword("password");
        loser.setToken("token2");
        loser = userRepository.save(loser);
        userRepository.flush();
        
        User judge = new User();
        judge.setUsername("judge");
        judge.setPassword("password");
        judge.setToken("token3");
        judge = userRepository.save(judge);
        userRepository.flush();
        
        Story story = new Story();
        story.setWinner(winner);
        story.setLoser(loser);
        story.setJudges(Arrays.asList(judge));
        story = storyRepository.save(story);
        storyRepository.flush();
        
        List<StoryGetDTO> result = userService.findAllStoriesOfUser(judge.getId());
        
        assertEquals(1, result.size());
    }

    @Test
    public void findAllStoriesOfUser_multipleStories_returnsAllRelevant() {
        User user = new User();
        user.setUsername("mainUser");
        user.setPassword("password");
        user.setToken("token1");
        user = userRepository.save(user);
        userRepository.flush();
        
        User otherUser1 = new User();
        otherUser1.setUsername("other1");
        otherUser1.setPassword("password");
        otherUser1.setToken("token2");
        otherUser1 = userRepository.save(otherUser1);
        userRepository.flush();
        
        User otherUser2 = new User();
        otherUser2.setUsername("other2");
        otherUser2.setPassword("password");
        otherUser2.setToken("token3");
        otherUser2 = userRepository.save(otherUser2);
        userRepository.flush();
        
        Story story1 = new Story();
        story1.setWinner(user);
        story1.setLoser(otherUser1);
        story1.setJudges(new ArrayList<>());
        story1 = storyRepository.save(story1);
        
        Story story2 = new Story();
        story2.setWinner(otherUser1);
        story2.setLoser(user);
        story2.setJudges(new ArrayList<>());
        story2 = storyRepository.save(story2);
        
        Story story3 = new Story();
        story3.setWinner(otherUser1);
        story3.setLoser(otherUser2);
        story3.setJudges(Arrays.asList(user));
        story3 = storyRepository.save(story3);
        
        Story story4 = new Story();
        story4.setWinner(otherUser1);
        story4.setLoser(otherUser2);
        story4.setJudges(new ArrayList<>());
        story4 = storyRepository.save(story4);
        
        storyRepository.flush();
        
        List<StoryGetDTO> result = userService.findAllStoriesOfUser(user.getId());
        
        assertEquals(3, result.size());
    }

    @Test
    public void findAllStoriesOfUser_userNotInvolved_returnsEmpty() {
        User user = new User();
        user.setUsername("mainUser");
        user.setPassword("password");
        user.setToken("token1");
        user = userRepository.save(user);
        userRepository.flush();
        
        User otherUser1 = new User();
        otherUser1.setUsername("other1");
        otherUser1.setPassword("password");
        otherUser1.setToken("token2");
        otherUser1 = userRepository.save(otherUser1);
        userRepository.flush();
        
        User otherUser2 = new User();
        otherUser2.setUsername("other2");
        otherUser2.setPassword("password");
        otherUser2.setToken("token3");
        otherUser2 = userRepository.save(otherUser2);
        userRepository.flush();
        
        Story story = new Story();
        story.setWinner(otherUser1);
        story.setLoser(otherUser2);
        story.setJudges(new ArrayList<>());
        story = storyRepository.save(story);
        storyRepository.flush();
        
        List<StoryGetDTO> result = userService.findAllStoriesOfUser(user.getId());
        
        assertEquals(0, result.size());
    }

    @Test
    public void findAllStoriesOfUser_noStories_returnsEmpty() {
        User user = new User();
        user.setUsername("mainUser");
        user.setPassword("password");
        user.setToken("token1");
        user = userRepository.save(user);
        userRepository.flush();
        
        List<StoryGetDTO> result = userService.findAllStoriesOfUser(user.getId());
        
        assertEquals(0, result.size());
    }

    @Test
    public void findAllStoriesOfUser_multipleJudges_success() {
        User winner = new User();
        winner.setUsername("winner");
        winner.setPassword("password");
        winner.setToken("token1");
        winner = userRepository.save(winner);
        userRepository.flush();
        
        User loser = new User();
        loser.setUsername("loser");
        loser.setPassword("password");
        loser.setToken("token2");
        loser = userRepository.save(loser);
        userRepository.flush();
        
        User judge1 = new User();
        judge1.setUsername("judge1");
        judge1.setPassword("password");
        judge1.setToken("token3");
        judge1 = userRepository.save(judge1);
        userRepository.flush();
        
        User judge2 = new User();
        judge2.setUsername("judge2");
        judge2.setPassword("password");
        judge2.setToken("token4");
        judge2 = userRepository.save(judge2);
        userRepository.flush();
        
        Story story = new Story();
        story.setWinner(winner);
        story.setLoser(loser);
        story.setJudges(Arrays.asList(judge1, judge2));
        story = storyRepository.save(story);
        storyRepository.flush();
        
        List<StoryGetDTO> result1 = userService.findAllStoriesOfUser(judge1.getId());
        List<StoryGetDTO> result2 = userService.findAllStoriesOfUser(judge2.getId());
        
        assertEquals(1, result1.size());
        assertEquals(1, result2.size());
    }

    // --- User Statistics Integration ---

    @Test
    public void getUserStatistics_newlyCreatedUser_hasEmptyStats() {
        User user = new User();
        user.setUsername("statCheck");
        user.setPassword("password");
        user = userService.createUser(user);

        ch.uzh.ifi.hase.soprafs26.entity.UserStatistics stats = userService.getUserStatistics(user.getId(), "Bearer " + user.getToken());

        assertNotNull(stats);
        assertEquals(0, stats.getGamesPlayed());
        assertEquals(0, stats.getGamesWon());
        assertTrue(stats.getWinsByGenre().isEmpty());
    }
}