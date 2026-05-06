package ch.uzh.ifi.hase.soprafs26.repository;
 
import ch.uzh.ifi.hase.soprafs26.entity.Story;
import ch.uzh.ifi.hase.soprafs26.entity.StoryRating;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
 
@Repository("storyRatingRepository")
public interface StoryRatingRepository extends JpaRepository<StoryRating, Long> {

    StoryRating findByStoryAndVoter(Story story, User voter);
    
    long countByStoryAndVotedFor(Story story, User votedFor);
}

