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
		if(!(sender instanceof Player)) {
			return false;
		}
		
		Player player = (Player) sender;
		
		if(args.length >= 1) {
			switch(args[0].toLowerCase()){
				case "redeem":
					redeemGold(player);
					break;
				default:
					showHelp(player);
					break;
			}
		}
		else {
			showHelp(player);
		}
		
		return true;
	}
	
	private void showHelp(Player player) {
		if(player.isOnline()) {
			player.sendMessage(commandHelp);
		}
	}
	private void redeemGold(Player player) {
		if(player.isOnline()) {		
			this.plugin.getMySQLHelper().getPlayerIncomeToRedeem(player).thenAccept(amount -> {
				if(amount > 0) {
					this.plugin.getMySQLHelper().playerRedeemGold(player, amount).thenAccept(result -> {
						if(result) {
							PlayerInventory inventory = player.getInventory();
							
							ItemStack redeemedCurrency = plugin.SHOP_CURRENCY_XMATERIAL.parseItem();
							
							Bukkit.getScheduler().runTask(this.plugin, () -> {
								int goldStacks = amount / 64;
								int remainder  = amount - (64 * goldStacks);
								
								while(goldStacks > 0) {
									redeemedCurrency.setAmount(64);
									HashMap<Integer, ItemStack> remain = inventory.addItem(redeemedCurrency);
									if(!remain.isEmpty()) {
										player.getWorld().dropItem(player.getLocation(), remain.get(0));
									}
									goldStacks--;
								}
								
								if(remainder > 0) {
									redeemedCurrency.setAmount(remainder);
									HashMap<Integer, ItemStack> remain = inventory.addItem(redeemedCurrency);
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
		}
	}
}