package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.entity.Story;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.entity.Writer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class GameStreamServiceTest {

    private GameStreamService gameStreamService;

    @BeforeEach
    public void setup() {
        gameStreamService = new GameStreamService();
    }

    private Game createTestGame() {
        Game game = new Game();
        game.setId(1L);
        game.setTimer(60L);
        game.setTurnStartedAt(System.currentTimeMillis());
        game.setCurrentRound(1);

        User user1 = new User();
        user1.setId(10L);
        user1.setUsername("writer1");
        user1.setToken("token1");
        user1.setPassword("pw");
        user1.setCreationDate(new Date());

        User user2 = new User();
        user2.setId(11L);
        user2.setUsername("writer2");
        user2.setToken("token2");
        user2.setPassword("pw");
        user2.setCreationDate(new Date());

        Writer w1 = new Writer();
        w1.setId(100L);
        w1.setUser(user1);
        w1.setTurn(true);
        w1.setGenre("Fantasy");
        w1.setText("Hello");

        Writer w2 = new Writer();
        w2.setId(101L);
        w2.setUser(user2);
        w2.setTurn(false);
        w2.setGenre("Sci-Fi");
        w2.setText("World");

        List<Writer> writers = new ArrayList<>();
        writers.add(w1);
        writers.add(w2);
        game.setWriters(writers);

        Story story = new Story();
        story.addContribution(1L, "Story text");
        story.setCreationDate(new Date());
        game.setStory(story);

        return game;
    }

    @Test
    public void addClient_validGameId_returnsEmitter() {
        SseEmitter emitter = gameStreamService.addClient(1L);

        assertNotNull(emitter);
    }

    @Test
    public void sendGameToAllClients_validGameWithNoClients_doesNotThrow() {
        Game game = createTestGame();

        assertDoesNotThrow(() -> gameStreamService.sendGameToAllClients(game));
    }

    @Test
    public void sendGameToAllClients_validGameWithClient_doesNotThrow() {
        Game game = createTestGame();
        gameStreamService.addClient(1L);

        assertDoesNotThrow(() -> gameStreamService.sendGameToAllClients(game));
    }

    @Test
    public void sendGameToAllClients_nullGame_doesNotThrow() {
        assertDoesNotThrow(() -> gameStreamService.sendGameToAllClients(null));
    }

    @Test
    public void sendGameDeletedToAllClients_validGameIdWithNoClients_doesNotThrow() {
        assertDoesNotThrow(() -> gameStreamService.sendGameDeletedToAllClients(1L));
    }

    @Test
    public void sendGameDeletedToAllClients_validGameIdWithClient_doesNotThrow() {
        gameStreamService.addClient(1L);

        assertDoesNotThrow(() -> gameStreamService.sendGameDeletedToAllClients(1L));
    }
    @Test
    public void sendGameToAllClients_gameWithoutId_doesNotThrow() {
        Game game = new Game();
        game.setId(null);

        assertDoesNotThrow(() -> gameStreamService.sendGameToAllClients(game));
    }

    // === sendGameToAllClients Exception Branch ===

    @Test
    public void sendGameToAllClients_emitterThrowsException_removesClient() throws Exception {
        Game game = createTestGame();

        SseEmitter mockEmitter = Mockito.mock(SseEmitter.class);
        Mockito.doThrow(new IllegalStateException("Mock exception"))
                .when(mockEmitter).send(Mockito.any(SseEmitter.SseEventBuilder.class));

        @SuppressWarnings("unchecked")
        Map<Long, List<SseEmitter>> gameEmitters = (Map<Long, List<SseEmitter>>) ReflectionTestUtils.getField(gameStreamService, "gameEmitters");
        List<SseEmitter> list = new CopyOnWriteArrayList<>();
        list.add(mockEmitter);
        gameEmitters.put(1L, list);

        gameStreamService.sendGameToAllClients(game);

        assertTrue(gameEmitters.isEmpty());
    }

    // === sendGameDeletedToAllClients Exception Branch ===

    @Test
    public void sendGameDeletedToAllClients_emitterThrowsException_removesClient() throws Exception {
        SseEmitter mockEmitter = Mockito.mock(SseEmitter.class);
        Mockito.doThrow(new IllegalStateException("Mock exception"))
                .when(mockEmitter).send(Mockito.any(SseEmitter.SseEventBuilder.class));

        @SuppressWarnings("unchecked")
        Map<Long, List<SseEmitter>> gameEmitters = (Map<Long, List<SseEmitter>>) ReflectionTestUtils.getField(gameStreamService, "gameEmitters");
        List<SseEmitter> list = new CopyOnWriteArrayList<>();
        list.add(mockEmitter);
        gameEmitters.put(1L, list);

        gameStreamService.sendGameDeletedToAllClients(1L);

        assertTrue(gameEmitters.isEmpty());
    }

    // === removeClient Private Method Branches ===

    @Test
    public void removeClient_emittersIsNull_doesNothing() {
        SseEmitter emitter = new SseEmitter();

        assertDoesNotThrow(() ->
                ReflectionTestUtils.invokeMethod(gameStreamService, "removeClient", 999L, emitter)
        );
    }

    @Test
    public void removeClient_listNotEmpty_keepsMapEntry() {
        SseEmitter emitter1 = gameStreamService.addClient(1L);
        SseEmitter emitter2 = gameStreamService.addClient(1L);

        ReflectionTestUtils.invokeMethod(gameStreamService, "removeClient", 1L, emitter1);

        @SuppressWarnings("unchecked")
        Map<Long, List<SseEmitter>> gameEmitters = (Map<Long, List<SseEmitter>>) ReflectionTestUtils.getField(gameStreamService, "gameEmitters");

        assertNotNull(gameEmitters.get(1L));
        assertEquals(1, gameEmitters.get(1L).size());
        assertEquals(emitter2, gameEmitters.get(1L).get(0));
    }
}
