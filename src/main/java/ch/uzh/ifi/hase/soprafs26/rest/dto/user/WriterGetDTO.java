package ch.uzh.ifi.hase.soprafs26.rest.dto.user;

public class WriterGetDTO {
    private Long id;
    private String username;
    private Boolean turn;
    private String genre;
    private String genreDescription;
    private String text;
    private String quote;
    private Integer quoteAssignedRound;
    private int reduceTimeReceived;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public Boolean getTurn() { return turn; }
    public void setTurn(Boolean turn) { this.turn = turn; }

    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }

    public String getGenreDescription() { return genreDescription; }
    public void setGenreDescription(String genreDescription) { this.genreDescription = genreDescription; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public String getQuote() { return quote; }
    public void setQuote(String quote) { this.quote = quote; }

    public Integer getQuoteAssignedRound() { return quoteAssignedRound; }
    public void setQuoteAssignedRound(Integer quoteAssignedRound) { this.quoteAssignedRound = quoteAssignedRound; }

    public int getReduceTimeReceived() { return reduceTimeReceived; }
    public void setReduceTimeReceived(int v) { this.reduceTimeReceived = v; }
}