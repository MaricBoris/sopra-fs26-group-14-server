package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.entity.Story;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.entity.UserStatistics;
import ch.uzh.ifi.hase.soprafs26.repository.*;
import ch.uzh.ifi.hase.soprafs26.rest.dto.stats.LeaderboardEntryGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.user.StoryGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.user.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import ch.uzh.ifi.hase.soprafs26.entity.GenreMaster;
import java.util.HashMap;
import java.util.Map;
import ch.uzh.ifi.hase.soprafs26.entity.GenreMaster;
import ch.uzh.ifi.hase.soprafs26.rest.dto.user.UserDeleteDTO;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;

public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private StoryRepository storyRepository;

    @Mock
    private GenreMasterRepository genreMasterRepository;

    @Mock
    private UserStatisticsRepository userStatisticsRepository;

    @Mock
    private UserAchievementRepository  userAchievementRepository;

    @Mock
    private UserStatisticsRepository userStatisticsRepository2;

    @Mock
    private StoryRatingRepository storyRatingRepository;

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

    // --- STORY MANAGEMENT (findAllStories & findStoryById) ---

    @Test
    public void findAllStories_success() {
        List<Story> mockStories = new ArrayList<>();
        mockStories.add(new Story());
        Mockito.when(storyRepository.findAll()).thenReturn(mockStories);

        List<StoryGetDTO> result = userService.findAllStories();

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    public void findStoryById_success() {
        Story story = new Story();
        story.setId(1L);
        Mockito.when(storyRepository.findById(1L)).thenReturn(Optional.of(story));

        StoryGetDTO result = userService.findStoryById(1L);

        assertNotNull(result);
    }

    @Test
    public void findStoryById_notFound_404NotFound() {
        Mockito.when(storyRepository.findById(anyLong())).thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class, () -> userService.findStoryById(99L));
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

    @Test
    public void findAllStoriesOfUser_userIsWinner_returnsStories() {
        long userId = 1L;
        
        User user = new User();
        user.setId(userId);
        
        User otherUser = new User();
        otherUser.setId(2L);
        
        Story story1 = new Story();
        story1.setWinner(user);
        story1.setLoser(otherUser);
        story1.setJudges(new ArrayList<>());
        
        Story story2 = new Story();
        story2.setWinner(otherUser);
        story2.setLoser(user);
        story2.setJudges(new ArrayList<>());
        
        List<Story> allStories = Arrays.asList(story1, story2);
        
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(storyRepository.findHistoryForUser(any())).willReturn(allStories);
        

        List<StoryGetDTO> result = userService.findAllStoriesOfUser(userId);
        
        assertEquals(2, result.size());
    }

    @Test
    public void findAllStoriesOfUser_userIsLoser_returnsStories() {

        long userId = 1L;
        
        User user = new User();
        user.setId(userId);
        
        User otherUser = new User();
        otherUser.setId(2L);
        
        Story story = new Story();
        story.setWinner(otherUser);
        story.setLoser(user);
        story.setJudges(new ArrayList<>());
        
        List<Story> allStories = Arrays.asList(story);
        
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(storyRepository.findHistoryForUser(any())).willReturn(allStories);
        
    
        List<StoryGetDTO> result = userService.findAllStoriesOfUser(userId);
        
    
        assertEquals(1, result.size());
    }

    @Test
    public void findAllStoriesOfUser_userIsJudge_returnsStories() {

        long userId = 1L;
        
        User judge = new User();
        judge.setId(userId);
        
        User user1 = new User();
        user1.setId(2L);
        
        User user2 = new User();
        user2.setId(3L);
        
        Story story = new Story();
        story.setWinner(user1);
        story.setLoser(user2);
        story.setJudges(Arrays.asList(judge));
        
        List<Story> allStories = Arrays.asList(story);
        
        given(userRepository.findById(userId)).willReturn(Optional.of(judge));
        given(storyRepository.findHistoryForUser(any())).willReturn(allStories);

        List<StoryGetDTO> result = userService.findAllStoriesOfUser(userId);
        
        assertEquals(1, result.size());
    }


    @Test
    public void findAllStoriesOfUser_userHasNone_returnsEmptyList() {
        long userId = 1L;

        User user1 = new User(); user1.setId(2L);
        User user2 = new User(); user2.setId(3L);
        User user3 = new User(); user3.setId(4L);

        Story story = new Story();
        story.setWinner(user1);
        story.setLoser(user2);
        story.setJudges(Arrays.asList(user3));

        User mainUser = new User();
        mainUser.setId(userId);
        given(userRepository.findById(userId)).willReturn(Optional.of(mainUser));
        given(storyRepository.findHistoryForUser(any())).willReturn(new ArrayList<>());

        List<StoryGetDTO> result = userService.findAllStoriesOfUser(userId);

        assertEquals(0, result.size());
    }

    @Test
    public void findAllStoriesOfUser_noStories_returnsEmptyList() {
    
        long userId = 1L;
        
        User mainUser = new User();
        mainUser.setId(userId);
        given(userRepository.findById(userId)).willReturn(Optional.of(mainUser));
        given(storyRepository.findHistoryForUser(any())).willReturn(new ArrayList<>());
        
   
        List<StoryGetDTO> result = userService.findAllStoriesOfUser(userId);

        assertEquals(0, result.size());
    }

    @Test
    public void findAllStoriesOfUser_multipleJudges_userIsOneOfThem_returnsStory() {
 
        long userId = 1L;
        
        User judge1 = new User();
        judge1.setId(userId);
        
        User judge2 = new User();
        judge2.setId(2L);
        
        User user1 = new User();
        user1.setId(3L);
        
        User user2 = new User();
        user2.setId(4L);
        
        Story story = new Story();
        story.setWinner(user1);
        story.setLoser(user2);
        story.setJudges(Arrays.asList(judge1, judge2));
        
        List<Story> allStories = Arrays.asList(story);
        
        given(userRepository.findById(userId)).willReturn(Optional.of(judge1));
        given(storyRepository.findHistoryForUser(any())).willReturn(allStories);

        List<StoryGetDTO> result = userService.findAllStoriesOfUser(userId);
        
        assertEquals(1, result.size());
    }

    @Test
    public void isAJudge_userIsJudge_returnsTrue() {
        
        long userId = 1L;
        
        User judge = new User();
        judge.setId(userId);
        
        Story story = new Story();
        story.setJudges(Arrays.asList(judge));
        

        Boolean result = userService.isAJudge(story, userId);

        assertTrue(result);
    }

    @Test
    public void isAJudge_userIsNotJudge_returnsFalse() {
        
        long userId = 1L;
        
        User judge = new User();
        judge.setId(2L);
        
        Story story = new Story();
        story.setJudges(Arrays.asList(judge));
        
        
        Boolean result = userService.isAJudge(story, userId);
        
        
        assertFalse(result);
    }


    @Test
    public void isAJudge_multipleJudges_userIsSecondJudge_returnsTrue() {

        long userId = 1L;
        
        User judge1 = new User();
        judge1.setId(2L);
        
        User judge2 = new User();
        judge2.setId(userId);
        
        User judge3 = new User();
        judge3.setId(3L);
        
        Story story = new Story();
        story.setJudges(Arrays.asList(judge1, judge2, judge3));
        
  
        Boolean result = userService.isAJudge(story, userId);
  
        assertTrue(result);
    }

    // --- LEADERBOARD (getLeaderboard) ---

    @Test
    public void getLeaderboard_noGenre_returnsSortedByTotalWins() {
        UserStatistics stats1 = new UserStatistics();
        stats1.setGamesWon(3);

        UserStatistics stats2 = new UserStatistics();
        stats2.setGamesWon(7);

        User user1 = new User(); user1.setId(2L); user1.setUsername("alice"); user1.setStatistics(stats1);
        User user2 = new User(); user2.setId(3L); user2.setUsername("bob");   user2.setStatistics(stats2);

        Mockito.when(userRepository.findByToken("some-token")).thenReturn(testUser);
        Mockito.when(userRepository.findAll()).thenReturn(Arrays.asList(user1, user2));

        List<LeaderboardEntryGetDTO> result = userService.getLeaderboard(null, 10, "Bearer some-token");

        assertEquals(2, result.size());
        assertEquals("bob", result.get(0).getUsername());
        assertEquals(7, result.get(0).getScore());
        assertEquals("alice", result.get(1).getUsername());
    }

    @Test
    public void getLeaderboard_withGenre_returnsSortedByGenreWins() {
        UserStatistics stats1 = new UserStatistics();
        stats1.getWinsByGenre().put("Horror", 5);

        UserStatistics stats2 = new UserStatistics();
        stats2.getWinsByGenre().put("Horror", 2);

        User user1 = new User(); user1.setId(2L); user1.setUsername("horrorKing"); user1.setStatistics(stats1);
        User user2 = new User(); user2.setId(3L); user2.setUsername("horrorFan");  user2.setStatistics(stats2);

        Mockito.when(userRepository.findByToken("some-token")).thenReturn(testUser);
        Mockito.when(userRepository.findAll()).thenReturn(Arrays.asList(user1, user2));

        List<LeaderboardEntryGetDTO> result = userService.getLeaderboard("Horror", 10, "Bearer some-token");

        assertEquals(2, result.size());
        assertEquals("horrorKing", result.get(0).getUsername());
        assertEquals(5, result.get(0).getScore());
    }

    @Test
    public void getLeaderboard_zeroWinsExcluded_notInResult() {
        UserStatistics statsWithWins = new UserStatistics();
        statsWithWins.setGamesWon(3);

        UserStatistics statsNoWins = new UserStatistics();
        statsNoWins.setGamesWon(0);

        User winner = new User(); winner.setId(2L); winner.setUsername("winner"); winner.setStatistics(statsWithWins);
        User loser  = new User(); loser.setId(3L);  loser.setUsername("loser");   loser.setStatistics(statsNoWins);

        Mockito.when(userRepository.findByToken("some-token")).thenReturn(testUser);
        Mockito.when(userRepository.findAll()).thenReturn(Arrays.asList(winner, loser));

        List<LeaderboardEntryGetDTO> result = userService.getLeaderboard(null, 10, "Bearer some-token");

        assertEquals(1, result.size());
        assertEquals("winner", result.get(0).getUsername());
    }

    @Test
    public void getLeaderboard_nullStatisticsUser_skippedInResult() {
        UserStatistics stats = new UserStatistics();
        stats.setGamesWon(4);

        User withStats    = new User(); withStats.setId(2L);    withStats.setUsername("hasStats");  withStats.setStatistics(stats);
        User withoutStats = new User(); withoutStats.setId(3L); withoutStats.setUsername("noStats"); withoutStats.setStatistics(null);

        Mockito.when(userRepository.findByToken("some-token")).thenReturn(testUser);
        Mockito.when(userRepository.findAll()).thenReturn(Arrays.asList(withStats, withoutStats));

        List<LeaderboardEntryGetDTO> result = userService.getLeaderboard(null, 10, "Bearer some-token");

        assertEquals(1, result.size());
        assertEquals("hasStats", result.get(0).getUsername());
    }

    @Test
    public void getLeaderboard_limitApplied_returnsOnlyTopN() {
        List<User> users = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            UserStatistics s = new UserStatistics();
            s.setGamesWon(i);
            User u = new User();
            u.setId((long) i + 10);
            u.setUsername("user" + i);
            u.setStatistics(s);
            users.add(u);
        }

        Mockito.when(userRepository.findByToken("some-token")).thenReturn(testUser);
        Mockito.when(userRepository.findAll()).thenReturn(users);

        List<LeaderboardEntryGetDTO> result = userService.getLeaderboard(null, 3, "Bearer some-token");

        assertEquals(3, result.size());
        assertEquals(5, result.get(0).getScore());
    }

    @Test
    public void getLeaderboard_invalidToken_401Unauthorized() {
        Mockito.when(userRepository.findByToken(anyString())).thenReturn(null);

        assertThrows(ResponseStatusException.class,
                () -> userService.getLeaderboard(null, 10, "Bearer invalid-token"));
    }

    @Test
    public void getLeaderboard_blankGenre_treatedAsOverall() {
        UserStatistics stats = new UserStatistics();
        stats.setGamesWon(2);

        User user = new User(); user.setId(2L); user.setUsername("player"); user.setStatistics(stats);

        Mockito.when(userRepository.findByToken("some-token")).thenReturn(testUser);
        Mockito.when(userRepository.findAll()).thenReturn(Collections.singletonList(user));

        List<LeaderboardEntryGetDTO> result = userService.getLeaderboard("   ", 10, "Bearer some-token");

        assertEquals(1, result.size());
        assertEquals(2, result.get(0).getScore());
    }

    // --- USER STATISTICS (getUserStatistics) ---

    @Test
    public void getUserStatistics_validId_success() {
        ch.uzh.ifi.hase.soprafs26.entity.UserStatistics stats = new ch.uzh.ifi.hase.soprafs26.entity.UserStatistics();
        stats.setGamesPlayed(5);
        testUser.setStatistics(stats);

        Mockito.when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        Mockito.when(userRepository.findByToken(anyString())).thenReturn(testUser);

        ch.uzh.ifi.hase.soprafs26.entity.UserStatistics result = userService.getUserStatistics(1L, "Bearer some-token");

        assertNotNull(result);
        assertEquals(5, result.getGamesPlayed());
    }

    @Test
    public void getUserStatistics_userNotFound_throws404() {
        Mockito.when(userRepository.findByToken(anyString())).thenReturn(testUser);
        Mockito.when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> userService.getUserStatistics(99L, "Bearer token"));
    }

    @Test
    public void getUserStatistics_statsNull_404NotFound() {
        testUser.setStatistics(null);

        Mockito.when(userRepository.findByToken(anyString())).thenReturn(testUser);
        Mockito.when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> userService.getUserStatistics(testUser.getId(), "Bearer some-token"));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("Statistics for this user were not found.", exception.getReason());
    }

    // --- GENRE MASTER CLEANUP (Tested via deleteUser) ---

    @Test
    public void deleteUser_userIsMasterWithSuccessor_promotesSuccessor() {
        UserDeleteDTO dto = new UserDeleteDTO();
        dto.setPassword("password123");

        User successor = new User();
        successor.setId(2L);
        successor.setUsername("nextInLine");

        GenreMaster gm = new GenreMaster();
        gm.setGenre("Horror");
        gm.setCurrentMaster(testUser);
        Map<Long, Integer> votes = new HashMap<>();
        votes.put(testUser.getId(), 100);
        votes.put(successor.getId(), 50);
        gm.setVotes(votes);

        Mockito.when(userRepository.findByToken(anyString())).thenReturn(testUser);
        Mockito.when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        Mockito.when(userRepository.findById(successor.getId())).thenReturn(Optional.of(successor));
        Mockito.when(genreMasterRepository.findAll()).thenReturn(Collections.singletonList(gm));

        userService.deleteUser(testUser.getId(), dto, "Bearer token");

        assertFalse(gm.getVotes().containsKey(testUser.getId()));
        assertEquals(successor, gm.getCurrentMaster());
        Mockito.verify(genreMasterRepository).save(gm);
    }

    @Test
    public void deleteUser_userIsMasterNoSuccessor_clearsMaster() {
        UserDeleteDTO dto = new UserDeleteDTO();
        dto.setPassword("password123");

        GenreMaster gm = new GenreMaster();
        gm.setCurrentMaster(testUser);
        Map<Long, Integer> votes = new HashMap<>();
        votes.put(testUser.getId(), 10);
        gm.setVotes(votes);

        Mockito.when(userRepository.findByToken(anyString())).thenReturn(testUser);
        Mockito.when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        Mockito.when(genreMasterRepository.findAll()).thenReturn(Collections.singletonList(gm));

        userService.deleteUser(testUser.getId(), dto, "Bearer token");

        assertTrue(gm.getVotes().isEmpty());
        assertNull(gm.getCurrentMaster());
    }

    @Test
    public void deleteUser_userInLeaderboardNotMaster_removesUserOnly() {
        UserDeleteDTO dto = new UserDeleteDTO();
        dto.setPassword("password123");

        User actualKing = new User();
        actualKing.setId(99L);

        GenreMaster gm = new GenreMaster();
        gm.setCurrentMaster(actualKing);
        Map<Long, Integer> votes = new HashMap<>();
        votes.put(actualKing.getId(), 50);
        votes.put(testUser.getId(), 10);
        gm.setVotes(votes);

        Mockito.when(userRepository.findByToken(anyString())).thenReturn(testUser);
        Mockito.when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        Mockito.when(genreMasterRepository.findAll()).thenReturn(Collections.singletonList(gm));

        userService.deleteUser(testUser.getId(), dto, "Bearer token");

        assertFalse(gm.getVotes().containsKey(testUser.getId()));
        assertEquals(actualKing, gm.getCurrentMaster());
    }
}