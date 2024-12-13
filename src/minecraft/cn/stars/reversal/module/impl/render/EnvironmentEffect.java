package cn.stars.reversal.module.impl.render;

import cn.stars.reversal.event.impl.Render3DEvent;
import cn.stars.reversal.event.impl.UpdateEvent;
import cn.stars.reversal.module.Category;
import cn.stars.reversal.module.Module;
import cn.stars.reversal.module.ModuleInfo;
import cn.stars.reversal.util.animation.simple.AnimationUtils2;
import cn.stars.reversal.util.math.MathUtil;
import cn.stars.reversal.util.math.RandomUtil;
import cn.stars.reversal.util.render.*;
import cn.stars.reversal.value.impl.BoolValue;
import cn.stars.reversal.value.impl.ModeValue;
import cn.stars.reversal.value.impl.NumberValue;
import lombok.Getter;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@ModuleInfo(name = "EnvironmentEffect", chineseName = "环境效果", description = "Draw modern environment effects around you", chineseDescription = "在你的身边绘制时髦的环境效果", category = Category.RENDER)
public class EnvironmentEffect extends Module {
    private final ModeValue mode = new ModeValue("Mode", this, "Firefly", "Firefly", "Stars", "Snowflake");
    private final NumberValue count = new NumberValue("Count", this, 30, 10, 500, 1);
    private final BoolValue darkImprint = new BoolValue("Dark Imprint", this, false);
    private final BoolValue lighting = new BoolValue("Lighting", this, false);
    private final NumberValue spawnDelay = new NumberValue("Spawn Delay", this, 3.0f, 1.0f, 10.0f, 1.0f);
    private final ArrayList<FirePart> FIRE_PARTS_LIST = new ArrayList<>();
    public final ResourceLocation FIRE_PART_TEX = new ResourceLocation("reversal/images/texture/fireflies/firepart.png");
    public final ResourceLocation STARS_TEX = new ResourceLocation("reversal/images/texture/fireflies/star.png");
    public final ResourceLocation SNOWFLAKE_TEX = new ResourceLocation("reversal/images/texture/fireflies/snowflake.png");
    private final Tessellator tessellator = Tessellator.getInstance();
    private final WorldRenderer buffer = this.tessellator.getWorldRenderer();

    @Override
    public void onUpdate(UpdateEvent event) {
        if (mc.thePlayer != null && mc.thePlayer.ticksExisted == 1) {
            this.FIRE_PARTS_LIST.forEach(FirePart::setToRemove);
        }
        this.FIRE_PARTS_LIST.forEach(FirePart::updatePart);
        this.FIRE_PARTS_LIST.removeIf(FirePart::isToRemove);
        if (mc.thePlayer.ticksExisted % (int) (this.spawnDelay.getValue() + 1.0f) == 0 && this.FIRE_PARTS_LIST.size() < count.getInt()) {
            this.FIRE_PARTS_LIST.add(new FirePart(this.generateVecForPart(10.0, 4.0), this.getMaxPartAliveTime()));
            this.FIRE_PARTS_LIST.add(new FirePart(this.generateVecForPart(6.0, 5.0), this.getMaxPartAliveTime()));
        }
    }

    @Override
    public void onRender3D(Render3DEvent event) {
        if (!this.FIRE_PARTS_LIST.isEmpty()) {
            this.setupGLDrawsFireParts(() -> {
                switch (mode.getMode()) {
                    case "Firefly": {
                        this.bindResource(this.FIRE_PART_TEX);
                        break;
                    }
                    case "Stars": {
                        this.bindResource(this.STARS_TEX);
                        break;
                    }
                    case "Snowflake": {
                        this.bindResource(this.SNOWFLAKE_TEX);

                        break;
                    }
                }
                this.FIRE_PARTS_LIST.forEach(part -> this.drawPart(part, event.getPartialTicks()));
            });
        }
    }

    private long getMaxPartAliveTime() {
        return 6000L;
    }

    private int getPartColor() {
        return ThemeUtil.getThemeColorInt(ThemeType.ARRAYLIST);
    }

    private float getRandom(double min, double max) {
        return (float) RandomUtil.INSTANCE.nextDouble(min, max);
    }

    private Vec3 generateVecForPart(double rangeXZ, double rangeY) {
        Vec3 pos = mc.thePlayer.getPositionVector().addVector(this.getRandom(-rangeXZ, rangeXZ), this.getRandom(-rangeY / 2.0, rangeY), this.getRandom(-rangeXZ, rangeXZ));
        for (int i = 0; i < 30; ++i) {
            pos = mc.thePlayer.getPositionVector().addVector(this.getRandom(-rangeXZ, rangeXZ), this.getRandom(-rangeY / 2.0, rangeY), this.getRandom(-rangeXZ, rangeXZ));
        }
        return pos;
    }

