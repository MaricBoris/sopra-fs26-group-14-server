package ch.uzh.ifi.hase.soprafs26.rest.dto.room;

import ch.uzh.ifi.hase.soprafs26.rest.dto.user.JudgeGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.user.UserGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.user.WriterGetDTO;

import java.util.List;

public class RoomGetDTO {

    private Long id;
    private String name;
    private UserGetDTO lobbyLeader;
    private Integer playerCount;
    private List<UserGetDTO> users;
    private List<WriterGetDTO> writers;
    private List<JudgeGetDTO> judges;
    private Long timer;
    private int maxRounds;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UserGetDTO getLobbyLeader() {
        return lobbyLeader;
    }

    public void setLobbyLeader(UserGetDTO lobbyLeader) {
        this.lobbyLeader = lobbyLeader;
    }

    public Integer getPlayerCount() {
        return (users != null) ? users.size() : 0;
    }

    public void setPlayerCount(Integer playerCount) {
        this.playerCount = playerCount;
    }

    public List<UserGetDTO> getUsers() {
        return users;
    }

    public void setUsers(List<UserGetDTO> users) {
        this.users = users;
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

    public int getMaxRounds() { return maxRounds; }
    public void setMaxRounds(int r) { this.maxRounds = r; }

    public Long getTimer() { return timer; }
    public void setTimer(Long timer) { this.timer = timer; }
}