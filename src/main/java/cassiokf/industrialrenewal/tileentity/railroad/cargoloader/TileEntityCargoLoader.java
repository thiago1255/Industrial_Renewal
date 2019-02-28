package cassiokf.industrialrenewal.tileentity.railroad.cargoloader;

import cassiokf.industrialrenewal.network.NetworkHandler;
import cassiokf.industrialrenewal.network.PacketCargoLoader;
import cassiokf.industrialrenewal.network.PacketReturnCargoLoader;
import cassiokf.industrialrenewal.tileentity.railroad.railloader.BlockLoaderRail;
import cassiokf.industrialrenewal.tileentity.railroad.railloader.TileEntityLoaderRail;
import net.minecraft.block.Block;
import net.minecraft.block.BlockChest;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.*;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.IHopper;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityLockableLoot;
import net.minecraft.util.EntitySelectors;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.NetworkRegistry;

import javax.annotation.Nullable;
import java.util.List;

public class TileEntityCargoLoader extends TileEntityLockableLoot implements IHopper, ITickable {

    private NonNullList<ItemStack> inventory = NonNullList.withSize(5, ItemStack.EMPTY);
    private int transferCooldown = -1;
    private long tickedGameTime;
    private waitEnum waitE;

    public enum waitEnum {
        WAIT_FULL(0),
        WAIT_EMPTY(1),
        NO_ACTIVITY(2),
        NEVER(3);

        public int intValue;

        waitEnum(int value) {
            intValue = value;
        }

