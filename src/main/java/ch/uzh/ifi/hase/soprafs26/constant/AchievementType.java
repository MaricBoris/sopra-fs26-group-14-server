package ch.uzh.ifi.hase.soprafs26.constant;

public enum AchievementType {
    ROOKIE_SCRIBE(
            "Rookie Scribe",
            "Complete your very first game.",
            "feather-icon"
    ),
    PUBLISHED_AUTHOR(
            "Published Author",
            "Win 20 games total.",
            "book-icon"
    ),
    MASTER_OF_MACABRE(
            "Master of Macabre",
            "Win 10 games as a Horror writer.",
            "horror-icon"
    ),
    CROWD_FAVORITE(
            "Crowd Favorite",
            "Win a round where every single judge voted for you.",
            "star-icon"
    ),
    SUDDEN_DEATH_SURVIVOR(
            "Sudden Death Survivor",
            "Win a game in the Sudden Death phase.",
            "skull-icon"
    ),
    HORROR_LEGEND(
            "Horror Legend",
            "Reach the top 1% of Horror writers globally.",
            "crown-icon"
    );

    private final String displayName;
    private final String description;
    private final String icon;

    AchievementType(String displayName, String description, String icon) {
        this.displayName = displayName;
        this.description = description;
        this.icon = icon;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public String getIcon() { return icon; }
}