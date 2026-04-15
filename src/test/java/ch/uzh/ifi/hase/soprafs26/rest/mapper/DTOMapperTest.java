package ch.uzh.ifi.hase.soprafs26.rest.mapper;

import ch.uzh.ifi.hase.soprafs26.entity.*;
import ch.uzh.ifi.hase.soprafs26.rest.dto.game.GameGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.room.RoomGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.room.RoomPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.user.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * DTOMapperTest
 * Verifies that MapStruct correctly translates between the internal Entity
 * and the various DTO representations used by the Controller.
 */
public class DTOMapperTest {

    @Test
    public void testCreateUser_fromUserPostDTO_toUser_success() {
        // create UserPostDTO
        UserPostDTO userPostDTO = new UserPostDTO();
        userPostDTO.setUsername("testUsername");
        userPostDTO.setPassword("testPassword123");

        // MAP -> Create user
        User user = DTOMapper.INSTANCE.convertUserPostDTOtoEntity(userPostDTO);

        // check content
        assertEquals(userPostDTO.getUsername(), user.getUsername());
        assertEquals(userPostDTO.getPassword(), user.getPassword());
    }

    @Test
    public void testGetUser_fromUser_toUserGetDTO_success() {
        // create User
        User user = new User();
        user.setId(1L);
        user.setUsername("publicUser");

        // MAP -> Create UserGetDTO
        UserGetDTO userGetDTO = DTOMapper.INSTANCE.convertEntityToUserGetDTO(user);

        // check content
        assertEquals(user.getId(), userGetDTO.getId());
        assertEquals(user.getUsername(), userGetDTO.getUsername());
    }

    @Test
    public void testGetPersonalUser_fromUser_toUserPersonalGetDTO_success() {
        // create User with full details
        User user = new User();
        user.setId(1L);
        user.setUsername("privateUser");
        user.setToken("secret-uuid-token");
        user.setBio("This is my private bio.");

        // MAP -> Create UserPersonalGetDTO
        UserPersonalGetDTO personalDTO = DTOMapper.INSTANCE.convertEntityToUserPersonalGetDTO(user);

        // check content - specifically the sensitive/extra fields
        assertEquals(user.getId(), personalDTO.getId());
        assertEquals(user.getUsername(), personalDTO.getUsername());
        assertEquals(user.getToken(), personalDTO.getToken());
        assertEquals(user.getBio(), personalDTO.getBio());
    }

    @Test
    public void testCreateRoom_fromRoomPostDTO_toRoom_success() {
        // create RoomPostDTO
        RoomPostDTO roomPostDTO = new RoomPostDTO();
        roomPostDTO.setName("TestRoom");

        // MAP -> Create Room
        Room room = DTOMapper.INSTANCE.convertRoomPostDTOtoEntity(roomPostDTO);

        // check content
        assertEquals(roomPostDTO.getName(), room.getName());
    }

    @Test
    public void testGetWriter_fromWriter_toWriterGetDTO_success() {
        // 1. Create User for the Writer
        User user = new User();
        user.setId(10L);
        user.setUsername("writerUser");

        // 2. Create Writer and set ALL fields including the Quote
        Writer writer = new Writer(user);
        writer.setGenre("Sci-Fi");
        writer.setTurn(true);
        writer.setText("Once upon a time...");
        writer.setQuote("The pen is mightier than the sword."); // <--- SET THIS

        // 3. MAP
        WriterGetDTO writerGetDTO = DTOMapper.INSTANCE.convertEntityToWriterGetDTO(writer);

        // 4. CHECK content
        assertEquals(user.getId(), writerGetDTO.getId());
        assertEquals(user.getUsername(), writerGetDTO.getUsername());
        assertEquals(writer.getGenre(), writerGetDTO.getGenre());
        assertEquals(writer.getTurn(), writerGetDTO.getTurn());
        assertEquals(writer.getText(), writerGetDTO.getText());

        // 5. This call triggers the getter and fixes SonarCloud
        assertEquals(writer.getQuote(), writerGetDTO.getQuote());
    }

    @Test
    public void testGetJudge_fromJudge_toJudgeGetDTO_success() {
        // create User for the Judge
        User user = new User();
        user.setId(20L);
        user.setUsername("judgeUser");

        // create Judge (Composition)
        Judge judge = new Judge(user);
        judge.setInsertions(5L);

        // MAP -> Create JudgeGetDTO
        JudgeGetDTO judgeGetDTO = DTOMapper.INSTANCE.convertEntityToJudgeGetDTO(judge);

        // check flattened content
        assertEquals(user.getId(), judgeGetDTO.getId());
        assertEquals(user.getUsername(), judgeGetDTO.getUsername());
        assertEquals(judge.getInsertions(), judgeGetDTO.getInsertions());
    }

    @Test
    public void testGetRoom_fromRoom_toRoomGetDTO_success() {
        // 1. Create a Leader with an ID
        User leader = new User();
        leader.setId(1L);
        leader.setUsername("leaderUser");

        // 2. Create the Room
        Room room = new Room();
        room.setId(100L);
        room.setName("TestRoom");
        room.setPlayerCount(1);
        room.setLobbyLeader(leader);

        // Optional: Add the leader to the users list as well to match real logic
        room.getUsers().add(leader);

        // 3. MAP
        RoomGetDTO roomGetDTO = DTOMapper.INSTANCE.convertEntityToRoomGetDTO(room);

        // 4. CHECK
        assertEquals(room.getId(), roomGetDTO.getId());
        assertEquals(room.getName(), roomGetDTO.getName());
        assertEquals(1, roomGetDTO.getPlayerCount());

        assertNotNull(roomGetDTO.getLobbyLeader());
        assertEquals(leader.getId(), roomGetDTO.getLobbyLeader().getId());
        assertEquals(leader.getUsername(), roomGetDTO.getLobbyLeader().getUsername());
    }

    @Test
    public void testGetGame_fromGame_toGameGetDTO_success() {
        // create Game
        Game game = new Game();
        game.setId(500L);
        game.setTimer(90L);

        // Add a writer to test nested list mapping
        User user = new User();
        user.setUsername("nestedWriter");
        Writer writer = new Writer(user);
        game.getWriters().add(writer);

        // MAP -> Create GameGetDTO
        GameGetDTO gameGetDTO = DTOMapper.INSTANCE.convertEntityToGameGetDTO(game);

        // check content
        assertEquals(game.getId(), gameGetDTO.getGameId());
        assertEquals(game.getTimer(), gameGetDTO.getTimer());
        assertEquals(1, gameGetDTO.getWriters().size());
        assertEquals(user.getUsername(), gameGetDTO.getWriters().get(0).getUsername());
    }

    @Test
    public void testGetRoom_nullLists_returnsZeroPlayerCount() {
        // 1. Create a Room and explicitly break the initialization to test null-safety
        Room room = new Room();
        room.setId(2L);
        room.setName("EmptyRoom");
        room.setUsers(null);
        room.setWriters(null);
        room.setJudges(null);

        // 2. MAP -> MapStruct will map null source lists to null target lists
        RoomGetDTO roomGetDTO = DTOMapper.INSTANCE.convertEntityToRoomGetDTO(room);

        // 3. CHECK
        // This triggers the "else" branch ( : 0 ) in RoomGetDTO.getPlayerCount()
        assertNotNull(roomGetDTO);
        assertEquals(0, roomGetDTO.getPlayerCount());
    }
}