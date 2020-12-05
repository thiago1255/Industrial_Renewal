package cassiokf.industrialrenewal.item;

import cassiokf.industrialrenewal.init.BlocksRegistration;
import cassiokf.industrialrenewal.tileentity.TileEntityBarrel;
import net.minecraft.block.BlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;
import java.util.Objects;

public class ItemBarrel extends ItemBase
{
    public ItemBarrel(Item.Properties properties)
    {
        super(properties);
    }

    @Override
    public ActionResultType onItemUse(ItemUseContext context)
    {
        ItemStack itemstack = context.getPlayer().getHeldItemMainhand();
        BlockPos posOffset = context.getPos().offset(context.getFace());
        World worldIn = context.getWorld();
        BlockState state = worldIn.getBlockState(posOffset);
        if (worldIn.isAirBlock(posOffset) || state.getBlock().isReplaceable(state, new BlockItemUseContext(context)))
        {
            playSound(worldIn, context.getPos(), SoundEvents.BLOCK_METAL_PLACE);

            worldIn.setBlockState(posOffset, BlocksRegistration.BARREL.get().getStateForPlacement(new BlockItemUseContext(context)));
            TileEntity te = worldIn.getTileEntity(posOffset);
            if (te instanceof TileEntityBarrel && itemstack.getTag() != null && itemstack.getTag().contains("FluidName"))
                ((TileEntityBarrel) te).tank.readFromNBT(itemstack.getTag());
            itemstack.shrink(1);

            return ActionResultType.SUCCESS;
        }
        return ActionResultType.FAIL;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void addInformation(ItemStack itemstack, World world, List<ITextComponent> list, ITooltipFlag flag)
    {
        CompoundNBT nbt = itemstack.getTag();
        if (nbt != null && nbt.contains("FluidName") && nbt.contains("Amount"))
        {
            list.add(new StringTextComponent(nbt.getString("FluidName") + ": " + nbt.getInt("Amount")));
        }
    }

    private static void playSound(World world, BlockPos pos, SoundEvent soundEvent)
    {
        world.playSound(null, pos, soundEvent, SoundCategory.BLOCKS, 1.0F, 1.0F);
    }
}
