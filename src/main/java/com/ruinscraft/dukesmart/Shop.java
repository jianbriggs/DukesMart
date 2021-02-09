package com.ruinscraft.dukesmart;

import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class Shop {
	private final String id;
	private final String owner_uuid;
	private final String world_name;
	private final short loc_x;
	private final byte loc_y;
	private final short loc_z;
	private ItemStack item;
	
	private short quantity;
	private int price;
	
	public Shop(String id, String owner_uuid, String world, short loc_x, byte loc_y, short loc_z, ItemStack item, short quantity, int price) {
		this.id = id;
		this.owner_uuid = owner_uuid;
		this.world_name = world;
		this.loc_x = loc_x;
		this.loc_y = loc_y;
		this.loc_z = loc_z;
		this.item = item;
		this.quantity = quantity;
		this.price = price;
	}
	
	public String getID() {
		return this.id;
	}
	
	public ItemStack getItem() {
		return this.item;
	}
	
	public void setItem(ItemStack item) {
		this.item = item;
	}
	
	public Map<Enchantment, Integer> getItemEnchantments() {
		return this.item.getEnchantments();
	}
	
	public ItemMeta getItemMeta() {
		if(this.item.hasItemMeta()) {
			return this.item.getItemMeta();
		}
		else return null;
	}
	
	public String getOwnerName() {
		OfflinePlayer owner = Bukkit.getOfflinePlayer(UUID.fromString(this.owner_uuid));
		if(owner.getName() != null) {
			return owner.getName();
		}
		else {
			return "???";
		}
	}
	
	public String getOwner() {
		return this.owner_uuid;
	}
	
	public String getName() {
		return this.getOwnerName() + "'s Shop";
	}
	
	public short getQuantity() {
		return this.quantity;
	}
	
	public void setQuantity(short quantity) {
		this.quantity = quantity;
	}
	
	public int getPrice() {
		return this.price;
	}
	
	public void setPrice(int price) {
		this.price = price;
	}
	
	public Location getLocation() {
		World shopWorld = Bukkit.getServer().getWorld(this.world_name);
		Location shopLocation = new Location(shopWorld, (double) this.loc_x, (double) this.loc_y, (double) this.loc_z);
		return shopLocation;
	}
	
	public int getXLocation() {
		return this.loc_x;
	}
	
	public int getYLocation() {
		return this.loc_y;
	}
	
	public int getZLocation() {
		return this.loc_z;
	}
	
	public String getWorld() {
		return this.world_name;
	}
	
	public boolean playerOwnsShop(Player player) {
		return player.getUniqueId().toString().compareTo(this.owner_uuid) == 0;
	}
	
	public boolean equals(Shop shop) {
		return this.world_name.compareTo(shop.getWorld()) == 0 && this.loc_x == shop.getXLocation() && this.loc_y == shop.getYLocation() && this.loc_z == shop.getZLocation();
	}
}