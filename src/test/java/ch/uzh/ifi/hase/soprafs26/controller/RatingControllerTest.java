package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.entity.Story;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs26.service.StoryRatingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.entity.StoryContribution;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RatingController.class)
public class RatingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StoryRatingService storyRatingService;

    private Story createTestStory(Long id, String title) {
        Story story = new Story();
        story.setId(id);
        story.setTitle(title);
        story.setStoryContributions(List.of(new StoryContribution(1L, "Some story text.")));
        story.setHasWinner(false);
        return story;
    }

    // PUT /stories/{storyId}/title, 200 OK

    @Test
    public void changeTitle_validInput_200Ok() throws Exception {
        Story story = createTestStory(1L, "A Brand New Title");

        given(storyRatingService.changeTitle(anyLong(), anyString(), anyString()))
                .willReturn(story);

        MockHttpServletRequestBuilder putRequest = put("/stories/1/title")
                .header("Authorization", "Bearer token123")
                .contentType(MediaType.APPLICATION_JSON)
                .content("\"A Brand New Title\"");

        mockMvc.perform(putRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.title", is("A Brand New Title")));
    }

    // PUT /stories/{storyId}/title, 401 Unauthorized

    @Test
    public void changeTitle_invalidToken_401Unauthorized() throws Exception {
        given(storyRatingService.changeTitle(anyLong(), anyString(), anyString()))
                .willThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token"));

        MockHttpServletRequestBuilder putRequest = put("/stories/1/title")
                .header("Authorization", "Bearer wrong-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("\"Some Title\"");

        mockMvc.perform(putRequest)
                .andExpect(status().isUnauthorized());
    }

    // PUT /stories/{storyId}/title, 403 Forbidden

    @Test
    public void changeTitle_notJudge_403Forbidden() throws Exception {
        given(storyRatingService.changeTitle(anyLong(), anyString(), anyString()))
                .willThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the judge can set the title"));

        MockHttpServletRequestBuilder putRequest = put("/stories/1/title")
                .header("Authorization", "Bearer writer-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("\"Some Title\"");

        mockMvc.perform(putRequest)
                .andExpect(status().isForbidden());
    }

    // PUT /stories/{storyId}/title, 404 Not Found

    @Test
    public void changeTitle_storyNotFound_404NotFound() throws Exception {
        given(storyRatingService.changeTitle(anyLong(), anyString(), anyString()))
                .willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Story not found"));

        MockHttpServletRequestBuilder putRequest = put("/stories/999/title")
                .header("Authorization", "Bearer token123")
                .contentType(MediaType.APPLICATION_JSON)
                .content("\"Some Title\"");

        mockMvc.perform(putRequest)
                .andExpect(status().isNotFound());
    }

    // PUT /stories/{storyId}/title, empty title

    @Test
    public void changeTitle_emptyTitle_400BadRequest() throws Exception {
        given(storyRatingService.changeTitle(anyLong(), anyString(), anyString()))
                .willThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Title cannot be empty"));

        MockHttpServletRequestBuilder putRequest = put("/stories/1/title")
                .header("Authorization", "Bearer token123")
                .contentType(MediaType.APPLICATION_JSON)
                .content("\"\"");

        mockMvc.perform(putRequest)
                .andExpect(status().isBadRequest());
    }
}