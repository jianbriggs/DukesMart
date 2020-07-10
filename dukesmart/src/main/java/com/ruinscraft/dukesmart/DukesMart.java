package com.ruinscraft.dukesmart;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DukesMart extends JavaPlugin {

    @Override
    public void onEnable() {
    	getLogger().info("DukesMart has been enabled!");
    	for (Player player : Bukkit.getServer().getOnlinePlayers()) {
    	    //playerList.put(player.getName(), playerData(player));
    	}
    }

    @Override
    public void onDisable() {
    	getLogger().info("DukesMart has been disabled!");
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
        Player player = (Player) sender;
        if(cmd.getName().equalsIgnoreCase("hello")){
            player.sendMessage("Hello, world!");
            return true;
        }

        return false;
    }
}