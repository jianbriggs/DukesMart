package com.ruinscraft.dukesmart;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import net.md_5.bungee.api.ChatColor;

public class NotifyPlayerIncomeTask extends BukkitRunnable{
	
	private final Player player;

	public NotifyPlayerIncomeTask(Player player) {
		this.player = player;
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		if(player.isOnline()) {
			player.sendMessage(ChatColor.GREEN + "You've got cash!" + ChatColor.AQUA + " Check your balance with "
								+ ChatColor.GOLD + "/shop balance" + ChatColor.AQUA + " or withdraw with " + ChatColor.GOLD + "/shop withdraw");
		}
	}

}
