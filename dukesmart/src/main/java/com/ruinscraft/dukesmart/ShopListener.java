package com.ruinscraft.dukesmart;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.Sign;
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
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import com.google.gson.Gson;

import net.md_5.bungee.api.ChatColor;

public class ShopListener implements Listener{
	private DukesMart dmref;
	
	private String mySQLhost;
	private String mySQLport;
	private String mySQLdatabase;
	private String mySQLusername;
	private String mySQLpassword;
	private String mySQLurl;
	
	private HashMap<String, Location> signSelectedMap = new HashMap<String, Location>();
	
    public ShopListener(String host, String port, String database, String username, String password, DukesMart dmref) {
    	this.dmref = dmref;
    	
    	this.mySQLhost = host;
    	this.mySQLport = port;
    	this.mySQLdatabase = database;
    	this.mySQLusername = username;
    	this.mySQLpassword = password;
    	
    	this.mySQLurl = "jdbc:mysql://" + this.mySQLhost + ":" + this.mySQLport + "/" + this.mySQLdatabase + "?useSSL=true";
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

        int earned_gold = 128;
        player.sendMessage(pluginName() + ChatColor.WHITE + "Hello, " + ChatColor.AQUA + player.getName() + ChatColor.WHITE
        				   + ". Since last login, you have made " + ChatColor.GOLD + earned_gold + ChatColor.WHITE + " gold!");
       
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

                			ItemStack itemToSell = player.getInventory().getItemInMainHand();
                			XMaterial itemMat = XMaterial.matchXMaterial(itemToSell);
                			
                			// if player has something in hand (and is owner) set the shop's item
                			if(!(itemToSell.getType() == Material.AIR) && playerIsOwner(player, signLines[3])) {
	                			s.setLine(1, itemMat.name());
	                			s.update();
	                			registerShop(player, s, itemToSell);
	                			player.sendMessage(ChatColor.AQUA + "Shop item set. Place items to sell in chest below sign.");
                			}
                		}
                		else{
                			Location shopLocation = getShopLocation(s);
                			Location playerSelected = this.signSelectedMap.get(playerUID);
                			
                			// On right-click the player "selects" the shop.
                			// In the map, store location of the shop the player has selected.
                			if( playerSelected == null || !playerSelected.equals(shopLocation)) {
                				this.signSelectedMap.put(playerUID, shopLocation);
                				player.sendMessage(pluginName() + ChatColor.GREEN + " Shop selected.");
                				
                				// TODO: Implement this
                				displayShopInformation(player, shopLocation);
                			}
                			else if( playerSelected.equals(shopLocation)){
			                	// get the material to buy
			                	// note that this will eventually be replaced with an ItemStack
			                	// from the DB
			                	
	                			//Material blockToBuy = Material.getMaterial(signLines[1].toUpperCase());
	                			String materialNameFix = signLines[1].toUpperCase().replace(" ", "_");;
	                			
			                	XMaterial blockToBuy = XMaterial.valueOf(materialNameFix);
	
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
			                			if(dci.getLeftSide().contains(blockToBuy.parseMaterial(), quantity)) {
			                				storeStock = dci.getLeftSide();
			                				hasStock = true;
			                			}
			                			else if(dci.getRightSide().contains(blockToBuy.parseMaterial(), quantity)) {
			                				storeStock = dci.getRightSide();
			                				hasStock = true;
			                			}
			                		}
			                		else{
			                			player.sendMessage("Store is single chest");
			                			if(storeStock.contains(blockToBuy.parseMaterial(), quantity)) {
			                				hasStock = true;
			                			}
			                		}
			                		
			                		if(hasStock) {
			                			PlayerInventory pi = player.getInventory();
			                			
			                			if(pi.containsAtLeast(new ItemStack(Material.GOLD_INGOT), cost)){
				                			// convert material to ItemStack
				                			ItemStack item = blockToBuy.parseItem();
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
                	}
                	else {
                		player.sendMessage("Debug: Sign is not a shop");
                	}
                }
            }
        }
    }
    
    @EventHandler
    public void onBlockBreak(BlockBreakEvent evt) {
    	Player player = evt.getPlayer();
    	Block block = evt.getBlock();
    	
    	if(block.getType() == Material.WALL_SIGN || block.getType() == Material.SIGN) {
    		Sign s = (Sign) block.getState();
    		
    		if(signIsShop(s)) {
    			
    			BukkitRunnable runnable = new BukkitRunnable() {
					@Override
					public void run() {
					
						// TODO Auto-generated method stub
						Location shopLocation = s.getLocation();
						Shop shop = getShopFromLocation(shopLocation);
						
						// If player is the owner, delete it.
						// For testing purposes we are going to negate it
						// to see the other effect.
						if(false) {
							evt.setCancelled(true);
							player.sendMessage(pluginName() + ChatColor.GREEN + "You are the owner of this shop.");		
						}
						else {
							evt.setCancelled(true);
							player.sendMessage(pluginName() + ChatColor.RED + "You cannot remove a shop that you do not own.");	
						}
						
					}
    			};
    			runnable.runTaskAsynchronously(this.dmref);
    		}
    		else {
    			return;
    		}
    	}
    }
    
    public Shop getShopFromLocation(Location loc){
    	// separate world/coordinate data
    	String world = loc.getWorld().getName();
    	short x = (short) loc.getX();
    	byte  y = (byte)  loc.getY();
    	short z = (short) loc.getZ();
    	
    	String url      = this.mySQLurl;
    	String username = this.mySQLusername;
    	String password = this.mySQLpassword;

    	try{
    		Connection con = DriverManager.getConnection(url, username, password);
    		String query = "SELECT (player_uuid, shop_name, item_serialization) FROM dukesmart_shops WHERE world = ?, location_x = ?, location_y = ?, location_z = ?";

            PreparedStatement ps = con.prepareStatement(query);
            ps.setString(1, world);
            ps.setShort(2, x);
            ps.setShort(3, y);
            ps.setShort(4, z);
            
            ResultSet rs = ps.executeQuery();
            rs.next();
            
            String s_uuid = rs.getString(0);
            String s_name = rs.getString(1);
            ItemStack item = itemFrom64(rs.getString(2));
            
            Shop shop = new Shop(s_uuid, s_name, world, x, y, z);
            shop.setItem(item);
            
            return shop;
        }
    	catch (SQLException ex) {
    		ex.getMessage();
        } catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    	return null;
    }
    /**
     * Registers a player created shop sign into the database.
     * 
     * @param player Player who created shop sign
     * @param shopSign Shop sign with player input data
     * @param item ItemStack representing item player would like to sell.
     * @return True if shop successfully registered in database,
     *         False if database could not save information.
     */
    
    private void registerShop(Player player, Sign shopSign, ItemStack item) {
    	String url = this.mySQLurl;
    	String username = this.mySQLusername;
    	String password = this.mySQLpassword;
    	DukesMart dmref = this.dmref;
    	BukkitRunnable runnable = new BukkitRunnable() {
			@Override
			public void run() {
		    	try{
		    		Connection con = DriverManager.getConnection(url, username, password);
		    		String query = "INSERT INTO dukesmart_shops (player_uuid, shop_name, world, location_x, location_y, location_z,"
		    				       + "material, item_serialization, item_hash) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?)";
		    		
		    		// inputs
		    		Location shopLocation = shopSign.getLocation();
		    		String player_uuid = player.getUniqueId().toString();
		    		String shop_name = player.getName() + "'s Shop";
		    		String world = shopLocation.getWorld().getName();
		    		short loc_x = (short) shopLocation.getX();
		    		byte loc_y = (byte) shopLocation.getY();
		    		short loc_z = (short) shopLocation.getZ();
		    		String material = item.getType().name();
		    		
		    		item.setAmount(1);

		    		Map<String, Object> item_serial = item.serialize();
		    		String item_serial_base64 = itemTo64(item);
		    		
		    		MessageDigest md = MessageDigest.getInstance("MD5");
		    	    md.update(item_serial.toString().getBytes());
		    	    byte[] digest = md.digest();

		    		String hash = Base64.getEncoder().encodeToString(digest);
		    		
		            PreparedStatement ps = con.prepareStatement(query);
		            ps.setString(1, player_uuid);
		            ps.setString(2, shop_name);
		            ps.setString(3, world);
		            ps.setShort(4, loc_x);
		            ps.setShort(5, loc_y);
		            ps.setShort(6, loc_z);
		            ps.setString(7, material);
		            ps.setString(8, item_serial_base64);
		            ps.setString(9, hash);
		            
		            ps.executeUpdate();
		            player.sendMessage("(Debug) Shop added to database");

		        }
		    	catch (SQLException ex) {
		    		player.sendMessage(ex.getMessage());
		        } catch (NoSuchAlgorithmException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
    	};
    	
    	runnable.runTaskAsynchronously(dmref);
	}
    
    private String itemTo64(ItemStack stack) throws IllegalStateException {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
            dataOutput.writeObject(stack);

            // Serialize that array
            dataOutput.close();
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        }
        catch (Exception e) {
            throw new IllegalStateException("Unable to save item stack.", e);
        }
    }
    
    private static ItemStack itemFrom64(String data) throws IOException {
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64.getDecoder().decode(data));
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
            try {
                return (ItemStack) dataInput.readObject();
            } finally {
                dataInput.close();
            }
        }
        catch (ClassNotFoundException e) {
            throw new IOException("Unable to decode class type.", e);
        }
    }
	/**
     * Displays a Scoreboard containing information related to
     * the shop the player has selected.
     * @param shopLocation Location of the selected shop sign
     */
    private void displayShopInformation(Player player, Location shopLocation) {

    	Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
    	Objective obj = board.registerNewObjective("DukesMart", "Shop", ChatColor.GOLD + "[" + ChatColor.DARK_GREEN + "DukesMart" + ChatColor.GOLD + "]");
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        
        obj.getScore(shopGuiPad(ChatColor.WHITE + "ianfreakingb's Shop")).setScore( 20 );
        obj.getScore(shopGuiPad(" ")).setScore(19);
        
        obj.getScore(shopGuiPad("" + ChatColor.BOLD + ChatColor.RED  + "For sale")).setScore( 18 );
        obj.getScore(shopGuiPad(" ")).setScore(17);
        
        obj.getScore(shopGuiPad(ChatColor.AQUA + "Diamond Sword")).setScore( 16 );
        obj.getScore(shopGuiPad(" ")).setScore(15);
        
        obj.getScore(shopGuiPad(ChatColor.ITALIC + "- Mending")).setScore( 14 );
        obj.getScore(shopGuiPad(" ")).setScore(13);
        
        obj.getScore(shopGuiPad("" + ChatColor.BOLD + ChatColor.RED + "Quantity")).setScore( 12 );
        obj.getScore(shopGuiPad(" ")).setScore(11);
        
        obj.getScore(shopGuiPad(ChatColor.WHITE + "1")).setScore( 10 );   
        obj.getScore(shopGuiPad(" ")).setScore(9);
        
        obj.getScore(shopGuiPad("" + ChatColor.BOLD + ChatColor.RED + "Cost")).setScore( 8 );
        obj.getScore(shopGuiPad(" ")).setScore(7);
        
        obj.getScore(shopGuiPad(ChatColor.WHITE + "24 gold")).setScore( 6 );
        obj.getScore(shopGuiPad(" ")).setScore(5);
        
        obj.getScore(shopGuiPad(" ")).setScore(4);

        obj.getScore(shopGuiPad(ChatColor.WHITE + "To buy, right click sign.")).setScore(3);

        player.setScoreboard(board);    
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
