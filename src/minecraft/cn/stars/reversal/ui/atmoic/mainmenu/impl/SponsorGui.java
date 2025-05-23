/*
 * Reversal Client - A PVP Client with hack visual.
 * Copyright 2025 Aerolite Society, Some rights reserved.
 */
package cn.stars.reversal.ui.atmoic.mainmenu.impl;

import cn.stars.reversal.ui.atmoic.mainmenu.AtomicGui;
import cn.stars.reversal.ui.atmoic.mainmenu.AtomicMenu;
import cn.stars.reversal.ui.atmoic.misc.component.TextButton;
import cn.stars.reversal.util.misc.ModuleInstance;
import cn.stars.reversal.util.render.RenderUtil;
import cn.stars.reversal.util.render.RoundedUtil;
import cn.stars.reversal.util.render.UIUtil;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;

public class SponsorGui extends AtomicGui {
    public GuiScreen parent;
    private TextButton exitButton;
    private TextButton[] buttons;
    private final ArrayList<String> sponsors = new ArrayList<>();
    private TextButton showSponsorButton;
    private boolean showSponsor = false;

    public SponsorGui() {
        super("Sponsor", "sponsor", "g");
    }

    @Override
    public void drawIcon(int posX, int posY, int color) {
        atomic24.drawString(icon, posX + 1, posY + 0.5, color);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {

        ModuleInstance.getPostProcessing().drawElementWithBloom(() -> {
            RoundedUtil.drawRound(50, 65, width - 100, height - 85, 3, Color.BLACK);
        }, 2, 2);

        RoundedUtil.drawRound(50, 65, width - 100, height - 85, 3, new Color(20, 20, 20, 160));

        regular18.drawString("以下是为Reversal客户端开发者提供过赞助的人员名单!", 60, 80, new Color(220,220,220,220).getRGB());
        regular18.drawString("本客户端一直免费开源，开发者没有渠道盈利，有能力的话给我们买杯奶茶~!", 60, 90, new Color(220,220,220,220).getRGB());
        regular18.drawString("非常感谢各位对我们开发的支持! =w=", 60, 100, new Color(220,220,220,220).getRGB());
        regular18.drawString("(为了回馈大家，我们为赞助者准备了一些福利，请看游戏内公告或者官方群公告!)", 60, 110, new Color(220,220,220,220).getRGB());

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        RenderUtil.scissor(50, 65, width - 100, height - 120);
        for (String string : sponsors) {
            regular20.drawString(string, 60, 130 + sponsors.indexOf(string) * 15, Color.WHITE.getRGB());
        }
        if (showSponsor) {
            RenderUtil.image(new ResourceLocation("reversal/images/sponsor.png"), width - 250, 100, 200, 200);
        }
        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        regular18.drawString("特别鸣谢: ZedWAre, Bzdhyp, Crazy1101010, ChuKai, BingSiNiao, Petey.", 60, height - 40, new Color(220,220,220,220).getRGB());
        regular18.drawString("※ 顺序不分先后； 如果你赞助过但不在列表里，请联系Stars留下你的网名!", 60, height - 30, new Color(220,220,220,220).getRGB());

        for (TextButton button : buttons) {
            button.draw(mouseX, mouseY, partialTicks);
        }
        
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public void initGui() {
        super.initGui();
        this.exitButton = new TextButton(width / 2f - 60, height - 60, 120, 35, () -> AtomicMenu.switchGui(0),
                "返回主菜单", "g", true, 12, 38, 11);
        this.showSponsor = false;
        this.showSponsorButton = new TextButton(this.width - 150, 68, 90, 20, this::toggle, "显示赞助二维码", "", true, 1, 10, 5, 20);

        buttons = new TextButton[]{exitButton, showSponsorButton};

        this.addSponsors();
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        UIUtil.onButtonClick(buttons, mouseX, mouseY, mouseButton);
    }

    private void toggle() {
        showSponsor = !showSponsor;
        if (showSponsor) this.showSponsorButton = new TextButton(this.width - 150, 68, 90, 20, this::toggle, "关闭赞助二维码", "", true, 1, 10, 5, 20);
        else this.showSponsorButton = new TextButton(this.width - 150, 68, 90, 20, this::toggle, "显示赞助二维码", "", true, 1, 10, 5, 20);
        buttons = new TextButton[]{exitButton, showSponsorButton};
    }

    // Only edit when necessary.
    private void addSponsors() {
        sponsors.clear();
        String s = "小逸 # A_su # 酚酞 # 西瓜 # Prodee163 # Valor#1337 # SmokeKing_ # INK_qwp # failure # Juice_awa # Tianlol # Petey # 雫 # Chsy # 尘埃杰瑞 # Realonker # MaceAura # zhangjh";
        String[] result = s.split(" # ");
        sponsors.addAll(Arrays.asList(result));
    }
}
