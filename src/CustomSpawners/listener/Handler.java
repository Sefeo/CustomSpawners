package CustomSpawners.listener;

import CustomSpawners.data.SpawnerData;
import CustomSpawners.enums.Mobs;
import CustomSpawners.manager.Manager;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.SpawnerSpawnEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SpawnEggMeta;

import java.sql.*;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.bukkit.Bukkit.getServer;

public class Handler implements Listener {

    private Manager manager;
    public Handler(Manager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void spawnEvent(SpawnerSpawnEvent e) {
        Block spawner = e.getSpawner().getBlock();
        if(manager.getSpawner().isSpawnerNatural(spawner)) { // если мобов спавнит натуральный спавнер - превращаем его в кастомный
            e.setCancelled(true); // возможно придётся убрать в меру оптимизации, слишком часто запросы к бд делает, но пока что пусть будет
            manager.getSpawner().turnSpawnerIntoCustom(spawner);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e) {
        Block block = e.getBlockPlaced();

        if(block.getType().equals(Material.MOB_SPAWNER)) { // если поставили спавнер

            String type = manager.getNBT().getTag(e.getItemInHand(), "mob_type"); // берём его статы из тегов
            CreatureSpawner spawner = (CreatureSpawner) block.getState();

            if(type == null) { // если по натуральному (без тега), то перерабатываем его в кастомный и выставляем в переменную тип не по тегу
                manager.getSpawner().turnSpawnerIntoCustom(block);
                type = spawner.getSpawnedType().toString();
            }

            int level = manager.getNBT().getTagInt(e.getItemInHand(), "level");

            spawner.setSpawnedType(EntityType.fromName(type)); // ставим второстепенные штуки
            spawner.setMaxNearbyEntities(50);
            spawner.update();

            manager.getSpawner().insertSpawnerSQL(spawner.getBlock(), type, level); // записываем его в бд
        }
    }

    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        Block block = e.getBlock();
        Location loc = block.getLocation();
        Material type = e.getPlayer().getInventory().getItemInMainHand().getType();
        String sql;

        if(!block.getType().equals(Material.MOB_SPAWNER)) return; // если ломают именно спавнер

        int level;
        String entityString;

        if(manager.getSpawner().isSpawnerNatural(e.getBlock())) { // если спавнер натуральный
            CreatureSpawner cs = (CreatureSpawner) e.getBlock().getState();

            if(type.equals(Material.IRON_PICKAXE) || type.equals(Material.DIAMOND_PICKAXE) || type.equals(Material.GOLD_PICKAXE)) { // если он сломан киркой
                e.setExpToDrop(0);
                ItemStack spawner = manager.getSpawner().createSpawnerFromLevel(cs.getSpawnedType().toString(), 1, true);
                loc.getWorld().dropItemNaturally(loc, spawner); // дропаем кастомный 1-го лвла, то есть заменяем натуральный на кастомный
            }
            return; // если не киркой - просто ничего не делаем, пусть исчезает
        }
        // если спавнер кастомный - всё ниже
        if(type.equals(Material.IRON_PICKAXE) || type.equals(Material.DIAMOND_PICKAXE) || type.equals(Material.GOLD_PICKAXE)) { // если сломан киркой

            SpawnerData spawnerStats = manager.getSpawner().selectSpawnerStatsSQL(block);
            level = spawnerStats.getLevel();
            entityString = spawnerStats.getType();

            e.setExpToDrop(0);
            if(level == 0) return; // если спавнер с кастомными статами, то не даем выпасть

            boolean enabled = manager.getSpawner().isSpawnerEnabled(block);
            ItemStack spawnerItem;

            spawnerItem = manager.getSpawner().createSpawnerFromLevel(entityString, level, enabled);
            loc.getWorld().dropItemNaturally(loc, spawnerItem); // дропаем его в мире с характеристиками из бд
        }

        CreatureSpawner spawner = (CreatureSpawner) block.getState();

        if(manager.getSpawnerInv().spawnerBlock.containsValue(spawner)) { // закрываем меню сломанного спавнера всем игрокам, у которых открыто

            for(UUID uuid: getKeysByValue(manager.getSpawnerInv().spawnerBlock, spawner)) {
                manager.getSpawnerInv().spawnerBlock.remove(uuid);

                Player p = null;
                for(Player player : getServer().getOnlinePlayers()) {
                    if(player.getUniqueId().equals(uuid)) p = player;
                }
                if(p != null) {
                    p.sendMessage(ChatColor.YELLOW + "Спавнер был сломан!");
                    p.closeInventory();
                }
            }
        }

        sql = "DELETE FROM spawners WHERE x = ? AND y = ? AND z = ?"; // удаляем спавнер из бд, неважно как он сломан, хоть рукой
        try (Connection con = manager.connect();
            PreparedStatement pstmt = con.prepareStatement(sql)) {

            pstmt.setDouble(1, block.getX());
            pstmt.setDouble(2, block.getY());
            pstmt.setDouble(3, block.getZ());

            pstmt.executeUpdate();
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }
    }

    // метод для Вайта, ему нужен был шанс выпадения
    public void dropSpawner(Block block, int chance) {

        SpawnerData spawnerStats = manager.getSpawner().selectSpawnerStatsSQL(block);
        int level = spawnerStats.getLevel();
        String entityString = spawnerStats.getType();

        if(level == 0) return; // если спавнер с кастомными статами, то не даем выпасть

        int random = 1 + (int) (Math.random() * 100);
        if(random <= chance || level > 1) {

            boolean enabled = manager.getSpawner().isSpawnerEnabled(block);
            ItemStack spawnerItem;
            Location loc = block.getLocation();

            spawnerItem = manager.getSpawner().createSpawnerFromLevel(entityString, level, enabled);
            loc.getWorld().dropItemNaturally(loc, spawnerItem); // дропаем его в мире с характеристиками из бд
        }
    }

    private <T, E> Set<T> getKeysByValue(Map<T, E> map, E value) {
        return map.entrySet()
                .stream()
                .filter(entry -> Objects.equals(entry.getValue(), value))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        Block block = e.getClickedBlock();

        if(e.getHand() == EquipmentSlot.HAND) return; // защита от двойного срабатывания ивента при ПКМ

        if (e.getAction() == Action.RIGHT_CLICK_BLOCK || e.getAction() == Action.RIGHT_CLICK_AIR) {
            if(block == null || !block.getType().equals(Material.MOB_SPAWNER)) return;

            if(manager.getSpawner().isSpawnerNatural(block)) // если по натуральному, то перерабатываем его в кастомный и открываем сразу меню
                manager.getSpawner().turnSpawnerIntoCustom(block);

            p.openInventory(manager.getSpawnerInv().createSpawnerInv(p, block)); // если не по натуральному, открываем меню
        }
    }

    @EventHandler
    public void onInvClose(InventoryCloseEvent e) {
        Player p = (Player) e.getPlayer();
        if(e.getInventory().getName().equals("Спавнер")) {
            manager.getSpawnerInv().spawnerBlockType.remove(p.getUniqueId());
            manager.getSpawnerInv().spawnerBlockLevel.remove(p.getUniqueId());
            manager.getSpawnerInv().spawnerBlock.remove(p.getUniqueId()); // когда закрывается инвентарь спавнера - удаляем привязанные к игроку мапы
        }
        else if(e.getInventory().getName().equals("Смена типа")) {
            if(e.getInventory().getItem(22) == null) return; // если центральный слот пуст

            if(p.getInventory().firstEmpty() != -1)
                p.getInventory().addItem(e.getInventory().getItem(22)); // если у игрока не фулл инв, закидываем ему туда
            else p.getLocation().getWorld().dropItem(p.getLocation(), e.getInventory().getItem(22)); // если фулл инв, дропаем под ним

            e.getInventory().getItem(22).setAmount(0); // очищаем слот для предотвращения дюпа
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        Inventory inv = e.getInventory();
        Player p = (Player) e.getWhoClicked();

        if(inv.getName().equals("Спавнер")) {
            ItemStack item = e.getCurrentItem();

            if(item == null) return;
            e.setCancelled(true);

            if(!item.hasItemMeta() || item.getItemMeta().getDisplayName() == null) return;
            ItemMeta meta = item.getItemMeta();

            if(meta.getDisplayName().equals(ChatColor.GREEN + "" + ChatColor.BOLD + "Повысить уровень")) onClickLevelUp(p);
            else if(meta.getDisplayName().equals(ChatColor.RED + "" + ChatColor.BOLD + "Понизить уровень")) onClickLevelDown(p);

            else if(meta.getDisplayName().equals(ChatColor.YELLOW + "" + ChatColor.BOLD + "Сменить тип")) {
                Block block = manager.getSpawnerInv().spawnerBlock.get(p.getUniqueId()).getBlock();
                int level = manager.getSpawnerInv().spawnerBlockLevel.get(p.getUniqueId());

                p.closeInventory(); // закрываем инвентарь чтобы блок в мапе стёрся, а потом при создании инвентаря переназначаем её
                Inventory changeTypeInv = manager.getSpawnerInv().createChangeTypeInv(p, block, level);
                p.openInventory(changeTypeInv);
            }
            else if(meta.getDisplayName().equals(ChatColor.YELLOW + "" + ChatColor.BOLD + "Выключить спавнер")) {
                CreatureSpawner spawner = manager.getSpawnerInv().spawnerBlock.get(p.getUniqueId());

                spawner.setSpawnCount(0);
                spawner.update();

                p.sendMessage(ChatColor.YELLOW + "Спавнер выключен");
                p.closeInventory();
                p.openInventory(manager.getSpawnerInv().createSpawnerInv(p, spawner.getBlock()));
            }
            else if(meta.getDisplayName().equals(ChatColor.GREEN + "" + ChatColor.BOLD + "Включить спавнер")) {
                CreatureSpawner spawner = manager.getSpawnerInv().spawnerBlock.get(p.getUniqueId());
                int level = manager.getSpawnerInv().spawnerBlockLevel.get(p.getUniqueId());
                int spawncount = manager.getSpawner().getMobAmountFromLevel(level);

                spawner.setSpawnCount(spawncount);
                spawner.update();

                p.sendMessage(ChatColor.GREEN + "Спавнер включён");
                p.closeInventory();
                p.openInventory(manager.getSpawnerInv().createSpawnerInv(p, spawner.getBlock()));
            }
        }
        else if(inv.getName().equals("Смена типа")) {

            ItemStack item = e.getCurrentItem();

            if (item == null) return; // игнорируем нажатия по пустым слотам
            if (e.getClickedInventory().equals(inv) && e.getSlot() != 22) e.setCancelled(true);

            if (!item.hasItemMeta() || item.getItemMeta().getDisplayName() == null) return;
            ItemMeta meta = item.getItemMeta();

            if (meta.getDisplayName().equals(ChatColor.YELLOW + "" + ChatColor.BOLD + "Вернуться")) p.closeInventory();
            else if (meta.getDisplayName().equals(ChatColor.GREEN + "" + ChatColor.BOLD + "Подтвердить")) onClickAcceptType(p, e.getClickedInventory());
        }
    }

    private void onClickLevelDown(Player p) {
        if(p.getInventory().firstEmpty() == -1) { // если инвентарь заполнен
            p.sendMessage(ChatColor.YELLOW + "Инвентарь переполнен!");
            return;
        }

        int level = manager.getSpawnerInv().spawnerBlockLevel.get(p.getUniqueId());
        int levelNew = level-1;

        String typeOriginal = manager.getSpawnerInv().spawnerBlockType.get(p.getUniqueId());

        ItemStack spawner = manager.getSpawner().createSpawnerFromLevel(typeOriginal, 1, true);
        p.getInventory().addItem(spawner); // даем игроку спавнер 1-го уровня

        // Тут обновляется непосредственно лвл
        CreatureSpawner spawnerOriginal = manager.getSpawnerInv().spawnerBlock.get(p.getUniqueId());

        int amount = 0;
        int delay = manager.getSpawner().getDelayFromLevel(levelNew);
        boolean enabled = manager.getSpawner().isSpawnerEnabled(spawnerOriginal.getBlock());
        if(enabled) amount = manager.getSpawner().getMobAmountFromLevel(levelNew);


        manager.getSpawner().updateSpawnerStats(spawnerOriginal, delay, amount, true);
        manager.getSpawner().updateSpawnerSQL("level", levelNew, spawnerOriginal.getBlock());

        p.closeInventory();
        p.openInventory(manager.getSpawnerInv().createSpawnerInv(p, spawnerOriginal.getBlock()));
        p.sendMessage(ChatColor.YELLOW + "Уровень спавнера понижен до " + levelNew + "!");
    }

    private void onClickLevelUp(Player p) {
        if(!p.getInventory().contains(Material.MOB_SPAWNER)) {
            p.sendMessage(ChatColor.YELLOW + "Необходимо иметь в инвентаре спавнер аналогичного типа");
            return;
        }

        String typeOriginal = manager.getSpawnerInv().spawnerBlockType.get(p.getUniqueId());
        int levelOriginal = manager.getSpawnerInv().spawnerBlockLevel.get(p.getUniqueId()); // получаем из хешмапов игрока данные об открытом спавнере
        int levelNew = levelOriginal+1;

        boolean isSpawnerAvailable = false;
        boolean isSpawnerHigherLevel = false;

        for(ItemStack invStuff: p.getInventory().getContents()) { // проходимся по всем предметам в инвентаре
            if(invStuff == null) continue;
            if(!invStuff.getType().equals(Material.MOB_SPAWNER)) continue; // игнорируем пустые слоты и всё кроме спавнеров

            if(!manager.getNBT().hasTag(invStuff, "mob_type") || !manager.getNBT().getTag(invStuff, "mob_type").equals(typeOriginal)) {
                System.out.println("PROBLEMA TEGA");
                continue; // игнорируем спавнеры других типов
            }

            int level = manager.getNBT().getTagInt(invStuff, "level");
            if (level > 1 || level == 0) {
                isSpawnerHigherLevel = true; // если нашли спавнер выше 1-го лвла - продолжаем искать, если не будет больше никаких других - переменная отправит уведомление
                continue;
            }
            // всё ниже если всё же нашли подходящий

            invStuff.setAmount(invStuff.getAmount()-1); // удаляем его из инвентаря
            isSpawnerAvailable = true; // переменная для того, чтобы отправить уведомление об неудаче если подходящий спавнер не был найден

            CreatureSpawner spawnerOriginal = manager.getSpawnerInv().spawnerBlock.get(p.getUniqueId());

            int amount = 0;
            int delay = manager.getSpawner().getDelayFromLevel(levelNew);
            boolean enabled = manager.getSpawner().isSpawnerEnabled(spawnerOriginal.getBlock());
            if(enabled) amount = manager.getSpawner().getMobAmountFromLevel(levelNew);

            manager.getSpawner().updateSpawnerStats(spawnerOriginal, delay, amount, false); // обновляем статы у спавнера-блока
            manager.getSpawner().updateSpawnerSQL("level", levelNew, spawnerOriginal.getBlock());

            p.closeInventory(); // переоткрываем игроку инвентарь чтобы обновились характеристики
            p.openInventory(manager.getSpawnerInv().createSpawnerInv(p, spawnerOriginal.getBlock()));
            p.sendMessage(ChatColor.GREEN + "Уровень спавнера повышен до " + levelNew + "!");
            break;
        }

        if(!isSpawnerAvailable && isSpawnerHigherLevel) p.sendMessage(ChatColor.YELLOW + "Спавнер в инвентаре слишком высокого уровня, необходимо использовать 1-го");
        else if(!isSpawnerAvailable) p.sendMessage(ChatColor.YELLOW + "Необходимо иметь в инвентаре спавнер аналогичного типа");
    }

    private void onClickAcceptType(Player p, Inventory clickedInv) {
        int level = manager.getSpawnerInv().spawnerBlockLevel.get(p.getUniqueId());
        int eggsCount = manager.getSpawner().eggForTypeChange*level;

        if (clickedInv.getItem(22) == null || !clickedInv.getItem(22).hasItemMeta()) { // если игрок не закинул ничего в слот
            p.sendMessage(ChatColor.YELLOW + "Для смены типа поместите в центральный слот " + eggsCount + " яиц призыва!");
            return;
        }

        ItemStack middleSlot = clickedInv.getItem(22);

        if (!middleSlot.getType().equals(Material.MONSTER_EGG) || middleSlot.getAmount() < eggsCount) // если это не яйцо призыва или если яиц мало
            p.sendMessage(ChatColor.YELLOW + "Для смены типа поместите в центральный слот " + eggsCount + " яиц призыва!");
        else {
            CreatureSpawner spawner = manager.getSpawnerInv().spawnerBlock.get(p.getUniqueId());
            SpawnEggMeta egg = (SpawnEggMeta) middleSlot.getItemMeta();
            String eggType = egg.getSpawnedType().toString();

            if(eggType.equals(spawner.getSpawnedType().toString().toUpperCase())) {
                p.sendMessage(ChatColor.YELLOW + "У спавнера уже установлен данный тип мобов");
                return;
            }

            spawner.setSpawnedType(egg.getSpawnedType());
            spawner.update();

            p.sendMessage(ChatColor.GREEN + "Тип спавнера успешно сменён на `" + Mobs.MobsTranslated.valueOf(eggType) + "`");
            manager.getSpawner().updateSpawnerSQL("type", eggType, spawner.getBlock());
            middleSlot.setAmount(middleSlot.getAmount()-eggsCount);
        }
    }
}
