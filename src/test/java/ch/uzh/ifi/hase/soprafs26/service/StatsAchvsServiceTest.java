package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.entity.*;
import ch.uzh.ifi.hase.soprafs26.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

public class StatsAchvsServiceTest {/*

    @Mock
    private UserRepository userRepository;
    @Mock
    private AchievementRepository achievementRepository;
    @Mock
    private UserAchievementRepository userAchievementRepository;
    @Mock
    private GenreMasterRepository genreMasterRepository;

    @InjectMocks
    private StatsAchvsService statsAchvsService;

    private User winner;
    private User loser;
    private Game game;
    private Story story;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);

        // Setup Winner
        winner = new User();
        winner.setId(1L);
        winner.setUsername("winnerUser");
        winner.setStatistics(new UserStatistics());

        // Setup Loser
        loser = new User();
        loser.setId(2L);
        loser.setUsername("loserUser");
        loser.setStatistics(new UserStatistics());

        // Setup Story
        story = new Story();
        story.setWinner(winner);
        story.setLoser(loser);
        story.setWinGenre("Horror");
        story.setJudges(new ArrayList<>());

        // Setup Game
        game = new Game();
        game.setId(100L);
        game.setStory(story);
    }

    @Test
    public void processGameResults_updatesStatsCorrectly() {
        // Execute
        statsAchvsService.processGameResults(game, false);

        // Verify Winner Stats
        assertEquals(1, winner.getStatistics().getGamesWon());
        assertEquals(1, winner.getStatistics().getGamesPlayed());
        assertEquals(1, winner.getStatistics().getWinsByGenre().get("Horror"));

        // Verify Loser Stats
        assertEquals(0, loser.getStatistics().getGamesWon());
        assertEquals(1, loser.getStatistics().getGamesPlayed());
        assertEquals(1, loser.getStatistics().getGamesLost());
    }

    @Test
    public void processGameResults_unanimousWin_incrementsUnanimousStat() {
        // Execute with unanimous flag = true
        statsAchvsService.processGameResults(game, true);

        assertEquals(1, winner.getStatistics().getUnanimousWins());
    }

    @Test
    public void processGameResults_unlocksRookieAchievementOnFirstWin() {
        // Mock the Achievement definition existing in DB
        Achievement rookieAch = new Achievement();
        rookieAch.setName("ROOKIE_SCRIBE");
        given(achievementRepository.findByName("ROOKIE_SCRIBE")).willReturn(rookieAch);

        // Execute
        statsAchvsService.processGameResults(game, false);

        // Verify winner got the achievement
        assertEquals(1, winner.getAchievements().size());
        assertEquals("ROOKIE_SCRIBE", winner.getAchievements().get(0).getAchievement().getName());
    }

    @Test
    public void setGenreMaster_electsNewMasterBasedOnVotes() {
        // Setup GenreMaster entity
        GenreMaster gm = new GenreMaster();
        gm.setGenre("Horror");
        Map<Long, Long> votes = new HashMap<>();
        votes.put(10L, 1L); // Voter 10 votes for Winner (ID 1)
        votes.put(11L, 1L); // Voter 11 votes for Winner (ID 1)
        votes.put(12L, 2L); // Voter 12 votes for Loser (ID 2)
        gm.setVotes(votes);

        given(genreMasterRepository.findByGenre("Horror")).willReturn(gm);
        given(userRepository.findById(1L)).willReturn(Optional.of(winner));

        // Execute
        statsAchvsService.processGameResults(game, false);

        // Verify Winner is now the Genre Master
        assertEquals("winnerUser", gm.getCurrentMaster().getUsername());
        verify(genreMasterRepository, atLeastOnce()).save(gm);
    }

    @Test
    public void checkTopPercentile_grantsLegendAchievement() {
        // Setup a scenario where total users = 100, and winner has 50 wins (definitely top 1%)
        winner.getStatistics().getWinsByGenre().put("Horror", 50);

        Achievement legendAch = new Achievement();
        legendAch.setName("HORROR_LEGEND");

        given(userRepository.count()).willReturn(100L);
        given(userRepository.findAll()).willReturn(Arrays.asList(winner, loser)); // Simplified
        given(achievementRepository.findByName("HORROR_LEGEND")).willReturn(legendAch);

        // Execute
        statsAchvsService.processGameResults(game, false);

        // Check if Legend was granted
        boolean hasLegend = winner.getAchievements().stream()
                .anyMatch(a -> a.getAchievement().getName().equals("HORROR_LEGEND"));
        assertTrue(hasLegend);
    }
*/}