package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.entity.Room;
import ch.uzh.ifi.hase.soprafs26.rest.dto.game.GameGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.room.RoomGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.room.RoomPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.room.RoomRoleDTO;
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
    public List<RoomGetDTO> getAllRooms(@RequestHeader(value = "Authorization", required = false) String bearerToken) {
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
    public RoomGetDTO joinRoom(@PathVariable Long roomId, @RequestHeader(value = "Authorization", required = false) String bearerToken) {
        Room joinedRoom = roomService.joinRoom(roomId, bearerToken);
        return DTOMapper.INSTANCE.convertEntityToRoomGetDTO(joinedRoom);
    }

    @PutMapping("/rooms/{roomId}/roles")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public RoomGetDTO updateRole(@PathVariable Long roomId,
                                 @RequestBody RoomRoleDTO roomRoleDTO,
                                 @RequestHeader(value = "Authorization", required = false) String bearerToken) {
        Room room = roomService.swapRole(roomId, roomRoleDTO.getRole(), bearerToken);
        return DTOMapper.INSTANCE.convertEntityToRoomGetDTO(room);
    }

    @GetMapping("/rooms/{roomId}")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public RoomGetDTO getRoom(@PathVariable Long roomId,
                              @RequestHeader(value = "Authorization", required = false) String bearerToken) {
        Room room = roomService.getRoomById(roomId, bearerToken);
        return DTOMapper.INSTANCE.convertEntityToRoomGetDTO(room);
    }

    @PutMapping("/rooms/{roomId}/leave")
    @ResponseStatus(HttpStatus.NO_CONTENT) // Matches "204" in Spec
    public void leaveRoom(@PathVariable Long roomId,
                          @RequestHeader(value = "Authorization", required = false) String bearerToken) {
        roomService.leaveRoom(roomId, bearerToken);
    }

    @PostMapping("/rooms/{roomId}")
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    public GameGetDTO startGame(@PathVariable Long roomId,
                                @RequestHeader(value = "Authorization", required = false) String bearerToken) {
        Game startedGame = roomService.startGame(roomId, bearerToken);

        return DTOMapper.INSTANCE.convertEntityToGameGetDTO(startedGame);
    }

    @PutMapping("/rooms/{roomId}/timer")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public RoomGetDTO updateTimer(@PathVariable Long roomId,
                                 @RequestBody Long timer,
                                 @RequestHeader(value = "Authorization", required = false) String bearerToken) {
        Room room = roomService.setTimer(roomId, timer, bearerToken);
        return DTOMapper.INSTANCE.convertEntityToRoomGetDTO(room);
    }

    @PutMapping("/rooms/{roomId}/rounds")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public RoomGetDTO updateMaxRounds(@PathVariable Long roomId,
                                 @RequestBody int rounds,
                                 @RequestHeader(value = "Authorization", required = false) String bearerToken) {
        Room room = roomService.setMaxRounds(roomId, rounds, bearerToken);
        return DTOMapper.INSTANCE.convertEntityToRoomGetDTO(room);
    }


}