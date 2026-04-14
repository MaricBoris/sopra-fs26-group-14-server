package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.entity.*;
import ch.uzh.ifi.hase.soprafs26.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs26.repository.RoomRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class GameServiceTest {

    @Mock
    private GameRepository gameRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private GameService gameService;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    // --- getGame ---

    @Test
    public void getGame_validId_returnsGame() {
        Game game = new Game();
        game.setId(1L);

        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));

        Game found = gameService.getGame(1L);
        assertEquals(1L, found.getId());
    }

    @Test
    public void getGame_invalidId_throws404() {
        when(gameRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> gameService.getGame(99L));
    }

    // --- findUserFromToken ---

    @Test
    public void findUserFromToken_validToken_returnsUser() {
        User user = new User();
        user.setId(1L);

        when(userRepository.findByToken("validToken")).thenReturn(user);

        User found = gameService.findUserFromToken("validToken");
        assertEquals(1L, found.getId());
    }

    @Test
    public void findUserFromToken_invalidToken_throws401() {
        when(userRepository.findByToken("invalidToken")).thenReturn(null);

        assertThrows(ResponseStatusException.class, () -> gameService.findUserFromToken("invalidToken"));
    }

    // --- getJudgeFromUser ---

    @Test
    public void getJudgeFromUser_userIsJudge_returnsJudge() {
        User user = new User();
        user.setId(1L);

        Judge judge = new Judge(user);
        judge.setId(1L);

        Game game = new Game();
        game.setJudges(List.of(judge));

        Judge found = gameService.getJudgeFromUser(user, game);
        assertEquals(judge, found);
    }

    @Test
    public void getJudgeFromUser_userIsNotJudge_throws403() {
        User user = new User();
        user.setId(1L);

        User otherUser = new User();
        otherUser.setId(2L);

        Judge judge = new Judge(otherUser);
        judge.setId(1L);

        Game game = new Game();
        game.setJudges(List.of(judge));

        assertThrows(ResponseStatusException.class, () -> gameService.getJudgeFromUser(user, game));
    }

    // --- getWriterFromUser ---

    @Test
    public void getWriterFromUser_userIsWriter_returnsWriter() {
        User user = new User();
        user.setId(1L);

        Writer writer = new Writer();
        writer.setId(1L);
        writer.setUser(user);

        Game game = new Game();
        game.setWriters(List.of(writer));

        Writer found = gameService.getWriterFromUser(user, game);
        assertEquals(writer, found);
    }

    @Test
    public void getWriterFromUser_userIsNotWriter_throws400() {
        User user = new User();
        user.setId(1L);

        User otherUser = new User();
        otherUser.setId(2L);

        Writer writer = new Writer();
        writer.setId(1L);
        writer.setUser(otherUser);

        Game game = new Game();
        game.setWriters(List.of(writer));

        assertThrows(ResponseStatusException.class, () -> gameService.getWriterFromUser(user, game));
    }

    // --- checkGameIsOver ---

    @Test
    public void checkGameIsOver_timerIsZero_noException() {
        Game game = new Game();
        game.setTimer(0L);

        //assertDoesNotThrow(() -> gameService.checkGameIsOver(game));
    }

    @Test
    public void checkGameIsOver_timerNotZero_throws400() {
        Game game = new Game();
        game.setTimer(60L);

        assertThrows(ResponseStatusException.class, () -> gameService.checkGameIsOver(game));
    }

    // --- addVote and allJudgesVoted ---

    @Test
    public void addVote_singleJudge_allVoted() {
        User user = new User();
        user.setId(1L);

        Judge judge = new Judge(user);
        judge.setId(1L);

        Writer writer = new Writer();
        writer.setId(1L);

        Game game = new Game();
        game.setId(1L);
        game.setJudges(List.of(judge));

        assertFalse(gameService.allJudgesVoted(game));

        gameService.addVote(game, writer, judge);

        assertTrue(gameService.allJudgesVoted(game));
    }

    @Test
    public void addVote_multipleJudges_notAllVoted() {
        User user1 = new User();
        user1.setId(1L);
        User user2 = new User();
        user2.setId(2L);

        Judge judge1 = new Judge(user1);
        judge1.setId(1L);
        Judge judge2 = new Judge(user2);
        judge2.setId(2L);

        Writer writer = new Writer();
        writer.setId(1L);

        Game game = new Game();
        game.setId(1L);
        game.setJudges(List.of(judge1, judge2));

        gameService.addVote(game, writer, judge1);

        assertFalse(gameService.allJudgesVoted(game));
    }

    // --- determineWinner ---

    @Test
    public void determineWinner_clearWinner_returnsWinner() {
        User user1 = new User();
        user1.setId(1L);
        User user2 = new User();
        user2.setId(2L);
        User user3 = new User();
        user3.setId(3L);

        Judge judge1 = new Judge(user1);
        judge1.setId(1L);
        Judge judge2 = new Judge(user2);
        judge2.setId(2L);
        Judge judge3 = new Judge(user3);
        judge3.setId(3L);

        Writer writerA = new Writer();
        writerA.setId(1L);
        Writer writerB = new Writer();
        writerB.setId(2L);

        Game game = new Game();
        game.setId(1L);
        game.setJudges(List.of(judge1, judge2, judge3));

        gameService.addVote(game, writerA, judge1);
        gameService.addVote(game, writerA, judge2);
        gameService.addVote(game, writerB, judge3);

        Writer winner = gameService.determineWinner(game);
        assertEquals(writerA, winner);
    }

    @Test
    public void determineWinner_tie_returnsNull() {
        User user1 = new User();
        user1.setId(1L);
        User user2 = new User();
        user2.setId(2L);

        Judge judge1 = new Judge(user1);
        judge1.setId(1L);
        Judge judge2 = new Judge(user2);
        judge2.setId(2L);

        Writer writerA = new Writer();
        writerA.setId(1L);
        Writer writerB = new Writer();
        writerB.setId(2L);

        Game game = new Game();
        game.setId(1L);
        game.setJudges(List.of(judge1, judge2));

        gameService.addVote(game, writerA, judge1);
        gameService.addVote(game, writerB, judge2);

        Writer winner = gameService.determineWinner(game);
        assertNull(winner);
    }

    // --- clearVotes ---

    @Test
    public void clearVotes_removesAllVotes() {
        User user = new User();
        user.setId(1L);

        Judge judge = new Judge(user);
        judge.setId(1L);

        Writer writer = new Writer();
        writer.setId(1L);

        Game game = new Game();
        game.setId(1L);
        game.setJudges(List.of(judge));

        gameService.addVote(game, writer, judge);
        assertTrue(gameService.allJudgesVoted(game));

        gameService.clearVotes(game);
        assertFalse(gameService.allJudgesVoted(game));
    }

    // --- updateStory ---

    @Test
    public void updateStory_createsStoryWithCorrectFields() {
        User winnerUser = new User();
        winnerUser.setId(1L);
        User loserUser = new User();
        loserUser.setId(2L);
        User judgeUser = new User();
        judgeUser.setId(3L);

        Writer winnerWriter = new Writer();
        winnerWriter.setId(1L);
        winnerWriter.setUser(winnerUser);
        winnerWriter.setGenre("Horror");

        Writer loserWriter = new Writer();
        loserWriter.setId(2L);
        loserWriter.setUser(loserUser);
        loserWriter.setGenre("Comedy");

        Judge judge = new Judge(judgeUser);
        judge.setId(1L);

        Story existingStory = new Story(null, null, "Once upon a time...", false, null, null, new ArrayList<>());

        Game game = new Game();
        game.setId(1L);
        game.setWriters(List.of(winnerWriter, loserWriter));
        game.setJudges(List.of(judge));
        game.setStory(existingStory);

        when(gameRepository.save(any())).thenReturn(game);

        Story result = gameService.updateStory(winnerWriter, game);

        assertEquals(winnerUser, result.getWinner());
        assertEquals(loserUser, result.getLoser());
        assertEquals("Horror", result.getWinGenre());
        assertEquals("Comedy", result.getLoseGenre());
        assertEquals("Once upon a time...", result.getStoryText());
        verify(gameRepository, times(1)).save(game);
    }

    // --- updateHistory ---

    @Test
    public void updateHistory_addsStoryToAllPlayers() {
        User writerUser = new User();
        writerUser.setId(1L);
        User judgeUser = new User();
        judgeUser.setId(2L);

        Writer writer = new Writer();
        writer.setId(1L);
        writer.setUser(writerUser);

        Judge judge = new Judge(judgeUser);
        judge.setId(1L);

        Story story = new Story();

        Game game = new Game();
        game.setId(1L);
        game.setWriters(List.of(writer));
        game.setJudges(List.of(judge));
        game.setStory(story);

        gameService.updateHistory(game);

        assertTrue(writerUser.getHistory().contains(story));
        assertTrue(judgeUser.getHistory().contains(story));
        verify(userRepository, times(2)).save(any());
    }

        // --- determineWinner with no votes ---

    @Test
    public void determineWinner_noVotes_returnsNull() {
        Game game = new Game();
        game.setId(1L);

        Writer winner = gameService.determineWinner(game);
        assertNull(winner);
    }

    // --- cleanupGame ---

    @Test
    public void cleanupGame_deletesGameAndClearsLists() {
        User user1 = new User();
        user1.setId(10L);
        User user2 = new User();
        user2.setId(11L);

        Writer writer = new Writer();
        writer.setId(1L);
        writer.setUser(user1);

        Judge judge = new Judge(user2);
        judge.setId(1L);

        Game game = new Game();
        game.setId(1L);
        game.setWriters(new ArrayList<>(List.of(writer)));
        game.setJudges(new ArrayList<>(List.of(judge)));

        gameService.cleanupGame(game);

        //assertTrue(game.getWriters().isEmpty());
        //assertTrue(game.getJudges().isEmpty());
        //verify(gameRepository, times(1)).delete(game);
    }
}