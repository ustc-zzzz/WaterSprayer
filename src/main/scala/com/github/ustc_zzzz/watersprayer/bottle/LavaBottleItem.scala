package com.github.ustc_zzzz.watersprayer.bottle

import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.{Item, ItemGroup, ItemStack}
import net.minecraft.util.{ActionResult, Hand}
import net.minecraft.world.World

import scala.util.chaining._

object LavaBottleItem extends Item(new Item.Properties().group(ItemGroup.MISC).maxStackSize(4)) {
  override def onItemRightClick(world: World, player: PlayerEntity, hand: Hand): ActionResult[ItemStack] = {
    ActionResult.resultConsume(player.tap(_.setActiveHand(hand)).getHeldItem(hand))
  }
}
