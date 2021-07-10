package de.xite.scoreboard.manager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import de.xite.scoreboard.main.Main;

public class ScoreboardManager {
	// The name of the scoreboard
	String name;
	
	// all scores with all animations
	HashMap<Integer, ArrayList<String>> scores = new HashMap<>(); // <score ID, <animations>>
	
	// the title with all animations
	ArrayList<String> title = new ArrayList<>(); // <animatons>
	
	// Store all schedulers to stop them later
	ArrayList<Integer> scheduler = new ArrayList<>();
	
	// Store all players with this scoreboard
	ArrayList<Player> players = new ArrayList<>();
	
	public ScoreboardManager(String name) {
		this.name = name;
		
		// Get the config
		File f = new File(Main.pluginfolder+"/scoreboards/"+name+".yml");
		if(!f.exists()) {
			Main.pl.getLogger().severe("Could not load Scoreboard named "+name+", because the config file does not exists!");
			return;
		}
		YamlConfiguration cfg = YamlConfiguration.loadConfiguration(f);
		importScores(cfg); // Import all scores
		importTitle(cfg); // Import the title
	}
	public static ScoreboardManager get(String name) {
		if(!Main.scoreboards.containsKey(name))
			Main.scoreboards.put(name, new ScoreboardManager(name));
		return Main.scoreboards.get(name);
	}
	
	// Import
	private void importScores(YamlConfiguration cfg) {
		for(String s : cfg.getConfigurationSection("").getValues(false).keySet()) {
			try {
				int id = Integer.parseInt(s);
				if(cfg.getStringList(id+".scores") != null && !cfg.getStringList(id+".scores").isEmpty()) {
					
					// Add all animations
	        		scores.put(id, new ArrayList<String>());
	        		scores.get(id).addAll(cfg.getStringList(id+".scores")); 
	        		
	        		// Migrate from old syntax
	        		if(cfg.getInt(id+".wait") != 0) {
	        			cfg.set(id+".speed", cfg.getInt(id+".wait"));
	        			cfg.set(id+".wait", null);
	        			cfg.save(new File(Main.pluginfolder+"/"+name+".yml"));
	        		}
	        		
	        		// Start the animation
					startScoreAnimation(id, cfg.getInt(id+".speed"));
	    		}
			}catch (Exception e) {}
		}
		if(scores.size() > 14) // Check if more than 14 scores
			Main.pl.getLogger().warning("You have more than 14 scors in you scoreboard! Some scores cannot be displayed! This is a problem in Minecraft.");
		
	}
	private void importTitle(YamlConfiguration cfg) {
		// Migrate from old syntax
		if(cfg.getInt("titel.wait") != 0) {
			cfg.set("titel.speed", cfg.getInt("titel.wait"));
			cfg.set("titel.wait", null);
			try {
				cfg.save(new File(Main.pluginfolder+"/"+name+".yml"));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		title.addAll(cfg.getStringList("titel.titles"));
		startTitleAnimation(cfg.getInt("titel.speed"));
	}
	
	
	// ---- Start the animations ---- //
	
	int currentTitleStep; // animation id
	private void startTitleAnimation(int speed) {
		// check if scheduler is needed (don't schedule if higher than '9999')
		if(speed >= 9999 || speed < 0) {
			if(Main.debug)
				Main.pl.getLogger().info("Scoreboard-Title (Name: "+name+"): no animation needed (speed higher than 9999 or negative)");
			return;
		}else
			if(Main.debug)
				Main.pl.getLogger().info("Scoreboard-Title (Name: "+name+"): animation started");
		
		currentTitleStep = 0;
		// Start animation scheduler
		scheduler.add(
			Bukkit.getScheduler().scheduleSyncRepeatingTask(Main.pl, new Runnable() {
				@Override
				public void run() {
					String s = title.get(currentTitleStep); // get the current score (text)
					for(Player p : players)
						ScoreboardPlayer.setTitle(p, p.getScoreboard(), s, true, get(name)); // set the score
					if(currentTitleStep >= title.size()-1) {
						currentTitleStep = 0;
					}else
						currentTitleStep++;
				}
			}, 20, speed)
		);
	}
	
	HashMap<Integer, Integer> currentScoreStep = new HashMap<>(); // Score ID; Animation ID
	private void startScoreAnimation(int id, int speed) {
		currentScoreStep.put(id, 0);
		
		// check if scheduler is needed (don't schedule if higher than '9999')
		if(speed >= 9999) {
			if(Main.debug)
				Main.pl.getLogger().info("Scoreboard-Score (ID: "+id+", Name: "+name+"): no animation needed");
			return;
		}else
			if(Main.debug)
				Main.pl.getLogger().info("Scoreboard-Score (ID: "+id+", Name: "+name+"): animation started");
			
		// Start animation scheduler
		scheduler.add(
			Bukkit.getScheduler().scheduleSyncRepeatingTask(Main.pl, new Runnable() {
				@Override
				public void run() {
					String s = scores.get(id).get(currentScoreStep.get(id)); // get the current score (text)
					for(Player p : players)
						ScoreboardPlayer.setScore(p, p.getScoreboard(), s, scores.size()-id-1, true, get(name)); // set the score
					
					if(currentScoreStep.get(id) >= scores.get(id).size()-1) {
						currentScoreStep.replace(id, 0);
					}else
						currentScoreStep.replace(id, currentScoreStep.get(id)+1);
					}
				}, 20, speed)
			);
	}
	public void addPlayer(Player p) {
		if(!players.contains(p))
			players.add(p);
	}
	public void removePlayer(Player p) {
		if(players.contains(p))
			players.remove(p);
	}
	public String getCurrentTitle() {
		return title.get(currentTitleStep);
	}
	public ArrayList<String> getCurrentScore() {
		ArrayList<String> list = new ArrayList<>();
		for(Entry<Integer, Integer> s : currentScoreStep.entrySet()) {
			int score = s.getKey();
			int animation = s.getValue();
			list.add(scores.get(score).get(animation));
		}
		return list;
	}
	public String getName() {
		return this.name;
	}
	
	public static void unregister(String name) {
		ScoreboardManager sm = get(name);
		for(int i : sm.scheduler)
			Bukkit.getScheduler().cancelTask(i);
		sm.scores.clear();
		sm.title.clear();
		sm.currentScoreStep.clear();
		sm.currentTitleStep = 0;
		sm.name = null;
		sm.players.clear();
		Main.scoreboards.remove(name);
	}
}
