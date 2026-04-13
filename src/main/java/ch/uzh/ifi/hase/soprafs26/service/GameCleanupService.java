package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.repository.GameRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GameCleanupService {

    private final GameRepository gameRepository;

    public GameCleanupService(GameRepository gameRepository) {
        this.gameRepository = gameRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteGameAndFlush(Game playedGame) {
        gameRepository.delete(playedGame);
        gameRepository.flush();
    }
}