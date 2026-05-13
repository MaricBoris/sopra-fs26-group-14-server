package ch.uzh.ifi.hase.soprafs26.repository;

import ch.uzh.ifi.hase.soprafs26.entity.Story;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface StoryRepository extends JpaRepository<Story, Long> {
    
    List<Story> findByWinner(User winner);
    List<Story> findByLoser(User loser);
    List<Story> findByJudgesContaining(User user);

    @Query("SELECT s FROM Story s WHERE s.winner = :user OR s.loser = :user OR :user MEMBER OF s.judges")
    List<Story> findHistoryForUser(@Param("user") User user);
}

