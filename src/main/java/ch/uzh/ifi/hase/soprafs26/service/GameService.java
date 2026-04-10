package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.entity.*;
import ch.uzh.ifi.hase.soprafs26.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.ArrayList;
import java.util.List;



@Service
@Transactional
public class GameService {

    private final GameRepository gameRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    @Autowired
    public GameService(GameRepository gameRepository, UserService userService, UserRepository userRepository) {
        this.gameRepository = gameRepository;
        this.userService = userService;
        this.userRepository=userRepository;
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
                gameRepository.delete(playedGame);
                gameRepository.flush();
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
            gameRepository.delete(playedGame);
            gameRepository.flush();
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Game ended because judge disconnected");
            }
            else{
                gameRepository.save(playedGame);
            }
        }   

        if (playedGame.getWriters().size()!=2 || playedGame.getJudges().size()!=1 ){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Erroneous Game State"); //Check 400 
        }
        return playedGame;
    }


    public Game insertWriterInput(Long id, Integer player , String inputText , String bearerToken) {

        //currently player turned out to be useless, but maybe it's useful later, so decided to keep it

        String token = userService.extractToken(bearerToken);
        Game playedGame=getandCheckGame(id, token);
        User requestingUser=getandCheckUser(token);
        boolean WriterInGame=false;
        Writer requestingWriter=null;
        for (Writer writer : playedGame.getWriters()) {
            if(writer.getUser().getId().equals(requestingUser.getId())){
                WriterInGame=true;
                requestingWriter=writer;
            }
        }
        if (!WriterInGame){
           throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not a writer in game"); //Check 403 
        }
        if (!requestingWriter.getTurn()){
           throw new ResponseStatusException(HttpStatus.FORBIDDEN, "It's not this writers turn!"); //Check 403 
        }
        String currentStory = playedGame.getStory();
        if (currentStory == null || currentStory.isBlank()) {
            playedGame.setStory(inputText);
        } else if (!inputText.isBlank()){
            playedGame.setStory(currentStory+" "+inputText);
        }

        playedGame.nextRound();
        gameRepository.save(playedGame);
        return playedGame;
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
}