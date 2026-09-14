package com.steam.skin.scheduler.content.repository;

import com.steam.skin.scheduler.content.entity.content.CsSkin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CsSkinRepository extends JpaRepository<CsSkin, Integer> {
}