    private void setupGLDrawsFireParts(Runnable partsRender) {
        double glX = mc.getRenderManager().viewerPosX;
        double glY = mc.getRenderManager().viewerPosY;
        double glZ = mc.getRenderManager().viewerPosZ;
        GL11.glPushMatrix();
        GlStateManager.tryBlendFuncSeparate(770, 1, 1, 0);
        EnvironmentEffect.mc.entityRenderer.disableLightmap();
        GL11.glEnable(3042);
        GL11.glLineWidth(1.0f);
        GL11.glEnable(3553);
        GL11.glDisable(2896);
        GL11.glShadeModel(7425);
        GL11.glDisable(3008);
        GL11.glDisable(2884);
        GL11.glDepthMask(false);
        GL11.glTranslated(-glX, -glY, -glZ);
        partsRender.run();
        GL11.glTranslated(glX, glY, glZ);
        GL11.glDepthMask(true);
        GL11.glEnable(2884);
        GL11.glEnable(3008);
        GL11.glLineWidth(1.0f);
        GL11.glShadeModel(7424);
        GL11.glEnable(3553);
        GlStateManager.resetColor();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GL11.glPopMatrix();
    }

    private void bindResource(ResourceLocation toBind) {
        mc.getTextureManager().bindTexture(toBind);
    }

    private void drawBindedTexture(float x, float y, float x2, float y2, int c) {
        this.buffer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
        this.buffer.pos(x, y).tex(0.0, 0.0).color(c).endVertex();
        this.buffer.pos(x, y2).tex(0.0, 1.0).color(c).endVertex();
        this.buffer.pos(x2, y2).tex(1.0, 1.0).color(c).endVertex();
        this.buffer.pos(x2, y).tex(1.0, 0.0).color(c).endVertex();
        this.tessellator.draw();
    }

    private void drawPart(FirePart part, float pTicks) {
        int color = this.getPartColor();
        if (this.darkImprint.isEnabled()) {
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            this.drawSparkPartsList(color, part, pTicks);
            this.drawTrailPartsList(color, part);
            GlStateManager.tryBlendFuncSeparate(770, 1, 1, 0);
        } else {
            this.drawSparkPartsList(color, part, pTicks);
            this.drawTrailPartsList(color, part);
        }
        Vec3 pos = part.getRenderPosVec(pTicks);
        GL11.glPushMatrix();
        GL11.glTranslated(pos.xCoord, pos.yCoord, pos.zCoord);
        GL11.glNormal3d(1.0, 1.0, 1.0);
        GL11.glRotated(-EnvironmentEffect.mc.getRenderManager().playerViewY, 0.0, 1.0, 0.0);
        GL11.glRotated(EnvironmentEffect.mc.getRenderManager().playerViewX, EnvironmentEffect.mc.gameSettings.thirdPersonView == 2 ? -1.0 : 1.0, 0.0, 0.0);
        GL11.glScaled(-0.1, -0.1, 0.1);
        float scale = 7.0f;
        this.drawBindedTexture(-scale / 2.0f, -scale / 2.0f, scale / 2.0f, scale / 2.0f, color);
        if (this.lighting.isEnabled()) {
            this.drawBindedTexture(-(scale *= 3.0f) / 2.0f, -scale / 2.0f, scale / 2.0f, scale / 2.0f, ColorUtils.INSTANCE.applyOpacity(ColorUtil.darker(color, 0.2f), (float) ColorUtil.getAlphaFromColor(color) / 7.0f));
        }
        GL11.glPopMatrix();
    }

    private void drawSparkPartsList(int color, FirePart firePart, float partialTicks) {
        if (firePart.SPARK_PARTS.size() < 2) {
            return;
        }
        GL11.glDisable(3553);
        GL11.glEnable(3042);
        GL11.glDisable(3008);
        GL11.glEnable(2832);
        GL11.glPointSize(1.5f + 6.0f * MathHelper.clamp_float(1.0f - (mc.thePlayer.getSmoothDistanceToCoord((float) firePart.getPosVec().xCoord, (float) firePart.getPosVec().yCoord + 1.6f, (float) firePart.getPosVec().zCoord) - 3.0f) / 10.0f, 0.0f, 1.0f));
        GL11.glBegin(0);
        for (SparkPart spark : firePart.SPARK_PARTS) {
            int c = ColorUtils.INSTANCE.applyOpacity(ColorUtils.INSTANCE.interpolateColor(-1, color, (float) spark.timePC()), (float) ColorUtil.getAlphaFromColor(color) * (float) 1 * (1.0f - (float) spark.timePC()));
            RenderUtil.color(c);
            GL11.glVertex3d(spark.getRenderPosX(partialTicks), spark.getRenderPosY(partialTicks), spark.getRenderPosZ(partialTicks));
        }
        GL11.glEnd();
        GlStateManager.resetColor();
        GL11.glEnable(3008);
        GL11.glEnable(3553);
    }

