package cn.stars.starx.module.impl.hud;

import cn.stars.starx.StarX;
import cn.stars.starx.event.impl.*;
import cn.stars.starx.font.CustomFont;
import cn.stars.starx.module.Category;
import cn.stars.starx.module.Module;
import cn.stars.starx.module.ModuleInfo;
import cn.stars.starx.setting.impl.BoolValue;
import cn.stars.starx.setting.impl.ModeValue;
import cn.stars.starx.ui.gui.GuiEditHUD;
import cn.stars.starx.ui.hud.Hud;
import cn.stars.starx.util.math.MathUtil;
import cn.stars.starx.util.math.TimeUtil;
import cn.stars.starx.util.misc.ModuleInstance;
import cn.stars.starx.util.render.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiPlayerTabOverlay;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@ModuleInfo(name = "TargetHud", description = "Renders a Gui with your targets information",
        chineseDescription = "显示你攻击敌人的信息", category = Category.HUD)
public final class TargetHud extends Module {

    private final TimeUtil timer = new TimeUtil();
    private final ModeValue mode = new ModeValue("Mode", this, "Normal", "Normal", "Simple", "Classic", "Other", "FpsEater3000", "Exhibition", "OldExhibition", "Remix");
    private final BoolValue backGround = new BoolValue("Background", this, true);
    private final BoolValue shadow = new BoolValue("Shadow", this, true);

    private Entity target;
    private Entity lastTarget;
    private float displayHealth;
    private float health;
    private int ticks;
    private final List<THParticleUtils> particles = new ArrayList<>();
    private boolean sentParticles;
    private double scale = 1;
    private final TimeUtil timeUtil = new TimeUtil();
    ScaledResolution sr = new ScaledResolution(mc);

    public TargetHud() {
        setCanBeEdited(true);
        setX(300);
        setY(300);
        setHeight(70);
        setWidth(180);
    }

    public static void renderSteveModelTexture(final double x, final double y, final float u, final float v, final int uWidth, final int vHeight, final int width, final int height, final float tileWidth, final float tileHeight) {
        final ResourceLocation skin = new ResourceLocation("textures/entity/steve.png");
        Minecraft.getMinecraft().getTextureManager().bindTexture(skin);
        GL11.glEnable(GL11.GL_BLEND);
        Gui.drawScaledCustomSizeModalRect(x, y, u, v, uWidth, vHeight, width, height, tileWidth, tileHeight);
        GL11.glDisable(GL11.GL_BLEND);
    }

    public static void renderPlayerModelTexture(final double x, final double y, final float u, final float v, final int uWidth, final int vHeight, final int width, final int height, final float tileWidth, final float tileHeight, final AbstractClientPlayer target) {
        final ResourceLocation skin = target.getLocationSkin();
        Minecraft.getMinecraft().getTextureManager().bindTexture(skin);
        GL11.glEnable(GL11.GL_BLEND);
        Gui.drawScaledCustomSizeModalRect(x, y, u, v, uWidth, vHeight, width, height, tileWidth, tileHeight);
        GL11.glDisable(GL11.GL_BLEND);
    }

    @Override
    public void onUpdateAlwaysInGui() {
        backGround.hidden = !(mode.is("Normal") || mode.is("FpsEater3000"));
    }

    @Override
    public void onAttack(final AttackEvent event) {
        target = event.getTarget();
    }


