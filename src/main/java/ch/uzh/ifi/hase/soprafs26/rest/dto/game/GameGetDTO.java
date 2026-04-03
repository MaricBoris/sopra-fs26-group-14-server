package ch.uzh.ifi.hase.soprafs26.rest.dto.game;

import ch.uzh.ifi.hase.soprafs26.rest.dto.user.JudgeGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.user.WriterGetDTO;
import java.util.List;

public class GameGetDTO {

    private Long gameId;
    private Long timer;
    private List<WriterGetDTO> writers;
    private JudgeGetDTO judge;
    //private Story story;

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

    public List<WriterGetDTO> getWriters() {
        return writers;
    }

    public void setWriters(List<WriterGetDTO> writers) {
        this.writers = writers;
    }

    public JudgeGetDTO getJudge() {
        return judge;
    }

    public void setJudge(JudgeGetDTO judge) {
        this.judge = judge;
    }

    /**public String getStory() {
        return story;
    }

    public void setStory(String story) {
        this.story = story;
    }**/
}