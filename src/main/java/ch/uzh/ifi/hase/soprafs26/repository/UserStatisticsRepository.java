package ch.uzh.ifi.hase.soprafs26.repository;

import ch.uzh.ifi.hase.soprafs26.entity.UserStatistics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository("userStatisticsRepository")
public interface UserStatisticsRepository extends JpaRepository<UserStatistics, Long> {
    // Standard CRUD methods are inherited automatically.
    // In the future, we can add custom queries here for leaderboards!
}