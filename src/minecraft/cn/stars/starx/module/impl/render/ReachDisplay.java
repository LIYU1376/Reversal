/*
 * StarX Client - A PVP Client with hack visual.
 * Copyright 2024 Starlight, All rights reserved.
 */
package cn.stars.starx.module.impl.render;

import cn.stars.starx.event.impl.Render3DEvent;
import cn.stars.starx.module.Category;
import cn.stars.starx.module.Module;
import cn.stars.starx.module.ModuleInfo;
import cn.stars.starx.setting.impl.NumberValue;
import cn.stars.starx.util.render.RenderUtils;
import cn.stars.starx.util.render.ThemeType;
import cn.stars.starx.util.render.ThemeUtil;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;

import static java.lang.Math.cos;
import static java.lang.Math.sin;

@ModuleInfo(name = "ReachDisplay", chineseName = "攻击距离", description = "Display your reach distance", chineseDescription = "显示你的攻击距离", category = Category.RENDER)
public class ReachDisplay extends Module {
    private final NumberValue thickness = new NumberValue("Thickness", this, 2f, 0.1f, 5f, 0.1f);
    @Override
    public void onRender3D(Render3DEvent event) {
        GL11.glPushMatrix();
        GL11.glTranslated(
                mc.thePlayer.lastTickPosX + (mc.thePlayer.posX - mc.thePlayer.lastTickPosX) * mc.timer.renderPartialTicks - mc.renderManager.renderPosX,
                mc.thePlayer.lastTickPosY + (mc.thePlayer.posY - mc.thePlayer.lastTickPosY) * mc.timer.renderPartialTicks - mc.renderManager.renderPosY - 0.1,
                mc.thePlayer.lastTickPosZ + (mc.thePlayer.posZ - mc.thePlayer.lastTickPosZ) * mc.timer.renderPartialTicks - mc.renderManager.renderPosZ
        );
        GlStateManager.enableBlend();
        GlStateManager.enableLineSmooth();
        GlStateManager.disableTexture2D();
        GlStateManager.disableDepth();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        GL11.glLineWidth((float) thickness.getValue());
        GL11.glRotatef(90F, 1F, 0F, 0F);
        GL11.glBegin(GL11.GL_LINE_STRIP);

        for (int i = 0; i <= 360; i += 5) { // You can change circle accuracy (60 - accuracy)
            RenderUtils.color(ThemeUtil.getThemeColor(ThemeType.ARRAYLIST, i / 5f));
            float x = (float) (cos(i * Math.PI / 180.0) * 3);
            float y = (float) (sin(i * Math.PI / 180.0) * 3);
            GL11.glVertex2f(x, y);
        }

        GL11.glEnd();

        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.enableDepth();
        GlStateManager.disableLineSmooth();

        GL11.glPopMatrix();
    }
}
