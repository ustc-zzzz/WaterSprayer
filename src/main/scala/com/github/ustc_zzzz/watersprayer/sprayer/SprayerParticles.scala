package com.github.ustc_zzzz.watersprayer.sprayer

import com.github.ustc_zzzz.watersprayer.sprayer.SprayerItem.{Amount, FluidType}
import net.minecraft.block.BlockState
import net.minecraft.client.Minecraft
import net.minecraft.client.particle.{BreakingParticle, IParticleFactory, Particle}
import net.minecraft.client.renderer.model.ModelManager
import net.minecraft.client.world.ClientWorld
import net.minecraft.entity.LivingEntity
import net.minecraft.item.{IItemPropertyGetter, ItemStack}
import net.minecraft.particles.BasicParticleType
import net.minecraftforge.api.distmarker.{Dist, OnlyIn}
import net.minecraftforge.client.model.data.EmptyModelData

object SprayerParticles {
  object HoneyType extends Type

  object LavaFluidType extends Type

  object WaterFluidType extends Type

  sealed class Type extends BasicParticleType(false)

  @OnlyIn(Dist.CLIENT)
  class Getter(particleType: Type) extends IItemPropertyGetter {
    override def call(stack : ItemStack, world : ClientWorld, entity : LivingEntity): Float = {
      if (FluidType.get(stack) == particleType && Amount.get(stack) > 0) 1.0F else 0.0F
    }
  }

  @OnlyIn(Dist.CLIENT)
  class Factory(state: BlockState) extends IParticleFactory[BasicParticleType] {
    private val modelManager: ModelManager = Minecraft.getInstance.getModelManager

    override def createParticle(particleType: BasicParticleType, world: ClientWorld,
                                x: Double, y: Double, z: Double, dx: Double, dy: Double, dz: Double): Particle = {
      new BreakingParticle(world, x, y, z, ItemStack.EMPTY) {
        setSprite(modelManager.getBlockModelShaper.getBlockModel(state).getParticleTexture(EmptyModelData.INSTANCE))
        xd = 0.01 * math.random() - 0.01 * math.random()
        yd = 0.01 * math.random() - 0.01 * math.random()
        zd = 0.01 * math.random() - 0.01 * math.random()
        age = (3.0 / (0.5 + math.random())).toInt
        quadSize *= 0.2F
        gravity = 0.01F
      }
    }
  }
}
