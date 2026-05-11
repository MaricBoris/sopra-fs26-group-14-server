package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.entity.Story;
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
        testStory.setStoryText("Some story text.");
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
}