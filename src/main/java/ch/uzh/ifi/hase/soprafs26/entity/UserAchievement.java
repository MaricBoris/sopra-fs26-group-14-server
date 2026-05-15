package ch.uzh.ifi.hase.soprafs26.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "USER_ACHIEVEMENT")
public class UserAchievement implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "achievement_id")
    private Achievement achievement;

    @Column(nullable = false)
    private Date unlockedAt = new Date();

    @Column(nullable = false)
    private boolean isDisplayed = true;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Achievement getAchievement() { return achievement; }
    public void setAchievement(Achievement achievement) { this.achievement = achievement; }

    public Date getUnlockedAt() { return unlockedAt; }
    public void setUnlockedAt(Date unlockedAt) { this.unlockedAt = unlockedAt; }

    public boolean getIsDisplayed() { return isDisplayed; }
    public void setIsDisplayed(boolean displayed) { isDisplayed = displayed; }
}