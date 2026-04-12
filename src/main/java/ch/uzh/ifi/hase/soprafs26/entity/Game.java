package ch.uzh.ifi.hase.soprafs26.entity;

import jakarta.persistence.*;
import ch.uzh.ifi.hase.soprafs26.entity.Story;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "GAME")
public class Game implements Serializable {
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


    public void nextRound() {
        setTimer(60L);
        setTurnStartedAt(System.currentTimeMillis());
        setCurrentRound(currentRound + 1);
        for (Writer writer : writers) {
            writer.setTurn(!writer.getTurn());
        }
    }
    

    public Story getStory() { return story; }
    public void setStory(Story story) { this.story = story; }


}