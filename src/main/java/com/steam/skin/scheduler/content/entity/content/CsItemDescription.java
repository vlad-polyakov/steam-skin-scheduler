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
public class CsItemDescription {
    @Id
    private String itemKey;
    private String name;
    private String nameRussian;
}
