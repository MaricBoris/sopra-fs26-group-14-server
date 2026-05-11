package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.entity.Story;
import ch.uzh.ifi.hase.soprafs26.entity.User;
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
        testStory.setStoryText("Some story text.");
        testStory = storyRepository.save(testStory);
        storyRepository.flush();
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
}