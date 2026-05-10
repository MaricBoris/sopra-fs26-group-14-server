package ch.uzh.ifi.hase.soprafs26;

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
        seedAchievement("MASTER_OF_MACABRE", "Master of Macabre", "Win 10 games as a Horror writer.", "horror-icon");
        seedAchievement("PUBLISHED_AUTHOR", "Published Author", "Win 20 games total.", "book-icon");
        seedAchievement("SUDDEN_DEATH_SURVIVOR", "Sudden Death Survivor", "Win a game in Sudden Death phase.", "skull-icon");
    }

    private void seedAchievement(String name, String displayName, String desc, String icon) {
        if (achievementRepository.findByName(name) == null) {
            Achievement a = new Achievement();
            a.setName(name);
            a.setDisplayName(displayName);
            a.setDescription(desc);
            a.setIcon(icon);
            achievementRepository.save(a);
        }
    }
}