package net.minecraft.potion;

import cn.stars.reversal.util.animation.rise.Animation;
import cn.stars.reversal.util.animation.rise.Easing;
import cn.stars.reversal.util.math.TimeUtil;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.NBTTagCompound;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Setter
@Getter
public class PotionEffect
{
    private static final Logger LOGGER = LogManager.getLogger();
    public int potionID;
    public int initialDuration;
    public int duration;
    public int amplifier;
    private boolean isSplashPotion;
    private boolean isAmbient;
    private boolean isPotionDurationMax;
    private boolean showParticles;

    private final Animation yAnimation = new Animation(Easing.EASE_OUT_EXPO, 1000);
    private final Animation xAnimation = new Animation(Easing.EASE_OUT_EXPO, 1000);
    private final Animation progressAnimation = new Animation(Easing.EASE_OUT_EXPO, 1000);
    private double progress;
    private final TimeUtil progressTimer = new TimeUtil();

    public PotionEffect(int id, int effectDuration)
    {
        this(id, effectDuration, 0);
    }

    public PotionEffect(int id, int effectDuration, int effectAmplifier)
    {
        this(id, effectDuration, effectAmplifier, false, true);
    }

    public PotionEffect(int id, int effectDuration, int effectAmplifier, boolean ambient, boolean showParticles)
    {
        this.potionID = id;
        this.initialDuration = effectDuration;
        this.duration = effectDuration;
        this.amplifier = effectAmplifier;
        this.isAmbient = ambient;
        this.showParticles = showParticles;
    }

    public PotionEffect(PotionEffect other)
    {
        this.potionID = other.potionID;
        this.initialDuration = other.initialDuration;
        this.duration = other.duration;
        this.amplifier = other.amplifier;
        this.isAmbient = other.isAmbient;
        this.showParticles = other.showParticles;
    }

    public void combine(PotionEffect other)
    {
        if (this.potionID != other.potionID)
        {
            LOGGER.warn("This method should only be called for matching effects!");
        }

        if (other.amplifier > this.amplifier)
        {
            this.amplifier = other.amplifier;
            this.initialDuration = other.initialDuration;
            this.duration = other.duration;
        }
        else if (other.amplifier == this.amplifier && this.duration < other.duration)
        {
            this.initialDuration = other.initialDuration;
            this.duration = other.duration;
        }
        else if (!other.isAmbient && this.isAmbient)
        {
            this.isAmbient = other.isAmbient;
        }

        this.showParticles = other.showParticles;
    }

    public void setSplashPotion(boolean splashPotion)
    {
        this.isSplashPotion = splashPotion;
    }

    public boolean getIsAmbient()
    {
        return this.isAmbient;
    }

    public boolean getIsShowParticles()
    {
        return this.showParticles;
    }

    public boolean onUpdate(EntityLivingBase entityIn)
    {
        if (this.duration > 0)
        {
            if (Potion.potionTypes[this.potionID].isReady(this.duration, this.amplifier))
            {
                this.performEffect(entityIn);
            }
            this.deincrementDuration();
        }

        return this.duration > 0;
    }

    private void deincrementDuration()
    {
        --this.duration;
    }

    public void performEffect(EntityLivingBase entityIn)
    {
        if (this.duration > 0)
        {
            Potion.potionTypes[this.potionID].performEffect(entityIn, this.amplifier);
        }
    }

    public String getEffectName()
    {
        return Potion.potionTypes[this.potionID].getName();
    }

    public int hashCode()
    {
        return this.potionID;
    }

    public String toString()
    {
        String s = "";

        if (this.getAmplifier() > 0)
        {
            s = this.getEffectName() + " x " + (this.getAmplifier() + 1) + ", Duration: " + this.getDuration();
        }
        else
        {
            s = this.getEffectName() + ", Duration: " + this.getDuration();
        }

        if (this.isSplashPotion)
        {
            s = s + ", Splash: true";
        }

        if (!this.showParticles)
        {
            s = s + ", Particles: false";
        }

        return Potion.potionTypes[this.potionID].isUsable() ? "(" + s + ")" : s;
    }

    public boolean equals(Object p_equals_1_)
    {
        if (!(p_equals_1_ instanceof PotionEffect))
        {
            return false;
        }
        else
        {
            PotionEffect potioneffect = (PotionEffect)p_equals_1_;
            return this.potionID == potioneffect.potionID && this.amplifier == potioneffect.amplifier && this.duration == potioneffect.duration && this.isSplashPotion == potioneffect.isSplashPotion && this.isAmbient == potioneffect.isAmbient;
        }
    }

    public NBTTagCompound writeCustomPotionEffectToNBT(NBTTagCompound nbt)
    {
        nbt.setByte("Id", (byte)this.getPotionID());
        nbt.setByte("Amplifier", (byte)this.getAmplifier());
        nbt.setInteger("Duration", this.getDuration());
        nbt.setBoolean("Ambient", this.getIsAmbient());
        nbt.setBoolean("ShowParticles", this.getIsShowParticles());
        return nbt;
    }

    public static PotionEffect readCustomPotionEffectFromNBT(NBTTagCompound nbt)
    {
        int i = nbt.getByte("Id");

        if (i >= 0 && i < Potion.potionTypes.length && Potion.potionTypes[i] != null)
        {
            int j = nbt.getByte("Amplifier");
            int k = nbt.getInteger("Duration");
            boolean flag = nbt.getBoolean("Ambient");
            boolean flag1 = true;

            if (nbt.hasKey("ShowParticles", 1))
            {
                flag1 = nbt.getBoolean("ShowParticles");
            }

            return new PotionEffect(i, k, j, flag, flag1);
        }
        else
        {
            return null;
        }
    }

    public void setPotionDurationMax(boolean maxDuration)
    {
        this.isPotionDurationMax = maxDuration;
    }

    public boolean getIsPotionDurationMax()
    {
        return this.isPotionDurationMax;
    }
}
