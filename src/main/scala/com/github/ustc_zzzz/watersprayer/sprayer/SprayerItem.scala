package com.github.ustc_zzzz.watersprayer.sprayer

import net.minecraft.client.util.ITooltipFlag
import net.minecraft.entity.monster.{BlazeEntity, EndermanEntity}
import net.minecraft.entity.passive.SnowGolemEntity
import net.minecraft.entity.player.{PlayerEntity, ServerPlayerEntity}
import net.minecraft.entity.{Entity, LivingEntity}
import net.minecraft.item.{IItemPropertyGetter, Item, ItemGroup, ItemStack}
import net.minecraft.particles.IParticleData
import net.minecraft.potion.Effects
import net.minecraft.util.text.{ITextComponent, TextFormatting, TranslationTextComponent}
import net.minecraft.util.{ActionResult, Hand, IndirectEntityDamageSource, NonNullList, ResourceLocation}
import net.minecraft.world.World
import net.minecraft.world.server.ServerWorld
import net.minecraftforge.api.distmarker.{Dist, OnlyIn}
import net.minecraftforge.common.util.Constants

import scala.util.chaining._

object SprayerItem extends Item(new Item.Properties().group(ItemGroup.MISC).maxStackSize(1)) {
  // noinspection ConvertExpressionToSAM
  addPropertyOverride(new ResourceLocation("honey"), new IItemPropertyGetter {
    @OnlyIn(Dist.CLIENT)
    override def call(stack: ItemStack, world: World, entity: LivingEntity): Float = {
      if (FluidType.get(stack) == SprayerParticles.HoneyType && Amount.get(stack) > 0) 1.0F else 0.0F
    }
  })

  // noinspection ConvertExpressionToSAM
  addPropertyOverride(new ResourceLocation("lava_fluid"), new IItemPropertyGetter {
    @OnlyIn(Dist.CLIENT)
    override def call(stack: ItemStack, world: World, entity: LivingEntity): Float = {
      if (FluidType.get(stack) == SprayerParticles.LavaFluidType && Amount.get(stack) > 0) 1.0F else 0.0F
    }
  })

  // noinspection ConvertExpressionToSAM
  addPropertyOverride(new ResourceLocation("water_fluid"), new IItemPropertyGetter {
    @OnlyIn(Dist.CLIENT)
    override def call(stack: ItemStack, world: World, entity: LivingEntity): Float = {
      if (FluidType.get(stack) == SprayerParticles.WaterFluidType && Amount.get(stack) > 0) 1.0F else 0.0F
    }
  })

  @OnlyIn(Dist.CLIENT)
  override def addInformation(stack: ItemStack, world: World, tooltip: java.util.List[ITextComponent], flag: ITooltipFlag): Unit = {
    val text = new TranslationTextComponent("item.watersprayer.sprayer.tooltip", Amount.get(stack), 3_600)
    tooltip.add(text.applyTextStyle(TextFormatting.GRAY))
    super.addInformation(stack, world, tooltip, flag)
  }

  override def getUseDuration(stack: ItemStack): Int = 7_200

  override def showDurabilityBar(stack: ItemStack): Boolean = true

  override def getDurabilityForDisplay(stack: ItemStack): Double = 1.0 - Amount.get(stack) / 3_600.0

  override def getTranslationKey(stack: ItemStack): String = FluidType.get(stack) match {
    case _ if Amount.get(stack) <= 0 => super.getTranslationKey
    case SprayerParticles.HoneyType => super.getTranslationKey + ".honey"
    case SprayerParticles.LavaFluidType => super.getTranslationKey + ".lava_fluid"
    case SprayerParticles.WaterFluidType => super.getTranslationKey + ".water_fluid"
  }

  override def fillItemGroup(group: ItemGroup, items: NonNullList[ItemStack]): Unit = if (isInGroup(group)) {
    items.add(new ItemStack(SprayerItem).tap(FluidType.set(_, SprayerParticles.WaterFluidType)))
    items.add(new ItemStack(SprayerItem).tap(FluidType.set(_, SprayerParticles.LavaFluidType)))
    items.add(new ItemStack(SprayerItem).tap(FluidType.set(_, SprayerParticles.HoneyType)))
    items.add(new ItemStack(SprayerItem).tap(Amount.set(_, 0)))
  }

