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

    @Column(nullable = false)
    private Integer playerCount;

    @OneToMany(cascade = CascadeType.ALL)
    private List<User> users = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Writer> writers = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Judge> judges = new ArrayList<>();

    @ManyToOne
    private User lobbyLeader;

    @ElementCollection
    @CollectionTable(name = "ROOM_CHAT", joinColumns = @JoinColumn(name = "room_id"))
    private List<ChatMessage> chat = new ArrayList<>();

    private Long timer;
    private int maxRounds;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getPlayerCount() { return playerCount; }
    public void setPlayerCount(Integer playerCount) { this.playerCount = playerCount; }

    public List<User> getUsers() { return users; }
    public void setUsers(List<User> users) { this.users = users; }

    public List<Writer> getWriters() { return writers; }
    public void setWriters(List<Writer> writers) { this.writers = writers; }

    public List<Judge> getJudges() { return judges; }
    public void setJudges(List<Judge> judges) { this.judges = judges; }

    public User getLobbyLeader() { return lobbyLeader; }
    public void setLobbyLeader(User lobbyLeader) { this.lobbyLeader = lobbyLeader; }

    public List<ChatMessage> getChat() { return chat; }
    public void setChat(List<ChatMessage> chat) { this.chat = chat; }

    public int getMaxRounds() { return maxRounds; }
    public void setMaxRounds(int r) { this.maxRounds = r; }

    public Long getTimer() { return timer; }
    public void setTimer(Long timer) { this.timer = timer; }
}