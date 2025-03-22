package net.minecraft.world.chunk;

import cn.stars.addons.phosphor.LightingEngine;
import cn.stars.addons.phosphor.LightingHooks;
import cn.stars.addons.phosphor.WorldChunkSlice;
import cn.stars.reversal.util.ReversalLogger;
import com.google.common.base.Predicate;
import com.google.common.collect.Maps;
import com.google.common.collect.Queues;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ClassInheritanceMultiMap;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ReportedException;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.biome.WorldChunkManager;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraft.world.gen.ChunkProviderDebug;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Chunk
{
    private static final Logger logger = LogManager.getLogger();
    private final ExtendedBlockStorage[] storageArrays;
    private final byte[] blockBiomeArray;
    private final int[] precipitationHeightMap;
    private final boolean[] updateSkylightColumns;
    private boolean isChunkLoaded;
    private final World worldObj;
    private final int[] heightMap;
    public final int xPosition;
    public final int zPosition;
    private boolean isGapLightingUpdated;
    private final Map<BlockPos, TileEntity> chunkTileEntityMap;
    private final ClassInheritanceMultiMap<Entity>[] entityLists;
    private boolean isTerrainPopulated;
    private boolean isLightPopulated;
    private boolean field_150815_m;
    private boolean isModified;
    public boolean hasEntities;
    private long lastSaveTime;
    private int heightMapMinimum;
    private long inhabitedTime;
    private int queuedLightChecks;
    private final ConcurrentLinkedQueue<BlockPos> tileEntityPosQueue;
    private static final EnumFacing[] HORIZONTAL = EnumFacing.Plane.HORIZONTAL.facings();
    public final LightingEngine lightingEngine;
    private boolean onlyOne;
    private short[] neighborLightChecks;
    private boolean isLightInitialized;

    public Chunk(World worldIn, int x, int z)
    {
        this.storageArrays = new ExtendedBlockStorage[16];
        this.blockBiomeArray = new byte[256];
        this.precipitationHeightMap = new int[256];
        this.updateSkylightColumns = new boolean[256];
        this.chunkTileEntityMap = Maps.<BlockPos, TileEntity>newHashMap();
        this.queuedLightChecks = 4096;
        this.tileEntityPosQueue = Queues.<BlockPos>newConcurrentLinkedQueue();
        this.entityLists = (ClassInheritanceMultiMap[])(new ClassInheritanceMultiMap[16]);
        this.worldObj = worldIn;
        this.xPosition = x;
        this.zPosition = z;
        this.heightMap = new int[256];
        this.lightingEngine = this.worldObj.lightingEngine;

        for (int i = 0; i < this.entityLists.length; ++i)
        {
            this.entityLists[i] = new ClassInheritanceMultiMap<>(Entity.class);
        }

        Arrays.fill(this.precipitationHeightMap, (int) - 999);
        Arrays.fill(this.blockBiomeArray, (byte) - 1);
    }

    public Chunk(World worldIn, ChunkPrimer primer, int x, int z)
    {
        this(worldIn, x, z);
        int i = 256;
        boolean flag = !worldIn.provider.getHasNoSky();

        for (int j = 0; j < 16; ++j)
        {
            for (int k = 0; k < 16; ++k)
            {
                for (int l = 0; l < i; ++l)
                {
                    int i1 = j * i * 16 | k * i | l;
                    IBlockState iblockstate = primer.getBlockState(i1);

                    if (iblockstate.getBlock().getMaterial() != Material.air)
                    {
                        int j1 = l >> 4;

                        if (this.storageArrays[j1] == null)
                        {
                            this.storageArrays[j1] = new ExtendedBlockStorage(j1 << 4, flag);
                        }

                        this.storageArrays[j1].set(j, l & 15, k, iblockstate);
                    }
                }
            }
        }
    }

    public boolean isAtLocation(int x, int z)
    {
        return x == this.xPosition && z == this.zPosition;
    }

    public int getHeight(BlockPos pos)
    {
        return this.getHeightValue(pos.getX() & 15, pos.getZ() & 15);
    }

    public int getHeightValue(int x, int z)
    {
        return this.heightMap[z << 4 | x];
    }

    public int getTopFilledSegment()
    {
        for (int i = this.storageArrays.length - 1; i >= 0; --i)
        {
            if (this.storageArrays[i] != null)
            {
                return this.storageArrays[i].getYLocation();
            }
        }

        return 0;
    }

    public ExtendedBlockStorage[] getBlockStorageArray()
    {
        return this.storageArrays;
    }

    protected void generateHeightMap()
    {
        int i = this.getTopFilledSegment();
        this.heightMapMinimum = Integer.MAX_VALUE;

        for (int j = 0; j < 16; ++j)
        {
            for (int k = 0; k < 16; ++k)
            {
                this.precipitationHeightMap[j + (k << 4)] = -999;

                for (int l = i + 16; l > 0; --l)
                {
                    Block block = this.getBlock0(j, l - 1, k);

                    if (block.getLightOpacity() != 0)
                    {
                        this.heightMap[k << 4 | j] = l;

                        if (l < this.heightMapMinimum)
                        {
                            this.heightMapMinimum = l;
                        }

                        break;
                    }
                }
            }
        }

        this.isModified = true;
    }

    public void generateSkylightMap()
    {
        int i = this.getTopFilledSegment();
        this.heightMapMinimum = Integer.MAX_VALUE;

        for (int j = 0; j < 16; ++j)
        {
            for (int k = 0; k < 16; ++k)
            {
                this.precipitationHeightMap[j + (k << 4)] = -999;

                for (int l = i + 16; l > 0; --l)
                {
                    if (this.getBlockLightOpacity(j, l - 1, k) != 0)
                    {
                        this.heightMap[k << 4 | j] = l;

                        if (l < this.heightMapMinimum)
                        {
                            this.heightMapMinimum = l;
                        }

                        break;
                    }
                }

                if (!this.worldObj.provider.getHasNoSky())
                {
                    int k1 = 15;
                    int i1 = i + 16 - 1;

                    while (true)
                    {
                        int j1 = this.getBlockLightOpacity(j, i1, k);

                        if (j1 == 0 && k1 != 15)
                        {
                            j1 = 1;
                        }

                        k1 -= j1;

                        if (k1 > 0)
                        {
                            ExtendedBlockStorage extendedblockstorage = this.storageArrays[i1 >> 4];

                            if (extendedblockstorage != null)
                            {
                                extendedblockstorage.setExtSkylightValue(j, i1 & 15, k, k1);
                                this.worldObj.notifyLightSet(new BlockPos((this.xPosition << 4) + j, i1, (this.zPosition << 4) + k));
                            }
                        }

                        --i1;

                        if (i1 <= 0 || k1 <= 0)
                        {
                            break;
                        }
                    }
                }
            }
        }

        this.isModified = true;
    }

    private void propagateSkylightOcclusion(int x, int z)
    {
        this.updateSkylightColumns[x + z * 16] = true;
        this.isGapLightingUpdated = true;
    }

    private void recheckGaps(boolean p_150803_1_)
    {
        this.worldObj.theProfiler.startSection("recheckGaps");

        WorldChunkSlice slice = new WorldChunkSlice(this.worldObj, this.xPosition, this.zPosition);

        if (this.worldObj.isAreaLoaded(new BlockPos(this.xPosition * 16 + 8, 0, this.zPosition * 16 + 8), 16)) {
            for (int x = 0; x < 16; ++x) {
                for (int z = 0; z < 16; ++z) {
                    if (this.recheckGapsForColumn(slice, x, z)) {
                        if (onlyOne) {
                            this.worldObj.theProfiler.endSection();

                            return;
                        }
                    }
                }
            }

            this.isGapLightingUpdated = false;
        }

        this.worldObj.theProfiler.endSection();
    }

    private void checkSkylightNeighborHeight(int x, int z, int maxValue)
    {
        int i = this.worldObj.getHeight(new BlockPos(x, 0, z)).getY();

        if (i > maxValue)
        {
            this.updateSkylightNeighborHeight(x, z, maxValue, i + 1);
        }
        else if (i < maxValue)
        {
            this.updateSkylightNeighborHeight(x, z, i, maxValue + 1);
        }
    }

    private void updateSkylightNeighborHeight(int x, int z, int startY, int endY)
    {
        if (endY > startY && this.worldObj.isAreaLoaded(new BlockPos(x, 0, z), 16))
        {
            for (int i = startY; i < endY; ++i)
            {
                this.worldObj.checkLightFor(EnumSkyBlock.SKY, new BlockPos(x, i, z));
            }

            this.isModified = true;
        }
    }

    private void relightBlock(int x, int y, int z)
    {
        int i = this.heightMap[z << 4 | x] & 255;

        int j = Math.max(y, i);

        while (j > 0 && this.getBlockLightOpacity(x, j - 1, z) == 0) {
            --j;
        }

        if (j != i) {
            this.heightMap[z << 4 | x] = j;

            if (!this.worldObj.provider.getHasNoSky()) {
                LightingHooks.relightSkylightColumn(this.worldObj, (Chunk) (Object) this, x, z, i, j);
            }

            int l1 = this.heightMap[z << 4 | x];

            if (l1 < this.heightMapMinimum) {
                this.heightMapMinimum = l1;
            }
        }
    }

    public int getBlockLightOpacity(BlockPos pos)
    {
        return this.getBlock(pos).getLightOpacity();
    }

    public int getBlockLightOpacity(int x, int y, int z) {
        IBlockState state = this.getBlockState(new BlockPos(x, y, z));
        IBlockState state1 = this.getBlockState(new BlockPos(this.xPosition << 4 | x & 15, y, this.zPosition << 4 | z & 15));
        return !this.isChunkLoaded ? state.getBlock().getLightOpacity() : state1.getBlock().getLightOpacity();
    }

    private Block getBlock0(int x, int y, int z)
    {
        Block block = Blocks.air;

        if (y >= 0 && y >> 4 < this.storageArrays.length)
        {
            ExtendedBlockStorage extendedblockstorage = this.storageArrays[y >> 4];

            if (extendedblockstorage != null)
            {
                try
                {
                    block = extendedblockstorage.getBlockByExtId(x, y & 15, z);
                }
                catch (Throwable throwable)
                {
                    CrashReport crashreport = CrashReport.makeCrashReport(throwable, "Getting block");
                    throw new ReportedException(crashreport);
                }
            }
        }

        return block;
    }

    public Block getBlock(final int x, final int y, final int z)
    {
        try
        {
            return this.getBlock0(x & 15, y, z & 15);
        }
        catch (ReportedException reportedexception)
        {
            CrashReportCategory crashreportcategory = reportedexception.getCrashReport().makeCategory("Block being got");
            crashreportcategory.addCrashSectionCallable("Location", new Callable<String>()
            {
                public String call() throws Exception
                {
                    return CrashReportCategory.getCoordinateInfo(new BlockPos(Chunk.this.xPosition * 16 + x, y, Chunk.this.zPosition * 16 + z));
                }
            });
            throw reportedexception;
        }
    }

    public Block getBlock(final BlockPos pos)
    {
        try
        {
            return this.getBlock0(pos.getX() & 15, pos.getY(), pos.getZ() & 15);
        }
        catch (ReportedException reportedexception)
        {
            CrashReportCategory crashreportcategory = reportedexception.getCrashReport().makeCategory("Block being got");
            crashreportcategory.addCrashSectionCallable("Location", new Callable<String>()
            {
                public String call() throws Exception
                {
                    return CrashReportCategory.getCoordinateInfo(pos);
                }
            });
            throw reportedexception;
        }
    }

    public IBlockState getBlockState(BlockPos pos) {
        final int y = pos.getY();
        if (y >= 0 && y >> 4 < this.entityLists.length) {
            final ExtendedBlockStorage storage = this.getBlockStorageArray()[y >> 4];
            if (storage != null) return storage.get(pos.getX() & 15, y & 15, pos.getZ() & 15);
        }
        return Blocks.air.getDefaultState();
    }

    private int getBlockMetadata(int x, int y, int z)
    {
        if (y >> 4 >= this.storageArrays.length)
        {
            return 0;
        }
        else
        {
            ExtendedBlockStorage extendedblockstorage = this.storageArrays[y >> 4];
            return extendedblockstorage != null ? extendedblockstorage.getExtBlockMetadata(x, y & 15, z) : 0;
        }
    }

    public int getBlockMetadata(BlockPos pos)
    {
        return this.getBlockMetadata(pos.getX() & 15, pos.getY(), pos.getZ() & 15);
    }

    public IBlockState setBlockState(BlockPos pos, IBlockState state)
    {
        int i = pos.getX() & 15;
        int j = pos.getY();
        int k = pos.getZ() & 15;
        int l = k << 4 | i;

        if (j >= this.precipitationHeightMap[l] - 1)
        {
            this.precipitationHeightMap[l] = -999;
        }

        int i1 = this.heightMap[l];
        IBlockState iblockstate = this.getBlockState(pos);

        if (iblockstate == state)
        {
            return null;
        }
        else
        {
            Block block = state.getBlock();
            Block block1 = iblockstate.getBlock();
            ExtendedBlockStorage extendedblockstorage = this.storageArrays[j >> 4];

            if (extendedblockstorage == null)
            {
                if (block == Blocks.air)
                {
                    return null;
                }

                extendedblockstorage = this.storageArrays[j >> 4] = this.initSection(j >> 4 << 4, !this.worldObj.provider.getHasNoSky());
            }

            extendedblockstorage.set(i, j & 15, k, state);

            if (block1 != block)
            {
                if (!this.worldObj.isRemote)
                {
                    block1.breakBlock(this.worldObj, pos, iblockstate);
                }
                else if (block1 instanceof ITileEntityProvider)
                {
                    this.worldObj.removeTileEntity(pos);
                }
            }

            if (extendedblockstorage.getBlockByExtId(i, j & 15, k) != block)
            {
                return null;
            }
            else
            {
            /*    if (flag)
                {
                    this.generateSkylightMap();
                }
                else */
                // Something removed by phosphor
                {
                    int j1 = block.getLightOpacity();
                    int k1 = block1.getLightOpacity();

                    if (j1 > 0)
                    {
                        if (j >= i1)
                        {
                            this.relightBlock(i, j + 1, k);
                        }
                    }
                    else if (j == i1 - 1)
                    {
                        this.relightBlock(i, j, k);
                    }

                    /*
                    if (j1 != k1 && (j1 < k1 || this.getLightFor(EnumSkyBlock.SKY, pos) > 0 || this.getLightFor(EnumSkyBlock.BLOCK, pos) > 0))
                    {
                        this.propagateSkylightOcclusion(i, k);
                    }
                     */
                }

                if (block1 instanceof ITileEntityProvider)
                {
                    TileEntity tileentity = this.getTileEntity(pos, Chunk.EnumCreateEntityType.CHECK);

                    if (tileentity != null)
                    {
                        tileentity.updateContainingBlockInfo();
                    }
                }

                if (!this.worldObj.isRemote && block1 != block)
                {
                    block.onBlockAdded(this.worldObj, pos, state);
                }

                if (block instanceof ITileEntityProvider)
                {
                    TileEntity tileentity1 = this.getTileEntity(pos, Chunk.EnumCreateEntityType.CHECK);

                    if (tileentity1 == null)
                    {
                        tileentity1 = ((ITileEntityProvider)block).createNewTileEntity(this.worldObj, block.getMetaFromState(state));
                        this.worldObj.setTileEntity(pos, tileentity1);
                    }

                    if (tileentity1 != null)
                    {
                        tileentity1.updateContainingBlockInfo();
                    }
                }

                this.isModified = true;
                return iblockstate;
            }
        }
    }

    public int getLightFor(EnumSkyBlock p_177413_1_, BlockPos pos)
    {
        lightingEngine.processLightUpdatesForType(p_177413_1_);

        return this.getCachedLightFor(p_177413_1_, pos);
    }

    public void setLightFor(EnumSkyBlock p_177431_1_, BlockPos pos, int value)
    {
        int i = pos.getX() & 15;
        int j = pos.getY();
        int k = pos.getZ() & 15;
        ExtendedBlockStorage extendedblockstorage = this.storageArrays[j >> 4];

        if (extendedblockstorage == null)
        {
            extendedblockstorage = this.storageArrays[j >> 4] = new ExtendedBlockStorage(j >> 4 << 4, !this.worldObj.provider.getHasNoSky());
            LightingHooks.initSkylightForSection(this.worldObj, this, this.storageArrays[pos.getY() >> 4]);
        //    this.generateSkylightMap();
        }

        this.isModified = true;

        if (p_177431_1_ == EnumSkyBlock.SKY)
        {
            if (!this.worldObj.provider.getHasNoSky())
            {
                extendedblockstorage.setExtSkylightValue(i, j & 15, k, value);
            }
        }
        else if (p_177431_1_ == EnumSkyBlock.BLOCK)
        {
            extendedblockstorage.setExtBlocklightValue(i, j & 15, k, value);
        }
    }

    public int getLightSubtracted(BlockPos pos, int amount)
    {
        lightingEngine.processLightUpdates();

        int i = pos.getX() & 15;
        int j = pos.getY();
        int k = pos.getZ() & 15;
        ExtendedBlockStorage extendedblockstorage = this.storageArrays[j >> 4];

        if (extendedblockstorage == null)
        {
            return !this.worldObj.provider.getHasNoSky() && amount < EnumSkyBlock.SKY.defaultLightValue ? EnumSkyBlock.SKY.defaultLightValue - amount : 0;
        }
        else
        {
            int l = this.worldObj.provider.getHasNoSky() ? 0 : extendedblockstorage.getExtSkylightValue(i, j & 15, k);
            l = l - amount;
            int i1 = extendedblockstorage.getExtBlocklightValue(i, j & 15, k);

            if (i1 > l)
            {
                l = i1;
            }

            return l;
        }
    }

    public void addEntity(Entity entityIn)
    {
        this.hasEntities = true;
        int i = MathHelper.floor_double(entityIn.posX / 16.0D);
        int j = MathHelper.floor_double(entityIn.posZ / 16.0D);

        if (i != this.xPosition || j != this.zPosition)
        {
            logger.warn("Wrong location! (" + i + ", " + j + ") should be (" + this.xPosition + ", " + this.zPosition + "), " + entityIn, new Object[] {entityIn});
            entityIn.setDead();
        }

        int k = MathHelper.floor_double(entityIn.posY / 16.0D);

        if (k < 0)
        {
            k = 0;
        }

        if (k >= this.entityLists.length)
        {
            k = this.entityLists.length - 1;
        }

        entityIn.addedToChunk = true;
        entityIn.chunkCoordX = this.xPosition;
        entityIn.chunkCoordY = k;
        entityIn.chunkCoordZ = this.zPosition;
        this.entityLists[k].add(entityIn);
    }

    public void removeEntity(Entity entityIn)
    {
        this.removeEntityAtIndex(entityIn, entityIn.chunkCoordY);
    }

    public void removeEntityAtIndex(Entity entityIn, int p_76608_2_)
    {
        if (p_76608_2_ < 0)
        {
            p_76608_2_ = 0;
        }

        if (p_76608_2_ >= this.entityLists.length)
        {
            p_76608_2_ = this.entityLists.length - 1;
        }

        this.entityLists[p_76608_2_].remove(entityIn);
    }

    public boolean canSeeSky(BlockPos pos)
    {
        int i = pos.getX() & 15;
        int j = pos.getY();
        int k = pos.getZ() & 15;
        return j >= this.heightMap[k << 4 | i];
    }

    private TileEntity createNewTileEntity(BlockPos pos)
    {
        Block block = this.getBlock(pos);
        return !block.hasTileEntity() ? null : ((ITileEntityProvider)block).createNewTileEntity(this.worldObj, this.getBlockMetadata(pos));
    }

    public TileEntity getTileEntity(BlockPos pos, Chunk.EnumCreateEntityType p_177424_2_)
    {
        TileEntity tileentity = (TileEntity)this.chunkTileEntityMap.get(pos);

        if (tileentity == null)
        {
            if (p_177424_2_ == Chunk.EnumCreateEntityType.IMMEDIATE)
            {
                tileentity = this.createNewTileEntity(pos);
                this.worldObj.setTileEntity(pos, tileentity);
            }
            else if (p_177424_2_ == Chunk.EnumCreateEntityType.QUEUED)
            {
                this.tileEntityPosQueue.add(pos);
            }
        }
        else if (tileentity.isInvalid())
        {
            this.chunkTileEntityMap.remove(pos);
            return null;
        }

        return tileentity;
    }

    public void addTileEntity(TileEntity tileEntityIn)
    {
        this.addTileEntity(tileEntityIn.getPos(), tileEntityIn);

        if (this.isChunkLoaded)
        {
            this.worldObj.addTileEntity(tileEntityIn);
        }
    }

    public void addTileEntity(BlockPos pos, TileEntity tileEntityIn)
    {
        tileEntityIn.setWorldObj(this.worldObj);
        tileEntityIn.setPos(pos);

        if (this.getBlock(pos) instanceof ITileEntityProvider)
        {
            if (this.chunkTileEntityMap.containsKey(pos))
            {
                ((TileEntity)this.chunkTileEntityMap.get(pos)).invalidate();
            }

            tileEntityIn.validate();
            this.chunkTileEntityMap.put(pos, tileEntityIn);
        }
    }

    public void removeTileEntity(BlockPos pos)
    {
        if (this.isChunkLoaded)
        {
            TileEntity tileentity = (TileEntity)this.chunkTileEntityMap.remove(pos);

            if (tileentity != null)
            {
                tileentity.invalidate();
            }
        }
    }

    public void onChunkLoad()
    {
        List<EntityPlayer> players = new ArrayList<>();

        for (final ClassInheritanceMultiMap<Entity> classinheritancemultimap : entityLists) {
            for (final EntityPlayer player : classinheritancemultimap.getByClass(EntityPlayer.class)) {
                players.add(player);
            }
        }

        for (final EntityPlayer player : players) {
            worldObj.updateEntityWithOptionalForce(player, false);
        }

        this.isChunkLoaded = true;
        this.worldObj.addTileEntities(this.chunkTileEntityMap.values());

        for (ClassInheritanceMultiMap<Entity> entityList : this.entityLists) {
            for (Entity entity : entityList) {
                entity.onChunkLoad();
            }

            this.worldObj.loadEntities(entityList);
        }

        LightingHooks.scheduleRelightChecksForChunkBoundaries(this.worldObj, (Chunk) this);
    }

    public void onChunkUnload()
    {
        this.isChunkLoaded = false;

        for (TileEntity tileentity : this.chunkTileEntityMap.values())
        {
            this.worldObj.markTileEntityForRemoval(tileentity);
        }

        for (int i = 0; i < this.entityLists.length; ++i)
        {
            this.worldObj.unloadEntities(this.entityLists[i]);
        }
    }

    public void setChunkModified()
    {
        this.isModified = true;
    }

    public void getEntitiesWithinAABBForEntity(Entity entityIn, AxisAlignedBB aabb, List<Entity> listToFill, Predicate <? super Entity > p_177414_4_)
    {
        int i = MathHelper.floor_double((aabb.minY - 2.0D) / 16.0D);
        int j = MathHelper.floor_double((aabb.maxY + 2.0D) / 16.0D);
        i = MathHelper.clamp_int(i, 0, this.entityLists.length - 1);
        j = MathHelper.clamp_int(j, 0, this.entityLists.length - 1);

        for (int k = i; k <= j; ++k)
        {
            if (!this.entityLists[k].isEmpty())
            {
                for (Entity entity : this.entityLists[k])
                {
                    if (entity.getEntityBoundingBox().intersectsWith(aabb) && entity != entityIn)
                    {
                        if (p_177414_4_ == null || p_177414_4_.apply(entity))
                        {
                            listToFill.add(entity);
                        }

                        Entity[] aentity = entity.getParts();

                        if (aentity != null)
                        {
                            for (int l = 0; l < aentity.length; ++l)
                            {
                                entity = aentity[l];

                                if (entity != entityIn && entity.getEntityBoundingBox().intersectsWith(aabb) && (p_177414_4_ == null || p_177414_4_.apply(entity)))
                                {
                                    listToFill.add(entity);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public <T extends Entity> void getEntitiesOfTypeWithinAAAB(Class <? extends T > entityClass, AxisAlignedBB aabb, List<T> listToFill, Predicate <? super T > p_177430_4_)
    {
        int i = MathHelper.floor_double((aabb.minY - 2.0D) / 16.0D);
        int j = MathHelper.floor_double((aabb.maxY + 2.0D) / 16.0D);
        i = MathHelper.clamp_int(i, 0, this.entityLists.length - 1);
        j = MathHelper.clamp_int(j, 0, this.entityLists.length - 1);

        for (int k = i; k <= j; ++k)
        {
            for (T t : this.entityLists[k].getByClass(entityClass))
            {
                if (t.getEntityBoundingBox().intersectsWith(aabb) && (p_177430_4_ == null || p_177430_4_.apply(t)))
                {
                    listToFill.add(t);
                }
            }
        }
    }

    public boolean needsSaving(boolean p_76601_1_)
    {
        if (p_76601_1_)
        {
            if (this.hasEntities && this.worldObj.getTotalWorldTime() != this.lastSaveTime || this.isModified)
            {
                return true;
            }
        }
        else if (this.hasEntities && this.worldObj.getTotalWorldTime() >= this.lastSaveTime + 600L)
        {
            return true;
        }

        return this.isModified;
    }

    public Random getRandomWithSeed(long seed)
    {
        return new Random(this.worldObj.getSeed() + (long)(this.xPosition * this.xPosition * 4987142) + (long)(this.xPosition * 5947611) + (long)(this.zPosition * this.zPosition) * 4392871L + (long)(this.zPosition * 389711) ^ seed);
    }

    public boolean isEmpty()
    {
        return false;
    }

    public void populateChunk(IChunkProvider p_76624_1_, IChunkProvider p_76624_2_, int x, int z)
    {
        boolean flag = p_76624_1_.chunkExists(x, z - 1);
        boolean flag1 = p_76624_1_.chunkExists(x + 1, z);
        boolean flag2 = p_76624_1_.chunkExists(x, z + 1);
        boolean flag3 = p_76624_1_.chunkExists(x - 1, z);
        boolean flag4 = p_76624_1_.chunkExists(x - 1, z - 1);
        boolean flag5 = p_76624_1_.chunkExists(x + 1, z + 1);
        boolean flag6 = p_76624_1_.chunkExists(x - 1, z + 1);
        boolean flag7 = p_76624_1_.chunkExists(x + 1, z - 1);

        if (flag1 && flag2 && flag5)
        {
            if (!this.isTerrainPopulated)
            {
                p_76624_1_.populate(p_76624_2_, x, z);
            }
            else
            {
                p_76624_1_.populateChunk(p_76624_2_, this, x, z);
            }
        }

        if (flag3 && flag2 && flag6)
        {
            Chunk chunk = p_76624_1_.provideChunk(x - 1, z);

            if (!chunk.isTerrainPopulated)
            {
                p_76624_1_.populate(p_76624_2_, x - 1, z);
            }
            else
            {
                p_76624_1_.populateChunk(p_76624_2_, chunk, x - 1, z);
            }
        }

        if (flag && flag1 && flag7)
        {
            Chunk chunk1 = p_76624_1_.provideChunk(x, z - 1);

            if (!chunk1.isTerrainPopulated)
            {
                p_76624_1_.populate(p_76624_2_, x, z - 1);
            }
            else
            {
                p_76624_1_.populateChunk(p_76624_2_, chunk1, x, z - 1);
            }
        }

        if (flag4 && flag && flag3)
        {
            Chunk chunk2 = p_76624_1_.provideChunk(x - 1, z - 1);

            if (!chunk2.isTerrainPopulated)
            {
                p_76624_1_.populate(p_76624_2_, x - 1, z - 1);
            }
            else
            {
                p_76624_1_.populateChunk(p_76624_2_, chunk2, x - 1, z - 1);
            }
        }
    }

    public BlockPos getPrecipitationHeight(BlockPos pos)
    {
        int i = pos.getX() & 15;
        int j = pos.getZ() & 15;
        int k = i | j << 4;
        BlockPos blockpos = new BlockPos(pos.getX(), this.precipitationHeightMap[k], pos.getZ());

        if (blockpos.getY() == -999)
        {
            int l = this.getTopFilledSegment() + 15;
            blockpos = new BlockPos(pos.getX(), l, pos.getZ());
            int i1 = -1;

            while (blockpos.getY() > 0 && i1 == -1)
            {
                Block block = this.getBlock(blockpos);
                Material material = block.getMaterial();

                if (!material.blocksMovement() && !material.isLiquid())
                {
                    blockpos = blockpos.down();
                }
                else
                {
                    i1 = blockpos.getY() + 1;
                }
            }

            this.precipitationHeightMap[k] = i1;
        }

        return new BlockPos(pos.getX(), this.precipitationHeightMap[k], pos.getZ());
    }

    public void func_150804_b(boolean p_150804_1_)
    {
        if (this.isGapLightingUpdated && !this.worldObj.provider.getHasNoSky() && !p_150804_1_)
        {
            this.recheckGaps(this.worldObj.isRemote);
        }

        this.field_150815_m = true;

        if (!this.isLightPopulated && this.isTerrainPopulated)
        {
            this.func_150809_p();
        }

        while (!this.tileEntityPosQueue.isEmpty())
        {
            BlockPos blockpos = this.tileEntityPosQueue.poll();

            if (this.getTileEntity(blockpos, Chunk.EnumCreateEntityType.CHECK) == null && this.getBlock(blockpos).hasTileEntity())
            {
                TileEntity tileentity = this.createNewTileEntity(blockpos);
                this.worldObj.setTileEntity(blockpos, tileentity);
                this.worldObj.markBlockRangeForRenderUpdate(blockpos, blockpos);
            }
        }
    }

    public boolean isPopulated()
    {
        return this.field_150815_m && this.isTerrainPopulated && this.isLightPopulated;
    }

    public ChunkCoordIntPair getChunkCoordIntPair()
    {
        return new ChunkCoordIntPair(this.xPosition, this.zPosition);
    }

    public boolean getAreLevelsEmpty(int startY, int endY)
    {
        if (startY < 0)
        {
            startY = 0;
        }

        if (endY >= 256)
        {
            endY = 255;
        }

        for (int i = startY; i <= endY; i += 16)
        {
            ExtendedBlockStorage extendedblockstorage = this.storageArrays[i >> 4];

            if (extendedblockstorage != null && !extendedblockstorage.isEmpty())
            {
                return false;
            }
        }

        return true;
    }

    public void setStorageArrays(ExtendedBlockStorage[] newStorageArrays)
    {
        if (this.storageArrays.length != newStorageArrays.length)
        {
            logger.warn("Could not set level chunk sections, array length is " + newStorageArrays.length + " instead of " + this.storageArrays.length);
        }
        else
        {
            for (int i = 0; i < this.storageArrays.length; ++i)
            {
                this.storageArrays[i] = newStorageArrays[i];
            }
        }
    }

    public void fillChunk(byte[] p_177439_1_, int p_177439_2_, boolean p_177439_3_)
    {
        int i = 0;
        boolean flag = !this.worldObj.provider.getHasNoSky();

        for (int j = 0; j < this.storageArrays.length; ++j)
        {
            if ((p_177439_2_ & 1 << j) != 0)
            {
                if (this.storageArrays[j] == null)
                {
                    this.storageArrays[j] = new ExtendedBlockStorage(j << 4, flag);
                }

                char[] achar = this.storageArrays[j].getData();

                for (int k = 0; k < achar.length; ++k)
                {
                    achar[k] = (char)((p_177439_1_[i + 1] & 255) << 8 | p_177439_1_[i] & 255);
                    i += 2;
                }
            }
            else if (p_177439_3_ && this.storageArrays[j] != null)
            {
                this.storageArrays[j] = null;
            }
        }

        for (int l = 0; l < this.storageArrays.length; ++l)
        {
            if ((p_177439_2_ & 1 << l) != 0 && this.storageArrays[l] != null)
            {
                NibbleArray nibblearray = this.storageArrays[l].getBlocklightArray();
                System.arraycopy(p_177439_1_, i, nibblearray.getData(), 0, nibblearray.getData().length);
                i += nibblearray.getData().length;
            }
        }

        if (flag)
        {
            for (int i1 = 0; i1 < this.storageArrays.length; ++i1)
            {
                if ((p_177439_2_ & 1 << i1) != 0 && this.storageArrays[i1] != null)
                {
                    NibbleArray nibblearray1 = this.storageArrays[i1].getSkylightArray();
                    System.arraycopy(p_177439_1_, i, nibblearray1.getData(), 0, nibblearray1.getData().length);
                    i += nibblearray1.getData().length;
                }
            }
        }

        if (p_177439_3_)
        {
            System.arraycopy(p_177439_1_, i, this.blockBiomeArray, 0, this.blockBiomeArray.length);
            int k1 = i + this.blockBiomeArray.length;
        }

        for (int j1 = 0; j1 < this.storageArrays.length; ++j1)
        {
            if (this.storageArrays[j1] != null && (p_177439_2_ & 1 << j1) != 0)
            {
                this.storageArrays[j1].removeInvalidBlocks();
            }
        }

        this.isLightPopulated = true;
        this.isTerrainPopulated = true;
        this.generateHeightMap();

        for (TileEntity tileentity : this.chunkTileEntityMap.values())
        {
            tileentity.updateContainingBlockInfo();
        }
    }

    public BiomeGenBase getBiome(BlockPos pos, WorldChunkManager chunkManager)
    {
        int i = pos.getX() & 15;
        int j = pos.getZ() & 15;
        int k = this.blockBiomeArray[j << 4 | i] & 255;

        if (k == 255)
        {
            BiomeGenBase biomegenbase = chunkManager.getBiomeGenerator(pos, BiomeGenBase.plains);
            k = biomegenbase.biomeID;
            this.blockBiomeArray[j << 4 | i] = (byte)(k & 255);
        }

        BiomeGenBase biomegenbase1 = BiomeGenBase.getBiome(k);
        return biomegenbase1 == null ? BiomeGenBase.plains : biomegenbase1;
    }

    public byte[] getBiomeArray()
    {
        return this.blockBiomeArray;
    }

    public void setBiomeArray(byte[] biomeArray)
    {
        if (this.blockBiomeArray.length != biomeArray.length)
        {
            logger.warn("Could not set level chunk biomes, array length is " + biomeArray.length + " instead of " + this.blockBiomeArray.length);
        }
        else
        {
            for (int i = 0; i < this.blockBiomeArray.length; ++i)
            {
                this.blockBiomeArray[i] = biomeArray[i];
            }
        }
    }

    public void resetRelightChecks()
    {
        this.queuedLightChecks = 0;
    }

    public void enqueueRelightChecks()
    {
        if (this.queuedLightChecks >= 4096)
        {
            return;
        }

        boolean isActiveChunk = worldObj.activeChunkSet.contains(new ChunkCoordIntPair(this.xPosition, this.zPosition));
        int lightRecheckSpeed;

        if (worldObj.isRemote && isActiveChunk) {
            lightRecheckSpeed = 256;
        } else if (worldObj.isRemote) {
            lightRecheckSpeed = 64;
        } else {
            lightRecheckSpeed = 32;
        }

        BlockPos blockpos = new BlockPos(this.xPosition << 4, 0, this.zPosition << 4);
        for (int i = 0; i < lightRecheckSpeed; ++i) {

            int j = this.queuedLightChecks % 16;
            int k = this.queuedLightChecks / 16 % 16;
            int l = this.queuedLightChecks / 256;
            ++this.queuedLightChecks;

            for (int y = 0; y < 16; ++y)
            {
                ExtendedBlockStorage storage = this.storageArrays[j];
                BlockPos blockpos1 = blockpos.add(k, (j << 4) + y, l);
                boolean flag = y == 0 || y == 15 || k == 0 || k == 15 || l == 0 || l == 15;

                if (storage == null && flag || storage != null && storage.getBlockByExtId(k, y, l).getMaterial() == Material.air)
                {
                    for (EnumFacing enumfacing : EnumFacing.values())
                    {
                        BlockPos blockpos2 = blockpos1.offset(enumfacing);

                        if (this.worldObj.getBlockState(blockpos2).getBlock().getLightValue() > 0)
                        {
                            this.worldObj.checkLight(blockpos2);
                        }
                    }

                    this.worldObj.checkLight(blockpos1);
                }
            }
        }
    }

    public void func_150809_p()
    {
        this.isTerrainPopulated = true;
        LightingHooks.checkChunkLighting(this, this.worldObj);
    }

    private void func_177441_y()
    {
        Arrays.fill(this.updateSkylightColumns, true);

        this.recheckGaps(false);
    }

    private boolean recheckGapsForColumn(WorldChunkSlice slice, int x, int z) {
        int i = x + z * 16;

        if (this.updateSkylightColumns[i]) {
            this.updateSkylightColumns[i] = false;

            int height = this.getHeightValue(x, z);

            int x1 = this.xPosition * 16 + x;
            int z1 = this.zPosition * 16 + z;

            int max = this.recheckGapsGetLowestHeight(slice, x1, z1);

            this.recheckGapsSkylightNeighborHeight(slice, x1, z1, height, max);

            return true;
        }

        return false;
    }

    private int recheckGapsGetLowestHeight(WorldChunkSlice slice, int x, int z) {
        int max = Integer.MAX_VALUE;

        for (EnumFacing facing : HORIZONTAL) {
            int j = x + facing.getFrontOffsetX();
            int k = z + facing.getFrontOffsetZ();
            Chunk chunk = slice.getChunkFromWorldCoords(j, k);
            if (chunk != null) {
                max = Math.min(max, chunk.getLowestHeight());
            }

        }

        return max;
    }

    private void recheckGapsSkylightNeighborHeight(WorldChunkSlice slice, int x, int z, int height, int max) {
        this.checkSkylightNeighborHeight(slice, x, z, max);

        for (EnumFacing facing : HORIZONTAL) {
            int j = x + facing.getFrontOffsetX();
            int k = z + facing.getFrontOffsetZ();

            this.checkSkylightNeighborHeight(slice, j, k, height);
        }
    }

    private void checkSkylightNeighborHeight(WorldChunkSlice slice, int x, int z, int maxValue) {

        Chunk chunk = slice.getChunkFromWorldCoords(x, z);
        if (chunk != null) {
            int i = chunk.getHeightValue(x & 15, z & 15);

            if (i > maxValue) {
                this.updateSkylightNeighborHeight(slice, x, z, maxValue, i + 1);
            } else if (i < maxValue) {
                this.updateSkylightNeighborHeight(slice, x, z, i, maxValue + 1);
            }

        } else {
            ReversalLogger.warn("Chunk is null! x: " + x + " z: " + z + " maxValue: " + maxValue);
        }
    }

    private void updateSkylightNeighborHeight(WorldChunkSlice slice, int x, int z, int startY, int endY) {
        if (endY > startY) {

            if (!slice.isLoaded(x, z, 16)) {
                return;
            }

            for (int i = startY; i < endY; ++i) {
                this.worldObj.checkLightFor(EnumSkyBlock.SKY, new BlockPos(x, i, z));
            }

            this.isModified = true;
        }
    }

    public short[] getNeighborLightChecks() {
        return this.neighborLightChecks;
    }

    public void setNeighborLightChecks(short[] data) {
        this.neighborLightChecks = data;
    }

    public boolean isLightInitialized() {
        return this.isLightInitialized;
    }

    public void setLightInitialized(boolean lightInitialized) {
        this.isLightInitialized = lightInitialized;
    }

    public void setSkylightUpdatedPublic() {
        this.func_177441_y();
    }

    public int getCachedLightFor(EnumSkyBlock type, BlockPos pos) {
        int i = pos.getX() & 15;
        int j = pos.getY();
        int k = pos.getZ() & 15;

        ExtendedBlockStorage storage = this.storageArrays[j >> 4];

        if (storage == null) {
            if (this.canSeeSky(pos)) {
                return type.defaultLightValue;
            }
            else {
                return 0;
            }
        }
        else if (type == EnumSkyBlock.SKY) {
            if (this.worldObj.provider.getHasNoSky()) {
                return 0;
            }
            else {
                return storage.getExtSkylightValue(i, j & 15, k);
            }
        }
        else {
            if (type == EnumSkyBlock.BLOCK) {
                return storage.getExtBlocklightValue(i, j & 15, k);
            }
            else {
                return type.defaultLightValue;
            }
        }
    }

    private ExtendedBlockStorage initSection(int y, boolean storeSkylight) {

        ExtendedBlockStorage storage = new ExtendedBlockStorage(y, storeSkylight);

        LightingHooks.initSkylightForSection(this.worldObj, (Chunk) (Object) this, storage);

        return storage;
    }

    public boolean isLoaded()
    {
        return this.isChunkLoaded;
    }

    public void setChunkLoaded(boolean loaded)
    {
        this.isChunkLoaded = loaded;
    }

    public World getWorld()
    {
        return this.worldObj;
    }

    public int[] getHeightMap()
    {
        return this.heightMap;
    }

    public void setHeightMap(int[] newHeightMap)
    {
        if (this.heightMap.length != newHeightMap.length)
        {
            logger.warn("Could not set level chunk heightmap, array length is " + newHeightMap.length + " instead of " + this.heightMap.length);
        }
        else
        {
            for (int i = 0; i < this.heightMap.length; ++i)
            {
                this.heightMap[i] = newHeightMap[i];
            }
        }
    }

    public Map<BlockPos, TileEntity> getTileEntityMap()
    {
        return this.chunkTileEntityMap;
    }

    public ClassInheritanceMultiMap<Entity>[] getEntityLists()
    {
        return this.entityLists;
    }

    public boolean isTerrainPopulated()
    {
        return this.isTerrainPopulated;
    }

    public void setTerrainPopulated(boolean terrainPopulated)
    {
        this.isTerrainPopulated = terrainPopulated;
    }

    public boolean isLightPopulated()
    {
        return this.isLightPopulated;
    }

    public void setLightPopulated(boolean lightPopulated)
    {
        this.isLightPopulated = lightPopulated;
    }

    public void setModified(boolean modified)
    {
        this.isModified = modified;
    }

    public void setHasEntities(boolean hasEntitiesIn)
    {
        this.hasEntities = hasEntitiesIn;
    }

    public void setLastSaveTime(long saveTime)
    {
        this.lastSaveTime = saveTime;
    }

    public int getLowestHeight()
    {
        return this.heightMapMinimum;
    }

    public long getInhabitedTime()
    {
        return this.inhabitedTime;
    }

    public void setInhabitedTime(long newInhabitedTime)
    {
        this.inhabitedTime = newInhabitedTime;
    }

    public enum EnumCreateEntityType
    {
        IMMEDIATE,
        QUEUED,
        CHECK;
    }
}
