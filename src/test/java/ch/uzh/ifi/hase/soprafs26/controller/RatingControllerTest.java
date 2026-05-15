package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.entity.Story;
import ch.uzh.ifi.hase.soprafs26.entity.StoryContribution;
import ch.uzh.ifi.hase.soprafs26.entity.StoryRating;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.storyRating.GenreRatingPostDTO;
import ch.uzh.ifi.hase.soprafs26.service.StoryRatingService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RatingController.class)
public class RatingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StoryRatingService storyRatingService;

    // --- Helpers ---

    private Story createTestStory(Long id, String title) {
        Story story = new Story();
        story.setId(id);
        story.setTitle(title);
        story.setStoryContributions(List.of(new StoryContribution(1L, "Some story text.")));
        story.setHasWinner(false);
        return story;
    }

    private Story createFullyPopulatedStory(Long id) {
        User winner = new User();
        winner.setId(1L);
        winner.setUsername("winnerUser");

        User loser = new User();
        loser.setId(2L);
        loser.setUsername("loserUser");

        Story story = new Story();
        story.setId(id);
        story.setWinner(winner);
        story.setLoser(loser);
        story.setWinGenre("Horror");
        story.setLoseGenre("Sci-Fi");
        return story;
    }

    private String asJsonString(final Object object) {
        try {
            return new ObjectMapper().writeValueAsString(object);
        } catch (JacksonException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format("The request body could not be created.%s", e.toString()));
        }
    }

    // --- PUT /stories/{storyId}/title ---

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

    // --- GET /stories/{storyId}/genre-rating ---

    @Test
    public void getGenreRating_validRequest_200Ok() throws Exception {
        Story story = createFullyPopulatedStory(100L);
        User voter = new User();
        voter.setId(3L);

        given(storyRatingService.getStory(anyLong(), anyString())).willReturn(story);
        given(storyRatingService.getCurrentUser(anyString())).willReturn(voter);
        given(storyRatingService.countVotesFor(any(), any())).willReturn(5L);
        given(storyRatingService.hasParticipated(any(), any())).willReturn(false);

        MockHttpServletRequestBuilder getRequest = get("/stories/100/genre-rating")
                .header("Authorization", "Bearer token123");

        mockMvc.perform(getRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.winnerUsername", is("winnerUser")))
                .andExpect(jsonPath("$.winnerVotes", is(5)))
                .andExpect(jsonPath("$.loserGenre", is("Sci-Fi")))
                .andExpect(jsonPath("$.canVote", is(true)));
    }

    @Test
    public void getGenreRating_alreadyVoted_returnsPreviousVoteId() throws Exception {
        Story story = createFullyPopulatedStory(100L);
        User voter = new User();

        StoryRating rating = new StoryRating();
        rating.setVotedFor(story.getWinner());

        given(storyRatingService.getStory(anyLong(), anyString())).willReturn(story);
        given(storyRatingService.getCurrentUser(anyString())).willReturn(voter);
        given(storyRatingService.findOwnRating(any(), any())).willReturn(rating);

        MockHttpServletRequestBuilder getRequest = get("/stories/100/genre-rating")
                .header("Authorization", "Bearer token123");

        mockMvc.perform(getRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userVotedForId", is(1)));
    }

    // --- POST /stories/{storyId}/genre-rating ---

    @Test
    public void rateGenre_validInput_200Ok() throws Exception {
        GenreRatingPostDTO postDTO = new GenreRatingPostDTO();
        postDTO.setVotedForUserId(1L);

        Story story = createFullyPopulatedStory(100L);
        User voter = new User();

        // FIX: Using given().willReturn() because rateGenre is likely not a void method
        given(storyRatingService.rateGenre(anyLong(), anyLong(), anyString())).willReturn(new StoryRating());
        given(storyRatingService.getStory(anyLong(), anyString())).willReturn(story);
        given(storyRatingService.getCurrentUser(anyString())).willReturn(voter);

        MockHttpServletRequestBuilder postRequest = post("/stories/100/genre-rating")
                .header("Authorization", "Bearer token123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(postDTO));

        mockMvc.perform(postRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.winnerUserId", is(1)));

        Mockito.verify(storyRatingService).rateGenre(eq(100L), eq(1L), eq("Bearer token123"));
    }

    @Test
    public void rateGenre_forbidden_403Forbidden() throws Exception {
        GenreRatingPostDTO postDTO = new GenreRatingPostDTO();
        postDTO.setVotedForUserId(1L);

        Mockito.doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Participants cannot vote"))
                .when(storyRatingService).rateGenre(anyLong(), anyLong(), anyString());

        MockHttpServletRequestBuilder postRequest = post("/stories/100/genre-rating")
                .header("Authorization", "Bearer token123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(postDTO));

        mockMvc.perform(postRequest)
                .andExpect(status().isForbidden());
    }
}