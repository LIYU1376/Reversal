package cn.stars.starx.module.impl.addons;

import cn.stars.starx.StarX
import cn.stars.starx.event.impl.Render3DEvent
import cn.stars.starx.module.Category;
import cn.stars.starx.module.Module;
import cn.stars.starx.module.ModuleInfo;
import cn.stars.starx.util.wrapper.WrapperFreeLook
import org.lwjgl.input.Keyboard

@ModuleInfo(name = "FreeLook", chineseName = "自由视角", description = "Move around freely", chineseDescription = "自由地移动你的视角", category = Category.ADDONS)
class FreeLook : Module() {

    init {
        shouldCallNotification = false
    }

    override fun onEnable() {
        if (this.keyBind == Keyboard.KEY_NONE) {
            StarX.showMsg("Bind FreeLook to a key to use!")
            toggleModule()
            return
        }
        using = true
    }

    override fun onDisable() {
        using = false
    }

    override fun onRender3D(event: Render3DEvent) {
        if (mc.currentScreen != null) return
        if (!perspectiveToggled) {
            if (Keyboard.isKeyDown(this.keyBind)) {
                perspectiveToggled = true
                cameraYaw = mc.thePlayer.rotationYaw
                cameraPitch = mc.thePlayer.rotationPitch
                previousPerspective = mc.gameSettings.hideGUI
                mc.gameSettings.thirdPersonView = 1
            } else {
                toggleModule()
            }
        } else if (!Keyboard.isKeyDown(this.keyBind)) {
            perspectiveToggled = false
            mc.gameSettings.thirdPersonView = if (previousPerspective) 1 else 0
            toggleModule()
        }
    }

    companion object {
        @JvmField
        var using = false
        @JvmField
        var perspectiveToggled = false
        @JvmField
        var cameraYaw = 0f
        @JvmField
        var cameraPitch = 0f
        private var previousPerspective = false
        @JvmStatic
        fun getCameraYaw(): Float {
            return WrapperFreeLook.getCameraYaw()
        }

        @JvmStatic
        fun getCameraPitch(): Float {
            return WrapperFreeLook.getCameraPitch()
        }

        @JvmStatic
        val cameraPrevYaw: Float
            get() = WrapperFreeLook.getCameraPrevYaw()
        @JvmStatic
        val cameraPrevPitch: Float
            get() = WrapperFreeLook.getCameraPrevPitch()

        @JvmStatic
        fun overrideMouse(): Boolean {
            return WrapperFreeLook.overrideMouse()
        }
    }
}
