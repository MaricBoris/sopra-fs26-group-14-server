package ch.uzh.ifi.hase.soprafs26.controller;
import ch.uzh.ifi.hase.soprafs26.rest.dto.game.GameInputDTO;

import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.entity.Judge;
import ch.uzh.ifi.hase.soprafs26.entity.Writer;
import ch.uzh.ifi.hase.soprafs26.rest.dto.user.*;
import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.rest.dto.game.GameGetDTO;
import ch.uzh.ifi.hase.soprafs26.entity.Story;
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


   
    @PostMapping("/games/{gameId}/vote")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public GameGetDTO voteGame(@PathVariable Long gameId,
                                @RequestHeader(value = "Authorization") String bearerToken, @RequestBody UserPostDTO userPostVoted) throws InterruptedException {
        Game currentGame = gameService.getGame(gameId);

        String token = bearerToken;
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        User userVoted = DTOMapper.INSTANCE.convertUserPostDTOtoEntity(userPostVoted);
        User userJudge = gameService.findUserFromToken(token);

        Judge judge = gameService.getJudgeFromUser(userJudge, currentGame);
        Writer voted = gameService.getWriterFromUser(userVoted, currentGame);

        gameService.checkGameIsOver(currentGame);

        gameService.addVote(currentGame, voted, judge);

        long waited = 0L;
        while (!gameService.allJudgesVoted(currentGame) && waited < 70) {
            Thread.sleep(1000);
            waited++;
        }

        if(currentGame.getStory().getWinner() != null){
            return DTOMapper.INSTANCE.convertEntityToGameGetDTO(currentGame);
        }

        Writer winner = gameService.determineWinner(currentGame);
        
        gameService.updateStory(winner, currentGame);

        gameService.updateHistory(currentGame);

        gameService.clearVotes(currentGame);
        gameService.cleanupGame(currentGame);

        return DTOMapper.INSTANCE.convertEntityToGameGetDTO(currentGame);
    }


    // 📝 GET /games/current, returns the active game for the authenticated user
    // 📝 used by non-leader players to get gameId after room is dissolved on game start
    @GetMapping("/games/current")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public GameGetDTO getCurrentGame(@RequestHeader(value = "Authorization", required = false) String bearerToken) {
        Game game = gameService.getGameForUser(bearerToken);
        return DTOMapper.INSTANCE.convertEntityToGameGetDTO(game);
    }

    @GetMapping("/games/{gameid}/quotes")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public GameGetDTO fetchQuote(@PathVariable("gameid") Long gameid, @RequestParam("player") Integer player,
            @RequestHeader(value = "Authorization", required = false) String bearerToken) {
        Game game = gameService.assignQuote(gameid, player, bearerToken);
        return DTOMapper.INSTANCE.convertEntityToGameGetDTO(game);
    }
}

