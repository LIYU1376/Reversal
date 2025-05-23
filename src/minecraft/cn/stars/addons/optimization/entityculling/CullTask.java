package cn.stars.addons.optimization.entityculling;

import cn.stars.addons.culling.OcclusionCullingInstance;
import cn.stars.addons.culling.util.Vec3d;
import cn.stars.reversal.util.ReversalLogger;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3;

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Set;

public class CullTask implements Runnable {

    public boolean requestCull = false;

    private final OcclusionCullingInstance culling;
    private final Minecraft client = Minecraft.getMinecraft();
    private final int sleepDelay = 10;
    private final int hitboxLimit = 50;
    private final Set<String> unCullable;
    public long lastTime = 0;

    // reused preallocated vars
    private Vec3d lastPos = new Vec3d(0, 0, 0);
    private Vec3d aabbMin = new Vec3d(0, 0, 0);
    private Vec3d aabbMax = new Vec3d(0, 0, 0);

    public CullTask(OcclusionCullingInstance culling, Set<String> unCullable) {
        this.culling = culling;
        this.unCullable = unCullable;
    }

    @Override
    public void run() {
        ReversalLogger.info("[*] Culling task is running!");
        while (client != null) { // not correct, but the running field is hidden
            try {
                Thread.sleep(sleepDelay);

                if (client.theWorld != null && client.thePlayer != null && client.thePlayer.ticksExisted > 10 && client.getRenderViewEntity() != null) {
                    Vec3 cameraMC = getCameraPos();
                    if (requestCull || !(cameraMC.xCoord == lastPos.x && cameraMC.yCoord == lastPos.y && cameraMC.zCoord == lastPos.z)) {
                        long start = System.currentTimeMillis();
                        requestCull = false;
                        lastPos.set(cameraMC.xCoord, cameraMC.yCoord, cameraMC.zCoord);
                        Vec3d camera = lastPos;
                        culling.resetCache();
                        boolean noCulling = client.thePlayer.isSpectator() || client.gameSettings.thirdPersonView != 0;
                        Iterator<TileEntity> iterator = client.theWorld.loadedTileEntityList.iterator();
                        TileEntity entry;
                        while(iterator.hasNext()) {
                            try {
                                entry = iterator.next();
                            }catch(NullPointerException | ConcurrentModificationException ex) {
                                break; // We are not synced to the main thread, so NPE's/CME are allowed here and way less
                                // overhead probably than trying to sync stuff up for no really good reason
                            }
                            if(unCullable.contains(entry.getBlockType().getUnlocalizedName())) {
                                continue;
                            }
                            Cullable cullable = (Cullable) entry;
                            if (!cullable.isForcedVisible()) {
                                if (noCulling) {
                                    cullable.setCulled(false);
                                    continue;
                                }
                                BlockPos pos = entry.getPos();
                                if(pos.distanceSq(cameraMC.xCoord, cameraMC.yCoord, cameraMC.zCoord) < 64*64) { // 64 is the fixed max tile view distance
                                    aabbMin.set(pos.getX(), pos.getY(), pos.getZ());
                                    aabbMax.set(pos.getX()+1d, pos.getY()+1d, pos.getZ()+1d);
                                    boolean visible = culling.isAABBVisible(aabbMin, aabbMax, camera);
                                    cullable.setCulled(!visible);
                                }

                            }
                        }
                        Entity entity = null;
                        Iterator<Entity> iterable = client.theWorld.getLoadedEntityList().iterator();
                        while (iterable.hasNext()) {
                            try {
                                entity = iterable.next();
                            } catch (NullPointerException | ConcurrentModificationException ex) {
                                break; // We are not synced to the main thread, so NPE's/CME are allowed here and way less
                                // overhead probably than trying to sync stuff up for no really good reason
                            }
                            if(entity == null || !(entity instanceof Cullable)) {
                                continue; // Not sure how this could happen outside from mixin screwing up the inject into Entity
                            }
                            Cullable cullable = (Cullable) entity;
                            if (!cullable.isForcedVisible()) {
                                if (noCulling || isSkippableArmorstand(entity)) {
                                    cullable.setCulled(false);
                                    continue;
                                }
                                if(entity.getPositionVector().squareDistanceTo(cameraMC) > 128 * 128) {
                                    cullable.setCulled(false); // If your entity view distance is larger than tracingDistance just render it
                                    continue;
                                }
                                AxisAlignedBB boundingBox = entity.getEntityBoundingBox();
                                if(boundingBox.maxX - boundingBox.minX > hitboxLimit || boundingBox.maxY - boundingBox.minY > hitboxLimit || boundingBox.maxZ - boundingBox.minZ > hitboxLimit) {
                                    cullable.setCulled(false); // To big to bother to cull
                                    continue;
                                }
                                aabbMin.set(boundingBox.minX, boundingBox.minY, boundingBox.minZ);
                                aabbMax.set(boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ);
                                boolean visible = culling.isAABBVisible(aabbMin, aabbMax, camera);
                                cullable.setCulled(!visible);
                            }
                        }
                        lastTime = (System.currentTimeMillis()-start);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        ReversalLogger.info("[*] Shutting down culling task!");
    }

    // 1.8 doesnt know where the heck the camera is... what?!?
    private Vec3 getCameraPos() {
        if (client.gameSettings.thirdPersonView == 0) {
            return client.getRenderViewEntity().getPositionEyes(0);
        }
        return client.getRenderViewEntity().getPositionEyes(0);
        // doesnt work correctly
//        Entity entity = client.getRenderViewEntity();
//        float f = entity.getEyeHeight();
//        double d0 = entity.posX;
//        double d1 = entity.posY + f;
//        double d2 = entity.posZ;
//        double d3 = 4.0F;
//        float f1 = entity.rotationYaw;
//        float f2 = entity.rotationPitch;
//        if (client.gameSettings.thirdPersonView == 2)
//            f2 += 180.0F;
//        double d4 = (-MathHelper.sin(f1 / 180.0F * 3.1415927F) * MathHelper.cos(f2 / 180.0F * 3.1415927F)) * d3;
//        double d5 = (MathHelper.cos(f1 / 180.0F * 3.1415927F) * MathHelper.cos(f2 / 180.0F * 3.1415927F)) * d3;
//        double d6 = -MathHelper.sin(f2 / 180.0F * 3.1415927F) * d3;
//        for (int i = 0; i < 8; i++) {
//            float f3 = ((i & 0x1) * 2 - 1);
//            float f4 = ((i >> 1 & 0x1) * 2 - 1);
//            float f5 = ((i >> 2 & 0x1) * 2 - 1);
//            f3 *= 0.1F;
//            f4 *= 0.1F;
//            f5 *= 0.1F;
//            MovingObjectPosition movingobjectposition = client.theWorld.rayTraceBlocks(
//                    new Vec3(d0 + f3, d1 + f4, d2 + f5),
//                    new Vec3(d0 - d4 + f3 + f5, d1 - d6 + f4, d2 - d5 + f5));
//            if (movingobjectposition != null) {
//                double d7 = movingobjectposition.hitVec.distanceTo(new Vec3(d0, d1, d2));
//                if (d7 < d3)
//                    d3 = d7;
//            }
//        }
//        float pitchRadian = f2 * (3.1415927F / 180); // X rotation
//        float yawRadian   = f1   * (3.1415927F / 180); // Y rotation
//        double newPosX = d0 - d3 *  MathHelper.sin( yawRadian ) * MathHelper.cos( pitchRadian );
//        double newPosY = d1 - d3 * -MathHelper.sin( pitchRadian );
//        double newPosZ = d2 - d3 *  MathHelper.cos( yawRadian ) * MathHelper.cos( pitchRadian );
//        Vec3 vec = new Vec3(newPosX, newPosY, newPosZ);
//        System.out.println(newPosX + " " + newPosY + " " + newPosZ);
//        return vec;
    }

    private boolean isSkippableArmorstand(Entity entity) {
        return entity instanceof EntityArmorStand && ((EntityArmorStand) entity).hasMarker();
    }
}