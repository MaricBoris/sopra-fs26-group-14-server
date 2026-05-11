package ch.uzh.ifi.hase.soprafs26;

import ch.uzh.ifi.hase.soprafs26.constant.AchievementType;
import ch.uzh.ifi.hase.soprafs26.entity.Achievement;
import ch.uzh.ifi.hase.soprafs26.repository.AchievementRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class AchievementSeeder implements CommandLineRunner {

    private final AchievementRepository achievementRepository;

    public AchievementSeeder(AchievementRepository achievementRepository) {
        this.achievementRepository = achievementRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        for (AchievementType type : AchievementType.values()) {
            seedAchievement(type);
        }
    }

    private void seedAchievement(AchievementType type) {
        if (achievementRepository.findByName(type.name()) == null) {
            Achievement a = new Achievement();
            a.setName(type.name());
            a.setDisplayName(type.getDisplayName());
            a.setDescription(type.getDescription());
            a.setIcon(type.getIcon());
            achievementRepository.save(a);
        }
    }
}