    private void drawTrailPartsList(int color, FirePart firePart) {
        if (firePart.TRAIL_PARTS.size() < 2) {
            return;
        }
        GL11.glDisable(3553);
        GL11.glLineWidth(1.0E-5f + 8.0f * MathHelper.clamp_float(1.0f - (mc.thePlayer.getSmoothDistanceToCoord((float) firePart.getPosVec().xCoord, (float) firePart.getPosVec().yCoord + 1.6f, (float) firePart.getPosVec().zCoord) - 3.0f) / 20.0f, 0.0f, 1.0f));
        GL11.glEnable(3042);
        GL11.glDisable(3008);
        GL11.glEnable(2848);
        GL11.glHint(3154, 4354);
        int point = 0;
        int pointsCount = firePart.TRAIL_PARTS.size();
        GL11.glBegin(3);
        for (TrailPart trail : firePart.TRAIL_PARTS) {
            float sizePC = (float) point / (float) pointsCount;
            sizePC = ((double) sizePC > 0.5 ? 1.0f - sizePC : sizePC) * 2.0f;
            sizePC = sizePC > 1.0f ? 1.0f : (Math.max(sizePC, 0.0f));
            int c = ColorUtils.INSTANCE.applyOpacity(color, (float) ColorUtil.getAlphaFromColor(color) * (float) 1 * sizePC);
            RenderUtil.color(c);
            GL11.glVertex3d(trail.x, trail.y, trail.z);
            ++point;
        }
        GL11.glEnd();
        GlStateManager.resetColor();
        GL11.glEnable(3008);
        GL11.glDisable(2848);
        GL11.glHint(3154, 4352);
        GL11.glLineWidth(1.0f);
        GL11.glEnable(3553);
    }

    private class FirePart {
        List<TrailPart> TRAIL_PARTS;
        List<SparkPart> SPARK_PARTS = new ArrayList<>();
        Vec3 prevPos;
        Vec3 pos;
        AnimationUtils2 alphaPC = new AnimationUtils2(0.0f, 1.0f, 0.02f);
        int msChangeSideRate = this.getMsChangeSideRate();
        float moveYawSet = EnvironmentEffect.this.getRandom(0.0, 360.0);
        float speed = EnvironmentEffect.this.getRandom(0.1, 0.25);
        float yMotion = EnvironmentEffect.this.getRandom(-0.075, 0.1);
        float moveYaw = this.moveYawSet;
        float maxAlive;
        long startTime;
        long rateTimer = this.startTime = System.currentTimeMillis();
        @Getter
        boolean toRemove;

        public FirePart(Vec3 pos, float maxAlive) {
            this.pos = pos;
            this.prevPos = pos;
            this.maxAlive = maxAlive;
            TRAIL_PARTS = new ArrayList<>();
        }

        public float getTimePC() {
            return MathHelper.clamp_float((float) (System.currentTimeMillis() - this.startTime) / this.maxAlive, 0.0f, 1.0f);
        }

        public void setAlphaPCTo(float to) {
            this.alphaPC.to = to;
        }

        public float getAlphaPC() {
            return this.alphaPC.getAnim();
        }

        public Vec3 getPosVec() {
            return this.pos;
        }

        public Vec3 getRenderPosVec(float pTicks) {
            Vec3 pos = this.getPosVec();
            return pos.addVector(-(this.prevPos.xCoord - pos.xCoord) * (double) pTicks, -(this.prevPos.yCoord - pos.yCoord) * (double) pTicks, -(this.prevPos.zCoord - pos.zCoord) * (double) pTicks);
        }

