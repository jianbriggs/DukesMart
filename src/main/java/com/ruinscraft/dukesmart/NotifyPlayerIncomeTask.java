package com.ruinscraft.dukesmart;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import net.md_5.bungee.api.ChatColor;

public class NotifyPlayerIncomeTask extends BukkitRunnable{
	
	private final Player player;
	private boolean complete;
	
	public NotifyPlayerIncomeTask(Player player) {
		this.player = player;
		this.complete = false;
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		this.complete = true;
		if(player.isOnline()) {
			player.sendMessage(ChatColor.GREEN + "You've got cash!" + ChatColor.AQUA + " Check your balance with "
								+ ChatColor.GOLD + "/shop balance" + ChatColor.AQUA + " or withdraw with " + ChatColor.GOLD + "/shop withdraw");
		}
	}
	
	public boolean isComplete() {
		return this.complete;
	}
}
