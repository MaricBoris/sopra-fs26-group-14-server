package ch.uzh.ifi.hase.soprafs26.rest.dto.user;

import java.util.Date;

public class UserPersonalGetDTO {

    private Long id;
    private String username;
    private String bio;
    private String token;
    private Date creationDate;
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

    public Date getCreationDate() {
        return creationDate;
    }
    public void setCreationDate(Date creationDate) { this.creationDate = creationDate; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    //public History getHistory() { return history; }
    //public void setHistory(History history) { this.history = history; }
}
