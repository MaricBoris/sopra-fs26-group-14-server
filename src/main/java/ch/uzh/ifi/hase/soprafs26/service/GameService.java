package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs26.entity.*;
import ch.uzh.ifi.hase.soprafs26.repository.GameRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;


@Service
@Transactional
public class GameService {

    private final GameRepository gameRepository;

    private final UserRepository userRepository;
    private final UserService userService;

    private final GameCleanupService gameCleanupService;
    private final QuoteService quoteService;

    @Autowired
    public GameService(GameRepository gameRepository, UserService userService, UserRepository userRepository, QuoteService quoteService, GameCleanupService gameCleanupService) {
        this.gameRepository = gameRepository;
        this.userService = userService;
        this.userRepository=userRepository;
        this.quoteService = quoteService;
        this.gameCleanupService = gameCleanupService;
    }

    public Game getGame(Long id, String bearerToken) {
        String token = userService.extractToken(bearerToken);
        Game playedGame=getandCheckGame(id, token);
        User requestingUser=getandCheckUser(token);
        boolean partOfGame=false;
        long now = System.currentTimeMillis();
        long timeoutMillis = 15000L;
        for (Writer writer : playedGame.getWriters()) {
            if(writer.getUser().getId().equals(requestingUser.getId())){
                partOfGame=true;
                writer.setLastSeenAt(now);
            }
        }
        for (Judge judge : playedGame.getJudges()) {
            if(judge.getUser().getId().equals(requestingUser.getId())){
                partOfGame=true;
                judge.setLastSeenAt(now);
            }
        }
        if (!partOfGame){
           throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User not part of game"); //Check 403 
        }
       
        
        for (Writer writer : playedGame.getWriters()) {
            if (now - writer.getLastSeenAt() > timeoutMillis) {
                gameCleanupService.deleteGameAndFlush(playedGame); //we need to outsource this because of transactional that would rollback the whole thing after the exception
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Game ended because a writer disconnected");
                
            }
        }  
        List<Judge> disconnectedJudges = new ArrayList<>();
        for (Judge judge : playedGame.getJudges()) {
            if (now - judge.getLastSeenAt() > timeoutMillis) {
               disconnectedJudges.add(judge);
            }
        }
        if (!disconnectedJudges.isEmpty()) {
            playedGame.getJudges().removeAll(disconnectedJudges);
            if ( playedGame.getJudges().size()<1 ){
            gameCleanupService.deleteGameAndFlush(playedGame);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Game ended because judge disconnected");
            }
            else{
                gameRepository.save(playedGame);
            }
        }
        if (playedGame.getWriters().size()!=2 || playedGame.getJudges().size()!=1 ){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Erroneous Game State"); //Check 400
        }
        resolveExpiredTurnIfNeeded(playedGame);
        gameRepository.saveAndFlush(playedGame);
        return playedGame;
    }

    private void resolveExpiredTurnIfNeeded(Game playedGame) {
        if (playedGame == null) return;
        if (playedGame.getPhase() != GamePhase.WRITING) return;
        if (playedGame.isRoundResolved()) return;
        if (playedGame.getTurnStartedAt() == null || playedGame.getTimer() == null) return;

        long now = System.currentTimeMillis();
        long turnEndsAt = playedGame.getTurnStartedAt() + playedGame.getTimer() * 1000;

        if (now < turnEndsAt) {
            return;
        }

        Writer activeWriter = null;
        for (Writer writer : playedGame.getWriters()) {
            if (writer.getTurn()) {
                activeWriter = writer;
                break;
            }
        }

        if (activeWriter == null) {
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "No active writer found"
            );
        }
        activeWriter.setText("");

        playedGame.setRoundResolved(true);
        playedGame.nextRound();

