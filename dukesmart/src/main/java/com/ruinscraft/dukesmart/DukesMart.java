package com.ruinscraft.dukesmart;

import org.bukkit.plugin.java.JavaPlugin;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

import java.util.ArrayList;
import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class DukesMart extends JavaPlugin {
	ShopListener sl;
	
    @Override
    public void onEnable() {
    	this.sl = new ShopListener();
    	
    	Bukkit.getPluginManager().registerEvents(this.sl, this);
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
            player.sendMessage(ChatColor.AQUA + "Hello, world!");
            return true;
        }
        else if(cmd.getName().equalsIgnoreCase("goodbye")) {
        	player.sendMessage(ChatColor.RED + "It's been nice knowing ya...");
        	player.damage(10.0);
        	return true;
        }
        else if(cmd.getName().equalsIgnoreCase("godmode")) {
        	player.setGameMode(GameMode.CREATIVE);
        	player.sendMessage("Creative mode activated!");
        	return true;
        }
        else if(cmd.getName().equalsIgnoreCase("display_list")){
        	HashMap<String,String> map = this.sl.getSelectedMap();
        	for(String key : map.keySet()) {
        		player.sendMessage(ChatColor.GRAY + key + " : " + map.get(key));
        	}
        	return true;
        }
        else if(cmd.getName().equalsIgnoreCase("check_sign")) {
			String playerUID = player.getUniqueId().toString();
			HashMap<String, String> signMap = this.sl.getSelectedMap();
			if( signMap.containsKey(playerUID)) {
				String value = signMap.get(playerUID);
				
				player.sendMessage(ChatColor.GRAY + "Debug: selected sign at coords: " + value);
			}
        	return true;
        }

        return false;
    }
}