package com.steam.skin.scheduler.content.repository;

import com.steam.skin.scheduler.content.entity.content.CsWeapon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CsWeaponRepository extends JpaRepository<CsWeapon, String> {
}
