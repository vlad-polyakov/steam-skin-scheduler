package com.steam.skin.scheduler.content.entity.content;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CsSkin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @JoinColumn(name = "skin_key")
    private String skinKey;

    @ManyToOne
    @JoinColumn(name = "item_description")
    private CsItemDescription itemDescription;

    @ManyToOne
    @JoinColumn(name = "weapon")
    private CsWeapon weapon;

    @ManyToOne
    @JoinColumn(name = "rarity")
    private CsRarity rarity;

    @ManyToOne
    @JoinColumn(name = "itemSet")
    private CsItemSet itemSet;

    private float minFloat;
    private float maxFloat;
}
