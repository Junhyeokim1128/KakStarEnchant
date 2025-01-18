package me.junhyeokim.starForce.Listener;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class ClickAnvil implements Listener {

    private ItemStack originalItem; // 플레이어가 클릭한 원본 아이템

    @EventHandler
    public void clickEnchantTable(PlayerInteractEvent event) {
        if (event.getAction().toString().contains("RIGHT_CLICK_BLOCK")) {
            if (event.getClickedBlock() != null && event.getClickedBlock().getType() == Material.ANVIL
                    || event.getClickedBlock().getType() == Material.DAMAGED_ANVIL
                    || event.getClickedBlock().getType() == Material.CHIPPED_ANVIL) {
                event.setCancelled(true);

                Player player = event.getPlayer();
                Inventory anvilMenu = Bukkit.createInventory(player, 27, ChatColor.WHITE + "\uEBBB\uE004");
                player.openInventory(anvilMenu);
                originalItem = null; // 원본 아이템 초기화
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
                originalItem = clickedItem; // 원본 아이템 참조 저장
                ItemStack itemToEnchant = clickedItem.clone();
                clickedInventory.setItem(13, itemToEnchant);
                return;
            }
        }
    }
}


