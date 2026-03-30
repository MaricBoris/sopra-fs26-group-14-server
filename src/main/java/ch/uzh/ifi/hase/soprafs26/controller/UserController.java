package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.rest.dto.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs26.service.UserService;

import java.util.ArrayList;
import java.util.List;

/**
 * User Controller
 * This class is responsible for handling all REST request that are related to
 * the user.
 * The controller will receive the request and delegate the execution to the
 * UserService and finally return the result.
 */
@RestController
public class UserController {

    private final UserService userService;

    UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/users")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public List<UserGetDTO> getAllUsers(@RequestHeader(value = "Authorization", required = false) String bearerToken) {
        // fetch all users in the internal representation
        List<User> users = userService.getUsers(bearerToken);
        List<UserGetDTO> userGetDTOs = new ArrayList<>();

        // convert each user to the API representation
        for (User user : users) {
            userGetDTOs.add(DTOMapper.INSTANCE.convertEntityToUserGetDTO(user));
        }
        return userGetDTOs;
    }

    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    public UserPersonalGetDTO createUser(@RequestBody UserPostDTO userPostDTO) {
        // convert API user to internal representation
        User userInput = DTOMapper.INSTANCE.convertUserPostDTOtoEntity(userPostDTO);

        // create user
        User createdUser = userService.createUser(userInput);
        // convert internal representation of user back to API
        return DTOMapper.INSTANCE.convertEntityToUserPersonalGetDTO(createdUser);
    }

    @PostMapping("/users/login")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public UserPersonalGetDTO login(@RequestBody UserPostDTO userPostDTO) {
        User userInput = DTOMapper.INSTANCE.convertUserPostDTOtoEntity(userPostDTO);

        User createdUser = userService.loginUser(userInput);

        return DTOMapper.INSTANCE.convertEntityToUserPersonalGetDTO(createdUser);
    }

    @PostMapping("/users/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ResponseBody
    public void logout(@RequestHeader(value = "Authorization", required = false) String bearerToken) {
        userService.logoutUser(bearerToken);
    }

    @GetMapping("/users/{id}")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public UserGetDTO findUserFromId(@PathVariable Long id, @RequestHeader("Authorization") String authHeader) {
        User foundUserId = userService.findUserFromId(id);

        String token = authHeader;
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        //User foundUserToken = userService.findUserFromToken(token);
        //userService.checkUsersMatch(foundUserId, foundUserToken); this check makes it impossible to visit other profiles

        userService.findUserFromToken(token); //ensures only authenticated users can fetch

        return DTOMapper.INSTANCE.convertEntityToUserGetDTO(foundUserId);
    }

    @PutMapping("/users/{id}/password")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public void changePassword(@PathVariable Long id, @RequestBody UserPasswordPutDTO passwordDTO,
                               @RequestHeader("Authorization") String bearerToken) {
        userService.changePassword(id, passwordDTO, bearerToken);
    }

    @PutMapping("/users/{userId}")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public UserPersonalGetDTO updateUserBio(@PathVariable Long userId, @RequestBody UserPutDTO userPutDTO,
                                            @RequestHeader(value = "Authorization", required = false) String bearerToken) {

        User updatedUser = userService.updateUserBio(userId, userPutDTO, bearerToken);
        return DTOMapper.INSTANCE.convertEntityToUserPersonalGetDTO(updatedUser);
    }

    @DeleteMapping("/users/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ResponseBody
    public void deleteUser(@PathVariable Long id, @RequestBody(required = false) UserDeleteDTO deleteDTO,
                           @RequestHeader("Authorization") String bearerToken) {
        userService.deleteUser(id, deleteDTO, bearerToken);
    }
}