        public static waitEnum valueOf(int waitNo) {
            if (waitNo > waitEnum.values().length - 1) {
                waitNo = 0;
            }
            for (waitEnum l : waitEnum.values()) {
                if (l.intValue == waitNo) return l;
            }
            throw new IllegalArgumentException("waitEnum not found");
        }
    }

    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newState) {
        return (oldState.getBlock() != newState.getBlock());
    }

    private void setBlockState(boolean value) {
        IBlockState oldState = this.world.getBlockState(this.pos);
        if (oldState.getValue(BlockCargoLoader.LOADING) != value) {
            this.world.setBlockState(this.pos, oldState.withProperty(BlockCargoLoader.LOADING, value));
            onChange();
            markDirty();
        }
    }

    public waitEnum getWaitEnum() {
        if (waitE == null) {
            waitE = waitEnum.WAIT_FULL;
        }
        return waitE;
    }

    public void setWaitEnum(int value) {
        waitE = waitEnum.valueOf(value);
    }

    public void setNextWaitEnum(boolean value) {
        int old = getWaitEnum().intValue;
        if (value) {
            waitE = waitEnum.valueOf(old + 1);
        }
        onChange();
        markDirty();
    }

    /**
     * Gets the inventory that the provided hopper will transfer items from.
     */
    private static IInventory getSourceInventory(IHopper hopper) {
        return getInventoryAtPosition(hopper.getWorld(), new BlockPos(hopper.getXPos(), hopper.getYPos() + 1.0D, hopper.getZPos()));
    }

    @Override
    public void onLoad() {
        if (world.isRemote) {
            NetworkHandler.INSTANCE.sendToServer(new PacketReturnCargoLoader(this));
        }
    }

    private EnumFacing getOutput() {
        if (!isUnload()) {
            return EnumFacing.DOWN;
        }
        IBlockState state = this.world.getBlockState(this.pos).getActualState(this.world, this.pos);
        return state.getValue(BlockCargoLoader.FACING).getOpposite();
    }

    private boolean isUnload() {
        IBlockState state = this.world.getBlockState(this.pos).getActualState(this.world, this.pos);
        return state.getValue(BlockCargoLoader.UNLOAD);
    }

    /**
     * Returns the IInventory (if applicable) of the TileEntity at the specified position
     */
    private static IInventory getInventoryAtPosition(World worldIn, BlockPos pos) {
        IInventory iinventory = null;
        int i = MathHelper.floor(pos.getX());
        int j = MathHelper.floor(pos.getY());
        int k = MathHelper.floor(pos.getZ());
        BlockPos blockpos = new BlockPos(i, j, k);
        IBlockState state = worldIn.getBlockState(blockpos);
        Block block = state.getBlock();

        if (block.hasTileEntity(state)) {
            TileEntity tileentity = worldIn.getTileEntity(blockpos);

            if (tileentity instanceof IInventory) {
                iinventory = (IInventory) tileentity;

                if (iinventory instanceof TileEntityChest && block instanceof BlockChest) {
                    iinventory = ((BlockChest) block).getContainer(worldIn, blockpos, true);
                }
            }
        }

        if (iinventory == null) {
            List<Entity> list = worldIn.getEntitiesInAABBexcluding(null, new AxisAlignedBB(pos.getX() - 0.5D, pos.getY() - 0.5D, pos.getZ() - 0.5D, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D), EntitySelectors.HAS_INVENTORY);

            if (!list.isEmpty()) {
                iinventory = (IInventory) list.get(worldIn.rand.nextInt(list.size()));
            }
        }

        return iinventory;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        if (!this.checkLootAndWrite(compound)) {
            ItemStackHelper.saveAllItems(compound, this.inventory);
        }

        compound.setInteger("TransferCooldown", this.transferCooldown);
        compound.setInteger("EnumConfig", this.waitE.intValue);

        if (this.hasCustomName()) {
            compound.setString("CustomName", this.customName);
        }

        return super.writeToNBT(compound);
    }

    void onChange() {
        if (!this.world.isRemote) {
            NetworkHandler.INSTANCE.sendToAllAround(new PacketCargoLoader(TileEntityCargoLoader.this), new NetworkRegistry.TargetPoint(world.provider.getDimension(), pos.getX(), pos.getY(), pos.getZ(), 32));
        }
    }

    /**
     * Returns the number of slots in the inventory.
     */
    @Override
    public int getSizeInventory() {
        return this.inventory.size();
    }

    /**
     * Removes up to a specified number of items from an inventory slot and returns them in a new stack.
     */
    public ItemStack decrStackSize(int index, int count) {
        this.fillWithLoot(null);
        return ItemStackHelper.getAndSplit(this.getItems(), index, count);
    }

    /**
     * Sets the given item stack to the specified slot in the inventory (can be crafting or armor sections).
     */
    public void setInventorySlotContents(int index, ItemStack stack) {
        this.fillWithLoot(null);
        this.getItems().set(index, stack);

        if (stack.getCount() > this.getInventoryStackLimit()) {
            stack.setCount(this.getInventoryStackLimit());
        }
    }

    /**
     * Get the name of this object. For players this returns their username
     */
    public String getName() {
        return this.hasCustomName() ? this.customName : "container.hopper";
    }

    /**
     * Returns the maximum stack size for a inventory slot. Seems to always be 64, possibly will be extended.
     */
    public int getInventoryStackLimit() {
        return 64;
    }

    /**
     * Like the old updateEntity(), except more generic.
     */
    @Override
    public void update() {
        if (this.world != null && !this.world.isRemote) {
            --this.transferCooldown;
            this.tickedGameTime = this.world.getTotalWorldTime();

            if (!this.isOnTransferCooldown()) {
                this.setTransferCooldown(0);
                this.updateHopper();
            }
        }
    }

    private void letCartPass(boolean value) {
        if (getWaitEnum() == waitEnum.NEVER) {
            return;
        }
        if (isUnload()) {
            IBlockState oldState = this.world.getBlockState(this.pos.up());
            if (oldState.getBlock() instanceof BlockLoaderRail) {
                TileEntityLoaderRail te = (TileEntityLoaderRail) this.world.getTileEntity(this.pos.up());
                if (te != null) {
                    te.ChangePass(value);
                }
            }
            return;
        }
        IBlockState oldState = this.world.getBlockState(this.pos.down(2));
        if (oldState.getBlock() instanceof BlockLoaderRail) {
            TileEntityLoaderRail te = (TileEntityLoaderRail) this.world.getTileEntity(this.pos.down(2));
            if (te != null) {
                te.ChangePass(value);
            }
        }
    }

    private boolean isInventoryEmpty() {
        for (ItemStack itemstack : this.inventory) {
            if (!itemstack.isEmpty()) {
                return false;
            }
        }

        return true;
    }

    public boolean isEmpty() {
        return this.isInventoryEmpty();
    }

    private boolean isFull() {
        for (ItemStack itemstack : this.inventory) {
            if (itemstack.isEmpty() || itemstack.getCount() != itemstack.getMaxStackSize()) {
                return false;
            }
        }

        return true;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        this.inventory = NonNullList.withSize(this.getSizeInventory(), ItemStack.EMPTY);

        if (!this.checkLootAndRead(compound)) ItemStackHelper.loadAllItems(compound, this.inventory);
        if (compound.hasKey("CustomName", 8)) this.customName = compound.getString("CustomName");

        if (compound.hasKey("TransferCooldown")) this.transferCooldown = compound.getInteger("TransferCooldown");
        if (compound.hasKey("EnumConfig")) waitE = waitEnum.valueOf(compound.getInteger("EnumConfig"));

        super.readFromNBT(compound);
    }

    /**
     * Returns false if the inventory has any room to place items in
     */
    private boolean isInventoryFull(IInventory inventoryIn, EnumFacing side) {
        if (inventoryIn instanceof ISidedInventory) {
            ISidedInventory isidedinventory = (ISidedInventory) inventoryIn;
            int[] aint = isidedinventory.getSlotsForFace(side);

            for (int k : aint) {
                ItemStack itemstack1 = isidedinventory.getStackInSlot(k);

                if (itemstack1.isEmpty() || itemstack1.getCount() != itemstack1.getMaxStackSize()) {
                    return false;
                }
            }
        } else {
            int i = inventoryIn.getSizeInventory();

            for (int j = 0; j < i; ++j) {
                ItemStack itemstack = inventoryIn.getStackInSlot(j);

                if (itemstack.isEmpty() || itemstack.getCount() != itemstack.getMaxStackSize()) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Returns false if the specified IInventory contains any items
     */
    private static boolean isInventoryEmpty(IInventory inventoryIn, EnumFacing side) {
        if (inventoryIn instanceof ISidedInventory) {
            ISidedInventory isidedinventory = (ISidedInventory) inventoryIn;
            int[] aint = isidedinventory.getSlotsForFace(side);

            for (int i : aint) {
                if (!isidedinventory.getStackInSlot(i).isEmpty()) {
                    return false;
                }
            }
        } else {
            int j = inventoryIn.getSizeInventory();

            for (int k = 0; k < j; ++k) {
                if (!inventoryIn.getStackInSlot(k).isEmpty()) {
                    return false;
                }
            }
        }

        return true;
    }

    protected void updateHopper() {
        if (this.world != null && !this.world.isRemote) {
            if (!this.isOnTransferCooldown()) {
                boolean flag = false;
                if (!this.isInventoryEmpty()) {
                    flag = this.transferItemsOut();
                } else if (!isUnload() && getWaitEnum() == waitEnum.NO_ACTIVITY) {
                    letCartPass(true);
                    return;
                }

                setBlockState(flag);

                if (!this.isFull()) {
                    flag = pullItems(this) || flag;
                } else if (isUnload() && getWaitEnum() == waitEnum.NO_ACTIVITY) {
                    letCartPass(true);
                    return;
                }

                if (flag) {
                    letCartPass(false);
                    this.setTransferCooldown(1);
                    this.markDirty();
                }
            }
        }
    }

    /**
     * Pulls from the specified slot in the inventory and places in any available slot in the hopper. Returns true if
     * the entire stack was moved
     */
    private static boolean pullItemFromSlot(IHopper hopper, IInventory inventoryIn, int index, EnumFacing direction) {
        ItemStack itemstack = inventoryIn.getStackInSlot(index);

        if (!itemstack.isEmpty() && canExtractItemFromSlot(inventoryIn, itemstack, index, direction)) {
            ItemStack itemstack1 = itemstack.copy();
            ItemStack itemstack2 = putStackInInventoryAllSlots(inventoryIn, hopper, inventoryIn.decrStackSize(index, 1), null);

            if (itemstack2.isEmpty()) {
                inventoryIn.markDirty();
                return true;
            }

            inventoryIn.setInventorySlotContents(index, itemstack1);
        }

        return false;
    }

    /**
     * Attempts to place the passed EntityItem's stack into the inventory using as many slots as possible. Returns false
     * if the stackSize of the drop was not depleted.
     */
    private static boolean putDropInInventoryAllSlots(IInventory source, IInventory destination, EntityItem entity) {
        boolean flag = false;

        if (entity == null) {
            return false;
        } else {
            ItemStack itemstack = entity.getItem().copy();
            ItemStack itemstack1 = putStackInInventoryAllSlots(source, destination, itemstack, null);

            if (itemstack1.isEmpty()) {
                flag = true;
                entity.setDead();
            } else {
                entity.setItem(itemstack1);
            }

            return flag;
        }
    }

    /**
     * Attempts to place the passed stack in the inventory, using as many slots as required. Returns leftover items
     */
    private static ItemStack putStackInInventoryAllSlots(IInventory source, IInventory destination, ItemStack stack, @Nullable EnumFacing direction) {
        if (destination instanceof ISidedInventory && direction != null) {
            ISidedInventory isidedinventory = (ISidedInventory) destination;
            int[] aint = isidedinventory.getSlotsForFace(direction);

            for (int k = 0; k < aint.length && !stack.isEmpty(); ++k) {
                stack = insertStack(source, destination, stack, aint[k], direction);
            }
        } else {
            int i = destination.getSizeInventory();

            for (int j = 0; j < i && !stack.isEmpty(); ++j) {
                stack = insertStack(source, destination, stack, j, direction);
            }
        }

        return stack;
    }

    /**
     * Can this hopper insert the specified item from the specified slot on the specified side?
     */
    private static boolean canInsertItemInSlot(IInventory inventoryIn, ItemStack stack, int index, EnumFacing side) {
        if (!inventoryIn.isItemValidForSlot(index, stack)) {
            return false;
        } else {
            return !(inventoryIn instanceof ISidedInventory) || ((ISidedInventory) inventoryIn).canInsertItem(index, stack, side);
        }
    }

    /**
     * Can this hopper extract the specified item from the specified slot on the specified side?
     */
    private static boolean canExtractItemFromSlot(IInventory inventoryIn, ItemStack stack, int index, EnumFacing side) {
        return !(inventoryIn instanceof ISidedInventory) || ((ISidedInventory) inventoryIn).canExtractItem(index, stack, side);
    }

    /**
     * Insert the specified stack to the specified inventory and return any leftover items
     */
    private static ItemStack insertStack(IInventory source, IInventory destination, ItemStack stack, int index, EnumFacing direction) {
        ItemStack itemstack = destination.getStackInSlot(index);

        if (canInsertItemInSlot(destination, stack, index, direction)) {
            boolean flag = false;
            boolean flag1 = destination.isEmpty();

            if (itemstack.isEmpty()) {
                destination.setInventorySlotContents(index, stack);
                stack = ItemStack.EMPTY;
                flag = true;
            } else if (canCombine(itemstack, stack)) {
                int i = stack.getMaxStackSize() - itemstack.getCount();
                int j = Math.min(stack.getCount(), i);
                stack.shrink(j);
                itemstack.grow(j);
                flag = j > 0;
            }

            if (flag) {
                if (flag1 && destination instanceof TileEntityCargoLoader) {
                    TileEntityCargoLoader tileentityhopper1 = (TileEntityCargoLoader) destination;

                    if (!tileentityhopper1.mayTransfer()) {
                        int k = 0;

                        if (source instanceof TileEntityCargoLoader) {
                            TileEntityCargoLoader tileentityhopper = (TileEntityCargoLoader) source;

                            if (tileentityhopper1.tickedGameTime >= tileentityhopper.tickedGameTime) {
                                k = 1;
                            }
                        }

                        tileentityhopper1.setTransferCooldown(8 - k);
                    }
                }

                destination.markDirty();
            }
        }

        return stack;
    }

    private boolean transferItemsOut() {
        IInventory iinventory = this.getInventoryForHopperTransfer();

        if (iinventory == null) {
            return false;
        } else {
            EnumFacing enumfacing = getOutput();
            boolean load = !isUnload();
            if (this.isInventoryFull(iinventory, enumfacing)) {
                if (load && (getWaitEnum() == waitEnum.WAIT_FULL || getWaitEnum() == waitEnum.NO_ACTIVITY)) {
                    letCartPass(true);
                }
                return false;
            } else {
                for (int i = 0; i < this.getSizeInventory(); ++i) {
                    if (!this.getStackInSlot(i).isEmpty()) {
                        ItemStack itemstack = this.getStackInSlot(i).copy();
                        ItemStack itemstack1 = putStackInInventoryAllSlots(this, iinventory, this.decrStackSize(i, 1), enumfacing);

                        if (itemstack1.isEmpty()) {
                            iinventory.markDirty();
                            return true;
                        }
                        this.setInventorySlotContents(i, itemstack);
                    }
                }
                if (load && (getWaitEnum() == waitEnum.NO_ACTIVITY || getWaitEnum() == waitEnum.WAIT_FULL)) {
                    letCartPass(true);
                }
                return false;
            }
        }
    }

    /**
     * Pull dropped {@link net.minecraft.entity.item.EntityItem EntityItem}s from the world above the hopper and items
     * from any inventory attached to this hopper into the hopper's inventory.
     *
     * @param hopper the hopper in question
     * @return whether any items were successfully added to the hopper
     */
    private boolean pullItems(IHopper hopper) {
        IInventory iinventory = getSourceInventory(hopper);
        boolean unloader = isUnload();

        if (iinventory != null) {
            EnumFacing enumfacing = getOutput();

            if (isInventoryEmpty(iinventory, enumfacing)) {
                if (unloader && (getWaitEnum() == waitEnum.WAIT_EMPTY || getWaitEnum() == waitEnum.NO_ACTIVITY)) {
                    letCartPass(true);
                }
                return false;
            }

            if (iinventory instanceof ISidedInventory) {
                ISidedInventory isidedinventory = (ISidedInventory) iinventory;
                int[] aint = isidedinventory.getSlotsForFace(enumfacing);

                for (int i : aint) {
                    if (pullItemFromSlot(hopper, iinventory, i, enumfacing)) {
                        return true;
                    }
                }
            } else {
                int j = iinventory.getSizeInventory();

                for (int k = 0; k < j; ++k) {
                    if (pullItemFromSlot(hopper, iinventory, k, enumfacing)) {
                        return true;
                    }
                }
            }
        } else {
            for (EntityItem entityitem : getCaptureItems(hopper.getWorld(), hopper.getXPos(), hopper.getYPos(), hopper.getZPos())) {
                if (putDropInInventoryAllSlots(null, hopper, entityitem)) {
                    return true;
                }
            }
        }
        if (unloader && getWaitEnum() == waitEnum.NO_ACTIVITY) {
            letCartPass(true);
        }
        return false;
    }

    private static List<EntityItem> getCaptureItems(World worldIn, double p_184292_1_, double p_184292_3_, double p_184292_5_) {
        return worldIn.getEntitiesWithinAABB(EntityItem.class, new AxisAlignedBB(p_184292_1_ - 0.5D, p_184292_3_, p_184292_5_ - 0.5D, p_184292_1_ + 0.5D, p_184292_3_ + 1.5D, p_184292_5_ + 0.5D), EntitySelectors.IS_ALIVE);
    }

    /**
     * Returns the IInventory that this hopper is pointing into
     */
    private IInventory getInventoryForHopperTransfer() {
        EnumFacing enumfacing = getOutput();
        BlockPos pos = this.pos.offset(enumfacing);
        if (!isUnload()) pos = pos.down();
        return getInventoryAtPosition(this.getWorld(), pos);
    }

    private static boolean canCombine(ItemStack stack1, ItemStack stack2) {
        if (stack1.getItem() != stack2.getItem()) {
            return false;
        } else if (stack1.getMetadata() != stack2.getMetadata()) {
            return false;
        } else if (stack1.getCount() > stack1.getMaxStackSize()) {
            return false;
        } else {
            return ItemStack.areItemStackTagsEqual(stack1, stack2);
        }
    }

    /**
     * Gets the world X position for this hopper entity.
     */
    public double getXPos() {
        return (double) this.pos.getX() + 0.5D;
    }

    /**
     * Gets the world Y position for this hopper entity.
     */
    public double getYPos() {
        return (double) this.pos.getY() + 0.5D;
    }

    /**
     * Gets the world Z position for this hopper entity.
     */
    public double getZPos() {
        return (double) this.pos.getZ() + 0.5D;
    }

    private void setTransferCooldown(int ticks) {
        this.transferCooldown = ticks;
    }

    private boolean isOnTransferCooldown() {
        return this.transferCooldown > 0;
    }

    private boolean mayTransfer() {
        return this.transferCooldown > 8;
    }

    @Override
    public String getGuiID() {
        return "minecraft:hopper";
    }

    @Override
    public Container createContainer(InventoryPlayer playerInventory, EntityPlayer playerIn) {
        this.fillWithLoot(playerIn);
        return new ContainerHopper(playerInventory, this, playerIn);
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return this.inventory;
    }
}
