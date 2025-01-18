package me.junhyeokim.starForce.Commands;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class GetStarDust implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // 명령어를 실행한 객체가 플레이어인지 확인
        if (sender instanceof Player) {
            Player player = (Player) sender;

            // 커스텀 데이터가 적용된 발광석 가루 64개 아이템 생성
            ItemStack starDust = new ItemStack(Material.GLOWSTONE_DUST, 64);

            // 아이템의 메타데이터 가져오기
            ItemMeta meta = starDust.getItemMeta();

            meta.setCustomModelData(1);
            meta.setDisplayName(ChatColor.GOLD +"별가루");
            // 아이템에 메타데이터 적용
            starDust.setItemMeta(meta);


            // 플레이어에게 아이템 지급
            player.getInventory().addItem(starDust);

            // 명령어가 정상적으로 실행되었음을 알리는 메시지


            return true;
        } else {
            // 콘솔에서 실행 시 오류 메시지
            sender.sendMessage("Only players can use this command.");
            return false;
        }
    }
}
