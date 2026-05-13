package ch.uzh.ifi.hase.soprafs26.entity;

import ch.uzh.ifi.hase.soprafs26.constant.GamePhase;
import jakarta.persistence.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "GAME")
public class Game implements Serializable {

    private int maxRounds;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    private Long timer;
    private Long turnStartedAt;
    private int currentRound = 1;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Writer> writers = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Judge> judges = new ArrayList<>();

    @OneToOne(cascade = CascadeType.ALL)
    private Story story;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GamePhase phase = GamePhase.WRITING;

    @Column(nullable = false)
    private boolean roundResolved = false;

    private Long startedAt = System.currentTimeMillis();

    public boolean isRoundResolved() {
    return roundResolved;
    }

    public void setRoundResolved(boolean roundResolved) {
        this.roundResolved = roundResolved;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getTimer() { return timer; }
    public void setTimer(Long timer) { this.timer = timer; }

    public Long getTurnStartedAt() { return turnStartedAt; }
    public void setTurnStartedAt(Long turnStartedAt) { this.turnStartedAt = turnStartedAt; }

    public List<Writer> getWriters() { return writers; }
    public void setWriters(List<Writer> writers) { this.writers = writers; }

    public List<Judge> getJudges() { return judges; }
    public void setJudges(List<Judge> judges) { this.judges = judges; }


    public int getCurrentRound() { return currentRound; }
    public void setCurrentRound(int r) { this.currentRound = r; }

    public int getMaxRounds() { return maxRounds; }
    public void setMaxRounds(int r) { this.maxRounds = r; }

    public GamePhase getPhase() { return phase; }
    public void setPhase(GamePhase phase) { this.phase = phase; }

    public Long getStartedAt() { return startedAt; }
    public void setStartedAt(Long startedAt) { this.startedAt = startedAt; }

    public void nextRound() {
        //setTimer(90L);
        setTurnStartedAt(System.currentTimeMillis());
        setCurrentRound(currentRound + 1);
        setRoundResolved(false);

        for (Writer writer : writers) {
            writer.setTurn(!writer.getTurn());
        }

        if (currentRound > maxRounds) {
            setCurrentRound(maxRounds);
            setPhase(GamePhase.EVALUATION);
            setTurnStartedAt(System.currentTimeMillis());
        }
    }
    

    public Story getStory() { return story; }
    public void setStory(Story story) { this.story = story; }


}