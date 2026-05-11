package ch.uzh.ifi.hase.soprafs26.entity;

import jakarta.persistence.*;

import java.util.List;
import java.util.ArrayList;
import java.io.Serializable;
import java.util.Date;

/**
 * Internal User Representation
 * This class composes the internal representation of the user and defines how
 * the user is stored in the database.
 * Every variable will be mapped into a database field with the @Column
 * annotation
 * - nullable = false -> this cannot be left empty
 * - unique = true -> this value must be unqiue across the database -> composes
 * the primary key
 */
@Entity
@Table(name = "users")
@Inheritance(strategy = InheritanceType.JOINED)
public class User implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue
	private Long id;

	@Column(nullable = false, unique = true)
	private String username;

	@Column(nullable = false, unique = true)
	private String token;

	@Column(nullable = true)
	private String bio;

    @Column(nullable = false)
    private String password;
	
    @Column(nullable = false, updatable = false)
    private Date creationDate = new Date();

	@ManyToMany
	private List<Story> history = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserAchievement> achievements = new ArrayList<>();

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "statistics_id", referencedColumnName = "id")
    private UserStatistics statistics;

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

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

    public String getBio() { return bio; }

    public void setBio(String bio) { this.bio = bio; }

    public String getPassword() { return password; }

    public void setPassword(String password) { this.password = password; }

    public Date getCreationDate() { return creationDate; }

    public void setCreationDate(Date creationDate) { this.creationDate = creationDate; }

	public void addStory(Story story) { this.history.add(story); }
	public List<Story> getHistory() { return history; }

    public List<UserAchievement> getAchievements() { return achievements; }
    public void setAchievements(List<UserAchievement> achievements) { this.achievements = achievements; }

    public UserStatistics getStatistics() { return statistics; }
    public void setStatistics(UserStatistics statistics) { this.statistics = statistics; }
}
