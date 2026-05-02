package ch.uzh.ifi.hase.soprafs26.rest.dto.room;

import java.util.Date;

public class ChatMessageGetDTO {
    private String username;
    private String message;
    private Date timestamp;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }
}