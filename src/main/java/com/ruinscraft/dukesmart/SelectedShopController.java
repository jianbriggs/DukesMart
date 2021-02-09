package com.ruinscraft.dukesmart;

import java.util.HashMap;

import org.bukkit.entity.Player;

public class SelectedShopController {
	private DukesMart plugin;
	private HashMap<Player, Shop> selectionMap;
	
	public SelectedShopController(DukesMart plugin) {
		this.plugin = plugin;
		this.selectionMap = new HashMap<Player, Shop>();
	}
	
	public void addSelection(Player player, Shop shop) {
		selectionMap.put(player, shop);
	}
	public void removeSelection(Player player) {
		if(playerHasSelection(player)) {
			selectionMap.remove(player);
		}
	}
	
	public Shop getSelection(Player player) {
		if(playerHasSelection(player)) {
			return selectionMap.get(player);
		}
		return null;
	}
	
	public boolean playerHasSelection(Player player) {
		return this.selectionMap.containsKey(player);
	}
}
