package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.entity.Room;
import ch.uzh.ifi.hase.soprafs26.rest.dto.room.RoomGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.room.RoomPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs26.service.RoomService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
public class RoomController {

    private final RoomService roomService;

    RoomController(RoomService roomService) { this.roomService = roomService; }

    @PostMapping("/rooms")
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    public RoomGetDTO createRoom(@RequestBody RoomPostDTO roomPostDTO,
                                 @RequestHeader(value = "Authorization", required = false) String bearerToken) {

        Room roomInput = DTOMapper.INSTANCE.convertRoomPostDTOtoEntity(roomPostDTO);

        Room createdRoom = roomService.createRoom(roomInput, bearerToken);

        return DTOMapper.INSTANCE.convertEntityToRoomGetDTO(createdRoom);
    }

    @GetMapping("/rooms")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public List<RoomGetDTO> getAllRooms(@RequestHeader("Authorization") String bearerToken) {
        List<Room> rooms = roomService.getRooms(bearerToken);
        List<RoomGetDTO> roomGetDTOs = new ArrayList<>();

        for (Room room : rooms) {
            roomGetDTOs.add(DTOMapper.INSTANCE.convertEntityToRoomGetDTO(room));
        }
        return roomGetDTOs;
    }

    @PutMapping("/rooms/{roomId}/join")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public RoomGetDTO joinRoom(@PathVariable Long roomId, @RequestHeader("Authorization") String bearerToken) {
        Room joinedRoom = roomService.joinRoom(roomId, bearerToken);
        return DTOMapper.INSTANCE.convertEntityToRoomGetDTO(joinedRoom);
    }
}