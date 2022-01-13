package CustomSpawners.manager;

import net.minecraft.server.v1_12_R1.NBTTagCompound;
import org.bukkit.craftbukkit.v1_12_R1.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;

public class NBTManager {

    public String getTag(ItemStack item, String tag){
        net.minecraft.server.v1_12_R1.ItemStack nmsCopy = CraftItemStack.asNMSCopy(item);
        if(nmsCopy.getTag() == null) return null;
        NBTTagCompound compound = nmsCopy.getTag();
        return compound.getString(tag);
    }

    public int getTagInt(ItemStack item, String text){
        net.minecraft.server.v1_12_R1.ItemStack nmsCopy = CraftItemStack.asNMSCopy(item);
        if(nmsCopy.getTag() == null) return 1;
        NBTTagCompound compound = nmsCopy.getTag();
        return compound.getInt(text);
    }

    public boolean hasTag(ItemStack item, String text) {
        net.minecraft.server.v1_12_R1.ItemStack nmsCopy = CraftItemStack.asNMSCopy(item);
        if(nmsCopy.getTag() == null) return false;
        NBTTagCompound compound = nmsCopy.getTag();
        return compound.getString(text) != null;
    }

    ItemStack setTagCopy(ItemStack item, String tag, String text){
        net.minecraft.server.v1_12_R1.ItemStack nmsCopy = CraftItemStack.asNMSCopy(item);
        NBTTagCompound compound = (nmsCopy.getTag() == null) ? new NBTTagCompound() : nmsCopy.getTag();
        compound.setString(tag, text);
        nmsCopy.setTag(compound);
        return CraftItemStack.asBukkitCopy(nmsCopy);
    }

    ItemStack setTagCopyInt(ItemStack item, String tag, int value){
        net.minecraft.server.v1_12_R1.ItemStack nmsCopy = CraftItemStack.asNMSCopy(item);
        NBTTagCompound compound = (nmsCopy.getTag() == null) ? new NBTTagCompound() : nmsCopy.getTag();
        compound.setInt(tag, value);
        nmsCopy.setTag(compound);
        return CraftItemStack.asBukkitCopy(nmsCopy);
    }
}
