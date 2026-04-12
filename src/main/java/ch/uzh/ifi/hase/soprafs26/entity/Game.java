package ch.uzh.ifi.hase.soprafs26.entity;

import jakarta.persistence.*;
import ch.uzh.ifi.hase.soprafs26.entity.Story;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import ch.uzh.ifi.hase.soprafs26.entity.GamePhase;

@Entity
@Table(name = "GAME")
public class Game implements Serializable {

    public static final int MAX_ROUNDS = 20;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long timer = 60L;
    private Long turnStartedAt = System.currentTimeMillis();
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



    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getTimer() { return timer; }
    public void setTimer(Long timer) { this.timer = timer; }

    public List<Writer> getWriters() { return writers; }
    public void setWriters(List<Writer> writers) { this.writers = writers; }

    public List<Judge> getJudges() { return judges; }
    public void setJudges(List<Judge> judges) { this.judges = judges; }

    public Long getTurnStartedAt() { return turnStartedAt; }
    public void setTurnStartedAt(Long t) { this.turnStartedAt = t; }

    public int getCurrentRound() { return currentRound; }
    public void setCurrentRound(int r) { this.currentRound = r; }

    public GamePhase getPhase() { return phase; }
    public void setPhase(GamePhase phase) { this.phase = phase; }


    public void nextRound() {
        setTimer(60L);
        setTurnStartedAt(System.currentTimeMillis());
        setCurrentRound(currentRound + 1);
        for (Writer writer : writers) {
            writer.setTurn(!writer.getTurn());
        }
        if (currentRound > MAX_ROUNDS) {
            setPhase(GamePhase.EVALUATION);
        }
    }
    

    public Story getStory() { return story; }
    public void setStory(Story story) { this.story = story; }


}