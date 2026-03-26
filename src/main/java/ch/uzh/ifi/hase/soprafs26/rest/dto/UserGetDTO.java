package ch.uzh.ifi.hase.soprafs26.rest.dto;

import java.util.Date;

public class UserGetDTO {

	private Long id;
	private String bio;
	private String username;
	private Date creationDate;
	//private History history;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getBio() {
		return bio;
	}

	public void setBio(String name) {
		this.bio = name;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) { this.creationDate = creationDate; }

	//public History getHistory() { return history; }
    //public void setHistory(History history) { this.history = history; }
}
