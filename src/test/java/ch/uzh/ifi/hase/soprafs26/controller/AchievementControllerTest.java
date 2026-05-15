package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.entity.*;
import ch.uzh.ifi.hase.soprafs26.service.AchievementService;
import ch.uzh.ifi.hase.soprafs26.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AchievementController.class)
public class AchievementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AchievementService achievementService;

    @MockitoBean
    private UserService userService;

    private Achievement ach;
    private UserAchievement uAch;
    private GenreMaster master;

    @BeforeEach
    public void setup() {
        ach = new Achievement();
        ach.setId(1L);
        ach.setName("TEST_ACH");
        ach.setDisplayName("Test Achievement");

        User user = new User();
        user.setId(1L);
        user.setUsername("testUser");

        uAch = new UserAchievement();
        uAch.setId(10L);
        uAch.setAchievement(ach);
        uAch.setUser(user);

        master = new GenreMaster();
        master.setId(100L);
        master.setGenre("Horror");
        master.setCurrentMaster(user);

        given(userService.extractToken(anyString())).willReturn("token");
        given(userService.findUserFromToken(anyString())).willReturn(new User());
    }

    @Test
    public void getAllAchievements_200Ok() throws Exception {
        given(achievementService.getAllAchievements()).willReturn(Collections.singletonList(ach));

        mockMvc.perform(get("/achievements")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].displayName", is("Test Achievement")));
    }

    @Test
    public void getUnlockedByUser_200Ok() throws Exception {
        given(achievementService.getUnlockedByUserId(anyLong())).willReturn(Collections.singletonList(uAch));

        mockMvc.perform(get("/users/1/achievements")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].achievement.displayName", is("Test Achievement")))
                .andExpect(jsonPath("$[0].isDisplayed", is(true)));
    }

    @Test
    public void getAllGenreMasters_200Ok() throws Exception {
        given(achievementService.getAllGenreMasters()).willReturn(Collections.singletonList(master));

        mockMvc.perform(get("/genres/masters")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].genre", is("Horror")))
                .andExpect(jsonPath("$[0].currentMaster.username", is("testUser")));
    }

    // --- SINGLE RESOURCE LOOKUPS ---

    @Test
    public void getAchievementById_validId_200Ok() throws Exception {
        given(achievementService.getAchievementById(anyLong())).willReturn(ach);

        mockMvc.perform(get("/achievements/1")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is("TEST_ACH")));
    }

    @Test
    public void getGenreMasterById_validId_200Ok() throws Exception {
        given(achievementService.getGenreMasterById(anyLong())).willReturn(master);

        mockMvc.perform(get("/genres/masters/100")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(100)))
                .andExpect(jsonPath("$.genre", is("Horror")))
                .andExpect(jsonPath("$.currentMaster.username", is("testUser")));
    }

    // --- UNLOCKED ACHIEVEMENTS (TROPHIES) ---

    @Test
    public void getAllUnlocked_200Ok() throws Exception {
        given(achievementService.getAllUnlockedAchievements()).willReturn(Collections.singletonList(uAch));

        mockMvc.perform(get("/achievements/unlocked")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].username", is("testUser")))
                .andExpect(jsonPath("$[0].achievement.displayName", is("Test Achievement")));
    }

    @Test
    public void getUnlockedById_validId_200Ok() throws Exception {
        given(achievementService.getUnlockedAchievementById(anyLong())).willReturn(uAch);

        mockMvc.perform(get("/achievements/unlocked/10")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(10)))
                .andExpect(jsonPath("$.username", is("testUser")));
    }

    @Test
    public void getUnlockedByAchievement_validId_200Ok() throws Exception {
        given(achievementService.getUnlockedByAchievementId(anyLong())).willReturn(Collections.singletonList(uAch));

        mockMvc.perform(get("/achievements/1/unlocked")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].achievement.id", is(1)));
    }
}