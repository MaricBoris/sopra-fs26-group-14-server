package ch.uzh.ifi.hase.soprafs26.rest.dto.storyRating;

public class GenreRatingGetDTO {

    private Long winnerUserId;
    private String winnerUsername;
    private String winnerGenre;
    private long winnerVotes;
 
    private Long loserUserId;
    private String loserUsername;
    private String loserGenre;
    private long loserVotes;
 
    private Long userVotedForId;   
    private boolean canVote;       
 
    public Long getWinnerUserId() { return winnerUserId; }
    public void setWinnerUserId(Long winnerUserId) { this.winnerUserId = winnerUserId; }
 
    public String getWinnerUsername() { return winnerUsername; }
    public void setWinnerUsername(String winnerUsername) { this.winnerUsername = winnerUsername; }
 
    public String getWinnerGenre() { return winnerGenre; }
    public void setWinnerGenre(String winnerGenre) { this.winnerGenre = winnerGenre; }
 
    public long getWinnerVotes() { return winnerVotes; }
    public void setWinnerVotes(long winnerVotes) { this.winnerVotes = winnerVotes; }
 
    public Long getLoserUserId() { return loserUserId; }
    public void setLoserUserId(Long loserUserId) { this.loserUserId = loserUserId; }
 
    public String getLoserUsername() { return loserUsername; }
    public void setLoserUsername(String loserUsername) { this.loserUsername = loserUsername; }
 
    public String getLoserGenre() { return loserGenre; }
    public void setLoserGenre(String loserGenre) { this.loserGenre = loserGenre; }
 
    public long getLoserVotes() { return loserVotes; }
    public void setLoserVotes(long loserVotes) { this.loserVotes = loserVotes; }
 
    public Long getUserVotedForId() { return userVotedForId; }
    public void setUserVotedForId(Long userVotedForId) { this.userVotedForId = userVotedForId; }
 
    public boolean isCanVote() { return canVote; }
    public void setCanVote(boolean canVote) { this.canVote = canVote; }

}
