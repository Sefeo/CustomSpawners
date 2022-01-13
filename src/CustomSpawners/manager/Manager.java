package CustomSpawners.manager;

import CustomSpawners.Main;
import CustomSpawners.inventories.SpawnerInventory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Manager {

    private Main plugin;
    private NBTManager NBT;
    private SpawnerManager spawner;
    private SpawnerInventory spawnerInv;

    public Manager(Main main) {
        this.plugin = main;
        this.NBT = new NBTManager();
        this.spawner = new SpawnerManager(this);
        this.spawnerInv = new SpawnerInventory(this);
    }

    public NBTManager getNBT() {
        return NBT;
    }

    public SpawnerManager getSpawner() { return spawner; }

    public SpawnerInventory getSpawnerInv() { return spawnerInv; }

    public Connection connect() {
        String url = "jdbc:sqlite:CustomSpawners.db";
        Connection con = null;
        try {
            con = DriverManager.getConnection(url);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return con;
    }
}
