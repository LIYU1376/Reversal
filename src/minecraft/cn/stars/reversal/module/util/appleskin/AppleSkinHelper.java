package cn.stars.reversal.module.util.appleskin;

import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;

public class AppleSkinHelper {

	public static FoodValues getFoodValues(final ItemStack stack) {
		final ItemFood food = (ItemFood) stack.getItem();
		final int hunger = food != null ? food.getHealAmount(stack) : 0;
		final float saturationModifier = food != null ? food.getSaturationModifier(stack) : 0;
		
		return new FoodValues(hunger, saturationModifier);
	}
	
	public static boolean isRottenFood(final ItemStack stack) {
		
        if (!(stack.getItem() instanceof ItemFood)) {
            return false;
        }

        final ItemFood food = (ItemFood) stack.getItem();

        if (food.getPotionEffect(stack) != null) {
            return Potion.potionTypes[food.potionId].isBadEffect();
        }

        return false;
	}
}
