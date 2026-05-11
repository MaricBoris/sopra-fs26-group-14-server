package ch.uzh.ifi.hase.soprafs26.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "STORY_CONTRIBUTION")
public class StoryContribution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    @Column(columnDefinition = "TEXT")
    private String text;

    public StoryContribution() {}

    public StoryContribution(Long userId, String text) {
        this.userId = userId;
        this.text = text;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
}