        public void updatePart() {
            if (System.currentTimeMillis() - this.rateTimer >= (long) this.msChangeSideRate) {
                this.msChangeSideRate = this.getMsChangeSideRate();
                this.rateTimer = System.currentTimeMillis();
                this.moveYawSet = EnvironmentEffect.this.getRandom(0.0, 360.0);
            }
            this.moveYaw = MathUtil.lerp(this.moveYaw, this.moveYawSet, 0.065f);
            float motionX = -((float) Math.sin(Math.toRadians(this.moveYaw))) * (this.speed /= 1.005f);
            float motionZ = (float) Math.cos(Math.toRadians(this.moveYaw)) * this.speed;
            this.prevPos = this.pos;
            float scaleBox = 0.1f;
            float delente = !mc.theWorld.getCollisionBoxes(new AxisAlignedBB(this.pos.xCoord - (double) (scaleBox / 2.0f), this.pos.yCoord, this.pos.zCoord - (double) (scaleBox / 2.0f), this.pos.xCoord + (double) (scaleBox / 2.0f), this.pos.yCoord + (double) scaleBox, this.pos.zCoord + (double) (scaleBox / 2.0f))).isEmpty() ? 0.3f : 1.0f;
            this.pos = this.pos.addVector(motionX / delente, (this.yMotion /= 1.02f) / delente, motionZ / delente);
            if (this.getTimePC() >= 1.0f) {
                this.setAlphaPCTo(0.0f);
                if (this.getAlphaPC() < 0.003921569f) {
                    this.setToRemove();
                }
            }
            this.TRAIL_PARTS.add(new TrailPart(this, 400));
            if (!this.TRAIL_PARTS.isEmpty()) {
                this.TRAIL_PARTS.removeIf(TrailPart::toRemove);
            }
            for (int i = 0; i < 2; ++i) {
                this.SPARK_PARTS.add(new SparkPart(this, 300));
            }
            this.SPARK_PARTS.forEach(SparkPart::motionSparkProcess);
            if (!this.SPARK_PARTS.isEmpty()) {
                this.SPARK_PARTS.removeIf(SparkPart::toRemove);
            }
        }

        public void setToRemove() {
            this.toRemove = true;
        }

        int getMsChangeSideRate() {
            return (int) EnvironmentEffect.this.getRandom(300.5, 900.5);
        }
    }

    private class SparkPart {
        double posX;
        double posY;
        double posZ;
        double prevPosX;
        double prevPosY;
        double prevPosZ;
        double speed = Math.random() / 30.0;
        double radianYaw = Math.random() * 360.0;
        double radianPitch = -90.0 + Math.random() * 180.0;
        long startTime = System.currentTimeMillis();
        int maxTime;

        SparkPart(FirePart part, int maxTime) {
            this.maxTime = maxTime;
            this.posX = part.getPosVec().xCoord;
            this.posY = part.getPosVec().yCoord;
            this.posZ = part.getPosVec().zCoord;
            this.prevPosX = this.posX;
            this.prevPosY = this.posY;
            this.prevPosZ = this.posZ;
        }

        double timePC() {
            return MathHelper.clamp_float((float) (System.currentTimeMillis() - this.startTime) / (float) this.maxTime, 0.0f, 1.0f);
        }

        boolean toRemove() {
            return this.timePC() == 1.0;
        }

        void motionSparkProcess() {
            double radYaw = Math.toRadians(this.radianYaw);
            this.prevPosX = this.posX;
            this.prevPosY = this.posY;
            this.prevPosZ = this.posZ;
            this.posX += Math.sin(radYaw) * this.speed;
            this.posY += Math.cos(Math.toRadians(this.radianPitch - 90.0)) * this.speed;
            this.posZ += Math.cos(radYaw) * this.speed;
        }

        double getRenderPosX(float partialTicks) {
            return this.prevPosX + (this.posX - this.prevPosX) * (double) partialTicks;
        }

        double getRenderPosY(float partialTicks) {
            return this.prevPosY + (this.posY - this.prevPosY) * (double) partialTicks;
        }

        double getRenderPosZ(float partialTicks) {
            return this.prevPosZ + (this.posZ - this.prevPosZ) * (double) partialTicks;
        }
    }

    private class TrailPart {
        double x;
        double y;
        double z;
        long startTime = System.currentTimeMillis();
        int maxTime;

        public TrailPart(FirePart part, int maxTime) {
            this.maxTime = maxTime;
            this.x = part.getPosVec().xCoord;
            this.y = part.getPosVec().yCoord;
            this.z = part.getPosVec().zCoord;
        }

        public float getTimePC() {
            return MathHelper.clamp_float((float) (System.currentTimeMillis() - this.startTime) / (long) this.maxTime, 0.0f, 1.0f);
        }

        public boolean toRemove() {
            return this.getTimePC() == 1.0f;
        }
    }
}
