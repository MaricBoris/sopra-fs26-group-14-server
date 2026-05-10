package ch.uzh.ifi.hase.soprafs26.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "GENRE_MASTER")
public class GenreMaster implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String genre;

    @ManyToOne
    private User currentMaster;

    @ElementCollection
    @CollectionTable(name = "GENRE_VOTES", joinColumns = @JoinColumn(name = "genre_master_id"))
    @MapKeyColumn(name = "voter_id")
    @Column(name = "candidate_id")
    private Map<Long, Long> votes = new HashMap<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }

    public User getCurrentMaster() { return currentMaster; }
    public void setCurrentMaster(User currentMaster) { this.currentMaster = currentMaster; }

    public Map<Long, Long> getVotes() { return votes; }
    public void setVotes(Map<Long, Long> votes) { this.votes = votes; }
}