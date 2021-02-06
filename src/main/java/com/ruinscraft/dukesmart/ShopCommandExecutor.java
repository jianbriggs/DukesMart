package com.ruinscraft.dukesmart;

import java.util.HashMap;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import net.md_5.bungee.api.ChatColor;

public class ShopCommandExecutor implements CommandExecutor{
	private final DukesMart plugin;
	
	private final String MSG_ERROR_NO_PERMISSION = "" + ChatColor.RED + "You do not have permission to use that command.";
	private final String MSG_ERROR_LEDGER_NOT_ENOUGH_MONEY = "" + ChatColor.RED + "You do not have that much in your ledger.";
	private final String MSG_ERROR_LEDGER_INVALID_WITHDRAW = "" + ChatColor.RED + "Invalid withdraw amount!";
	private final String MSG_LEDGER_WITHDRAW_AMOUNT = "" + ChatColor.AQUA + "You redeemed $%d from your ledger.";
	private final String MSG_LEDGER_PRINT_BALANCE = "" + ChatColor.AQUA + "Your balance is $%d";
	private final String MSG_ADMIN_PRINT_PLAYER_BALANCE = "" + ChatColor.AQUA + "%s's balance is $%d";
	private final String MSG_ADMIN_PLAYER_NO_LEDGER = "" + ChatColor.AQUA + "%s does not have a ledger";
	private final String MSG_ERROR_ADMIN_PLAYER_NOT_EXIST = "" + ChatColor.RED + "%s has not played before, or does not exist";
	
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
				case "with":
					if(args.length >= 2) {
						int amount = safeStringToInt(args[1]);
						if(amount > 0)
						{
							withdrawMoney(player, amount);
						}
						else {
							player.sendMessage(this.MSG_ERROR_LEDGER_INVALID_WITHDRAW);
						}
					}
					else {
						withdrawAllMoney(player);
					}
					break;
				case "balance":
				case "bal":
					if(args.length >= 2) {
						if(player.hasPermission("dukesmart.shop.admin")) {
							String playerName = "";
							if(args[1].length() > 16) {
								playerName = args[1].substring(0, 16);
							}
							else {
								playerName = args[1];
							}
							adminCheckPlayerBalance(player, playerName);
							//layer.sendMessage("Player balance lookup placeholder");
						}
						else {
							player.sendMessage(this.MSG_ERROR_NO_PERMISSION);
						}
					}
					else {
						checkBalance(player);
					}
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
		    ChatColor.DARK_AQUA + "  /shop" + ChatColor.AQUA + " balance (player)" + ChatColor.GRAY + ": Check a player's ledger balance"
		};
			
		if(player.isOnline()) {
			player.sendMessage(commandHelpBase);
			
			if(player.hasPermission("dukesmart.shop.admin")) {
				player.sendMessage(commandHelpAdmin);
			}
		}
	}

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
					
					player.sendMessage(String.format(this.MSG_LEDGER_WITHDRAW_AMOUNT, amount));
				}
				else if(result < 0){
					player.sendMessage(this.MSG_ERROR_LEDGER_NOT_ENOUGH_MONEY);
				}
			});			
		}
	}
	
	private void checkBalance(Player player) {
		this.plugin.getMySQLHelper().getPlayerIncome(player).thenAccept(balance -> {
        	if(player.isOnline()) {
        		player.sendMessage(String.format(this.MSG_LEDGER_PRINT_BALANCE, balance));
        	}
        });
	}
	
	@SuppressWarnings("deprecation")
	private void adminCheckPlayerBalance(Player caller, String playerName) {
		OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
		
		if(player.hasPlayedBefore()) {
			this.plugin.getMySQLHelper().getPlayerIncome(player.getUniqueId()).thenAccept(balance -> {
				if(caller.isOnline()) {
					if(balance > 0) {
						caller.sendMessage(String.format(this.MSG_ADMIN_PRINT_PLAYER_BALANCE, playerName, balance));
					}
					else {
						caller.sendMessage(String.format(this.MSG_ADMIN_PLAYER_NO_LEDGER, playerName));
					}
				}
	        });
		}
		else {
			if(caller.isOnline()) {
				caller.sendMessage(String.format(this.MSG_ERROR_ADMIN_PLAYER_NOT_EXIST, playerName));
			}
		}
	}
	
	/**
	 * Checks if a given string is a number
	 * 
	 * @param str - String to check
	 * @return True if the string consists of only numbers, False otherwise
	 */
	private boolean stringIsNumeric(String str) {
		for(char c : str.toCharArray()) {
			if(!Character.isDigit(c)) {
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * Safely converts a string consisting of numeric values
	 * into an integer. If the value of the number is greater than
	 * an integer's max value, it will truncate the value to it.
	 * 
	 * @param str - String to check for numeric value
	 * @return int value of the string, or -1 on error
	 */
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