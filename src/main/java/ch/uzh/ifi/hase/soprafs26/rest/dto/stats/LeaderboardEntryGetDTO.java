package ch.uzh.ifi.hase.soprafs26.rest.dto.stats;

public class LeaderboardEntryGetDTO {
    private Long userId;
    private String username;
    private Integer score;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }
}
