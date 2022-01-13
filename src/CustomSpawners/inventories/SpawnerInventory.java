package CustomSpawners.inventories;

import CustomSpawners.data.SpawnerData;
import CustomSpawners.enums.Mobs;
import CustomSpawners.manager.Manager;
import javafx.util.Pair;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

public class SpawnerInventory {

    private Manager manager;
    public HashMap<UUID, String> spawnerBlockType = new HashMap<>();
    public HashMap<UUID, CreatureSpawner> spawnerBlock = new HashMap<>();
    public HashMap<UUID, Integer> spawnerBlockLevel = new HashMap<>();

    public SpawnerInventory(Manager manager) {
        this.manager = manager;
    }

    public Inventory createSpawnerInv(Player p, Block block) {
        SpawnerData spawnerStats = manager.getSpawner().selectSpawnerStatsSQL(block);
        int level = spawnerStats.getLevel();
        String entityString = spawnerStats.getType();

        spawnerBlockLevel.put(p.getUniqueId(), level); // записываем статы спавнера в хешмап чтобы можно было улучшить его в onClick
        spawnerBlockType.put(p.getUniqueId(), entityString.toUpperCase());
        CreatureSpawner cs = (CreatureSpawner) block.getState();
        spawnerBlock.put(p.getUniqueId(), cs);

        ItemStack statsItem = new ItemStack(Material.MOB_SPAWNER); // настраиваем предмет статистики спавнера
        ItemMeta statsMeta = statsItem.getItemMeta();
        statsMeta.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + "Характеристики спавнера");
        ArrayList<String> statsLore = new ArrayList<>();
        statsLore.add(ChatColor.WHITE + "Тип: " + ChatColor.YELLOW + Mobs.MobsTranslated.valueOf(entityString.toUpperCase()));
        if(level == 0) {
            statsLore.add("");
            statsLore.add(ChatColor.YELLOW + "Спавнер является кастомным");
        }
        else {
            statsLore.add(ChatColor.WHITE + "Уровень: " + ChatColor.YELLOW + level);
            statsLore.add("");
            statsLore.add(ChatColor.WHITE + "Частота призыва: " + ChatColor.YELLOW + manager.getSpawner().getDelayFromLevel(level) / 20 + " секунд");
            statsLore.add(ChatColor.WHITE + "Количество существ: " + ChatColor.YELLOW + "до " + manager.getSpawner().getMobAmountFromLevel(level));
            if(manager.getSpawner().isSpawnerEnabled(block)) statsLore.add(ChatColor.WHITE + "Статус: " + ChatColor.YELLOW + "Включён");
            else statsLore.add(ChatColor.WHITE + "Статус: " + ChatColor.YELLOW + "Выключен");
        }
        statsMeta.setLore(statsLore);
        statsItem.setItemMeta(statsMeta);

