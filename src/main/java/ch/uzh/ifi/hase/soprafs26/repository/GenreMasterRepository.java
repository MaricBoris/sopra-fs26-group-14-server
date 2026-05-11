package ch.uzh.ifi.hase.soprafs26.repository;

import ch.uzh.ifi.hase.soprafs26.entity.GenreMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository("genreMasterRepository")
public interface GenreMasterRepository extends JpaRepository<GenreMaster, Long> {
    GenreMaster findByGenre(String genre);
}