    @Override
    public void onRender2D(final Render2DEvent event) {
        if (!getModule("HUD").isEnabled()) return;
        if (!ModuleInstance.getBool("HUD", "Display when debugging").isEnabled() && mc.gameSettings.showDebugInfo) return;

        sr = new ScaledResolution(mc);
        final float nameWidth = 38;
        final float posX = getX();
        final float posY = getY() + 40;

        if (timer.hasReached(1000 / 110)) {
            if (target != null && (target.getDistanceSqToEntity(mc.thePlayer) > 100 || mc.theWorld.getEntityByID(Objects.requireNonNull(target).getEntityId()) == null)) {
                scale = Math.max(0, scale - timeUtil.getElapsedTime() / 8E+13 - (1 - scale) / 10);
                if (scale == 0) particles.clear();
                timer.reset();
            } else {
                scale = Math.min(1, scale + timeUtil.getElapsedTime() / 4E+14 + (1 - scale) / 10);
            }
        }

        if (mc.currentScreen instanceof GuiChat || mc.currentScreen instanceof GuiEditHUD) {
            target = mc.thePlayer;
        } else {
            target = null;
        }

        switch (mode.getMode()) {
            case "Normal": {
                if (target == null) {
                    particles.clear();
                    return;
                }

                if (scale == 0) return;

                GlStateManager.pushMatrix();
                GlStateManager.translate((posX + 38 + 2 + 129 / 2f) * (1 - scale), (posY - 34 + 48 / 2f) * (1 - scale), 0);
                GlStateManager.scale(scale, scale, 0);

                final EntityPlayer en = (EntityPlayer) target;
                final double dist = mc.thePlayer.getDistanceToEntity(target);

                final String name = target.getName();

                //Background
                if (backGround.isEnabled()) {
                    RenderUtil.roundedRectangle(posX + 38 + 2, posY - 34, 129, 48, 8, new Color(0, 0, 0, 110));
                    if (ModuleInstance.getBool("PostProcessing", "Bloom").isEnabled()) {
                        MODERN_BLOOM_RUNNABLES.add(() -> {
                            RenderUtil.roundedRectangle(posX + 38 + 2, posY - 34, 129, 48, 8, Color.BLACK);
                        });
                    }
                    if (canBlur()) {
                        MODERN_BLUR_RUNNABLES.add(() -> {
                            RenderUtil.roundedRectangle(posX + 38 + 2, posY - 34, 129, 47, 8, Color.BLACK);
                        });
                    }
                }
                GlStateManager.popMatrix();

                final int scaleOffset = (int) (((EntityPlayer) target).hurtTime * 0.35f);

                for (final THParticleUtils p : particles) {
                    if (p.opacity > 4) NORMAL_RENDER_RUNNABLES.add(p::render2D);
                }

                GlStateManager.pushMatrix();
                GlStateManager.translate((posX + 38 + 2 + 129 / 2f) * (1 - scale), (posY - 34 + 48 / 2f) * (1 - scale), 0);
                GlStateManager.scale(scale, scale, 0);

                //Renders face
                if (target instanceof AbstractClientPlayer) {
                    //offset other colors aside from red, so the face turns red
                    final double offset = -(((AbstractClientPlayer) target).hurtTime * 23);
                    //sets color to red
                    RenderUtil.color(new Color(255, (int) (255 + offset), (int) (255 + offset)));
                    //renders face
                    renderPlayerModelTexture(posX + 38 + 6 + scaleOffset / 2f, posY - 34 + 5 + scaleOffset / 2f, 3, 3, 3, 3, 30 - scaleOffset, 30 - scaleOffset, 24, 24.5f, (AbstractClientPlayer) en);
                    //renders top layer of face
                    renderPlayerModelTexture(posX + 38 + 6 + scaleOffset / 2f, posY - 34 + 5 + scaleOffset / 2f, 15, 3, 3, 3, 30 - scaleOffset, 30 - scaleOffset, 24, 24.5f, (AbstractClientPlayer) en);
                    //resets color to white
                    RenderUtil.color(Color.WHITE);
                }

                final double fontHeight = CustomFont.getHeight();

                CustomFont.drawString("Distance: " + MathUtil.round(dist, 1), posX + 38 + 6 + 30 + 3, posY - 34 + 5 + 15 + 2, Color.WHITE.hashCode());

                GlStateManager.pushMatrix();
                GL11.glEnable(GL11.GL_SCISSOR_TEST);
                RenderUtil.scissor(posX + 38 + 6 + 30 + 3, posY - 34 + 5 + 15 - fontHeight, 91, 30);

                CustomFont.drawString("Name: " + name, posX + 38 + 6 + 30 + 3, posY - 34 + 5 + 15 - fontHeight, Color.WHITE.hashCode());

                GL11.glDisable(GL11.GL_SCISSOR_TEST);
                GlStateManager.popMatrix();

                if (!String.valueOf(((EntityPlayer) target).getHealth()).equals("NaN"))
                    health = Math.min(20, ((EntityPlayer) target).getHealth());

                if (String.valueOf(displayHealth).equals("NaN")) {
                    displayHealth = (float) (Math.random() * 20);
                }

                if ((dist > 20 || target.isDead)) {
                    health = 0;
                }

                final int speed = 6;
                if (timer.hasReached(1000 / 60)) {
                    displayHealth = (displayHealth * (speed - 1) + health) / speed;

                    ticks += 1;

                    for (final THParticleUtils p : particles) {
                        p.updatePosition();

                        if (p.opacity < 1) particles.remove(p);
                    }

                    timer.reset();
                }

                // Get the current theme of the client.
                final ModeValue setting = ((ModeValue) StarX.INSTANCE.getModuleManager().getSetting("ClientSettings", "Theme"));

                // If the setting was null return to prevent crashes bc shit setting system.
                if (setting == null) return;

                // Get the theme and convert it to lower case.
                final String theme = setting.getMode().toLowerCase();

                float offset = 6;
                final float drawBarPosX = posX + nameWidth;

                if (displayHealth > 0.1)
                    for (int i = 0; i < displayHealth * 4; i++) {

                        int color = ThemeUtil.getThemeColorInt(ThemeType.GENERAL);

                        // If the user is using a rainbow theme adjust based on it.
                        if (theme.contains("rainbow")) {
                            color = ColorUtil.getColor(1 * posX * 1.4f + i / 6f, 0.5f, 1);
                        }

                        // If the user is on the blend theme blend accordingly.
                        else if (theme.contains("blend")) {

                            final Color color1 = new Color(78, 161, 253, 100);
                            final Color color2 = new Color(78, 253, 154, 100);

                            color = ColorUtil.mixColors(color1, color2, (Math.sin(Hud.ticks + posX * 0.4f + i * 0.6f / 14f) + 1) * 0.5f).hashCode();
                        }

                        float finalOffset = offset;
                        int finalColor = ModuleInstance.getMode("ClientSettings", "Color Style").getMode().equals("Rainbow") ?
                                color : ColorUtils.INSTANCE.interpolateColorsBackAndForth(8, 1000, Color.WHITE, Color.BLACK, true).getRGB();
                        Gui.drawRect(drawBarPosX + finalOffset, posY + 5, drawBarPosX + 1 + finalOffset * 1.25, posY + 10, finalColor);

                        if (ModuleInstance.getBool("PostProcessing", "Bloom").isEnabled()) {
                            MODERN_BLOOM_RUNNABLES.add(() -> {
                                Gui.drawRect(drawBarPosX + finalOffset, posY + 5, drawBarPosX + 1 + finalOffset * 1.25, posY + 10, finalColor);
                            });
                        }

                        offset += 1;
                    }

                if ((((EntityPlayer) target).hurtTime == 9 && !sentParticles) || (lastTarget != null && ((EntityPlayer) lastTarget).hurtTime == 9 && !sentParticles)) {

                    for (int i = 0; i <= 15; i++) {
                        final THParticleUtils p = new THParticleUtils();
                        final Color color1 = new Color(StarX.CLIENT_THEME_COLOR);
                        final Color color2 = new Color(255, 255, 255);

                        final Color c;

                        if (theme.contains("rainbow")) {
                            c = new Color(ColorUtil.getColor(10 + i, 0.5f, 1));
                        } else if (theme.contains("blend")) {
                            final Color color12 = new Color(78, 161, 253, 100);
                            final Color color22 = new Color(78, 253, 154, 100);

                            c = ColorUtil.mixColors(color12, color22, (Math.sin(Hud.ticks + posX * 0.4f + i) + 1) * 0.5f);
                        } else if (theme.contains("rice")) {

                            final Color color21 = new Color(190, 0, 255, 100);
                            final Color color11 = new Color(0, 190, 255, 100);

                            c = ColorUtil.mixColors(color21, color11, (Math.sin(Hud.ticks + posX * 0.4f + i) + 1) * 0.5f);
                        } else {
                            c = ColorUtil.mixColors(color1, color2, Math.random());
                        }


                        p.init(posX + 55, posY - 15, ((Math.random() - 0.5) * 2) * 1.4, ((Math.random() - 0.5) * 2) * 1.4, Math.random() * 4, c);
                        particles.add(p);
                    }

                    sentParticles = true;
                }

                if (((EntityPlayer) target).hurtTime == 8) sentParticles = false;

                if (!((dist > 20 || target.isDead))) {
                    float finalOffset1 = offset;
                    CustomFont.drawString(String.valueOf(MathUtil.round(displayHealth, 1)), drawBarPosX + 2 + finalOffset1 * 1.25, posY + 2.5f, -1);
                }

                if (lastTarget != target) {
                    lastTarget = target;
                }

                final ArrayList<THParticleUtils> removeList = new ArrayList<>();
                for (final THParticleUtils p : particles) {
                    if (p.opacity <= 1) {
                        removeList.add(p);
                    }
                }

                for (final THParticleUtils p : removeList) {
                    particles.remove(p);
                }
            }

            GlStateManager.popMatrix();
            timeUtil.reset();
            break;

            case "FpsEater3000": {
                if (target == null || !(target instanceof EntityPlayer)) {
                    particles.clear();
                    return;
                }

                if (scale == 0) return;

                GlStateManager.pushMatrix();
                GlStateManager.translate((posX + 38 + 2 + 129 / 2f) * (1 - scale), (posY - 34 + 48 / 2f) * (1 - scale), 0);
                GlStateManager.scale(scale, scale, 0);

                final EntityPlayer en = (EntityPlayer) target;
                final double dist = mc.thePlayer.getDistanceToEntity(target);

                final String name = ((EntityPlayer) target).getName();

                //Background
                if (backGround.isEnabled()) RenderUtil.roundedRect(posX + 38 + 2, posY - 34, 129, 40, 8, new Color(0, 0, 0, 110));

                GlStateManager.popMatrix();

                final int scaleOffset = (int) (((EntityPlayer) target).hurtTime * 0.35f);

                for (final THParticleUtils p : particles) {
                    if (p.opacity > 4) p.render2D();
                }

                GlStateManager.pushMatrix();
                GlStateManager.translate((posX + 38 + 2 + 129 / 2f) * (1 - scale), (posY - 34 + 48 / 2f) * (1 - scale), 0);
                GlStateManager.scale(scale, scale, 0);

                RenderUtil.circle(posX + 38 + 6 + scaleOffset / 2f - 1, posY - 34 + 5 + scaleOffset / 2f - 1, 32 - scaleOffset, new Color(0, 0, 0, 40));

                //Renders face
                if (target instanceof AbstractClientPlayer) {
                    StencilUtil.write(false);
                    RenderUtil.circle(posX + 38 + 6 + scaleOffset / 2f, posY - 34 + 5 + scaleOffset / 2f, 30 - scaleOffset, Color.BLACK);
                    StencilUtil.erase(true);
                    //offset other colors aside from red, so the face turns red
                    final double offset = -(((AbstractClientPlayer) target).hurtTime * 23);
                    //sets color to red
                    RenderUtil.color(new Color(255, (int) (255 + offset), (int) (255 + offset)));
                    //renders face
                    renderPlayerModelTexture(posX + 38 + 6 + scaleOffset / 2f, posY - 34 + 5 + scaleOffset / 2f, 3, 3, 3, 3, 30 - scaleOffset, 30 - scaleOffset, 24, 24.5f, (AbstractClientPlayer) en);
                    //renders top layer of face
                    renderPlayerModelTexture(posX + 38 + 6 + scaleOffset / 2f, posY - 34 + 5 + scaleOffset / 2f, 15, 3, 3, 3, 30 - scaleOffset, 30 - scaleOffset, 24, 24.5f, (AbstractClientPlayer) en);
                    //resets color to white
                    RenderUtil.color(Color.WHITE);

                    StencilUtil.dispose();
                }

                final double fontHeight = comfortaaNigger.getHeight();

                GlStateManager.pushMatrix();
                GL11.glEnable(GL11.GL_SCISSOR_TEST);
                RenderUtil.scissor(posX + 38 + 6 + 30 + 3, posY - 34 + 5 + 15 - fontHeight, 91, 30);

                comfortaaNigger.drawString(name, posX + 38 + 6 + 30 + 2, (float) (posY - 30 + 5 + 10 - fontHeight / 2) - 3, Color.WHITE.hashCode());

                GL11.glDisable(GL11.GL_SCISSOR_TEST);
                GlStateManager.popMatrix();

                if (!String.valueOf(((EntityPlayer) target).getHealth()).equals("NaN"))
                    health = Math.min(20, ((EntityPlayer) target).getHealth());

                if (String.valueOf(displayHealth).equals("NaN")) {
                    displayHealth = (float) (Math.random() * 20);
                }

                final int speed = 5;
                if (timer.hasReached(1000 / 60)) {
                    displayHealth = (displayHealth * (speed - 1) + health) / speed;

                    ticks += 1;

                    for (final THParticleUtils p : particles) {
                        p.updatePosition();

                        if (p.opacity < 1) particles.remove(p);
                    }

                    timer.reset();
                }

                // Get the current theme of the client.
                final ModeValue setting = ((ModeValue) StarX.INSTANCE.getModuleManager().getSetting("ClientSettings", "Theme"));

                // If the setting was null return to prevent crashes bc shit setting system.
                if (setting == null) return;

                // Get the theme and convert it to lower case.
                final String theme = setting.getMode().toLowerCase();

                if ((((EntityPlayer) target).hurtTime == 9 && !sentParticles) || (lastTarget != null && ((EntityPlayer) lastTarget).hurtTime == 9 && !sentParticles)) {

                    for (int i = 0; i <= 15; i++) {
                        final THParticleUtils p = new THParticleUtils();
                        final Color color1 = new Color(StarX.CLIENT_THEME_COLOR);
                        final Color color2 = new Color(255, 255, 255);

                        final Color c;

                        if (theme.contains("rainbow")) {
                            c = new Color(ColorUtil.getColor(10 + i, 0.5f, 1));
                        } else if (theme.contains("blend")) {
                            final Color color12 = new Color(78, 161, 253, 100);
                            final Color color22 = new Color(78, 253, 154, 100);

                            c = ColorUtil.mixColors(color12, color22, (Math.sin(Hud.ticks + posX * 0.4f + i) + 1) * 0.5f);
                        } else if (theme.contains("rice")) {

                            final Color color21 = new Color(190, 0, 255, 100);
                            final Color color11 = new Color(0, 190, 255, 100);

                            c = ColorUtil.mixColors(color21, color11, (Math.sin(Hud.ticks + posX * 0.4f + i) + 1) * 0.5f);
                        } else {
                            c = ColorUtil.mixColors(color1, color2, Math.random());
                        }


                        p.init(posX + 55, posY - 15, ((Math.random() - 0.5) * 2) * 1.4, ((Math.random() - 0.5) * 2) * 1.4, Math.random() * 4, c);
                        particles.add(p);
                    }

                    sentParticles = true;
                }

                if (((EntityPlayer) target).hurtTime == 8) sentParticles = false;

                if ((dist > 20 || target.isDead)) {
                    health = 0;
                }

                float offset = 39;
                final float drawBarPosX = posX + nameWidth;

                if (!((dist > 20 && !Objects.requireNonNull(target.isDead)))) {
                    altoSmall.drawString(MathUtil.round(displayHealth, 1) + "", (float) (drawBarPosX + offset + displayHealth * 3.5) + 2, posY + 3 - 10, -1);
                }

                if (displayHealth > 0.1) {
                    for (int i = 0; i < displayHealth * 4; i++) {
                        Color color = ThemeUtil.getThemeColor(ThemeType.GENERAL);

                        RenderUtil.rect(drawBarPosX + offset, posY - 7 + 0.5, displayHealth * 3.5, 5, true, color);
                    }
                }

                if (lastTarget != target) {
                    lastTarget = target;
                }

                final ArrayList<THParticleUtils> removeList = new ArrayList<>();
                for (final THParticleUtils p : particles) {
                    if (p.opacity <= 1) {
                        removeList.add(p);
                    }
                }

                for (final THParticleUtils p : removeList) {
                    particles.remove(p);
                }

                GlStateManager.popMatrix();
                timeUtil.reset();
            }
            break;

            case "Exhibition": {
                if (target == null || !(target instanceof EntityPlayer) || mc.theWorld.getEntityByID(target.getEntityId()) == null || mc.theWorld.getEntityByID(target.getEntityId()).getDistanceSqToEntity(mc.thePlayer) > 100) {
                    return;
                }
                GlStateManager.pushMatrix();
                // Width and height
                final float width = mc.displayWidth / (float) (mc.gameSettings.guiScale * 2) + 680;
                final float height = mc.displayHeight / (float) (mc.gameSettings.guiScale * 2) + 280;
                GlStateManager.translate(width - 660, height - 160.0f - 90.0f, 0.0f);
                // Draws the skeet rectangles.
                RenderUtil.skeetRect(0, -2.0, skeetBig.getWidth(((EntityPlayer) target).getName()) > 70.0f ? (double) (124.0f + skeetBig.getWidth(((EntityPlayer) target).getName()) - 70.0f) : 124.0, 38.0, 1.0);
                RenderUtil.skeetRectSmall(0.0f, -2.0f, 124.0f, 38.0f, 1.0);
                // Draws name.
                skeetBig.drawStringWithShadow(((EntityPlayer) target).getName(), 42.3f, 0.3f, -1);
                // Gets health.
                final float health = ((EntityPlayer) target).getHealth();
                // Gets health and absorption
                final float healthWithAbsorption = ((EntityPlayer) target).getHealth() + ((EntityPlayer) target).getAbsorptionAmount();
                // Color stuff for the healthBar.
                final float[] fractions = new float[]{0.0F, 0.5F, 1.0F};
                final Color[] colors = new Color[]{Color.RED, Color.YELLOW, Color.GREEN};
                // Max health.
                final float progress = health / ((EntityPlayer) target).getMaxHealth();
                // Color.
                final Color healthColor = health >= 0.0f ? ColorUtil.blendColors(fractions, colors, progress).brighter() : Color.RED;
                // Round.
                double cockWidth = 0.0;
                cockWidth = MathUtil.round(cockWidth, (int) 5.0);
                if (cockWidth < 50.0) {
                    cockWidth = 50.0;
                }
                // Healthbar + absorption
                final double healthBarPos = cockWidth * (double) progress;
                RenderUtil.rectangle(42.5, 10.3, 103, 13.5, healthColor.darker().darker().darker().darker().getRGB());
                RenderUtil.rectangle(42.5, 10.3, 53.0 + healthBarPos + 0.5, 13.5, healthColor.getRGB());
                if (((EntityPlayer) target).getAbsorptionAmount() > 0.0f) {
                    RenderUtil.rectangle(97.5 - (double) ((EntityPlayer) target).getAbsorptionAmount(), 10.3, 103.5, 13.5, new Color(137, 112, 9).getRGB());
                }
                // Draws rect around health bar.
                RenderUtil.rectangleBordered(42.0, 9.8f, 54.0 + cockWidth, 14.0, 0.5, 0, Color.BLACK.getRGB());
                // Draws the lines between the healthbar to make it look like boxes.
                for (int dist = 1; dist < 10; ++dist) {
                    final double cock = cockWidth / 8.5 * (double) dist;
                    RenderUtil.rectangle(43.5 + cock, 9.8, 43.5 + cock + 0.5, 14.0, Color.BLACK.getRGB());
                }
                // Draw targets hp number and distance number.
                GlStateManager.scale(0.5, 0.5, 0.5);
                final int distance = (int) mc.thePlayer.getDistanceToEntity(target);
                final String nice = "HP: " + (int) healthWithAbsorption + " | Dist: " + distance;
                mc.fontRendererObj.drawString(nice, 85.3f, 32.3f, -1, true);
                GlStateManager.scale(2.0, 2.0, 2.0);
                GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
                GlStateManager.enableAlpha();
                GlStateManager.enableBlend();
                GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
                // Draw targets armor and tools and weapons and shows the enchants.
                if (target != null) drawEquippedShit(28, 20);
                GlStateManager.disableAlpha();
                GlStateManager.disableBlend();
                // Draws targets model.
                GlStateManager.scale(0.31, 0.31, 0.31);
                GlStateManager.translate(73.0f, 102.0f, 40.0f);
                GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
                RenderUtil.drawModel(target.rotationYaw, target.rotationPitch, (EntityLivingBase) target);
                GlStateManager.popMatrix();
            }
            break;

            case "OldExhibition": {
                if (target == null || !(target instanceof EntityPlayer) || mc.theWorld.getEntityByID(target.getEntityId()) == null || mc.theWorld.getEntityByID(target.getEntityId()).getDistanceSqToEntity(mc.thePlayer) > 100) {
                    return;
                }
                GlStateManager.pushMatrix();
                // Width and height
                final float width = mc.displayWidth / (float) (mc.gameSettings.guiScale * 2) + 680;
                final float height = mc.displayHeight / (float) (mc.gameSettings.guiScale * 2) + 280;
                GlStateManager.translate(width - 660, height - 160.0f - 90.0f, 0.0f);
                // Draws the skeet rectangles.
                RenderUtil.rectangle(4, -2, mc.fontRendererObj.getStringWidth(((EntityPlayer) target).getName()) > 70.0f ? (124.0D + mc.fontRendererObj.getStringWidth(((EntityPlayer) target).getName()) - 70.0f) : 124.0, 37.0, new Color(0, 0, 0, 160).getRGB());
                // Draws name.
                mc.fontRendererObj.drawStringWithShadow(((EntityPlayer) target).getName(), 42.3f, 0.3f, -1);
                // Gets health.
                final float health = ((EntityPlayer) target).getHealth();
                // Gets health and absorption
                final float healthWithAbsorption = ((EntityPlayer) target).getHealth() + ((EntityPlayer) target).getAbsorptionAmount();
                // Color stuff for the healthBar.
                final float[] fractions = new float[]{0.0F, 0.5F, 1.0F};
                final Color[] colors = new Color[]{Color.RED, Color.YELLOW, Color.GREEN};
                // Max health.
                final float progress = health / ((EntityPlayer) target).getMaxHealth();
                // Color.
                final Color healthColor = health >= 0.0f ? ColorUtil.blendColors(fractions, colors, progress).brighter() : Color.RED;
                // Round.
                double cockWidth = 0.0;
                cockWidth = MathUtil.round(cockWidth, (int) 5.0);
                if (cockWidth < 50.0) {
                    cockWidth = 50.0;
                }
                // Healthbar + absorption
                final double healthBarPos = cockWidth * (double) progress;
                RenderUtil.rectangle(42.5, 10.3, 53.0 + healthBarPos + 0.5, 13.5, healthColor.getRGB());
                if (((EntityPlayer) target).getAbsorptionAmount() > 0.0f) {
                    RenderUtil.rectangle(97.5 - (double) ((EntityPlayer) target).getAbsorptionAmount(), 10.3, 103.5, 13.5, new Color(137, 112, 9).getRGB());
                }
                // Draws rect around health bar.
                RenderUtil.rectangleBordered(42.0, 9.8f, 54.0 + cockWidth, 14.0, 0.5, 0, Color.BLACK.getRGB());
                // Draws the lines between the healthbar to make it look like boxes.
                for (int dist = 1; dist < 10; ++dist) {
                    final double cock = cockWidth / 8.5 * (double) dist;
                    RenderUtil.rectangle(43.5 + cock, 9.8, 43.5 + cock + 0.5, 14.0, Color.BLACK.getRGB());
                }
                // Draw targets hp number and distance number.
                GlStateManager.scale(0.5, 0.5, 0.5);
                final int distance = (int) mc.thePlayer.getDistanceToEntity(target);
                final String nice = "HP: " + (int) healthWithAbsorption + " | Dist: " + distance;
                mc.fontRendererObj.drawString(nice, 85.3f, 32.3f, -1, true);
                GlStateManager.scale(2.0, 2.0, 2.0);
                GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
                GlStateManager.enableAlpha();
                GlStateManager.enableBlend();
                GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
                // Draw targets armor and tools and weapons and shows the enchants.
                if (target != null) drawEquippedShit(28, 20);
                GlStateManager.disableAlpha();
                GlStateManager.disableBlend();
                // Draws targets model.
                GlStateManager.scale(0.31, 0.31, 0.31);
                GlStateManager.translate(73.0f, 102.0f, 40.0f);
                RenderUtil.drawModel(target.rotationYaw, target.rotationPitch, (EntityLivingBase) target);
                GlStateManager.popMatrix();
            }
            break;

            case "Remix": {
                if (target == null || !(target instanceof EntityPlayer) || mc.theWorld.getEntityByID(target.getEntityId()) == null || mc.theWorld.getEntityByID(target.getEntityId()).getDistanceSqToEntity(mc.thePlayer) > 100) {
                    return;
                }
                GlStateManager.pushMatrix();
                // Width and height
                final float width = mc.displayWidth / (float) (mc.gameSettings.guiScale * 2) + 680;
                final float height = mc.displayHeight / (float) (mc.gameSettings.guiScale * 2) + 280;
                GlStateManager.translate(width - 660, height - 160.0f - 90.0f, 0.0f);
                // Border rect.
                RenderUtil.rectangle(2, -6, 156.0, 47.0, new Color(25, 25, 25).getRGB());
                // Main rect.
                RenderUtil.rectangle(4, -4, 154.0, 45.0, new Color(45, 45, 45).getRGB());
                // Draws name.
                mc.fontRendererObj.drawStringWithShadow(((EntityPlayer) target).getName(), 46f, 0.3f, -1);
                // Gets health.
                final float health = ((EntityPlayer) target).getHealth();
                // Color stuff for the healthBar.
                final float[] fractions = new float[]{0.0F, 0.5F, 1.0F};
                final Color[] colors = new Color[]{Color.RED, Color.YELLOW, Color.GREEN};
                // Max health.
                final float progress = health / ((EntityPlayer) target).getMaxHealth();
                // Color.
                final Color healthColor = health >= 0.0f ? ColorUtil.blendColors(fractions, colors, progress).brighter() : Color.RED;
                // $$ draws the 4 fucking boxes killing my self btw. $$
                RenderUtil.rect(45, 11, 20, 20, new Color(25, 25, 25));
                RenderUtil.rect(46, 12, 18, 18, new Color(95, 95, 95));
                RenderUtil.rect(67, 11, 20, 20, new Color(25, 25, 25));
                RenderUtil.rect(68, 12, 18, 18, new Color(95, 95, 95));
                RenderUtil.rect(89, 11, 20, 20, new Color(25, 25, 25));
                RenderUtil.rect(90, 12, 18, 18, new Color(95, 95, 95));
                RenderUtil.rect(111, 11, 20, 20, new Color(25, 25, 25));
                RenderUtil.rect(112, 12, 18, 18, new Color(95, 95, 95));
                // Draws the current ping/ms.
                NetworkPlayerInfo networkPlayerInfo = mc.getNetHandler().getPlayerInfo(target.getUniqueID());
                final String ping = (Objects.isNull(networkPlayerInfo) ? "0ms" : networkPlayerInfo.getResponseTime() + "ms");
                GlStateManager.pushMatrix();
                GlStateManager.scale(0.6, 0.6, 0.6);
                mc.fontRendererObj.drawCenteredStringWithShadow(ping, 240, 40, Color.WHITE.getRGB());
                GlStateManager.popMatrix();
                // Draws the ping thingy from tab. :sunglasses:
                if (target != null && networkPlayerInfo != null) GuiPlayerTabOverlay.drawPingStatic(103, 50, 14, networkPlayerInfo);
                // Round.
                double cockWidth = 0.0;
                cockWidth = MathUtil.round(cockWidth, (int) 5.0);
                if (cockWidth < 50.0) {
                    cockWidth = 50.0;
                }
                // Bar behind healthbar.
                RenderUtil.rectangle(6.5, 37.3, 151, 43, Color.RED.darker().darker().getRGB());
                final double healthBarPos = cockWidth * (double) progress;
                // health bar.
                RenderUtil.rect(6f, 37.3f, (healthBarPos * 2.9), 6f, healthColor);
                // Gets the armor thingy for the bar.
                float armorValue = ((EntityPlayer) target).getTotalArmorValue();
                double armorWidth = armorValue / 20D;
                // Bar behind armor bar.
                RenderUtil.rect(45.5f, 32.3f, 105, 2.5f, new Color(0, 0, 255));
                // Armor bar.
                RenderUtil.rect(45.5f, 32.3f, (105 * armorWidth), 2.5f, new Color(0, 45, 255));
                // White rect around head.
                RenderUtil.rect(6, -2, 37, 37, new Color(205, 205, 205));
                // Draws head.
                renderPlayerModelTexture(7, -1, 3, 3, 3, 3, 35, 35, 24, 24, (AbstractClientPlayer) target);
                // Draws armor.
                GlStateManager.scale(1.1, 1.1, 1.1);
                GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
                GlStateManager.enableAlpha();
                GlStateManager.enableBlend();
                GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
                // Draw targets armor the worst way possible.
                if (target != null) drawHelmet(24, 11); drawChest(44, 11); drawLegs(64, 11); drawBoots(84, 11);
                GlStateManager.disableAlpha();
                GlStateManager.disableBlend();
                GlStateManager.popMatrix();
            }
            break;

            case "Simple": {
                if (target == null || !(target instanceof EntityPlayer)) return;

                if (!String.valueOf(((EntityPlayer) target).getHealth()).equals("NaN"))
                    health = Math.min(20, ((EntityPlayer) target).getHealth());

                if (String.valueOf(((EntityPlayer) target).getHealth()).equals("NaN")) {
                    if (mc.thePlayer.ticksExisted % 20 == 0)
                        health = (float) (Math.random() * 20);
                }

                if (String.valueOf(displayHealth).equals("NaN")) {
                    displayHealth = (float) (Math.random() * 20);
                }

                if ((mc.thePlayer.getDistanceToEntity(target) > 20 || target.isDead)) {
                    health = 0;
                }

                final int speed = 15;
                if (timer.hasReached(1000 / 140)) {
                    displayHealth = (displayHealth * (speed - 1) + health) / speed;

                    ticks += 1;

                    timer.reset();
                }

                float offset = 6;
                final float drawBarPosX = posX + nameWidth;

                if (displayHealth > 0.1)
                    for (int i = 0; i < displayHealth * 4; i++) {
                        final float o = (float) (Math.abs(Math.sin((ticks * 0.006 + i * 0.005) * (((EntityPlayer) target).hurtTime / 8.0F + 2))) / 2) + 1;

                        // Get the current theme of the client.
                        final ModeValue setting = ((ModeValue) StarX.INSTANCE.getModuleManager().getSetting("ClientSettings", "Theme"));

                        // If the setting was null return to prevent crashes bc shit setting system.
                        if (setting == null) return;

                        // Get the theme and convert it to lower case.
                        final String theme = setting.getMode().toLowerCase();

                        int color = ThemeUtil.getThemeColorInt(ThemeType.GENERAL);

                        // If the user is using a rainbow theme adjust based on it.
                        if (theme.contains("rainbow")) {
                            color = ColorUtil.getColor(1 * posX * 1.4f + i / 6f, 0.5f, 1);
                        }

                        // If the user is on the blend theme blend accordingly.
                        else if (theme.contains("blend")) {
                            final Color color1 = new Color(78, 161, 253, 100);
                            final Color color2 = new Color(78, 253, 154, 100);

                            color = ColorUtil.mixColors(color1, color2, (Math.sin(Hud.ticks + posX * 0.4f + i * 0.6f / 14f) + 1) * 0.5f).hashCode();
                        }

                        Gui.drawRect(drawBarPosX + offset, posY, drawBarPosX + 1 + offset, posY + 10, color);

                        if (ModuleInstance.getBool("PostProcessing", "Bloom").isEnabled()) {
                            float finalOffset = offset;
                            int finalColor = color;
                            MODERN_BLOOM_RUNNABLES.add(() -> Gui.drawRect(drawBarPosX + finalOffset, posY, drawBarPosX + 1 + finalOffset, posY + 10, finalColor));
                        }

                        offset += 1;
                    }

                if (!((mc.thePlayer.getDistanceToEntity(target) > 20|| target.isDead))) {
                    CustomFont.drawStringWithShadow(String.valueOf(MathUtil.round(displayHealth, 1)), drawBarPosX + 4 + offset, posY, -1);
                }

            }
            break;

            case "Classic": {
                if (target == null || !(target instanceof EntityPlayer) || mc.theWorld.getEntityByID(target.getEntityId()) == null || mc.theWorld.getEntityByID(target.getEntityId()).getDistanceSqToEntity(mc.thePlayer) > 100) {
                    particles.clear();
                    return;
                }

                final EntityPlayer ent = (EntityPlayer) target;
                final double dista = mc.thePlayer.getDistanceToEntity(target);

                int x = getX() + 100;
                int y = getY() + 50;

                final float clampedHealthValue = MathHelper.clamp_float(ent.getHealth(), 0, ent.getMaxHealth());
                final double enHeartsValue = (clampedHealthValue / ent.getMaxHealth()) * 20.0;
                final float normalizedenHealthValue = clampedHealthValue / ent.getMaxHealth();
                final String enHearts = String.valueOf(enHeartsValue).split("\\.")[0] + "." + String.valueOf(enHeartsValue).split("\\.")[1].charAt(0);
                final String enDistance = String.valueOf(dista).split("\\.")[0] + "." + String.valueOf(dista).split("\\.")[1].charAt(0);

                Gui.drawRect(x - 75, y + 4, x + 75, y - 40, 0xBB000000);

                CustomFont.drawString(ent.getName(), x - 35, y - 36, -1);
                CustomFont.drawString("Dist: " + enDistance, x - 35, y - 26, -1);
                CustomFont.drawString(enHearts, x + 74 - CustomFont.getWidth(enHearts), y - 16, -1);

                RenderUtil.renderGradientRectLeftRight(x - 35, y - 5, (int) (x - 35 + (216 * normalizedenHealthValue) / 2), y - 2, new Color(0xFF078301).darker().getRGB(), 0xFF00FF50);
                RenderUtil.renderGradientRectLeftRight(x - 35, y, (int) (x - 35 + 216 * (ent.getTotalArmorValue() / 20f) / 2), y + 3, 0xFF0050FF, 0xFF00FFFF);

                //offset other colors aside from red, so the face turns red
                final double offset = -(ent.hurtTime * 23);
                //sets color to red
                RenderUtil.color(new Color(255, (int) (255 + offset), (int) (255 + offset)));

                //Renders face
                if (target instanceof AbstractClientPlayer) {
                    //renders face
                    renderPlayerModelTexture(x - 73, y - 36, 3, 3, 3, 3, 36, 36, 24, 24, (AbstractClientPlayer) ent);
                    //renders top layer of face
                    renderPlayerModelTexture(x - 73, y - 36, 15, 3, 3, 3, 36, 36, 24, 24, (AbstractClientPlayer) ent);
                } else {
                    // renders face
                    renderSteveModelTexture(x - 73, y - 36, 3, 3, 3, 3, 36, 36, 24, 24);
                    // renders top layer of face
                    renderSteveModelTexture(x - 73, y - 36, 15, 3, 3, 3, 36, 36, 24, 24);
                }

                //resets color to white
                RenderUtil.color(Color.WHITE);
            }
            break;

            case "Other": {
                if (target == null || !(target instanceof EntityPlayer) || mc.theWorld.getEntityByID(target.getEntityId()) == null || mc.theWorld.getEntityByID(target.getEntityId()).getDistanceSqToEntity(mc.thePlayer) > 100) {
                    return;
                }

                final EntityPlayer enti = (EntityPlayer) target;
                final double distan = mc.thePlayer.getDistanceToEntity(target);

                this.health = (float) MathUtil.lerp(this.health, enti.getHealth(), 0.1);
                final String distance = String.valueOf(distan).split("\\.")[0] + "." + String.valueOf(distan).split("\\.")[1].charAt(0);

                RenderUtil.color(new Color(255, 255, 255, 255));

                if (target instanceof EntityLivingBase)
                    GuiInventory.drawEntityOnScreen((int) (sr.getScaledWidth() / 2F) + 54 + 16, (int) (sr.getScaledHeight() / 2F) + 54 + 65, 32, 0, 0, (EntityLivingBase) target);

                Gui.drawRect(sr.getScaledWidth() / 2F + 50, sr.getScaledHeight() / 2F + 50, sr.getScaledWidth() / 2F + 50 + 160, sr.getScaledHeight() / 2F + 50 + 80, 0xcc000000);

                for (int i = 0; i < (this.health / ((EntityPlayer) target).getMaxHealth()) * 160; i++) {
                    RenderUtil.rect(sr.getScaledWidth() / 2F + 50 + i, sr.getScaledHeight() / 2F + 50 + 78.5, 1, 1.5, new Color(ColorUtil.getStaticColor(i / 8f, 0.7f, 1)));
                }

                CustomFont.drawStringMedium(((EntityPlayer) target).getName(), (sr.getScaledWidth() / 2F) + 54 + 35, sr.getScaledHeight() / 2F + 50 + 6, Color.WHITE.getRGB());

                if (enti.getHealth() / enti.getMaxHealth() <= mc.thePlayer.getHealth() / mc.thePlayer.getMaxHealth())
                    CustomFont.drawStringMedium("Winning", (sr.getScaledWidth() / 2F) + 54 + 35, sr.getScaledHeight() / 2F + 50 + 45 + CustomFont.getHeightMedium(), Color.WHITE.getRGB());
                else
                    CustomFont.drawStringMedium("Losing", (sr.getScaledWidth() / 2F) + 54 + 35, sr.getScaledHeight() / 2F + 50 + 45 + CustomFont.getHeightMedium(), Color.WHITE.getRGB());

                CustomFont.drawString("Dist: " + distance, (sr.getScaledWidth() / 2F) + 54 + 35.5, sr.getScaledHeight() / 2F + 50 + CustomFont.getHeightMedium() + 6, Color.WHITE.getRGB());
                CustomFont.drawString("Hurt Resistant Time: " + enti.hurtResistantTime, (sr.getScaledWidth() / 2F) + 54 + 35.5, sr.getScaledHeight() / 2F + 50 + CustomFont.getHeightMedium() + 6 + CustomFont.getHeight() + 1, Color.WHITE.getRGB());
            }
            break;
        }
    }


