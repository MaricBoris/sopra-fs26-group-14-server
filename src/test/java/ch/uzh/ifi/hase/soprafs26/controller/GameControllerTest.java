package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.entity.*;
import ch.uzh.ifi.hase.soprafs26.service.GameService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GameController.class)
public class GameControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GameService gameService;

    @Test
    public void vote_validInput_200Ok() throws Exception {
        Game game = new Game();
        game.setId(1L);
        Story story = new Story();
        game.setStory(story);

        Writer writer = new Writer();
        writer.setId(10L);

        given(gameService.getGame(anyLong())).willReturn(game);
        given(gameService.findWriterFromId(anyLong(), any(Game.class))).willReturn(writer);
        given(gameService.findUserFromToken(anyString())).willReturn(new User());
        given(gameService.getJudgeFromUser(any(), any())).willReturn(new Judge());
        doNothing().when(gameService).checkGameIsOver(any());
        doNothing().when(gameService).addVote(any(), any(), any());
        given(gameService.allJudgesVoted(any())).willReturn(true);
        given(gameService.determineWinner(any())).willReturn(writer);
        given(gameService.updateStory(any(), any())).willReturn(new Story());
        doNothing().when(gameService).updateHistory(any());
        doNothing().when(gameService).clearVotes(any());
        doNothing().when(gameService).cleanupGame(any());

        // Send a Long writerId as the request body (matching @RequestBody Long writerId)
        MockHttpServletRequestBuilder postRequest = post("/games/1/vote")
                .header("Authorization", "Bearer token123")
                .contentType(MediaType.APPLICATION_JSON)
                .content("10");

        mockMvc.perform(postRequest)
                .andExpect(status().isOk());
    }

    @Test
    public void vote_invalidToken_401Unauthorized() throws Exception {
        Game game = new Game();
        game.setId(1L);

        Writer writer = new Writer();
        writer.setId(10L);

        given(gameService.getGame(anyLong())).willReturn(game);
        given(gameService.findWriterFromId(anyLong(), any(Game.class))).willReturn(writer);
        given(gameService.findUserFromToken(anyString()))
                .willThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Error: You are not Authorized."));

        MockHttpServletRequestBuilder postRequest = post("/games/1/vote")
                .header("Authorization", "Bearer invalidToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("10");

        mockMvc.perform(postRequest)
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void vote_userNotJudge_403Forbidden() throws Exception {
        Game game = new Game();
        game.setId(1L);

        Writer writer = new Writer();
        writer.setId(10L);

        given(gameService.getGame(anyLong())).willReturn(game);
        given(gameService.findWriterFromId(anyLong(), any(Game.class))).willReturn(writer);
        given(gameService.findUserFromToken(anyString())).willReturn(new User());
        given(gameService.getJudgeFromUser(any(), any()))
                .willThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Error: You are not allowed to vote for this game."));

        MockHttpServletRequestBuilder postRequest = post("/games/1/vote")
                .header("Authorization", "Bearer token123")
                .contentType(MediaType.APPLICATION_JSON)
                .content("10");

        mockMvc.perform(postRequest)
                .andExpect(status().isForbidden());
    }

    @Test
    public void vote_gameNotFound_404NotFound() throws Exception {
        given(gameService.getGame(anyLong()))
                .willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Error: The provided id is invalid."));

        MockHttpServletRequestBuilder postRequest = post("/games/99/vote")
                .header("Authorization", "Bearer token123")
                .contentType(MediaType.APPLICATION_JSON)
                .content("10");

        mockMvc.perform(postRequest)
                .andExpect(status().isNotFound());
    }

    @Test
    public void vote_gameNotOver_400BadRequest() throws Exception {
        Game game = new Game();
        game.setId(1L);

        Writer writer = new Writer();
        writer.setId(10L);

        given(gameService.getGame(anyLong())).willReturn(game);
        given(gameService.findWriterFromId(anyLong(), any(Game.class))).willReturn(writer);
        given(gameService.findUserFromToken(anyString())).willReturn(new User());
        given(gameService.getJudgeFromUser(any(), any())).willReturn(new Judge());
        doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Error: The game is not over."))
                .when(gameService).checkGameIsOver(any());

        MockHttpServletRequestBuilder postRequest = post("/games/1/vote")
                .header("Authorization", "Bearer token123")
                .contentType(MediaType.APPLICATION_JSON)
                .content("10");

        mockMvc.perform(postRequest)
                .andExpect(status().isBadRequest());
    }
}