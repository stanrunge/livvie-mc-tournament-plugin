package dev.stanrunge;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;

import net.kyori.adventure.text.Component;

public class LivvieMinecraftTournamentPlugin extends JavaPlugin implements Listener {
    private HashMap<UUID, Integer> playerPlaytimes = new HashMap<>();
    private Scoreboard scoreboard;
    private Objective xpObjective;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);

        scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        Component displayName = Component.text("Livvie's XP Race Tourney (Day 1)");

        if (scoreboard.getObjective("xpObjective") != null) {
            scoreboard.getObjective("xpObjective").unregister();
        }

        xpObjective = scoreboard.registerNewObjective("xpObjective", Criteria.DUMMY, displayName);
        xpObjective.setDisplaySlot(DisplaySlot.SIDEBAR);

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream("playerPlaytimes.ser"))) {
            Object obj = ois.readObject();
            if (!(obj instanceof HashMap)) {
                throw new ClassNotFoundException();
            }
            playerPlaytimes = (HashMap<UUID, Integer>) obj;
        } catch (IOException | ClassNotFoundException e) {
            getLogger().warning("Failed to load login times");
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                for (String entry : scoreboard.getEntries()) {
                    scoreboard.resetScores(entry);
                }

                List<Player> players = new ArrayList<>();
                for (UUID playerId : playerPlaytimes.keySet()) {
                    Player player = getServer().getPlayer(playerId);
                    if (player != null) {
                        players.add(player);
                    }
                }

                players.sort(Comparator.comparingInt(Player::getTotalExperience).reversed());

                setScoreboardIndex(players);

                updateOnlinePlayersPlaytime();
            }
        }.runTaskTimer(this, 0L, 20L);

        this.getCommand("resetplaytime").setExecutor(new ResetPlaytimeCommand(this));
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        player.setScoreboard(scoreboard);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Save playerPlaytimes to file
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("playerPlaytimes.ser"))) {
            oos.writeObject(playerPlaytimes);
        } catch (IOException e) {
            getLogger().warning("Failed to save login times");
        }
    }

    public String getPlayerDisplayText(Player player) {
        int playerPlaytime = playerPlaytimes.get(player.getUniqueId());

        long playTime = playerPlaytime * 1000L;

        // Convert playtime from milliseconds to hours, minutes, and seconds
        long hours = TimeUnit.MILLISECONDS.toHours(playTime);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(playTime) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(playTime) % 60;

        // Format the playtime as "h:mm:ss"
        return String.format("%s | Today's Playtime: %d:%02d:%02d | XP: %d", player.getName(), hours, minutes, seconds,
                player.getTotalExperience());
    }

    public void resetPlaytimes() {
        for (UUID playerId : playerPlaytimes.keySet()) {
            playerPlaytimes.put(playerId, 0);
        }
    }

    public void resetPlaytime(Player player) {
        playerPlaytimes.put(player.getUniqueId(), 0);
    }

    public void updateOnlinePlayersPlaytime() {
        for (Player player : getServer().getOnlinePlayers()) {
            UUID playerId = player.getUniqueId();

            // Get the player's playtime, or 0 if the player has no playtime
            int playtime = playerPlaytimes.containsKey(playerId) ? playerPlaytimes.get(playerId) : 0;

            playerPlaytimes.put(playerId, playtime + 1);
        }
    }

    private void setScoreboardIndex(List<Player> players) {
        int i = 1;
        for (Player player : players) {
            String playTimeFormatted = getPlayerDisplayText(player);

            Score score = xpObjective.getScore(playTimeFormatted);
            score.setScore(i);

            i++;
        }
    }
}