package com.ruinscraft.dukesmart;

import java.util.HashMap;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

public class SelectedShopController {
	private DukesMart plugin;
	private HashMap<Player, Location> selectionMap;
	
	public SelectedShopController(DukesMart plugin) {
		this.plugin = plugin;
		this.selectionMap = new HashMap<Player, Location>();
	}
	
	public void addSelection(Player player, Location shop) {
		selectionMap.put(player, shop);
	}
	public void removeSelection(Player player) {
		if(playerHasSelection(player)) {
			selectionMap.remove(player);
		}
	}
	
	public Location getSelection(Player player) {
		if(playerHasSelection(player)) {
			return selectionMap.get(player);
		}
		return null;
	}
	
	public boolean playerHasSelection(Player player) {
		return this.selectionMap.containsKey(player);
	}
}
