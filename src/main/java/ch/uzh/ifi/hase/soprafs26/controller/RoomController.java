package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.entity.Room;
import ch.uzh.ifi.hase.soprafs26.rest.dto.room.RoomGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.room.RoomPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs26.service.RoomService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
public class RoomController {

    private final RoomService roomService;

    RoomController(RoomService roomService) { this.roomService = roomService; }

    @PostMapping("/rooms")
    @ResponseStatus(HttpStatus.CREATED) // Matches "201" in Spec
    @ResponseBody
    public RoomGetDTO createRoom(@RequestBody RoomPostDTO roomPostDTO,
                                 @RequestHeader(value = "Authorization", required = false) String bearerToken) {

        Room roomInput = DTOMapper.INSTANCE.convertRoomPostDTOtoEntity(roomPostDTO);

        // Delegate logic to service
        Room createdRoom = roomService.createRoom(roomInput, bearerToken);

        return DTOMapper.INSTANCE.convertEntityToRoomGetDTO(createdRoom);
    }
}