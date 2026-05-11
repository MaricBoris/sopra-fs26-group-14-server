package ch.uzh.ifi.hase.soprafs26.rest.dto.achvs;

import java.util.Date;

public class UserAchievementGetDTO {

    private Long id;
    private AchievementGetDTO achievement;
    private Date unlockedAt;
    private boolean isDisplayed;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AchievementGetDTO getAchievement() {
        return achievement;
    }

    public void setAchievement(AchievementGetDTO achievement) {
        this.achievement = achievement;
    }

    public Date getUnlockedAt() {
        return unlockedAt;
    }

    public void setUnlockedAt(Date unlockedAt) {
        this.unlockedAt = unlockedAt;
    }

    public boolean getIsDisplayed() {
        return isDisplayed;
    }

    public void setIsDisplayed(boolean displayed) {
        isDisplayed = displayed;
    }
}