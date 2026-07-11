package com.steam.skin.scheduler.getupdates.repository;

import com.steam.skin.scheduler.userauth.entity.Skin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SkinRepository extends JpaRepository<Skin, Long> {

}
