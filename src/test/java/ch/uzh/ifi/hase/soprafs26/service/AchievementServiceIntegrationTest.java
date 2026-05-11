package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.entity.*;
import ch.uzh.ifi.hase.soprafs26.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class AchievementServiceIntegrationTest {

    @Autowired
    private AchievementRepository achievementRepository;
    @Autowired
    private UserAchievementRepository userAchievementRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AchievementService achievementService;

    private User savedUser;
    private Achievement savedAch;

    @BeforeEach
    public void setup() {
        userRepository.deleteAll();
        achievementRepository.deleteAll();

        User user = new User();
        user.setUsername("trophyHunter");
        user.setPassword("pass");
        user.setToken("dummy-token-123");

        savedUser = userRepository.save(user);

        Achievement ach = new Achievement();
        ach.setName("GLADIATOR");
        ach.setDisplayName("Gladiator");
        ach.setDescription("Win a game");
        ach.setIcon("sword");
        savedAch = achievementRepository.save(ach);
    }

    @Test
    public void getUnlockedByUserId_returnsPersistedLink() {
        UserAchievement link = new UserAchievement();
        link.setUser(savedUser);
        link.setAchievement(savedAch);
        userAchievementRepository.save(link);

        List<UserAchievement> results = achievementService.getUnlockedByUserId(savedUser.getId());

        assertEquals(1, results.size());
        assertEquals("GLADIATOR", results.get(0).getAchievement().getName());
    }
}