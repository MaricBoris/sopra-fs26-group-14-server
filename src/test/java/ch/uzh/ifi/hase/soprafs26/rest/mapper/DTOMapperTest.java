package ch.uzh.ifi.hase.soprafs26.rest.mapper;

import ch.uzh.ifi.hase.soprafs26.constant.GamePhase;
import ch.uzh.ifi.hase.soprafs26.entity.*;
import ch.uzh.ifi.hase.soprafs26.rest.dto.achvs.AchievementGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.achvs.GenreMasterGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.achvs.UserAchievementGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.game.GameGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.room.ChatMessageGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.room.RoomGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.room.RoomPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.stats.UserStatisticsGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.user.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class DTOMapperTest {

    // --- USER MAPPING ---

    @Test
    public void convertUserPostDTOtoEntity_success() {
        UserPostDTO userPostDTO = new UserPostDTO();
        userPostDTO.setUsername("testUsername");
        userPostDTO.setPassword("testPassword123");

        User user = DTOMapper.INSTANCE.convertUserPostDTOtoEntity(userPostDTO);

        assertEquals(userPostDTO.getUsername(), user.getUsername());
        assertEquals(userPostDTO.getPassword(), user.getPassword());
    }

    @Test
    public void convertEntityToUserGetDTO_success() {
        User user = new User();
        user.setId(1L);
        user.setUsername("publicUser");
        Date now = new Date();
        user.setCreationDate(now);

        UserGetDTO userGetDTO = DTOMapper.INSTANCE.convertEntityToUserGetDTO(user);

        assertEquals(user.getId(), userGetDTO.getId());
        assertEquals(user.getUsername(), userGetDTO.getUsername());
        assertEquals(now, user.getCreationDate());
    }

    // --- ROLES MAPPING (WRITER & JUDGE) ---

    @Test
    public void convertEntityToWriterGetDTO_success() {
        User user = new User();
        user.setId(10L);
        user.setUsername("writerUser");

        Writer writer = new Writer();
        writer.setUser(user);
        writer.setId(55L);
        writer.setTurn(true);
        writer.setGenre("Sci-Fi");
        writer.setGenreDescription("A futuristic setting");
        writer.setText("The story begins...");
        writer.setQuote("To be or not to be.");
        writer.setQuoteAssignedRound(2);
        Long time = 12345L;
        writer.setLastSeenAt(time);

        WriterGetDTO dto = DTOMapper.INSTANCE.convertEntityToWriterGetDTO(writer);

        assertEquals(user.getId(), dto.getId());
        assertEquals(user.getUsername(), dto.getUsername());
        assertEquals(true, dto.getTurn());
        assertEquals("Sci-Fi", dto.getGenre());
        assertEquals("A futuristic setting", dto.getGenreDescription());
        assertEquals("The story begins...", dto.getText());
        assertEquals("To be or not to be.", dto.getQuote());
        assertEquals(2, dto.getQuoteAssignedRound());
    }

    @Test
    public void convertEntityToJudgeGetDTO_success() {
        User user = new User();
        user.setId(20L);
        user.setUsername("judgeUser");

        Judge judge = new Judge();
        judge.setUser(user);
        judge.setId(88L);
        judge.setInsertions(5L);

        JudgeGetDTO dto = DTOMapper.INSTANCE.convertEntityToJudgeGetDTO(judge);

        assertEquals(user.getId(), dto.getId());
        assertEquals(user.getUsername(), dto.getUsername());
        assertEquals(5L, dto.getInsertions());
    }

    // --- GAME, ROOM & STORY MAPPING ---

    @Test
    public void convertEntityToStoryGetDTO_success() {
        User winner = new User(); winner.setUsername("win");
        User loser = new User(); loser.setUsername("loss");
        Story story = new Story(winner, loser, new ArrayList<>(), true, "WinG", "LoseG", new ArrayList<>());

        story.addContribution(1L, "New story text");
        story.setObjective("Incorporate a red apple"); // New Field
        story.setTieBreakerQuote("Life is a journey"); // New Field

        User j = new User();
        story.getJudges().add(j);

        StoryGetDTO storyGetDTO = DTOMapper.INSTANCE.convertEntityToStoryGetDTO(story);

        assertFalse(storyGetDTO.getStoryContributions().isEmpty());
        assertEquals("New story text", storyGetDTO.getStoryContributions().get(0).getText());
        assertEquals(winner.getUsername(), storyGetDTO.getWinnerUsername());
        assertEquals("Incorporate a red apple", storyGetDTO.getObjective());
        assertEquals("Life is a journey", storyGetDTO.getTieBreakerQuote());
    }

    @Test
    public void convertEntityToGameGetDTO_success() {
        Game game = new Game();
        game.setId(100L);
        game.setTimer(60L);
        game.setCurrentRound(3);
        game.setPhase(GamePhase.SUDDEN_DEATH);

        GameGetDTO dto = DTOMapper.INSTANCE.convertEntityToGameGetDTO(game);

        assertEquals(100L, dto.getGameId());
        assertEquals(60L, dto.getTimer());
        assertEquals(3, dto.getCurrentRound());
        assertEquals(GamePhase.SUDDEN_DEATH.toString(), dto.getPhase());
    }

    @Test
    public void convertEntityToRoomGetDTO_nullLists_returnsZeroPlayerCount() {
        Room room = new Room();
        room.setId(2L);
        room.setName("EmptyRoom");
        room.setUsers(null);
        room.setWriters(null);
        room.setJudges(null);

        RoomGetDTO roomGetDTO = DTOMapper.INSTANCE.convertEntityToRoomGetDTO(room);

        assertNotNull(roomGetDTO);
        assertEquals(0, roomGetDTO.getPlayerCount());
    }

    // --- DIRECT DTO POJO TESTS ---

    @Test
    public void testUserPutDTO_gettersSetters() {
        UserPutDTO userPutDTO = new UserPutDTO();
        userPutDTO.setBio("This is a new bio");
        assertEquals("This is a new bio", userPutDTO.getBio());
    }

    @Test
    public void testUserPasswordPutDTO_gettersSetters() {
        UserPasswordPutDTO dto = new UserPasswordPutDTO();
        dto.setCurrentPassword("oldPassword123");
        dto.setNewPassword("newPassword456");

        assertEquals("oldPassword123", dto.getCurrentPassword());
        assertEquals("newPassword456", dto.getNewPassword());
    }

    @Test
    public void testUserDeleteDTO_gettersSetters() {
        UserDeleteDTO userDeleteDTO = new UserDeleteDTO();
        userDeleteDTO.setPassword("permanentDeletePassword");
        assertEquals("permanentDeletePassword", userDeleteDTO.getPassword());
    }

    // --- STATISTICS MAPPING ---

    @Test
    public void convertEntityToUserStatisticsGetDTO_success() {
        ch.uzh.ifi.hase.soprafs26.entity.UserStatistics stats = new ch.uzh.ifi.hase.soprafs26.entity.UserStatistics();
        stats.setGamesPlayed(50);
        stats.setGamesWon(25);
        stats.setHighestWinStreak(5);
        stats.getWinsByGenre().put("Sci-Fi", 12);

        UserStatisticsGetDTO dto = DTOMapper.INSTANCE.convertEntityToUserStatisticsGetDTO(stats);

        assertEquals(50, dto.getGamesPlayed());
        assertEquals(25, dto.getGamesWon());
        assertEquals(5, dto.getHighestWinStreak());
        assertEquals(12, dto.getWinsByGenre().get("Sci-Fi"));
    }

    // --- NEW ACHIEVEMENT & TROPHY MAPPING ---

    @Test
    public void convertEntityToAchievementGetDTO_success() {
        Achievement achievement = new Achievement();
        achievement.setId(1L);
        achievement.setName("ROOKIE_SCRIBE");
        achievement.setDisplayName("Rookie Scribe");
        achievement.setDescription("First game finished");
        achievement.setIcon("feather");

        AchievementGetDTO dto = DTOMapper.INSTANCE.convertEntityToAchievementGetDTO(achievement);

        assertEquals(achievement.getName(), dto.getName());
        assertEquals(achievement.getDisplayName(), dto.getDisplayName());
        assertEquals("feather", dto.getIcon());
    }

    @Test
    public void convertEntityToUserAchievementGetDTO_success() {
        User user = new User();
        user.setUsername("trophyWinner");

        Achievement ach = new Achievement();
        ach.setDisplayName("Legend");

        UserAchievement ua = new UserAchievement();
        ua.setId(500L);
        ua.setUser(user);
        ua.setAchievement(ach);
        ua.setUnlockedAt(new Date());
        ua.setIsDisplayed(true);

        UserAchievementGetDTO dto = DTOMapper.INSTANCE.convertEntityToUserAchievementGetDTO(ua);

        assertNotNull(dto);
        assertEquals("trophyWinner", dto.getUsername());
        assertEquals("Legend", dto.getAchievement().getDisplayName());
        assertTrue(dto.getIsDisplayed());
    }

    // --- NEW GENRE MASTER MAPPING ---

    @Test
    public void convertEntityToGenreMasterGetDTO_success() {
        User king = new User();
        king.setUsername("TheMaster");

        GenreMaster gm = new GenreMaster();
        gm.setId(1L);
        gm.setGenre("Horror");
        gm.setCurrentMaster(king);

        Map<Long, Integer> votes = new HashMap<>();
        votes.put(101L, 10);
        votes.put(102L, 5);
        votes.put(103L, 2);
        gm.setVotes(votes);

        GenreMasterGetDTO dto = DTOMapper.INSTANCE.convertEntityToGenreMasterGetDTO(gm);

        assertEquals("Horror", dto.getGenre());
        assertEquals("TheMaster", dto.getCurrentMaster().getUsername());
        assertEquals(3, dto.getTotalVotesCast());
    }

    // --- ADDITIONAL MISSING MAPPERS ---

    @Test
    public void convertEntityToUserPersonalGetDTO_success() {
        User user = new User();
        user.setId(1L);
        user.setUsername("karim");
        user.setToken("secret-token");
        user.setBio("Hello world");

        UserPersonalGetDTO dto = DTOMapper.INSTANCE.convertEntityToUserPersonalGetDTO(user);

        assertEquals("secret-token", dto.getToken());
        assertEquals("Hello world", dto.getBio());
    }

    @Test
    public void convertEntityToChatMessageGetDTO_success() {
        ChatMessage msg = new ChatMessage();
        msg.setUsername("sender");
        msg.setMessage("Hello!");
        msg.setTimestamp(new Date());

        ChatMessageGetDTO dto = DTOMapper.INSTANCE.convertEntityToChatMessageGetDTO(msg);

        assertEquals("sender", dto.getUsername());
        assertEquals("Hello!", dto.getMessage());
        assertNotNull(dto.getTimestamp());
    }

    @Test
    public void convertEntityToGameGetDTO_verifiesTimerExpression() {
        Game game = new Game();
        game.setTimer(100L);

        GameGetDTO dto = DTOMapper.INSTANCE.convertEntityToGameGetDTO(game);

        assertEquals(50L, dto.getReducedTimeThreshold());
    }

    @Test
    public void convertRoomPostDTOtoEntity_success() {
        RoomPostDTO dto = new RoomPostDTO();
        dto.setName("Thriller Room");

        Room room = DTOMapper.INSTANCE.convertRoomPostDTOtoEntity(dto);

        assertEquals("Thriller Room", room.getName());
    }
}