package net.optifine.entity.model;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.model.ModelSkeletonHead;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.tileentity.RenderWitherSkull;
import net.minecraft.entity.projectile.EntityWitherSkull;

public class ModelAdapterWitherSkull extends ModelAdapter
{
    public ModelAdapterWitherSkull()
    {
        super(EntityWitherSkull.class, "wither_skull", 0.0F);
    }

    public ModelBase makeModel()
    {
        return new ModelSkeletonHead();
    }

    public ModelRenderer getModelRenderer(ModelBase model, String modelPart)
    {
        if (!(model instanceof ModelSkeletonHead))
        {
            return null;
        }
        else
        {
            ModelSkeletonHead modelskeletonhead = (ModelSkeletonHead)model;
            return modelPart.equals("head") ? modelskeletonhead.skeletonHead : null;
        }
    }

    public String[] getModelRendererNames()
    {
        return new String[] {"head"};
    }

    public IEntityRenderer makeEntityRender(ModelBase modelBase, float shadowSize) {
        RenderManager rendermanager = Minecraft.getMinecraft().getRenderManager();
        RenderWitherSkull renderwitherskull = new RenderWitherSkull(rendermanager);

        renderwitherskull.skeletonHeadModel = (ModelSkeletonHead) modelBase;
        renderwitherskull.shadowSize = shadowSize;
        return renderwitherskull;
    }
}
