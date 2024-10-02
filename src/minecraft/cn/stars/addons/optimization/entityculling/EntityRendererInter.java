package cn.stars.addons.optimization.entityculling;

import net.minecraft.entity.Entity;

public interface EntityRendererInter<T extends Entity> {

    boolean shadowShouldShowName(T entity);

    void shadowRenderNameTag(T entity, double p_renderName_2_, double offsetX, double offsetY);

}