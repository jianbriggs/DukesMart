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
import java.util.concurrent.CompletableFuture;

import org.bukkit.Location;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

public class MySQLHelper {
	private String host;
	private int port;
	private String database;
	private String username;
	private String password;
	
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
                	String query = "CREATE TABLE IF NOT EXISTS dukesmart_shops ("
                				 + " shop_id int(11) NOT NULL,"
                				 + " player_uuid char(40) NOT NULL,"
                				 + " shop_name varchar(32) NOT NULL,"
                				 + " world varchar(10) NOT NULL DEFAULT 'NORMAL',"
                				 + " location_x smallint(6) NOT NULL,"
                				 + " location_y smallint(6) NOT NULL,"
                				 + " location_z smallint(6) NOT NULL,"
                				 + " material varchar(32) NOT NULL,"
                				 + " item_serialization blob NOT NULL,"
                				 + " item_hash varchar(256) NOT NULL)";
                    statement.execute(query);
                }
                // Ledger table
                try (Statement statement = connection.createStatement()){
                	String query = "CREATE TABLE IF NOT EXISTS dukesmart_ledgers ("
                			  	 + " ledger_id int(11) NOT NULL,"
                			  	 + " player_uuid char(40) NOT NULL,"
                			  	 + " income int(11) NOT NULL DEFAULT '0',"
                			  	 + " total_earned int(11) NOT NULL DEFAULT '0')";
                	statement.execute(query);
                }
                
                // Transaction history table
                try (Statement statement = connection.createStatement()){
                	String query = "CREATE TABLE IF NOT EXISTS dukesmart_transactions ("
                				 + " transaction_id int(11) NOT NULL," 
                				 + " buyer_uuid char(40) NOT NULL,"
                				 + " shop_id int(11) NOT NULL,"
                				 + " purchase_date datetime NOT NULL)";
                	statement.execute(query);
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
            	String sql = "SELECT shop_id, player_uuid, shop_name, item_serialization, quantity, price FROM dukesmart_shops"
            			     + " WHERE world = ? AND location_x = ? AND location_y = ? AND location_z = ?";
            	
                try (PreparedStatement query = connection.prepareStatement(sql)) {
                    query.setString(1, world);
                    query.setShort(2, x);
                    query.setShort(3, y);
                    query.setShort(4, z);
                    
                    try (ResultSet result = query.executeQuery()) {
                        result.next();
                        int s_id = result.getInt(1);
                    	String s_uuid  = result.getString(2);
                        String s_name  = result.getString(3);
                        short  s_quantity = result.getShort(5);
                        int    s_price = result.getInt(6);
						try {
							ItemStack item = itemFrom64(result.getString(4));
							shop = new Shop(s_id, s_uuid, s_name, world, x, y, z, item, s_quantity, s_price);
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
        	byte  y = (byte)  shop.getYLocation();
        	short z = (short) shop.getZLocation();
        	
        	if(shop.playerOwnsShop(player)) {
	            try (Connection connection = getConnection()) {
	            	String sql = "DELETE FROM dukesmart_shops WHERE world = ? AND location_x = ? AND location_y = ? AND location_z = ? AND player_uuid = ?";
	            	
	                try (PreparedStatement query = connection.prepareStatement(sql)) {
	                    query.setString(1, world);
	                    query.setShort(2, x);
	                    query.setShort(3, y);
	                    query.setShort(4, z);
	                    query.setString(5, player.getUniqueId().toString());
	                    
	                    if(query.executeUpdate() > 0 ) {
	                    	return true;
	                    }
	                    else {
	                    	return false;
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
    		Boolean result = false;
	    	try(Connection connection = getConnection()){
	    		String sql = "INSERT INTO dukesmart_shops (player_uuid, shop_name, world, location_x, location_y, location_z,"
	    				   + " material, quantity, price, item_serialization, item_hash) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
	    				   + " ON DUPLICATE KEY UPDATE player_uuid = ?, shop_name = ?, material = ?, quantity = ?, price = ?,"
	    				   + " item_serialization = ?, item_hash = ?";
	    		
	    		// inputs
	    		Location shopLocation = shopSign.getLocation();
	    		String player_uuid = player.getUniqueId().toString();
	    		String shop_name = player.getName() + "'s Shop";
	    		String world = shopLocation.getWorld().getName();
	    		short loc_x = (short) shopLocation.getX();
	    		byte loc_y = (byte) shopLocation.getY();
	    		short loc_z = (short) shopLocation.getZ();
	    		String material = item.getType().name();
	    		
	    		String[] tokens = shopSign.getLine(2).split(" ");
	    		int quantity = Integer.parseInt(tokens[0]);
	    		int price = Integer.parseInt(tokens[2].substring(1));
	    		item.setAmount(1);
	    		// TODO: set item durability to 100%
	    		
	    		Map<String, Object> item_serial = item.serialize();
	    		String item_serial_base64 = itemTo64(item);
	    		
	    		MessageDigest md = MessageDigest.getInstance("MD5");
	    	    md.update(item_serial.toString().getBytes());
	    	    byte[] digest = md.digest();
	
	    		String hash = Base64.getEncoder().encodeToString(digest);
	    		
	            try(PreparedStatement query = connection.prepareStatement(sql)){
		            query.setString(1, player_uuid);
		            query.setString(2, shop_name);
		            query.setString(3, world);
		            query.setShort(4, loc_x);
		            query.setShort(5, loc_y);
		            query.setShort(6, loc_z);
		            query.setString(7, material);
		            query.setInt(8, quantity);
		            query.setInt(9, price);
		            query.setString(10, item_serial_base64);
		            query.setString(11, hash);
		            
		            // duplicate
		            query.setString(12, player_uuid);
		            query.setString(13, shop_name);
		            query.setString(14, material);
		            query.setInt(15, quantity);
		            query.setInt(16, price);
		            query.setString(17, item_serial_base64);
		            query.setString(18, hash);

		            if(query.executeUpdate() > 0) {
		            	result = true;
		            }
		            else {
		            	result = false;
		            }
	            }
	        }
	    	catch (SQLException ex) {
	    		player.sendMessage(ex.getMessage());
	        } catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    	
	    	return result;
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
        	
        	String sql_addGoldToShopLedger = "UPDATE dukesmart_ledgers SET income = income + ?, total_earned = total_earned + ?"
        								   + " WHERE player_uuid = ?";
        	
        	String sql_logTransaction = "INSERT INTO dukesmart_transactions (buyer_uuid, shop_id, purchase_date) VALUES(?, ?, NOW())";
        	
            try (Connection connection = getConnection()) {

                try (PreparedStatement query = connection.prepareStatement(sql_logTransaction)) {
                	
                    query.setString(1, buyer.getUniqueId().toString());
                    query.setInt(2, shop.getID());
                    
                    if( query.executeUpdate() > 0 ) {
                    	transactionComplete = true;
                    }
                }
                
                try (PreparedStatement query = connection.prepareStatement(sql_addGoldToShopLedger)){
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
        	String sql_createPlayerLedger = "INSERT INTO dukesmart_ledgers (player_uuid, income, total_earned) VALUES(?, 0, 0)"
        								  + " ON DUPLICATE KEY UPDATE income = income, total_earned = total_earned";
        	
            try (Connection connection = getConnection()) {

                try (PreparedStatement query = connection.prepareStatement(sql_createPlayerLedger)) {
                	
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
    
    public CompletableFuture<Integer> getPlayerIncomeToRedeem(Player player){
        return CompletableFuture.supplyAsync(() -> {
        	String sql_getPlayerIncome = "SELECT income FROM dukesmart_ledgers WHERE player_uuid = ?";
        	
            try (Connection connection = getConnection()) {

                try (PreparedStatement query = connection.prepareStatement(sql_getPlayerIncome)) {
                	
                    query.setString(1, player.getUniqueId().toString());

                    try (ResultSet result = query.executeQuery()) {
                    	result.last();
                    	if(result.getRow() > 0) {
                    		return result.getInt(1);
                    	}
                    	else {
                    		return 0;
                    	}
                    }
                }

            } catch (SQLException e) {
                e.printStackTrace();
            }
            
            return 0;
        });
    }
    
    public CompletableFuture<Boolean> playerRedeemGold(Player player, int amount){
        return CompletableFuture.supplyAsync(() -> {
        	String sql_getPlayerIncome = "UPDATE dukesmart_ledgers SET income = income - ? WHERE player_uuid = ?";
        	
            try (Connection connection = getConnection()) {

                try (PreparedStatement query = connection.prepareStatement(sql_getPlayerIncome)) {
                	
                	query.setInt(1, amount);
                    query.setString(2, player.getUniqueId().toString());

                    if(query.executeUpdate() > 0) {
                    	return true;
                    }
                    else {
                    	return false;
                    }
                }

            } catch (SQLException e) {
                e.printStackTrace();
            }
            
            return false;
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
