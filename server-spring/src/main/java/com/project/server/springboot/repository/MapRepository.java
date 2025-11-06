package com.project.server.springboot.repository;

import com.project.server.springboot.model.Map;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Map repository for database queries
 */
@Repository
public interface MapRepository extends JpaRepository<Map, String> {
    
    /**
     * Find all verified maps
     */
    List<Map> findByVerifiedTrue();
    
    /**
     * Count verified maps
     */
    long countByVerifiedTrue();
    
    /**
     * Find a random verified map
     * Note: This uses random() which may not be optimal for large datasets
     * For production, consider using a more efficient approach
     */
    @Query(value = "SELECT * FROM maps WHERE verified = true ORDER BY RANDOM() LIMIT 1", nativeQuery = true)
    Optional<Map> findRandomVerifiedMap();
}

