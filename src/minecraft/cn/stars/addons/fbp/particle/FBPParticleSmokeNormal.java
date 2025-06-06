package cn.stars.addons.fbp.particle;

import cn.stars.addons.fbp.FBP;
import cn.stars.addons.fbp.util.FBPRenderUtil;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.client.particle.EntitySmokeFX;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import javax.vecmath.Vector2f;
import java.util.List;

public class FBPParticleSmokeNormal extends EntitySmokeFX {
	Minecraft mc;

	double startScale, scaleAlpha, prevParticleScale, prevParticleAlpha;
	double endMult = 0.75;

	Vec3[] cube;

	Vector2f par;

	EntitySmokeFX original;

	protected FBPParticleSmokeNormal(World worldIn, double xCoordIn, double yCoordIn, double zCoordIn, final double mX,
			final double mY, final double mZ, float scale, boolean b, TextureAtlasSprite tex, EntitySmokeFX original) {
		super(worldIn, xCoordIn, yCoordIn, zCoordIn, mX, mY, mZ, scale);

		this.original = original;

		this.motionX = mX;
		this.motionY = mY;
		this.motionZ = mZ;

		mc = Minecraft.getMinecraft();
		this.particleIcon = tex;

		scaleAlpha = particleScale * 0.85;

		Block block = worldIn.getBlockState(new BlockPos(posX, posY, posZ)).getBlock();

		if (block == Blocks.fire) {
			this.particleScale *= 0.65f;
			this.particleGravity *= 0.25f;

			this.motionX = FBP.random.nextDouble(-0.05, 0.05);
			this.motionY = FBP.random.nextDouble() * 0.5;
			this.motionZ = FBP.random.nextDouble(-0.05, 0.05);

			this.motionY *= 0.35f;

			scaleAlpha = particleScale * 0.5;

			particleMaxAge = FBP.random.nextInt(7, 18);
		} else if (block == Blocks.torch) {
			particleScale *= 0.45f;

			this.motionX = FBP.random.nextDouble(-0.05, 0.05);
			this.motionY = FBP.random.nextDouble() * 0.5;
			this.motionZ = FBP.random.nextDouble(-0.05, 0.05);

			this.motionX *= 0.925f;
			this.motionY = 0.005f;
			this.motionZ *= 0.925f;

			this.particleRed = 0.275f;
			this.particleGreen = 0.275f;
			this.particleBlue = 0.275f;

			scaleAlpha = particleScale * 0.75;

			particleMaxAge = FBP.random.nextInt(5, 10);
		} else {
			particleScale = scale;
			motionY *= 0.935;
		}

		particleScale *= FBP.scaleMult;

		startScale = particleScale;

		float angleY = rand.nextFloat() * 80;

		cube = new Vec3[FBP.CUBE.length];

		for (int i = 0; i < FBP.CUBE.length; i++) {
			Vec3 vec = FBP.CUBE[i];
			cube[i] = FBPRenderUtil.rotatef_d(vec, 0, (float) angleY, 0);
		}

		particleAlpha = 1f;

		if (FBP.randomFadingSpeed)
			endMult = MathHelper.clamp_double(FBP.random.nextDouble(0.425, 1.15), 0.5432, 1);

		multipleParticleScaleBy(1);
	}

	@Override
	public EntityFX multipleParticleScaleBy(float scale) {
		EntityFX p = super.multipleParticleScaleBy(scale);

		float f = particleScale / 20;

		this.setEntityBoundingBox(new AxisAlignedBB(posX - f, posY - f, posZ - f, posX + f, posY + f, posZ + f));

		return p;
	}

	@Override
	public int getFXLayer() {
		return 1;
	}

	@Override
	public void onUpdate() {
		prevPosX = posX;
		prevPosY = posY;
		prevPosZ = posZ;

		prevParticleAlpha = particleAlpha;
		prevParticleScale = particleScale;

		if (!FBP.fancySmoke)
			this.isDead = true;

		if (++this.particleAge >= this.particleMaxAge) {
			if (FBP.randomFadingSpeed)
				particleScale *= 0.887654321F * endMult;
			else
				particleScale *= 0.887654321F;

			if (particleAlpha > 0.01 && particleScale <= scaleAlpha) {
				if (FBP.randomFadingSpeed)
					particleAlpha *= 0.7654321F * endMult;
				else
					particleAlpha *= 0.7654321F;
			}

			if (particleAlpha <= 0.01)
				setDead();
		}

		this.motionY += 0.004D;
		this.moveEntity(this.motionX, this.motionY, this.motionZ);

		if (this.posY == this.prevPosY) {
			this.motionX *= 1.1D;
			this.motionZ *= 1.1D;
		}

		this.motionX *= 0.9599999785423279D;
		this.motionY *= 0.9599999785423279D;
		this.motionZ *= 0.9599999785423279D;

		if (this.onGround) {
			this.motionX *= 0.899999988079071D;
			this.motionZ *= 0.899999988079071D;
		}
	}

