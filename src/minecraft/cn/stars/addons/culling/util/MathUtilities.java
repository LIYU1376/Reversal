package cn.stars.addons.culling.util;

import net.minecraft.util.MathHelper;

/**
 * Contains MathHelper methods
 */
public final class MathUtilities {

    private MathUtilities() {
    }

    public static int floor(double d) {
        return MathHelper.floor_double(d);
    }

    public static int fastFloor(double d) {
        return (int) (d + 1024.0) - 1024;
    }

    public static int ceil(double d) {
        return MathHelper.ceiling_double_int(d);
    }

}
