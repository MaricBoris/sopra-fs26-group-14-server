package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.repository.GameRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.*;

public class GameCleanupServiceTest {

    @Mock
    private GameRepository gameRepository;

    @InjectMocks
    private GameCleanupService gameCleanupService;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void deleteGameAndFlush_deletesAndFlushes() {
        Game game = new Game();
        game.setId(1L);

        gameCleanupService.deleteGameAndFlush(game);

        verify(gameRepository).delete(game);
        verify(gameRepository).flush();
    }
}