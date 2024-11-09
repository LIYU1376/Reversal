/*
 * Reversal Client - A PVP Client with hack visual.
 * Copyright 2024 Starlight, All rights reserved.
 */
package cn.stars.reversal.module.impl.hud;

import cn.stars.reversal.RainyAPI;
import cn.stars.reversal.Reversal;
import cn.stars.reversal.event.impl.Render2DEvent;
import cn.stars.reversal.event.impl.Shader3DEvent;
import cn.stars.reversal.module.Category;
import cn.stars.reversal.module.Module;
import cn.stars.reversal.module.ModuleInfo;
import cn.stars.reversal.music.api.base.LyricLine;
import cn.stars.reversal.music.api.player.MusicPlayer;
import cn.stars.reversal.music.ui.ThemeColor;
import cn.stars.reversal.setting.impl.BoolValue;
import cn.stars.reversal.setting.impl.ModeValue;
import cn.stars.reversal.setting.impl.NumberValue;
import cn.stars.reversal.util.render.ColorUtil;
import cn.stars.reversal.util.render.RenderUtil;
import cn.stars.reversal.util.render.ThemeType;
import cn.stars.reversal.util.render.ThemeUtil;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.texture.DynamicTexture;

import javax.imageio.ImageIO;
import java.awt.*;

@ModuleInfo(name = "MusicInfo", chineseName = "音乐信息", description = "Display the music info",
        chineseDescription = "显示音乐信息", category = Category.HUD)
public class MusicInfo extends Module {
    private final ModeValue mode = new ModeValue("Mode", this, "Simple", "Simple", "Empathy");
    private final BoolValue lyrics = new BoolValue("Lyrics", this, false);
    private final NumberValue heightValue = new NumberValue("Height", this, 50, 50, 100, 1);
    private DynamicTexture coverTexture;
    public static String currentLyric = "暂无歌词...";

    public MusicInfo() {
        setCanBeEdited(true);
        setX(100);
        setY(100);
    }

    public void reset() {
        coverTexture = null;
    }

    @Override
    public void onRender2D(Render2DEvent event) {
        if (!RainyAPI.hasJavaFX) return;
        MusicPlayer player = Reversal.musicManager.screen.player;
        if (player.getMusic() == null) return;
        if (coverTexture == null) {
            try {
                coverTexture = new DynamicTexture(ImageIO.read(player.getMusic().getCoverImage()));
            } catch (Exception e) {

            }
        }
        setHeight((int) heightValue.getValue());
        if (mode.getMode().equals("Simple")) {
            Gui.drawNewRect(getX(), getY(), getWidth(), getHeight(), new Color(0, 0, 0, 80).getRGB());
        } else {
            RenderUtil.roundedRectangle(getX() - 2, getY(), getWidth() + 2, getHeight(), 3f, ColorUtil.empathyColor());
            RenderUtil.roundedRectangle(getX() - 2.5, getY() + 2.5, 1.5, regular20Bold.height() - 2.5, 1f, ThemeUtil.getThemeColor(ThemeType.ARRAYLIST));
        }
        try {
            RenderUtil.image(coverTexture, getX(), getY(), getHeight(), getHeight());
        } catch (Exception e) {

        }
        if (lyrics.isEnabled()) {
            regular20Bold.drawString(player.getMusic().getName(), getX() + getHeight() + 2, getY() + getHeight() / 2f - 18, Color.WHITE.getRGB());
            regular16.drawString(player.getMusic().getArtist(), getX() + getHeight() + 2, getY() + getHeight() / 2f - 7, ThemeColor.greyColor.getRGB());
            if (player.getMusic().getLyrics().isEmpty()) currentLyric = "纯音乐，请欣赏";
            else for (LyricLine lyricLine : player.getMusic().getLyrics()) {
                try {
                    if (lyricLine.getLine().contains("纯音乐") || lyricLine.getLine().contains("暂无")) {
                        currentLyric = lyricLine.getLine();
                        break;
                    }
                    if (player.getCurrentTime() < player.getMusic().getLyrics().get(0).getStart()) {
                        currentLyric = "...";
                        break;
                    }
                    if (player.getCurrentTime() >= lyricLine.getStart() && player.getCurrentTime() < player.getMusic().getLyrics().get(player.getMusic().getLyrics().indexOf(lyricLine) + 1).getStart()) {
                        currentLyric = lyricLine.getLine();
                        break;
                    }
                } catch (Exception e) {
                }
            }
            regular16.drawString(currentLyric, getX() + getHeight() + 2, getY() + getHeight() / 2f + 8, ThemeColor.greyColor.getRGB());
            setWidth((int) (getHeight() + Math.max(Math.max(
                                regular20Bold.getStringWidth(player.getMusic().getName()),
                                regular18.getStringWidth(player.getMusic().getArtist())) + 4, regular16.getWidth(currentLyric) + 4)));
        } else {
            regular20Bold.drawString(player.getMusic().getName(), getX() + getHeight() + 2, getY() + getHeight() / 2f - 8, Color.WHITE.getRGB());
            regular16.drawString(player.getMusic().getArtist(), getX() + getHeight() + 2, getY() + getHeight() / 2f + 3, ThemeColor.greyColor.getRGB());
            setWidth((int) (getHeight() + Math.max(
                                regular20Bold.getStringWidth(player.getMusic().getName()),
                                regular18.getStringWidth(player.getMusic().getArtist())) + 4));
        }
    }

    @Override
    public void onShader3D(Shader3DEvent event) {
        if (!RainyAPI.hasJavaFX) return;
        MusicPlayer player = Reversal.musicManager.screen.player;
        if (player.getMusic() == null) return;
        if (mode.getMode().equals("Simple")) {
            Gui.drawNewRect(getX(), getY(), getWidth(), getHeight(), Color.BLACK.getRGB());
        } else {
            RenderUtil.roundedRectangle(getX() - 2, getY(), getWidth() + 2, getHeight(), 3f, ColorUtil.empathyGlowColor());
            RenderUtil.roundedRectangle(getX() - 2.5, getY() + 2.5, 1.5, regular20Bold.height() - 2.5, 1f, ThemeUtil.getThemeColor(ThemeType.ARRAYLIST));
        }
    }
}
