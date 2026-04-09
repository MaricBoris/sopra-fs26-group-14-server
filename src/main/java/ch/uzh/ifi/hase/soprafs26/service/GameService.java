package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.GameRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class GameService {

    private final GameRepository gameRepository;
    private final UserService userService;

    @Autowired
    public GameService(GameRepository gameRepository, UserService userService) {
        this.gameRepository = gameRepository;
        this.userService = userService;
    }

    // 📝 find the active game for the authenticated user (as writer or judge)
    public Game getGameForUser(String bearerToken) {
        User user = userService.findUserFromToken(userService.extractToken(bearerToken));

        return gameRepository.findAll().stream()
                .filter(g ->
                    g.getWriters().stream().anyMatch(w -> w.getUser().getId().equals(user.getId())) ||
                    g.getJudges().stream().anyMatch(j -> j.getUser().getId().equals(user.getId()))
                )
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No active game found for this user"));
    }
}
