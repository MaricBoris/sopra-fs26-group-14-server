package ch.uzh.ifi.hase.soprafs26.rest.dto.game;

public class GameInputDTO {

    private Integer player;
    private String input;

    public Integer getPlayer() {
        return player;
    }

    public void setPlayer(Integer player) {
        this.player = player;
    }

    public String getInput() {
        return input;
    }

    public void setInput(String input) {
        this.input = input;
    }
}