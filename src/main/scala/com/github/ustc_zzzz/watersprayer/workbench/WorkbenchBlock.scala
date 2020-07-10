package com.github.ustc_zzzz.watersprayer.workbench

import net.minecraft.block.material.Material
import net.minecraft.block.{Block, BlockState, SoundType}
import net.minecraft.entity.player.{PlayerEntity, PlayerInventory}
import net.minecraft.inventory.container.{Container, INamedContainerProvider}
import net.minecraft.util.math.{BlockPos, BlockRayTraceResult}
import net.minecraft.util.text.{ITextComponent, TranslationTextComponent}
import net.minecraft.util.{ActionResultType, Hand}
import net.minecraft.world.World

import scala.util.chaining._

object WorkbenchBlock extends Block(Block.Properties.create(Material.ROCK).hardnessAndResistance(2.5F).sound(SoundType.STONE).notSolid()) {
  // noinspection ScalaDeprecation
  override def onBlockActivated(state: BlockState, world: World, pos: BlockPos,
                                player: PlayerEntity, hand: Hand, hit: BlockRayTraceResult): ActionResultType = {
    ActionResultType.SUCCESS.tap(_ => if (!world.isRemote) player.openContainer(new ContainerProvider(world, pos)))
  }

  class ContainerProvider(world: World, pos: BlockPos) extends INamedContainerProvider {
    override def getDisplayName: ITextComponent = {
      new TranslationTextComponent("container.watersprayer.workbench")
    }

    override def createMenu(id: Int, inv: PlayerInventory, player: PlayerEntity): Container = {
      new WorkbenchContainer(id, inv, Some(world -> pos))
    }
  }
}
