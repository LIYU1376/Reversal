package net.minecraft.client.multiplayer;

import cn.stars.reversal.module.impl.render.TrueSights;
import cn.stars.reversal.module.impl.world.TimeTraveller;
import cn.stars.reversal.util.misc.ModuleInstance;
import com.google.common.collect.Sets;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.MovingSoundMinecart;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.particle.EntityFirework;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.profiler.Profiler;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.src.Config;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.*;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.storage.SaveDataMemoryStorage;
import net.minecraft.world.storage.SaveHandlerMP;
import net.minecraft.world.storage.WorldInfo;
import net.optifine.CustomGuis;
import net.optifine.DynamicLights;
import net.optifine.override.PlayerControllerOF;

import java.util.Random;
import java.util.Set;

public class WorldClient extends World
{
    private NetHandlerPlayClient sendQueue;
    private ChunkProviderClient clientChunkProvider;
    private final Set<Entity> entityList = Sets.<Entity>newHashSet();
    private final Set<Entity> entitySpawnQueue = Sets.<Entity>newHashSet();
    private final Minecraft mc = Minecraft.getMinecraft();
    private final Set<ChunkCoordIntPair> previousActiveChunkSet = Sets.<ChunkCoordIntPair>newHashSet();
    private boolean playerUpdate = false;

    public WorldClient(NetHandlerPlayClient netHandler, WorldSettings settings, int dimension, EnumDifficulty difficulty, Profiler profilerIn)
    {
        super(new SaveHandlerMP(), new WorldInfo(settings, "MpServer"), WorldProvider.getProviderForDimension(dimension), profilerIn, true);
        this.sendQueue = netHandler;
        this.getWorldInfo().setDifficulty(difficulty);
        this.provider.registerWorld(this);
        this.setSpawnPoint(new BlockPos(8, 64, 8));
        this.chunkProvider = this.createChunkProvider();
        this.mapStorage = new SaveDataMemoryStorage();
        this.calculateInitialSkylight();
        this.calculateInitialWeather();

        if (this.mc.playerController != null && this.mc.playerController.getClass() == PlayerControllerMP.class)
        {
            this.mc.playerController = new PlayerControllerOF(this.mc, netHandler);
            CustomGuis.setPlayerControllerOF((PlayerControllerOF)this.mc.playerController);
        }
    }

    public void tick()
    {
        super.tick();
        this.setTotalWorldTime(this.getTotalWorldTime() + 1L);

        if (this.getGameRules().getBoolean("doDaylightCycle"))
        {
            this.setWorldTime(this.getWorldTime() + 1L);
        }

        this.theProfiler.startSection("reEntryProcessing");

        for (int i = 0; i < 10 && !this.entitySpawnQueue.isEmpty(); ++i)
        {
            Entity entity = (Entity)this.entitySpawnQueue.iterator().next();
            this.entitySpawnQueue.remove(entity);

            if (!this.loadedEntityList.contains(entity))
            {
                this.spawnEntityInWorld(entity);
            }
        }

        this.theProfiler.endStartSection("chunkCache");
        this.clientChunkProvider.unloadQueuedChunks();
        this.theProfiler.endStartSection("blocks");
        this.updateBlocks();
        this.theProfiler.endSection();
    }

    public void invalidateBlockReceiveRegion(int x1, int y1, int z1, int x2, int y2, int z2)
    {
    }

    protected IChunkProvider createChunkProvider()
    {
        this.clientChunkProvider = new ChunkProviderClient(this);
        return this.clientChunkProvider;
    }

    protected void updateBlocks()
    {
        super.updateBlocks();
        this.previousActiveChunkSet.retainAll(this.activeChunkSet);

        if (this.previousActiveChunkSet.size() == this.activeChunkSet.size())
        {
            this.previousActiveChunkSet.clear();
        }

        int i = 0;

        for (ChunkCoordIntPair chunkcoordintpair : this.activeChunkSet)
        {
            if (!this.previousActiveChunkSet.contains(chunkcoordintpair))
            {
                int j = chunkcoordintpair.chunkXPos * 16;
                int k = chunkcoordintpair.chunkZPos * 16;
                this.theProfiler.startSection("getChunk");
                Chunk chunk = this.getChunkFromChunkCoords(chunkcoordintpair.chunkXPos, chunkcoordintpair.chunkZPos);
                this.playMoodSoundAndCheckLight(j, k, chunk);
                this.theProfiler.endSection();
                this.previousActiveChunkSet.add(chunkcoordintpair);
                ++i;

                if (i >= 10)
                {
                    return;
                }
            }
        }
    }

    public void doPreChunk(int chuncX, int chuncZ, boolean loadChunk)
    {
        if (loadChunk)
        {
            this.clientChunkProvider.loadChunk(chuncX, chuncZ);
        }
        else
        {
            this.clientChunkProvider.unloadChunk(chuncX, chuncZ);
        }

        if (!loadChunk)
        {
            this.markBlockRangeForRenderUpdate(chuncX * 16, 0, chuncZ * 16, chuncX * 16 + 15, 256, chuncZ * 16 + 15);
        }
    }

