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

            // 인자가 주어졌는지 확인하고, 숫자 값으로 변환
            if (args.length == 1) {
                try {
                    int amount = Integer.parseInt(args[0]);

                    // 유효한 수량인지 체크 (최소 1 이상)
                    if (amount > 0) {
                        // 커스텀 데이터가 적용된 발광석 가루 아이템 생성
                        ItemStack starPiece = new ItemStack(Material.GLOWSTONE_DUST, amount);

                        // 아이템의 메타데이터 가져오기
                        ItemMeta meta = starPiece.getItemMeta();
                        meta.setCustomModelData(1);
                        meta.setDisplayName(ChatColor.GOLD + "별가루");

                        // 아이템에 메타데이터 적용
                        starPiece.setItemMeta(meta);

                        // 플레이어에게 아이템 지급
                        player.getInventory().addItem(starPiece);

                    } else {
                    }
                } catch (NumberFormatException e) {
                }
            } else {
                player.sendMessage(ChatColor.RED + "사용법: /stardust <수량>");
            }

            return true;
        } else {
            // 콘솔에서 실행 시 오류 메시지
            sender.sendMessage("Only players can use this command.");
            return false;
        }
    }
}