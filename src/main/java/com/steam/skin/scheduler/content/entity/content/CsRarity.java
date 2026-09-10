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
@AllArgsConstructor
@NoArgsConstructor
public class CsRarity {

    @Id
    private int id;
    private String weaponKey;
    private String name;
    private String nameRussian;
}
