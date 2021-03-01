package com.ruinscraft.dukesmart;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

public class MySQLHelper {
	private final String host;
	private final int port;
	private final String database;
	private final String username;
	private final String password;
	
	//private final String STR_TOP_TEN_LISTING = ChatColor.DARK_AQUA + "%s " + ChatColor.GRAY + "-" + ChatColor.GOLD + " $%d";
	private final String STR_TOP_TEN_LISTING = ChatColor.GOLD + "$%d" + ChatColor.GRAY + " - " + ChatColor.DARK_AQUA + "%s";
	private final String STR_VIEW_TRANSACTION = ChatColor.AQUA + "%s" + ChatColor.GRAY + " - " + ChatColor.DARK_AQUA + "%s";
	private final String STR_VIEW_PLAYER_HISTORY = ChatColor.AQUA + "%s" + ChatColor.GRAY + " - Shop owner: " + ChatColor.DARK_AQUA + "%s";

	private final String SQL_CREATE_TABLE_SHOPS = "CREATE TABLE IF NOT EXISTS dukesmart_shops ("
												+ " shop_id varchar(32) NOT NULL,"
												+ " player_uuid char(40) NOT NULL,"
												+ " world varchar(10) NOT NULL DEFAULT 'NORMAL',"
												+ " location_x int(11) NOT NULL,"
											    + " location_y int(11) NOT NULL,"
												+ " location_z int(11) NOT NULL,"
												+ " material varchar(32) NOT NULL,"
												+ " item_serialization blob NOT NULL,"
												+ " quantity int(11) NOT NULL DEFAULT '0',"
												+ " price int(11) NOT NULL DEFAULT '0',"
												+ " PRIMARY KEY (shop_id),"
												+ " UNIQUE KEY world (world, location_x, location_y, location_z))";
	
	private final String SQL_CREATE_TABLE_LEDGERS = "CREATE TABLE IF NOT EXISTS dukesmart_ledgers ("
											  	  + " player_uuid char(40) NOT NULL,"
											  	  + " income int(11) NOT NULL DEFAULT '0',"
											  	  + " total_earned int(11) NOT NULL DEFAULT '0',"
											  	  + " withdraw_timer date DEFAULT NULL,"
											  	  + " PRIMARY KEY(player_uuid),"
											  	  + " UNIQUE KEY player_uuid (player_uuid))";
	
	private final String SQL_CREATE_TABLE_TRANSACTIONS = "CREATE TABLE IF NOT EXISTS dukesmart_transactions ("
													   + " transaction_id int(11) NOT NULL AUTO_INCREMENT," 
													   + " buyer_uuid char(40) NOT NULL,"
													   + " shop_id varchar(32) NOT NULL,"
													   + " purchase_date datetime NOT NULL,"
													   + " PRIMARY KEY (transaction_id),"
													   + " CONSTRAINT dukesmart_transactions_ibfk_1 FOREIGN KEY (shop_id)"
													   + " REFERENCES dukesmart_shops (shop_id) ON DELETE CASCADE ON UPDATE CASCADE)";
	
	private final String SQL_SELECT_SHOP_FROM_LOCATION = "SELECT shop_id, player_uuid, item_serialization, quantity, price FROM dukesmart_shops"
		     										   + " WHERE world = ? AND location_x = ? AND location_y = ? AND location_z = ?";
	
	private final String SQL_DELETE_SHOP = "DELETE FROM dukesmart_shops WHERE world = ? AND location_x = ? AND location_y = ? AND location_z = ?";

	private final String SQL_CREATE_SHOP = "INSERT INTO dukesmart_shops (shop_id, player_uuid, world, location_x, location_y, location_z,"
									     + " material, quantity, price, item_serialization) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
									     + " ON DUPLICATE KEY UPDATE player_uuid = ?, material = ?, quantity = ?, price = ?, item_serialization = ?";
	
	
	private final String SQL_CREATE_LEDGER = "INSERT INTO dukesmart_ledgers (player_uuid, income, total_earned) VALUES(?, 0, 0)"
			  							   + " ON DUPLICATE KEY UPDATE income = income, total_earned = total_earned";
	
