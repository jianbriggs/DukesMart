package com.ruinscraft.dukesmart;

import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

public class NotifyPlayerIncomeTaskController {
	private DukesMart plugin;
	private HashMap<Player, BukkitTask> tasks = new HashMap<Player, BukkitTask>();
	
	private int DELAY_SECONDS = 1800; // 30 minutes
	
	public NotifyPlayerIncomeTaskController(DukesMart plugin) {
		this.plugin = plugin;
	}
	
	public void addTask(Player player) {
		if(player.isOnline()) {
			if(playerHasTask(player)) {
				BukkitTask task = tasks.get(player);
				int taskID = task.getTaskId();
				if(Bukkit.getScheduler().isQueued(taskID)) {
					return;
				}
			}
	
			BukkitTask task = new NotifyPlayerIncomeTask(player).runTaskLater(this.plugin, 20*this.DELAY_SECONDS);
			tasks.put(player, task);
		}
	}
	
	public void removeTask(Player player) {
		if(playerHasTask(player)) {
			BukkitTask task = tasks.get(player);
			cancelTask(task);
			tasks.replace(player, null);
		}
	}
	
	public boolean playerHasTask(Player player) {
		return this.tasks.containsKey(player) && this.tasks.get(player) != null;
	}
	
	public void removePlayer(Player player) {
		if(this.tasks.containsKey(player)) {
			BukkitTask task = this.tasks.get(player);
			if(task != null) {
				cancelTask(task);
			}
			tasks.remove(player);
		}
	}
	
	private void cancelTask(BukkitTask task) {
		int taskID = task.getTaskId();

		if(Bukkit.getScheduler().isQueued(taskID)) {
			Bukkit.getScheduler().cancelTask(taskID);
		}
	}
}
