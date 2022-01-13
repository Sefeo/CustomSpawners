package CustomSpawners.data;

public class SpawnerData {
    private int level;
    private String type;

    public SpawnerData(int level, String type) {
        this.level = level;
        this.type = type;
    }

    public int getLevel() { return level; }
    public String getType() { return type; }

}
