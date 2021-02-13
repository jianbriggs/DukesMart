package com.ruinscraft.dukesmart;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

public class NotifyPlayerIncomeTask extends BukkitRunnable{
	
	private final Player player;
	private final short maxSeconds = 3;
	private short time = 0;
	
	public NotifyPlayerIncomeTask(Player player) {
		this.player = player;
	}
	
	@Override
	public void run() {
		if(time == maxSeconds ) {
			Bukkit.getScheduler().cancelTask(this.getTaskId());
		}
		else {
			if(player.isOnline()) {
				final String notification = "" + ChatColor.GREEN + ChatColor.BOLD + "You've got cash! " + ChatColor.GOLD + "/shop balance";
				player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(notification));
			}
			time++;
		}
	}
}
