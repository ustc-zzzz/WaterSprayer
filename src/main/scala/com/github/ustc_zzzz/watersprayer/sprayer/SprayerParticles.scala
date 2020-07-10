package com.github.ustc_zzzz.watersprayer.sprayer

import net.minecraft.block.BlockState
import net.minecraft.client.Minecraft
import net.minecraft.client.particle.{BreakingParticle, IParticleFactory, Particle}
import net.minecraft.client.renderer.model.ModelManager
import net.minecraft.item.ItemStack
import net.minecraft.particles.BasicParticleType
import net.minecraft.world.World
import net.minecraftforge.api.distmarker.{Dist, OnlyIn}
import net.minecraftforge.client.model.data.EmptyModelData

object SprayerParticles {
  object HoneyType extends Type

  object LavaFluidType extends Type

  object WaterFluidType extends Type

  sealed class Type extends BasicParticleType(false)

  @OnlyIn(Dist.CLIENT)
  class Factory(state: BlockState) extends IParticleFactory[BasicParticleType] {
    private val modelManager: ModelManager = Minecraft.getInstance.getModelManager

    override def makeParticle(particleType: BasicParticleType, world: World,
                              x: Double, y: Double, z: Double, dx: Double, dy: Double, dz: Double): Particle = {
      new BreakingParticle(world, x, y, z, ItemStack.EMPTY) {
        setSprite(modelManager.getBlockModelShapes.getModel(state).getParticleTexture(EmptyModelData.INSTANCE))
        motionX = 0.01 * math.random() - 0.01 * math.random()
        motionY = 0.01 * math.random() - 0.01 * math.random()
        motionZ = 0.01 * math.random() - 0.01 * math.random()
        maxAge = (3.0 / (0.5 + math.random())).toInt
        multipleParticleScaleBy(0.2F)
        particleGravity = 0.01F
      }
    }
  }
}
