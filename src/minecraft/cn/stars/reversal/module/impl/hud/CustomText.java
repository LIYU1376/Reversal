/*
 * Reversal Client - A PVP Client with hack visual.
 * Copyright 2025 Aerolite Society, Some rights reserved.
 */
package cn.stars.reversal.module.impl.hud;

import cn.stars.reversal.Reversal;
import cn.stars.reversal.event.impl.Render2DEvent;
import cn.stars.reversal.event.impl.Shader3DEvent;
import cn.stars.reversal.font.FontManager;
import cn.stars.reversal.module.Category;
import cn.stars.reversal.module.Module;
import cn.stars.reversal.module.ModuleInfo;
import cn.stars.reversal.value.impl.BoolValue;
import cn.stars.reversal.value.impl.ColorValue;
import cn.stars.reversal.value.impl.NoteValue;
import cn.stars.reversal.value.impl.NumberValue;

import java.awt.*;

@ModuleInfo(name = "CustomText", localizedName = "module.CustomText.name", description = "Show a custom text on screen", localizedDescription = "module.CustomText.desc", category = Category.HUD)
public class CustomText extends Module {
    private final NoteValue note = new NoteValue("使用指令 '.setText <文字>' 来设置自定义文字!", this);
    public final ColorValue colorValue = new ColorValue("Color", this);
    private final NumberValue size = new NumberValue("Size", this, 16, 4, 64, 1);
    private final BoolValue bloom = new BoolValue("Bloom", this, false);
    private final BoolValue bold = new BoolValue("Bold", this, false);
    private final BoolValue gradient = new BoolValue("Gradient", this, false);

    public CustomText() {
        setX(100);
        setY(100);
        setCanBeEdited(true);
    }

    @Override
    public void onRender2D(Render2DEvent event) {
        drawText();
        setWidthAndHeight();
    }

    @Override
    public void onShader3D(Shader3DEvent event) {
        if (bloom.enabled && event.isBloom()) drawText();
    }

    private void drawText() {
        int x = getX() + 1;
        int y = getY() + 1;
        if (bold.isEnabled()) {
            if (gradient.isEnabled()) {
                float off = 0;
                for (int i = 0; i < Reversal.customText.length(); i++) {
                    final Color c = colorValue.getColor(i);

                    final String character = String.valueOf(Reversal.customText.charAt(i));
                    FontManager.getRegularBold((int)size.getValue()).drawString(character, x + off, y, c.getRGB());
                    off += FontManager.getRegularBold((int)size.getValue()).width(character);
                }
            } else {
                final Color c = colorValue.getColor();
                FontManager.getRegularBold((int)size.getValue()).drawString(Reversal.customText, x, y, c.getRGB());
            }
        } else {
            if (gradient.isEnabled()) {
                float off = 0;
                for (int i = 0; i < Reversal.customText.length(); i++) {
                    final Color c = colorValue.getColor(i);

                    final String character = String.valueOf(Reversal.customText.charAt(i));
                    FontManager.getRegularBold((int)size.getValue()).drawString(character, x + off, y, c.getRGB());
                    off += FontManager.getRegular((int)size.getValue()).width(character);
                }
            } else {
                final Color c = colorValue.getColor();
                FontManager.getRegular((int)size.getValue()).drawString(Reversal.customText, x, y, c.getRGB());
            }
        }
    }

    private void setWidthAndHeight() {
        if (bold.isEnabled()) {
            setWidth(FontManager.getRegularBold((int)size.getValue()).width(Reversal.customText) + 5);
            setHeight((int) (FontManager.getRegularBold((int)size.getValue()).height() + 5));
        } else {
            setWidth(FontManager.getRegular((int)size.getValue()).width(Reversal.customText) + 5);
            setHeight((int) (FontManager.getRegular((int)size.getValue()).height() + 5));
        }
    }

}
