package ch.uzh.ifi.hase.soprafs26.rest.dto.game;

import ch.uzh.ifi.hase.soprafs26.entity.Story;
import ch.uzh.ifi.hase.soprafs26.rest.dto.user.JudgeGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.user.WriterGetDTO;
import java.util.List;

public class GameGetDTO {

    private Long gameId;
    private Long timer;
    private List<WriterGetDTO> writers;
    private List<JudgeGetDTO> judges;

    private Story story;


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


    public List<JudgeGetDTO> getJudges() {

        return judges;
    }

    public void setJudges(List<JudgeGetDTO> judges) {
        this.judges = judges;
    }
   

    public Story getStory() {
    return story;
    }

    public void setStory(Story story) {
        this.story = story;
    }
}