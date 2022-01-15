package com.github.ustc_zzzz.watersprayer.workbench

import com.github.ustc_zzzz.watersprayer.bottle.LavaBottleItem
import com.github.ustc_zzzz.watersprayer.sprayer.{SprayerItem, SprayerParticles}
import net.minecraft.entity.player.{PlayerEntity, PlayerInventory}
import net.minecraft.inventory.container.{Container, Slot}
import net.minecraft.inventory.{CraftResultInventory, IInventory, Inventory}
import net.minecraft.item.{ItemStack, Items}
import net.minecraft.potion.PotionUtils
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

import scala.util.chaining._

final class WorkbenchContainer(id: Int, inv: PlayerInventory, pos: Option[(World, BlockPos)]) extends Container(WorkbenchContainerType, id) {
  workbench =>
  for (i <- 0 until 3; j <- 0 until 9) addSlot(new Slot(inv, 9 + j + i * 9, 8 + j * 18, 84 + i * 18))
  for (i <- 0 until 9) addSlot(new Slot(inv, i, 8 + i * 18, 142))

  private val sprayerOutputSlot: SprayerOutputSlot = new SprayerOutputSlot(118, 51).tap(addSlot)

  private val sprayerSlot: SprayerSlot = new SprayerSlot(42, 51).tap(addSlot)

  private val bottleSlot: BottleSlot = new BottleSlot(80, 24).tap(addSlot)

  override def slotsChanged(inv: IInventory): Unit = super.slotsChanged(inv).tap { _ =>
    val stack = sprayerSlot.container.getItem(0).copy()
    stack.getItem match {
      case SprayerItem => locally {
        val targetFluidType = if (SprayerItem.Amount.get(stack) > 0) Some(SprayerItem.FluidType.get(stack)) else None
        val sourceFluidType = bottleSlot.getItem.getItem match {
          case Items.HONEY_BOTTLE => Some(SprayerParticles.HoneyType)
          case LavaBottleItem => Some(SprayerParticles.LavaFluidType)
          case Items.POTION => Some(SprayerParticles.WaterFluidType)
          case _ => None
        }
        if (!sourceFluidType.exists(s => targetFluidType.forall(t => s == t))) stack.setCount(0) else {
          SprayerItem.Amount.set(stack, SprayerItem.Amount.get(stack) + 600)
          SprayerItem.FluidType.set(stack, sourceFluidType.get)
        }
      }
      case _ => stack.setCount(0)
    }
    sprayerOutputSlot.container.setItem(0, stack)
  }

  override def quickMoveStack(player: PlayerEntity, index: Int): ItemStack = ItemStack.EMPTY

  override def stillValid(player: PlayerEntity): Boolean = pos.forall {
    case (world, pos) => world.getBlockState(pos).getBlock == WorkbenchBlock && {
      player.distanceToSqr(pos.getX + 0.5, pos.getY + 0.5, pos.getZ + 0.5) <= 64.0
    }
  }

  override def removed(player: PlayerEntity): Unit = super.removed(player).tap(_ => pos.foreach {
    case (world, _) => locally {
      clearContainer(player, world, sprayerSlot.container)
      clearContainer(player, world, bottleSlot.container)
    }
  })

  private class SprayerOutputSlot(x: Int, y: Int) extends Slot(new CraftResultInventory, 0, x, y) {
    override def onTake(player: PlayerEntity, stack: ItemStack): ItemStack = super.onTake(player, stack).tap { _ =>
      bottleSlot.container.setItem(0, new ItemStack(Items.GLASS_BOTTLE))
      sprayerSlot.container.setItem(0, ItemStack.EMPTY)
    }

    override def mayPlace(stack: ItemStack): Boolean = false

    override def getMaxStackSize: Int = 1
  }

  private class SprayerSlot(x: Int, y: Int) extends Slot(new Inventory(1), 0, x, y) {
    override def setChanged(): Unit = super.setChanged().tap(_ => workbench.slotsChanged(container))

    override def mayPlace(stack: ItemStack): Boolean = stack.getItem == SprayerItem

    override def getMaxStackSize: Int = 1
  }

  private class BottleSlot(x: Int, y: Int) extends Slot(new Inventory(1), 0, x, y) {
    override def setChanged(): Unit = super.setChanged().tap(_ => workbench.slotsChanged(container))

    override def mayPlace(stack: ItemStack): Boolean = stack.getItem match {
      case Items.GLASS_BOTTLE | Items.HONEY_BOTTLE | LavaBottleItem => true
      case Items.POTION => PotionUtils.getMobEffects(stack).isEmpty
      case _ => false
    }

    override def getMaxStackSize: Int = 1
  }
}