    @Override
    public void onPreBlur(final PreBlurEvent event) {
     /*  if (this.getModule(Blur.class).isEnabled()) {
            if (target == null || !(target instanceof EntityPlayer))
                return;

            switch (mode.getMode()) {
                case "FpsEater3000":
                case "Normal": {
                    final int hurtime = ((EntityPlayer) target).hurtTime;
                    if (hurtime == 0) return;
                    final int scaleOffset = (int) (hurtime * 0.35f);

                    final float nameWidth = 38;
                    final float posX = mc.displayWidth / (float) (mc.gameSettings.guiScale * 2) - nameWidth - 45 + 80;
                    final float posY = mc.displayHeight / (float) (mc.gameSettings.guiScale * 2) + 20 + 50;

                    GlStateManager.pushMatrix();
                    GlStateManager.translate((posX + 38 + 2 + 129 / 2f) * (1 - scale), (posY - 34 + 48 / 2f) * (1 - scale), 0);
                    GlStateManager.scale(scale, scale, 0);

                    RenderUtil.rect(posX + 38 + 6 + scaleOffset / 2f, posY - 34 + 5 + scaleOffset / 2f, 30, 30, new Color(255, 0, 0, (int) (hurtime * (255 / 10f))));

                    GlStateManager.popMatrix();
                    break;
                }
            }
        } */
    }

