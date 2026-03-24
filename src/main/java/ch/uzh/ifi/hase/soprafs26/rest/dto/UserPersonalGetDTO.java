package ch.uzh.ifi.hase.soprafs26.rest.dto;

public class UserPersonalGetDTO {

    private Long id;
    private String username;
    private String bio;
    private String token;
    //private History history;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
    }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
}
