package ch.uzh.ifi.hase.soprafs26.rest.dto.user;


public class JudgeGetDTO {
    private Long id;
    private String username;
    private Long insertions;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public Long getInsertions() { return insertions; }
    public void setInsertions(Long insertions) { this.insertions = insertions; }
}