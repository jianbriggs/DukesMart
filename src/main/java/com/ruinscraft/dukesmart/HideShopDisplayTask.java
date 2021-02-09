package com.ruinscraft.dukesmart;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class HideShopDisplayTask extends BukkitRunnable{
	
	private final Player player;

	public HideShopDisplayTask(Player player) {
		this.player = player;
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		if(player.isOnline() && player.getScoreboard() != null) {
			player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
		}
	}

}