    @Override
    public void onFadingOutline(final FadingOutlineEvent event) {
        if (target == null || !(target instanceof EntityPlayer))
            return;

        switch (mode.getMode()) {
            case "FpsEater3000":
            case "Normal": {

                final float nameWidth = 38;

                final float posX = mc.displayWidth / (float) (mc.gameSettings.guiScale * 2) - 45 - nameWidth + 80;
                final float posY = mc.displayHeight / (float) (mc.gameSettings.guiScale * 2) + 20 + 50;

                GlStateManager.pushMatrix();
                GlStateManager.translate((posX + 38 + 2 + 129 / 2f) * (1 - scale), (posY - 34 + 48 / 2f) * (1 - scale), 0);
                GlStateManager.scale(scale, scale, 0);

                //Background
                if (mode.is("FpsEater3000")) {
                    RenderUtil.roundedRect(posX + 38 + 2.5, posY - 33.5, 128, 39, 8, new Color(0, 0, 0, 235));
                } else {
                    RenderUtil.roundedRect(posX + 38 + 2.5, posY - 33.5, 128, 47, 8, new Color(0, 0, 0, 235));
                }

                GlStateManager.popMatrix();

                break;
            }
        }
    }

    @Override
    public void onBlur(final BlurEvent event) {
        if (target == null || !(target instanceof EntityPlayer))
            return;

        switch (mode.getMode()) {
            case "FpsEater3000":
            case "Normal": {

                final float nameWidth = 38;

                final float posX = mc.displayWidth / (float) (mc.gameSettings.guiScale * 2) - 45 - nameWidth + 80;
                final float posY = mc.displayHeight / (float) (mc.gameSettings.guiScale * 2) + 20 + 50;

                GlStateManager.pushMatrix();
                GlStateManager.translate((posX + 38 + 2 + 129 / 2f) * (1 - scale), (posY - 34 + 48 / 2f) * (1 - scale), 0);
                GlStateManager.scale(scale, scale, 0);

                //Background
                if (mode.is("FpsEater3000")) {
                    RenderUtil.roundedRect(posX + 38 + 2.5, posY - 33.5, 128, 39, 8, Color.black);
                } else {
                    RenderUtil.roundedRect(posX + 38 + 2.5, posY - 33.5, 128, 47, 8, Color.black);
                }

                GlStateManager.popMatrix();

                break;
            }

        }
    }

