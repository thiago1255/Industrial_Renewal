package cassiokf.industrialrenewal.recipes;

import cassiokf.industrialrenewal.config.IRConfig;
import cassiokf.industrialrenewal.init.BlocksRegistration;
import cassiokf.industrialrenewal.init.ItemsRegistration;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.RecipeManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.GameRegistry;

public class ModRecipes
{

    public static void init()
    {
        //if (IRConfig.MainConfig.Recipes.spongeIronRecipeActive)
        //{
        //    Item item = IRConfig.MainConfig.Recipes.spongeDefaultIngotSteel ? ModItems.ingotSteel : OreDictionary.getOres("ingotSteel").get(0).getItem();
        //    GameRegistry.addSmelting(ModItems.spongeIron, new ItemStack(item), 0.7f);
        //}
        //
        //GameRegistry.addShapelessRecipe(new ResourceLocation("industrialrenewal:small_slabs"), new ResourceLocation("small_slabs"), new ItemStack(OreDictionary.getOres("minislabStone").get(0).getItem(), 8),
        //        Ingredient.fromStacks(new ItemStack(ItemsRegistration.SteelSaw, 1, OreDictionary.WILDCARD_VALUE)), Ingredient.fromStacks(new ItemStack(Blocks.STONE_SLAB, 1)));
        //GameRegistry.addShapelessRecipe(new ResourceLocation("industrialrenewal:record_royal_entrance"), new ResourceLocation("record_royal_entrance"),
        //        new ItemStack(ModItems.disc1),
        //        Ingredient.fromStacks(new ItemStack(ModItems.steelSaw, 1, OreDictionary.WILDCARD_VALUE)),
        //        OreIngredient.fromItems(Items.RECORD_13,
        //                Items.RECORD_11, Items.RECORD_CAT, Items.RECORD_BLOCKS, Items.RECORD_MALL, Items.RECORD_CHIRP, Items.RECORD_FAR, Items.RECORD_MELLOHI, Items.RECORD_STAL, Items.RECORD_STRAD,
        //                Items.RECORD_WAIT, Items.RECORD_WARD));
    }

}