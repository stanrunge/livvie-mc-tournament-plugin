package dev.stanrunge;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ResetPlaytimeCommand implements CommandExecutor {
    private LivvieMinecraftTournamentPlugin plugin;

    public ResetPlaytimeCommand(LivvieMinecraftTournamentPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender.hasPermission("playtime.reset")) {
            if (args.length == 1) {
                Player target = Bukkit.getPlayer(args[0]);
                if (target != null) {
                    plugin.resetPlaytime(target);
                    sender.sendMessage("Playtime for " + target.getName() + " has been reset.");
                } else {
                    sender.sendMessage("Player not found.");
                }
            } else {
                plugin.resetPlaytimes();
                sender.sendMessage("Playtimes have been reset.");
            }
            return true;
        } else {
            sender.sendMessage("You do not have permission to use this command.");
            return false;
        }
    }
}