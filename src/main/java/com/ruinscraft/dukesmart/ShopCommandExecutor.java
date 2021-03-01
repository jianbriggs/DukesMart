package com.ruinscraft.dukesmart;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ShopCommandExecutor implements CommandExecutor, TabCompleter{
	private final DukesMart plugin;
	
	private final String PLUGIN_BANNER = "" + ChatColor.GOLD + "----------------[ DukesMart ]----------------";
	private final String MSG_ERROR_NO_PERMISSION = "" + ChatColor.RED + "You do not have permission to use that command.";
	private final String MSG_ERROR_LEDGER_NOT_ENOUGH_MONEY = "" + ChatColor.RED + "You do not have that much in your ledger.";
	private final String MSG_ERROR_LEDGER_INVALID_WITHDRAW = "" + ChatColor.RED + "Invalid withdraw amount!";
	private final String MSG_LEDGER_WITHDRAW_AMOUNT = "" + ChatColor.AQUA + "You redeemed $%d from your ledger.";
	private final String MSG_LEDGER_PRINT_BALANCE = "" + ChatColor.AQUA + "Your balance is $%d";
	private final String MSG_ADMIN_PRINT_PLAYER_BALANCE = "" + ChatColor.AQUA + "%s's balance is $%d";
	private final String MSG_ADMIN_PLAYER_NO_LEDGER = "" + ChatColor.AQUA + "%s does not have a ledger";
	private final String MSG_ERROR_ADMIN_PLAYER_NOT_EXIST = "" + ChatColor.RED + "%s has not played before, or does not exist";
	private final String MSG_ERROR_ADMIN_NO_SHOP_SELECTED = "" + ChatColor.RED + "You need to select a shop before running this command";
	
	private final List<String> tabOptions;
	private final List<String> adminTabOptions;

	private Map<Player, Long> recentWithdraws;
	
	public ShopCommandExecutor(DukesMart plugin) {
		this.plugin = plugin;
		this.tabOptions = new ArrayList<String>();
		this.adminTabOptions = new ArrayList<String>();
		this.recentWithdraws = new HashMap<>();
		
		tabOptions.add("withdraw");
		tabOptions.add("balance");
		tabOptions.add("top");
		
		adminTabOptions.add("view");
		adminTabOptions.add("history");
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
					if (recentWithdraws.containsKey(player)) {
						long lastWithdrawTime = recentWithdraws.get(player);
						long currentTime = System.currentTimeMillis();

						if (lastWithdrawTime + TimeUnit.SECONDS.toMillis(60) > currentTime) {
							player.sendMessage(ChatColor.RED + "Please wait some time before withdrawing again.");
							return false;
						}
					}

					boolean success = false;

					if(args.length >= 2) {
						int amount = safeStringToInt(args[1]);
						if(amount > 0)
						{
							success = true;
							withdrawMoney(player, amount);
						}
						else {
							player.sendMessage(this.MSG_ERROR_LEDGER_INVALID_WITHDRAW);
						}
					}
					else {
						success = true;
						withdrawAllMoney(player);
					}

					if (success) {
						recentWithdraws.put(player, System.currentTimeMillis());
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
				case "top":
					viewTopTen(player);
					break;
				case "view":
				case "v":
					if(player.hasPermission("dukesmart.shop.admin")) {
						Shop selectedShop = this.plugin.getSelectedShopController().getSelection(player);
						if(selectedShop == null) {
							player.sendMessage(this.MSG_ERROR_ADMIN_NO_SHOP_SELECTED);
						}
						else {
							if(args.length >= 2 && args[1].compareToIgnoreCase("recent") == 0) {
								adminViewRecentTransactions(player, selectedShop);
							}
						}
					}
					else {
						player.sendMessage(this.MSG_ERROR_NO_PERMISSION);
					}
					break;
				case "history":
				case "his":
					if(player.hasPermission("dukesmart.shop.admin")) {
						if(args.length >= 2) {
							String playerName = "";
							if(args[1].length() > 16) {
								playerName = args[1].substring(0, 16);
							}
							else {
								playerName = args[1];
							}
							adminCheckPlayerHistory(player, playerName);
						}
					}
					else {
						player.sendMessage(this.MSG_ERROR_NO_PERMISSION);
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
	

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            for (String option : tabOptions) {
                if (option.startsWith(args[0].toLowerCase())) {
                    completions.add(option);
                }
            }
            
            if(sender.hasPermission("dukesmart.shop.admin")) {
            	for (String option : adminTabOptions) {
                    if (option.startsWith(args[0].toLowerCase())) {
                        completions.add(option);
                    }
                }
            }
        }
        
        if (args.length == 2) {
            if(args[0].equalsIgnoreCase("view") && sender.hasPermission("dukesmart.shop.admin")) {
            	completions.add("recent");
            }
            else if( (args[0].equalsIgnoreCase("history") || args[0].equalsIgnoreCase("balance") )&& sender.hasPermission("dukesmart.shop.admin")) {
            	for(Player p : Bukkit.getOnlinePlayers()) {
            		completions.add(p.getName());
            	}
            }
        }

        return completions;
	}
	
	private void showHelp(Player player) {
		String[] commandHelpBase = {
			ChatColor.DARK_AQUA + "  /shop" + ChatColor.AQUA + " withdraw ($)" + ChatColor.GRAY + ": Removes money from your ledger",
			ChatColor.DARK_AQUA + "  /shop" + ChatColor.AQUA + " balance" + ChatColor.GRAY + ": Check your ledger balance",
			ChatColor.DARK_AQUA + "  /shop" + ChatColor.AQUA + " top" + ChatColor.GRAY + ": View top 10 earners"
		};
		
		String[] commandHelpAdmin = {
		    ChatColor.DARK_AQUA + "  /shop" + ChatColor.AQUA + " view recent" + ChatColor.GRAY + ": View ten most recent transactions for a shop",
		    ChatColor.DARK_AQUA + "  /shop" + ChatColor.AQUA + " history (player)" + ChatColor.GRAY + ": View ten most recent transactions made by a player",
		    ChatColor.DARK_AQUA + "  /shop" + ChatColor.AQUA + " balance (player)" + ChatColor.GRAY + ": Check a player's ledger balance"    
		};
			
		if(player.isOnline()) {
			player.sendMessage(this.PLUGIN_BANNER);
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
		this.plugin.getMySQLHelper().getPlayerIncome(player).thenAccept(result -> {
        	withdrawMoney(player, result.getIncome());
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
					
					ItemStack redeemedCurrency = this.plugin.SHOP_CURRENCY_XMATERIAL.parseItem();
					
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
					this.plugin.getNotifyPlayerController().removeTask(player);
				}
				else if(result < 0){
					player.sendMessage(this.MSG_ERROR_LEDGER_NOT_ENOUGH_MONEY);
				}
			});			
		}
	}
	
	private void checkBalance(Player player) {
		this.plugin.getMySQLHelper().getPlayerIncome(player).thenAccept(result -> {
        	if(player.isOnline()) {
        		player.sendMessage(String.format(this.MSG_LEDGER_PRINT_BALANCE, result.getIncome()));
        		this.plugin.getNotifyPlayerController().removeTask(player);
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

	@SuppressWarnings("deprecation")
	private void adminCheckPlayerHistory(Player caller, String playerName) {
		OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
		
		if(player.hasPlayedBefore()) {
			this.plugin.getMySQLHelper().viewPlayerHistory(player.getUniqueId()).thenAccept(transactions -> {
				if(caller.isOnline()) {
					caller.sendMessage(PLUGIN_BANNER);
					caller.sendMessage(ChatColor.AQUA + "Viewing ten recent transactions for player " + ChatColor.DARK_AQUA + playerName);
					caller.sendMessage(" ");
					byte i = 1;
					for(String t : transactions) {
						caller.sendMessage(i + ". " + t);
						i++;
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
	
	private void adminViewRecentTransactions(Player caller, Shop shop) {
		this.plugin.getMySQLHelper().viewRecentTransactions(shop).thenAccept(transactions -> {
			if(caller.isOnline()) {
				caller.sendMessage(PLUGIN_BANNER);
				caller.sendMessage(ChatColor.AQUA + "Viewing ten recent transactions for selected shop");
				caller.sendMessage(" ");
				byte i = 1;
				for(String t : transactions) {
					caller.sendMessage(i + ". " + t);
					i++;
				}
			}
		});
	}
	private void viewTopTen(Player caller) {
		this.plugin.getMySQLHelper().viewTopTenEarners().thenAccept(players -> {
			if(caller.isOnline()) {
				caller.sendMessage(PLUGIN_BANNER);
				caller.sendMessage(ChatColor.AQUA + "Viewing top ten highest-earning players");
				caller.sendMessage(" ");
				byte i = 1;
				for(String player : players) {
					caller.sendMessage(i + ". " + player);
					i++;
				}
			}
		});
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