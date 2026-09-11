package com.steam.skin.scheduler.content.entity.content;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CsWeapon {
    @Id
    private String itemKey;
    private String descrKey;
}
