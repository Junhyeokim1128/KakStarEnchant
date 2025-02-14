package me.junhyeokim.starForce.Listener;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class ClickEnchantTable implements Listener {

    // 플레이어별 originalItem을 저장할 Map
    private final Map<Player, ItemStack> playerOriginalItems = new HashMap<>();

    @EventHandler
    public void clickEnchantTable(PlayerInteractEvent event) {
        if (event.getAction().toString().contains("RIGHT_CLICK_BLOCK")) {
            if (event.getClickedBlock() != null && event.getClickedBlock().getType() == Material.ENCHANTING_TABLE) {
                event.setCancelled(true);

                Player player = event.getPlayer();
                Inventory enchantMenu = Bukkit.createInventory(player, 27, ChatColor.WHITE + "\uEBBB\uE003");
                player.openInventory(enchantMenu);

                // 플레이어별 원본 아이템을 초기화
                playerOriginalItems.remove(player);
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals(ChatColor.WHITE + "\uEBBB\uE003")) {
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

                // 허용된 아이템인지 확인
                if (!isAllowedItemType(clickedItem.getType())) {
                    player.sendMessage(ChatColor.RED + "이 아이템은 인첸트할 수 없습니다!");
                    event.setCancelled(true);
                    return;
                }

                playerOriginalItems.put(player, clickedItem.clone()); // 플레이어별 원본 아이템 저장
                ItemStack itemToEnchant = clickedItem.clone();
                clickedInventory.setItem(13, itemToEnchant);
                return;
            }

            if (event.getSlot() == 13 && clickedItem != null) {
                // 최초로 원본 아이템이 설정되지 않았다면 초기화
                if (!playerOriginalItems.containsKey(player)) {
                    playerOriginalItems.put(player, clickedItem.clone());
                }

                // 원본 아이템 검사 (처음만 확인)
                if (!playerOriginalItems.get(player).isSimilar(clickedItem)) {
                    player.sendMessage(ChatColor.RED + "13번 슬롯의 아이템이 원래 설정된 아이템과 일치하지 않습니다.");
                    event.setCancelled(true);
                    return;
                }

                // "별가루" 확인 및 소비
                if (hasValidStarDust(player)) {
                    removeStarDust(player);

                    ItemMeta itemMeta = clickedItem.getItemMeta();
                    if (itemMeta != null) {
                        Random random = new Random();

                        // 기존 인첸트 확인
                        List<Enchantment> existingEnchantments = new ArrayList<>(itemMeta.getEnchants().keySet());
                        int originalEnchantmentCount = existingEnchantments.size();

                        // 기존 인첸트 제거
                        for (Enchantment enchantment : existingEnchantments) {
                            itemMeta.removeEnchant(enchantment);
                        }

                        // 추가 가능한 인첸트 목록 (저주 제외)
                        List<Enchantment> possibleEnchantments = new ArrayList<>();
                        for (Enchantment enchantment : Enchantment.values()) {
                            if (enchantment != Enchantment.BINDING_CURSE && enchantment != Enchantment.VANISHING_CURSE) {
                                possibleEnchantments.add(enchantment);
                            }
                        }

                        // 랜덤으로 인첸트 추가 (기존 로직 유지)
                        if (originalEnchantmentCount == 0) {
                            addRandomEnchantmentsWithoutDuplicates(itemMeta, random, possibleEnchantments);
                        }

                        for (int i = 0; i < originalEnchantmentCount; i++) {
                            addRandomEnchantmentsWithoutDuplicates(itemMeta, random, possibleEnchantments);

                        }

                        if (originalEnchantmentCount <= 6 && random.nextInt(100) < 10) {
                            addRandomEnchantmentsWithoutDuplicates(itemMeta, random, possibleEnchantments);
                        }

                        player.playSound(player.getLocation(), Sound.BLOCK_CHAIN_HIT, 1.0f, 1.0f);
                        clickedItem.setItemMeta(itemMeta);

                        // ** 인첸트된 아이템을 원래 아이템과 동기화 **
                        syncItemWithInventory(player, playerOriginalItems.get(player), clickedItem);

                        // 여기서 `originalItem`을 업데이트한다!
                        playerOriginalItems.put(player, clickedItem.clone()); // 인첸트 후 현재 상태로 갱신

                    }
                } else {
                    player.sendMessage(ChatColor.GOLD + "\"별가루\"" + ChatColor.RED + "가 부족합니다!");
                    player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 1.0f);
                }
            }
        }
    }

    private boolean hasValidStarDust(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.GLOWSTONE_DUST) {
                ItemMeta meta = item.getItemMeta();
                if (meta != null && meta.hasDisplayName() && meta.getDisplayName().equals(ChatColor.GOLD + "별가루")) {
                    return true;
                }
            }
        }
        return false;
    }

    private void removeStarDust(Player player) {
        for (ItemStack item : player.getInventory().getStorageContents()) {
            if (item != null && item.getType() == Material.GLOWSTONE_DUST) {
                ItemMeta meta = item.getItemMeta();
                if (meta != null && meta.getDisplayName().equals(ChatColor.GOLD + "별가루")) {
                    item.setAmount(item.getAmount() - 1);
                    if (item.getAmount() <= 0) {
                        player.getInventory().remove(item);
                    }
                    break;
                }
            }
        }
    }

    private void addRandomEnchantmentsWithoutDuplicates(ItemMeta itemMeta, Random random, List<Enchantment> possibleEnchantments) {
        if (possibleEnchantments == null || possibleEnchantments.isEmpty()) return;
        List<Enchantment> shuffledEnchantments = new ArrayList<>(possibleEnchantments);
        Collections.shuffle(shuffledEnchantments, random);
        for (Enchantment enchantment : shuffledEnchantments) {
            if (!itemMeta.hasEnchant(enchantment)) {
                int level = random.nextInt(enchantment.getMaxLevel()) + 1;
                itemMeta.addEnchant(enchantment, level, true);
                break;
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
        player.updateInventory();
    }

    // 허용된 아이템 종류를 확인하는 메서드
    private boolean isAllowedItemType(Material material) {
        Set<Material> allowedItemTypes = EnumSet.of(
                Material.NETHERITE_HELMET, Material.NETHERITE_CHESTPLATE, Material.NETHERITE_LEGGINGS, Material.NETHERITE_BOOTS,
                Material.DIAMOND_HELMET, Material.DIAMOND_CHESTPLATE, Material.DIAMOND_LEGGINGS, Material.DIAMOND_BOOTS,
                Material.IRON_HELMET, Material.IRON_CHESTPLATE, Material.IRON_LEGGINGS, Material.IRON_BOOTS,
                Material.GOLDEN_HELMET, Material.GOLDEN_CHESTPLATE, Material.GOLDEN_LEGGINGS, Material.GOLDEN_BOOTS,
                Material.LEATHER_HELMET, Material.LEATHER_CHESTPLATE, Material.LEATHER_LEGGINGS, Material.LEATHER_BOOTS,
                Material.CHAINMAIL_HELMET, Material.CHAINMAIL_CHESTPLATE, Material.CHAINMAIL_LEGGINGS, Material.CHAINMAIL_BOOTS,

                Material.TURTLE_HELMET,

                Material.DIAMOND_SWORD, Material.NETHERITE_SWORD, Material.IRON_SWORD, Material.STONE_SWORD, Material.WOODEN_SWORD, Material.GOLDEN_SWORD,
                Material.DIAMOND_PICKAXE, Material.NETHERITE_PICKAXE, Material.IRON_PICKAXE, Material.STONE_PICKAXE, Material.WOODEN_PICKAXE, Material.GOLDEN_PICKAXE,
                Material.DIAMOND_AXE, Material.NETHERITE_AXE, Material.IRON_AXE, Material.STONE_AXE, Material.WOODEN_AXE, Material.GOLDEN_AXE,
                Material.DIAMOND_SHOVEL, Material.NETHERITE_SHOVEL, Material.IRON_SHOVEL, Material.STONE_SHOVEL, Material.WOODEN_SHOVEL, Material.GOLDEN_SHOVEL,
                Material.DIAMOND_HOE, Material.NETHERITE_HOE, Material.IRON_HOE, Material.STONE_HOE, Material.WOODEN_HOE, Material.GOLDEN_HOE,

                Material.TRIDENT, Material.SHIELD, Material.BOW, Material.CROSSBOW,
                Material.FISHING_ROD, Material.ELYTRA, Material.MACE
        );
        return allowedItemTypes.contains(material);
    }
}