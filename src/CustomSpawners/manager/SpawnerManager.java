package CustomSpawners.manager;

import CustomSpawners.data.SpawnerData;
import CustomSpawners.enums.Mobs;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class SpawnerManager {

    private Manager manager;

    // всё ниже в конфиг переносить нет смысла, пусть файналами будет

    public final int maxSpawnerLvl = 5; // Максимальный уровень спавнеров
    public final int eggForTypeChange = 5; // Количество яиц для смены типа спавнера

    private final int baseDelay = 400; // Базовая задержка спавнера на 1 уровне
    private final int delayReduction = 40; // Уменьшение задержки спавнера за каждый уровень

    private final int baseMobAmount = 1; // Базовое кол-во спавна мобов на 1 уровне
    private final int mobAmountFrequency = 2; // Каждые сколько лвлов будет увеличиваться показатель спавна мобов
    private final int mobAmountPerLevel = 1; // На сколько мобов повышать спавн за каждое улучшение статы (см. переменную выше)


    SpawnerManager(Manager manager) {
        this.manager = manager;
    }

    public ItemStack createSpawner(String type, int delay, int spawnCount, int level) { // в этом методе надо вручную вводить задержку и кол-во мобов
        ItemStack spawner = new ItemStack(Material.MOB_SPAWNER);

        spawner = manager.getNBT().setTagCopy(spawner, "mob_type", type);
        spawner = manager.getNBT().setTagCopyInt(spawner, "level", level); // вешаем теги

        ItemMeta meta = spawner.getItemMeta();
        meta.setDisplayName(ChatColor.WHITE + "Спавнер [" + ChatColor.GREEN + Mobs.MobsTranslated.valueOf(manager.getNBT().getTag(spawner, "mob_type").toUpperCase()) + ChatColor.WHITE + "]");
        ArrayList<String> lore = new ArrayList<>();
        lore.add(ChatColor.YELLOW + "Спавнер является кастомным"); // ставим мету и имя
        meta.setLore(lore);
        spawner.setItemMeta(meta);

        BlockStateMeta bsm = (BlockStateMeta) spawner.getItemMeta();
        CreatureSpawner cs = (CreatureSpawner) bsm.getBlockState();

        cs.setSpawnedType(EntityType.fromName(type));
        cs.setDelay(delay);
        cs.setMinSpawnDelay(delay);
        cs.setMaxSpawnDelay(delay); // ставим статы
        cs.setSpawnCount(spawnCount);
        cs.update();

        bsm.setBlockState(cs);
        spawner.setItemMeta(bsm);

        return spawner;
    }

    public ItemStack createSpawnerFromLevel(String type, int level, boolean enabled) { // в этом методе задержка и число мобов свитчится от лвла спавнера
        ItemStack spawner = new ItemStack(Material.MOB_SPAWNER);

        spawner = manager.getNBT().setTagCopy(spawner, "mob_type", type.toUpperCase()); // вешаем теги
        spawner = manager.getNBT().setTagCopyInt(spawner, "level", level);

        if(enabled) spawner = manager.getNBT().setTagCopyInt(spawner, "enabled", 1);
        else spawner = manager.getNBT().setTagCopyInt(spawner, "enabled", 0);

        ItemMeta meta = spawner.getItemMeta();
        meta.setDisplayName(ChatColor.WHITE + "Спавнер [" + ChatColor.GREEN + Mobs.MobsTranslated.valueOf(manager.getNBT().getTag(spawner, "mob_type").toUpperCase()) + ChatColor.WHITE + "]");
        ArrayList<String> lore = new ArrayList<>();
        lore.add(ChatColor.WHITE + "Уровень: " + ChatColor.YELLOW + level); // ставим мету и имя
        if(enabled) lore.add(ChatColor.WHITE + "Статус: " + ChatColor.YELLOW + "Включён");
        else lore.add(ChatColor.WHITE + "Статус: " + ChatColor.YELLOW + "Выключен");
        meta.setLore(lore);
        spawner.setItemMeta(meta);

        BlockStateMeta bsm = (BlockStateMeta) spawner.getItemMeta();
        CreatureSpawner cs = (CreatureSpawner) bsm.getBlockState();
        int delay = getDelayFromLevel(level); // определяем статы от уровня
        int amount = getMobAmountFromLevel(level);

        cs.setSpawnedType(EntityType.fromName(type)); // и вешаем их
        cs.setDelay(delay);
        cs.setMinSpawnDelay(delay);
        cs.setMaxSpawnDelay(delay);
        if(enabled) cs.setSpawnCount(amount);
        else cs.setSpawnCount(0);
        cs.update();

        bsm.setBlockState(cs);
        spawner.setItemMeta(bsm);

        return spawner;
    }

    public boolean isSpawnerNatural(Block block) { // проверка на то, является ли спавнер кастомным или создан миром (в данже)
        String sql = "SELECT * FROM spawners WHERE `x` = ? AND `y` = ? AND `z` = ?";
        try (Connection con = manager.connect();
             PreparedStatement statement  = con.prepareStatement(sql)){

            statement.setDouble(1, block.getX());
            statement.setDouble(2, block.getY());
            statement.setDouble(3, block.getZ());

            ResultSet result = statement.executeQuery();
            return !result.next();

        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
            return false;
        }
    }

    public boolean isSpawnerEnabled(Block block) {
        CreatureSpawner spawner = (CreatureSpawner) block.getState();
        return spawner.getSpawnCount() > 0;
    }

    public void turnSpawnerIntoCustom(Block block) {
        CreatureSpawner spawner = (CreatureSpawner) block.getState();
        String type = spawner.getCreatureTypeName().toUpperCase();
        int level = 1;
        int delay = manager.getSpawner().getDelayFromLevel(level);
        int amount = manager.getSpawner().getMobAmountFromLevel(level);

        spawner.setSpawnedType(EntityType.fromName(type));
        spawner.setDelay(delay);
        spawner.setMaxSpawnDelay(delay);
        spawner.setMinSpawnDelay(delay);
        spawner.setSpawnCount(amount);
        spawner.setMaxNearbyEntities(50);
        spawner.update();

        insertSpawnerSQL(block, spawner.getCreatureTypeName(), level);
    }

    public void updateSpawnerStats(CreatureSpawner spawner, int delay, int mobAmount, boolean reverse) {
        spawner.setDelay(delay);
        if(!reverse) { // реверс нужен для того, чтобы консоль не орала, что MinSpawnDelay должно быть меньше Max и т.п.
            spawner.setMinSpawnDelay(delay);
            spawner.setMaxSpawnDelay(delay);
        } else {
            spawner.setMaxSpawnDelay(delay);
            spawner.setMinSpawnDelay(delay);
        }
        spawner.setSpawnCount(mobAmount);
        spawner.update();
    }

    public SpawnerData selectSpawnerStatsSQL(Block block) {
        int level = 0;
        String entityString = "PIG"; // стандартные значения

        String sql = "SELECT level, type FROM spawners WHERE `x` = ? AND `y` = ? AND `z` = ?";
        try (Connection con = manager.connect();
             PreparedStatement statement  = con.prepareStatement(sql)){

            statement.setDouble(1, block.getX());
            statement.setDouble(2, block.getY());
            statement.setDouble(3, block.getZ());

            ResultSet result = statement.executeQuery();

            level = result.getInt("level");
            entityString = result.getString("type");
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }

        return new SpawnerData(level, entityString);
    }

    public void insertSpawnerSQL(Block block, String type, int level) {
        String sql = "INSERT INTO spawners (x, y, z, type, level) "
                + "VALUES (?, ?, ?, ?, ? );";
        try (Connection con = manager.connect();
             PreparedStatement statement  = con.prepareStatement(sql)){

            statement.setDouble(1, block.getX());
            statement.setDouble(2, block.getY());
            statement.setDouble(3, block.getZ());
            statement.setString(4, type);
            statement.setInt(5, level);

            statement.executeUpdate();
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }
    }

    public void updateSpawnerSQL(String field, Object value, Block block) {
        String sql = "UPDATE spawners SET " + field + " = ? WHERE x = ? AND y = ? AND z = ? ";
        try (Connection con = manager.connect();
             PreparedStatement statement = con.prepareStatement(sql)) {

            if(value instanceof Integer) statement.setInt(1, Integer.parseInt(value.toString()));
            else statement.setString(1, value.toString());
            statement.setDouble(2, block.getX());
            statement.setDouble(3, block.getY());
            statement.setDouble(4, block.getZ());

            statement.executeUpdate();
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }
    }

    public int getDelayFromLevel(int level) { // получаем задержку спавнера в зависимости от лвла
        if(level == 1) return baseDelay;
        else return baseDelay-((level-1) * delayReduction);
    }

    public int getMobAmountFromLevel(int level) { // получаем кол-во заспавненных за один раз мобов в зависимости от лвла
        if(level == 1 || mobAmountFrequency == 0) return baseMobAmount; // 1 лвл или частота увеличения 0 = возвращаем базовое
        else {
            int amount = baseMobAmount;

            for(int i = 1; i <= maxSpawnerLvl; i+=mobAmountFrequency) {
                if(i >= level) break;
                amount+=mobAmountPerLevel;
            }
            return amount;
        }
    }
}
