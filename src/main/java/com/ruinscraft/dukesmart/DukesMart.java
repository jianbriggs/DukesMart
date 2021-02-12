package com.ruinscraft.dukesmart;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

public class DukesMart extends JavaPlugin {
	private FileConfiguration config = getConfig();
	private MySQLHelper mySQLHelper;
	private ShopListener sl;
	private NotifyPlayerIncomeTaskController notifyController;
	private SelectedShopController selectController;
	
	public final Material SHOP_CURRENCY_MATERIAL   = Material.GOLD_INGOT;
	public final XMaterial SHOP_CURRENCY_XMATERIAL = XMaterial.GOLD_INGOT;

    @Override
    public void onEnable() {
    	
    	// check config.yml for database credentials
    	if(!config.contains("mysql.host") || !config.contains("mysql.port") || !config.contains("mysql.database")
    	   || !config.contains("mysql.username") || !config.contains("mysql.password")) {
    		
    		getLogger().info("Failed to find database information in config.yml!");
    		getLogger().info("Adding default values to config.yml");
    		
    		config.addDefault("mysql.host", "localhost");
    		config.addDefault("mysql.port", 3306);
    		config.addDefault("mysql.database", "mcatlas");
    		config.addDefault("mysql.username", "mcatlasdev");
    		config.addDefault("mysql.password", "password123");
    		
    		config.options().copyDefaults(true);
            saveConfig();
    		
    	}
    	
    	setupMySQLHelper();
    	
    	this.sl = new ShopListener(this);
    	this.notifyController = new NotifyPlayerIncomeTaskController(this);
    	this.selectController = new SelectedShopController();
    	
    	this.getCommand("shop").setExecutor(new ShopCommandExecutor(this));
    	this.getCommand("shop").setTabCompleter(this);
    	
    	Bukkit.getPluginManager().registerEvents(this.sl, this);
    	
    	getLogger().info("DukesMart has been enabled!");
    }

    @Override
    public void onDisable() {
    	getLogger().info("DukesMart has been disabled!");
    }

    public void setupMySQLHelper() {
    	// load MySQL database info
    	String host     = config.getString("mysql.host");
    	int    port     = config.getInt("mysql.port");
    	String database = config.getString("mysql.database");
    	String username = config.getString("mysql.username");
    	String password = config.getString("mysql.password");
    	
    	this.mySQLHelper = new MySQLHelper(host, port, database, username, password);
    }
    
    public MySQLHelper getMySQLHelper() {
    	return this.mySQLHelper;
    }
    
    public NotifyPlayerIncomeTaskController getNotifyPlayerController() {
    	return this.notifyController;
    }
    
    public SelectedShopController getSelectedShopController() {
    	return this.selectController;
    }
}