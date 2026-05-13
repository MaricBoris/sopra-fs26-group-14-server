package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.entity.UserStatistics;
import ch.uzh.ifi.hase.soprafs26.rest.dto.user.UserDeleteDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.user.UserPasswordPutDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.user.UserPutDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.user.StoryGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.entity.Story;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs26.repository.StoryRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * User Service
 * This class is the "worker" and responsible for all functionality related to
 * the user
 * (e.g., it creates, modifies, deletes, finds). The result will be passed back
 * to the caller.
 */
@Service
@Transactional
public class UserService {

	private final Logger log = LoggerFactory.getLogger(UserService.class);

	private final UserRepository userRepository;
    private final StoryRepository storyRepository;

	public UserService(@Qualifier("userRepository") UserRepository userRepository, StoryRepository storyRepository) {
		this.userRepository = userRepository;
        this.storyRepository = storyRepository;


	}

	public List<User> getUsers(String bearerToken) {
        
        if (bearerToken == null || bearerToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated"); //Check 401
        }

        if (!bearerToken.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Authentication header"); //Check 401
        }

        String token = bearerToken.substring(7);

        if (token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token"); //Check 401
        }

        User user = userRepository.findByToken(token);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token"); //Check 401
        }
		return this.userRepository.findAll();
	}

	public User createUser(User newUser) {
        // Check 400
        if (newUser.getUsername() == null || newUser.getUsername().isBlank() ||
                newUser.getPassword() == null || newUser.getPassword().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username and password cannot be empty!");
        }
        // Check 409
		checkIfUserExists(newUser);

        UserStatistics stats = new UserStatistics();
        stats.setUser(newUser);
        newUser.setStatistics(stats);

        // 201
        newUser.setToken(UUID.randomUUID().toString());
        // saves the given entity but data is only persisted in the database once
		// flush() is called
		newUser = userRepository.save(newUser);
		userRepository.flush();

		log.debug("Created Information for User: {}", newUser);
		return newUser;
	}

    public User loginUser(User userToLogin) {
        // Check 400
        if (userToLogin.getUsername() == null || userToLogin.getUsername().isBlank() ||
                userToLogin.getPassword() == null || userToLogin.getPassword().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username and password cannot be empty!");
        }

        // Check 401
        User userByUsername = userRepository.findByUsername(userToLogin.getUsername());
        if (userByUsername == null || !userByUsername.getPassword().equals(userToLogin.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password.");
        }

        // 200
        userByUsername.setToken(UUID.randomUUID().toString());
        userByUsername = userRepository.save(userByUsername);
        userRepository.flush();
        return userByUsername;
    }

    public void logoutUser(String bearerToken) {
        String token;
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            token = bearerToken.substring(7);
        }
        else{
            return;
        }

        User userToLogout = userRepository.findByToken(token);

        if (userToLogout == null) {
            return;
        }

        userToLogout.setToken(UUID.randomUUID().toString());

        userRepository.save(userToLogout);
        userRepository.flush();
    }

    public User updateUserBio(Long userId, UserPutDTO userPutDTO, String bearerToken) {

        String token = extractToken(bearerToken);
        User tokenUser = findUserFromToken(token);
        User user = findUserFromId(userId);

        checkUsersMatch(user, tokenUser);
        user.setBio(userPutDTO.getBio());

        userRepository.save(user);
        userRepository.flush();

        return user;
    }

    public void changePassword(Long userId, UserPasswordPutDTO passwordDTO, String bearerToken) {

        User user = findUserFromId(userId);
        checkUsersMatch(user, findUserFromToken(extractToken(bearerToken)));

        if (passwordDTO.getCurrentPassword() == null || passwordDTO.getCurrentPassword().isBlank() ||
                passwordDTO.getNewPassword() == null || passwordDTO.getNewPassword().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current and new password are required");
        }

        if (!user.getPassword().equals(passwordDTO.getCurrentPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Incorrect password!");
        }

        user.setPassword(passwordDTO.getNewPassword());
        userRepository.save(user);
        userRepository.flush();
    }

    public void deleteUser(Long userId, UserDeleteDTO deleteDTO, String bearerToken) {

        String token = extractToken(bearerToken);
        User tokenUser = findUserFromToken(token);
        User user = findUserFromId(userId);
        checkUsersMatch(user, tokenUser);

        if (deleteDTO == null || deleteDTO.getPassword() == null || deleteDTO.getPassword().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Error: Password is required to delete account");
        }

        if (!user.getPassword().equals(deleteDTO.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Incorrect password");
        }

        List<Story> asWinner = storyRepository.findByWinner(user);
        asWinner.forEach(s -> s.setWinner(null));
        storyRepository.saveAll(asWinner);
        storyRepository.flush(); 

        List<Story> asLoser = storyRepository.findByLoser(user);
        asLoser.forEach(s -> s.setLoser(null));
        storyRepository.saveAll(asLoser);
        storyRepository.flush(); 

        List<Story> asJudge = storyRepository.findByJudgesContaining(user);
        asJudge.forEach(s -> s.getJudges().remove(user));
        storyRepository.saveAll(asJudge);
        storyRepository.flush(); 

        userRepository.delete(user);
        userRepository.flush();
    }


        /**
         * This is a helper method that will check the uniqueness criteria of the
         * username and the name
         * defined in the User entity. The method will do nothing if the input is unique
         * and throw an error otherwise.
         *
         * @param userToBeCreated
         * @throws org.springframework.web.server.ResponseStatusException
         * @see User
         */
    private void checkIfUserExists(User userToBeCreated) {
        User userByUsername = userRepository.findByUsername(userToBeCreated.getUsername());

        if (userByUsername != null) {
            String errorMessage = "The username provided is not unique. Therefore, the user could not be created!";
            throw new ResponseStatusException(HttpStatus.CONFLICT, errorMessage);
        }
    }

    public String extractToken(String bearerToken) {
        String token;
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            token = bearerToken.substring(7);
            return token;
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or missing session token. Go to login and clear local Storage!");
    }
	

	public User findUserFromId(Long id) {

		String baseErrorMessage = "Error: The provided id: %s is invalid and doesn't match any user.";
		User userById = userRepository.findById(id).orElseThrow(() ->     //Needs this notation cause .findByID(id) returns a <Optional> User. All other find by are self implemented and will return a User or Null
		new ResponseStatusException(HttpStatus.NOT_FOUND, String.format(baseErrorMessage, id))); //NOT_FOUND 404

		return userById; 
    }

	public User findUserFromToken(String token) {
       
		User userByToken = userRepository.findByToken(token);

		String baseErrorMessage = "Error: You are not Authorized. Go to login and clear local Storage";
		if (userByToken == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, String.format(baseErrorMessage));
		}

		return userByToken;
	}

	public void checkUsersMatch(User user1, User user2) {
		String baseErrorMessage = "Error: You are not Authorized. Go to login and clear local Storage";
        if(!(user1.getId().equals(user2.getId()) && user1.getToken().equals(user2.getToken()))) {  //fixed to handle longer tokens
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, String.format(baseErrorMessage));
		}
	}

    public List<StoryGetDTO> findAllStories(){
        List<Story> results = storyRepository.findAll();
        List<StoryGetDTO> getResults = new ArrayList<>();
        for (Story story:results) {
            StoryGetDTO getStory = DTOMapper.INSTANCE.convertEntityToStoryGetDTO(story);
            getResults.add(getStory);

        }
        return getResults;
    }

    public StoryGetDTO findStoryById(Long storyId) {
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Story not found"));
        return DTOMapper.INSTANCE.convertEntityToStoryGetDTO(story);
    }

    public List<StoryGetDTO> findAllStoriesOfUser(long id){
        List<Story> allStories = storyRepository.findAll();

        List<StoryGetDTO> allStoriesFromUser = new ArrayList<>();
        for (Story story:allStories) {
            if (story.getWinner().getId().equals(id) || story.getLoser().getId().equals(id) || isAJudge(story, id)){
                StoryGetDTO getStory = DTOMapper.INSTANCE.convertEntityToStoryGetDTO(story);
                allStoriesFromUser.add(getStory);
            }
        }
        return allStoriesFromUser;
    }

    public Boolean isAJudge(Story story, long id){
        for (User judge:story.getJudges()){
            if (judge.getId().equals(id)){
                return true;
            } 
        }
        return false;
    }

    public UserStatistics getUserStatistics(Long userId, String bearerToken) {
        // 1. Ensure the requester is authenticated
        findUserFromToken(extractToken(bearerToken));

        // 2. Find the user whose stats we want
        User user = findUserFromId(userId);

        // 3. Return the stats object (should not be null since we initialize it in createUser)
        UserStatistics stats = user.getStatistics();
        if (stats == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Statistics for this user were not found.");
        }

        return stats;
    }
}