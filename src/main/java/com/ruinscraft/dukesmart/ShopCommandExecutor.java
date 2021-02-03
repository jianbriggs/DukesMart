package com.ruinscraft.dukesmart;

import java.util.HashMap;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import net.md_5.bungee.api.ChatColor;

public class ShopCommandExecutor implements CommandExecutor{
	private final DukesMart plugin;
	
	private String[] commandHelp =
	{
		ChatColor.GOLD + "----------------[ DukesMart ]----------------",
		ChatColor.DARK_AQUA + "  /shop" + ChatColor.AQUA + " redeem " + ChatColor.GRAY + ": Redeem your gold",
		ChatColor.DARK_AQUA + "  /shop" + ChatColor.AQUA + " edit [name/price/amount] [value]" + ChatColor.GRAY + ": Edit your shop's attributes. You must"
				+ "have a shop selected first."
	};
	
	public ShopCommandExecutor(DukesMart plugin) {
		this.plugin = plugin;
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(sender instanceof Player) {
			Player player = (Player) sender;
			
			player.sendMessage("(Command) You issued /shop");
			
			if(args.length >= 1) {
				String subcommand = args[0];
				
				if(subcommand.equalsIgnoreCase("redeem")) {
					return redeemGold(player);
				}
			}
			else {
				if(player.isOnline()) {
					player.sendMessage(commandHelp);
					return true;
				}
			}
		}
		return false;
	}
	
	private boolean redeemGold(Player player) {
		if(player.isOnline()) {		
			this.plugin.getMySQLHelper().getPlayerIncomeToRedeem(player).thenAccept(amount -> {
				if(amount > 0) {
					this.plugin.getMySQLHelper().playerRedeemGold(player, amount).thenAccept(result -> {
						if(result) {
							PlayerInventory inventory = player.getInventory();
							
							ItemStack redeemedGold = XMaterial.GOLD_INGOT.parseItem();
							
							Bukkit.getScheduler().runTask(this.plugin, () -> {
								int goldStacks = amount / 64;
								int remainder  = amount - (64 * goldStacks);
								
								while(goldStacks > 0) {
									redeemedGold.setAmount(64);
									HashMap<Integer, ItemStack> remain = inventory.addItem(redeemedGold);
									if(!remain.isEmpty()) {
										player.getWorld().dropItem(player.getLocation(), remain.get(0));
									}
									goldStacks--;
								}
								
								if(remainder > 0) {
									redeemedGold.setAmount(remainder);
									HashMap<Integer, ItemStack> remain = inventory.addItem(redeemedGold);
									if(!remain.isEmpty()) {
										player.getWorld().dropItem(player.getLocation(), remain.get(0));
									}
								}
							});			
							
							player.sendMessage(ChatColor.AQUA + "You redeemed " + amount + " gold from your ledger.");
						}
						else {
							player.sendMessage(ChatColor.RED + "Unable to redeem gold. Please try again later.");
						}
					});
					
				}
				else {
					player.sendMessage(ChatColor.RED + "You have no gold to redeem.");
				}
	
			});
			return true;
		}
		
		return false;
	}
}
