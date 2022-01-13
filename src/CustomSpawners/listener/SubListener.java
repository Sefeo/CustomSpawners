package CustomSpawners.listener;

import CustomSpawners.manager.Manager;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class SubListener implements Listener {

    private Manager manager;

    public SubListener(Manager manager) {
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteract(PlayerInteractEvent e){
        if((e.getClickedBlock() != null) && (e.getItem() != null) && (e.getClickedBlock().getType() == Material.MOB_SPAWNER) && (e.getItem().getType() == Material.MONSTER_EGG))
            e.setCancelled(true);
    }

    @EventHandler
    public void onExplode(EntityExplodeEvent e){
        e.blockList().removeIf(b -> b.getType() == Material.MOB_SPAWNER);
    }
}
