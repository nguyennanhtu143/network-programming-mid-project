package dev.p3ntest.zombies.repository;

import dev.p3ntest.zombies.entity.CustomAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomAssetRepository extends JpaRepository<CustomAsset, String> {
    
    @Query("SELECT a FROM CustomAsset a WHERE " +
           "LOWER(a.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(a.description) LIKE LOWER(CONCAT('%', :search, '%'))")
    List<CustomAsset> searchByNameOrDescription(@Param("search") String search);
}


