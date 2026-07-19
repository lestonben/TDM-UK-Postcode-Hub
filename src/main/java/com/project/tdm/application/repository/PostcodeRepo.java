package com.project.tdm.application.repository;

import com.project.tdm.application.entity.PostcodeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

@Repository
public interface PostcodeRepo extends JpaRepository<PostcodeEntity, Long> {
    @Query("SELECT p FROM PostcodeEntity p WHERE p.postcode = :postcode")
    Optional<PostcodeEntity> findPostcode(@Param("postcode") String postcode);

    @Query("SELECT p FROM PostcodeEntity p WHERE p.postcode = :from OR p.postcode = :to")
    List<PostcodeEntity> findPostcodes(@Param("from") String postCodeFrom, @Param("to") String postCodeTo);

    @Query("SELECT p.postcode FROM PostcodeEntity p WHERE UPPER(p.postcode) LIKE UPPER(CONCAT(:input, '%')) ORDER BY p.postcode ASC")
    List<String> findPostcodeSuggestions(@Param("input") String input, Pageable pageable);
}
