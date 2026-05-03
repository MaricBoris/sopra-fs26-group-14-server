package ch.uzh.ifi.hase.soprafs26.rest.dto.game;

import ch.uzh.ifi.hase.soprafs26.rest.dto.user.JudgeGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.user.WriterGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.user.StoryGetDTO;
import java.util.List;

public class GameGetDTO {

    private Long gameId;
    private Long timer;
    private int maxRounds;
    private List<WriterGetDTO> writers;
    private List<JudgeGetDTO> judges;
    private int currentRound;

    private Long turnStartedAt;

    private StoryGetDTO story;


    public Long getGameId() {
        return gameId;
    }

    public void setGameId(Long gameId) {
        this.gameId = gameId;
    }

    public Long getTimer() {
        return timer;
    }

    public void setTimer(Long timer) {
        this.timer = timer;
    }

    public int getMaxRounds() { return maxRounds; }
    public void setMaxRounds(int r) { this.maxRounds = r; }

     public Long getTurnStartedAt() {
        return turnStartedAt;
    }

    public void setTurnStartedAt(Long turnStartedAt) {
        this.turnStartedAt = turnStartedAt;
    }
    public List<WriterGetDTO> getWriters() {
        return writers;
    }

    public void setWriters(List<WriterGetDTO> writers) {
        this.writers = writers;
    }

    public List<JudgeGetDTO> getJudges() {

        return judges;
    }

    public void setJudges(List<JudgeGetDTO> judges) {
        this.judges = judges;

    }
    public StoryGetDTO getStory() {
    return story;
    }

    public void setStory(StoryGetDTO story) {
        this.story = story;
    }

    public int getCurrentRound() { return currentRound; }
    public void setCurrentRound(int currentRound) { this.currentRound = currentRound; }

    private String phase;

    public String getPhase() { return phase; }
    public void setPhase(String phase) { this.phase = phase; }

}