        gameRepository.saveAndFlush(playedGame);
    }

    public Game insertWriterInput(Long id, Integer player, String inputText, String bearerToken) {

        // currently player turned out to be useless, but maybe it's useful later, so decided to keep it

        String token = userService.extractToken(bearerToken);
        Game playedGame = getandCheckGame(id, token);
        User requestingUser = getandCheckUser(token);

        // Falls der Turn inzwischen durch Zeitablauf vorbei ist, erst serverseitig auflösen
        resolveExpiredTurnIfNeeded(playedGame);

        if (playedGame.getPhase() != GamePhase.WRITING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Users may not write anymore");
        }

        if (playedGame.isRoundResolved()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Current round already resolved");
        }

        Writer requestingWriter = null;
        for (Writer writer : playedGame.getWriters()) {
            if (writer.getUser().getId().equals(requestingUser.getId())) {
                requestingWriter = writer;
                break;
            }
        }

        if (requestingWriter == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not a writer in game");
        }

        if (!requestingWriter.getTurn()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "It's not this writers turn!");
        }

        String prettyInput = (inputText == null) ? "" : inputText.trim();

        Story story = playedGame.getStory();
        if (story == null) {
            story = new Story();
            playedGame.setStory(story);
        }

        String currentStory = story.getStoryText();
        if (!prettyInput.isBlank()) {
            if (currentStory == null || currentStory.isBlank()) {
                story.setStoryText(prettyInput);
            } else {
                story.setStoryText(currentStory + " " + prettyInput);
            }
        }

        requestingWriter.setText("");
        playedGame.setRoundResolved(true);
        playedGame.nextRound();

        return gameRepository.saveAndFlush(playedGame);
    }
    public void exitGame(Long id, String bearerToken) {
        String token = userService.extractToken(bearerToken);
        Game playedGame=getandCheckGame(id, token);
        User requestingUser=getandCheckUser(token);

        boolean partOfGame = false;
        Writer writerToRemove = null;

        for (Writer writer : playedGame.getWriters()) {
            if (writer.getUser().getId().equals(requestingUser.getId())) {
                partOfGame = true;
                writerToRemove = writer;
                break;
            }
        }

        if (writerToRemove != null) {
            playedGame.getWriters().remove(writerToRemove);
        }

        Judge judgeToRemove = null;

        for (Judge judge : playedGame.getJudges()) {
            if (judge.getUser().getId().equals(requestingUser.getId())) {
                partOfGame = true;
                judgeToRemove = judge;
                break;
            }
        }

        if (judgeToRemove != null) {
            playedGame.getJudges().remove(judgeToRemove);
        }
        if (!partOfGame){
           throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User not part of game"); //Check 403 
        }

        if (playedGame.getWriters().size()<2 || playedGame.getJudges().size()<1 ){
            gameRepository.delete(playedGame);
            gameRepository.flush();
        }
        else{
            gameRepository.save(playedGame);
        }
    }

        public Game saveWriterDraft(Long id, String inputText, String bearerToken) {
        String token = userService.extractToken(bearerToken);
        Game playedGame = getandCheckGame(id, token);
        User requestingUser = getandCheckUser(token);

        Writer requestingWriter = null;
        for (Writer writer : playedGame.getWriters()) {
            if (writer.getUser().getId().equals(requestingUser.getId())) {
                requestingWriter = writer;
                break;
            }
        }

        if (requestingWriter == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not a writer in game");
        }

        if (!requestingWriter.getTurn()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "It's not this writers turn!");
        }

        String prettyInput = (inputText == null) ? "" : inputText;

        if (prettyInput.length() > 200) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Input too long");
        }

        requestingWriter.setText(prettyInput);
        gameRepository.save(playedGame);
        return playedGame;
    }

    public Game getandCheckGame(Long id, String token){
     Game playedGame= gameRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, //Check 404
                "Error: A game with that id could not be found"
            ));
         if (token==null || token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token"); //Check 401
        }
        return playedGame;
    }
    public User getandCheckUser(String token){
        User requestingUser = userRepository.findByToken(token);
        if (requestingUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token"); //Check 401
        }
        return requestingUser;
    }




    private Map<Long, Map<Judge, Writer>> gameVotes = new HashMap<>();


    public Game getGame(Long gameId){
        String baseErrorMessage = "Error: The provided id: %s is invalid and doesn't match any game.";
		Game gameById = gameRepository.findById(gameId).orElseThrow(() ->  
		new ResponseStatusException(HttpStatus.NOT_FOUND, String.format(baseErrorMessage, gameId))); //NOT_FOUND 404

		return gameById; 
    }

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

    public Judge getJudgeFromUser(User userJudge, Game currentGame){
        String baseErrorMessage = "Error: You are not allowed to vote for this game.";
        for (Judge judge : currentGame.getJudges()){
            if(userJudge.getId().equals(judge.getUser().getId())){
                return judge;
            }
        } 
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, baseErrorMessage);
    }


    public Writer findWriterFromId(Long id, Game currentGame){
        String baseErrorMessage = "Error: You are not allowed to vote for a non writer.";
        for (Writer writer : currentGame.getWriters()){
            if(writer.getId().equals(id)){
                return writer;
            }
        }  
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, baseErrorMessage);
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
    if(!currentGame.getPhase().equals(GamePhase.EVALUATION)){
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
        currentGame.setPhase(GamePhase.FINISHED);
        gameRepository.save(currentGame);
    }

    public void deleteGame(Game currentGame) {
        currentGame.getWriters().clear();
        currentGame.getJudges().clear();
        currentGame.setStory(null);  
        gameRepository.save(currentGame);
        gameRepository.delete(currentGame);
        gameRepository.flush();
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

    public Game assignQuote(Long id, Integer player, String bearerToken) {
        String token = userService.extractToken(bearerToken);
        Game playedGame = getandCheckGame(id, token);
        User requestingUser = getandCheckUser(token);

        getJudgeFromUser(requestingUser, playedGame); // throws 403 if not a judge

        String quote = quoteService.fetchRandomQuote();
        if (quote == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to fetch quote from external API");
        }

        Writer targetWriter = playedGame.getWriters().get(player - 1);
        targetWriter.setQuote(quote);

        gameRepository.saveAndFlush(playedGame);
        return playedGame;
    }
}

