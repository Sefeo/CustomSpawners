package CustomSpawners.command;

import CustomSpawners.manager.Manager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class Commands implements CommandExecutor {

    private Manager manager;

    public Commands(Manager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        Player p = (Player) sender;
        if(cmd.getName().equals("spawnerCustom") || cmd.getName().equals("spawnerC") || cmd.getName().equals("sc")) { // команда кастомного спавнера
            if (!p.isOp()) {
                p.sendMessage(ChatColor.RED + "У вас недостаточно прав");
                return true;
            }
            if(args.length < 3) {
                p.sendMessage(ChatColor.WHITE + "/spawnerCustom [type] [delay] [mobsPerSpawn]");
                return true;
            }
            if(args[0].length() == 0 || args[1].length() == 0 || args[2].length() == 0) {
                p.sendMessage(ChatColor.WHITE + "/spawnerCustom [type] [delay] [mobsPerSpawn]");
                return true;
            }

            try { // проверяем являются ли 1-2 аргументы числами
                Integer.parseInt(args[1]);
                Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                p.sendMessage(ChatColor.WHITE + "/spawnerCustom [type] [delay] [mobsPerSpawn]");
                return true;
            }

            String type = args[0].toUpperCase(); // проверяем есть ли энтити вида 3-го аргумента
            try{
                EntityType.valueOf(type);
            } catch(IllegalArgumentException exp){
                p.sendMessage("Invalid mob type");
                return true;
            }

            int delay = Integer.parseInt(args[1]);
            int amount = Integer.parseInt(args[2]);

            if(delay > 600 || delay < 1) {
                p.sendMessage("Delay: 1-600 seconds");
                return true;
            }
            if(amount > 10 || amount < 1) {
                p.sendMessage("Amount: 1-10 per spawn");
                return true;
            }

            ItemStack spawner = manager.getSpawner().createSpawner(type, delay, amount,0); // лвл 0 = указание на кастомность
            p.getInventory().addItem(spawner); // и выдаем сам спавнер
            p.sendMessage("Created spawner with custom values: type - " + type + ", delay - " + delay + "ticks, mobsPerSpawn - " + amount);
            p.sendMessage(ChatColor.RED + "Данный спавнер нужен только для теста, его нельзя выключить и при ломании он не выпадет");
            return true;
        }

        if(cmd.getName().equals("spawner") || cmd.getName().equals("sp")) {
            if (!p.isOp()) {
                p.sendMessage(ChatColor.RED + "У вас недостаточно прав");
                return true;
            }
            if(args.length < 2) {
                p.sendMessage(ChatColor.WHITE + "/spawner [type] [level]");
                return true;
            }
            if(args[0].length() == 0 || args[1].length() == 0 ) {
                p.sendMessage(ChatColor.WHITE + "/spawner [type] [level]");
                return true;
            }

            try { // проверяем является ли первый аргумент числом
                Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                p.sendMessage(ChatColor.WHITE + "/spawner [type] [level]");
                return true;
            }

            int level = Integer.parseInt(args[1]); // не даем дать левел выше указанного и ниже 1
            if(level > manager.getSpawner().maxSpawnerLvl || level < 1) {
                p.sendMessage(ChatColor.WHITE + "Level: 1 - " + manager.getSpawner().maxSpawnerLvl);
                return true;
            }

            String type = args[0].toUpperCase(); // проверяем есть ли тип моба из аргумента
            try {
                EntityType.valueOf(type);
            } catch(IllegalArgumentException exp){
                p.sendMessage("Invalid mob type");
                return true;
            }

            ItemStack spawner = manager.getSpawner().createSpawnerFromLevel(type, level, true); // и выдаем сам спавнер
            p.getInventory().addItem(spawner);
            p.sendMessage("Created spawner: type - " + type + ", level - " + level);
            return true;
        }
        return false;
    }
}