    public boolean spawnEntityInWorld(Entity entityIn)
    {
        boolean flag = super.spawnEntityInWorld(entityIn);
        this.entityList.add(entityIn);

        if (!flag)
        {
            this.entitySpawnQueue.add(entityIn);
        }
        else if (entityIn instanceof EntityMinecart)
        {
            this.mc.getSoundHandler().playSound(new MovingSoundMinecart((EntityMinecart)entityIn));
        }

        return flag;
    }

    public void removeEntity(Entity entityIn)
    {
        super.removeEntity(entityIn);
        this.entityList.remove(entityIn);
    }

    protected void onEntityAdded(Entity entityIn)
    {
        super.onEntityAdded(entityIn);

        if (this.entitySpawnQueue.contains(entityIn))
        {
            this.entitySpawnQueue.remove(entityIn);
        }
    }

    protected void onEntityRemoved(Entity entityIn)
    {
        super.onEntityRemoved(entityIn);
        boolean flag = false;

        if (this.entityList.contains(entityIn))
        {
            if (entityIn.isEntityAlive())
            {
                this.entitySpawnQueue.add(entityIn);
                flag = true;
            }
            else
            {
                this.entityList.remove(entityIn);
            }
        }
    }

    public void addEntityToWorld(int entityID, Entity entityToSpawn)
    {
        Entity entity = this.getEntityByID(entityID);

        if (entity != null)
        {
            this.removeEntity(entity);
        }

        this.entityList.add(entityToSpawn);
        entityToSpawn.setEntityId(entityID);

        if (!this.spawnEntityInWorld(entityToSpawn))
        {
            this.entitySpawnQueue.add(entityToSpawn);
        }

        this.entitiesById.addKey(entityID, entityToSpawn);
    }

    public Entity getEntityByID(int id)
    {
        return (Entity)(id == this.mc.thePlayer.getEntityId() ? this.mc.thePlayer : super.getEntityByID(id));
    }

    public Entity removeEntityFromWorld(int entityID)
    {
        Entity entity = (Entity)this.entitiesById.removeObject(entityID);

        if (entity != null)
        {
            this.entityList.remove(entity);
            this.removeEntity(entity);
        }

        return entity;
    }

    public boolean invalidateRegionAndSetBlock(BlockPos pos, IBlockState state)
    {
        int i = pos.getX();
        int j = pos.getY();
        int k = pos.getZ();
        this.invalidateBlockReceiveRegion(i, j, k, i, j, k);
        return super.setBlockState(pos, state, 3);
    }

    public void sendQuittingDisconnectingPacket()
    {
        this.sendQueue.getNetworkManager().closeChannel(new ChatComponentText("Quitting"));
    }

    protected void updateWeather()
    {
    }

    protected int getRenderDistanceChunks()
    {
        return this.mc.gameSettings.renderDistanceChunks;
    }

    public void doVoidFogParticles(int posX, int posY, int posZ)
    {
        int i = 16;
        Random random = new Random();
        ItemStack itemstack = this.mc.thePlayer.getHeldItem();
        boolean flag = this.mc.playerController.getCurrentGameType() == WorldSettings.GameType.CREATIVE && itemstack != null && Block.getBlockFromItem(itemstack.getItem()) == Blocks.barrier;
        BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

        for (int j = 0; j < 1000; ++j)
        {
            int k = posX + this.rand.nextInt(i) - this.rand.nextInt(i);
            int l = posY + this.rand.nextInt(i) - this.rand.nextInt(i);
            int i1 = posZ + this.rand.nextInt(i) - this.rand.nextInt(i);
            blockpos$mutableblockpos.set(k, l, i1);
            IBlockState iblockstate = this.getBlockState(blockpos$mutableblockpos);
            iblockstate.getBlock().randomDisplayTick(this, blockpos$mutableblockpos, iblockstate, random);

            if ((flag || ModuleInstance.getModule(TrueSights.class).isEnabled()) && iblockstate.getBlock() == Blocks.barrier)
            {
                this.spawnParticle(EnumParticleTypes.BARRIER, (double)((float)k + 0.5F), (double)((float)l + 0.5F), (double)((float)i1 + 0.5F), 0.0D, 0.0D, 0.0D, new int[0]);
            }
        }
    }

