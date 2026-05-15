package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.entity.*;
import ch.uzh.ifi.hase.soprafs26.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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
    @Autowired
    private GenreMasterRepository genreMasterRepository;

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

    // --- Achievement Integration Tests ---

    @Test
    public void getAllAchievements_returnsAllPersisted() {
        List<Achievement> results = achievementService.getAllAchievements();

        assertFalse(results.isEmpty());
        assertTrue(results.stream().anyMatch(a -> a.getName().equals("GLADIATOR")));
    }

    @Test
    public void getAchievementById_validId_returnsAchievement() {
        Achievement found = achievementService.getAchievementById(savedAch.getId());

        assertNotNull(found);
        assertEquals("GLADIATOR", found.getName());
        assertEquals("sword", found.getIcon());
    }

    @Test
    public void getAchievementById_invalidId_throws404() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> achievementService.getAchievementById(9999L));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    // --- UserAchievement Integration Tests ---

    @Test
    public void getAllUnlockedAchievements_returnsPersistedLinks() {
        UserAchievement link = new UserAchievement();
        link.setUser(savedUser);
        link.setAchievement(savedAch);
        userAchievementRepository.save(link);

        List<UserAchievement> results = achievementService.getAllUnlockedAchievements();

        assertFalse(results.isEmpty());
        assertTrue(results.stream().anyMatch(u -> u.getAchievement().getName().equals("GLADIATOR")));
    }

    @Test
    public void getUnlockedAchievementById_validId_returnsLink() {
        UserAchievement link = new UserAchievement();
        link.setUser(savedUser);
        link.setAchievement(savedAch);
        link = userAchievementRepository.save(link);

        UserAchievement found = achievementService.getUnlockedAchievementById(link.getId());

        assertNotNull(found);
        assertEquals(savedAch.getId(), found.getAchievement().getId());
        assertEquals(savedUser.getId(), found.getUser().getId());
    }

    @Test
    public void getUnlockedAchievementById_invalidId_throws404() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> achievementService.getUnlockedAchievementById(9999L));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    public void getUnlockedByAchievementId_returnsPersistedLinks() {
        UserAchievement link = new UserAchievement();
        link.setUser(savedUser);
        link.setAchievement(savedAch);
        userAchievementRepository.save(link);

        List<UserAchievement> results = achievementService.getUnlockedByAchievementId(savedAch.getId());

        assertEquals(1, results.size());
        assertEquals(savedUser.getId(), results.get(0).getUser().getId());
    }

    // --- GenreMaster Integration Tests ---

    @Test
    public void getAllGenreMasters_returnsPersistedMasters() {
        GenreMaster gm = new GenreMaster();
        gm.setGenre("Sci-Fi");
        gm.setCurrentMaster(savedUser);
        genreMasterRepository.save(gm);

        List<GenreMaster> results = achievementService.getAllGenreMasters();

        assertFalse(results.isEmpty());
        assertTrue(results.stream().anyMatch(g -> g.getGenre().equals("Sci-Fi")));
        assertEquals(savedUser.getId(), results.get(0).getCurrentMaster().getId());
    }

    @Test
    public void getGenreMasterById_validId_returnsMaster() {
        GenreMaster gm = new GenreMaster();
        gm.setGenre("Fantasy");
        gm.setCurrentMaster(savedUser);
        gm = genreMasterRepository.save(gm);

        GenreMaster found = achievementService.getGenreMasterById(gm.getId());

        assertNotNull(found);
        assertEquals("Fantasy", found.getGenre());
        assertEquals("trophyHunter", found.getCurrentMaster().getUsername());
    }

    @Test
    public void getGenreMasterById_invalidId_throws404() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> achievementService.getGenreMasterById(9999L));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }
}
