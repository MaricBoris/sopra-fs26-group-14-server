package ch.uzh.ifi.hase.soprafs26.rest.mapper;

import ch.uzh.ifi.hase.soprafs26.entity.*;
import ch.uzh.ifi.hase.soprafs26.rest.dto.room.RoomGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.user.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class DTOMapperTest {

    @Test
    public void testCreateUser_fromUserPostDTO_toUser_success() {
        UserPostDTO userPostDTO = new UserPostDTO();
        userPostDTO.setUsername("testUsername");
        userPostDTO.setPassword("testPassword123");

        User user = DTOMapper.INSTANCE.convertUserPostDTOtoEntity(userPostDTO);

        assertEquals(userPostDTO.getUsername(), user.getUsername());
        assertEquals(userPostDTO.getPassword(), user.getPassword());
    }

    @Test
    public void testGetUser_fromUser_toUserGetDTO_success() {
        User user = new User();
        user.setId(1L);
        user.setUsername("publicUser");

        // 1. Manually trigger uncovered User setter
        Date now = new Date();
        user.setCreationDate(now);

        UserGetDTO userGetDTO = DTOMapper.INSTANCE.convertEntityToUserGetDTO(user);

        assertEquals(user.getId(), userGetDTO.getId());
        assertEquals(user.getUsername(), userGetDTO.getUsername());
        assertEquals(now, user.getCreationDate()); // Verification
    }

    @Test
    public void testGetWriter_fromWriter_toWriterGetDTO_success() {
        // 1. Setup
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

        // 2. MAP
        WriterGetDTO dto = DTOMapper.INSTANCE.convertEntityToWriterGetDTO(writer);

        // 3. COVER WriterGetDTO (Fixes the 0% in your latest screenshot)
        assertEquals(user.getId(), dto.getId());
        assertEquals(user.getUsername(), dto.getUsername());
        assertEquals(true, dto.getTurn());              // <--- Hits getTurn()
        assertEquals("Sci-Fi", dto.getGenre());         // <--- Hits getGenre()
        assertEquals("The story begins...", dto.getText()); // <--- Hits getText()
        assertEquals("To be or not to be.", dto.getQuote());

        // 4. COVER Writer Entity (Keeps Entity at 100%)
        assertEquals(55L, writer.getId());
        assertEquals(time, writer.getLastSeenAt());
    }

    @Test
    public void testGetJudge_fromJudge_toJudgeGetDTO_success() {
        // 1. Setup
        User user = new User();
        user.setId(20L);
        user.setUsername("judgeUser");

        Judge judge = new Judge();
        judge.setUser(user);
        judge.setId(88L);
        judge.setInsertions(5L);
        Long time = 67890L;
        judge.setLastSeenAt(time);

        // 2. MAP
        JudgeGetDTO dto = DTOMapper.INSTANCE.convertEntityToJudgeGetDTO(judge);

        // 3. COVER JudgeGetDTO
        assertEquals(user.getId(), dto.getId());
        assertEquals(user.getUsername(), dto.getUsername());
        assertEquals(5L, dto.getInsertions()); // <--- Hits getInsertions()

        // 4. COVER Judge Entity
        assertEquals(88L, judge.getId());
        assertEquals(time, judge.getLastSeenAt());
    }

    @Test
    public void testGetStory_fromStory_toStoryGetDTO_success() {
        User winner = new User(); winner.setUsername("win");
        User loser = new User(); loser.setUsername("loss");

        // 7. Use the large constructor to cover it
        Story story = new Story(winner, loser, "Old text", true, "WinG", "LoseG", new ArrayList<>());

        // 8. Trigger uncovered setStoryText
        story.setStoryText("New story text");

        // 9. Trigger uncovered getJudges
        User j = new User();
        story.getJudges().add(j);

        StoryGetDTO storyGetDTO = DTOMapper.INSTANCE.convertEntityToStoryGetDTO(story);

        assertEquals("New story text", storyGetDTO.getStoryText());
        assertEquals(winner.getUsername(), storyGetDTO.getWinnerUsername());
        assertEquals(1, story.getJudges().size());
        assertNotNull(story.getCreationDate());
    }

    @Test
    public void testGetRoom_nullLists_returnsZeroPlayerCount() {
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
}