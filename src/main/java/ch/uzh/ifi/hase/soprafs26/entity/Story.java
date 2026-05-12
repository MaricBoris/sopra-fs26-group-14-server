package ch.uzh.ifi.hase.soprafs26.entity;

import java.util.Date;
import java.io.Serializable;
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

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StoryContribution> storyContributions = new ArrayList<>();

    private Boolean hasWinner;

    private String winGenre;

    private String loseGenre;

    @Column
    private String title;

    @ManyToMany
    private List<User> judges = new ArrayList<>();

    @Column(updatable = false)
    private Date creationDate;

    @Column
    private String objective;

    @Column
    private String tieBreakerQuote;

    public Story() {
        this.hasWinner = false;
        this.creationDate = new Date();
    }

    public Story(User winner, User loser, List<StoryContribution> storyContributions, Boolean hasWinner, String winGenre, String loseGenre, List<User> judges) {
        this.winner = winner;
        this.loser = loser;
        this.storyContributions = storyContributions;
        this.hasWinner = hasWinner;
        this.winGenre = winGenre;
        this.loseGenre = loseGenre;
        this.judges = judges;
        this.creationDate = new Date();
        this.tieBreakerQuote = "";
    }

    public void addContribution(Long userId, String text) {
        this.storyContributions.add(new StoryContribution(userId, text));
    }


    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getWinner() { return winner; }
    public void setWinner(User winner) { this.winner = winner; }

    public User getLoser() { return loser; }
    public void setLoser(User loser) { this.loser = loser; }

    public List<StoryContribution> getStoryContributions() { return storyContributions; }
    public void setStoryContributions(List<StoryContribution> storyContributions) { this.storyContributions = storyContributions; }

    public Boolean getHasWinner() { return hasWinner; }
    public void setHasWinner(Boolean hasWinner) { this.hasWinner = hasWinner; }

    public String getWinGenre() { return winGenre; }
    public void setWinGenre(String winGenre) { this.winGenre = winGenre; }

    public String getLoseGenre() { return loseGenre; }
    public void setLoseGenre(String loseGenre) { this.loseGenre = loseGenre; }

    public List<User> getJudges() { return judges; }
    public void setJudges(List<User> judges) { this.judges = judges; }

    public Date getCreationDate() { return creationDate; }
    public void setCreationDate(Date creationDate) { this.creationDate = creationDate; }

    public String getObjective() { return objective; }
    public void setObjective(String objective) { this.objective = objective; }

    public String getTieBreakerQuote() { return tieBreakerQuote; }
    public void setTieBreakerQuote(String tieBreakerQuote) { this.tieBreakerQuote = tieBreakerQuote; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
}