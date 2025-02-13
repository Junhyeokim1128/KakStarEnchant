package me.junhyeokim.starForce.Listener;

import org.bukkit.entity.Player;
import org.bukkit.entity.Arrow;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class ArrowDamageListener implements Listener {

    @EventHandler
    public void onArrowDamage(EntityDamageByEntityEvent event) {
        // 만약 다가오는 피해가 화살에 의한 것이라면
        if (event.getDamager() instanceof Arrow) {
            Arrow arrow = (Arrow) event.getDamager();
            if (arrow.getShooter() instanceof Player) {
                Player player = (Player) arrow.getShooter();
                ItemStack bow = player.getInventory().getItemInMainHand();

                if (bow != null && bow.hasItemMeta()) {
                    ItemMeta meta = bow.getItemMeta();
                    if (meta != null && meta.hasLore()) {
                        // Lore에서 별(★)의 개수를 찾는다.
                        int starCount = 0;
                        for (String line : meta.getLore()) {
                            starCount += countStars(line);
                        }

                        // 기존 대미지에 별 개수 × 2 추가
                        double baseDamage = event.getDamage();
                        double newDamage = baseDamage + (starCount * 2);

                        // 대미지 적용
                        event.setDamage(newDamage);
                    }
                }
            }
        }
    }

    // 별의 개수를 세는 함수
    private int countStars(String line) {
        int count = 0;
        for (char c : line.toCharArray()) {
            if (c == '★') {
                count++;
            }
        }
        return count;
    }
}