	private final String SQL_ADD_GOLD_TO_SHOP_LEDGER = "UPDATE dukesmart_ledgers SET income = income + ?, total_earned = total_earned + ?, withdraw_timer = NOW() WHERE player_uuid = ?";
	
	private final String SQL_GET_PLAYER_INCOME = "SELECT income, withdraw_timer FROM dukesmart_ledgers WHERE player_uuid = ?";
	
	private final String SQL_UPDATE_LEDGER_INCOME = "UPDATE dukesmart_ledgers SET income = ?, withdraw_timer = NULL WHERE player_uuid = ?";
	
	private final String SQL_CLEAR_LEDGER_INCOME = "UPDATE dukesmart_ledgers SET income = 0, withdraw_timer = NULL WHERE player_uuid = ?";
	
	private final String SQL_LOG_TRANSACTION = "INSERT INTO dukesmart_transactions (buyer_uuid, shop_id, purchase_date) VALUES(?, ?, NOW())";
	
	private final String SQL_VIEW_TOP_TEN = "SELECT player_uuid, total_earned FROM dukesmart_ledgers ORDER BY total_earned DESC LIMIT 10";
	
	private final String SQL_VIEW_RECENT_TRANSACTIONS = "SELECT buyer_uuid, purchase_date FROM dukesmart_transactions WHERE shop_id = ? ORDER BY purchase_date DESC LIMIT 10";
	
	private final String SQL_VIEW_PLAYER_HISTORY = "SELECT dukesmart_shops.player_uuid, purchase_date FROM dukesmart_transactions INNER JOIN dukesmart_shops ON dukesmart_shops.shop_id = dukesmart_transactions.shop_id WHERE buyer_uuid = ? ORDER BY purchase_date DESC LIMIT 10";
	
