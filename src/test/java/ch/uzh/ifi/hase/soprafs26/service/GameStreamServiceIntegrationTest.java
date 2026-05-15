package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.entity.Game;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class GameStreamServiceIntegrationTest {

    @Autowired
    private GameStreamService gameStreamService;

    @BeforeEach
    public void setup() {
        @SuppressWarnings("unchecked")
        Map<Long, List<SseEmitter>> gameEmitters = (Map<Long, List<SseEmitter>>) ReflectionTestUtils.getField(gameStreamService, "gameEmitters");
        if (gameEmitters != null) {
            gameEmitters.clear();
        }
    }

    @Test
    public void contextLoads_serviceIsInjected() {
        assertNotNull(gameStreamService, "GameStreamService should be injected by Spring context");
    }

    @Test
    public void addClient_createsNewEmitterAndStoresIt() {
        Long gameId = 100L;

        SseEmitter emitter = gameStreamService.addClient(gameId);

        assertNotNull(emitter);

        @SuppressWarnings("unchecked")
        Map<Long, List<SseEmitter>> gameEmitters = (Map<Long, List<SseEmitter>>) ReflectionTestUtils.getField(gameStreamService, "gameEmitters");

        assertNotNull(gameEmitters);
        assertTrue(gameEmitters.containsKey(gameId));
        assertEquals(1, gameEmitters.get(gameId).size());
        assertEquals(emitter, gameEmitters.get(gameId).get(0));
    }

    @Test
    public void sendGameToAllClients_validGame_sendsThroughMapperWithoutCrashing() {
        Long gameId = 200L;
        Game game = new Game();
        game.setId(gameId);

        gameStreamService.addClient(gameId);

        assertDoesNotThrow(() -> gameStreamService.sendGameToAllClients(game));

        @SuppressWarnings("unchecked")
        Map<Long, List<SseEmitter>> gameEmitters = (Map<Long, List<SseEmitter>>) ReflectionTestUtils.getField(gameStreamService, "gameEmitters");

        assertTrue(gameEmitters.containsKey(gameId));
        assertEquals(1, gameEmitters.get(gameId).size());
    }

    @Test
    public void sendGameToAllClients_nullGameOrId_abortsGracefully() {
        Long gameId = 300L;
        gameStreamService.addClient(gameId);

        assertDoesNotThrow(() -> gameStreamService.sendGameToAllClients(null));

        Game gameWithoutId = new Game();
        assertDoesNotThrow(() -> gameStreamService.sendGameToAllClients(gameWithoutId));
    }

    @Test
    public void sendGameDeletedToAllClients_cleansUpMapAndCompletesEmitters() {
        Long gameId = 400L;

        gameStreamService.addClient(gameId);
        gameStreamService.addClient(gameId);

        @SuppressWarnings("unchecked")
        Map<Long, List<SseEmitter>> gameEmitters = (Map<Long, List<SseEmitter>>) ReflectionTestUtils.getField(gameStreamService, "gameEmitters");

        assertTrue(gameEmitters.containsKey(gameId));
        assertEquals(2, gameEmitters.get(gameId).size());

        assertDoesNotThrow(() -> gameStreamService.sendGameDeletedToAllClients(gameId));

        assertFalse(gameEmitters.containsKey(gameId), "The game ID should be completely removed from the map");
    }
}