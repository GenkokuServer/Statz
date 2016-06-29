package me.staartvin.statz.listeners;

import java.util.HashMap;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;

import me.staartvin.statz.Statz;
import me.staartvin.statz.datamanager.PlayerStat;
import me.staartvin.statz.datamanager.player.PlayerInfo;
import me.staartvin.statz.util.StatzUtil;
import net.md_5.bungee.api.ChatColor;

public class PlayerJoinListener implements Listener {

	private final Statz plugin;

	public PlayerJoinListener(final Statz plugin) {
		this.plugin = plugin;
	}

	@EventHandler
	public void onJoin(final PlayerJoinEvent event) {

		final PlayerStat stat = PlayerStat.JOINS;

		// Get player
		final Player player = event.getPlayer();

		// Update name in database.
		plugin.getSqlConnector().setObjects(plugin.getSqlConnector().getSQLiteTable("players"),
				StatzUtil.makeQuery("uuid", player.getUniqueId().toString(), "playerName", player.getName()));

		// Get player info.
		final PlayerInfo info = plugin.getDataManager().getPlayerInfo(player.getUniqueId(), stat);

		// Get current value of stat.
		int currentValue = 0;

		// Check if it is valid!
		if (info.isValid()) {
			currentValue = Integer.parseInt(info.getResults().get(0).get("value").toString());
		}

		// Update value to new stat.
		plugin.getDataManager().setPlayerInfo(player.getUniqueId(), stat,
				StatzUtil.makeQuery("uuid", player.getUniqueId().toString(), "value", (currentValue + 1)));
		
		// Player has joined, so create a timer that runs every minute to add time.
		BukkitRunnable run = new BukkitRunnable() {
			public void run(){
				if (!player.isOnline()) {
					System.out.println("Cancel task");
					this.cancel();
					return;
				}
				
				// Get player info.
				final PlayerInfo info = plugin.getDataManager().getPlayerInfo(player.getUniqueId(), PlayerStat.TIME_PLAYED);

				// Get current value of stat.
				int currentValue = 0;

				// Check if it is valid!
				if (info.isValid()) {
					for (HashMap<String, Object> map : info.getResults()) {
						if (map.get("world") != null && map.get("world").toString().equalsIgnoreCase(player.getWorld().getName())) {
							currentValue += Integer.parseInt(map.get("value").toString());
						}
					}
				}
				
				player.sendMessage(ChatColor.GOLD + "Statz: " + ChatColor.GREEN + "Updated your play time to " + (currentValue + 1));

				// Update value to new stat.
				plugin.getDataManager().setPlayerInfo(player.getUniqueId(), PlayerStat.TIME_PLAYED,
						StatzUtil.makeQuery("uuid", player.getUniqueId().toString(), "value", (currentValue + 1), "world", player.getWorld().getName()));
				
			}
		};
		
		run.runTaskTimer(plugin, (currentValue == 0 ? 0 : 20*60) /*If currentValue is 0, schedule a check immediately, otherwise after a minute*/, 20 * 60 /*Every minute*/);
	}
}
