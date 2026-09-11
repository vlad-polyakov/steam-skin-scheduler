package com.steam.skin.scheduler.content.service;

import com.steam.skin.scheduler.content.entity.content.CsItemDescription;
import com.steam.skin.scheduler.content.entity.content.CsRarity;
import com.steam.skin.scheduler.content.entity.vdf.VdfNode;
import com.steam.skin.scheduler.content.repository.CsItemDescriptionRepository;
import com.steam.skin.scheduler.content.repository.CsRarityRepository;
import com.steam.skin.scheduler.content.util.vdf.VdfParser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CsContentService {

    private final CsRarityRepository csRarityRepository;
    private final FileDownloadingService fileDownloadingService;
    private final CsItemDescriptionRepository csItemDescriptionRepository;
    private VdfNode itemsGameTree;
    private VdfNode csgoEnglishTree;
    private VdfNode csgoRussianTree;



    public void fillRaritiesTable() throws Exception {
        csRarityRepository.deleteAll();
        generateAllTrees();
        List<VdfNode> rarityList = itemsGameTree.first("items_game").get().first("rarities").get().children();
        for (VdfNode rarity: rarityList) {
            int id = Integer.parseInt(rarity.first("value").get().value());
            if(id == 99) {
                return;
            }
            if (csRarityRepository.findById(id).isPresent()) {
                return;
            }
            String weaponKey = rarity.first("loc_key_weapon").get().value();
            String key = rarity.key();
            String name = csgoEnglishTree.children().get(0).first("Tokens").get().first(weaponKey).get().value();
            String nameRussian = csgoRussianTree.children().get(0).first("Tokens").get().first(weaponKey).get().value();
            CsRarity csRarity = new CsRarity(id, key, weaponKey);
            CsItemDescription itemDescription = new CsItemDescription(weaponKey, name, nameRussian);
            csRarityRepository.save(csRarity);
            csItemDescriptionRepository.save(itemDescription);
        }
    }

    private void generateItemsGameTree() throws Exception {
        if (this.itemsGameTree == null) {
            byte[] data = fileDownloadingService.downloadFile("items_game");
            this.itemsGameTree = VdfParser.parse(data);
        }
    }

    private void generateCsgoEnglishTree() throws Exception {
        if (this.csgoEnglishTree == null) {
            byte[] data = fileDownloadingService.downloadFile("csgo_english");
            this.csgoEnglishTree = VdfParser.parse(data);
        }
    }

    private void generateCsgoRussianTree() throws Exception {
        if(this.csgoRussianTree == null) {
            byte[] data = fileDownloadingService.downloadFile("csgo_russian");
            this.csgoRussianTree = VdfParser.parse(data);
        }
    }

    private void generateAllTrees() throws Exception {
        generateCsgoRussianTree();
        generateItemsGameTree();
        generateCsgoEnglishTree();
    }

}
