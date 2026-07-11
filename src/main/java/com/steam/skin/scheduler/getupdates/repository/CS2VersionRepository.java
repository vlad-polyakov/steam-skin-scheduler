package com.steam.skin.scheduler.getupdates.repository;

import com.steam.skin.scheduler.getupdates.entity.pics.CS2VersionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CS2VersionRepository extends JpaRepository<CS2VersionEntity, Long>  {
    Optional<CS2VersionEntity> findFirstByOrderByIdDesc();
}
