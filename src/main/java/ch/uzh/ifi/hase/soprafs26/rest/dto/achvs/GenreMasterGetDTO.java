package ch.uzh.ifi.hase.soprafs26.rest.dto.achvs;


import ch.uzh.ifi.hase.soprafs26.rest.dto.user.UserGetDTO;

public class GenreMasterGetDTO {

    private Long id;
    private String genre;
    private UserGetDTO currentMaster;
    private Integer totalVotesCast;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }

    public UserGetDTO getCurrentMaster() { return currentMaster; }
    public void setCurrentMaster(UserGetDTO currentMaster) { this.currentMaster = currentMaster; }

    public Integer getTotalVotesCast() { return totalVotesCast; }
    public void setTotalVotesCast(Integer totalVotesCast) { this.totalVotesCast = totalVotesCast; }
}