package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.entity.Achievement;
import ch.uzh.ifi.hase.soprafs26.entity.UserAchievement;
import ch.uzh.ifi.hase.soprafs26.rest.dto.achvs.AchievementGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.achvs.GenreMasterGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.achvs.UserAchievementGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs26.service.AchievementService;
import ch.uzh.ifi.hase.soprafs26.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
public class AchievementController {

    private final AchievementService achievementService;
    private final UserService userService;

    public AchievementController(AchievementService achievementService, UserService userService) {
        this.achievementService = achievementService;
        this.userService = userService;
    }

    @GetMapping("/achievements")
    @ResponseStatus(HttpStatus.OK)
    public List<AchievementGetDTO> getAllAchievements(@RequestHeader("Authorization") String token) {
        userService.findUserFromToken(userService.extractToken(token));
        return achievementService.getAllAchievements().stream()
                .map(DTOMapper.INSTANCE::convertEntityToAchievementGetDTO)
                .collect(Collectors.toList());
    }

    @GetMapping("/achievements/{id}")
    @ResponseStatus(HttpStatus.OK)
    public AchievementGetDTO getAchievementById(@PathVariable Long id, @RequestHeader("Authorization") String token) {
        userService.findUserFromToken(userService.extractToken(token));
        return DTOMapper.INSTANCE.convertEntityToAchievementGetDTO(achievementService.getAchievementById(id));
    }

    @GetMapping("/achievements/unlocked")
    @ResponseStatus(HttpStatus.OK)
    public List<UserAchievementGetDTO> getAllUnlocked(@RequestHeader("Authorization") String token) {
        userService.findUserFromToken(userService.extractToken(token));
        return achievementService.getAllUnlockedAchievements().stream()
                .map(DTOMapper.INSTANCE::convertEntityToUserAchievementGetDTO)
                .collect(Collectors.toList());
    }

    @GetMapping("/achievements/unlocked/{id}")
    @ResponseStatus(HttpStatus.OK)
    public UserAchievementGetDTO getUnlockedById(@PathVariable Long id, @RequestHeader("Authorization") String token) {
        userService.findUserFromToken(userService.extractToken(token));
        return DTOMapper.INSTANCE.convertEntityToUserAchievementGetDTO(achievementService.getUnlockedAchievementById(id));
    }

    @GetMapping("/achievements/{id}/unlocked")
    @ResponseStatus(HttpStatus.OK)
    public List<UserAchievementGetDTO> getUnlockedByAchievement(@PathVariable Long id, @RequestHeader("Authorization") String token) {
        userService.findUserFromToken(userService.extractToken(token));
        return achievementService.getUnlockedByAchievementId(id).stream()
                .map(DTOMapper.INSTANCE::convertEntityToUserAchievementGetDTO)
                .collect(Collectors.toList());
    }

    @GetMapping("/users/{userId}/achievements")
    @ResponseStatus(HttpStatus.OK)
    public List<UserAchievementGetDTO> getUnlockedByUser(@PathVariable Long userId, @RequestHeader("Authorization") String token) {
        userService.findUserFromToken(userService.extractToken(token));
        return achievementService.getUnlockedByUserId(userId).stream()
                .map(DTOMapper.INSTANCE::convertEntityToUserAchievementGetDTO)
                .collect(Collectors.toList());
    }

    @GetMapping("/genres/masters")
    @ResponseStatus(HttpStatus.OK)
    public List<GenreMasterGetDTO> getAllGenreMasters(@RequestHeader("Authorization") String token) {
        userService.findUserFromToken(userService.extractToken(token));
        return achievementService.getAllGenreMasters().stream()
                .map(DTOMapper.INSTANCE::convertEntityToGenreMasterGetDTO)
                .collect(Collectors.toList());
    }

    @GetMapping("/genres/masters/{id}")
    @ResponseStatus(HttpStatus.OK)
    public GenreMasterGetDTO getGenreMasterById(@PathVariable Long id, @RequestHeader("Authorization") String token) {
        userService.findUserFromToken(userService.extractToken(token));
        return DTOMapper.INSTANCE.convertEntityToGenreMasterGetDTO(achievementService.getGenreMasterById(id));
    }
}