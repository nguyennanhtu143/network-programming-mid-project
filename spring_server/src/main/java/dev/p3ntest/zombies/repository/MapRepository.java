package dev.p3ntest.zombies.repository;

import dev.p3ntest.zombies.entity.Map;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MapRepository extends JpaRepository<Map, String> {
    
    List<Map> findByVerifiedTrue();
    
    List<Map> findByAuthorId(String authorId);
    
    List<Map> findByAuthorIdAndVerifiedFalse(String authorId);
    
    @Query("SELECT m FROM Map m WHERE m.published = true AND m.author.id <> ?1 ORDER BY m.createdAt DESC")
    List<Map> findPublishedCommunityMaps(String excludeAuthorId);

    List<Map> findByPublishedTrueOrderByCreatedAtDesc();
    
    long countByVerifiedTrue();
}


