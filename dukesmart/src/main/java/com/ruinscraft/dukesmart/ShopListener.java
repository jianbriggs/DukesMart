package com.ruinscraft.dukesmart;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.Sign;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import net.md_5.bungee.api.ChatColor;

public class ShopListener implements Listener{
	
	private final String SHOP_SIGN_NO_ITEM = "" + ChatColor.WHITE + "?";
	
	private DukesMart plugin;
	
	private HashMap<String, Location> signSelectedMap = new HashMap<String, Location>();
	
    public ShopListener(DukesMart plugin) {
    	this.plugin = plugin;
    }
    
	public HashMap<String, Location> getSelectedMap(){
		return this.signSelectedMap;
	}
	
    @EventHandler
    /*
     * Whenever a player joins the server, they should be greeted
     * with a message saying if any of the player's shops made
     * any income (gold ingots). If The player has made money
     * since last login, it will print the amount and suggest
     * to the player to run the /redeem command to retrieve the
     * gold.
     */
    public void onPlayerJoin(PlayerJoinEvent evt) {
        Player player = evt.getPlayer(); // The player who joined
        
        this.plugin.getMySQLHelper().getPlayerIncomeToRedeem(player).thenAccept(income -> {
        	if( income > 0  && player.isOnline()) {
	            player.sendMessage(ChatColor.YELLOW + "Since last login, you made " + ChatColor.GOLD + income + ChatColor.YELLOW
	            				   + " gold." + ChatColor.GOLD + " /shop redeem");
        	}
        });

    }
    
    /*
     * This will handle shop creation if a player
     * places a sign with proper values.
     */
    @EventHandler
    public void onSignChangeEvent(SignChangeEvent evt) {
    	Player player = evt.getPlayer();
    	Block b = evt.getBlock();
    	
    	if(b.getState() instanceof Sign) {
    		
    		if(shopIsValid(evt.getLines())) {
    			
    		}
	    	if(evt.getLine(0).equalsIgnoreCase("[Buy]") && validateShopPrice(evt.getLine(2))) {

	    		evt.setLine(0, ChatColor.DARK_PURPLE + "[Buy]");
    			evt.setLine(1, ChatColor.WHITE + "?");
	    		evt.setLine(3, ChatColor.DARK_BLUE + player.getName());

	    		player.sendMessage(ChatColor.AQUA + "Sign shop created! Now right-click the sign with an item to assign it.");
	    	}
    	}
    }
    
    /*
     * This event may end up unused.
     */
    @EventHandler
    public void onBlockPlaceEvent(BlockPlaceEvent evt) {
    	Player player = evt.getPlayer();
    	Block  block  = evt.getBlock();
    	
    	if(block.getState() instanceof Sign) {
    		//player.sendMessage(player.getName() + " placed a sign.");
    	}
    }
    
