package ch.uzh.ifi.hase.soprafs26.rest.dto;

import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;

public class UserGetDTO {

	private Long id;
	private String username;
	private String bio;
 	//private History history;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}
	public String getBio() {
		return bio;
	}

	public void setBio(String bio) {
		this.bio = bio;
	}

	/*public History getHistory() {
		return history;
	}

	public void setHistory(History history) {
		this.history = history;
	}*/
	
}
