package ch.uzh.ifi.hase.soprafs26.rest.mapper;

import ch.uzh.ifi.hase.soprafs26.entity.*;
import ch.uzh.ifi.hase.soprafs26.rest.dto.game.GameGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.room.RoomGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.user.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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

    @Test
    public void convertEntityToUserPersonalGetDTO_success() {
        User user = new User();
        user.setId(1L);
        user.setUsername("personalUser");
        user.setToken("secret-token");

        UserPersonalGetDTO dto = DTOMapper.INSTANCE.convertEntityToUserPersonalGetDTO(user);

        assertEquals(user.getId(), dto.getId());
        assertEquals(user.getToken(), dto.getToken());
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
        writer.setText("The story begins...");
        writer.setQuote("To be or not to be.");
        Long time = 12345L;
        writer.setLastSeenAt(time);

        WriterGetDTO dto = DTOMapper.INSTANCE.convertEntityToWriterGetDTO(writer);

        assertEquals(user.getId(), dto.getId());
        assertEquals(user.getUsername(), dto.getUsername());
        assertEquals(true, dto.getTurn());
        assertEquals("Sci-Fi", dto.getGenre());
        assertEquals("The story begins...", dto.getText());
        assertEquals("To be or not to be.", dto.getQuote());
        assertEquals(55L, writer.getId());
        assertEquals(time, writer.getLastSeenAt());
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
        Long time = 67890L;
        judge.setLastSeenAt(time);

        JudgeGetDTO dto = DTOMapper.INSTANCE.convertEntityToJudgeGetDTO(judge);

        assertEquals(user.getId(), dto.getId());
        assertEquals(user.getUsername(), dto.getUsername());
        assertEquals(5L, dto.getInsertions());
        assertEquals(88L, judge.getId());
        assertEquals(time, judge.getLastSeenAt());
    }

    // --- GAME, ROOM & STORY MAPPING ---

    @Test
    public void convertEntityToStoryGetDTO_success() {
        User winner = new User(); winner.setUsername("win");
        User loser = new User(); loser.setUsername("loss");
        Story story = new Story(winner, loser, "Old text", true, "WinG", "LoseG", new ArrayList<>());

        story.setStoryText("New story text");
        User j = new User();
        story.getJudges().add(j);

        StoryGetDTO storyGetDTO = DTOMapper.INSTANCE.convertEntityToStoryGetDTO(story);

        assertEquals("New story text", storyGetDTO.getStoryText());
        assertEquals(winner.getUsername(), storyGetDTO.getWinnerUsername());
        assertEquals(1, story.getJudges().size());
        assertNotNull(story.getCreationDate());
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

    @Test
    public void convertEntityToGameGetDTO_success() {
        Game game = new Game();
        game.setId(100L);
        game.setTimer(90L);
        game.setCurrentRound(1);

        GameGetDTO dto = DTOMapper.INSTANCE.convertEntityToGameGetDTO(game);

        assertEquals(100L, dto.getGameId());
        assertEquals(90L, dto.getTimer());
        assertEquals(1, dto.getCurrentRound());
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
}