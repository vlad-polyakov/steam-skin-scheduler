package com.steam.skin.scheduler.content.repository;

import com.steam.skin.scheduler.content.entity.content.CsItemSet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CsItemSetRepository extends JpaRepository<CsItemSet, Integer> {
    @Query("SELECT w.itemSetKey FROM CsItemSet w")
    List<String> findAllItemKeys();
}