    private void drawEquippedShit(final int x, final int y) {
        if (target == null || !(target instanceof EntityPlayer)) return;
        GL11.glPushMatrix();
        final List<ItemStack> stuff = new ArrayList<>();
        int cock = -2;
        for (int geraltOfNigeria = 3; geraltOfNigeria >= 0; --geraltOfNigeria) {
            final ItemStack armor = ((EntityPlayer) target).getCurrentArmor(geraltOfNigeria);
            if (armor != null) {
                stuff.add(armor);
            }
        }
        if (((EntityPlayer) target).getHeldItem() != null) {
            stuff.add(((EntityPlayer) target).getHeldItem());
        }

        for (final ItemStack yes : stuff) {
            if (Minecraft.getMinecraft().theWorld != null) {
                RenderHelper.enableGUIStandardItemLighting();
                cock += 16;
            }
            GlStateManager.pushMatrix();
            GlStateManager.disableAlpha();
            GlStateManager.clear(256);
            GlStateManager.enableBlend();
            Minecraft.getMinecraft().getRenderItem().renderItemIntoGUI(yes, cock + x, y);
            Minecraft.getMinecraft().getRenderItem().renderItemOverlays(Minecraft.getMinecraft().fontRendererObj, yes, cock + x, y);
            RenderUtil.renderEnchantText(yes, cock + x, (y + 0.5f));
            GlStateManager.disableBlend();
            GlStateManager.scale(0.5, 0.5, 0.5);
            GlStateManager.disableDepth();
            GlStateManager.disableLighting();
            GlStateManager.enableDepth();
            GlStateManager.scale(2.0f, 2.0f, 2.0f);
            GlStateManager.enableAlpha();
            GlStateManager.popMatrix();
            yes.getEnchantmentTagList();
        }
        GL11.glPopMatrix();
    }

