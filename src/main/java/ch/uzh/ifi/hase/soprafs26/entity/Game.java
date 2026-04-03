package ch.uzh.ifi.hase.soprafs26.entity;

import jakarta.persistence.*;

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

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Writer> writers = new ArrayList<>();

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    private Judge judge;

    //@Column(columnDefinition = "TEXT")
    //private Story story;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getTimer() { return timer; }
    public void setTimer(Long timer) { this.timer = timer; }

    public List<Writer> getWriters() { return writers; }
    public void setWriters(List<Writer> writers) { this.writers = writers; }

    public Judge getJudge() { return judge; }
    public void setJudge(Judge judge) { this.judge = judge; }

    //public String getStory() { return story; }
    //public void setStory(String story) { this.story = story; }
}