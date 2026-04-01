package ch.uzh.ifi.hase.soprafs26.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ROOM")
public class Room implements Serializable {

    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @OneToMany(cascade = CascadeType.ALL)
    private List<User> users = new ArrayList<>();

    //@OneToMany(cascade = CascadeType.ALL)
    //private List<Writer> writers = new ArrayList<>();

    //@OneToMany(cascade = CascadeType.ALL)
    //private List<Judge> judges = new ArrayList<>();

    @ManyToOne
    private User lobbyLeader;

    // --- Getters and Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public List<User> getUsers() { return users; }
    public void setUsers(List<User> users) { this.users = users; }

    //public List<Writer> getWriters() { return writers; }
    //public void setWriters(List<Writer> writers) { this.writers = writers; }

    //public List<Judge> getJudges() { return judges; }
    //public void setJudges(List<Judge> judges) { this.judges = judges; }

    public User getLobbyLeader() { return lobbyLeader; }
    public void setLobbyLeader(User lobbyLeader) { this.lobbyLeader = lobbyLeader; }
}