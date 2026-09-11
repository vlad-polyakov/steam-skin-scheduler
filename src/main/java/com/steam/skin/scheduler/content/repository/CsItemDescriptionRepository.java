package com.steam.skin.scheduler.content.repository;

import com.steam.skin.scheduler.content.entity.content.CsItemDescription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CsItemDescriptionRepository extends JpaRepository<CsItemDescription, String> {
}
