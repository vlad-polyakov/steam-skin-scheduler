package com.steam.skin.scheduler.content.repository;

import com.steam.skin.scheduler.content.entity.content.CsWeapon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CsWeaponRepository extends JpaRepository<CsWeapon, Integer> {
    Optional<CsWeapon> findByItemKey(String itemKey);

    @Query("select w.itemKey from CsWeapon w")
    List<String> findAllItemKeys();
}
