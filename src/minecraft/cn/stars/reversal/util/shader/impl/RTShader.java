package cn.stars.reversal.util.shader.impl;

import cn.stars.reversal.util.shader.base.RiseShaderProgram;
import cn.stars.reversal.util.shader.base.ShaderUniforms;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;

import java.awt.*;

public class RTShader {

    private final RiseShaderProgram program = new RiseShaderProgram("lrq.frag", "vertex.vsh");

    /**
     * Draws a rounded rectangle at the given coordinates with the given lengths
     *
     * @param x      The top left x coordinate of the rectangle
     * @param y      The top y coordinate of the rectangle
     * @param width  The width which is used to determine the second x rectangle
     * @param height The height which is used to determine the second y rectangle
     * @param radius The radius for the corners of the rectangles (>0)
     * @param color  The color used to draw the rectangle
     */
    public void draw(final float x, final float y, final float width, final float height, float offset, final float radius, final Color color) {
        final int programId = this.program.getProgramId();
        this.program.start();
        ShaderUniforms.uniform2f(programId, "u_size", width, height);
        ShaderUniforms.uniform1f(programId, "u_radius", radius);
        ShaderUniforms.uniform4f(programId, "u_color", color.getRed() / 255.0F, color.getGreen() / 255.0F, color.getBlue() / 255.0F, color.getAlpha() / 255.0F);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.0F);
        RiseShaderProgram.drawRightTrapezoid(x - 1, y - 1, offset, width + 2, height + 2);
        GlStateManager.disableBlend();
        RiseShaderProgram.stop();
    }

    /**
     * Draws a rounded rectangle at the given coordinates with the given lengths
     *
     * @param x      The top left x coordinate of the rectangle
     * @param y      The top y coordinate of the rectangle
     * @param width  The width which is used to determine the second x rectangle
     * @param height The height which is used to determine the second y rectangle
     * @param radius The radius for the corners of the rectangles (>0)
     * @param color  The color used to draw the rectangle
     */
    public void draw(final double x, final double y, final double width, final double height, final double offset, final double radius, final Color color) {
        draw((float) x, (float) y, (float) width, (float) height, (float) offset, (float) radius, color);
    }
}
