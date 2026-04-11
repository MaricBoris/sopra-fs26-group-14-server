package ch.uzh.ifi.hase.soprafs26.rest.mapper;

import ch.uzh.ifi.hase.soprafs26.entity.*;
import ch.uzh.ifi.hase.soprafs26.rest.dto.game.GameGetDTO;

import ch.uzh.ifi.hase.soprafs26.rest.dto.room.RoomGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.room.RoomPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.user.*;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

/**
 * DTOMapper
 * This class is responsible for generating classes that will automatically
 * transform/map the internal representation
 * of an entity (e.g., the User) to the external/API representation (e.g.,
 * UserGetDTO for getting, UserPostDTO for creating)
 * and vice versa.
 * Additional mappers can be defined for new entities.
 * Always created one mapper for getting information (GET) and one mapper for
 * creating information (POST).
 */
@Mapper
public interface DTOMapper {

	DTOMapper INSTANCE = Mappers.getMapper(DTOMapper.class);

    @Mapping(source = "username", target = "username")
	@Mapping(source = "password", target = "password")
    @Mapping(source = "bio", target = "bio")
	User convertUserPostDTOtoEntity(UserPostDTO userPostDTO);

    //@Mapping(source = "history", target = "history")
	@Mapping(source = "id", target = "id")
	@Mapping(source = "username", target = "username")
    @Mapping(source = "bio", target = "bio")
    @Mapping(source = "creationDate", target = "creationDate")
	UserGetDTO convertEntityToUserGetDTO(User user);

    //@Mapping(source = "history", target = "history")
    @Mapping(source = "id", target = "id")
    @Mapping(source = "username", target = "username")
    @Mapping(source = "bio", target = "bio")
    @Mapping(source = "creationDate", target = "creationDate")
    @Mapping(source = "token", target = "token")
    UserPersonalGetDTO convertEntityToUserPersonalGetDTO(User user);

    @Mapping(source = "name", target = "name")
    Room convertRoomPostDTOtoEntity(RoomPostDTO roomPostDTO);

    @Mapping(source = "user.id", target = "id")
    @Mapping(source = "user.username", target = "username")
    @Mapping(source = "turn", target = "turn")
    @Mapping(source = "genre", target = "genre")
    @Mapping(source = "text", target = "text")
    WriterGetDTO convertEntityToWriterGetDTO(Writer writer);

    @Mapping(source = "user.id", target = "id")
    @Mapping(source = "user.username", target = "username")
    @Mapping(source = "insertions", target = "insertions")
    JudgeGetDTO convertEntityToJudgeGetDTO(Judge judge);

    @Mapping(source = "id", target = "id")
    @Mapping(source = "name", target = "name")
    @Mapping(source = "playerCount", target = "playerCount")
    @Mapping(source = "lobbyLeader", target = "lobbyLeader")
    @Mapping(source = "users", target = "users")
    @Mapping(source = "writers", target = "writers")
    @Mapping(source = "judges", target = "judges")
    RoomGetDTO convertEntityToRoomGetDTO(Room room);

    @Mapping(source = "id", target = "gameId")
    @Mapping(source = "writers", target = "writers")
    @Mapping(source = "judges", target = "judges")
    @Mapping(source = "timer", target = "timer")
    @Mapping(source = "story", target = "story")
    GameGetDTO convertEntityToGameGetDTO(Game game);

    
}
