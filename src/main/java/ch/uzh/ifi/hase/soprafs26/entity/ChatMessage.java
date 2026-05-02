package ch.uzh.ifi.hase.soprafs26.entity;

import jakarta.persistence.Embeddable;
import java.util.Date;

@Embeddable
public class ChatMessage {
    private String username;
    private String message;
    private Date timestamp;

    public ChatMessage() {
        this.timestamp = new Date();
    }

    public ChatMessage(String username, String message) {
        this.username = username;
        this.message = message;
        this.timestamp = new Date();
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }
}