    // It's a retarded way to do, but I couldn't figure how to space them proper. (I'll improve this some other time can't be asked rn)
    private void drawHelmet(final int x, final int y) {
        if (target == null || !(target instanceof EntityPlayer)) return;
        GL11.glPushMatrix();
        final List<ItemStack> stuff = new ArrayList<>();
        int cock = -2;
        final ItemStack helmet = ((EntityPlayer) target).getCurrentArmor(3);
        if (helmet != null) {
            stuff.add(helmet);
        }

        for (final ItemStack yes : stuff) {
            if (Minecraft.getMinecraft().theWorld != null) {
                RenderHelper.enableGUIStandardItemLighting();
                cock += 20;
            }
            GlStateManager.pushMatrix();
            GlStateManager.disableAlpha();
            GlStateManager.clear(256);
            GlStateManager.enableBlend();
            Minecraft.getMinecraft().getRenderItem().renderItemIntoGUI(yes, cock + x, y);
            GlStateManager.disableBlend();
            GlStateManager.scale(0.5, 0.5, 0.5);
            GlStateManager.disableDepth();
            GlStateManager.disableLighting();
            GlStateManager.enableDepth();
            GlStateManager.scale(2.0f, 2.0f, 2.0f);
            GlStateManager.enableAlpha();
            GlStateManager.popMatrix();
        }
        GL11.glPopMatrix();
    }