    public void removeAllEntities()
    {
        this.loadedEntityList.removeAll(this.unloadedEntityList);

        for (Entity value : this.unloadedEntityList) {
            Entity entity = (Entity) value;
            int j = entity.chunkCoordX;
            int k = entity.chunkCoordZ;

            if (entity.addedToChunk && this.isChunkLoaded(j, k, true)) {
                this.getChunkFromChunkCoords(j, k).removeEntity(entity);
            }
        }

        for (Entity entity : this.unloadedEntityList) {
            this.onEntityRemoved(entity);
        }

        this.unloadedEntityList.clear();

        for (Entity entity1 : this.loadedEntityList) {

            if (entity1.ridingEntity != null) {
                if (!entity1.ridingEntity.isDead && entity1.ridingEntity.riddenByEntity == entity1) {
                    continue;
                }

                entity1.ridingEntity.riddenByEntity = null;
                entity1.ridingEntity = null;
            }

            if (entity1.isDead) {
                final int j1 = entity1.chunkCoordX;
                final int k1 = entity1.chunkCoordZ;

                if (entity1.addedToChunk && this.isChunkLoaded(j1, k1, true)) {
                    this.getChunkFromChunkCoords(j1, k1).removeEntity(entity1);
                }

                this.loadedEntityList.remove(entity1);
                this.onEntityRemoved(entity1);
            }
        }
    }

    public CrashReportCategory addWorldInfoToCrashReport(CrashReport report)
    {
        CrashReportCategory crashreportcategory = super.addWorldInfoToCrashReport(report);
        crashreportcategory.addCrashSection("生物", "共计" + this.entityList.size() + " ; " + this.entityList);
        crashreportcategory.addCrashSection("生成中生物", "共计" + this.entitySpawnQueue.size() + " ; " + this.entitySpawnQueue);
        crashreportcategory.addCrashSection("服务器标识", this.mc.thePlayer.getClientBrand());
        crashreportcategory.addCrashSection("服务器类型", this.mc.getIntegratedServer() == null ? "多人游戏服务器" : "单人游戏服务器");
        return crashreportcategory;
    }

    public void playSoundAtPos(BlockPos pos, String soundName, float volume, float pitch, boolean distanceDelay)
    {
        this.playSound((double)pos.getX() + 0.5D, (double)pos.getY() + 0.5D, (double)pos.getZ() + 0.5D, soundName, volume, pitch, distanceDelay);
    }

    public void playSound(final double x, final double y, final double z, final String soundName, final float volume, final float pitch, final boolean distanceDelay) {
        final double d0 = this.mc.getRenderViewEntity().getDistanceSq(x, y, z);
        final PositionedSoundRecord positionedsoundrecord = new PositionedSoundRecord(new ResourceLocation(soundName), volume, pitch, (float) x, (float) y, (float) z);

        if (distanceDelay && d0 > 100.0D) {
            final double d1 = Math.sqrt(d0) / 40.0D;
            this.mc.getSoundHandler().playDelayedSound(positionedsoundrecord, (int) (d1 * 20.0D));
        } else {
            this.mc.getSoundHandler().playSound(positionedsoundrecord);
        }
    }

    public void makeFireworks(double x, double y, double z, double motionX, double motionY, double motionZ, NBTTagCompound compund)
    {
        this.mc.effectRenderer.addEffect(new EntityFirework.StarterFX(this, x, y, z, motionX, motionY, motionZ, this.mc.effectRenderer, compund));
    }

    public void setWorldScoreboard(Scoreboard scoreboardIn)
    {
        this.worldScoreboard = scoreboardIn;
    }

    /**
     * Sets the world time.
     */
    public void setWorldTime(long time)
    {
        if (ModuleInstance.getModule(TimeTraveller.class).isEnabled()) return;
        if (time < 0L)
        {
            time = -time;
            this.getGameRules().setOrCreateGameRule("doDaylightCycle", "false");
        }
        else
        {
            this.getGameRules().setOrCreateGameRule("doDaylightCycle", "true");
        }

        super.setWorldTime(time);
    }

    public void setWorldTime2(long time)
    {
        if (time < 0L)
        {
            time = -time;
            this.getGameRules().setOrCreateGameRule("doDaylightCycle", "false");
        }
        else
        {
            this.getGameRules().setOrCreateGameRule("doDaylightCycle", "true");
        }

        super.setWorldTime(time);
    }

    public int getCombinedLight(BlockPos pos, int lightValue)
    {
        int i = super.getCombinedLight(pos, lightValue);

        if (Config.isDynamicLights())
        {
            i = DynamicLights.getCombinedLight(pos, i);
        }

        return i;
    }

    public boolean setBlockState(BlockPos pos, IBlockState newState, int flags)
    {
        this.playerUpdate = this.isPlayerActing();
        boolean flag = super.setBlockState(pos, newState, flags);
        this.playerUpdate = false;
        return flag;
    }

    private boolean isPlayerActing()
    {
        if (this.mc.playerController instanceof PlayerControllerOF)
        {
            PlayerControllerOF playercontrollerof = (PlayerControllerOF)this.mc.playerController;
            return playercontrollerof.isActing();
        }
        else
        {
            return false;
        }
    }

    public boolean isPlayerUpdate()
    {
        return this.playerUpdate;
    }
}
