package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.constant.GamePhase;
import ch.uzh.ifi.hase.soprafs26.entity.*;
import ch.uzh.ifi.hase.soprafs26.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs26.repository.RoomRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@WebAppConfiguration
@SpringBootTest
@Transactional
public class GameServiceIntegrationTest {

    @Qualifier("gameRepository")
    @Autowired
    private GameRepository gameRepository;

    @Qualifier("roomRepository")
    @Autowired
    private RoomRepository roomRepository;

    @Qualifier("userRepository")
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GameService gameService;

    @Autowired
    private UserService userService;

    @Autowired
    private StatsAchvsService statsAchvsService;

    private User user1;
    private User user2;
    private User user3;

    @BeforeEach
    public void setup() {
        for (User user : userRepository.findAll()) {
            user.getHistory().clear();
            userRepository.save(user);
        }
        gameRepository.deleteAll();
        roomRepository.deleteAll();
        userRepository.deleteAll();

        user1 = new User();
        user1.setUsername("judgeUser");
        user1.setPassword("password");
        user1 = userService.createUser(user1);

        user2 = new User();
        user2.setUsername("writerUser1");
        user2.setPassword("password");
        user2 = userService.createUser(user2);

        user3 = new User();
        user3.setUsername("writerUser2");
        user3.setPassword("password");
        user3 = userService.createUser(user3);
    }

    // --- getGame ---

    @Test
    public void getGame_validId_returnsGame() {
        Game game = new Game();
        game = gameRepository.save(game);

        Game found = gameService.getGame(game.getId());

        assertNotNull(found);
        assertEquals(game.getId(), found.getId());
    }

    @Test
    public void getGame_invalidId_throws404() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> gameService.getGame(999L));
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    // --- findUserFromToken ---

    @Test
    public void findUserFromToken_validToken_returnsUser() {
        User found = gameService.findUserFromToken(user1.getToken());

        assertNotNull(found);
        assertEquals(user1.getId(), found.getId());
    }

    @Test
    public void findUserFromToken_invalidToken_throws401() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> gameService.findUserFromToken("fake-token"));
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    }

    // --- getJudgeFromUser ---

    @Test
    public void getJudgeFromUser_userIsJudge_returnsJudge() {
        Judge judge = new Judge(user1);

        Game game = new Game();
        game.setJudges(List.of(judge));
        game = gameRepository.save(game);

        Judge found = gameService.getJudgeFromUser(user1, game);

        assertNotNull(found);
        assertEquals(user1.getId(), found.getUser().getId());
    }

    @Test
    public void getJudgeFromUser_userIsNotJudge_throws403() {
        Judge judge = new Judge(user1);

        Game game = new Game();
        game.setJudges(List.of(judge));
        game = gameRepository.save(game);

        final Game savedGame = game;
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> gameService.getJudgeFromUser(user2, savedGame));
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    // --- getWriterFromUser ---

    @Test
    public void getWriterFromUser_userIsWriter_returnsWriter() {
        Writer writer = new Writer();
        writer.setUser(user2);

        Game game = new Game();
        game.setWriters(List.of(writer));
        game = gameRepository.save(game);

        Writer found = gameService.getWriterFromUser(user2, game);

        assertNotNull(found);
        assertEquals(user2.getId(), found.getUser().getId());
    }

    @Test
    public void getWriterFromUser_userIsNotWriter_throws400() {
        Writer writer = new Writer();
        writer.setUser(user2);

        Game game = new Game();
        game.setWriters(List.of(writer));
        game = gameRepository.save(game);

        final Game savedGame = game;
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> gameService.getWriterFromUser(user3, savedGame));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    // --- checkGameIsOver ---

    @Test
    public void checkGameIsOver_timerZero_noException() {
        Game game = new Game();
        game.setTimer(0L);
        game.setPhase(GamePhase.EVALUATION);
        game = gameRepository.save(game);

        final Game savedGame = game;
        assertDoesNotThrow(() -> gameService.checkGameIsOver(savedGame));
    }

    @Test
    public void checkGameIsOver_timerNotZero_throws400() {
        Game game = new Game();
        game.setTimer(60L);
        game = gameRepository.save(game);

        final Game savedGame = game;
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> gameService.checkGameIsOver(savedGame));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    // --- updateStory ---

    @Test
    public void updateStory_createsAndPersistsStory() {
        Writer winner = new Writer();
        winner.setUser(user2);
        winner.setGenre("Horror");

        Writer loser = new Writer();
        loser.setUser(user3);
        loser.setGenre("Comedy");

        Judge judge = new Judge(user1);

        Story initialStory = new Story(null, null, new ArrayList<>(), false, null, null, new ArrayList<>());


        Game game = new Game();
        game.setWriters(List.of(winner, loser));
        game.setJudges(List.of(judge));
        game.setStory(initialStory);
        game = gameRepository.save(game);

        Story result = gameService.updateStory(winner, game);

        assertNotNull(result);
        assertEquals(user2, result.getWinner());
        assertEquals(user3, result.getLoser());
        assertEquals("Horror", result.getWinGenre());
        assertEquals("Comedy", result.getLoseGenre());

        Game reloaded = gameRepository.findById(game.getId()).orElse(null);
        assertNotNull(reloaded);
        assertNotNull(reloaded.getStory().getWinner());
    }

    // --- updateHistory ---

    @Test
    public void updateHistory_addsStoryToAllPlayers() {
        Writer writer = new Writer();
        writer.setUser(user2);

        Judge judge = new Judge(user1);

        Story story = new Story(null, null, new ArrayList<>(), false, null, null, new ArrayList<>());

        Game game = new Game();
        game.setWriters(List.of(writer));
        game.setJudges(List.of(judge));
        game.setStory(story);
        game = gameRepository.save(game);

        gameService.updateHistory(game);

        User reloadedWriter = userRepository.findById(user2.getId()).orElse(null);
        User reloadedJudge = userRepository.findById(user1.getId()).orElse(null);

        assertNotNull(reloadedWriter);
        assertNotNull(reloadedJudge);
        assertFalse(reloadedWriter.getHistory().isEmpty());
        assertFalse(reloadedJudge.getHistory().isEmpty());
    }

        // --- determineWinner with no votes ---

    @Test
    public void determineWinner_noVotes_returnsNull() {
        Game game = new Game();
        game = gameRepository.save(game);

        Writer winner = gameService.determineWinner(game);
        assertNull(winner);
    }

    // --- cleanupGame ---

    @Test
    public void cleanupGame_deletesGameAndClearsLists() {
        Writer writer = new Writer();
        writer.setUser(user2);

        Judge judge = new Judge(user1);

        Game game = new Game();
        game.setWriters(new ArrayList<>(List.of(writer)));
        game.setJudges(new ArrayList<>(List.of(judge)));
        game = gameRepository.save(game);

        Long gameId = game.getId();

        gameService.cleanupGame(game);

        Game result = gameRepository.findById(gameId).orElseThrow();
        assertEquals(GamePhase.FINISHED, result.getPhase());
    }

}