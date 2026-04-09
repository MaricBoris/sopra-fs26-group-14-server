package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.rest.dto.game.GameGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs26.service.GameService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
public class GameController {

    private final GameService gameService;

    GameController(GameService gameService) { this.gameService = gameService; }

    // 📝 GET /games/current, returns the active game for the authenticated user
    // 📝 used by non-leader players to get gameId after room is dissolved on game start
    @GetMapping("/games/current")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public GameGetDTO getCurrentGame(@RequestHeader(value = "Authorization", required = false) String bearerToken) {
        Game game = gameService.getGameForUser(bearerToken);
        return DTOMapper.INSTANCE.convertEntityToGameGetDTO(game);
    }
}
