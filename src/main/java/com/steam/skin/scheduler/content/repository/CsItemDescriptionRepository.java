package com.steam.skin.scheduler.content.repository;

import com.steam.skin.scheduler.content.entity.content.CsItemDescription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CsItemDescriptionRepository extends JpaRepository<CsItemDescription, Integer> {
    Optional<CsItemDescription> findByItemKey(String itemKey);
}
