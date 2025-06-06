package cn.stars.reversal.module.impl.addons;

import cn.stars.addons.fbp.FBP;
import cn.stars.reversal.event.impl.TickEvent;
import cn.stars.reversal.event.impl.ValueChangedEvent;
import cn.stars.reversal.event.impl.WorldEvent;
import cn.stars.reversal.module.Category;
import cn.stars.reversal.module.Module;
import cn.stars.reversal.module.ModuleInfo;
import cn.stars.reversal.value.impl.BoolValue;
import cn.stars.reversal.value.impl.NumberValue;

@ModuleInfo(name = "FancyBlockParticles", localizedName = "module.FancyBlockParticles.name", description = "Make the particles look better", localizedDescription = "module.FancyBlockParticles.desc", category = Category.ADDONS)
public class FancyBlockParticles extends Module {
    public final NumberValue minAge = new NumberValue("Min Age", this, 10, 1, 100, 1);
    public final NumberValue maxAge = new NumberValue("Max Age", this, 55, 1, 100, 1);
    public final NumberValue scaleMult = new NumberValue("Scale Multiplier", this, 0.75, 0.1, 2, 0.1);
    public final NumberValue gravityMult = new NumberValue("Gravity Multiplier", this, 1.0, 0.1, 2, 0.1);
    public final NumberValue rotationMult = new NumberValue("Rotation Multiplier", this, 1.0, 0.1, 2, 0.1);
    public final NumberValue particlesPerAxis = new NumberValue("Particles Per Axis", this, 4, 1, 10, 1);
    public final NumberValue weatherParticleDensity = new NumberValue("Weather Particle Density", this, 5.0, 0.1, 10.0, 0.1);
    public final BoolValue lowTraction = new BoolValue("Low Traction", this, false);
    public final BoolValue bounceOffWalls = new BoolValue("Bounce Off Walls", this, true);
    public final BoolValue randomRotation = new BoolValue("Random Rotation", this, true);
    public final BoolValue cartoonMode = new BoolValue("Cartoon Mode", this, false);
    public final BoolValue entityCollision = new BoolValue("Entity Collision", this, false);
    public final BoolValue randomizedScale = new BoolValue("Randomized Scale", this, true);
    public final BoolValue randomFadingSpeed = new BoolValue("Random Fading Speed", this, true);
    public final BoolValue infiniteDuration = new BoolValue("Infinite Duration", this, false);
    public final BoolValue spawnWhileFrozen = new BoolValue("Spawn While Frozen", this, true);
    public final BoolValue smartBreaking = new BoolValue("Smart Breaking", this, true);
    public final BoolValue fancyRain = new BoolValue("Fancy Rain", this, true);
    public final BoolValue fancySnow = new BoolValue("Fancy Snow", this, true);
    public final BoolValue fancySmoke = new BoolValue("Fancy Smoke", this, true);
    public final BoolValue fancyFlame = new BoolValue("Fancy Flame", this, true);
    public final BoolValue fancyAdditions = new BoolValue("Fancy Additions", this, false);
    public final BoolValue waterPhysics = new BoolValue("Water Physics", this, true);
    public final BoolValue restOnFloor = new BoolValue("Rest On Floor", this, true);

    @Override
    public void onTick(TickEvent event) {
        if (!mc.isGamePaused() && mc.theWorld != null && FBP.fancyWeatherRenderer != null) {
            FBP.fancyWeatherRenderer.onUpdate();
        }
    }

    @Override
    public void onWorld(WorldEvent event) {
        FBP.initRenderers(event.getWorld(), mc.renderEngine);
    }

    @Override
    protected void onEnable() {
        FBP.initRenderers(mc.theWorld, mc.renderEngine);
        FBP.setEnabled(true);
    }

    @Override
    protected void onDisable() {
        FBP.initRenderers(mc.theWorld, mc.renderEngine);
        FBP.setEnabled(false);
    }

    @Override
    public void onValueChanged(ValueChangedEvent event) {
        updateAllValues();
    }

    public void updateAllValues() {
        FBP.minAge = minAge.getInt();
        FBP.maxAge = maxAge.getInt();
        FBP.scaleMult = scaleMult.getValue();
        FBP.gravityMult = gravityMult.getValue();
        FBP.rotationMult = rotationMult.getValue();
        FBP.particlesPerAxis = particlesPerAxis.getInt();
        FBP.weatherParticleDensity = weatherParticleDensity.getValue();
        FBP.lowTraction = lowTraction.isEnabled();
        FBP.bounceOffWalls = bounceOffWalls.isEnabled();
        FBP.randomRotation = randomRotation.isEnabled();
        FBP.cartoonMode = cartoonMode.isEnabled();
        FBP.entityCollision = entityCollision.isEnabled();
        FBP.randomizedScale = randomizedScale.isEnabled();
        FBP.randomFadingSpeed = randomFadingSpeed.isEnabled();
        FBP.infiniteDuration = infiniteDuration.isEnabled();
        FBP.spawnWhileFrozen = spawnWhileFrozen.isEnabled();
        FBP.smartBreaking = smartBreaking.isEnabled();
        FBP.fancyRain = fancyRain.isEnabled();
        FBP.fancySnow = fancySnow.isEnabled();
        FBP.fancySmoke = fancySmoke.isEnabled();
        FBP.fancyFlame = fancyFlame.isEnabled();
        FBP.fancyAdditions = fancyAdditions.isEnabled();
        FBP.waterPhysics = waterPhysics.isEnabled();
        FBP.restOnFloor = restOnFloor.isEnabled();
    }
}
