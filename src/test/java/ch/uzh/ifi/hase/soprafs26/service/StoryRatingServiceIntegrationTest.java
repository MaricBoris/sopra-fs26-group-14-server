package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.entity.Story;
import ch.uzh.ifi.hase.soprafs26.entity.StoryRating;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.StoryRatingRepository;
import ch.uzh.ifi.hase.soprafs26.repository.StoryRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs26.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ch.uzh.ifi.hase.soprafs26.entity.StoryContribution;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@WebAppConfiguration
@SpringBootTest
@Transactional
public class StoryRatingServiceIntegrationTest {

    @Qualifier("storyRepository")
    @Autowired
    private StoryRepository storyRepository;

    @Qualifier("userRepository")
    @Autowired
    private UserRepository userRepository;

    @Qualifier("roomRepository")
    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private StoryRatingService storyRatingService;

    @Autowired
    private UserService userService;

    @Autowired
    private StoryRatingRepository storyRatingRepository;

    private User testUser;
    private Story testStory;

    @BeforeEach
    public void setup() {
        roomRepository.deleteAll();
        storyRepository.deleteAll();
        userRepository.deleteAll();

        testUser = new User();
        testUser.setUsername("testUser");
        testUser.setPassword("password");
        testUser = userService.createUser(testUser);

        testStory = new Story();
        testStory.setStoryContributions(List.of(new StoryContribution(1L, "Some story text.")));
        testStory = storyRepository.save(testStory);
        storyRepository.flush();
    }

    // --- Helpers ---

    private User createNamedUser(String username) {
        User u = new User();
        u.setUsername(username);
        u.setPassword("password");
        return userService.createUser(u);
    }

    @Test
    public void changeTitle_validInput_titleUpdatedInDb() {
        Story result = storyRatingService.changeTitle(
                testStory.getId(),
                "Bearer " + testUser.getToken(),
                "My Integration Title"
        );

        assertNotNull(result);
        assertEquals("My Integration Title", result.getTitle());

        Story reloaded = storyRepository.findById(testStory.getId()).orElse(null);
        assertNotNull(reloaded);
        assertEquals("My Integration Title", reloaded.getTitle());
    }

