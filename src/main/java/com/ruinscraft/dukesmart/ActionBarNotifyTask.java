package com.ruinscraft.dukesmart;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

public class ActionBarNotifyTask extends BukkitRunnable{
	
	private final Player player;
	private final String message;
	private int maxSeconds = 3;
	private int time = 0;
	
	public ActionBarNotifyTask(Player player, String message, int maxSeconds) {
		this.player = player;
		this.message = message;
		this.maxSeconds = maxSeconds;
	}
	
	@Override
	public void run() {
		if(time == maxSeconds ) {
			Bukkit.getScheduler().cancelTask(this.getTaskId());
		}
		else {
			if(player.isOnline()) {
				player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
			}
			time++;
		}
	}
}
