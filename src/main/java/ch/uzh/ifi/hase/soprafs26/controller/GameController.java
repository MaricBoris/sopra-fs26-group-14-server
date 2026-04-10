package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.entity.Judge;
import ch.uzh.ifi.hase.soprafs26.entity.Writer;
import ch.uzh.ifi.hase.soprafs26.entity.Story;
import ch.uzh.ifi.hase.soprafs26.rest.dto.user.*;
import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.entity.Room;
import ch.uzh.ifi.hase.soprafs26.rest.dto.game.GameGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.room.RoomGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.room.RoomPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.room.RoomRoleDTO;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs26.service.GameService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
public class GameController {

    private final GameService gameService;

    GameController(GameService gameService) { this.gameService = gameService; }

   
    @PostMapping("/games/{gameId}/vote")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public GameGetDTO voteGame(@PathVariable Long gameId,
                                @RequestHeader(value = "Authorization") String bearerToken, UserPostDTO userPostVoted) throws InterruptedException {
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

        Long waited = 0L;
        while (!gameService.allJudgesVoted(currentGame) && waited < 70) {
            Thread.sleep(1000);
            waited++;
        }

        if(currentGame.getStory().getWinner() != null){
            return DTOMapper.INSTANCE.convertEntityToGameGetDTO(currentGame);
        }

        Writer winner = gameService.determineWinner(currentGame);
        
        Story currentStory = gameService.updateStory(winner, currentGame);

        gameService.updateHistory(currentGame);

        gameService.cleanupGame(currentGame);

        return DTOMapper.INSTANCE.convertEntityToGameGetDTO(currentGame);
    }

}