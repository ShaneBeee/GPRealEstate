package me.SuperPyroManiac.GPR;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Logger;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class GPRealEstate extends JavaPlugin {
	
    Logger log;
    DataStore dataStore;
    
    // Dependencies Variables
    public static boolean vaultPresent = false;
    public static Economy econ = null;
    public static Permission perms = null;
    
    public void onEnable(){
        
        this.log = getLogger();
        this.dataStore = new DataStore(this);
        
        new GPREListener(this).registerEvents();

        if (checkVault()) {
            
            this.log.info("Vault has been detected and enabled.");
            
            if (setupEconomy()) {
                this.log.info("Vault is using " + econ.getName() + " as the economy plugin.");
            } else {
                this.log.warning("No compatible economy plugin detected [Vault].");
                this.log.warning("Disabling plugin.");
                getPluginLoader().disablePlugin(this);
                return;
            }
            
            if (setupPermissions()) {
                this.log.info("Vault is using " + perms.getName() + " for the permissions.");
            } else {
                this.log.warning("No compatible permissions plugin detected [Vault].");
                this.log.warning("Disabling plugin.");
                getPluginLoader().disablePlugin(this);
                return;
            }
            
        }

        loadConfig(false);
        
    }
    
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    	
    	if(command.getName().equalsIgnoreCase("gpre") && sender.hasPermission("gprealestate.command")){
    		
    		if(args.length == 0){
    			sender.sendMessage(dataStore.chatPrefix + ChatColor.GREEN + "Unknown. Use 'gpre help' for info.");
    			return true;
    		}
    		else if(args.length == 1){
    			
    			if(args[0].equalsIgnoreCase("version") && sender.hasPermission("gprealestate.admin")){
    				sender.sendMessage(dataStore.chatPrefix + ChatColor.GREEN + "You are running " + ChatColor.RED + dataStore.pdf.getName() + ChatColor.GREEN + " version " + ChatColor.RED + dataStore.pdf.getVersion());
    				return true;
    			}
    			else if(args[0].equalsIgnoreCase("reload") && sender.hasPermission("gprealestate.admin")){
    				loadConfig(true); 
    				sender.sendMessage(dataStore.chatPrefix + ChatColor.GREEN + "The config file was succesfully reloaded.");
    				return true;
    			}
//    			else if(args[0].equalsIgnoreCase("sell")){
//    				sender.sendMessage(dataStore.chatPrefix + ChatColor.GREEN + "Please use gpre sell [price]");
//    				return true;
//    			}
    			else if(args[0].equalsIgnoreCase("help")){
    				sender.sendMessage(dataStore.chatPrefix + ChatColor.GREEN + "Commands: -Permission");
    				sender.sendMessage(dataStore.chatPrefix + ChatColor.GREEN + "gpre version | -gprealestate.admin");
    				sender.sendMessage(dataStore.chatPrefix + ChatColor.GREEN + "gpre reload: | -gprealestate.admin");
    				//sender.sendMessage(dataStore.chatPrefix + ChatColor.GREEN + "gpre sell [price]: | -gprealestate.command");
    				return true;
    			}
    			else {
    				sender.sendMessage(dataStore.chatPrefix + ChatColor.GREEN + "Unknown. Use 'gpre help' for info");
        			return true;
    			}
    			
    		}
//    		else if(args.length == 2){
//    			 if(args[0].equalsIgnoreCase("sell")){
//    				Player pl = (Player)sender; 
//    				Location pos = pl.getLocation();
//    	            Block sign = pl.getWorld().getBlockAt(pos);
//    	            Sign s = (Sign) sign.getState();
//    	 
//    	            s.setLine(0, "[re]");
//    	            s.setLine(1, args[1]);
//    	            s.update();
//    				sender.sendMessage(dataStore.chatPrefix + ChatColor.GREEN + "Success!");
//    				return true;
//    			}
//    			
//    		}
    	
    		
    		    		
    	}
    	else {
    		sender.sendMessage(ChatColor.RED + "You do not have permissions to use this command.");
			return false;
    	}
    	
    	return false;
    	
    }
    
    private void loadConfig(boolean reload){
    	
    	FileConfiguration config = YamlConfiguration.loadConfiguration(new File(dataStore.configFilePath));
        FileConfiguration outConfig = new YamlConfiguration();
        
        
    	// Loading the config file items that exsists or setting the default values.
        dataStore.cfgSignShort = config.getString("GPRealEstate.Keywords.Signs.Short", "[RE]");
        dataStore.cfgSignLong = config.getString("GPRealEstate.Keywords.Signs.Long", "[RealEstate]");
        
        dataStore.cfgRentKeywords = dataStore.stringToList(config.getString("GPRealEstate.Keywords.Actions.Renting", "Rent;Renting;Rental;For Rent"));
        dataStore.cfgSellKeywords = dataStore.stringToList(config.getString("GPRealEstate.Keywords.Actions.Selling", "Sell;Selling;For Sale"));
        
        dataStore.cfgReplaceRent = config.getString("GPRealEstate.Keywords.Actions.ReplaceRent", "FOR LEASE");
        dataStore.cfgReplaceSell = config.getString("GPRealEstate.Keywords.Actions.ReplaceSell", "FOR SALE");
        dataStore.cfgReplaceValue = config.getInt("GPRealEstate.Keywords.Actions.BuyPrice", 5);
        
        dataStore.cfgEnableLeasing = config.getBoolean("GPRealEstate.Rules.EnableLeasing", false);
        dataStore.cfgAllowSellingParentAC = config.getBoolean("GPRealEstate.Rules.AllowSellingParentAC", false);
        dataStore.cfgIgnoreClaimSize = config.getBoolean("GPRealEstate.Rules.IgnoreSizeLimit", false);
        
        if(!reload) {
        	// Letting the console know the "Keywords"
        	this.log.info("Signs will be using the keywords \"" + dataStore.cfgSignShort + "\" or \"" + dataStore.cfgSignLong + "\"");
        }
        
        // Saving the config informations into the file.
        outConfig.set("GPRealEstate.Keywords.Signs.Short", dataStore.cfgSignShort);
        outConfig.set("GPRealEstate.Keywords.Signs.Long", dataStore.cfgSignLong);
        outConfig.set("GPRealEstate.Keywords.Actions.Renting", dataStore.listToString(dataStore.cfgRentKeywords));
        outConfig.set("GPRealEstate.Keywords.Actions.Selling", dataStore.listToString(dataStore.cfgSellKeywords));
        outConfig.set("GPRealEstate.Keywords.Actions.ReplaceRent", dataStore.cfgReplaceRent);
        outConfig.set("GPRealEstate.Keywords.Actions.ReplaceSell", dataStore.cfgReplaceSell);
        outConfig.set("GPRealEstate.Keywords.Actions.BuyPrice", dataStore.cfgReplaceValue);
        outConfig.set("GPRealEstate.Rules.EnableLeasing", dataStore.cfgEnableLeasing);
        outConfig.set("GPRealEstate.Rules.IgnoreSizeLimit", dataStore.cfgIgnoreClaimSize);
        outConfig.set("GPRealEstate.Rules.AllowSellingParentAC", dataStore.cfgAllowSellingParentAC);
        
        try {
        	outConfig.save(dataStore.configFilePath);
        }
        catch(IOException exception){
        	this.log.info("Unable to write to the configuration file at \"" + dataStore.configFilePath + "\"");
        }
        
    }

    public void addLogEntry(String entry) {
        try {
            File logFile = new File(dataStore.logFilePath);
            
            if (!logFile.exists()) { 
            	logFile.createNewFile(); 
            }
            
            FileWriter fw = new FileWriter(logFile, true);
            PrintWriter pw = new PrintWriter(fw);
            
            pw.println(entry);
            pw.flush();
            pw.close();
        } 
        catch (IOException e) { e.printStackTrace(); }
    }

    private boolean checkVault(){
        vaultPresent = getServer().getPluginManager().getPlugin("Vault") != null;
        return vaultPresent;
    }

    private boolean setupEconomy(){
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        econ = (Economy)rsp.getProvider();
        return econ != null;
    }

    private boolean setupPermissions(){
        RegisteredServiceProvider<Permission> rsp = getServer().getServicesManager().getRegistration(Permission.class);
        perms = (Permission)rsp.getProvider();
        return perms != null;
    }
}