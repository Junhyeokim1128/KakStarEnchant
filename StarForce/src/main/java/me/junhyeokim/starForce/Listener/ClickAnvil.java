package me.junhyeokim.starForce.Listener;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class ClickAnvil implements Listener {

    private ItemStack originalItem; // 플레이어가 클릭한 원본 아이템
    private static final String MODIFIER_NAME = "star_force";

    private final double[] successRates = {100, 90, 80, 70, 60, 50, 50, 50, 50, 50}; // 강화 성공 확률 (100, 90, 80, 70, 60, 50, 50, 50, 50, 50)
    private final double[] destructionRates = {0, 0, 0, 0, 0, 10, 27.5, 45, 62.5, 80}; // 아이템 파괴 확률 (0, 0, 0, 0, 0, 10, 27.5, 45, 62.5, 80)

    private final Map<Player, ItemStack> playerOriginalItems = new HashMap<>();

    @EventHandler
    public void clickEnchantTable(PlayerInteractEvent event) {
        if (event.getAction().toString().contains("RIGHT_CLICK_BLOCK")) {
            if (event.getClickedBlock() != null && event.getClickedBlock().getType() == Material.ANVIL
                    ||event.getClickedBlock().getType() == Material.ANVIL
                    ||event.getClickedBlock().getType() == Material.CHIPPED_ANVIL) {
                event.setCancelled(true);

                Player player = event.getPlayer();
                Inventory enchantMenu = Bukkit.createInventory(player, 27, ChatColor.WHITE + "\uEBBB\uE004");
                player.openInventory(enchantMenu);

                // 플레이어별 원본 아이템을 초기화
                playerOriginalItems.remove(player);
            }
        }
    }



    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals(ChatColor.WHITE + "\uEBBB\uE004")) {
            event.setCancelled(true); // 인벤토리 동작 차단

            Player player = (Player) event.getWhoClicked();
            ItemStack clickedItem = event.getCurrentItem();
            Inventory clickedInventory = event.getInventory();

            // 1. 플레이어 인벤토리에서 아이템 클릭 시 13번 슬롯으로 복사
            if (event.getClickedInventory() == player.getInventory() && clickedItem != null) {
                if (playerOriginalItems.containsKey(player) && playerOriginalItems.get(player).equals(clickedItem)) {
                    player.sendMessage(ChatColor.RED + "같은 아이템입니다! 다시 시도해주세요.");
                    event.setCancelled(true);
                    return;
                }

                if (!isAllowedItemType(clickedItem.getType())) {
                    player.sendMessage(ChatColor.RED + "이 아이템은 강화할 수 없습니다!");
                    event.setCancelled(true);
                    return;
                }

                playerOriginalItems.put(player, clickedItem.clone());
                ItemStack itemToEnchant = clickedItem.clone();
                clickedInventory.setItem(13, itemToEnchant);
                syncItemWithInventory(player, clickedItem, itemToEnchant);
                updateSlot13Lore(player, clickedItem);
                return;
            }

            // 2. 13번 슬롯 클릭 시 강화 작업
            if (event.getSlot() == 13 && clickedItem != null) {
                if (!playerOriginalItems.containsKey(player)) {
                    playerOriginalItems.put(player, clickedItem.clone());
                }

                if (!playerOriginalItems.get(player).isSimilar(clickedItem)) {
                    player.sendMessage(ChatColor.RED + "13번 슬롯의 아이템이 원래 설정된 아이템과 일치하지 않습니다.");
                    event.setCancelled(true);
                    return;
                }

                if (hasValidStarPiece(player)) {
                    removeStarPiece(player);

                    ItemMeta itemMeta = clickedItem.getItemMeta();
                    if (itemMeta != null) {
                        List<String> lore = itemMeta.hasLore() ? itemMeta.getLore() : new ArrayList<>();
                        String currentStars = lore != null && !lore.isEmpty() ? lore.get(0) : ChatColor.GOLD + "☆☆☆☆☆☆☆☆☆☆";

                        // 현재 별 개수 계산
                        int starCount = (int) currentStars.chars().filter(ch -> ch == '★').count();

                        // 강화 가능한 최대 별 개수 초과 시
                        if (starCount >= successRates.length) {
                            player.sendMessage(ChatColor.RED + "더 이상 강화를 할 수 없습니다!");
                            event.setCancelled(true);
                            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 1.0f);
                            return;
                        }

                        // 성공 및 파괴 여부 계산
                        boolean isSuccessful = Math.random() * 100 < successRates[starCount];
                        boolean isDestroyed = Math.random() * 100 < destructionRates[starCount];

                        if (isDestroyed) {
                            // 아이템 파괴 로직
                            player.getInventory().removeItem(playerOriginalItems.get(player));
                            clickedInventory.setItem(13, null);
                            playerOriginalItems.remove(player);
                            player.sendMessage(ChatColor.RED + "아이템이 파괴되었습니다");
                            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
                        } else if (isSuccessful) {
                            // 강화 성공 로직
                            starCount++;

                            if (clickedItem.getType().name().endsWith("_AXE")) {
                                itemMeta.removeAttributeModifier(Attribute.ATTACK_DAMAGE);
                                itemMeta.removeAttributeModifier(Attribute.ATTACK_SPEED);

                                NamespacedKey key = new NamespacedKey("starforce", MODIFIER_NAME + "_" + clickedItem.getType().name().toLowerCase());
                                AttributeModifier modifier_a = new AttributeModifier(key, 10 + (starCount * 2) , AttributeModifier.Operation.ADD_NUMBER);
                                AttributeModifier modifier_s = new AttributeModifier(key, -3.0, AttributeModifier.Operation.ADD_NUMBER);

                                itemMeta.addAttributeModifier(Attribute.ATTACK_DAMAGE, modifier_a);
                                itemMeta.addAttributeModifier(Attribute.ATTACK_SPEED, modifier_s);
                            }else if (clickedItem.getType().name().endsWith("_SWORD")) {
                                itemMeta.removeAttributeModifier(Attribute.ATTACK_DAMAGE);
                                itemMeta.removeAttributeModifier(Attribute.ATTACK_SPEED);

                                NamespacedKey key = new NamespacedKey("starforce", MODIFIER_NAME + "_" + clickedItem.getType().name().toLowerCase());
                                AttributeModifier modifier_a = new AttributeModifier(key, 8, AttributeModifier.Operation.ADD_NUMBER);
                                AttributeModifier modifier_s = new AttributeModifier(key, -2.4 + (starCount * 0.2) , AttributeModifier.Operation.ADD_NUMBER);

                                itemMeta.addAttributeModifier(Attribute.ATTACK_DAMAGE, modifier_a);
                                itemMeta.addAttributeModifier(Attribute.ATTACK_SPEED, modifier_s);
                            }else if (clickedItem.getType().name().endsWith("_HELMET")) {
                                itemMeta.removeAttributeModifier(Attribute.ARMOR);
                                itemMeta.removeAttributeModifier(Attribute.ARMOR_TOUGHNESS);
                                itemMeta.removeAttributeModifier(Attribute.KNOCKBACK_RESISTANCE);

                                NamespacedKey key = new NamespacedKey("starforce", MODIFIER_NAME + "_" + clickedItem.getType().name().toLowerCase());
                                AttributeModifier modifier_a = new AttributeModifier(key, 3 + starCount , AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.HEAD.getGroup());
                                AttributeModifier modifier_at = new AttributeModifier(key, 3 , AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.HEAD.getGroup());
                                AttributeModifier modifier_kr = new AttributeModifier(key, 0.1 , AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.HEAD.getGroup());

                                itemMeta.addAttributeModifier(Attribute.ARMOR, modifier_a);
                                itemMeta.addAttributeModifier(Attribute.ARMOR_TOUGHNESS, modifier_at);
                                itemMeta.addAttributeModifier(Attribute.KNOCKBACK_RESISTANCE, modifier_kr);
                            }else if (clickedItem.getType().name().endsWith("_CHESTPLATE")) {
                                itemMeta.removeAttributeModifier(Attribute.ARMOR);
                                itemMeta.removeAttributeModifier(Attribute.ARMOR_TOUGHNESS);
                                itemMeta.removeAttributeModifier(Attribute.KNOCKBACK_RESISTANCE);
                                if (starCount != 0) itemMeta.removeAttributeModifier(Attribute.MAX_HEALTH);

                                NamespacedKey key = new NamespacedKey("starforce", MODIFIER_NAME + "_" + clickedItem.getType().name().toLowerCase());
                                AttributeModifier modifier_a = new AttributeModifier(key, 8 , AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.CHEST.getGroup());
                                AttributeModifier modifier_at = new AttributeModifier(key, 3 , AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.CHEST.getGroup());
                                AttributeModifier modifier_kr = new AttributeModifier(key, 0.1 , AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.CHEST.getGroup());
                                AttributeModifier modifier_mh = new AttributeModifier(key, starCount * 2 , AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.CHEST.getGroup());

                                itemMeta.addAttributeModifier(Attribute.ARMOR, modifier_a);
                                itemMeta.addAttributeModifier(Attribute.ARMOR_TOUGHNESS, modifier_at);
                                itemMeta.addAttributeModifier(Attribute.KNOCKBACK_RESISTANCE, modifier_kr);
                                itemMeta.addAttributeModifier(Attribute.MAX_HEALTH, modifier_mh);
                            }else if (clickedItem.getType().name().endsWith("_LEGGINGS")) {
                                itemMeta.removeAttributeModifier(Attribute.ARMOR);
                                itemMeta.removeAttributeModifier(Attribute.ARMOR_TOUGHNESS);
                                itemMeta.removeAttributeModifier(Attribute.KNOCKBACK_RESISTANCE);
                                if (starCount != 0) itemMeta.removeAttributeModifier(Attribute.MAX_HEALTH);

                                NamespacedKey key = new NamespacedKey("starforce", MODIFIER_NAME + "_" + clickedItem.getType().name().toLowerCase());
                                AttributeModifier modifier_a = new AttributeModifier(key, 6 , AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.LEGS.getGroup());
                                AttributeModifier modifier_at = new AttributeModifier(key, 3 , AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.LEGS.getGroup());
                                AttributeModifier modifier_kr = new AttributeModifier(key, 0.1 , AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.LEGS.getGroup());
                                AttributeModifier modifier_mh = new AttributeModifier(key, starCount * 2 , AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.LEGS.getGroup());

                                itemMeta.addAttributeModifier(Attribute.ARMOR, modifier_a);
                                itemMeta.addAttributeModifier(Attribute.ARMOR_TOUGHNESS, modifier_at);
                                itemMeta.addAttributeModifier(Attribute.KNOCKBACK_RESISTANCE, modifier_kr);
                                itemMeta.addAttributeModifier(Attribute.MAX_HEALTH, modifier_mh);
                            }else if (clickedItem.getType().name().endsWith("_BOOTS")) {
                                itemMeta.removeAttributeModifier(Attribute.ARMOR);
                                itemMeta.removeAttributeModifier(Attribute.ARMOR_TOUGHNESS);
                                itemMeta.removeAttributeModifier(Attribute.KNOCKBACK_RESISTANCE);
                                if (starCount != 0) itemMeta.removeAttributeModifier(Attribute.MOVEMENT_SPEED);

                                NamespacedKey key = new NamespacedKey("starforce", MODIFIER_NAME + "_" + clickedItem.getType().name().toLowerCase());
                                AttributeModifier modifier_a = new AttributeModifier(key, 3 , AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.FEET.getGroup());
                                AttributeModifier modifier_at = new AttributeModifier(key, 3 , AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.FEET.getGroup());
                                AttributeModifier modifier_kr = new AttributeModifier(key, 0.1 , AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.FEET.getGroup());
                                AttributeModifier modifier_ms = new AttributeModifier(key, starCount * 0.05 , AttributeModifier.Operation.ADD_SCALAR, EquipmentSlot.FEET.getGroup());

                                itemMeta.addAttributeModifier(Attribute.ARMOR, modifier_a);
                                itemMeta.addAttributeModifier(Attribute.ARMOR_TOUGHNESS, modifier_at);
                                itemMeta.addAttributeModifier(Attribute.KNOCKBACK_RESISTANCE, modifier_kr);
                                itemMeta.addAttributeModifier(Attribute.MOVEMENT_SPEED, modifier_ms);
                            }else if (clickedItem.getType().name().endsWith("ELYTRA")) {

                                itemMeta.removeAttributeModifier(Attribute.MAX_HEALTH);

                                NamespacedKey key = new NamespacedKey("starforce", MODIFIER_NAME + "_" + clickedItem.getType().name().toLowerCase());
                                AttributeModifier modifier_mh = new AttributeModifier(key, starCount * 2 , AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.CHEST.getGroup());

                                itemMeta.addAttributeModifier(Attribute.MAX_HEALTH, modifier_mh);

                            }else if (clickedItem.getType().name().endsWith("TRIDENT")) {

                                itemMeta.removeAttributeModifier(Attribute.ATTACK_DAMAGE);
                                itemMeta.removeAttributeModifier(Attribute.ATTACK_SPEED);

                                NamespacedKey key = new NamespacedKey("starforce", MODIFIER_NAME + "_" + clickedItem.getType().name().toLowerCase());
                                AttributeModifier modifier_a = new AttributeModifier(key, 9 + (starCount * 2) , AttributeModifier.Operation.ADD_NUMBER);
                                AttributeModifier modifier_s = new AttributeModifier(key, -2.9, AttributeModifier.Operation.ADD_NUMBER);

                                itemMeta.addAttributeModifier(Attribute.ATTACK_DAMAGE, modifier_a);
                                itemMeta.addAttributeModifier(Attribute.ATTACK_SPEED, modifier_s);
                            }
                            updateLoreForStars(lore, starCount);

                            if (starCount == 10) {

                                itemMeta.setUnbreakable(true);

                                String originalName = itemMeta.hasDisplayName() ? itemMeta.getDisplayName() : clickedItem.getType().name();
                                String legendaryName = ChatColor.GOLD + "전설의 " + clickedItem.getType().name().toLowerCase();
                                itemMeta.setDisplayName(legendaryName);

                                Bukkit.broadcastMessage(ChatColor.GOLD + player.getName() + " 이/가 " + legendaryName + "을(를) 제작했습니다");

                            }
                            itemMeta.setLore(lore);
                            clickedItem.setItemMeta(itemMeta);

                            syncItemWithInventory(player, playerOriginalItems.get(player), clickedItem);
                            playerOriginalItems.put(player, clickedItem.clone());

                            if (starCount == 10) {
                                player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                                player.closeInventory();
                            } else {
                                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1.0f, 1.0f);
                            }
                        } else {
                            // 강화 실패 시 별 개수 하락
                            if (starCount > 5) {
                                starCount--;

                                if (clickedItem.getType().name().endsWith("_AXE")) {
                                    itemMeta.removeAttributeModifier(Attribute.ATTACK_DAMAGE);
                                    itemMeta.removeAttributeModifier(Attribute.ATTACK_SPEED);

                                    NamespacedKey key = new NamespacedKey("starforce", MODIFIER_NAME + "_" + clickedItem.getType().name().toLowerCase());
                                    AttributeModifier modifier_a = new AttributeModifier(key, 10 + (starCount * 2) , AttributeModifier.Operation.ADD_NUMBER);
                                    AttributeModifier modifier_s = new AttributeModifier(key, -3.0, AttributeModifier.Operation.ADD_NUMBER);

                                    itemMeta.addAttributeModifier(Attribute.ATTACK_DAMAGE, modifier_a);
                                    itemMeta.addAttributeModifier(Attribute.ATTACK_SPEED, modifier_s);
                                }else if (clickedItem.getType().name().endsWith("_SWORD")) {
                                    itemMeta.removeAttributeModifier(Attribute.ATTACK_DAMAGE);
                                    itemMeta.removeAttributeModifier(Attribute.ATTACK_SPEED);

                                    NamespacedKey key = new NamespacedKey("starforce", MODIFIER_NAME + "_" + clickedItem.getType().name().toLowerCase());
                                    AttributeModifier modifier_a = new AttributeModifier(key, 8, AttributeModifier.Operation.ADD_NUMBER);
                                    AttributeModifier modifier_s = new AttributeModifier(key, -2.4 + (starCount * 0.2) , AttributeModifier.Operation.ADD_NUMBER);

                                    itemMeta.addAttributeModifier(Attribute.ATTACK_DAMAGE, modifier_a);
                                    itemMeta.addAttributeModifier(Attribute.ATTACK_SPEED, modifier_s);
                                }else if (clickedItem.getType().name().endsWith("_HELMET")) {
                                    itemMeta.removeAttributeModifier(Attribute.ARMOR);
                                    itemMeta.removeAttributeModifier(Attribute.ARMOR_TOUGHNESS);
                                    itemMeta.removeAttributeModifier(Attribute.KNOCKBACK_RESISTANCE);

                                    NamespacedKey key = new NamespacedKey("starforce", MODIFIER_NAME + "_" + clickedItem.getType().name().toLowerCase());
                                    AttributeModifier modifier_a = new AttributeModifier(key, 3 + starCount , AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.HEAD.getGroup());
                                    AttributeModifier modifier_at = new AttributeModifier(key, 3 , AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.HEAD.getGroup());
                                    AttributeModifier modifier_kr = new AttributeModifier(key, 0.1 , AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.HEAD.getGroup());

                                    itemMeta.addAttributeModifier(Attribute.ARMOR, modifier_a);
                                    itemMeta.addAttributeModifier(Attribute.ARMOR_TOUGHNESS, modifier_at);
                                    itemMeta.addAttributeModifier(Attribute.KNOCKBACK_RESISTANCE, modifier_kr);
                                }else if (clickedItem.getType().name().endsWith("_CHESTPLATE")) {
                                    itemMeta.removeAttributeModifier(Attribute.ARMOR);
                                    itemMeta.removeAttributeModifier(Attribute.ARMOR_TOUGHNESS);
                                    itemMeta.removeAttributeModifier(Attribute.KNOCKBACK_RESISTANCE);
                                    if (starCount != 0) itemMeta.removeAttributeModifier(Attribute.MAX_HEALTH);

                                    NamespacedKey key = new NamespacedKey("starforce", MODIFIER_NAME + "_" + clickedItem.getType().name().toLowerCase());
                                    AttributeModifier modifier_a = new AttributeModifier(key, 8 , AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.CHEST.getGroup());
                                    AttributeModifier modifier_at = new AttributeModifier(key, 3 , AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.CHEST.getGroup());
                                    AttributeModifier modifier_kr = new AttributeModifier(key, 0.1 , AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.CHEST.getGroup());
                                    AttributeModifier modifier_mh = new AttributeModifier(key, starCount * 2 , AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.CHEST.getGroup());

                                    itemMeta.addAttributeModifier(Attribute.ARMOR, modifier_a);
                                    itemMeta.addAttributeModifier(Attribute.ARMOR_TOUGHNESS, modifier_at);
                                    itemMeta.addAttributeModifier(Attribute.KNOCKBACK_RESISTANCE, modifier_kr);
                                    itemMeta.addAttributeModifier(Attribute.MAX_HEALTH, modifier_mh);
                                }else if (clickedItem.getType().name().endsWith("_LEGGINGS")) {
                                    itemMeta.removeAttributeModifier(Attribute.ARMOR);
                                    itemMeta.removeAttributeModifier(Attribute.ARMOR_TOUGHNESS);
                                    itemMeta.removeAttributeModifier(Attribute.KNOCKBACK_RESISTANCE);
                                    if (starCount != 0) itemMeta.removeAttributeModifier(Attribute.MAX_HEALTH);

                                    NamespacedKey key = new NamespacedKey("starforce", MODIFIER_NAME + "_" + clickedItem.getType().name().toLowerCase());
                                    AttributeModifier modifier_a = new AttributeModifier(key, 6 , AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.LEGS.getGroup());
                                    AttributeModifier modifier_at = new AttributeModifier(key, 3 , AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.LEGS.getGroup());
                                    AttributeModifier modifier_kr = new AttributeModifier(key, 0.1 , AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.LEGS.getGroup());
                                    AttributeModifier modifier_mh = new AttributeModifier(key, starCount * 2 , AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.LEGS.getGroup());

                                    itemMeta.addAttributeModifier(Attribute.ARMOR, modifier_a);
                                    itemMeta.addAttributeModifier(Attribute.ARMOR_TOUGHNESS, modifier_at);
                                    itemMeta.addAttributeModifier(Attribute.KNOCKBACK_RESISTANCE, modifier_kr);
                                    itemMeta.addAttributeModifier(Attribute.MAX_HEALTH, modifier_mh);
                                }else if (clickedItem.getType().name().endsWith("_BOOTS")) {
                                    itemMeta.removeAttributeModifier(Attribute.ARMOR);
                                    itemMeta.removeAttributeModifier(Attribute.ARMOR_TOUGHNESS);
                                    itemMeta.removeAttributeModifier(Attribute.KNOCKBACK_RESISTANCE);
                                    if (starCount != 0) itemMeta.removeAttributeModifier(Attribute.MOVEMENT_SPEED);

                                    NamespacedKey key = new NamespacedKey("starforce", MODIFIER_NAME + "_" + clickedItem.getType().name().toLowerCase());
                                    AttributeModifier modifier_a = new AttributeModifier(key, 3 , AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.FEET.getGroup());
                                    AttributeModifier modifier_at = new AttributeModifier(key, 3 , AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.FEET.getGroup());
                                    AttributeModifier modifier_kr = new AttributeModifier(key, 0.1 , AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.FEET.getGroup());
                                    AttributeModifier modifier_ms = new AttributeModifier(key, starCount * 0.05 , AttributeModifier.Operation.ADD_SCALAR, EquipmentSlot.FEET.getGroup());

                                    itemMeta.addAttributeModifier(Attribute.ARMOR, modifier_a);
                                    itemMeta.addAttributeModifier(Attribute.ARMOR_TOUGHNESS, modifier_at);
                                    itemMeta.addAttributeModifier(Attribute.KNOCKBACK_RESISTANCE, modifier_kr);
                                    itemMeta.addAttributeModifier(Attribute.MOVEMENT_SPEED, modifier_ms);
                                }else if (clickedItem.getType().name().endsWith("ELYTRA")) {

                                    itemMeta.removeAttributeModifier(Attribute.MAX_HEALTH);

                                    NamespacedKey key = new NamespacedKey("starforce", MODIFIER_NAME + "_" + clickedItem.getType().name().toLowerCase());
                                    AttributeModifier modifier_mh = new AttributeModifier(key, starCount * 2 , AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.CHEST.getGroup());

                                    itemMeta.addAttributeModifier(Attribute.MAX_HEALTH, modifier_mh);

                                }else if (clickedItem.getType().name().endsWith("TRIDENT")) {

                                    itemMeta.removeAttributeModifier(Attribute.ATTACK_DAMAGE);
                                    itemMeta.removeAttributeModifier(Attribute.ATTACK_SPEED);

                                    NamespacedKey key = new NamespacedKey("starforce", MODIFIER_NAME + "_" + clickedItem.getType().name().toLowerCase());
                                    AttributeModifier modifier_a = new AttributeModifier(key, 9 + (starCount * 2) , AttributeModifier.Operation.ADD_NUMBER);
                                    AttributeModifier modifier_s = new AttributeModifier(key, -2.9, AttributeModifier.Operation.ADD_NUMBER);

                                    itemMeta.addAttributeModifier(Attribute.ATTACK_DAMAGE, modifier_a);
                                    itemMeta.addAttributeModifier(Attribute.ATTACK_SPEED, modifier_s);
                                }
                                updateLoreForStars(lore, starCount);

                                itemMeta.setLore(lore);
                                clickedItem.setItemMeta(itemMeta);

                                syncItemWithInventory(player, playerOriginalItems.get(player), clickedItem);
                                playerOriginalItems.put(player, clickedItem.clone());

                                player.sendMessage(ChatColor.RED + "강화에 실패하여 별이 하락했습니다!");
                                player.playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 1.0f, 1.0f);
                            } else {
                                player.sendMessage(ChatColor.RED + "강화에 실패했습니다!");
                                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 1.0f);
                            }
                        }
                    }
                } else {
                    player.sendMessage(ChatColor.DARK_PURPLE + "별조각" + ChatColor.RED + "이 부족합니다!");
                    player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 1.0f);
                }
            }
        }
    }


    private boolean hasValidStarPiece(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.ECHO_SHARD) {
                ItemMeta meta = item.getItemMeta();
                if (meta != null && meta.hasDisplayName() && meta.getDisplayName().equals(ChatColor.DARK_PURPLE + "별조각")) {
                    if (item.getAmount() > 0) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void removeStarPiece(Player player) {
        for (ItemStack item : player.getInventory().getStorageContents()) {
            if (item != null && item.getType() == Material.ECHO_SHARD) {
                ItemMeta meta = item.getItemMeta();
                if (meta != null && meta.getDisplayName().equals(ChatColor.DARK_PURPLE + "별조각")) {
                    int newAmount = item.getAmount() - 1;
                    if (newAmount <= 0) {
                        player.getInventory().remove(item);
                    } else {
                        item.setAmount(newAmount);
                    }
                    player.updateInventory();
                    break;
                }
            }
        }
    }

    private void syncItemWithInventory(Player player, ItemStack originalItem, ItemStack modifiedItem) {
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack inventoryItem = player.getInventory().getItem(i);
            if (inventoryItem != null && inventoryItem.equals(originalItem)) {
                player.getInventory().setItem(i, modifiedItem.clone());
                break;
            }
        }
        player.updateInventory(); // 인벤토리 갱신
    }

    private boolean isAllowedItemType(Material material) {
        Set<Material> allowedItemTypes = EnumSet.of(
                Material.NETHERITE_HELMET, Material.NETHERITE_CHESTPLATE, Material.NETHERITE_LEGGINGS, Material.NETHERITE_BOOTS,

                Material.NETHERITE_SWORD,
                Material.NETHERITE_AXE, Material.ELYTRA, Material.TRIDENT, Material.BOW

                //Material.SHIELD, Material.CROSSBOW,
                //Material.FISHING_ROD, Material.MACE
        );
        return allowedItemTypes.contains(material);
    }

    private void updateSlot13Lore(Player player, ItemStack item) {
        if (item != null && item.getItemMeta() != null) {
            ItemMeta itemMeta = item.getItemMeta();
            List<String> lore = itemMeta.hasLore() ? itemMeta.getLore() : new ArrayList<>();

            String currentStars = lore != null && !lore.isEmpty() ? lore.get(0) : ChatColor.GOLD + "☆☆☆☆☆☆☆☆☆☆";

            // Count current stars
            int starCount = (int) currentStars.chars().filter(ch -> ch == '★').count();

            // Update lore to include success and destruction rates dynamically
            lore.clear(); // Reset all lore entries to avoid duplicate stars or rates
            lore.add(currentStars); // Add stars as the first line
            if (!(starCount >= 10)) {
                lore.add(ChatColor.GREEN + "- 성공 확률: " + ChatColor.YELLOW + successRates[starCount] + "%");
                lore.add(ChatColor.RED + "- 파괴 확률: " + ChatColor.YELLOW + destructionRates[starCount] + "%");
            }
            if (starCount >= 5) {
                lore.add(ChatColor.DARK_AQUA + "- 강화 실패 시 하락 확률이 있습니다.");
            }

            itemMeta.setLore(lore);
            item.setItemMeta(itemMeta);
            player.updateInventory();
        }
    }


    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getView().getTitle().equals(ChatColor.WHITE + "\uEBBB\uE004")) {
            Player player = (Player) event.getPlayer();
            Inventory closedInventory = event.getInventory();
            ItemStack itemInSlot13 = closedInventory.getItem(13);

            if (itemInSlot13 != null && itemInSlot13.hasItemMeta()) {
                ItemMeta meta = itemInSlot13.getItemMeta();
                if (meta.hasLore()) {
                    List<String> lore = meta.getLore();

                    // Remove success and destruction rate lore entries
                    if (lore != null && lore.size() > 1) {
                        lore = lore.subList(0, 1); // Only keep the first line (stars)
                        meta.setLore(lore);
                        itemInSlot13.setItemMeta(meta);

                        // Synchronize the updated item with the player's inventory
                        syncItemWithInventory(player, playerOriginalItems.get(player), itemInSlot13);
                        updateSlot13Lore(player, itemInSlot13);
                    }
                }
            }

            // Clear player-original item mapping when inventory is closed
            playerOriginalItems.remove(player);
        }
    }

    private void updateLoreForStars(List<String> lore, int starCount) {
        lore.clear(); // 기존 로어 초기화
        lore.add(ChatColor.GOLD + "★".repeat(starCount) + "☆".repeat(10 - starCount)); // 별 개수 업데이트

        // 로어에 강화 성공률과 파괴 확률 동적 업데이트
        if (starCount < successRates.length) {
            if (!(starCount >= 10)) {
                lore.add(ChatColor.GREEN + "- 성공 확률: " + ChatColor.YELLOW + successRates[starCount] + "%");
                lore.add(ChatColor.RED + "- 파괴 확률: " + ChatColor.YELLOW + destructionRates[starCount] + "%");
            }
        }

        if (starCount >= 6) {
            lore.add(ChatColor.DARK_AQUA + "- 강화 실패 시 하락 확률이 있습니다.");
        }

    }
}
 