	public MySQLHelper(String host, int port, String database, String username, String password) {
		this.host = host;
		this.port = port;
		this.database = database;
		this.username = username;
		this.password = password;
		
		CompletableFuture.runAsync(() -> {
            try (Connection connection = getConnection()) {
            	// Shops table
                try (Statement statement = connection.createStatement()) {
                    statement.execute(this.SQL_CREATE_TABLE_SHOPS);
                }
                // Ledger table
                try (Statement statement = connection.createStatement()){
                	statement.execute(this.SQL_CREATE_TABLE_LEDGERS);
                }
                
                // Transaction history table
                try (Statement statement = connection.createStatement()){
                	statement.execute(this.SQL_CREATE_TABLE_TRANSACTIONS);
                }
                
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
	}
	
	private Connection getConnection() {
        String jdbcUrl = "jdbc:mysql://" + this.host + ":" + this.port + "/" + this.database;

        try {
            return DriverManager.getConnection(jdbcUrl, this.username, this.password);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }
	
	public CompletableFuture<Shop> getShopFromLocation(Location location){
        return CompletableFuture.supplyAsync(() -> {
        	// separate world/coordinate data
        	String world = location.getWorld().getName();
        	int x = (int) location.getX();
        	int y = (int) location.getY();
        	int z = (int) location.getZ();
        	Shop shop = null;
        	
            try (Connection connection = getConnection()) {

                try (PreparedStatement query = connection.prepareStatement(this.SQL_SELECT_SHOP_FROM_LOCATION)) {
                    query.setString(1, world);
                    query.setInt(2, x);
                    query.setInt(3, y);
                    query.setInt(4, z);
                    
                    try (ResultSet result = query.executeQuery()) {
                    	if(result.next()) {
	                        String s_id = result.getString(1);
	                    	String s_uuid  = result.getString(2);
	                        short  s_quantity = result.getShort(4);
	                        int    s_price = result.getInt(5);
							try {
								ItemStack item = itemFrom64(result.getString(3));
								shop = new Shop(s_id, s_uuid, world, x, y, z, item, s_quantity, s_price);
							} catch (IOException e) {
								e.printStackTrace();
							}
                    	}
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            
            return shop;
        });
	}

	public CompletableFuture<Boolean> removeShop(Player player, Shop shop){
        return CompletableFuture.supplyAsync(() -> {
        	// separate world/coordinate data
        	String world = shop.getWorld();
        	int x = (int) shop.getXLocation();
        	int y = (int) shop.getYLocation();
        	int z = (int) shop.getZLocation();
        	
            try (Connection connection = getConnection()) {
            	
                try (PreparedStatement query = connection.prepareStatement(this.SQL_DELETE_SHOP)) {
                    query.setString(1, world);
                    query.setInt(2, x);
                    query.setInt(3, y);
                    query.setInt(4, z);
                    
                    if(query.executeUpdate() > 0 ) {
                    	return true;
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

        	return false;
        });
	}
	/**
     * Registers a player created shop sign into the database.
     * 
     * @param player Player who created shop sign
     * @param shopSign Shop sign with player input data
     * @param item ItemStack representing item player would like to sell.
     */
    public CompletableFuture<Boolean> registerShop(Player player, Sign shopSign, ItemStack item) {
    	return CompletableFuture.supplyAsync(() -> {
	    	try(Connection connection = getConnection()){

	    		// inputs
	    		Location shopLocation = shopSign.getLocation();
	    		String player_uuid = player.getUniqueId().toString();
	    		String world = shopLocation.getWorld().getName();
	    		int loc_x = (int) shopLocation.getX();
	    		int loc_y = (int) shopLocation.getY();
	    		int loc_z = (int) shopLocation.getZ();
	    		String material = item.getType().name();
	    		
	    		String[] tokens = shopSign.getLine(2).split(" ");
	    		int quantity = Integer.parseInt(tokens[0]);
	    		int price = Integer.parseInt(tokens[2].substring(1));
	    		item.setAmount(1);
	    		ItemMeta meta = item.getItemMeta();
	    		
	    		// if the item can be damaged, we need to set the damage to 0
	    		if(meta instanceof Damageable) {
	    			Damageable damageData = (Damageable) meta;
	    			damageData.setDamage(0);
	    		}

	    		// update meta changes
	    		item.setItemMeta(meta);
	    		
	    		String item_serial_base64 = itemTo64(item);
	    		
	    		// generate shop id from player UUID, material name, and current time stamp
	    	    Instant timestamp = Instant.now();
	    	    String shop_id_plain = player.getUniqueId().toString() + material +  timestamp.toString();
	    	    MessageDigest md = MessageDigest.getInstance("MD5");
	    	    md.update(shop_id_plain.getBytes());
	    	    byte[] digest = md.digest();
	    	    char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
    	        char[] hexChars = new char[digest.length * 2];
    	        for (int j = 0; j < digest.length; j++) {
    	            int v = digest[j] & 0xFF;
    	            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
    	            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
    	        }
    	        String shop_id = new String(hexChars);
    	        
	            try(PreparedStatement query = connection.prepareStatement(this.SQL_CREATE_SHOP)){
	            	query.setString(1, shop_id);
		            query.setString(2, player_uuid);
		            query.setString(3, world);
		            query.setInt(4, loc_x);
		            query.setInt(5, loc_y);
		            query.setInt(6, loc_z);
		            query.setString(7, material);
		            query.setInt(8, quantity);
		            query.setInt(9, price);
		            query.setString(10, item_serial_base64);
		            
		            // duplicate
		            query.setString(11, player_uuid);
		            query.setString(12, material);
		            query.setInt(13, quantity);
		            query.setInt(14, price);
		            query.setString(15, item_serial_base64);

		            if(query.executeUpdate() > 0) {
		            	return true;
		            }
	            }
	        }
	    	catch (SQLException ex) {
	    		player.sendMessage(ex.getMessage());
	        } catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			}
	    	
	    	return false;
    	});
	}
    
    /**
     * Facilitates transaction data on buyer purchase.
     * 
     * @param buyer Player who made purchased
     * @param shop Shop Player purchased from
     * @return True if process was able to complete
     */
    public CompletableFuture<Boolean> processTransaction(Player buyer, Shop shop){
        return CompletableFuture.supplyAsync(() -> {
        	boolean transactionComplete = false;
        
            try (Connection connection = getConnection()) {

                try (PreparedStatement query = connection.prepareStatement(this.SQL_LOG_TRANSACTION)) {
                	
                    query.setString(1, buyer.getUniqueId().toString());
                    query.setString(2, shop.getID());
                    
                    if( query.executeUpdate() > 0 ) {
                    	transactionComplete = true;
                    }
                }
                
                // no need to change the ledger income if the price is free
                if(shop.getPrice() > 0) {
	                try (PreparedStatement query = connection.prepareStatement(this.SQL_ADD_GOLD_TO_SHOP_LEDGER)){
	                	query.setInt(1, shop.getPrice());
	                	query.setInt(2, shop.getPrice());
	                	query.setString(3, shop.getOwner());
	                	
	                	if( query.executeUpdate() > 0 ){
	                		transactionComplete = true;
	                	}
	                }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            
            return transactionComplete;
        });
    }
    
    public CompletableFuture<Boolean> clearPlayerLedger(Player player){
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = getConnection()) {
                try (PreparedStatement query = connection.prepareStatement(this.SQL_CLEAR_LEDGER_INCOME)){
                	query.setString(1, player.getUniqueId().toString());
                	
                	if( query.executeUpdate() > 0 ){
                		return true;
                	}
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            
            return false;
        });
    }
    
    public CompletableFuture<Boolean> setupLedger(Player player){
        return CompletableFuture.supplyAsync(() -> {
        	
            try (Connection connection = getConnection()) {

                try (PreparedStatement query = connection.prepareStatement(this.SQL_CREATE_LEDGER)) {
                	
                    query.setString(1, player.getUniqueId().toString());

                    if( query.executeUpdate() > 0 ) {
                        return true;
                    }
                }

            } catch (SQLException e) {
                e.printStackTrace();
            }
            
            return false;
        });
    }
    
    /**
     * Gets the amount of money in a player's shop ledger as well as last withdraw date
     * @param player - Player whose ledger will be checked
     * @return Amount of income on success, -1 if ledger does not exist, 0 otherwise
     */
    public CompletableFuture<IncomeDateWrapper> getPlayerIncome(Player player){
        return CompletableFuture.supplyAsync(() -> {
        	        	
            try (Connection connection = getConnection()) {

                try (PreparedStatement query = connection.prepareStatement(this.SQL_GET_PLAYER_INCOME)) {
                	
                    query.setString(1, player.getUniqueId().toString());

                    try (ResultSet result = query.executeQuery()) {
                    	result.last();
                    	if(result.getRow() > 0) {
                    		return new IncomeDateWrapper(result.getInt(1), result.getDate(2));
                    	}
                    	else {
                    		// player must not have a ledger
                    		return null;
                    	}
                    }
                }

            } catch (SQLException e) {
                e.printStackTrace();
            }
            
            return null;
        });
    }
    
    /**
     * Returns the income of player from a supplied unique ID
     * @param playerUUID - A player's unique ID
     * @return Amount of income on success, -1 if ledger does not exist, 0 otherwise
     */
    public CompletableFuture<Integer> getPlayerIncome(UUID playerUUID){
        return CompletableFuture.supplyAsync(() -> {
        	        	
            try (Connection connection = getConnection()) {

                try (PreparedStatement query = connection.prepareStatement(this.SQL_GET_PLAYER_INCOME)) {
                	
                    query.setString(1, playerUUID.toString());

                    try (ResultSet result = query.executeQuery()) {
                    	result.last();
                    	if(result.getRow() > 0) {
                    		return result.getInt(1);
                    	}
                    	else {
                    		// player must not have a ledger
                    		return -1;
                    	}
                    }
                }

            } catch (SQLException e) {
                e.printStackTrace();
            }
            
            return 0;
        });
    }
    /**
     * Withdraws a specified amount of money from a player's ledger
     * 
     * @param player - Player who's ledger will be redeemed
     * @param amount - amount to withdraw from ledger
     * @return 1 on success, 0 on no-change, -1 on error
     * 
     */
    public CompletableFuture<Integer> playerRedeemLedgerIncome(Player player, int amount){
        return CompletableFuture.supplyAsync(() -> {       	
            try (Connection connection = getConnection()) {
            	
            	try (PreparedStatement startIncomeQuery = connection.prepareStatement(this.SQL_GET_PLAYER_INCOME)){
            		startIncomeQuery.setString(1, player.getUniqueId().toString());
            		int startIncome, newAmount;
            		
            		try (ResultSet result = startIncomeQuery.executeQuery()) {
                        result.next();
                        startIncome = result.getInt(1);   
                        
                        /*
                         * if the player cannot withdraw the specified amount,
                         * exit with error to prevent new query and checking
                         */
                        if(startIncome < amount) {
                        	return -1;
                        }
                        
                        newAmount = startIncome - amount;
                        
                        /* similarly, if the new amount is equal to the
                         * starting amount (no change), don't bother updating the ledger
                         */
                        if(newAmount == startIncome) {
                        	return 0;
                        }
                    }
            		
	                try (PreparedStatement updateIncome = connection.prepareStatement(this.SQL_UPDATE_LEDGER_INCOME)) {
	                	
	                	updateIncome.setInt(1, newAmount);
	                    updateIncome.setString(2, player.getUniqueId().toString());
	                    
	                    if(updateIncome.executeUpdate() > 0) {
	                    	return 1;
	                    }
	                }
            	}

            } catch (SQLException e) {
                e.printStackTrace();
            }
            
            return -1;
        });
    }
    
    /**
     * Returns the top 10 highest earning players on the server
     * 
     * @return ArrayList containing formatted strings, or null on error
     */
    public CompletableFuture<ArrayList<String>> viewTopTenEarners(){
        return CompletableFuture.supplyAsync(() -> {
        	        	
            try (Connection connection = getConnection()) {

                try (PreparedStatement query = connection.prepareStatement(this.SQL_VIEW_TOP_TEN)) {
                	
                    try (ResultSet result = query.executeQuery()) {
                    	ArrayList<String> topTen = new ArrayList<String>();
                    	while(result.next()) {
                    		String playerName = Bukkit.getOfflinePlayer(UUID.fromString(result.getString(1))).getName();
                    		topTen.add(String.format(this.STR_TOP_TEN_LISTING, result.getInt(2), playerName));
                    	}
                    	return topTen;
                    }
                }

            } catch (SQLException e) {
                e.printStackTrace();
            }
            
            return null;
        });
    }
    
    /**
     * Returns 10 recent transactions for a given shop, in descending order
     * 
     * @return ArrayList containing formatted strings, or null on error
     */
    public CompletableFuture<ArrayList<String>> viewRecentTransactions(Shop shop){
        return CompletableFuture.supplyAsync(() -> {
        	        	
            try (Connection connection = getConnection()) {

                try (PreparedStatement query = connection.prepareStatement(this.SQL_VIEW_RECENT_TRANSACTIONS)) {
                	query.setString(1, shop.getID());
                    try (ResultSet result = query.executeQuery()) {
                    	ArrayList<String> recent= new ArrayList<String>();
                    	while(result.next()) {
                    		String playerName = Bukkit.getOfflinePlayer(UUID.fromString(result.getString(1))).getName();
                    		recent.add(String.format(this.STR_VIEW_TRANSACTION, result.getString(2), playerName));
                    	}
                    	return recent;
                    }
                }

            } catch (SQLException e) {
                e.printStackTrace();
            }
            
            return null;
        });
    }

    public CompletableFuture<ArrayList<String>> viewPlayerHistory(UUID playerUUID){
        return CompletableFuture.supplyAsync(() -> {
        	        	
            try (Connection connection = getConnection()) {

                try (PreparedStatement query = connection.prepareStatement(this.SQL_VIEW_PLAYER_HISTORY)) {
                	query.setString(1, playerUUID.toString());
                    try (ResultSet result = query.executeQuery()) {
                    	ArrayList<String> recent= new ArrayList<String>();
                    	while(result.next()) {
                    		String shopOwnerName = Bukkit.getOfflinePlayer(UUID.fromString(result.getString(1))).getName();
                    		recent.add(String.format(this.STR_VIEW_PLAYER_HISTORY, result.getString(2), shopOwnerName));
                    	}
                    	return recent;
                    }
                }

            } catch (SQLException e) {
                e.printStackTrace();
            }
            
            return null;
        });
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
    
    private ItemStack itemFrom64(String data) throws IOException {
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
}