	@Override
	public void moveEntity(double x, double y, double z) {
		double X = x;
		double Y = y;
		double Z = z;

		List<AxisAlignedBB> list = this.worldObj.getCollidingBoundingBoxes(this,
				this.getEntityBoundingBox().addCoord(x, y, z));

		for (AxisAlignedBB axisalignedbb : list) {
			y = axisalignedbb.calculateYOffset(this.getEntityBoundingBox(), y);
		}

		this.setEntityBoundingBox(this.getEntityBoundingBox().offset(0.0D, y, 0.0D));

		for (AxisAlignedBB axisalignedbb : list) {
			x = axisalignedbb.calculateXOffset(this.getEntityBoundingBox(), x);
		}

		this.setEntityBoundingBox(this.getEntityBoundingBox().offset(x, 0.0D, 0.0D));

		for (AxisAlignedBB axisalignedbb : list) {
			z = axisalignedbb.calculateZOffset(this.getEntityBoundingBox(), z);
		}

		this.setEntityBoundingBox(this.getEntityBoundingBox().offset(0.0D, 0.0D, z));

		// RESET
		AxisAlignedBB axisalignedbb = this.getEntityBoundingBox();
		this.posX = (axisalignedbb.minX + axisalignedbb.maxX) / 2.0D;
		this.posY = (axisalignedbb.minY + axisalignedbb.maxY) / 2.0D;
		this.posZ = (axisalignedbb.minZ + axisalignedbb.maxZ) / 2.0D;

		this.onGround = y != Y;
	}

	@Override
	public void renderParticle(WorldRenderer worldRendererIn, Entity entityIn, float partialTicks, float rotationX,
			float rotationZ, float rotationYZ, float rotationXY, float rotationXZ) {
		if (!FBP.isEnabled() && particleMaxAge != 0)
			particleMaxAge = 0;

		float f = particleIcon.getInterpolatedU((0.1f + 1) / 4 * 16);
		float f1 = particleIcon.getInterpolatedV((0.1f + 1) / 4 * 16);

		float f5 = (float) (prevPosX + (posX - prevPosX) * partialTicks - interpPosX);
		float f6 = (float) (prevPosY + (posY - prevPosY) * partialTicks - interpPosY);
		float f7 = (float) (prevPosZ + (posZ - prevPosZ) * partialTicks - interpPosZ);

		int i = getBrightnessForRender(partialTicks);

		float alpha = (float) (prevParticleAlpha + (particleAlpha - prevParticleAlpha) * partialTicks);
		;

		// SMOOTH TRANSITION
		float f4 = (float) (prevParticleScale + (particleScale - prevParticleScale) * partialTicks);

		// RENDER
		par = new Vector2f(f, f1);

		worldRendererIn.setTranslation(f5, f6, f7);
		putCube(worldRendererIn, f4 / 20, i >> 16 & 65535, i & 65535, particleRed, particleGreen, particleBlue, alpha);
		worldRendererIn.setTranslation(0, 0, 0);
	}

	public void putCube(WorldRenderer worldRendererIn, double scale, int j, int k, float r, float g, float b, float a) {
		float brightnessForRender = 1;

		float R = 0;
		float G = 0;
		float B = 0;

		for (int i = 0; i < cube.length; i += 4) {
			Vec3 v1 = cube[i];
			Vec3 v2 = cube[i + 1];
			Vec3 v3 = cube[i + 2];
			Vec3 v4 = cube[i + 3];

			R = r * brightnessForRender;
			G = g * brightnessForRender;
			B = b * brightnessForRender;

			brightnessForRender *= 0.875;

			addVt(worldRendererIn, scale, v1, par.x, par.y, j, k, R, G, B, a);
			addVt(worldRendererIn, scale, v2, par.x, par.y, j, k, R, G, B, a);
			addVt(worldRendererIn, scale, v3, par.x, par.y, j, k, R, G, B, a);
			addVt(worldRendererIn, scale, v4, par.x, par.y, j, k, R, G, B, a);
		}
	}

	private void addVt(WorldRenderer worldRendererIn, double scale, Vec3 pos, double u, double v, int j, int k, float r,
			float g, float b, float a) { // add vertex to buffer
		worldRendererIn.pos(pos.xCoord * scale, pos.yCoord * scale, pos.zCoord * scale).tex(u, v).color(r, g, b, a)
				.lightmap(j, k).endVertex();
	}

	@Override
	public int getBrightnessForRender(float p_189214_1_) {
		int i = super.getBrightnessForRender(p_189214_1_);
		int j = 0;

		if (this.worldObj.isBlockLoaded(new BlockPos(posX, posY, posZ))) {
			j = this.worldObj.getCombinedLight(new BlockPos(posX, posY, posZ), 0);
		}

		return i == 0 ? j : i;
	}

	@Override
	public void setDead() {
		this.isDead = true;

		original.setDead();
	}

	public void setMaxAge(int maxAge) {
		this.particleMaxAge = maxAge;
	}
}
