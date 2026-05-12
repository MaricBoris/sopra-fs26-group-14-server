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
import java.util.Map;

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

    @Mock
    private GameStreamService gameStreamService;

    @Mock
    private StatsAchvsService statsAchvsService;

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

    private User user(long id) {
        User u = new User();
        u.setId(id);
        return u;
    }

    private Judge judge(long id) {
        return new Judge(user(id));
    }

    private Writer writer(long id) {
        return new Writer(user(id));
    }

    private Game gameWith(User judgeUser, Writer... writers) {
        Game g = new Game();
        g.setId(1L);
        g.setJudges(List.of(new Judge(judgeUser)));
        g.setWriters(new ArrayList<>(List.of(writers)));
        mockExitDependencies(g, judgeUser);
        return g;
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
    public void addVote_writerIdIsNull_doesNotAddVote() {
        Game game = new Game();
        game.setId(1L);
        
        Writer writer = new Writer();  // Writer exists but ID is null
        
        gameService.addVote(game, writer, new Judge());
        
        Map<Long, Map<Judge, Writer>> allVotes = gameService.getGameVotes();
        assertFalse(allVotes.containsKey(game.getId()));
    }

    @Test
    public void addVote_singleJudge_allVoted() {
        User judgeUser = new User(); judgeUser.setId(1L);
        Judge judge = new Judge(judgeUser); judge.setId(1L);

        User u1 = new User(); u1.setId(10L);
        Writer writer1 = new Writer(u1); writer1.setId(1L);

        User u2 = new User(); u2.setId(11L);
        Writer writer2 = new Writer(u2); writer2.setId(2L);

        Game game = new Game();
        game.setId(1L);
        game.setJudges(List.of(judge));
        game.setWriters(new ArrayList<>(List.of(writer1, writer2)));
        game.setStory(new Story());

        gameService.addVote(game, writer1, judge);

        assertEquals(0, gameService.noVote);
    }

    @Test
    public void addVote_multipleJudges_allVoted() {
        User uj1 = new User(); uj1.setId(1L);
        Judge judge1 = new Judge(uj1); judge1.setId(1L);

        User uj2 = new User(); uj2.setId(2L);
        Judge judge2 = new Judge(uj2); judge2.setId(2L);

        User uw1 = new User(); uw1.setId(10L);
        Writer w1 = new Writer(uw1); w1.setId(1L);

        User uw2 = new User(); uw2.setId(11L);
        Writer w2 = new Writer(uw2); w2.setId(2L);

        Game game = new Game();
        game.setId(1L);
        game.setJudges(List.of(judge1, judge2));
        game.setWriters(new ArrayList<>(List.of(w1, w2)));
        game.setStory(new Story());

        gameService.addVote(game, w1, judge1);
        gameService.addVote(game, w1, judge2);

        assertEquals(0, gameService.noVote);
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

    // ==================== determineWinner ====================

    @Test
    public void determineWinner_clearWinner_returnsWinner() {
        User u1 = new User(); u1.setId(101L);
        User u2 = new User(); u2.setId(102L);
        User u3 = new User(); u3.setId(103L);
        User uw1 = new User(); uw1.setId(201L);
        User uw2 = new User(); uw2.setId(202L);

        Judge j1 = new Judge(u1); j1.setId(1L);
        Judge j2 = new Judge(u2); j2.setId(2L);
        Judge j3 = new Judge(u3); j3.setId(3L);

        Writer w1 = new Writer(uw1); w1.setId(1L);
        Writer w2 = new Writer(uw2); w2.setId(2L);

        Game game = new Game();
        game.setId(1L);
        game.setJudges(List.of(j1, j2, j3));
        game.setWriters(new ArrayList<>(List.of(w1, w2)));

        gameService.getGameVotes().computeIfAbsent(game.getId(), k -> new java.util.HashMap<>()).put(j1, w1);
        gameService.getGameVotes().get(game.getId()).put(j2, w1);
        gameService.getGameVotes().get(game.getId()).put(j3, w2);

        Writer winner = gameService.determineWinner(game);
        assertEquals(w1, winner);
    }

    @Test
    public void determineWinner_unanimousVote_returnsWinner() {
        User u1 = new User(); u1.setId(101L);
        User u2 = new User(); u2.setId(102L);
        User uw1 = new User(); uw1.setId(201L);

        Judge j1 = new Judge(u1); j1.setId(1L);
        Judge j2 = new Judge(u2); j2.setId(2L);
        Writer w1 = new Writer(uw1); w1.setId(1L);

        Game game = new Game();
        game.setId(1L);
        game.setJudges(List.of(j1, j2));
        game.setWriters(new ArrayList<>(List.of(w1, new Writer(new User()))));

        gameService.getGameVotes().computeIfAbsent(game.getId(), k -> new java.util.HashMap<>()).put(j1, w1);
        gameService.getGameVotes().get(game.getId()).put(j2, w1);

        Writer winner = gameService.determineWinner(game);
        assertEquals(w1, winner);
    }

    @Test
    public void determineWinner_tie_returnsNull() {
        User user1 = new User(); user1.setId(1L);
        User user2 = new User(); user2.setId(2L);
        Judge judge1 = new Judge(user1); judge1.setId(1L);
        Judge judge2 = new Judge(user2); judge2.setId(2L);

        Writer writerA = new Writer(); writerA.setId(1L);
        Writer writerB = new Writer(); writerB.setId(2L);

        Game game = new Game();
        game.setId(1L);
        game.setJudges(List.of(judge1, judge2));
        game.setWriters(new ArrayList<>(List.of(writerA, writerB)));
        game.setStory(new Story());

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

    // ==================== clearVotes ====================

    @Test
    public void clearVotes_removesAllVotes() {
        User u1 = new User(); u1.setId(101L);
        User uw1 = new User(); uw1.setId(201L);

        Judge judge = new Judge(u1); judge.setId(1L);
        Writer w1 = new Writer(uw1); w1.setId(1L);
        Writer w2 = new Writer(new User()); w2.setId(2L); w2.getUser().setId(202L);

        Game game = new Game();
        game.setId(1L);
        game.setJudges(List.of(judge));
        game.setWriters(new ArrayList<>(List.of(w1, w2)));
        game.setStory(new Story());

        gameService.addVote(game, w1, judge);

        assertEquals(0, gameService.noVote);
        assertNull(gameService.getGameVotes().get(game.getId()));
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
        User winnerUser = new User(); winnerUser.setId(1L);
        User loserUser = new User(); loserUser.setId(2L);
        User judgeUser = new User(); judgeUser.setId(3L);

        Writer winnerWriter = new Writer(); winnerWriter.setId(1L);
        winnerWriter.setUser(winnerUser); winnerWriter.setGenre("Horror");

        Writer loserWriter = new Writer(); loserWriter.setId(2L);
        loserWriter.setUser(loserUser); loserWriter.setGenre("Comedy");

        Judge judge = new Judge(judgeUser); judge.setId(1L);
        Story existingStory = new Story();
       

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
        assertTrue(result.getHasWinner());
        // verify based on your current implementation (save vs saveAndFlush)
        verify(gameRepository, atLeastOnce()).save(any());
    }

    @Test
    public void updateStory_withNullWinner_tieCase() {
        User user1 = new User(); user1.setId(1L);
        User user2 = new User(); user2.setId(2L);
        User judgeUser = new User(); judgeUser.setId(3L);

        Writer writer1 = new Writer(); writer1.setId(1L);
        writer1.setUser(user1); writer1.setGenre("Sci-Fi");

        Writer writer2 = new Writer(); writer2.setId(2L);
        writer2.setUser(user2); writer2.setGenre("Romance");

        Judge judge = new Judge(judgeUser); judge.setId(1L);
        Story existingStory = new Story();
        

        Game game = new Game();
        game.setId(1L);
        game.setWriters(List.of(writer1, writer2)); // FIX: Ensure writers are present to prevent IndexOutOfBounds
        game.setJudges(List.of(judge));
        game.setStory(existingStory);

        when(gameRepository.save(any())).thenReturn(game);

        // This test simulates the fallback logic in updateStory
        Story result = gameService.updateStory(null, game);

        assertEquals(user1, result.getWinner()); // Picks writer 0 by default
        assertEquals(user2, result.getLoser());
        assertFalse(result.getHasWinner());
        verify(gameRepository, atLeastOnce()).save(any());
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

    private User makeUser(Long id) {
        User user = new User();
        user.setId(id);
        return user;
    }

    private Game makeGame(Writer activeWriter, Writer otherWriter, Judge judge) {
        Game game = new Game();
        game.setId(1L);
        game.setPhase(GamePhase.WRITING);
        game.setRoundResolved(false);
        game.setCurrentRound(1);
        game.setTimer(60L);
        game.setTurnStartedAt(System.currentTimeMillis());

        List<Writer> writers = new ArrayList<>();
        writers.add(activeWriter);
        writers.add(otherWriter);
        game.setWriters(writers);

        List<Judge> judges = new ArrayList<>();
        judges.add(judge);
        game.setJudges(judges);

        return game;
    }

    private Writer makeActiveWriter(Long writerId, Long userId, String text, String genre) {
        Writer writer = new Writer();
        writer.setId(writerId);
        writer.setUser(makeUser(userId));
        writer.setTurn(true);
        writer.setText(text);
        writer.setGenre(genre);
        return writer;
    }

    private Writer makeOtherWriter(Long writerId, Long userId, String text, String genre) {
        Writer writer = new Writer();
        writer.setId(writerId);
        writer.setUser(makeUser(userId));
        writer.setTurn(false);
        writer.setText(text);
        writer.setGenre(genre);
        return writer;
    }

    @Test
    public void insertWriterInput_userNotWriter_throws403() {
        Writer activeWriter = makeActiveWriter(11L, 1L, "draft from active writer", "Fantasy");
        Writer otherWriter = makeOtherWriter(12L, 2L, "draft from other writer", "Sci-Fi");

        Judge judge = new Judge(makeUser(3L));
        judge.setId(13L);

        Game game = makeGame(activeWriter, otherWriter, judge);
        game.setStory(new Story());

        User outsider = makeUser(99L);

        when(userService.extractToken("Bearer outsider-token")).thenReturn("outsider-token");
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(userRepository.findByToken("outsider-token")).thenReturn(outsider);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> gameService.insertWriterInput(1L, 1, "Some text", "Bearer outsider-token"));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(gameRepository, never()).saveAndFlush(any(Game.class));
    }

    @Test
    public void insertWriterInput_notWritersTurn_throws403() {
        Writer activeWriter = makeActiveWriter(11L, 1L, "draft from active writer", "Fantasy");
        Writer otherWriter = makeOtherWriter(12L, 2L, "draft from other writer", "Sci-Fi");

        Judge judge = new Judge(makeUser(3L));
        judge.setId(13L);

        Game game = makeGame(activeWriter, otherWriter, judge);
        game.setStory(new Story());

        User otherWriterUser = makeUser(2L);

        when(userService.extractToken("Bearer notTurn-token")).thenReturn("notTurn-token");
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(userRepository.findByToken("notTurn-token")).thenReturn(otherWriterUser);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> gameService.insertWriterInput(1L, 2, "Some text", "Bearer notTurn-token"));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(gameRepository, never()).saveAndFlush(any(Game.class));
    }

    @Test
    public void insertWriterInput_inputTooLong_truncatedTo2000() {
        Writer activeWriter = makeActiveWriter(11L, 1L, "draft from active writer", "Fantasy");
        Writer otherWriter = makeOtherWriter(12L, 2L, "draft from other writer", "Sci-Fi");

        Judge judge = new Judge(makeUser(3L));
        judge.setId(13L);

        Game game = makeGame(activeWriter, otherWriter, judge);
        game.setStory(new Story());

        User activeUser = makeUser(1L);

        when(userService.extractToken("Bearer active-token")).thenReturn("active-token");
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(userRepository.findByToken("active-token")).thenReturn(activeUser);
        when(gameRepository.saveAndFlush(any(Game.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String tooLongInput = "x".repeat(1995) + " end.y";

        Game result = gameService.insertWriterInput(1L, 1, tooLongInput, "Bearer active-token");

        assertFalse(result.getStory().getStoryContributions().isEmpty());
        assertTrue(result.getStory().getStoryContributions().get(0).getText().length() <= 2000);
    }

   /* @Test
    public void insertWriterInput_validInput_appendsToStory_clearsDraft_switchesTurn_resetsTimerAndPersists() {
        Writer activeWriter = makeActiveWriter(11L, 1L, "draft from active writer", "Fantasy");
        Writer otherWriter = makeOtherWriter(12L, 2L, "draft from other writer", "Sci-Fi");

        Judge judge = new Judge(makeUser(3L));
        judge.setId(13L);

        Game game = makeGame(activeWriter, otherWriter, judge);

        Story story = new Story();
        story.setStoryText("Once upon a time");
        game.setStory(story);

        User activeUser = makeUser(1L);

        when(userService.extractToken("Bearer active-token")).thenReturn("active-token");
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(userRepository.findByToken("active-token")).thenReturn(activeUser);
        when(gameRepository.saveAndFlush(any(Game.class))).thenAnswer(invocation -> invocation.getArgument(0));

        long beforeCall = System.currentTimeMillis();
        Game result = gameService.insertWriterInput(1L, 1, "  there was a dragon  ", "Bearer active-token");
        long afterCall = System.currentTimeMillis();

        assertNotNull(result.getStory());
        assertEquals("Once upon a time there was a dragon", result.getStory().getStoryText());
        assertEquals("", result.getWriters().get(0).getText());
        assertFalse(result.getWriters().get(0).getTurn());
        assertTrue(result.getWriters().get(1).getTurn());
        assertEquals(2, result.getCurrentRound());
        assertEquals(90L, result.getTimer());
        assertNotNull(result.getTurnStartedAt());
        assertTrue(result.getTurnStartedAt() >= beforeCall);
        assertTrue(result.getTurnStartedAt() <= afterCall);

        verify(gameRepository, times(1)).saveAndFlush(game);
    }*/

    @Test
    public void insertWriterInput_nullStory_createsNewStory() {
        Writer activeWriter = makeActiveWriter(11L, 1L, "draft", "Fantasy");
        Writer otherWriter = makeOtherWriter(12L, 2L, "draft", "Sci-Fi");
        Judge judge = new Judge(makeUser(3L));
        judge.setId(13L);

        Game game = makeGame(activeWriter, otherWriter, judge);
        game.setStory(null); // null story

        User activeUser = makeUser(1L);
        when(userService.extractToken("Bearer active-token")).thenReturn("active-token");
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(userRepository.findByToken("active-token")).thenReturn(activeUser);
        when(gameRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Game result = gameService.insertWriterInput(1L, 1, "first sentence", "Bearer active-token");

        assertNotNull(result.getStory());
        assertFalse(result.getStory().getStoryContributions().isEmpty());
        assertEquals("first sentence", result.getStory().getStoryContributions().get(0).getText());
    }

    @Test
    public void insertWriterInput_blankStory_setsTextDirectly() {
        Writer activeWriter = makeActiveWriter(11L, 1L, "draft", "Fantasy");
        Writer otherWriter = makeOtherWriter(12L, 2L, "draft", "Sci-Fi");
        Judge judge = new Judge(makeUser(3L));
        judge.setId(13L);

        Game game = makeGame(activeWriter, otherWriter, judge);
        Story story = new Story();
        game.setStory(story);

        User activeUser = makeUser(1L);
        when(userService.extractToken("Bearer active-token")).thenReturn("active-token");
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(userRepository.findByToken("active-token")).thenReturn(activeUser);
        when(gameRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Game result = gameService.insertWriterInput(1L, 1, "first sentence", "Bearer active-token");

        assertFalse(result.getStory().getStoryContributions().isEmpty());
        assertEquals("first sentence", result.getStory().getStoryContributions().get(0).getText());
    }

    @Test
    public void insertWriterInput_phaseNotWriting_throws409() {
        Writer activeWriter = makeActiveWriter(11L, 1L, "draft", "Fantasy");
        Writer otherWriter = makeOtherWriter(12L, 2L, "draft", "Sci-Fi");
        Judge judge = new Judge(makeUser(3L));
        judge.setId(13L);

        Game game = makeGame(activeWriter, otherWriter, judge);
        game.setPhase(GamePhase.EVALUATION);
        game.setStory(new Story());

        User activeUser = makeUser(1L);
        when(userService.extractToken("Bearer active-token")).thenReturn("active-token");
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(userRepository.findByToken("active-token")).thenReturn(activeUser);

        assertThrows(ResponseStatusException.class,
                () -> gameService.insertWriterInput(1L, 1, "text", "Bearer active-token"));
    }

    @Test
    public void insertWriterInput_roundAlreadyResolved_throws409() {
        Writer activeWriter = makeActiveWriter(11L, 1L, "draft", "Fantasy");
        Writer otherWriter = makeOtherWriter(12L, 2L, "draft", "Sci-Fi");
        Judge judge = new Judge(makeUser(3L));
        judge.setId(13L);

        Game game = makeGame(activeWriter, otherWriter, judge);
        game.setRoundResolved(true);
        game.setStory(new Story());

        User activeUser = makeUser(1L);
        when(userService.extractToken("Bearer active-token")).thenReturn("active-token");
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(userRepository.findByToken("active-token")).thenReturn(activeUser);

        assertThrows(ResponseStatusException.class,
                () -> gameService.insertWriterInput(1L, 1, "text", "Bearer active-token"));
    }

   /* @Test
    public void getGame_expiredTurn_autoSubmitsEmptyRound_switchesTurn_resetsTimerAndPersists() {
        Writer activeWriter = makeActiveWriter(11L, 1L, "draft that should be cleared on timeout", "Fantasy");
        Writer otherWriter = makeOtherWriter(12L, 2L, "draft from other writer", "Sci-Fi");

        Judge judge = new Judge(makeUser(3L));
        judge.setId(13L);

        Game game = makeGame(activeWriter, otherWriter, judge);

        Story story = new Story();
        story.setStoryText("Existing story");
        game.setStory(story);

        game.setTimer(1L);
        game.setTurnStartedAt(System.currentTimeMillis() - 5000L);

        User activeUser = makeUser(1L);

        when(userService.extractToken("Bearer active-token")).thenReturn("active-token");
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(userRepository.findByToken("active-token")).thenReturn(activeUser);
        when(gameRepository.saveAndFlush(any(Game.class))).thenAnswer(invocation -> invocation.getArgument(0));

        long beforeCall = System.currentTimeMillis();
        Game result = gameService.getGame(1L, "Bearer active-token");
        long afterCall = System.currentTimeMillis();

        assertEquals("", result.getWriters().get(0).getText());
        assertFalse(result.getWriters().get(0).getTurn());
        assertTrue(result.getWriters().get(1).getTurn());
        assertEquals(2, result.getCurrentRound());
        assertEquals(90L, result.getTimer());
        assertNotNull(result.getTurnStartedAt());
        assertTrue(result.getTurnStartedAt() >= beforeCall);
        assertTrue(result.getTurnStartedAt() <= afterCall);
        assertEquals("Existing story", result.getStory().getStoryText());

        verify(gameRepository, times(2)).saveAndFlush(game);
    }*/

    // ==================== assignQuote ====================

    @Test
    void assignQuote_success() {
        User judge = user(1L);
        Writer w = writer(2L);
        Game game = gameWith(judge, w, writer(3L));
        when(quoteService.fetchRandomQuote()).thenReturn("quote");
        gameService.assignQuote(1L, 1, "Bearer token");
        assertEquals("quote", w.getQuote());
        verify(gameRepository).saveAndFlush(game);
    }

    @Test
    void assignQuote_userNotJudge_throws403() {
        User actualJudge = user(1L);
        Judge judge = new Judge(actualJudge);
        User requestingUser = user(99L);
        Game game = createGameWith(List.of(writer(2L), writer(3L)), List.of(judge));

        when(userService.extractToken(any())).thenReturn("token");
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(userRepository.findByToken("token")).thenReturn(requestingUser);

        assertThrows(ResponseStatusException.class, () -> gameService.assignQuote(1L, 1, "Bearer token"));
    }

    @Test
    void assignQuote_quoteFetchFails_throws502() {
        User judge = user(1L);
        Game game = gameWith(judge, writer(2L), writer(3L));
        when(quoteService.fetchRandomQuote()).thenReturn(null);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> gameService.assignQuote(1L, 1, "Bearer token"));
        assertEquals(HttpStatus.BAD_GATEWAY, ex.getStatusCode());
    }

    // ==================== getGame with bearer token ====================

    @Test
    void getGame_userIsWriter_updatesLastSeenAndReturns() {
        User user = user(10L);
        Writer writer = new Writer(user);
        Judge judge = judge(20L);
        Game game = createGameWith(List.of(writer, writer(30L)), List.of(judge));
        game.setPhase(GamePhase.WRITING);
        game.setTurnStartedAt(System.currentTimeMillis());
        game.setTimer(90L);
        game.setRoundResolved(false);

        mockExitDependencies(game, user);
        when(gameRepository.saveAndFlush(any())).thenReturn(game);

        Game result = gameService.getGame(1L, "Bearer token");

        assertNotNull(writer.getLastSeenAt());
        assertEquals(game, result);
        verify(gameRepository).saveAndFlush(game);
    }

    @Test
    void getGame_userIsJudge_updatesLastSeenAndReturns() {
        User user = user(20L);
        Judge judge = new Judge(user);
        Writer writer1 = writer(10L);
        Writer writer2 = writer(30L);
        Game game = createGameWith(List.of(writer1, writer2), List.of(judge));
        game.setPhase(GamePhase.WRITING);
        game.setTurnStartedAt(System.currentTimeMillis());
        game.setTimer(90L);
        game.setRoundResolved(false);

        mockExitDependencies(game, user);
        when(gameRepository.saveAndFlush(any())).thenReturn(game);

        Game result = gameService.getGame(1L, "Bearer token");

        assertNotNull(judge.getLastSeenAt());
        assertEquals(game, result);
        verify(gameRepository).saveAndFlush(game);
    }

    @Test
    void getGame_userNotPartOfGame_throws403() {
        User outsider = user(99L);
        Writer writer = writer(10L);
        Judge judge = judge(20L);
        Game game = createGameWith(List.of(writer, writer(30L)), List.of(judge));
        mockExitDependencies(game, outsider);

        assertThrows(ResponseStatusException.class, () -> gameService.getGame(1L, "Bearer token"));
    }

    @Test
    void getGame_invalidGameState_throws400() {
        User user = user(10L);
        Writer writer = new Writer(user);
        Game game = createGameWith(List.of(writer), List.of(judge(20L)));
        mockExitDependencies(game, user);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> gameService.getGame(1L, "Bearer token"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void getGame_expiredTurn_clearsTextAndAdvancesTurn() {
        User user = user(10L);
        Writer w1 = new Writer(user);
        w1.setTurn(true);
        w1.setText("draft");
        Writer w2 = writer(20L);
        Game game = createGameWith(List.of(w1, w2), List.of(judge(30L)));
        game.setPhase(GamePhase.WRITING);
        game.setTurnStartedAt(System.currentTimeMillis() - 100000L);
        game.setTimer(90L);
        mockExitDependencies(game, user);
        when(gameRepository.saveAndFlush(any())).thenReturn(game);

        gameService.getGame(1L, "Bearer token");

        assertEquals("", w1.getText());
        assertFalse(w1.getTurn());
        assertTrue(w2.getTurn());
    }

    // ==================== exitGame ====================
    @Test
    void exitGame_writerLeaves_success() {
        User user = user(10L);
        Writer writer = new Writer(user);
        Game game = createGameWith(List.of(writer, writer(20L)), List.of(judge(30L)));
        mockExitDependencies(game, user);

        gameService.exitGame(1L, "Bearer token");

        verify(gameRepository).delete(game);
    }

    @Test
    void exitGame_userNotInGame_throws403() {
        User outsider = user(99L);
        Game game = createGameWith(List.of(writer(10L), writer(20L)), List.of(judge(30L)));
        mockExitDependencies(game, outsider);

        assertThrows(ResponseStatusException.class, () -> gameService.exitGame(1L, "Bearer token"));
    }

    @Test
    void exitGame_judgeLeaves_success() {
        User user = user(10L);
        Judge judge = new Judge(user);
        Game game = createGameWith(List.of(writer(20L), writer(30L)), List.of(judge));
        mockExitDependencies(game, user);

        gameService.exitGame(1L, "Bearer token");

        verify(gameRepository).delete(game);
    }

    // ==================== saveWriterDraft tests ====================
    @Test
    void saveWriterDraft_success() {
        User user = user(10L);
        Writer writer = new Writer(user);
        writer.setTurn(true);
        Game game = createGameWith(List.of(writer, writer(20L)), List.of(judge(30L)));
        mockExitDependencies(game, user);
        when(gameRepository.save(any())).thenReturn(game);

        gameService.saveWriterDraft(1L, "hello", "Bearer token");

        assertEquals("hello", writer.getText());
    }

    @Test
    void saveWriterDraft_notWritersTurn_throws403() {
        User user = user(10L);
        Writer writer = new Writer(user);
        writer.setTurn(false);
        Game game = createGameWith(List.of(writer, writer(20L)), List.of(judge(30L)));
        mockExitDependencies(game, user);

        assertThrows(ResponseStatusException.class, () -> gameService.saveWriterDraft(1L, "hello", "Bearer token"));
    }

    @Test
    void saveWriterDraft_userNotWriter_throws403() {
        User user = user(99L);
        Game game = createGameWith(List.of(writer(10L), writer(20L)), List.of(judge(30L)));
        mockExitDependencies(game, user);

        assertThrows(ResponseStatusException.class, () -> gameService.saveWriterDraft(1L, "hello", "Bearer token"));
    }

    @Test
    void saveWriterDraft_inputTooLong_throws400() {
        User user = user(10L);
        Writer writer = new Writer(user);
        writer.setTurn(true);
        Game game = createGameWith(List.of(writer, writer(20L)), List.of(judge(30L)));
        mockExitDependencies(game, user);

        String longInput = "a".repeat(201);
        assertThrows(ResponseStatusException.class, () -> gameService.saveWriterDraft(1L, longInput, "Bearer token"));
    }

    // ==================== getGameForUser tests ====================
    @Test
    void getGameForUser_userIsWriter_returnsGame() {
        User user = user(10L);
        Writer writer = new Writer(user);
        Game game = createGameWith(List.of(writer, writer(20L)), List.of(judge(30L)));
        when(userService.extractToken(any())).thenReturn("token");
        when(userService.findUserFromToken("token")).thenReturn(user);
        when(gameRepository.findAll()).thenReturn(List.of(game));

        Game result = gameService.getGameForUser("Bearer token");

        assertEquals(game, result);
    }

    @Test
    void getGameForUser_noGame_throws404() {
        User user = user(10L);
        when(userService.extractToken(any())).thenReturn("token");
        when(userService.findUserFromToken("token")).thenReturn(user);
        when(gameRepository.findAll()).thenReturn(List.of());

        assertThrows(ResponseStatusException.class, () -> gameService.getGameForUser("Bearer token"));
    }


}