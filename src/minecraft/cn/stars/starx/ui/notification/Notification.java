package cn.stars.starx.ui.notification;

import cn.stars.starx.GameInstance;
import cn.stars.starx.StarX;
import cn.stars.starx.font.CustomFont;
import cn.stars.starx.util.math.TimeUtil;
import cn.stars.starx.util.render.RenderUtil;
import cn.stars.starx.util.render.RenderUtils;
import cn.stars.starx.util.render.ThemeType;
import cn.stars.starx.util.render.ThemeUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.ResourceLocation;
import org.apache.commons.lang3.StringUtils;

import java.awt.*;

public final class Notification implements GameInstance {

    private final String description;
    private final String title;
    private final NotificationType type;
    private long delay, start, end;

    private final ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());

    private float xVisual = sr.getScaledWidth();
    public float yVisual = sr.getScaledHeight() - 50;
    public float y = sr.getScaledHeight() - 50;

    private final TimeUtil timer = new TimeUtil();

    public long getStart() {
        return start;
    }

    public long getEnd() {
        return end;
    }

    public Notification(final String description, final String title, final long delay, final NotificationType type) {
        this.description = description;
        this.title = title;
        this.delay = delay;
        this.type = type;

        start = System.currentTimeMillis();
        end = start + delay;
    }

    public String getDescription() {
        return description;
    }

    public long getDelay() {
        return delay;
    }

    public void setDelay(final long delay) {
        this.delay = delay;
    }

    public void setStart(final long start) {
        this.start = start;
    }

    public void setEnd(final long end) {
        this.end = end;
    }

    public void render() {
        final String name = StringUtils.capitalize(type.name().toLowerCase());
        Color sideColor = new Color(-1);
        final float screenWidth = sr.getScaledWidth();
        float x = (screenWidth) - (Math.max(gs.getWidth(description), gs.getWidth(name))) - 2;

        final float curr = System.currentTimeMillis() - getStart();
        final float percentageLeft = curr / getDelay();

        if (percentageLeft > 0.9) x = screenWidth;

        if (timer.hasReached(1000 / 60)) {
            xVisual = lerp(xVisual, x, 0.2f);
            yVisual = lerp(yVisual, y, 0.2f);
            timer.reset();
        }

        switch (type) {
            case NOTIFICATION:
                sideColor = new Color(210,210,210,200);
                break;
            case WARNING:
                sideColor = new Color(255,255,120,200);
                break;
            case ERROR:
                sideColor = new Color(255,50,50,200);
                break;
            case SUCCESS:
                sideColor = new Color(50,255,50,200);
                break;
        }

        final Color c = ThemeUtil.getThemeColor(ThemeType.GENERAL);

        RenderUtil.roundedRectCustom(xVisual, yVisual - 3, sr.getScaledWidth() - xVisual, 25, 2, new Color(0, 0, 0, 100), true, false, true, false);
     //   RenderUtil.roundedRect(xVisual - 5, yVisual - 3, 3, 25, 2, sideColor);
     //   RenderUtils.drawImage2(new ResourceLocation("starx/images/info.png"), (int) (x - 27), (int) yVisual, 25,25);

        RenderUtil.roundedRect(xVisual + (percentageLeft * (gs.getWidth(description)) + 8), yVisual + 21, screenWidth + 1, 1, 2, sideColor);

        final Color bright = new Color(Math.min(c.getRed() + 16, 255), Math.min(c.getGreen() + 35, 255), Math.min(c.getBlue() + 7, 255));

        gsTitle.drawString(title, xVisual + 3, yVisual - 2, Color.WHITE.getRGB());
        gs.drawString(description, xVisual + 2, yVisual + 11, Color.WHITE.getRGB());
    }

    public final float lerp(final float a, final float b, final float c) {
        return a + c * (b - a);
    }
}
