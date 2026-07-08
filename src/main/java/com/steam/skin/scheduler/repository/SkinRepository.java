package com.steam.skin.scheduler.repository;

import com.steam.skin.scheduler.entity.Skin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SkinRepository extends JpaRepository<Skin, Long> {

}
