package me.junhyeokim.starForce;

import me.junhyeokim.starForce.Commands.GetStarDust;
import me.junhyeokim.starForce.Commands.GetStarPiece;
import me.junhyeokim.starForce.Listener.ClickAnvil;
import me.junhyeokim.starForce.Listener.ClickEnchantTable;
import org.bukkit.plugin.java.JavaPlugin;

public final class StarForce extends JavaPlugin {

    @Override
    public void onEnable() {
        //getServer().getPluginManager().registerEvents(new ClickEnchantTable(), this);
        getServer().getPluginManager().registerEvents(new ClickAnvil(), this);
        getServer().getPluginManager().registerEvents(new ClickEnchantTable(), this);
        getCommand("stardust").setExecutor(new GetStarDust());
        getCommand("starpiece").setExecutor(new GetStarPiece());
    }

}
