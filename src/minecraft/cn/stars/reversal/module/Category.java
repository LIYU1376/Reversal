package cn.stars.reversal.module;

import cn.stars.reversal.util.animation.rise.Animation;
import cn.stars.reversal.util.animation.rise.Easing;

public enum Category {
    COMBAT,
    MOVEMENT,
    PLAYER,
    RENDER,
    MISC,
    WORLD,
    HUD,
    ADDONS;

    public Animation alphaAnimation = new Animation(Easing.EASE_OUT_EXPO, 800);
}