    private void drawChest(final int x, final int y) {
        if (target == null || !(target instanceof EntityPlayer)) return;
        GL11.glPushMatrix();
        final List<ItemStack> stuff = new ArrayList<>();
        int cock = -2;
        final ItemStack chest = ((EntityPlayer) target).getCurrentArmor(2);
        if (chest != null) {
            stuff.add(chest);
        }

        for (final ItemStack yes : stuff) {
            if (Minecraft.getMinecraft().theWorld != null) {
                RenderHelper.enableGUIStandardItemLighting();
                cock += 20;
            }
            GlStateManager.pushMatrix();
            GlStateManager.disableAlpha();
            GlStateManager.clear(256);
            GlStateManager.enableBlend();
            Minecraft.getMinecraft().getRenderItem().renderItemIntoGUI(yes, cock + x, y);
            GlStateManager.disableBlend();
            GlStateManager.scale(0.5, 0.5, 0.5);
            GlStateManager.disableDepth();
            GlStateManager.disableLighting();
            GlStateManager.enableDepth();
            GlStateManager.scale(2.0f, 2.0f, 2.0f);
            GlStateManager.enableAlpha();
            GlStateManager.popMatrix();
        }
        GL11.glPopMatrix();
    }

    private void drawLegs(final int x, final int y) {
        if (target == null || !(target instanceof EntityPlayer)) return;
        GL11.glPushMatrix();
        final List<ItemStack> stuff = new ArrayList<>();
        int cock = -2;
        final ItemStack legs = ((EntityPlayer) target).getCurrentArmor(1);
        if (legs != null) {
            stuff.add(legs);
        }

        for (final ItemStack yes : stuff) {
            if (Minecraft.getMinecraft().theWorld != null) {
                RenderHelper.enableGUIStandardItemLighting();
                cock += 20;
            }
            GlStateManager.pushMatrix();
            GlStateManager.disableAlpha();
            GlStateManager.clear(256);
            GlStateManager.enableBlend();
            Minecraft.getMinecraft().getRenderItem().renderItemIntoGUI(yes, cock + x, y);
            GlStateManager.disableBlend();
            GlStateManager.scale(0.5, 0.5, 0.5);
            GlStateManager.disableDepth();
            GlStateManager.disableLighting();
            GlStateManager.enableDepth();
            GlStateManager.scale(2.0f, 2.0f, 2.0f);
            GlStateManager.enableAlpha();
            GlStateManager.popMatrix();
        }
        GL11.glPopMatrix();
    }

    private void drawBoots(final int x, final int y) {
        if (target == null || !(target instanceof EntityPlayer)) return;
        GL11.glPushMatrix();
        final List<ItemStack> stuff = new ArrayList<>();
        int cock = -2;
        final ItemStack boots = ((EntityPlayer) target).getCurrentArmor(0);
        if (boots != null) {
            stuff.add(boots);
        }

        for (final ItemStack yes : stuff) {
            if (Minecraft.getMinecraft().theWorld != null) {
                RenderHelper.enableGUIStandardItemLighting();
                cock += 20;
            }
            GlStateManager.pushMatrix();
            GlStateManager.disableAlpha();
            GlStateManager.clear(256);
            GlStateManager.enableBlend();
            Minecraft.getMinecraft().getRenderItem().renderItemIntoGUI(yes, cock + x, y);
            GlStateManager.disableBlend();
            GlStateManager.scale(0.5, 0.5, 0.5);
            GlStateManager.disableDepth();
            GlStateManager.disableLighting();
            GlStateManager.enableDepth();
            GlStateManager.scale(2.0f, 2.0f, 2.0f);
            GlStateManager.enableAlpha();
            GlStateManager.popMatrix();
        }
        GL11.glPopMatrix();
    }
}
