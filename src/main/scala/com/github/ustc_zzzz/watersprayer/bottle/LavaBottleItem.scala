package com.github.ustc_zzzz.watersprayer.bottle

import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.{Item, ItemGroup, ItemStack}
import net.minecraft.util.{ActionResult, Hand}
import net.minecraft.world.World

import scala.util.chaining._

object LavaBottleItem extends Item(new Item.Properties().tab(ItemGroup.TAB_MISC).stacksTo(4)) {
  override def use(world: World, player: PlayerEntity, hand: Hand): ActionResult[ItemStack] = {
    ActionResult.consume(player.tap(_.startUsingItem(hand)).getItemInHand(hand))
  }
}
