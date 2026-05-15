package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.constant.GamePhase;
import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.repository.GameRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
public class GameCleanupServiceIntegrationTest {

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private GameCleanupService gameCleanupService;

    @BeforeEach
    @AfterEach
    public void cleanup() {
        gameRepository.deleteAll();
    }

    @Test
    public void deleteGameAndFlush_validGame_deletesFromDatabase() {
        Game game = new Game();
        game.setPhase(GamePhase.WRITING);
        game.setTimer(60L);
        game = gameRepository.saveAndFlush(game);

        Long gameId = game.getId();

        Optional<Game> beforeDelete = gameRepository.findById(gameId);
        assertTrue(beforeDelete.isPresent(), "Game should exist in the database before deletion");

        gameCleanupService.deleteGameAndFlush(game);

        Optional<Game> afterDelete = gameRepository.findById(gameId);
        assertFalse(afterDelete.isPresent(), "Game should be completely deleted from the database");
    }
}