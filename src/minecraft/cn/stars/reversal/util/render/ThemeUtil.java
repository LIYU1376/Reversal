package cn.stars.reversal.util.render;

import cn.stars.reversal.GameInstance;
import cn.stars.reversal.Reversal;
import cn.stars.reversal.module.impl.client.ClientSettings;
import cn.stars.reversal.util.math.TimeUtil;
import cn.stars.reversal.util.misc.ModuleInstance;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.UtilityClass;

import java.awt.*;

@UtilityClass
public final class ThemeUtil implements GameInstance {

    @Getter
    @Setter
    private String customClientName = "";
    private final Color color1 = new Color(233,80,169);
    private final Color color2 = new Color(71, 253, 160);
    private Color color;
    private String theme;
    private String colorType;
    private boolean switcher;

    private final TimeUtil timer = new TimeUtil();

    public Color getThemeColor(final ThemeType type) {
        return getThemeColor(0, type, 1);
    }

    public int getThemeColorInt(final ThemeType type) {
        return getThemeColor(type).hashCode();
    }

    public int getThemeColorInt(final float colorOffset, final ThemeType type) {
        return getThemeColor(colorOffset, type, 1).hashCode();
    }

    public int getThemeColorInt(final float colorOffset, final ThemeType type, final float timeMultiplier) {
        return getThemeColor(colorOffset, type, timeMultiplier).hashCode();
    }

    public Color getThemeColor(final float colorOffset, final ThemeType type) {
        return getThemeColor(colorOffset, type, 1);
    }

    public static Color getThemeColor(final ThemeType type, final float colorOffset) {
        return getThemeColor(colorOffset, type, 1);
    }

    public Color getThemeColor(float colorOffset, final ThemeType type, final float timeMultiplier) {
        if (timer.hasReached(50 * 5)) {
            timer.reset();
            theme = ModuleInstance.getModule(ClientSettings.class).theme.getMode();
            colorType = ModuleInstance.getModule(ClientSettings.class).colorType.getMode();
            color = new Color(Reversal.CLIENT_THEME_COLOR);
        }

        if (theme == null || color == null || colorType == null) return color;

        float colorOffsetMultiplier = 1;

        if (type == ThemeType.GENERAL) {
            if (colorType.equals("Rainbow")) {
                colorOffsetMultiplier = 5f;
            } else if (colorType.equals("Fade")) {
                colorOffsetMultiplier = 2.2f;
            }
        }

        colorOffsetMultiplier *= ModuleInstance.getModule(ClientSettings.class).indexTimes.getFloat();
        colorOffset *= colorOffsetMultiplier;
        float speed = ModuleInstance.getModule(ClientSettings.class).indexSpeed.getFloat();

        final double timer = (System.currentTimeMillis() / 1E+8 * timeMultiplier) * 4E+5;

        switch (type) {
            case GENERAL:
            case ARRAYLIST: {
                switch (colorType) {
                    case "Rainbow":
                        color = new Color(ColorUtil.getColor(-(1 + colorOffset * 1.7f), 0.7f, 1));
                        break;
                    case "Fade":
                        final float offset1 = (float) (Math.abs(Math.sin(timer * 0.5 * speed + colorOffset * 0.45)) / 2.2f) + 1f;
                        color = ColorUtil.liveColorBrighter(new Color(Reversal.CLIENT_THEME_COLOR_BRIGHT), offset1);
                        break;
                    case "Double":
                        color = ColorUtils.INSTANCE.interpolateColorsBackAndForth((int) (20 * (1 / speed)), (int) colorOffset * 4, new Color(Reversal.CLIENT_THEME_COLOR), new Color(Reversal.CLIENT_THEME_COLOR_2), true);
                        break;
                    default:
                        color = new Color(Reversal.CLIENT_THEME_COLOR);
                        break;
                }
                break;
            }

            case LOGO: {
                switch (colorType) {
                    case "Rainbow":
                        color = new Color(ColorUtil.getColor(1 + colorOffset * 1.4f, 0.5f, 1));
                        break;
                    case "Fade":
                        color = new Color(Reversal.CLIENT_THEME_COLOR_BRIGHT);
                        break;
                    case "Double":
                        color = ColorUtils.INSTANCE.interpolateColorsBackAndForth(4, 1, new Color(Reversal.CLIENT_THEME_COLOR_BRIGHT), new Color(Reversal.CLIENT_THEME_COLOR_BRIGHT_2), true);
                        break;
                    default:
                        color = new Color(Reversal.CLIENT_THEME_COLOR);
                        break;
                }
                break;
            }

            default:
            case FLAT_COLOR:
                color = new Color(Reversal.CLIENT_THEME_COLOR);
        }

        return color;
    }
}