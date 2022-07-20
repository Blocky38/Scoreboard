package de.xite.scoreboard.main;

import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;

import de.xite.scoreboard.depend.BStatsMetrics;
import de.xite.scoreboard.depend.LuckPermsListener;
import de.xite.scoreboard.depend.PlaceholderAPIExpansion;
import de.xite.scoreboard.depend.VaultAPI;
import net.luckperms.api.LuckPerms;

public class ExternalPlugins {
	static PowerBoard pl = PowerBoard.pl;
	static Boolean debug = PowerBoard.debug;
	
	// APIs
	public static LuckPerms luckPerms = null;
	// Supported Plugins
	public static boolean hasVault = false;
	public static boolean hasPapi = false;
	public static boolean hasLuckPerms = false;
	
	public static void initializePlugins() {
		// ---- Check for compatible plugins ---- //
		if(Bukkit.getPluginManager().isPluginEnabled("Vault")) {
			if(debug)
				pl.getLogger().info("Loading Vault...");
			if(VaultAPI.setupEconomy()) {
				hasVault = true;
				if(debug)
					pl.getLogger().info("Successfully loaded Vault-Economy!");
			}
			//setupChat();
		}
		if(Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
			hasPapi = true;
			new PlaceholderAPIExpansion().register();
		}
			
		
		if(Bukkit.getPluginManager().isPluginEnabled("LuckPerms")) {
			hasLuckPerms = true;
			if(pl.getConfig().getBoolean("ranks.luckperms-api.enable") || pl.getConfig().getString("ranks.permissionsystem").equalsIgnoreCase("luckperms")) {
				RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
				if(provider != null)
					luckPerms = provider.getProvider();
				new LuckPermsListener(pl, luckPerms);
			}else
				if(luckPerms != null)
					pl.getLogger().warning("You have changed the rank permissions system from LuckPerms to something different. LuckPerms cannot be completely disabled whith a PB reload. Please restart your server soon.");
		}
		// BStats analytics
		try {
			int pluginId = 6722;
			BStatsMetrics metrics = new BStatsMetrics(pl, pluginId);
			// Custom charts
			metrics.addCustomChart(new BStatsMetrics.SimplePie("update_auto_update", () -> pl.getConfig().getBoolean("update.autoupdater") ? "Enabled" : "Disabled"));
			metrics.addCustomChart(new BStatsMetrics.SimplePie("update_notifications", () -> pl.getConfig().getBoolean("update.notification") ? "Enabled" : "Disabled"));
	        
			metrics.addCustomChart(new BStatsMetrics.SimplePie("setting_use_scoreboard", () -> pl.getConfig().getBoolean("scoreboard") ? "Enabled" : "Disabled"));
			metrics.addCustomChart(new BStatsMetrics.SimplePie("setting_use_tablist_text", () -> pl.getConfig().getBoolean("tablist.text") ? "Enabled" : "Disabled"));
			metrics.addCustomChart(new BStatsMetrics.SimplePie("setting_use_tablist_ranks", () -> pl.getConfig().getBoolean("tablist.ranks") ? "Enabled" : "Disabled"));
			metrics.addCustomChart(new BStatsMetrics.SimplePie("setting_use_chat", () -> pl.getConfig().getBoolean("chat.ranks") ? "Enabled" : "Disabled"));
			metrics.addCustomChart(new BStatsMetrics.SimplePie("setting_permsystem", () -> pl.getConfig().getString("ranks.permissionsystem").toLowerCase()));
			if(PowerBoard.debug)
				pl.getLogger().info("Analytics sent to BStats");
		} catch (Exception e) {
			pl.getLogger().warning("Could not send analytics to BStats!");
		}
	}

}
