package ch.uzh.ifi.hase.soprafs26.repository;

import ch.uzh.ifi.hase.soprafs26.entity.Room;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
public class RoomRepositoryIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private RoomRepository roomRepository;

    @Test
    public void findByName_success() {
        // given
        // We need a leader for a realistic room setup
        User leader = new User();
        leader.setUsername("leaderUser");
        leader.setPassword("password123");
        leader.setToken("token-leader");
        entityManager.persist(leader);

        Room room = new Room();
        room.setName("Epic Gaming Room");
        room.setPlayerCount(1);
        room.setLobbyLeader(leader);

        entityManager.persist(room);
        entityManager.flush();

        // when
        Room found = roomRepository.findByName(room.getName());

        // then
        assertNotNull(found.getId());
        assertEquals(found.getName(), room.getName());
        assertEquals(found.getPlayerCount(), room.getPlayerCount());
        assertEquals(found.getLobbyLeader().getId(), leader.getId());
    }

    @Test
    public void findByName_notFound_returnsNull() {
        // when
        Room found = roomRepository.findByName("NonExistentRoom");

        // then
        assertNull(found);
    }

    @Test
    public void findById_success() {
        // given
        Room room = new Room();
        room.setName("Solo Room");
        room.setPlayerCount(1);

        entityManager.persist(room);
        entityManager.flush();

        // when
        Room found = roomRepository.findById(room.getId()).orElse(null);

        // then
        assertNotNull(found);
        assertEquals(found.getId(), room.getId());
        assertEquals(found.getName(), room.getName());
    }

    @Test
    public void deleteRoom_success() {
        // given
        Room room = new Room();
        room.setName("Temporary Room");
        room.setPlayerCount(1);
        entityManager.persist(room);
        entityManager.flush();

        // when
        roomRepository.delete(room);
        entityManager.flush();
        Room found = roomRepository.findByName("Temporary Room");

        // then
        assertNull(found);
    }
}