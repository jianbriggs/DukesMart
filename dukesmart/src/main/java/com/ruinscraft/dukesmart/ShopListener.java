package com.ruinscraft.dukesmart;

import java.util.HashMap;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

public class ShopListener implements Listener{
	private HashMap<String, String> signSelectedMap = new HashMap<String, String>();
	
	public HashMap<String, String> getSelectedMap(){
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

        int earned_gold = 128;
        player.sendMessage(pluginName() + ChatColor.WHITE + "Hello, " + ChatColor.AQUA + player.getName() + ChatColor.WHITE
        				   + ". Since last login, you have made " + ChatColor.GOLD + earned_gold + ChatColor.WHITE + " gold!");
        
        this.signSelectedMap.putIfAbsent(player.getUniqueId().toString(), "Example Sign");
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
	    		if(evt.getLine(1).isEmpty()) {
	    			evt.setLine(1, ChatColor.WHITE + "?");
	    		}

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
    		player.sendMessage(player.getName() + " placed a sign.");
    	}
    }
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent evt) {
        Player player = evt.getPlayer();
        
        if (evt.getAction() == Action.RIGHT_CLICK_BLOCK) {
        	Block clickedBlock = evt.getClickedBlock();
            if ( clickedBlock.getType() == Material.WALL_SIGN || clickedBlock.getType() == Material.SIGN){
                Sign s = (Sign) clickedBlock.getState();
                Block b = clickedBlock.getRelative(BlockFace.DOWN, 1);
                
                if(b.getState() instanceof Chest || b.getState() instanceof DoubleChest) {
                	Chest c = (Chest) b.getState();
                	Inventory storeStock = c.getInventory();
                	
                	// get store information from sign
                	String[] signLines = s.getLines();

                	if(shopIsValid(signLines)) {
                		
                		/* 
                		 * if the shop has a white '?' on the 2nd line,
                		 * that means it has not been set an item/block
                		 * to sell.
                		 */
                		if(signLines[1].equals(ChatColor.WHITE + "?")) {
                			player.sendMessage("Sign does not have an item.");
                			ItemStack itemToSell = player.getInventory().getItemInMainHand();
                			
                			// if player has something in hand (and is owner) set the shop's item
                			if(!itemToSell.getType().equals(Material.AIR) && playerIsOwner(player, signLines[3])) {
	                			s.setLine(1, itemToSell.getType().name());
	                			s.update();
	                			player.sendMessage(ChatColor.AQUA + "Shop item set. Place items to sell in chest below sign.");
                			}
                		}
                		else if(playerIsOwner(player, signLines[3])){
                			String playerUID = player.getUniqueId().toString();
                			if( this.signSelectedMap.containsKey(playerUID)) {
                				this.signSelectedMap.put(playerUID, s.getLocation().toString());
                				player.sendMessage(pluginName() + ChatColor.GREEN + " Shop selected.");
                			}
                		}
                		else {
		                	// get the material to buy
		                	// note that this will eventually be replaced with an ItemStack
		                	// from the DB
		                	Material blockToBuy = Material.getMaterial(signLines[1].toUpperCase());

		                	// get the quantity and gold cost
		                	String[] transaction = signLines[2].split(" ");
		                	int quantity = Integer.parseInt(transaction[0]);
		                	int cost = Integer.parseInt(transaction[2].substring(1));
		                	
		                	if(blockToBuy != null) {
		                		// check if the chest inventory contains the item
		                		boolean hasStock = false;
		                		
		                		if(storeStock instanceof DoubleChestInventory) {
		                			player.sendMessage("Store is double chest");
		                			DoubleChestInventory dci = (DoubleChestInventory) storeStock;
		                			if(dci.getLeftSide().contains(blockToBuy, quantity)) {
		                				storeStock = dci.getLeftSide();
		                				hasStock = true;
		                			}
		                			else if(dci.getRightSide().contains(blockToBuy, quantity)) {
		                				storeStock = dci.getRightSide();
		                				hasStock = true;
		                			}
		                		}
		                		else{
		                			player.sendMessage("Store is single chest");
		                			if(storeStock.contains(blockToBuy, quantity)) {
		                				hasStock = true;
		                			}
		                		}
		                		
		                		if(hasStock) {
		                			PlayerInventory pi = player.getInventory();
		                			
		                			if(pi.containsAtLeast(new ItemStack(Material.GOLD_INGOT), cost)){
			                			// convert material to ItemStack
			                			ItemStack item = new ItemStack(blockToBuy);
			                			item.setAmount(quantity);
			                			
			                			// remove said items from the chest
			                			storeStock.removeItem(item);
			                			
			                			pi.removeItem(new ItemStack(Material.GOLD_INGOT, cost));
			                			// and put into the player's inventory
			                			pi.addItem(item);
		
			                			player.sendMessage("You purchased " + ChatColor.AQUA + quantity + " "
			                								+ blockToBuy + ChatColor.WHITE + " for " + ChatColor.GOLD + cost + " gold.");
		                			}
		                			else {
		                				sendError(player, "Sorry, you do not have enough gold to buy.");
		                			}
		                		}
		                		else {
		                			sendError(player, "Sorry, this shop is out of stock. Come back later.");
		                		}
		                	}
		                	else {
		                		sendError(player, "Oops! I don't recognize this item. The shop owner should fix this.");
		                	}
                		}
	                }
                	else {
                		player.sendMessage("Debug: Sign is not a shop");
                	}
                }
            }
        }
    }

    /*
     * Helper methods below this comment.
     */
    
    /**
     * Returns a formatted string representing the plugin name (DukesMart).
     * Make sure to reset the chat color AFTER this method is called.
     */
    private String pluginName() {
    	return ChatColor.GOLD + "[" + ChatColor.DARK_GREEN + "DukesMart" + ChatColor.GOLD + "]";
    }
    
    /**
     * Sends an error message to the player.
     * @param p Player to send message to
     * @param message Message to output
     */
    private void sendError(Player p, String message) {
    	p.sendMessage(ChatColor.RED + message);
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
	    	}
	    	// then, check if the word "for" is present
	    	if(!tokens[1].isBlank() && !tokens[1].equalsIgnoreCase("for")) {
	    		return false;
	    	}
	    	
	    	// finally, check the format of the price tag
	    	// first char should be '$', rest should be digit
	    	if(!tokens[2].isBlank()) {
	    		char[] letters = tokens[2].toCharArray();
	    		if(letters[0] != '$') {
	    			return false;
	    		}
	    		
	    		for(byte i = 1; i < letters.length; i++) {
	    			if(!Character.isDigit(letters[i])) {
	    				return false;
	    			}
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
    
    /*
     * This method will likely not be used, or changed entirely,
     * so consider it a placeholder.
     */
    private boolean confirmTransaction(Player p, ItemStack item) {
    	TextComponent prompt = new TextComponent( "You are about to purchase:" );
    	
    	TextComponent itemText = new TextComponent( "" + item.getAmount() + "x " + item.getType());
    	itemText.setColor(ChatColor.AQUA);
    	
    	TextComponent acceptButton = new TextComponent( "[ ACCEPT ]" );
    	acceptButton.setColor( ChatColor.GREEN );
    	
    	TextComponent cancelButton = new TextComponent( "[ CANCEL ]" );
    	cancelButton.setColor( ChatColor.RED );
    	
    	//subComponent.setHoverEvent( new HoverEvent( HoverEvent.Action.SHOW_TEXT, new ComponentBuilder( "Click me!" ).create() ) );
    	//subComponent.setClickEvent( new ClickEvent( ClickEvent.Action.));
    	prompt.addExtra(itemText);
    	prompt.addExtra( acceptButton );
    	prompt.addExtra( cancelButton );
    	p.spigot().sendMessage( prompt );
    	
    	return true;
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
