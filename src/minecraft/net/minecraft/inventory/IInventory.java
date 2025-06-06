package net.minecraft.inventory;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.IWorldNameable;

public interface IInventory extends IWorldNameable
{
    int getSizeInventory();

    ItemStack getStackInSlot(int index);

    default Item getItemInSlot(int index) {
        if (index < 0) return null;
        ItemStack stack = getStackInSlot(index);
        if (stack != null) return stack.getItem();
        return null;
    }

    ItemStack decrStackSize(int index, int count);

    ItemStack removeStackFromSlot(int index);

    void setInventorySlotContents(int index, ItemStack stack);

    int getInventoryStackLimit();

    void markDirty();

    boolean isUseableByPlayer(EntityPlayer player);

    void openInventory(EntityPlayer player);

    void closeInventory(EntityPlayer player);

    boolean isItemValidForSlot(int index, ItemStack stack);

    int getField(int id);

    void setField(int id, int value);

    int getFieldCount();

    void clear();
}
