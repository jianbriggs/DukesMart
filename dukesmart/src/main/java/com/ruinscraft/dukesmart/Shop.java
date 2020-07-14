package com.ruinscraft.dukesmart;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class Shop {
	private String owner_uuid;
	private String name;
	private String world;
	private short loc_x;
	private byte loc_y;
	private short loc_z;
	private ItemStack item;
	
	public Shop(String owner_uuid, String name, String world, short loc_x, byte loc_y, short loc_z) {
		this.owner_uuid = owner_uuid;
		this.name = name;
		this.world = world;
		this.loc_x = loc_x;
		this.loc_y = loc_y;
		this.loc_z = loc_z;
	}
	
	public void setItem(ItemStack item) {
		this.item = item;
	}
	
	public String getOwnerName() {
		return Bukkit.getPlayer(UUID.fromString(this.owner_uuid)).getName();
	}
	
	public String getOwner() {
		return this.owner_uuid;
	}
}
