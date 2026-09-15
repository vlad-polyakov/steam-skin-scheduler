package com.steam.skin.scheduler.content.service;

import com.steam.skin.scheduler.content.entity.TableUpdateStatus;
import com.steam.skin.scheduler.content.entity.content.*;
import com.steam.skin.scheduler.content.entity.vdf.VdfNode;
import com.steam.skin.scheduler.content.repository.*;
import com.steam.skin.scheduler.content.util.vdf.VdfParser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class CsContentService {

    private final CsRarityRepository csRarityRepository;
    private final FileDownloadingService fileDownloadingService;
    private final CsItemSetRepository csItemSetRepository;
    private final CsWeaponRepository csWeaponRepository;
    private final CsSkinRepository csSkinRepository;
    private final CsItemDescriptionRepository csItemDescriptionRepository;

    private VdfNode itemsGameTree;
    private VdfNode csgoEnglishTree;
    private VdfNode csgoRussianTree;



    public TableUpdateStatus fillRaritiesTable() throws Exception {
        generateAllTrees();
        TableUpdateStatus status = TableUpdateStatus.NO_CONTENT_UPDATE;
        List<VdfNode> rarityList = itemsGameTree.first("items_game").get().first("rarities").get().children();
        List<String> existingKeysList = csRarityRepository.findAllItemKeys();
        for (VdfNode rarity: rarityList) {
            int id = Integer.parseInt(rarity.first("value").get().value());
            if(id == 99) {
                return status;
            }
            String weaponKey = rarity.first("loc_key_weapon").get().value();
            String key = rarity.key();
            if (existingKeysList != null && existingKeysList.contains(key)) {
                continue;
            }
            String name = csgoEnglishTree.children().get(0).first("Tokens").get().first(weaponKey).get().value();
            String nameRussian = csgoRussianTree.children().get(0).first("Tokens").get().first(weaponKey).get().value();
            CsItemDescription itemDescription = new CsItemDescription(weaponKey, name, nameRussian);
            CsRarity csRarity = new CsRarity(id, key, itemDescription);
            checkAndSaveItemDescription(itemDescription);
            csRarityRepository.save(csRarity);
            status = TableUpdateStatus.UPDATED_SUCCESSFULLY;
        }
        return status;
    }

    public TableUpdateStatus fillWeaponTable() throws Exception {
        generateAllTrees();
        TableUpdateStatus status = TableUpdateStatus.NO_CONTENT_UPDATE;
        List<VdfNode> weaponList = itemsGameTree.first("items_game").get().first("prefabs").get().children();
        weaponList = weaponList.stream().filter(weaponNode ->
                weaponNode.key().startsWith("weapon_") &&
                !weaponNode.key().equals("weapon_base") &&
                weaponNode.key().contains("prefab")).toList();
        List<String> existingKeys = csWeaponRepository.findAllItemKeys();
        for (VdfNode weapon: weaponList) {
            if (weapon.first("item_class").isEmpty()) {
                continue;
            }
            String itemKey = weapon.key();
            String descrKey = weapon.first("item_name").get().value().substring(1);
            if(existingKeys != null && existingKeys.contains(itemKey)) {
                continue;
            }
            String name = csgoEnglishTree.children().get(0).first("Tokens").get().first(descrKey).get().value();
            String nameRussian = csgoRussianTree.children().get(0).first("Tokens").get().first(descrKey).get().value();
            CsItemDescription itemDescription = new CsItemDescription(descrKey, name, nameRussian);
            CsWeapon csWeapon = new CsWeapon(null, itemKey, itemDescription);
            checkAndSaveItemDescription(itemDescription);
            csWeaponRepository.save(csWeapon);
            status = TableUpdateStatus.UPDATED_SUCCESSFULLY;
        }
        return status;
    }

    public TableUpdateStatus fillItemSetTable() throws Exception {
        generateAllTrees();
        TableUpdateStatus status = TableUpdateStatus.NO_CONTENT_UPDATE;
        List<VdfNode> itemSetList = itemsGameTree.first("items_game").get().first("item_sets").get().children();
        List<String> existingKeysList = csItemSetRepository.findAllItemKeys();
        for(VdfNode itemSet: itemSetList) {
            String itemKey = itemSet.first("name").get().value().substring(1);
            if(existingKeysList != null && existingKeysList.contains(itemSet.key())) {
                continue;
            }
            String name = csgoEnglishTree.children().get(0).first("Tokens").get().first(itemKey).get().value();
            String nameRussian = csgoRussianTree.children().get(0).first("Tokens").get().first(itemKey).get().value();
            CsItemDescription itemDescription = new CsItemDescription(itemKey, name, nameRussian);
            CsItemSet csItemSet = new CsItemSet(null, itemSet.key(), itemDescription);
            checkAndSaveItemDescription(itemDescription);
            csItemSetRepository.save(csItemSet);
            List<VdfNode> skinsList = itemSet.first("items").get().children();
            fillSkinsTable(skinsList, csItemSet);
            status = TableUpdateStatus.UPDATED_SUCCESSFULLY;
        }
        return status;
    }

    private void fillSkinsTable(List<VdfNode> skinsList, CsItemSet itemSet) {
        List<VdfNode> allPaintRarityList = itemsGameTree.first("items_game").get().
                children().stream().filter(item -> item.key().equals("paint_kits_rarity"))
                .toList();
        List<VdfNode> paintRarityCommonList = new ArrayList<>();
        allPaintRarityList.forEach(paintRarity -> paintRarityCommonList.addAll(paintRarity.children()));
        List<VdfNode> allSkinsList = itemsGameTree.first("items_game").get().
                children().stream().filter(item -> item.key().equals("paint_kits")).toList();
        List<VdfNode> commonSkinsList = new ArrayList<>();
        allSkinsList.forEach(skinItem -> commonSkinsList.addAll(skinItem.children()));
        for(VdfNode skin: skinsList) {
            String key = skin.key();
            String skinKey;
            String weaponKey = null;
            Pattern skinPattern = Pattern.compile("\\[(.*?)\\]");
            Matcher matcher = skinPattern.matcher(key);
            if (matcher.find()) {
                skinKey = matcher.group(1);
            } else {
                skinKey = null;
            }
            Pattern weaponPattern = Pattern.compile("\\](.*)$");
            Matcher weaponMatcher = weaponPattern.matcher(key);
            if (weaponMatcher.find()) {
                weaponKey = weaponMatcher.group(1) + "_prefab";
            }

            CsWeapon csWeapon = csWeaponRepository.findByItemKey(weaponKey).get();

            VdfNode foundRarity =
                    paintRarityCommonList.stream().
                    filter(paintKitRarity -> paintKitRarity.key().
                            equals(skinKey)).findFirst().get();
            CsRarity csRarity = csRarityRepository.findByRarityKey(foundRarity.value()).get();

            VdfNode foundSkin = commonSkinsList.stream().
                    filter(paintKit -> paintKit.first("name").get().value().
                            equals(skinKey)).findFirst().get();
            String descrKey = foundSkin.first("description_tag").get().value().substring(1);
            String name = csgoEnglishTree.children().get(0).first("Tokens").get().first(descrKey).get().value();
            String nameRussian = csgoRussianTree.children().get(0).first("Tokens").get().first(descrKey).get().value();
            CsItemDescription itemDescription = new CsItemDescription(descrKey, name, nameRussian);
            float minFloat = 0;
            float maxFloat = 1;
            try {
                minFloat = Float.parseFloat(foundSkin.first("wear_remap_min").get().value());
                maxFloat = Float.parseFloat(foundSkin.first("wear_remap_max").get().value());

            }
            catch (NoSuchElementException exception) {
                //do nothing
            }
            checkAndSaveItemDescription(itemDescription);
            CsSkin csSkin = new CsSkin(null, skinKey, itemDescription, csWeapon, csRarity, itemSet, minFloat, maxFloat);
            csSkinRepository.save(csSkin);
        }
    }

    private void checkAndSaveItemDescription(CsItemDescription itemDescription) {
        if (csItemDescriptionRepository.findByItemKey(itemDescription.getItemKey()).isEmpty()) {
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
