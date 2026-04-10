package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.entity.Story;
import ch.uzh.ifi.hase.soprafs26.entity.*;
import ch.uzh.ifi.hase.soprafs26.repository.GameRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Service
@Transactional
public class GameService {

    private final GameRepository gameRepository;
    private final UserRepository userRepository;

    @Autowired
    public GameService( @Qualifier("gameRepository") GameRepository gameRepository, UserRepository userRepository) {
        this.gameRepository = gameRepository;
        this.userRepository = userRepository;
    }



    private Map<Long, Map<Judge, Writer>> gameVotes = new HashMap<>();

    public synchronized void addVote(Game currentGame, Writer voted, Judge judge) {
        gameVotes.computeIfAbsent(currentGame.getId(), k -> new HashMap<>()).put(judge, voted);
    }

    public boolean allJudgesVoted(Game currentGame) {
        Map<Judge, Writer> votes = gameVotes.getOrDefault(currentGame.getId(), new HashMap<>());
        return votes.size() >= currentGame.getJudges().size();
    }

    public void clearVotes(Game currentGame) {
        gameVotes.remove(currentGame.getId());
    }

    public Writer determineWinner(Game currentGame) {
        Map<Judge, Writer> votes = gameVotes.get(currentGame.getId());
        if (votes == null || votes.isEmpty()) {return null;}
        Map<Writer, Integer> voteCounts = new HashMap<>();
        for (Writer writer : votes.values()) {
            voteCounts.merge(writer, 1, Integer::sum);
        }

        Writer winner = null;
        int maxVotes = 0;
        boolean tie = false;

        for (Map.Entry<Writer, Integer> entry : voteCounts.entrySet()) {
            if (entry.getValue() > maxVotes) {
                maxVotes = entry.getValue();
                winner = entry.getKey();
                tie = false;
            } else if (entry.getValue() == maxVotes) {
                tie = true;
            }
        }

        if (tie || winner == null) return null;
        return winner;
    }

    public Game getGame(Long gameId){
        String baseErrorMessage = "Error: The provided id: %s is invalid and doesn't match any game.";
		Game gameById = gameRepository.findById(gameId).orElseThrow(() ->  
		new ResponseStatusException(HttpStatus.NOT_FOUND, String.format(baseErrorMessage, gameId))); //NOT_FOUND 404

		return gameById; 
    }

    public Judge getJudgeFromUser(User userJudge, Game currentGame){
        String baseErrorMessage = "Error: You are not allowed to vote for this game.";
        for (Judge judge : currentGame.getJudges()){
            if(userJudge.getId().equals(judge.getUser().getId())){
                return judge;
            }
        } 
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, baseErrorMessage);
    }


    public Writer getWriterFromUser(User userWriter, Game currentGame){
        String baseErrorMessage = "Error: You are not allowed to vote for a non writer.";
        for (Writer writer : currentGame.getWriters()){
            if(userWriter.getId().equals(writer.getUser().getId())){
                return writer;
            }
        }  
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, baseErrorMessage);
    }


    public User findUserFromToken(String token) {
       
		User userByToken = userRepository.findByToken(token);

		String baseErrorMessage = "Error: You are not Authorized.";
		if (userByToken == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, baseErrorMessage);
		}

		return userByToken;
	}

    public void checkGameIsOver(Game currentGame){
        String baseErrorMessage = "Error: The game is not over.";
        if(!currentGame.getTimer().equals(0L)){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, baseErrorMessage); 
        }
    }

    public Story updateStory(Writer winner, Game currentGame){
        Boolean hasWinner = false;
        Writer loser = null;
        if (winner == null){
            winner = currentGame.getWriters().get(0);
            loser = currentGame.getWriters().get(1);
        }
        else{
            hasWinner = true;
            loser = currentGame.getWriters().get(0).getId().equals(winner.getId())
            ? currentGame.getWriters().get(1)
            : currentGame.getWriters().get(0);
        }

        List<User> judgeUsers = new ArrayList<>();
        for (Judge judge : currentGame.getJudges()) {
            judgeUsers.add(judge.getUser());
        }

        Story newStory = new Story (winner.getUser(), loser.getUser(), currentGame.getStory().getStoryText(), hasWinner, winner.getGenre(), loser.getGenre(), judgeUsers);

        currentGame.setStory(newStory);

        gameRepository.save(currentGame);

        return newStory;
    }

    public void updateHistory(Game currentGame){
        for (Writer writer: currentGame.getWriters()){
            writer.getUser().addStory(currentGame.getStory());
            userRepository.save(writer.getUser());
        }
        for (Judge judge: currentGame.getJudges()){
            judge.getUser().addStory(currentGame.getStory());
            userRepository.save(judge.getUser());
        }
    } 

    public void cleanupGame(Game currentGame) {
    currentGame.getWriters().clear();
    currentGame.getJudges().clear();
    gameRepository.delete(currentGame);
    }
}