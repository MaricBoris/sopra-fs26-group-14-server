package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.entity.Achievement;
import ch.uzh.ifi.hase.soprafs26.entity.GenreMaster;
import ch.uzh.ifi.hase.soprafs26.entity.UserAchievement;
import ch.uzh.ifi.hase.soprafs26.repository.AchievementRepository;
import ch.uzh.ifi.hase.soprafs26.repository.GenreMasterRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserAchievementRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional
public class AchievementService {

    private final AchievementRepository achievementRepository;
    private final UserAchievementRepository userAchievementRepository;
    private final UserService userService;
    private final GenreMasterRepository genreMasterRepository;

    public AchievementService(AchievementRepository achievementRepository,
                              UserAchievementRepository userAchievementRepository,
                              UserService userService,  GenreMasterRepository genreMasterRepository) {
        this.achievementRepository = achievementRepository;
        this.userAchievementRepository = userAchievementRepository;
        this.userService = userService;
        this.genreMasterRepository = genreMasterRepository;
    }

    public List<Achievement> getAllAchievements() {
        return achievementRepository.findAll();
    }

    public Achievement getAchievementById(Long id) {
        return achievementRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Achievement not found"));
    }

    public List<UserAchievement> getAllUnlockedAchievements() {
        return userAchievementRepository.findAll();
    }

    public UserAchievement getUnlockedAchievementById(Long id) {
        return userAchievementRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unlock record not found"));
    }

    public List<UserAchievement> getUnlockedByAchievementId(Long achievementId) {
        return userAchievementRepository.findByAchievementId(achievementId);
    }

    public List<UserAchievement> getUnlockedByUserId(Long userId) {
        return userAchievementRepository.findByUserId(userId);
    }

    public List<GenreMaster> getAllGenreMasters() {
        return genreMasterRepository.findAll();
    }

    public GenreMaster getGenreMasterById(Long id) {
        return genreMasterRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Genre Master record not found"));
    }
}