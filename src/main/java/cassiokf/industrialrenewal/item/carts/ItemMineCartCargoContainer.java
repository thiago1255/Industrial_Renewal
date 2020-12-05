package cassiokf.industrialrenewal.item.carts;

import cassiokf.industrialrenewal.entity.EntityCargoContainer;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.item.MinecartEntity;
import net.minecraft.world.World;

public class ItemMineCartCargoContainer extends ItemCartBase
{
    public ItemMineCartCargoContainer(String name, CreativeTabs tab)
    {
        super(name, tab);
    }

    @Override
    public MinecartEntity getEntity(World world, double x, double y, double z)
    {
        return new EntityCargoContainer(world, x, y, z);
    }
}
