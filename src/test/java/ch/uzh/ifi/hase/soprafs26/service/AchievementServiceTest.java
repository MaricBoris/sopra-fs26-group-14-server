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
}