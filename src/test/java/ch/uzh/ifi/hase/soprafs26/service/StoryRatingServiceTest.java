package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.entity.Story;
import ch.uzh.ifi.hase.soprafs26.entity.StoryRating;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.StoryRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import ch.uzh.ifi.hase.soprafs26.entity.StoryContribution;
import ch.uzh.ifi.hase.soprafs26.repository.StoryRatingRepository;
import java.util.List;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;

public class StoryRatingServiceTest {

    @Mock
    private StoryRepository storyRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserService userService;

    @Mock
    private StoryRatingRepository storyRatingRepository;

    @InjectMocks
    private StoryRatingService storyRatingService;

    private User testUser;
    private Story testStory;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testUser");
        testUser.setToken("valid-token");

        testStory = new Story();
        testStory.setId(1L);
        testStory.setStoryContributions(List.of(new StoryContribution(1L, "Some story text.")));
        testStory.setTitle(null);

        Mockito.when(userService.extractToken("Bearer valid-token")).thenReturn("valid-token");
        Mockito.when(userService.findUserFromToken("valid-token")).thenReturn(testUser);

        Mockito.when(userService.extractToken(null))
                .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token"));
        Mockito.when(userService.extractToken("Basic valid-token"))
                .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token"));
        Mockito.when(userService.extractToken("Bearer invalid-token")).thenReturn("invalid-token");
        Mockito.when(userService.findUserFromToken("invalid-token"))
                .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token"));

        Mockito.when(storyRepository.findById(1L)).thenReturn(Optional.of(testStory));
        Mockito.when(storyRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    public void changeTitle_validInput_titleUpdated() {
        Story result = storyRatingService.changeTitle(1L, "Bearer valid-token", "My New Title");

        assertEquals("My New Title", result.getTitle());
        Mockito.verify(storyRepository).save(testStory);
        Mockito.verify(storyRepository).flush();
    }

    @Test
    public void changeTitle_storyNotFound_404NotFound() {
        Mockito.when(storyRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () ->
                storyRatingService.changeTitle(99L, "Bearer valid-token", "Title"));
    }

    @Test
    public void changeTitle_invalidToken_401Unauthorized() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                storyRatingService.changeTitle(1L, "Bearer invalid-token", "Title"));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    public void changeTitle_nullToken_401Unauthorized() {
        assertThrows(ResponseStatusException.class, () ->
                storyRatingService.changeTitle(1L, null, "Title"));
    }

    @Test
    public void changeTitle_noBearerPrefix_401Unauthorized() {
        assertThrows(ResponseStatusException.class, () ->
                storyRatingService.changeTitle(1L, "Basic valid-token", "Title"));
    }

    @Test
    public void changeTitle_titleOverwritten_newTitlePersisted() {
        testStory.setTitle("Old Title");

        Story result = storyRatingService.changeTitle(1L, "Bearer valid-token", "Updated Title");

        assertEquals("Updated Title", result.getTitle());
    }

    @Test
    public void changeTitle_emptyTitle_stillSaves() {
        Story result = storyRatingService.changeTitle(1L, "Bearer valid-token", "");

        assertEquals("", result.getTitle());
        Mockito.verify(storyRepository).save(testStory);
    }

    // --- getCurrentUser ---

    @Test
    public void getCurrentUser_validToken_returnsUser() {
        User result = storyRatingService.getCurrentUser("Bearer valid-token");
        assertEquals(testUser, result);
    }

    // --- countVotesFor ---

    @Test
    public void countVotesFor_nullPlayer_returnsZero() {
        long count = storyRatingService.countVotesFor(testStory, null);
        assertEquals(0L, count);
    }

    @Test
    public void countVotesFor_validPlayer_returnsCount() {
        Mockito.when(storyRatingRepository.countByStoryAndVotedFor(testStory, testUser)).thenReturn(5L);
        long count = storyRatingService.countVotesFor(testStory, testUser);
        assertEquals(5L, count);
    }

    // --- findOwnRating ---

    @Test
    public void findOwnRating_returnsRating() {
        StoryRating mockRating = new StoryRating();
        Mockito.when(storyRatingRepository.findByStoryAndVoter(testStory, testUser)).thenReturn(mockRating);

        StoryRating result = storyRatingService.findOwnRating(testStory, testUser);
        assertEquals(mockRating, result);
    }

    // --- hasParticipated ---

    @Test
    public void hasParticipated_nullUserOrId_returnsFalse() {
        assertFalse(storyRatingService.hasParticipated(testStory, null));
        assertFalse(storyRatingService.hasParticipated(testStory, new User()));
    }

