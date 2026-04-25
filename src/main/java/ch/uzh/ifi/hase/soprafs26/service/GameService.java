package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.constant.GamePhase;
import ch.uzh.ifi.hase.soprafs26.repository.StoryRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs26.entity.*;
import ch.uzh.ifi.hase.soprafs26.repository.GameRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;


@Service
@Transactional
public class GameService {

    private final GameRepository gameRepository;
    private final StoryRepository storyRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final GameStreamService gameStreamService;
    private final GameCleanupService gameCleanupService;
    private final QuoteService quoteService;

    
    private final List<String> abbreviations = new ArrayList<>(List.of( "z.b", "bzw", "usw", "etc", "d.h", "u.a", "ca", "vgl",
    "dr", "prof", "hr", "fr", "nr", "bspw", "evtl", "ggf", "inkl", "jr", "sr", "st", "mio", "mrd", "mr", "mrs", "ms", "vs", "e.g", "i.e"));

    @Autowired
    public GameService(GameRepository gameRepository, UserService userService, UserRepository userRepository, StoryRepository storyRepository, QuoteService quoteService, GameCleanupService gameCleanupService, GameStreamService gameStreamService) {
        this.gameRepository = gameRepository;
        this.storyRepository = storyRepository;
        this.userService = userService;
        this.userRepository=userRepository;
        this.quoteService = quoteService;
        this.gameCleanupService = gameCleanupService;
        this.gameStreamService = gameStreamService;
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
       
        
        //checkIfPlayerDisconnected(playedGame, timeoutMillis, now);
        if (playedGame.getWriters().size()!=2 || playedGame.getJudges().isEmpty()){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Erroneous Game State"); //Check 400
        }
        resolveExpiredTurnIfNeeded(playedGame);
        gameRepository.saveAndFlush(playedGame);
        return playedGame;
    }

