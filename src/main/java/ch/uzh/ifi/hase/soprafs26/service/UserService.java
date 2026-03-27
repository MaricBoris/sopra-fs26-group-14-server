package ch.uzh.ifi.hase.soprafs26.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;

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

	public UserService(@Qualifier("userRepository") UserRepository userRepository) {
		this.userRepository = userRepository;
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

    private String extractToken(String bearerToken) {
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

		String baseErrorMessage = "Error: You are not Autorized. Go to login and clear local Storage";
		if (userByToken == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, String.format(baseErrorMessage));
		}

		return userByToken;
	}

	public void checkUsersMatch(User user1, User user2) {
		String baseErrorMessage = "Error: You are not Autorized. Go to login and clear local Storage";
		if(!((user1.getId() == user2.getId()) && (user1.getToken() == user2.getToken()))) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, String.format(baseErrorMessage));
		}
	}





}