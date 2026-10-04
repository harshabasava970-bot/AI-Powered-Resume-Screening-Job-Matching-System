package com.resumescreening.repository;

import com.resumescreening.entity.Skill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SkillRepository extends JpaRepository<Skill, Long> {

    Optional<Skill> findByNormalizedName(String normalizedName);

    Optional<Skill> findByNameIgnoreCase(String name);

    boolean existsByNormalizedName(String normalizedName);

    List<Skill> findByCategory(String category);

    @Query("SELECT s FROM Skill s WHERE s.normalizedName IN :normalizedNames")
    List<Skill> findByNormalizedNameIn(List<String> normalizedNames);
}
