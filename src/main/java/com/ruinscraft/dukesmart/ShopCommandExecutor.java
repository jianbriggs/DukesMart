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
				case "withdraw":
					if(args.length >= 2) {
						if(stringIsNumeric(args[1])) {
							int amount = safeStringToInt(args[1]);
							withdrawMoney(player, amount);
						}
						else {
							player.sendMessage(ChatColor.RED + "Invalid withdraw amount!");
						}
					}
					else {
						withdrawAllMoney(player);
					}
					break;
				case "balance":
				case "bal":
					checkBalance(player);
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
		String[] commandHelpBase = {
			ChatColor.GOLD + "----------------[ DukesMart ]----------------",
			ChatColor.DARK_AQUA + "  /shop" + ChatColor.AQUA + " withdraw ($)" + ChatColor.GRAY + ": Removes money from your ledger",
			ChatColor.DARK_AQUA + "  /shop" + ChatColor.AQUA + " balance" + ChatColor.GRAY + ": Check your ledger balance"
		};
		
		String[] commandHelpAdmin = {
		    ChatColor.DARK_AQUA + "  /shop" + ChatColor.AQUA + " view [recent|oldest]" + ChatColor.GRAY + ": View transactions for a shop",
		    ChatColor.DARK_AQUA + "  /shop" + ChatColor.AQUA + "(player) balance" + ChatColor.GRAY + ": Check a player's ledger balance"
		};
			
		if(player.isOnline()) {
			player.sendMessage(commandHelpBase);
			
			if(player.hasPermission("dukesmart.shop.admin")) {
				player.sendMessage(commandHelpAdmin);
			}
		}
	}
	
	/* old method */
	/*
	private void withdrawMoney(Player player) {
		if(player.isOnline()) {		
			this.plugin.getMySQLHelper().getPlayerIncomeToRedeem(player).thenAccept(ledgerIncome -> {
				if(ledgerIncome > 0) {
					this.plugin.getMySQLHelper().playerRedeemLedgerIncome(player, ledgerIncome).thenAccept(result -> {
						if(result) {
							PlayerInventory inventory = player.getInventory();
							
							ItemStack redeemedCurrency = plugin.SHOP_CURRENCY_XMATERIAL.parseItem();
							
							Bukkit.getScheduler().runTask(this.plugin, () -> {
								int stackSize    = redeemedCurrency.getMaxStackSize();
								int incomeStacks = ledgerIncome / stackSize;
								int remainder    = ledgerIncome - (stackSize * incomeStacks);
								
								while(incomeStacks > 0) {
									redeemedCurrency.setAmount(64);
									HashMap<Integer, ItemStack> remain = inventory.addItem(redeemedCurrency);
									if(!remain.isEmpty()) {
										player.getWorld().dropItem(player.getLocation(), remain.get(0));
									}
									incomeStacks--;
								}
								
								if(remainder > 0) {
									redeemedCurrency.setAmount(remainder);
									HashMap<Integer, ItemStack> remain = inventory.addItem(redeemedCurrency);
									if(!remain.isEmpty()) {
										player.getWorld().dropItem(player.getLocation(), remain.get(0));
									}
								}
							});			
							
							player.sendMessage(ChatColor.AQUA + "You redeemed " + ledgerIncome + " gold from your ledger.");
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
	*/
	
	/**
	 * Withdraws all money from a player's ledger
	 * @param player
	 */
	private void withdrawAllMoney(Player player) {
		this.plugin.getMySQLHelper().getPlayerIncome(player).thenAccept(income -> {
        	withdrawMoney(player, income);
        });
	}
	
	/**
	 * Withdraws a specified amount of money from a player's ledger
	 * @param player
	 * @param amount
	 */
	private void withdrawMoney(Player player, int amount) {
		if(player.isOnline()) {
			player.sendMessage("(Debug) New withdraw method");
			this.plugin.getMySQLHelper().playerRedeemLedgerIncome(player, amount).thenAccept(result -> {
				if(result > 0) {
					PlayerInventory inventory = player.getInventory();
					
					ItemStack redeemedCurrency = plugin.SHOP_CURRENCY_XMATERIAL.parseItem();
					
					Bukkit.getScheduler().runTask(this.plugin, () -> {
						int stackSize    = redeemedCurrency.getMaxStackSize();
						int incomeStacks = amount / stackSize;
						int remainder    = amount - (stackSize * incomeStacks);
						
						while(incomeStacks > 0) {
							redeemedCurrency.setAmount(64);
							HashMap<Integer, ItemStack> remain = inventory.addItem(redeemedCurrency);
							if(!remain.isEmpty()) {
								player.getWorld().dropItem(player.getLocation(), remain.get(0));
							}
							incomeStacks--;
						}
						
						if(remainder > 0) {
							redeemedCurrency.setAmount(remainder);
							HashMap<Integer, ItemStack> remain = inventory.addItem(redeemedCurrency);
							if(!remain.isEmpty()) {
								player.getWorld().dropItem(player.getLocation(), remain.get(0));
							}
						}
					});			
					
					player.sendMessage(ChatColor.AQUA + "You redeemed $" + amount + " from your ledger.");
				}
				else if(result < 0){
					player.sendMessage(ChatColor.RED + "You do not have that much in your ledger.");
				}
			});			
		}
	}
	
	private void checkBalance(Player player) {
		this.plugin.getMySQLHelper().getPlayerIncome(player).thenAccept(balance -> {
        	if(player.isOnline()) {
        		player.sendMessage(ChatColor.AQUA + "Your balance is $" + balance);
        	}
        });
	}
	
	private boolean stringIsNumeric(String str) {
		for(char c : str.toCharArray()) {
			if(!Character.isDigit(c)) {
				return false;
			}
		}
		
		return true;
	}
	
	private int safeStringToInt(String str) {
		if(stringIsNumeric(str)) {
			if(str.length() > 10) {
				str = str.substring(0, 10);
			}

			if(Double.parseDouble(str) > Integer.MAX_VALUE) {
				return Integer.MAX_VALUE - 1;
			}
			else {
				return Integer.parseInt(str);
			}
		}
		return -1;
	}
}