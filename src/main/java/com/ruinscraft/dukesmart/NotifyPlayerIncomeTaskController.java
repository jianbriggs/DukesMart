package com.ruinscraft.dukesmart;

import java.util.HashMap;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

public class NotifyPlayerIncomeTaskController {
	private DukesMart plugin;
	private HashMap<Player, BukkitTask> tasks = new HashMap<Player, BukkitTask>();
	
	//private int DELAY_SECONDS = 1800; // 30 minutes
	private int DELAY_SECONDS = 10;
	
	public NotifyPlayerIncomeTaskController(DukesMart plugin) {
		this.plugin = plugin;
	}
	
	public void addTask(Player player) {
		if(!playerHasTask(player)) {
			BukkitTask task = new NotifyPlayerIncomeTask(player).runTaskLater(this.plugin, 20*this.DELAY_SECONDS);
			tasks.put(player, task);
		}
	}
	public void removeTask(Player player) {
		if(playerHasTask(player)) {
			tasks.remove(player);
		}
	}
	
	public boolean playerHasTask(Player player) {
		return this.tasks.containsKey(player);
	}
}