        ItemStack upgradeItem = new ItemStack(Material.STAINED_GLASS); // настраиваем предмет улучшения спавнера
        ItemMeta upgradeMeta = upgradeItem.getItemMeta();
        upgradeItem.setDurability((short) 5);
        if(level < manager.getSpawner().maxSpawnerLvl && level != 0) {
            upgradeMeta.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + "Повысить уровень");
            ArrayList<String> upgradeLore = new ArrayList<>();
            int newLvl = level+1;
            upgradeLore.add(ChatColor.YELLOW + "" + level + " --> " + newLvl);
            upgradeMeta.setLore(upgradeLore);
        } else upgradeMeta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + "Максимальный уровень");
        upgradeItem.setItemMeta(upgradeMeta);

        ItemStack degradeItem = new ItemStack(Material.STAINED_GLASS); // настраиваем предмет понижения уровня спавна
        ItemMeta degradeMeta = degradeItem.getItemMeta();
        degradeItem.setDurability((short) 14);
        if(level <= 1) degradeMeta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + "Минимальный уровень");
        else {
            int newLvl = level-1;
            degradeMeta.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + "Понизить уровень");
            ArrayList<String> degradeLore = new ArrayList<>();
            degradeLore.add(ChatColor.YELLOW + "" + level + " --> " + newLvl);
            degradeMeta.setLore(degradeLore);
        }
        degradeItem.setItemMeta(degradeMeta);

        ItemStack typeItem = new ItemStack(Material.MONSTER_EGG); // настраиваем предмет понижения уровня спавна
        ItemMeta typeMeta = typeItem.getItemMeta();
        typeMeta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + "Сменить тип");
        ArrayList<String> typeLore = new ArrayList<>();
        typeLore.add(ChatColor.WHITE + "Для смены типа необходимо положить");
        typeLore.add(ChatColor.WHITE + "в меню " + ChatColor.YELLOW + manager.getSpawner().eggForTypeChange*level + " яиц призыва " + ChatColor.WHITE + "одного вида");
        typeLore.add(ChatColor.WHITE + "и подтвердить смену.");
        typeMeta.setLore(typeLore);
        typeItem.setItemMeta(typeMeta);

        ItemStack switchItem;
        ItemMeta switchMeta;

        if(manager.getSpawner().isSpawnerEnabled(block)) {
            switchItem = new ItemStack(Material.REDSTONE_TORCH_ON);
            switchMeta = switchItem.getItemMeta();
            switchMeta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + "Выключить спавнер");
            switchItem.setItemMeta(switchMeta);
        } else {
            switchItem = new ItemStack(Material.REDSTONE_TORCH_ON);
            switchMeta = switchItem.getItemMeta();
            switchMeta.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + "Включить спавнер");
            switchItem.setItemMeta(switchMeta);
        }

        Inventory spawnerInv = Bukkit.createInventory(p, 45, "Спавнер"); // создаем инвентарь и туда предметы
        spawnerInv.setItem(4, typeItem);
        spawnerInv.setItem(20, degradeItem);
        spawnerInv.setItem(22, statsItem);
        spawnerInv.setItem(24, upgradeItem);
        if(level > 0) spawnerInv.setItem(40, switchItem);

        for(int i = 0; i < spawnerInv.getSize(); i ++) { // ставим стекло
            if(spawnerInv.getItem(i) != null) continue;
            /*if(i <= 19) spawnerInv.setItem(i, new ItemStack(Material.STAINED_GLASS_PANE));
            else if(i >= 25) spawnerInv.setItem(i, new ItemStack(Material.STAINED_GLASS_PANE));*/
            ItemStack item = new ItemStack(Material.STAINED_GLASS_PANE);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(" ");
            item.setItemMeta(meta);
            spawnerInv.setItem(i, item);
        }

        return spawnerInv;
    }

    public Inventory createChangeTypeInv(Player p, Block block, int level) {
        CreatureSpawner cs = (CreatureSpawner) block.getState();
        spawnerBlock.put(p.getUniqueId(), cs);
        spawnerBlockLevel.put(p.getUniqueId(), level);

        Inventory inv = Bukkit.createInventory(p, 45, "Смена типа");
        for(int i = 0; i < inv.getSize(); i ++) { // ставим стекло
            if(inv.getItem(i) != null) continue;
            if(i == 22) continue;
            ItemStack item = new ItemStack(Material.STAINED_GLASS_PANE);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(" ");
            item.setItemMeta(meta);
            inv.setItem(i, item);
        }

        ItemStack accept = new ItemStack(Material.STAINED_GLASS);
        accept.setDurability((short) 5);
        ItemMeta acceptMeta = accept.getItemMeta();
        acceptMeta.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + "Подтвердить");
        accept.setItemMeta(acceptMeta);

        ItemStack back = new ItemStack(Material.STAINED_GLASS);
        back.setDurability((short) 14);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + "Вернуться");
        back.setItemMeta(backMeta);

        inv.setItem(20, back);
        inv.setItem(24, accept);
        return inv;
    }
}