    @Test
    public void hasParticipated_userIsWinner_returnsTrue() {
        testStory.setWinner(testUser);
        assertTrue(storyRatingService.hasParticipated(testStory, testUser));
    }

    @Test
    public void hasParticipated_userIsLoser_returnsTrue() {
        testStory.setLoser(testUser);
        assertTrue(storyRatingService.hasParticipated(testStory, testUser));
    }

    @Test
    public void hasParticipated_userIsJudge_returnsTrue() {
        testStory.setJudges(List.of(testUser));
        assertTrue(storyRatingService.hasParticipated(testStory, testUser));
    }

    @Test
    public void hasParticipated_userNotParticipant_returnsFalse() {
        User otherUser = new User();
        otherUser.setId(99L);
        testStory.setWinner(otherUser);
        testStory.setLoser(otherUser);
        testStory.setJudges(List.of(otherUser));

        assertFalse(storyRatingService.hasParticipated(testStory, testUser));
    }

    // --- rateGenre & pickPlayer ---

    @Test
    public void rateGenre_storyNotFound_throws404() {
        Mockito.when(storyRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () ->
                storyRatingService.rateGenre(99L, 2L, "Bearer valid-token"));
    }

    @Test
    public void rateGenre_votedForUserIdNull_throws400() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                storyRatingService.rateGenre(1L, null, "Bearer valid-token"));
        assertEquals("There must be a voted for user", ex.getReason());
    }

    @Test
    public void rateGenre_votedForUserNotParticipant_throws400() {
        // No winner/loser set in testStory, so pickPlayer returns null
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                storyRatingService.rateGenre(1L, 99L, "Bearer valid-token"));
        assertEquals("The voted for user must be one of the two writers of the story", ex.getReason());
    }

    @Test
    public void rateGenre_voterParticipated_throws403() {
        // Make the voting user the winner (participant)
        testStory.setWinner(testUser);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                storyRatingService.rateGenre(1L, testUser.getId(), "Bearer valid-token"));
        assertEquals("You cannot vote on a story you participated in", ex.getReason());
    }

    @Test
    public void rateGenre_newRating_success() {
        User winner = new User(); winner.setId(2L);
        User loser = new User(); loser.setId(3L);
        testStory.setWinner(winner);
        testStory.setLoser(loser);

        Mockito.when(storyRatingRepository.findByStoryAndVoter(testStory, testUser)).thenReturn(null);
        Mockito.when(storyRatingRepository.save(Mockito.any())).thenAnswer(i -> i.getArgument(0));

        StoryRating result = storyRatingService.rateGenre(1L, 2L, "Bearer valid-token");

        assertNotNull(result);
        assertEquals(testUser, result.getVoter());
        assertEquals(winner, result.getVotedFor()); // Verifies pickPlayer(winner)
        Mockito.verify(storyRatingRepository).save(Mockito.any());
        Mockito.verify(storyRatingRepository).flush();
    }

    @Test
    public void rateGenre_existingRating_updatesAndSaves() {
        User winner = new User(); winner.setId(2L);
        User loser = new User(); loser.setId(3L);
        testStory.setWinner(winner);
        testStory.setLoser(loser);

        StoryRating existingRating = new StoryRating();
        existingRating.setVotedFor(loser); // Originally voted for loser

        Mockito.when(storyRatingRepository.findByStoryAndVoter(testStory, testUser)).thenReturn(existingRating);
        Mockito.when(storyRatingRepository.save(Mockito.any())).thenAnswer(i -> i.getArgument(0));

        StoryRating result = storyRatingService.rateGenre(1L, 2L, "Bearer valid-token"); // Changing vote to winner

        assertNotNull(result);
        assertEquals(winner, result.getVotedFor());
        assertNotNull(result.getTimestamp());
        Mockito.verify(storyRatingRepository).save(existingRating);
    }

    @Test
    public void rateGenre_pickLoser_success() {
        User winner = new User(); winner.setId(2L);
        User loser = new User(); loser.setId(3L);
        testStory.setWinner(winner);
        testStory.setLoser(loser);

        Mockito.when(storyRatingRepository.findByStoryAndVoter(testStory, testUser)).thenReturn(null);
        Mockito.when(storyRatingRepository.save(Mockito.any())).thenAnswer(i -> i.getArgument(0));

        StoryRating result = storyRatingService.rateGenre(1L, 3L, "Bearer valid-token"); // Voting for loser

        assertNotNull(result);
        assertEquals(loser, result.getVotedFor()); // Verifies pickPlayer(loser)
    }
}