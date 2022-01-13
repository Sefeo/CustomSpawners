package CustomSpawners;

import CustomSpawners.command.Commands;
import CustomSpawners.listener.Handler;
import CustomSpawners.listener.SubListener;
import CustomSpawners.manager.Manager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class Main extends JavaPlugin {

    @Override
    public void onEnable() {
        /*for(Entity ent: this.getServer().getWorld("world").getEntities()) {
            if(!ent.getType().equals(EntityType.PLAYER)) ent.remove(); // ВРЕМЕННО
        }*/
        Manager manager = new Manager(this);

        Bukkit.getPluginManager().registerEvents(new Handler(manager), this);
        Bukkit.getPluginManager().registerEvents(new SubListener(manager), this); // для незначительных ивентов, мб в будущем пригодится

        try {
            Class.forName("org.sqlite.JDBC");
            Connection c = DriverManager.getConnection("jdbc:sqlite:CustomSpawners.db");
            System.out.println("Opened database successfully");

            Statement statement = c.createStatement();
            String sql = "CREATE TABLE IF NOT EXISTS spawners " +
                    "(ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                    " x            FLOAT," +
                    " y            FLOAT," +
                    " z            FLOAT," +
                    " type            TEXT, " +
                    " level        INT)";
            statement.executeUpdate(sql);
            statement.close();
            c.close();
        } catch ( Exception e ) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
        }

        getCommand("spawner").setExecutor(new Commands(manager));
        getCommand("spawnerCustom").setExecutor(new Commands(manager));
    }
}
