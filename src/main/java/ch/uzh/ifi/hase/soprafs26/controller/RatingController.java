package ch.uzh.ifi.hase.soprafs26.controller;
 
import ch.uzh.ifi.hase.soprafs26.entity.Story;
import ch.uzh.ifi.hase.soprafs26.entity.StoryRating;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.user.StoryGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.storyRating.GenreRatingGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.storyRating.GenreRatingPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs26.service.StoryRatingService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
 
@RestController
public class RatingController {
 
    private final StoryRatingService storyRatingService;
 
    public RatingController(StoryRatingService storyRatingService) {
        this.storyRatingService = storyRatingService;
    }
 
    @GetMapping("/stories/{storyId}/genre-rating")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public GenreRatingGetDTO getGenreRating(@PathVariable Long storyId,
                                            @RequestHeader("Authorization") String bearerToken) {
        Story story = storyRatingService.getStory(storyId, bearerToken);
        User voter = storyRatingService.getCurrentUser(bearerToken);
        return buildRatingDto(story, voter);
    }
 
    @PostMapping("/stories/{storyId}/genre-rating")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public GenreRatingGetDTO rateGenre(@PathVariable Long storyId,
                                       @RequestBody GenreRatingPostDTO dto,
                                       @RequestHeader("Authorization") String bearerToken) {
        storyRatingService.rateGenre(storyId, dto.getVotedForUserId(), bearerToken);
        Story story = storyRatingService.getStory(storyId, bearerToken);
        User voter = storyRatingService.getCurrentUser(bearerToken);
        return buildRatingDto(story, voter);
    }
 
    //piecing together the dto
    private GenreRatingGetDTO buildRatingDto(Story story, User voter) {
        GenreRatingGetDTO out = new GenreRatingGetDTO();
 
        User winner = story.getWinner();
        if (winner != null) {
            out.setWinnerUserId(winner.getId());
            out.setWinnerUsername(winner.getUsername());
            out.setWinnerVotes(storyRatingService.countVotesFor(story, winner));
        }
        out.setWinnerGenre(story.getWinGenre());
 
        User loser = story.getLoser();
        if (loser != null) {
            out.setLoserUserId(loser.getId());
            out.setLoserUsername(loser.getUsername());
            out.setLoserVotes(storyRatingService.countVotesFor(story, loser));
        }
        out.setLoserGenre(story.getLoseGenre());
 
        StoryRating own = storyRatingService.findOwnRating(story, voter);
        if (own != null && own.getVotedFor() != null) {
            out.setUserVotedForId(own.getVotedFor().getId());
        }
 
        out.setCanVote(!storyRatingService.hasParticipated(story, voter));
        return out;
    }

    @PutMapping("/story/{storyId}/title")
    @ResponseStatus(HttpStatus.OK)
    public StoryGetDTO changeTitle(
            @PathVariable Long storyId,
            @RequestHeader(value = "Authorization", required = false) String bearerToken, @RequestBody String newTitle) {

        Story story = storyRatingService.changeTitle(storyId, bearerToken, newTitle);
        return DTOMapper.INSTANCE.convertEntityToStoryGetDTO(story);
    }
}
