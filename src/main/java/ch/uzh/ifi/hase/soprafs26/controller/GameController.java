package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.rest.dto.game.GameGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.game.GameInputDTO;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs26.service.GameService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;


@RestController
public class GameController {

    private final GameService gameService;

    GameController(GameService gameService) { 
        this.gameService = gameService; 
    }

    @GetMapping("/games/{gameid}")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public GameGetDTO getGame(
            @PathVariable("gameid") Long gameid,
            @RequestHeader(value = "Authorization", required = false) String bearerToken) {

        Game game = gameService.getGame(gameid, bearerToken);
        return DTOMapper.INSTANCE.convertEntityToGameGetDTO(game);
    }

    @PostMapping("/games/{gameid}/input")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public GameGetDTO saveWriterInput(
            @PathVariable("gameid") Long gameid,
            @RequestBody GameInputDTO inputDTO,
            @RequestHeader(value = "Authorization", required = false) String bearerToken) {

        Game updatedGame = gameService.insertWriterInput(gameid, inputDTO.getPlayer(),inputDTO.getInput(), bearerToken);

        return DTOMapper.INSTANCE.convertEntityToGameGetDTO(updatedGame);
    }

    @PostMapping("/games/{gameid}/leave")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public void exitGame(
            @PathVariable("gameid") Long gameid,
            @RequestHeader(value = "Authorization", required = false) String bearerToken) {

        gameService.exitGame(gameid, bearerToken);
    }
}