    //TODO: refactor the shit out of this
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent evt) {
        Player player = evt.getPlayer();
        String playerUID = player.getUniqueId().toString();
        
        if (evt.getAction() == Action.RIGHT_CLICK_BLOCK) {
        	Block clickedBlock = evt.getClickedBlock();
        	
            if ( clickedBlock.getType() == Material.WALL_SIGN || clickedBlock.getType() == Material.SIGN){
                Sign sign = (Sign) clickedBlock.getState();
                Block block = clickedBlock.getRelative(BlockFace.DOWN, 1);
                
                if(block.getState() instanceof Chest || block.getState() instanceof DoubleChest) {
                	Chest chest = (Chest) block.getState();
                	
                	// get store information from sign
                	String[] signLines = sign.getLines();

                	if(signIsShop(sign)) {        		
                		/* 
                		 * if the shop has a white '?' on the 2nd line,
                		 * that means it has not been set an item/block
                		 * to sell.
                		 */
                		if(shopSignHasNoItem(sign)) {

                			ItemStack itemToSell = player.getInventory().getItemInMainHand();
                			XMaterial itemMat = XMaterial.matchXMaterial(itemToSell);
                			
                			// if player has something in hand (and is owner) set the shop's item
                			if(!(itemToSell.getType() == Material.AIR) && playerIsOwner(player, signLines[3])) {
	                			sign.setLine(1, itemMat.name());
	                			sign.update();
	                			
	                			this.plugin.getMySQLHelper().setupLedger(player).thenAccept(result -> {
	                				if(result) {
	                					this.plugin.getMySQLHelper().registerShop(player, sign, itemToSell).thenAccept(callback -> {
	    	                				player.sendMessage(ChatColor.AQUA + "Shop item set. Place items to sell in chest below sign.");
	    	                			});	
	                				}
	                			});

                			}
                		}
                		else{
                			Location shopLocation = getShopLocation(sign);
                			Location playerSelected = this.signSelectedMap.get(playerUID);
                			
                			// On right-click the player "selects" the shop.
                			// In the map, store location of the shop the player has selected.
                			if( playerSelected == null || !playerSelected.equals(shopLocation)) {
                				this.signSelectedMap.put(playerUID, shopLocation);
                				player.sendMessage(ChatColor.GREEN + " Shop selected.");
                				
                				this.plugin.getMySQLHelper().getShopFromLocation(shopLocation).thenAccept(result ->{
                					//if(result != null && player.isOnline()) {
                					player.sendMessage("(Debug) Shop gotten from location");
                					
                					Bukkit.getScheduler().runTask(this.plugin, () -> {
                						displayShopInformation(player, result);
                					});
                					//}
                				});			
                			}
                			else if( playerSelected.equals(shopLocation)){
			                	// get the material to buy
			                	// note that this will eventually be replaced with an ItemStack
			                	// from the DB
			                	this.plugin.getMySQLHelper().getShopFromLocation(shopLocation).thenAccept(shop -> {
			                		ItemStack itemToBuy = shop.getItem();
			                		//itemToBuy.setAmount(shop.getQuantity());
			                		Inventory storeStock = chest.getInventory();
			                		//itemToBuy.setAmount(shop.getQuantity());
			                		
			                		// check if the chest inventory contains the item
			                		boolean hasStock = false;
				                		
			                		if(storeStock instanceof DoubleChestInventory) {
			                			DoubleChestInventory dci = (DoubleChestInventory) storeStock;
			                			if(dci.getLeftSide().containsAtLeast(itemToBuy, shop.getQuantity())) {
			                				storeStock = dci.getLeftSide();
			                				hasStock = true;
			                			}
			                			else if(dci.getRightSide().containsAtLeast(itemToBuy, shop.getQuantity())) {
			                				storeStock = dci.getRightSide();
			                				hasStock = true;
			                			}
			                		}
			                		else{
			                			if(storeStock.containsAtLeast(itemToBuy, shop.getQuantity())) {
			                				hasStock = true;
			                			}
			                		}
				                		
			                		if(hasStock) {   			
			                			if(playerCanStoreItem(player, itemToBuy, shop.getQuantity())) {
			                				player.updateInventory();
			                				PlayerInventory pi = player.getInventory();
			                				
				                			if(pi.containsAtLeast(new ItemStack(Material.GOLD_INGOT), shop.getPrice())){
					                			itemToBuy.setAmount(shop.getQuantity());
					                			
					                			// remove said items from the chest
					                			storeStock.removeItem(itemToBuy);
					                			
					                			pi.removeItem(new ItemStack(Material.GOLD_INGOT, shop.getPrice()));
					                			// and put into the player's inventory
					                			pi.addItem(itemToBuy);
					                			
					                			this.plugin.getMySQLHelper().processTransaction(player, shop).thenAccept(result -> {
						                			if(player.isOnline()) {
						                				player.sendMessage("You purchased " + ChatColor.AQUA + shop.getQuantity() + " "
						                						+ itemToBuy.getType() + ChatColor.WHITE + " for " + ChatColor.GOLD + shop.getPrice() + " gold.");
						                			}		
					                			});
				                			}
				                			else {
				                				sendError(player, "Sorry, you do not have enough gold to buy.");
				                			}
			                			}
			                			else {
			                				sendError(player, "You do not have enough free space for this purchase.");
			                			}
			                		}
			                		else {
			                			sendError(player, "Sorry, this shop is out of stock. Come back later.");
			                		}
			                	});
	                		}
		                }
                	}
                }
            }
        }
    }
        
	private boolean playerCanStoreItem(Player player, ItemStack itemToBuy, short quantity) {
		PlayerInventory inv = player.getInventory();
		int maxStackSize = itemToBuy.getMaxStackSize();

		for(ItemStack item : inv.getContents()) {
			if(item == null || item.getType() == Material.AIR) {
				continue;
			}
			// check if the slot's amount + quantity is
			// less than or equal to 64 (full stack)
			else if(item.isSimilar(itemToBuy) && item.getAmount() + quantity <= maxStackSize) {
				return true;
			}
		}
		
		// otherwise, return if there's a free, empty slot
		return inv.firstEmpty() >= 0;
	}

	@EventHandler
    public void onBlockBreak(BlockBreakEvent evt) {
    	Player player = evt.getPlayer();
    	Block block = evt.getBlock();
    	
    	if(block.getType() == Material.WALL_SIGN || block.getType() == Material.SIGN) {
    		Sign s = (Sign) block.getState();
    		
    		if(signIsShop(s)) {
    			evt.setCancelled(true);
				Location shopLocation = s.getLocation();
				this.plugin.getMySQLHelper().getShopFromLocation(shopLocation).thenAccept(shop -> {
					if(shop != null) {
						// If player is the owner, delete it.
						if(playerIsOwner(player, shop)) {
							player.sendMessage(ChatColor.GREEN + "You are the owner of this shop.");		
						}
						else {
							player.sendMessage(ChatColor.RED + "You cannot remove a shop that you do not own.");	
						}
					}
				});
    		}
    		else {
    			return;
    		}
    	}
    }
    
    private boolean shopSignHasNoItem(Sign sign) {
    	return sign.getLine(1).equals(SHOP_SIGN_NO_ITEM);
    }
    
    /**
     * Checks if a player is the owner of a specific shop
     * by comparing UUIDs.
     * 
     * @param player Player interacting with shop
     * @param shop Shop to check against
     * @return True if Player and Shop owner UUIDs match, False otherwise
     */
    private boolean playerIsOwner(Player player, Shop shop) {
    	return player.getUniqueId().toString().compareTo(shop.getOwner()) == 0;
    }
    

	/**
     * Displays a Scoreboard containing information related to
     * the shop the player has selected.
     * @param shopLocation Location of the selected shop sign
     */
    private void displayShopInformation(Player player, Shop shop) {
    	Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
    	Objective obj = board.registerNewObjective("DukesMart", "Shop", pluginName());
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        
        ItemStack item = shop.getItem();
        Map<Enchantment, Integer> itemEnchantments = item.getEnchantments();
        ItemMeta  meta = item.getItemMeta();
        
        ArrayList<String> shopInfoElements = new ArrayList<String>();
        shopInfoElements.add(shop.getName());
        shopInfoElements.add("" + ChatColor.DARK_GREEN + ChatColor.BOLD + "For sale");
        if(meta.hasDisplayName()) {
        	shopInfoElements.add("" + ChatColor.GOLD + ChatColor.ITALIC + "\"" + meta.getDisplayName() + "\"");
        }
        shopInfoElements.add("" + item.getType().name());
        for(Entry<Enchantment, Integer> entry : itemEnchantments.entrySet()) {
        	String enchantmentName = entry.getKey().getKey().toString().split(":")[1];
        	enchantmentName = enchantmentName.substring(0,1).toUpperCase() + enchantmentName.substring(1);
        	
        	shopInfoElements.add(" - " + ChatColor.ITALIC + enchantmentName + " " + entry.getValue());
        }
        shopInfoElements.add(" ");
        shopInfoElements.add("" + ChatColor.DARK_GREEN + ChatColor.BOLD + "Quantity: " + ChatColor.WHITE + shop.getQuantity());
        shopInfoElements.add(" ");
        shopInfoElements.add("" + ChatColor.DARK_GREEN + ChatColor.BOLD + "Cost....: " + ChatColor.WHITE + shop.getPrice() + " gold");
        shopInfoElements.add(" ");
        shopInfoElements.add("" + ChatColor.YELLOW + "To purchase, right click again.");

        int counter = shopInfoElements.size();
        for(String line : shopInfoElements) {
        	obj.getScore(shopGuiPad(line)).setScore(counter);
        	counter--;
        }

        player.setScoreboard(board);
        player.sendMessage("(Debug) Scoreboard display set");
	}
    
    private String shopGuiPad(String message) {
    	message += ChatColor.RESET;
    	
    	while(message.length() < 32) {
    		message += " ";
    	}
    	
    	return message;
    }
	/**
     * Returns the Location data of a shop sign
     * @param s Sign representing a shop
     * @return Sign location data
     */
    private Location getShopLocation(Sign s) {
		return s.getLocation();
	}

	/*
     * Helper methods below this comment.
     */
    
    /**
     * Returns a formatted string representing the plugin name (DukesMart).
     * Make sure to reset the chat color AFTER this method is called.
     */
    private String pluginName() {
    	return ChatColor.DARK_GREEN + "DukesMart";
    }
    
    /**
     * Sends an error message to the player.
     * @param p Player to send message to
     * @param message Message to output
     */
    private void sendError(Player player, String message) {
    	if(player.isOnline()) {
    		player.sendMessage(ChatColor.RED + message);
    	}
    }
    
    /**
     * Validates a shop sign's price tag to make
     * sure a player entered the format correctly.
     * 
     * Format is {quantity} " for " ${price}
     * 
     * @param line String to validate (should be 3rd line)
     * @return True if valid, False if not
     */
    private boolean validateShopPrice(String line) {
    	String[] tokens = line.split(" ");
    	if(tokens.length == 3) {
	    	// check the first token contains only integers
	    	if(!tokens[0].isBlank()) {
	    		
	    		for(char c : tokens[0].toCharArray()) {
	    			if(!Character.isDigit(c)) {
	    				return false;
	    			}
	    		}
	    		
	    		int quantity_int = Integer.parseInt(tokens[0]);
	    		
	    		if(quantity_int < 1 || quantity_int > 64) {
	    			return false;
	    		}
	    	}
	    	// then, check if the word "for" is present
	    	if(!tokens[1].isBlank() && !tokens[1].equalsIgnoreCase("for")) {
	    		return false;
	    	}
	    	
	    	// finally, check the format of the price tag
	    	// first char should be '$', rest should be digit
	    	if(!tokens[2].isBlank()) {
	    		String price = tokens[2];
	    		if(price.length() > 1) {
		    		if(price.charAt(0) != '$') {
		    			return false;
		    		}
		    		
		    		int price_int = Integer.parseInt(price.substring(1));
		    		
		    		if(price_int < 0 || price_int > 1000000) {
		    			return false;
		    		}
	    		}
	    		else {
	    			return false;
	    		}
	    	}
	    	
	    	// everything appears to check out!
	    	return true;
    	}
    	else {
    		return false;
    	}
    }
    
    /**
     * Checks if a shop is valid. This is used to distinct
     * other player made signs from shop signs.
     * @param lines Lines printed on the shop sign
     * @return True if sign represents a valid shop, false otherwise.
     */
    private boolean shopIsValid(String[] lines) {
    	if(lines[0].equals(ChatColor.DARK_PURPLE + "[Buy]")) {
    		return true;
    	}
    	else{
    		return false;
    	}
    }
    
    /**
     * Checks if a sign object is a shop.
     * @param s Sign to check
     * @return True if sign, False otherwise
     */
    private boolean signIsShop(Sign s) {
    	String[] lines = s.getLines();
    	return lines[0].equals(ChatColor.DARK_PURPLE + "[Buy]");
    }
    /**
     * Checks if a player is the owner of a shop.
     * @param p Player object
     * @param signname Name written on the sign
     * @return True if owner, false otherwise
     */
    private boolean playerIsOwner(Player p, String signname) {
    	//return (ChatColor.DARK_BLUE + p.getName()).equals(ChatColor.DARK_BLUE + signname);
    	return true;
    }
}