    @Test
    public void changeTitle_storyNotFound_404NotFound() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                storyRatingService.changeTitle(
                        999L,
                        "Bearer " + testUser.getToken(),
                        "Some Title"
                ));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    public void changeTitle_invalidToken_401Unauthorized() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                storyRatingService.changeTitle(
                        testStory.getId(),
                        "Bearer fake-token",
                        "Some Title"
                ));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    public void changeTitle_nullToken_401Unauthorized() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                storyRatingService.changeTitle(
                        testStory.getId(),
                        null,
                        "Some Title"
                ));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    public void changeTitle_noBearerPrefix_401Unauthorized() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                storyRatingService.changeTitle(
                        testStory.getId(),
                        "Basic " + testUser.getToken(),
                        "Some Title"
                ));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    public void changeTitle_overwritesExistingTitle() {
        testStory.setTitle("Old Title");
        storyRepository.save(testStory);

        Story result = storyRatingService.changeTitle(
                testStory.getId(),
                "Bearer " + testUser.getToken(),
                "New Title"
        );

        assertEquals("New Title", result.getTitle());

        Story reloaded = storyRepository.findById(testStory.getId()).orElse(null);
        assertNotNull(reloaded);
        assertEquals("New Title", reloaded.getTitle());
    }

    @Test
    public void changeTitle_emptyTitle_persistsEmptyString() {
        Story result = storyRatingService.changeTitle(
                testStory.getId(),
                "Bearer " + testUser.getToken(),
                ""
        );

        assertEquals("", result.getTitle());

        Story reloaded = storyRepository.findById(testStory.getId()).orElse(null);
        assertNotNull(reloaded);
        assertEquals("", reloaded.getTitle());
    }

    // --- Genre Rating Integration Tests ---

    @Test
    public void rateGenre_newRating_persistsInDb() {
        User writer1 = createNamedUser("writer1");
        User writer2 = createNamedUser("writer2");
        User voter = createNamedUser("voter1");

        testStory.setWinner(writer1);
        testStory.setLoser(writer2);
        storyRepository.saveAndFlush(testStory);

        StoryRating rating = storyRatingService.rateGenre(
                testStory.getId(),
                writer1.getId(),
                "Bearer " + voter.getToken()
        );

        // Verify returned object
        assertNotNull(rating.getId());
        assertEquals(voter.getId(), rating.getVoter().getId());
        assertEquals(writer1.getId(), rating.getVotedFor().getId());

        // Verify DB persistence
        StoryRating fromDb = storyRatingRepository.findByStoryAndVoter(testStory, voter);
        assertNotNull(fromDb);
        assertEquals(writer1.getId(), fromDb.getVotedFor().getId());

        // Verify vote counting works with DB
        assertEquals(1L, storyRatingService.countVotesFor(testStory, writer1));
        assertEquals(0L, storyRatingService.countVotesFor(testStory, writer2));
    }

    @Test
    public void rateGenre_updateExistingRating_updatesDbCorrectly() {
        User writer1 = createNamedUser("w1");
        User writer2 = createNamedUser("w2");
        User voter = createNamedUser("v1");

        testStory.setWinner(writer1);
        testStory.setLoser(writer2);
        storyRepository.saveAndFlush(testStory);

        // First vote for writer 1
        storyRatingService.rateGenre(testStory.getId(), writer1.getId(), "Bearer " + voter.getToken());

        // Change vote to writer 2
        StoryRating updatedRating = storyRatingService.rateGenre(testStory.getId(), writer2.getId(), "Bearer " + voter.getToken());

        assertEquals(writer2.getId(), updatedRating.getVotedFor().getId());

        // Verify DB only has 1 vote for this user, and it moved to writer 2
        long countWriter1 = storyRatingService.countVotesFor(testStory, writer1);
        long countWriter2 = storyRatingService.countVotesFor(testStory, writer2);

        assertEquals(0L, countWriter1);
        assertEquals(1L, countWriter2);
    }

    @Test
    public void rateGenre_voterParticipatedAsWriter_throws403() {
        User writer1 = createNamedUser("writer_participant1");
        User writer2 = createNamedUser("writer_participant2");

        testStory.setWinner(writer1);
        testStory.setLoser(writer2);
        storyRepository.saveAndFlush(testStory);

        // writer1 tries to vote on their own story
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                storyRatingService.rateGenre(
                        testStory.getId(),
                        writer2.getId(),
                        "Bearer " + writer1.getToken()
                ));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertEquals("You cannot vote on a story you participated in", ex.getReason());
    }

    @Test
    public void rateGenre_voterParticipatedAsJudge_throws403() {
        User writer1 = createNamedUser("w_one");
        User writer2 = createNamedUser("w_two");
        User judge = createNamedUser("j_one");

        testStory.setWinner(writer1);
        testStory.setLoser(writer2);

        testStory.setJudges(new java.util.ArrayList<>(List.of(judge)));

        storyRepository.saveAndFlush(testStory);

        // judge tries to post-game rate the story
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                storyRatingService.rateGenre(
                        testStory.getId(),
                        writer1.getId(),
                        "Bearer " + judge.getToken()
                ));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertEquals("You cannot vote on a story you participated in", ex.getReason());
    }

    @Test
    public void rateGenre_votedForUserNotAWriter_throws400() {
        User writer1 = createNamedUser("actual_writer1");
        User writer2 = createNamedUser("actual_writer2");
        User voter = createNamedUser("innocent_voter");
        User randomUser = createNamedUser("random_dude");

        testStory.setWinner(writer1);
        testStory.setLoser(writer2);
        storyRepository.saveAndFlush(testStory);

        // Voter tries to vote for a random user not attached to the story
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                storyRatingService.rateGenre(
                        testStory.getId(),
                        randomUser.getId(),
                        "Bearer " + voter.getToken()
                ));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("The voted for user must be one of the two writers of the story", ex.getReason());
    }

    @Test
    public void rateGenre_votedForUserIdNull_throws400() {
        User voter = createNamedUser("voter_null_test");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                storyRatingService.rateGenre(
                        testStory.getId(),
                        null,
                        "Bearer " + voter.getToken()
                ));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("There must be a voted for user", ex.getReason());
    }
}