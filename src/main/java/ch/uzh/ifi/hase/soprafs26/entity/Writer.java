package ch.uzh.ifi.hase.soprafs26.entity;

import jakarta.persistence.*;

import java.io.Serializable;

@Entity
@Table(name = "WRITER")
public class Writer implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(cascade = CascadeType.MERGE)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private Boolean turn = false;

    @Column(columnDefinition = "TEXT")
    private String genre;

    @Column(columnDefinition = "TEXT")
    private String genreDescription;

    @Column(columnDefinition = "TEXT")
    private String text;

    @Column(nullable = false)
    private Long lastSeenAt = System.currentTimeMillis();

    @Column(columnDefinition = "TEXT")
    private String quote;

    @Column
    private Integer quoteAssignedRound;


    public Writer() {}
    public Writer(User user) { this.user = user; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Boolean getTurn() { return turn; }
    public void setTurn(Boolean turn) { this.turn = turn; }

    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }

    public String getGenreDescription() { return genreDescription; }
    public void setGenreDescription(String genreDescription) { this.genreDescription = genreDescription; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public Long getLastSeenAt() { return lastSeenAt; }
    public void setLastSeenAt(Long lastSeenAt) { this.lastSeenAt = lastSeenAt; }

    public String getQuote() { return quote; }
    public void setQuote(String quote) { this.quote = quote; }

    public Integer getQuoteAssignedRound() { return quoteAssignedRound; }
    public void setQuoteAssignedRound(Integer quoteAssignedRound) { this.quoteAssignedRound = quoteAssignedRound; }
}