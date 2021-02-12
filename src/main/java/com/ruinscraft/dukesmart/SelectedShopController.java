package com.ruinscraft.dukesmart;

import java.util.HashMap;
import java.util.Map.Entry;

import org.bukkit.entity.Player;

public class SelectedShopController {
	private HashMap<Player, Shop> selectionMap;
	
	public SelectedShopController() {
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
	
	public void removeShop(Shop shop) {
		for(Entry<Player, Shop> e : selectionMap.entrySet()) {
			if(e.getValue().equals(shop)) {
				selectionMap.remove(e.getKey());
			}
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
