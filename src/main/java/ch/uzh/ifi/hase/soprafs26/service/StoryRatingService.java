package ch.uzh.ifi.hase.soprafs26.service;
 
import ch.uzh.ifi.hase.soprafs26.entity.Story;
import ch.uzh.ifi.hase.soprafs26.entity.StoryRating;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.StoryRatingRepository;
import ch.uzh.ifi.hase.soprafs26.repository.StoryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
 
import java.util.Date;
 

@Service
@Transactional
public class StoryRatingService {
 
    private final StoryRatingRepository storyRatingRepository;
    private final StoryRepository storyRepository;
    private final UserService userService;
 
    public StoryRatingService(StoryRatingRepository storyRatingRepository, StoryRepository storyRepository, UserService userService) {
        this.storyRatingRepository = storyRatingRepository;
        this.storyRepository = storyRepository;
        this.userService = userService;
    }
 
    public StoryRating rateGenre(Long storyId, Long votedForUserId, String bearerToken) {

        //throws 401
        String token= userService.extractToken(bearerToken);
        User voter= userService.findUserFromToken(token);
 
        Story story= storyRepository.findById(storyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Story not found"));
 
        if (votedForUserId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "There must be a voted for user");
        }
 
        //Checks if the voted for user was winner or loser in the story (actually if he actually participated in story as writer )
        User votedFor = pickPlayer(story, votedForUserId);
        if (votedFor == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "The voted for user must be one of the two writers of the story");
        }
 
        //check if the voting user already participated in the story->illegal
        if (hasParticipated(story, voter)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You cannot vote on a story you participated in");
        }
 
        // checks if the voter has already voted before, if yes, we just update his rating
        StoryRating existing = storyRatingRepository.findByStoryAndVoter(story, voter);
        StoryRating saved;
        if (existing == null) {
            saved = storyRatingRepository.save(new StoryRating(story, voter, votedFor));
        } else {
            existing.setVotedFor(votedFor);
            existing.setTimestamp(new Date());
            saved = storyRatingRepository.save(existing);
        }
        storyRatingRepository.flush();
        return saved;
    }
 
    //helper methods
 
    public Story getStory(Long storyId, String bearerToken) {
        String token = userService.extractToken(bearerToken);
        userService.findUserFromToken(token);
        Story story= storyRepository.findById(storyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Story not found"));
        return story;
    }
 
    public User getCurrentUser(String bearerToken) {
        String token = userService.extractToken(bearerToken);
        return userService.findUserFromToken(token);
    }
 
    public long countVotesFor(Story story, User player) {
        if (player == null) return 0;
        return storyRatingRepository.countByStoryAndVotedFor(story, player);
    }
 
    public StoryRating findOwnRating(Story story, User voter) {
        return storyRatingRepository.findByStoryAndVoter(story, voter);
    }
 
    public boolean hasParticipated(Story story, User user) {
        if (user == null || user.getId() == null) return false;
        Long uid = user.getId();
        if (story.getWinner() != null && uid.equals(story.getWinner().getId())) return true;
        if (story.getLoser() != null && uid.equals(story.getLoser().getId())) return true;
        if (story.getJudges() != null) {
            for (User judge : story.getJudges()) {
                if (judge != null && uid.equals(judge.getId())) return true;
            }
        }
        return false;
    }
 
    private User pickPlayer(Story story, Long userId) {
        if (story.getWinner() != null && userId.equals(story.getWinner().getId())) {
            return story.getWinner();
        }
        if (story.getLoser() != null && userId.equals(story.getLoser().getId())) {
            return story.getLoser();
        }
        return null;
    }

    public Story changeTitle(Long storyId, String bearerToken, String newTitle){
        Story story = getStory(storyId, bearerToken);
        story.setTitle(newTitle);
        storyRepository.save(story);
        storyRepository.flush();
        return story;
    }
}
