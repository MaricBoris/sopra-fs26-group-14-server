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

    @PostMapping("/games/{gameid}/draft")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public GameGetDTO saveWriterDraft(
            @PathVariable("gameid") Long gameid,
            @RequestBody GameInputDTO inputDTO,
            @RequestHeader(value = "Authorization", required = false) String bearerToken) {

        Game updatedGame = gameService.saveWriterDraft(gameid, inputDTO.getInput(), bearerToken);
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
                                @RequestHeader(value = "Authorization") String bearerToken, @RequestBody Long writerId) throws InterruptedException {
        System.out.println("=== VOTE ENDPOINT HIT ===");
        System.out.println("gameId: " + gameId);
        System.out.println("bearerToken: " + bearerToken);
        System.out.println("writerId: " + writerId);


        Game currentGame = gameService.getGame(gameId);
        System.out.println("1. Got game, phase: " + currentGame.getPhase() + ", timer: " + currentGame.getTimer());

        String token = bearerToken;
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        Writer voted = gameService.findWriterFromId(writerId, currentGame);
        System.out.println("2. Found voted writer, id: " + voted.getId());

        User userJudge = gameService.findUserFromToken(token);
        System.out.println("3. Found judge, id: " + userJudge.getId());

        Judge judge = gameService.getJudgeFromUser(userJudge, currentGame);
        System.out.println("4. Got judge entity");

        gameService.checkGameIsOver(currentGame);
        System.out.println("5. Game over check passed");

        gameService.addVote(currentGame, voted, judge);
        System.out.println("6. Vote added, entering wait loop");

        long waited = 0L;
        while (!gameService.allJudgesVoted(currentGame) && waited < 70) {
            Thread.sleep(1000);
            waited++;
        }
        System.out.println("7. Wait loop done, waited " + waited + "s");

        if(currentGame.getStory().getWinner() != null){
            System.out.println("8a. Winner already set, returning");
            return DTOMapper.INSTANCE.convertEntityToGameGetDTO(currentGame);
        }

        Writer winner = gameService.determineWinner(currentGame);
        System.out.println("8b. Winner determined, id: " + winner.getId());

        gameService.updateStory(winner, currentGame);
        System.out.println("9. Story updated");

        gameService.updateHistory(currentGame);
        System.out.println("10. History updated");

        gameService.clearVotes(currentGame);
        System.out.println("11. Votes cleared");

        gameService.cleanupGame(currentGame);
        System.out.println("12. Game cleaned up, returning");

        return DTOMapper.INSTANCE.convertEntityToGameGetDTO(currentGame);
    }


    @DeleteMapping("/games/{gameId}")
    @ResponseStatus(HttpStatus.OK)
    public void deleteGame(@PathVariable Long gameId,
                            @RequestHeader(value = "Authorization") String bearerToken) {
        Game currentGame = gameService.getGame(gameId);
        gameService.deleteGame(currentGame);
    }

    // 📝 GET /games/current, returns the active game for the authenticated user
    // 📝 used by non-leader players to get gameId after room is dissolved on game start
    @GetMapping("/games/current")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public GameGetDTO getCurrentGame(@RequestHeader(value = "Authorization") String bearerToken) {
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

