package com.steam.skin.scheduler.content.entity.content;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CsItemSet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String itemSetKey;

    @OneToOne
    @JoinColumn(name = "item_description")
    private CsItemDescription itemDescription;
}
