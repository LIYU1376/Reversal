package cn.stars.reversal.module.impl.hud;

import cn.stars.reversal.event.impl.Render2DEvent;
import cn.stars.reversal.font.FontManager;
import cn.stars.reversal.module.Category;
import cn.stars.reversal.module.Module;
import cn.stars.reversal.module.ModuleInfo;
import cn.stars.reversal.util.render.RenderUtil;
import cn.stars.reversal.value.impl.BoolValue;
import cn.stars.reversal.value.impl.ColorValue;
import cn.stars.reversal.value.impl.NoteValue;
import net.minecraft.client.resources.I18n;
import net.optifine.Lang;

import java.awt.*;

@ModuleInfo(name = "TestElement", localizedName = "module.TestElement.name", description = "Only for test",
        localizedDescription = "module.TestElement.desc", category = Category.HUD)
public class TestElement extends Module {
    private final NoteValue note = new NoteValue("测试功能,请勿开启!", this);
    private final ColorValue colorValue = new ColorValue("Color", this);
    private final BoolValue bg = new BoolValue("Bg", this, false);
    public TestElement() {
        setCanBeEdited(true);
        setX(100);
        setY(100);
        setWidth(100);
        setHeight(100);
    }

    @Override
    public void onRender2D(Render2DEvent event) {
        int x = getX() + 2;
        int y = getY() + 2;
        setWidth(120);
        setHeight(50);
        if (bg.enabled) RenderUtil.rect(x - 4, y - 4, FontManager.getRegular(32).width(I18n.format("reversal.test")) + 4, FontManager.getRegular(32).height() + 4, new Color(20,20,20,100));
        MODERN_BLUR_RUNNABLES.add(() -> RenderUtil.rect(x - 4, y - 4, FontManager.getRegular(32).width(I18n.format("reversal.test")) + 4, FontManager.getRegular(32).height() + 4, Color.BLACK));

        FontManager.getRegular(32).drawString(I18n.format("reversal.test"), x, y, colorValue.getColor().getRGB());
        MODERN_POST_BLOOM_RUNNABLES.add(() -> FontManager.getRegular(32).drawString(I18n.format("reversal.test"), x, y, colorValue.getColor().getRGB()));

    //    RenderUtil.roundedRectangle(x, y + 20, 32, 32, 4, colorValue.getColor());
    }

    @Override
    protected void onEnable() {
    }
}
