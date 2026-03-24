package ch.uzh.ifi.hase.soprafs26.rest.mapper;

import org.junit.jupiter.api.Test;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserPersonalGetDTO;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}