  override def onPlayerStoppedUsing(stack: ItemStack, world: World, player: LivingEntity, time: Int): Unit = {
    onUsingTick(stack, player, time) // same behavior
  }

  override def onUsingTick(stack: ItemStack, player: LivingEntity, time: Int): Unit = {
    val amount = Amount.get(stack)
    if (!player.world.isRemote && amount > 0) {
      val tracer = new SprayerRayTracer(player)
      val fluidType = FluidType.get(stack)
      Amount.set(stack, amount - 1)
      time % 2 match {
        case 1 => addParticles(player, tracer, fluidType)
        case 0 => tracer.hitEntity.foreach(handleHit(player, _, fluidType))
      }
    }
  }

  override def onItemRightClick(world: World, player: PlayerEntity, hand: Hand): ActionResult[ItemStack] = {
    ActionResult.resultConsume(player.tap(_.setActiveHand(hand)).getHeldItem(hand))
  }

  private def addParticles(player: LivingEntity, tracer: SprayerRayTracer, data: IParticleData): Unit = {
    val offsetX = player.getUpVector(1.0F).crossProduct(player.getLookVec)
    val offsetFactor = if (player.getActiveHand == Hand.MAIN_HAND) -0.4 else 0.4
    val world = player.world.asInstanceOf[ServerWorld]
    val discretized = tracer.discretized(64)
    val discretizedSize = discretized.length
    for (index <- 0 until discretizedSize) {
      val pos = discretized(index).add(offsetX.scale(index * offsetFactor / discretizedSize))
      world.getPlayers.forEach(p => world.spawnParticle(p, data, true, pos.x, pos.y, pos.z, 1, 0.0, 0.0, 0.0, 0.0))
    }
  }

  private def handleHit(player: LivingEntity, target: Entity, data: IParticleData): Unit = data match {
    case SprayerParticles.WaterFluidType => target.tap(_.extinguish) match {
      case target: ServerPlayerEntity if !target.isSpectator => SprayerWaterHUDManager.notify(target)
      case WaterScary(target) => damage(player, target, source = "drown", amount = 0.4F)
      case _ => ()
    }
    case SprayerParticles.LavaFluidType => target.tap(_.setFire(3)) match {
      case target if !target.isImmuneToFire => damage(player, target, source = "lava", amount = 0.8F)
      case _ => ()
    }
    case SprayerParticles.HoneyType => target match {
      case target: LivingEntity => target.tap(_.removePotionEffect(Effects.POISON)).heal(0.2F)
      case _ => ()
    }
  }

  private def damage(player: LivingEntity, target: Entity, source: String, amount: Float): Unit = {
    target.attackEntityFrom(new IndirectEntityDamageSource(source, player, player), amount)
  }

  private object WaterScary {
    def unapply(arg: Entity): Option[LivingEntity] = arg match {
      case e: SnowGolemEntity => Some(e)
      case e: EndermanEntity => Some(e)
      case e: BlazeEntity => Some(e)
      case _ => None
    }
  }

  object FluidType {
    private final val key = "FluidType"

    def get(stack: ItemStack): SprayerParticles.Type = Option(stack.getTag).map(_.getString(key)) match {
      case Some("Lava") => SprayerParticles.LavaFluidType
      case Some("Honey") => SprayerParticles.HoneyType
      case _ => SprayerParticles.WaterFluidType
    }

    def set(stack: ItemStack, particleType: SprayerParticles.Type): Unit = particleType match {
      case SprayerParticles.LavaFluidType => stack.getOrCreateTag.putString(key, "Lava")
      case SprayerParticles.HoneyType => stack.getOrCreateTag.putString(key, "Honey")
      case _ => stack.getOrCreateTag.putString(key, "Water")
    }
  }

  object Amount {
    private final val key = "Amount"

    def has(stack: ItemStack): Boolean = Option(stack.getTag).exists(_.contains(key, Constants.NBT.TAG_ANY_NUMERIC))

    def get(stack: ItemStack): Int = if (has(stack)) stack.getTag.getInt(key).min(3_600).max(0) else 3_600

    def set(stack: ItemStack, int: Int): Unit = stack.getOrCreateTag.putInt(key, int.min(3_600).max(0))
  }
}
