package ch.uzh.ifi.hase.soprafs26.entity;

import java.util.Date;
import java.io.Serializable;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;

@Entity
@Table(name = "STORY")
public class Story implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private User winner;

    @ManyToOne
    private User loser;

    @Column(columnDefinition = "TEXT")
    private String storyText;

    private Boolean hasWinner;

    private String winGenre;

    private String loseGenre;

    @ManyToMany
    private List<User> judges = new ArrayList<>();

    @Column(updatable = false)
    private Date creationDate;

    public Story() {
        this.storyText = "";
        this.hasWinner = false;
        this.creationDate = new Date();
    }

    public Story(User winner, User loser, String storyText, Boolean hasWinner, String winGenre, String loseGenre, List<User> judges) {
        this.winner = winner;
        this.loser = loser;
        this.storyText = storyText;
        this.hasWinner = hasWinner;
        this.winGenre = winGenre;
        this.loseGenre = loseGenre;
        this.judges = judges;
        this.creationDate = new Date();
    }


    public Long getId() { return id; }
    public User getWinner() { return winner; }
    public User getLoser() { return loser; }
    public String getStoryText() { return storyText; }
    public void setStoryText(String text) { this.storyText=text; }
    public Boolean getHasWinner() { return hasWinner; }
    public String getWinGenre() { return winGenre; }
    public String getLoseGenre() { return loseGenre; }
    public List<User> getJudges() { return judges; }
    public Date getCreationDate() { return creationDate; }
}