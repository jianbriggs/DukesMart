package com.ruinscraft.dukesmart;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import net.md_5.bungee.api.ChatColor;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class DukesMart extends JavaPlugin {
	private FileConfiguration config = getConfig();
	private ShopListener sl;
	
    @Override
    public void onEnable() {
    	this.sl = new ShopListener();
    	// check config.yml for database credentials
    	if(!config.contains("mysql.host") || !config.contains("mysql.port") || !config.contains("mysql.database")
    	   || !config.contains("mysql.username") || !config.contains("mysql.password")) {
    		
    		getLogger().info("[DukesMart] Failed to find database information in config.yml!");
    		getLogger().info("[DukesMart] Adding default values to config.yml");
    		
    		config.addDefault("mysql.host", "localhost");
    		config.addDefault("mysql.port", 3306);
    		config.addDefault("mysql.database", "mcatlas");
    		config.addDefault("mysql.username", "mcatlasdev");
    		config.addDefault("mysql.password", "password123");
    		
    		config.options().copyDefaults(true);
            saveConfig();
    		
    	}

    	Bukkit.getPluginManager().registerEvents(this.sl, this);
    	
    	getLogger().info("[DukesMart] has been enabled!");
    	
    	for (Player player : Bukkit.getServer().getOnlinePlayers()) {
    	    //playerList.put(player.getName(), playerData(player));
    	}
    }

    @Override
    public void onDisable() {
    	getLogger().info("[DukesMart] has been disabled!");
    }
    
    public String shopGuiPad(String message) {
    	message += ChatColor.RESET;
    	
    	while(message.length() < 32) {
    		message += " ";
    	}
    	
    	return message;
    }
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
        Player player = (Player) sender;
        if(cmd.getName().equalsIgnoreCase("clear")){
    	    player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    		player.sendMessage("(Debug) Scoreboard removed.");
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
        	HashMap<String, Location> map = this.sl.getSelectedMap();
        	for(String key : map.keySet()) {
        		player.sendMessage(ChatColor.GRAY + key + " : " + map.get(key));
        	}
        	return true;
        }
        else if(cmd.getName().equalsIgnoreCase("check_sign")) {
			String playerUID = player.getUniqueId().toString();
			HashMap<String, Location> signMap = this.sl.getSelectedMap();
			if( signMap.containsKey(playerUID)) {
				Location value = signMap.get(playerUID);
				
				player.sendMessage(ChatColor.GRAY + "Debug: selected sign at coords: " + value);
			}
        	return true;
        }
        else if(cmd.getName().equalsIgnoreCase("database_check")) {
        	String url = "jdbc:mysql://localhost:3306/mcatlas?useSSL=false";
            String user = "mcatlas_dev";
            String password = "ruinscraft1138$";
            
            String query = "SELECT VERSION()";

            try (Connection con = DriverManager.getConnection(url, user, password);
                Statement st = con.createStatement();
                ResultSet rs = st.executeQuery(query)) {

                if (rs.next()) {
                    player.sendMessage(ChatColor.LIGHT_PURPLE + rs.getString(1));
                }

            } catch (SQLException ex) {
            	player.sendMessage(ChatColor.RED + "Error with database.");
            	player.sendMessage(ex.getMessage());
            } 
            
            return true;
        }

        return false;
    }
}