    private void checkIfPlayerDisconnected(Game playedGame, Long timeoutMillis, Long now){
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
            if (playedGame.getJudges().isEmpty()){
            gameCleanupService.deleteGameAndFlush(playedGame);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Game ended because judge disconnected");
            }
            else{
                gameRepository.save(playedGame);
            }
        }
    }
      
    private String truncateToLastSentence(String input) {
        if (input == null) return "";
        String trimmedInput = input.trim();
        if (trimmedInput.isEmpty()) return "";

        //go through the characters from end to beginning to search for the last proper sentence ending 
        int i = trimmedInput.length() - 1;
        while (i >= 0) {

            char c = trimmedInput.charAt(i);

            if (c == '!' || c == '?') {
                return trimmedInput.substring(0, i + 1).trim();
            }

            if (c == '.') {

                //check for a ... ending
                if (i >= 2 && trimmedInput.charAt(i - 1) == '.' && trimmedInput.charAt(i - 2) == '.') {
                    return trimmedInput.substring(0, i + 1).trim();
                }

                // if theres a letter before and right after the ., were gonna consider it part of an abbreviation and not a sentence ending
                boolean beforeIsLetter = (i > 0) && Character.isLetter(trimmedInput.charAt(i - 1));
                boolean afterIsLetter = (i < (trimmedInput.length() - 1)) && Character.isLetter(trimmedInput.charAt(i + 1));
                if (beforeIsLetter && afterIsLetter) {
                    i--;
                    continue;
                }

                //scan for known abbreviations like Dr. or etc. 
                int potentialAbbrevStart = i - 1;
                while ( (potentialAbbrevStart >= 0) && (Character.isLetter(trimmedInput.charAt(potentialAbbrevStart)) || trimmedInput.charAt(potentialAbbrevStart) == '.')) {
                    potentialAbbrevStart--;
                }  
                potentialAbbrevStart++; //if we would stop at >0, then we wouldn't check, if char at 0 is even a letter or .
                String potentialAbbrev = trimmedInput.substring(potentialAbbrevStart, i).toLowerCase();
                if (abbreviations.contains(potentialAbbrev)) {
                    i=potentialAbbrevStart;
                    continue;
                }

                return trimmedInput.substring(0, i + 1).trim();
            }

            i--;
        }

        //in case we did not find a potential sentence ending, we don't want to return half a sentence
        return "";
    }

    // helper method for both manual and auto submit of writer input
    private void addInputToStory(Game playedGame, Writer writer, String input) {
        String clean = (input == null) ? "" : input.trim();

        // enforce maximum writer input length
        if (clean.length() > 2000) {
            clean = clean.substring(0, 2000);
            clean=truncateToLastSentence(clean);
        }

        Story story = playedGame.getStory();
        if (story == null) {
            story = new Story();
            playedGame.setStory(story);
        }
        
        if(story.getStoryText()==null){
            story.setStoryText("");
        }
        if (!clean.isBlank()) {
            String currentStory = story.getStoryText();
            if (currentStory.isBlank()) {
                story.setStoryText(clean);
            } else {
                story.setStoryText(currentStory + " " + clean);
            }
        }

        writer.setText("");
        playedGame.setRoundResolved(true);
        playedGame.nextRound();
    }

    private void resolveExpiredTurnIfNeeded(Game playedGame) {
        if (playedGame == null) return;
        if (playedGame.getPhase() != GamePhase.WRITING) return;
        if (playedGame.isRoundResolved()) return;
        if ( (playedGame.getTurnStartedAt() == null) || (playedGame.getTimer() == null)) return;

        //check if timer for this turn already expired
        long now = System.currentTimeMillis();
        long turnEndsAt = playedGame.getTurnStartedAt() + playedGame.getTimer() * 1000;
        if (now < turnEndsAt) return;

        //search for the active writer in this game 
        Writer activeWriter = null;
        for (Writer writer : playedGame.getWriters()) {
            if (writer.getTurn()) {
                activeWriter = writer;
                break;
            }
        }
        if (activeWriter == null) {
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR, "No active writer found");
        }

        // shorten writers draft to the last sentence ending
        String inputWithProperEnding = truncateToLastSentence(activeWriter.getText());
        addInputToStory(playedGame, activeWriter, inputWithProperEnding);

        gameRepository.saveAndFlush(playedGame);
    }

    public Game insertWriterInput(Long id, Integer player, String inputText, String bearerToken) {

        // currently player turned out to be useless, but maybe it's useful later, so decided to keep it

        String token = userService.extractToken(bearerToken);
        Game playedGame = getandCheckGame(id, token);
        User requestingUser = getandCheckUser(token);

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

        addInputToStory(playedGame, requestingWriter, prettyInput);
        return gameRepository.saveAndFlush(playedGame);
    }

    @Scheduled(fixedDelay = 2000) //Spring Annotation for automatically calling this every 2 seconds (2 seconds after the end of the last method call, not start) from the moment the app and spring started. 
    public void terminateExpiredTurns() {
        for (Game game : gameRepository.findAll()) {
            if (game.getPhase() != GamePhase.WRITING) continue;

            int currentRound = game.getCurrentRound();

            try { 
                resolveExpiredTurnIfNeeded(game); //check if timer is expired and if yes, resolve the round
            } catch (Exception e) {
                continue; //if something goes wrong, we just wanna skip this game but continue checking the rest of the games
            }

            //check if we had a round turn, and if yes, inform all the clients that a new round has started
            if (game.getCurrentRound() != currentRound) {
                gameStreamService.sendGameToAllClients(game);
            }
        }
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
            gameStreamService.sendGameDeletedToAllClients(playedGame.getId());
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
        gameRepository.saveAndFlush(playedGame);
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

 
    Map<Long, Map<Judge, Writer>> getGameVotes() {
        return gameVotes;
    }


    public Game getGame(Long gameId){
        String baseErrorMessage = "Error: The provided id: %s is invalid and doesn't match any game.";
		Game gameById = gameRepository.findById(gameId).orElseThrow(() ->  
		new ResponseStatusException(HttpStatus.NOT_FOUND, String.format(baseErrorMessage, gameId))); //NOT_FOUND 404

		return gameById; 
    }

    public int noVote = 0;

    public synchronized void addVote(Game currentGame, Writer voted, Judge judge) {
        noVote++;
        if (voted.getId() == null){
            return;
        }
        gameVotes.computeIfAbsent(currentGame.getId(), k -> new HashMap<>()).put(judge, voted);
    }

    public boolean allJudgesVoted(Game currentGame) {
        return noVote == currentGame.getJudges().size();
    }

    public void clearVotes(Game currentGame) {
        noVote = 0;
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
            if(writer.getUser().getId().equals(id)){
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
        Story oldStory = currentGame.getStory();
        Story newStory = new Story (winner.getUser(), loser.getUser(), currentGame.getStory().getStoryText(), hasWinner, winner.getGenre(), loser.getGenre(), judgeUsers);

        currentGame.setStory(newStory);

        gameRepository.save(currentGame);

        if (oldStory != null) {
            storyRepository.delete(oldStory);
        }

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
        if (targetWriter.getQuote() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Quote already assigned to this writer");
        }
        targetWriter.setQuote(quote);
        int assignedRound = playedGame.getCurrentRound();
        if (!targetWriter.getTurn()) {
            assignedRound += 1;
        }
        targetWriter.setQuoteAssignedRound(assignedRound);

        gameRepository.saveAndFlush(playedGame);
        return playedGame;
    }


}

