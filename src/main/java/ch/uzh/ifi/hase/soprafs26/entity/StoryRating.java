package ch.uzh.ifi.hase.soprafs26.entity;
 
import jakarta.persistence.*;
 
import java.io.Serializable;
import java.util.Date;
 
@Entity
@Table(name = "STORY_RATING")
public class StoryRating implements Serializable {
 
    @Id
    @GeneratedValue
    private Long id;
 
    @ManyToOne
    private Story story;
 
    @ManyToOne
    private User voter;
 
    @ManyToOne
    private User votedFor;
 
    @Column(nullable = false)
    private Date timestamp;
 
    public StoryRating() {
        this.timestamp = new Date();
    }
 
    public StoryRating(Story story, User voter, User votedFor) {
        this.story = story;
        this.voter = voter;
        this.votedFor = votedFor;
        this.timestamp = new Date();
    }
 
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
 
    public Story getStory() { return story; }
    public void setStory(Story story) { this.story = story; }
 
    public User getVoter() { return voter; }
    public void setVoter(User voter) { this.voter = voter; }
 
    public User getVotedFor() { return votedFor; }
    public void setVotedFor(User votedFor) { this.votedFor = votedFor; }
 
    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }
}
