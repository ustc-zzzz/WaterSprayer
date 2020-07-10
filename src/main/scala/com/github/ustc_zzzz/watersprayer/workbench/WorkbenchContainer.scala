package com.github.ustc_zzzz.watersprayer.workbench

import com.github.ustc_zzzz.watersprayer.bottle.LavaBottleItem
import com.github.ustc_zzzz.watersprayer.sprayer.{SprayerItem, SprayerParticles}
import net.minecraft.entity.player.{PlayerEntity, PlayerInventory}
import net.minecraft.inventory.container.{Container, Slot}
import net.minecraft.inventory.{CraftResultInventory, IInventory, Inventory}
import net.minecraft.item.{ItemStack, Items}
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

import scala.util.chaining._

final class WorkbenchContainer(id: Int, inv: PlayerInventory, pos: Option[(World, BlockPos)]) extends Container(WorkbenchContainerType, id) { container =>
  for (i <- 0 until 3; j <- 0 until 9) addSlot(new Slot(inv, 9 + j + i * 9, 8 + j * 18, 84 + i * 18))
  for (i <- 0 until 9) addSlot(new Slot(inv, i, 8 + i * 18, 142))

  private val sprayerOutputSlot: SprayerOutputSlot = new SprayerOutputSlot(118, 51).tap(addSlot)

  private val sprayerSlot: SprayerSlot = new SprayerSlot(42, 51).tap(addSlot)

  private val bottleSlot: BottleSlot = new BottleSlot(80, 24).tap(addSlot)

  override def onCraftMatrixChanged(inv: IInventory): Unit = super.onCraftMatrixChanged(inv).tap { _ =>
    val stack = sprayerSlot.inventory.getStackInSlot(0).copy()
    stack.getItem match {
      case SprayerItem => locally {
        val targetFluidType = if (SprayerItem.Amount.get(stack) > 0) Some(SprayerItem.FluidType.get(stack)) else None
        val sourceFluidType = bottleSlot.getStack.getItem match {
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
    sprayerOutputSlot.inventory.setInventorySlotContents(0, stack)
  }

  override def transferStackInSlot(player: PlayerEntity, index: Int): ItemStack = ItemStack.EMPTY

  override def canInteractWith(player: PlayerEntity): Boolean = pos.forall {
    case (world, pos) => world.getBlockState(pos).getBlock == WorkbenchBlock && {
      player.getDistanceSq(pos.getX + 0.5, pos.getY + 0.5, pos.getZ + 0.5) <= 64.0
    }
  }

  override def onContainerClosed(player: PlayerEntity): Unit = super.onContainerClosed(player).tap(_ => pos.foreach {
    case (world, _) => locally {
      clearContainer(player, world, sprayerSlot.inventory)
      clearContainer(player, world, bottleSlot.inventory)
    }
  })

  private class SprayerOutputSlot(x: Int, y: Int) extends Slot(new CraftResultInventory, 0, x, y) {
    override def onTake(player: PlayerEntity, stack: ItemStack): ItemStack = super.onTake(player, stack).tap { _ =>
      bottleSlot.inventory.setInventorySlotContents(0, new ItemStack(Items.GLASS_BOTTLE))
      sprayerSlot.inventory.setInventorySlotContents(0, ItemStack.EMPTY)
    }

    override def isItemValid(stack: ItemStack): Boolean = false

    override def getSlotStackLimit: Int = 1
  }

  private class SprayerSlot(x: Int, y: Int) extends Slot(new Inventory(1), 0, x, y) {
    override def onSlotChanged(): Unit = super.onSlotChanged().tap(_ => container.onCraftMatrixChanged(inventory))

    override def isItemValid(stack: ItemStack): Boolean = stack.getItem == SprayerItem

    override def getSlotStackLimit: Int = 1
  }

  private class BottleSlot(x: Int, y: Int) extends Slot(new Inventory(1), 0, x, y) {
    override def onSlotChanged(): Unit = super.onSlotChanged().tap(_ => container.onCraftMatrixChanged(inventory))

    override def isItemValid(stack: ItemStack): Boolean = stack.getItem match {
      case Items.GLASS_BOTTLE | Items.HONEY_BOTTLE | LavaBottleItem => true
      case Items.POTION => !Items.POTION.hasEffect(stack)
      case _ => false
    }

    override def getSlotStackLimit: Int = 1
  }
}
