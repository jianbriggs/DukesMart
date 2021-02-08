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
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

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
	
	private final String SQL_CREATE_TABLE_SHOPS = "CREATE TABLE IF NOT EXISTS dukesmart_shops ("
												+ " shop_id int(11) NOT NULL,"
												+ " player_uuid char(40) NOT NULL,"
												+ " world varchar(10) NOT NULL DEFAULT 'NORMAL',"
												+ " location_x smallint(6) NOT NULL,"
											    + " location_y smallint(6) NOT NULL,"
												+ " location_z smallint(6) NOT NULL,"
												+ " material varchar(32) NOT NULL,"
												+ " item_serialization blob NOT NULL)";
	
	private final String SQL_CREATE_TABLE_LEDGERS = "CREATE TABLE IF NOT EXISTS dukesmart_ledgers ("
											  	  + " player_uuid char(40) NOT NULL,"
											  	  + " income int(11) NOT NULL DEFAULT '0',"
											  	  + " total_earned int(11) NOT NULL DEFAULT '0')";
	
	private final String SQL_CREATE_TABLE_TRANSACTIONS = "CREATE TABLE IF NOT EXISTS dukesmart_transactions ("
													   + " transaction_id int(11) NOT NULL," 
													   + " buyer_uuid char(40) NOT NULL,"
													   + " shop_id int(11) NOT NULL,"
													   + " purchase_date datetime NOT NULL)";
	
	private final String SQL_SELECT_SHOP_FROM_LOCATION = "SELECT shop_id, player_uuid, item_serialization, quantity, price FROM dukesmart_shops"
		     										   + " WHERE world = ? AND location_x = ? AND location_y = ? AND location_z = ?";
	
	private final String SQL_DELETE_SHOP = "DELETE FROM dukesmart_shops WHERE world = ? AND location_x = ? AND location_y = ? AND location_z = ? AND player_uuid = ?";

	private final String SQL_CREATE_SHOP = "INSERT INTO dukesmart_shops (player_uuid, world, location_x, location_y, location_z,"
									     + " material, quantity, price, item_serialization) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?)"
									     + " ON DUPLICATE KEY UPDATE player_uuid = ?, material = ?, quantity = ?, price = ?, item_serialization = ?";
	
	
	private final String SQL_CREATE_LEDGER = "INSERT INTO dukesmart_ledgers (player_uuid, income, total_earned) VALUES(?, 0, 0)"
			  							   + " ON DUPLICATE KEY UPDATE income = income, total_earned = total_earned";
	
	private final String SQL_ADD_GOLD_TO_SHOP_LEDGER = "UPDATE dukesmart_ledgers SET income = income + ?, total_earned = total_earned + ? WHERE player_uuid = ?";
	
	private final String SQL_GET_PLAYER_INCOME = "SELECT income FROM dukesmart_ledgers WHERE player_uuid = ?";
	
	private final String SQL_UPDATE_LEDGER_INCOME = "UPDATE dukesmart_ledgers SET income = ? WHERE player_uuid = ?";
	
	private final String SQL_LOG_TRANSACTION = "INSERT INTO dukesmart_transactions (buyer_uuid, shop_id, purchase_date) VALUES(?, ?, NOW())";

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
        	short x = (short) location.getX();
        	byte  y = (byte)  location.getY();
        	short z = (short) location.getZ();
        	Shop shop = null;
        	
            try (Connection connection = getConnection()) {

                try (PreparedStatement query = connection.prepareStatement(this.SQL_SELECT_SHOP_FROM_LOCATION)) {
                    query.setString(1, world);
                    query.setShort(2, x);
                    query.setShort(3, y);
                    query.setShort(4, z);
                    
                    try (ResultSet result = query.executeQuery()) {
                        result.next();
                        int s_id = result.getInt(1);
                    	String s_uuid  = result.getString(2);
                        short  s_quantity = result.getShort(4);
                        int    s_price = result.getInt(5);
						try {
							ItemStack item = itemFrom64(result.getString(3));
							shop = new Shop(s_id, s_uuid, world, x, y, z, item, s_quantity, s_price);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
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
        	short x = (short) shop.getXLocation();
        	short y = (short) shop.getYLocation();
        	short z = (short) shop.getZLocation();
        	
        	if(shop.playerOwnsShop(player)) {
	            try (Connection connection = getConnection()) {
	            	
	                try (PreparedStatement query = connection.prepareStatement(this.SQL_DELETE_SHOP)) {
	                    query.setString(1, world);
	                    query.setShort(2, x);
	                    query.setShort(3, y);
	                    query.setShort(4, z);
	                    query.setString(5, player.getUniqueId().toString());
	                    
	                    if(query.executeUpdate() > 0 ) {
	                    	return true;
	                    }
	                }
	            } catch (SQLException e) {
	                e.printStackTrace();
	            }
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
	    		short loc_x = (short) shopLocation.getX();
	    		short loc_y = (short) shopLocation.getY();
	    		short loc_z = (short) shopLocation.getZ();
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
	    		
	    		Map<String, Object> item_serial = item.serialize();
	    		String item_serial_base64 = itemTo64(item);
	    		
	    		MessageDigest md = MessageDigest.getInstance("MD5");
	    	    md.update(item_serial.toString().getBytes());
	
	            try(PreparedStatement query = connection.prepareStatement(this.SQL_CREATE_SHOP)){
		            query.setString(1, player_uuid);
		            query.setString(2, world);
		            query.setShort(3, loc_x);
		            query.setShort(4, loc_y);
		            query.setShort(5, loc_z);
		            query.setString(6, material);
		            query.setInt(7, quantity);
		            query.setInt(8, price);
		            query.setString(9, item_serial_base64);
		            
		            // duplicate
		            query.setString(10, player_uuid);
		            query.setString(11, material);
		            query.setInt(12, quantity);
		            query.setInt(13, price);
		            query.setString(14, item_serial_base64);

		            if(query.executeUpdate() > 0) {
		            	return true;
		            }
	            }
	        }
	    	catch (SQLException ex) {
	    		player.sendMessage(ex.getMessage());
	        } catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
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
                    query.setInt(2, shop.getID());
                    
                    if( query.executeUpdate() > 0 ) {
                    	transactionComplete = true;
                    }
                }
                
                try (PreparedStatement query = connection.prepareStatement(this.SQL_ADD_GOLD_TO_SHOP_LEDGER)){
                	query.setInt(1, shop.getPrice());
                	query.setInt(2, shop.getPrice());
                	query.setString(3, shop.getOwner());
                	
                	if( query.executeUpdate() > 0 ){
                		transactionComplete = true;
                	}
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            
            return transactionComplete;
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
     * Gets the amount of money in a player's shop ledger
     * @param player - Player whose ledger will be checked
     * @return Amount of income on success, -1 if ledger does not exist, 0 otherwise
     */
    public CompletableFuture<Integer> getPlayerIncome(Player player){
        return CompletableFuture.supplyAsync(() -> {
        	        	
            try (Connection connection = getConnection()) {

                try (PreparedStatement query = connection.prepareStatement(this.SQL_GET_PLAYER_INCOME)) {
                	
                    query.setString(1, player.getUniqueId().toString());

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
