package ch.uzh.ifi.hase.soprafs26.rest.dto.stats;

import java.util.Map;

public class UserStatisticsGetDTO {
    private Integer gamesPlayed;
    private Integer gamesWon;
    private Integer gamesLost;
    private Integer currentWinStreak;
    private Integer highestWinStreak;
    private Integer winsAsWriter;
    private Integer winsAsJudge;
    private Integer totalVotesCast;
    private Map<String, Integer> winsByGenre;
    private Integer suddenDeathEntries;
    private Integer suddenDeathWins;
    private Integer unanimousWins;
    private Long totalWordsWritten;

    public Integer getGamesPlayed() { return gamesPlayed; }
    public void setGamesPlayed(Integer gamesPlayed) { this.gamesPlayed = gamesPlayed; }

    public Integer getGamesWon() { return gamesWon; }
    public void setGamesWon(Integer gamesWon) { this.gamesWon = gamesWon; }

    public Integer getGamesLost() { return gamesLost; }
    public void setGamesLost(Integer gamesLost) { this.gamesLost = gamesLost; }

    public Integer getCurrentWinStreak() { return currentWinStreak; }
    public void setCurrentWinStreak(Integer currentWinStreak) { this.currentWinStreak = currentWinStreak; }

    public Integer getHighestWinStreak() { return highestWinStreak; }
    public void setHighestWinStreak(Integer highestWinStreak) { this.highestWinStreak = highestWinStreak; }

    public Integer getWinsAsWriter() { return winsAsWriter; }
    public void setWinsAsWriter(Integer winsAsWriter) { this.winsAsWriter = winsAsWriter; }

    public Integer getWinsAsJudge() { return winsAsJudge; }
    public void setWinsAsJudge(Integer winsAsJudge) { this.winsAsJudge = winsAsJudge; }

    public Integer getTotalVotesCast() { return totalVotesCast; }
    public void setTotalVotesCast(Integer totalVotesCast) { this.totalVotesCast = totalVotesCast; }

    public Map<String, Integer> getWinsByGenre() { return winsByGenre; }
    public void setWinsByGenre(Map<String, Integer> winsByGenre) { this.winsByGenre = winsByGenre; }

    public Integer getSuddenDeathEntries() { return suddenDeathEntries; }
    public void setSuddenDeathEntries(Integer suddenDeathEntries) { this.suddenDeathEntries = suddenDeathEntries; }

    public Integer getSuddenDeathWins() { return suddenDeathWins; }
    public void setSuddenDeathWins(Integer suddenDeathWins) { this.suddenDeathWins = suddenDeathWins; }

    public Integer getUnanimousWins() { return unanimousWins; }
    public void setUnanimousWins(Integer unanimousWins) { this.unanimousWins = unanimousWins; }

    public Long getTotalWordsWritten() { return totalWordsWritten; }
    public void setTotalWordsWritten(Long totalWordsWritten) { this.totalWordsWritten = totalWordsWritten; }
}