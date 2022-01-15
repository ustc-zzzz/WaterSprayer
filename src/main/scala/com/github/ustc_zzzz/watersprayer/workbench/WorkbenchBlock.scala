package com.github.ustc_zzzz.watersprayer.workbench

import net.minecraft.block.material.Material
import net.minecraft.block.{AbstractBlock, Block, BlockState, SoundType}
import net.minecraft.entity.player.{PlayerEntity, PlayerInventory}
import net.minecraft.inventory.container.{Container, INamedContainerProvider}
import net.minecraft.util.math.shapes.{ISelectionContext, VoxelShape, VoxelShapes}
import net.minecraft.util.math.{BlockPos, BlockRayTraceResult}
import net.minecraft.util.text.{ITextComponent, TranslationTextComponent}
import net.minecraft.util.{ActionResultType, Hand}
import net.minecraft.world.{IBlockReader, World}

import scala.util.chaining._

object WorkbenchBlock extends Block(AbstractBlock.Properties.of(Material.STONE).strength(2.5F).sound(SoundType.STONE)) {
  private final val shape = VoxelShapes.or(
    Block.box(0.0, 0.0, 0.0, 16.0, 8.0, 16.0),
    Block.box(6.0, 8.0, 6.0, 10.0, 9.0, 10.0),
    Block.box(4.0, 9.0, 4.0, 12.0, 12.0, 12.0),
    Block.box(0.0, 12.0, 0.0, 16.0, 16.0, 16.0))

  // noinspection ScalaDeprecation
  override def getShape(state: BlockState, world: IBlockReader,
                        pos: BlockPos, context: ISelectionContext): VoxelShape = shape

  // noinspection ScalaDeprecation
  override def use(state: BlockState, world: World, pos: BlockPos,
                   player: PlayerEntity, hand: Hand, hit: BlockRayTraceResult): ActionResultType = world match {
    case world if world.isClientSide => ActionResultType.SUCCESS
    case _ => ActionResultType.CONSUME.tap(_ => player.openMenu(new ContainerProvider(world, pos)))
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
