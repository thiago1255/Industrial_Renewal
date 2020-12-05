package cassiokf.industrialrenewal.tileentity.tubes;

import cassiokf.industrialrenewal.blocks.pipes.BlockEnergyCable;
import cassiokf.industrialrenewal.blocks.pipes.BlockFluidPipe;
import cassiokf.industrialrenewal.config.IRConfig;
import cassiokf.industrialrenewal.init.BlocksRegistration;
import cassiokf.industrialrenewal.item.ItemPowerScrewDrive;
import cassiokf.industrialrenewal.util.Utils;
import cassiokf.industrialrenewal.util.enums.EnumCableIn;
import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class TileEntityCableTray extends TileEntityMultiBlocksTube<TileEntityCableTray>
{
    private EnumCableIn energyCable = EnumCableIn.NONE;
    private boolean fluidPipe = false;
    private boolean dataCable = false;

    public TileEntityCableTray(TileEntityType<?> tileEntityTypeIn)
    {
        super(tileEntityTypeIn);
    }

    @Override
    public void tick()
    {
        //if (!firstTick)
        //{
        //    firstTick = true;
        //    refreshConnections();
        //}
    }

    public boolean onBlockActivated(PlayerEntity player, ItemStack stack)
    {
        Block block = Block.getBlockFromItem(stack.getItem());
        if (block instanceof BlockFluidPipe && !fluidPipe)
        {
            fluidPipe = true;
            if (!world.isRemote && !player.isCreative()) stack.shrink(1);
            refreshConnections();
            if (!world.isRemote)
            {
                world.playSound(null, pos, SoundEvents.BLOCK_METAL_PLACE, SoundCategory.BLOCKS, 1f, 1f);
                sync();
            }
            return true;
        }
        if (block instanceof BlockEnergyCable && energyCable.equals(EnumCableIn.NONE))
        {
            switch (((BlockEnergyCable) block).type)
            {
                default:
                case LV:
                    energyCable = EnumCableIn.LV;
                    break;
                case MV:
                    energyCable = EnumCableIn.MV;
                    break;
                case HV:
                    energyCable = EnumCableIn.HV;
                    break;
            }
            if (!world.isRemote && !player.isCreative()) stack.shrink(1);
            refreshConnections();
            if (!world.isRemote)
            {
                world.playSound(null, pos, SoundEvents.BLOCK_METAL_PLACE, SoundCategory.BLOCKS, 1f, 1f);
                sync();
            }
            return true;
        }
        if (stack.getItem() instanceof ItemPowerScrewDrive && (fluidPipe || dataCable || energyCable != EnumCableIn.NONE))
        {
            if (!player.isCreative()) spawnBlocks(player);
            fluidPipe = false;
            dataCable = false;
            energyCable = EnumCableIn.NONE;
            refreshConnections();
            if (!world.isRemote)
            {
                ItemPowerScrewDrive.playDrillSound(world, pos);
                sync();
            }
            return true;
        }
        return false;
    }

    private void spawnBlocks(PlayerEntity player)
    {
        if (world.isRemote) return;
        if (fluidPipe)
        {
            ItemStack stack = new ItemStack(BlocksRegistration.FLUIDPIPE_ITEM.get());
            if (player != null) player.inventory.addItemStackToInventory(stack);
            else Utils.spawnItemStack(world, pos, stack);
        }
        //if (dataCable) ;
        if (energyCable != EnumCableIn.NONE)
        {
            switch (energyCable)
            {
                case LV:
                    ItemStack stack = new ItemStack(BlocksRegistration.ENERGYCABLELV_ITEM.get());
                    if (player != null) player.inventory.addItemStackToInventory(stack);
                    else Utils.spawnItemStack(world, pos, stack);
                    break;
                case MV:
                    ItemStack stack2 = new ItemStack(BlocksRegistration.ENERGYCABLEMV_ITEM.get());
                    if (player != null) player.inventory.addItemStackToInventory(stack2);
                    else Utils.spawnItemStack(world, pos, stack2);
                    break;
                case HV:
                    ItemStack stack3 = new ItemStack(BlocksRegistration.ENERGYCABLEHV_ITEM.get());
                    if (player != null) player.inventory.addItemStackToInventory(stack3);
                    else Utils.spawnItemStack(world, pos, stack3);
                    break;
            }
        }
    }

    public boolean hasPipe()
    {
        return fluidPipe;
    }

    public boolean hasData()
    {
        return dataCable;
    }

    public EnumCableIn getCableIn()
    {
        return energyCable;
    }

    @Override
    public boolean isMaster()
    {
        return false;
    }

    @Override
    public void setMaster(TileEntityCableTray master)
    {
    }

    @Override
    public void checkForOutPuts()
    {
    }

    @Override
    public boolean instanceOf(TileEntity te)
    {
        return te instanceof TileEntityCableTray;
    }

    @Override
    public boolean isTray()
    {
        return true;
    }

    public void refreshConnections()
    {
        if (IRConfig.Main.debugMessages.get()) System.out.println("Refresh connections at" + pos);
        List<TileEntityMultiBlocksTube> connectedCables = new ArrayList<>();
        List<TileEntityCableTray> cableTrayList = new ArrayList<>();
        Stack<TileEntityCableTray> traversingCables = new Stack<>();
        traversingCables.add(this);
        while (!traversingCables.isEmpty())
        {
            TileEntityCableTray storage = traversingCables.pop();
            cableTrayList.add(storage);
            for (Direction d : getFacesToCheck())
            {
                TileEntity te = world.getTileEntity(storage.getPos().offset(d));
                if (instanceOf(te) && !cableTrayList.contains(te))
                {
                    traversingCables.add((TileEntityCableTray) te);
                } else if (!instanceOf(te) && te instanceof TileEntityMultiBlocksTube && !connectedCables.contains(te))
                {
                    connectedCables.add((TileEntityMultiBlocksTube) te);
                }
            }
        }
        for (TileEntityMultiBlocksTube cables : connectedCables)
        {
            cables.initializeMultiblockIfNecessary(true);
        }
    }

    @Override
    public void onBlockBreak()
    {
        spawnBlocks(null);
        super.onBlockBreak();
        refreshConnections();
    }

    @Override
    public void read(CompoundNBT compound)
    {
        fluidPipe = compound.getBoolean("fluidPipe");
        dataCable = compound.getBoolean("dataCable");
        energyCable = EnumCableIn.byIndex(compound.getInt("energyCableIn"));
        super.read(compound);
    }

    @Override
    public CompoundNBT write(CompoundNBT compound)
    {
        compound.putBoolean("fluidPipe", fluidPipe);
        compound.putBoolean("dataCable", dataCable);
        compound.putInt("energyCableIn", energyCable.getIndex());
        return super.write(compound);
    }
}