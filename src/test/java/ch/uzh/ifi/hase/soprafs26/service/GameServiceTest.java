package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.constant.GamePhase;
import ch.uzh.ifi.hase.soprafs26.entity.*;
import ch.uzh.ifi.hase.soprafs26.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs26.repository.StoryRepository;
import ch.uzh.ifi.hase.soprafs26.repository.RoomRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
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

    @Mock
    private GameCleanupService gameCleanupService;

    @Mock
    private QuoteService quoteService;

    @Mock
    private StoryRepository storyRepository;

    @InjectMocks
    private GameService gameService;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }


    private Game createGameWith(List<Writer> writers, List<Judge> judges) {
        Game game = new Game();
        game.setId(1L);
        game.setWriters(new ArrayList<>(writers));
        game.setJudges(new ArrayList<>(judges));
        return game;
    }

    private void mockExitDependencies(Game game, User user) {
        when(userService.extractToken(any())).thenReturn("token");
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(userRepository.findByToken("token")).thenReturn(user);
    }

    // ==================== getGame(Long) ====================

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

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> gameService.getGame(99L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    // ==================== findUserFromToken ====================

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

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> gameService.findUserFromToken("invalidToken"));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    // ==================== getJudgeFromUser ====================

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

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> gameService.getJudgeFromUser(user, game));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    // ==================== getWriterFromUser ====================

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

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> gameService.getWriterFromUser(user, game));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    // ==================== findWriterFromId ====================

    @Test
    public void findWriterFromId_validId_returnsWriter() {
        User user = new User();
        user.setId(5L);

        Writer writer = new Writer();
        writer.setId(1L);
        writer.setUser(user);

        Game game = new Game();
        game.setWriters(List.of(writer));

        Writer found = gameService.findWriterFromId(5L, game);
        assertEquals(writer, found);
    }

    @Test
    public void findWriterFromId_invalidId_throws400() {
        User user = new User();
        user.setId(5L);
        Writer writer = new Writer();
        writer.setId(5L);
        writer.setUser(user);

        Game game = new Game();
        game.setWriters(List.of(writer));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> gameService.findWriterFromId(99L, game));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    public void findWriterFromId_multipleWriters_returnsCorrectOne() {
        User user1 = new User();
        user1.setId(1L);
        User user2 = new User();
        user2.setId(2L);

        Writer writer1 = new Writer();
        writer1.setId(1L);
        writer1.setUser(user1);

        Writer writer2 = new Writer();
        writer2.setId(2L);
        writer2.setUser(user2);

        Game game = new Game();
        game.setWriters(List.of(writer1, writer2));

        Writer found = gameService.findWriterFromId(2L, game);
        assertEquals(writer2, found);
    }

    // ==================== checkGameIsOver ====================

    @Test
    public void checkGameIsOver_evaluationPhase_noException() {
        Game game = new Game();
        game.setPhase(GamePhase.EVALUATION);

        assertDoesNotThrow(() -> gameService.checkGameIsOver(game));
    }

    @Test
    public void checkGameIsOver_writingPhase_throws400() {
        Game game = new Game();
        game.setPhase(GamePhase.WRITING);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> gameService.checkGameIsOver(game));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    public void checkGameIsOver_finishedPhase_throws400() {
        Game game = new Game();
        game.setPhase(GamePhase.FINISHED);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> gameService.checkGameIsOver(game));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    // ==================== addVote and allJudgesVoted ====================

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

    @Test
    public void addVote_multipleJudges_allVoted() {
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
        gameService.addVote(game, writer, judge2);

        assertTrue(gameService.allJudgesVoted(game));
    }

    @Test
    public void addVote_judgeChangesVote_stillCountsAsOne() {
        User user = new User();
        user.setId(1L);

        Judge judge = new Judge(user);
        judge.setId(1L);

        Writer writer1 = new Writer();
        writer1.setId(1L);
        Writer writer2 = new Writer();
        writer2.setId(2L);

        Game game = new Game();
        game.setId(1L);
        game.setJudges(List.of(judge));

        gameService.addVote(game, writer1, judge);
        gameService.addVote(game, writer2, judge);

        // Map replaces the value, so still 1 vote entry
        assertTrue(gameService.allJudgesVoted(game));
    }

    // ==================== determineWinner ====================

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

    @Test
    public void determineWinner_noVotes_returnsNull() {
        Game game = new Game();
        game.setId(1L);

        Writer winner = gameService.determineWinner(game);
        assertNull(winner);
    }

    @Test
    public void determineWinner_unanimousVote_returnsWinner() {
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

        Game game = new Game();
        game.setId(1L);
        game.setJudges(List.of(judge1, judge2));

        gameService.addVote(game, writerA, judge1);
        gameService.addVote(game, writerA, judge2);

        Writer winner = gameService.determineWinner(game);
        assertEquals(writerA, winner);
    }

    // ==================== clearVotes ====================

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

    @Test
    public void clearVotes_noVotesExist_noException() {
        Game game = new Game();
        game.setId(42L);
        game.setJudges(List.of());

        assertDoesNotThrow(() -> gameService.clearVotes(game));
    }

    // ==================== updateStory ====================

    @Test
    public void updateStory_withWinner_setsCorrectFields() {
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
        assertTrue(result.getHasWinner());
        verify(gameRepository, times(1)).save(game);
    }

    @Test
    public void updateStory_withNullWinner_tieCase() {
        User user1 = new User();
        user1.setId(1L);
        User user2 = new User();
        user2.setId(2L);
        User judgeUser = new User();
        judgeUser.setId(3L);

        Writer writer1 = new Writer();
        writer1.setId(1L);
        writer1.setUser(user1);
        writer1.setGenre("Sci-Fi");

        Writer writer2 = new Writer();
        writer2.setId(2L);
        writer2.setUser(user2);
        writer2.setGenre("Romance");

        Judge judge = new Judge(judgeUser);
        judge.setId(1L);

        Story existingStory = new Story(null, null, "A tie story...", false, null, null, new ArrayList<>());

        Game game = new Game();
        game.setId(1L);
        game.setWriters(List.of(writer1, writer2));
        game.setJudges(List.of(judge));
        game.setStory(existingStory);

        when(gameRepository.save(any())).thenReturn(game);

        Story result = gameService.updateStory(null, game);

        assertEquals(user1, result.getWinner());
        assertEquals(user2, result.getLoser());
        assertFalse(result.getHasWinner());
        assertEquals("A tie story...", result.getStoryText());
        verify(gameRepository, times(1)).save(game);
    }

    // ==================== updateHistory ====================

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

    @Test
    public void updateHistory_multipleWritersAndJudges_allGetStory() {
        User writerUser1 = new User();
        writerUser1.setId(1L);
        User writerUser2 = new User();
        writerUser2.setId(2L);
        User judgeUser1 = new User();
        judgeUser1.setId(3L);
        User judgeUser2 = new User();
        judgeUser2.setId(4L);

        Writer writer1 = new Writer();
        writer1.setId(1L);
        writer1.setUser(writerUser1);
        Writer writer2 = new Writer();
        writer2.setId(2L);
        writer2.setUser(writerUser2);

        Judge judge1 = new Judge(judgeUser1);
        judge1.setId(1L);
        Judge judge2 = new Judge(judgeUser2);
        judge2.setId(2L);

        Story story = new Story();

        Game game = new Game();
        game.setId(1L);
        game.setWriters(List.of(writer1, writer2));
        game.setJudges(List.of(judge1, judge2));
        game.setStory(story);

        gameService.updateHistory(game);

        assertTrue(writerUser1.getHistory().contains(story));
        assertTrue(writerUser2.getHistory().contains(story));
        assertTrue(judgeUser1.getHistory().contains(story));
        assertTrue(judgeUser2.getHistory().contains(story));
        verify(userRepository, times(4)).save(any());
    }

    // ==================== cleanupGame ====================

    @Test
    public void cleanupGame_setsPhaseToFinishedAndSaves() {
        Game game = new Game();
        game.setId(1L);
        game.setPhase(GamePhase.EVALUATION);
        game.setWriters(new ArrayList<>(List.of(new Writer())));
        game.setJudges(new ArrayList<>(List.of(new Judge())));

        gameService.cleanupGame(game);

        assertEquals(GamePhase.FINISHED, game.getPhase());
        assertFalse(game.getWriters().isEmpty());
        assertFalse(game.getJudges().isEmpty());
        verify(gameRepository, times(1)).save(game);
        verify(gameRepository, never()).delete(any());
    }

    // ==================== deleteGame ====================

    @Test
    public void deleteGame_clearsListsAndDeletes() {
        User user1 = new User();
        user1.setId(10L);
        User user2 = new User();
        user2.setId(11L);

        Writer writer = new Writer();
        writer.setId(1L);
        writer.setUser(user1);

        Judge judge = new Judge(user2);
        judge.setId(1L);

        Story story = new Story();

        Game game = new Game();
        game.setId(1L);
        game.setWriters(new ArrayList<>(List.of(writer)));
        game.setJudges(new ArrayList<>(List.of(judge)));
        game.setStory(story);

        gameService.deleteGame(game);

        assertTrue(game.getWriters().isEmpty());
        assertTrue(game.getJudges().isEmpty());
        assertNull(game.getStory());
        verify(gameRepository, times(1)).save(game);
        verify(gameRepository, times(1)).delete(game);
        verify(gameRepository, times(1)).flush();
    }

    // ==================== getandCheckGame ====================

    @Test
    public void getandCheckGame_validIdAndToken_returnsGame() {
        Game game = new Game();
        game.setId(1L);

        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));

        Game found = gameService.getandCheckGame(1L, "validToken");
        assertEquals(1L, found.getId());
    }

    @Test
    public void getandCheckGame_invalidId_throws404() {
        when(gameRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> gameService.getandCheckGame(99L, "validToken"));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    public void getandCheckGame_nullToken_throws401() {
        Game game = new Game();
        game.setId(1L);

        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> gameService.getandCheckGame(1L, null));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    public void getandCheckGame_blankToken_throws401() {
        Game game = new Game();
        game.setId(1L);

        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> gameService.getandCheckGame(1L, "   "));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    // ==================== getandCheckUser ====================

    @Test
    public void getandCheckUser_validToken_returnsUser() {
        User user = new User();
        user.setId(1L);

        when(userRepository.findByToken("validToken")).thenReturn(user);

        User found = gameService.getandCheckUser("validToken");
        assertEquals(1L, found.getId());
    }

    @Test
    public void getandCheckUser_invalidToken_throws401() {
        when(userRepository.findByToken("badToken")).thenReturn(null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> gameService.getandCheckUser("badToken"));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    // ==================== allJudgesVoted edge case ====================

    @Test
    public void allJudgesVoted_noJudges_returnsTrue() {
        Game game = new Game();
        game.setId(1L);
        game.setJudges(List.of());

        // 0 votes >= 0 judges → true
        assertTrue(gameService.allJudgesVoted(game));
    }

    // ==================== exitGame ====================

    @Test
    void exitGame_removesWriter_deletesGame() {
        User user = new User();
        user.setId(10L);
        User otherWriterUser = new User();
        otherWriterUser.setId(20L);
        Writer writer = new Writer(user);
        Writer otherWriter = new Writer(otherWriterUser);
        Judge judge = new Judge(new User());
        judge.getUser().setId(30L);
        Game game = createGameWith(List.of(writer, otherWriter), List.of(judge));
        mockExitDependencies(game, user);

        gameService.exitGame(1L, "Bearer token");

        assertFalse(game.getWriters().contains(writer));
        verify(gameRepository).delete(game);
        verify(gameRepository).flush();
        verify(gameRepository, never()).save(any());
    }

    @Test
    void exitGame_removesJudge_deletesIfNoJudgeLeft() {
        User user = new User();
        user.setId(20L);

        Writer writer1 = new Writer(new User());
        writer1.getUser().setId(11L);
        Writer writer2 = new Writer(new User());
        writer2.getUser().setId(12L);

        Judge judge = new Judge(user);

        Game game = createGameWith(List.of(writer1, writer2), List.of(judge));
        mockExitDependencies(game, user);

        gameService.exitGame(1L, "Bearer token");

        assertTrue(game.getJudges().isEmpty());
        verify(gameRepository).delete(game);
        verify(gameRepository).flush();
    }

    @Test
    void exitGame_userNotInGame_throws403() {
        User outsider = new User();
        outsider.setId(99L);

        Writer writer = new Writer(new User());
        writer.getUser().setId(1L);
        Judge judge = new Judge(new User());
        judge.getUser().setId(2L);

        Game game = createGameWith(List.of(writer), List.of(judge));
        mockExitDependencies(game, outsider);

        assertThrows(ResponseStatusException.class,
                () -> gameService.exitGame(1L, "Bearer token"));
    }
}