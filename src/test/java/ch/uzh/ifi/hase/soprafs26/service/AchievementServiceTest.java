package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.entity.Achievement;
import ch.uzh.ifi.hase.soprafs26.repository.AchievementRepository;
import ch.uzh.ifi.hase.soprafs26.repository.GenreMasterRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserAchievementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.server.ResponseStatusException;
import ch.uzh.ifi.hase.soprafs26.entity.GenreMaster;
import ch.uzh.ifi.hase.soprafs26.entity.UserAchievement;
import org.springframework.http.HttpStatus;

import java.util.Collections;
import java.util.List;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;

public class AchievementServiceTest {

    @Mock
    private AchievementRepository achievementRepository;
    @Mock
    private UserAchievementRepository userAchievementRepository;
    @Mock
    private GenreMasterRepository genreMasterRepository;
    @Mock
    private UserService userService;

    @InjectMocks
    private AchievementService achievementService;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void getAchievementById_success() {
        Achievement ach = new Achievement();
        ach.setId(1L);
        given(achievementRepository.findById(1L)).willReturn(Optional.of(ach));

        Achievement result = achievementService.getAchievementById(1L);
        assertEquals(1L, result.getId());
    }

    @Test
    public void getAchievementById_notFound_throws404() {
        given(achievementRepository.findById(1L)).willReturn(Optional.empty());
        assertThrows(ResponseStatusException.class, () -> achievementService.getAchievementById(1L));
    }

    @Test
    public void getAllAchievements_returnsList() {
        Achievement ach = new Achievement();
        ach.setId(1L);
        given(achievementRepository.findAll()).willReturn(Collections.singletonList(ach));

        List<Achievement> result = achievementService.getAllAchievements();
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
    }

    @Test
    public void getAllUnlockedAchievements_returnsList() {
        UserAchievement uAch = new UserAchievement();
        uAch.setId(10L);
        given(userAchievementRepository.findAll()).willReturn(Collections.singletonList(uAch));

        List<UserAchievement> result = achievementService.getAllUnlockedAchievements();
        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).getId());
    }

    @Test
    public void getUnlockedAchievementById_success() {
        UserAchievement uAch = new UserAchievement();
        uAch.setId(10L);
        given(userAchievementRepository.findById(10L)).willReturn(Optional.of(uAch));

        UserAchievement result = achievementService.getUnlockedAchievementById(10L);
        assertEquals(10L, result.getId());
    }

    @Test
    public void getUnlockedAchievementById_notFound_throws404() {
        given(userAchievementRepository.findById(99L)).willReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> achievementService.getUnlockedAchievementById(99L));
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("Unlock record not found", exception.getReason());
    }

    @Test
    public void getUnlockedByAchievementId_returnsList() {
        UserAchievement uAch = new UserAchievement();
        uAch.setId(10L);
        given(userAchievementRepository.findByAchievementId(1L)).willReturn(Collections.singletonList(uAch));

        List<UserAchievement> result = achievementService.getUnlockedByAchievementId(1L);
        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).getId());
    }

    @Test
    public void getAllGenreMasters_returnsList() {
        GenreMaster gm = new GenreMaster();
        gm.setId(100L);
        given(genreMasterRepository.findAll()).willReturn(Collections.singletonList(gm));

        List<GenreMaster> result = achievementService.getAllGenreMasters();
        assertEquals(1, result.size());
        assertEquals(100L, result.get(0).getId());
    }

    @Test
    public void getGenreMasterById_success() {
        GenreMaster gm = new GenreMaster();
        gm.setId(100L);
        given(genreMasterRepository.findById(100L)).willReturn(Optional.of(gm));

        GenreMaster result = achievementService.getGenreMasterById(100L);
        assertEquals(100L, result.getId());
    }

    @Test
    public void getGenreMasterById_notFound_throws404() {
        given(genreMasterRepository.findById(999L)).willReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> achievementService.getGenreMasterById(999L));
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("Genre Master record not found", exception.getReason());
    }
}
