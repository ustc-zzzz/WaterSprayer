package com.github.ustc_zzzz.watersprayer

import net.minecraft.block.{Block, Blocks}
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.ScreenManager
import net.minecraft.inventory.container.ContainerType
import net.minecraft.item.{BlockItem, Item, ItemGroup, ItemModelsProperties}
import net.minecraft.particles.ParticleType
import net.minecraft.util.ResourceLocation
import net.minecraftforge.api.distmarker.{Dist, OnlyIn}
import net.minecraftforge.client.event.ParticleFactoryRegisterEvent
import net.minecraftforge.event.RegistryEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.event.lifecycle.{FMLClientSetupEvent, FMLCommonSetupEvent}
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext
import net.minecraftforge.fml.{DistExecutor, ModLoadingContext}
import org.apache.logging.log4j.{LogManager, Logger}

@Mod(WaterSprayerMod.id)
object WaterSprayerMod {
  final val id = "watersprayer"
  final val logger: Logger = LogManager.getLogger("WaterSprayer")

  FMLJavaModLoadingContext.get.getModEventBus.register(DistExecutor.safeRunForDist[AnyRef](
    () => () => new AnyRef with CommonListener with ClientListener, () => () => new AnyRef with CommonListener))

  // noinspection ScalaUnusedSymbol
  trait CommonListener {
    @SubscribeEvent
    def onCommonSetup(e: FMLCommonSetupEvent): Unit = {
      sprayer.SprayerWaterHUDManager.registerCommonEvents()
      val info = ModLoadingContext.get.getActiveContainer.getModInfo
      logger.info(s"Hello ${info.getDescription} (version ${info.getVersion})!")
    }

    @SubscribeEvent
    def onRegisterItem(e: RegistryEvent.Register[Item]): Unit = {
      e.getRegistry.register(sprayer.SprayerItem.setRegistryName("sprayer"))
      e.getRegistry.register(bottle.LavaBottleItem.setRegistryName("lava_bottle"))
      e.getRegistry.register(new BlockItem(workbench.WorkbenchBlock,
        new Item.Properties().tab(ItemGroup.TAB_MISC)).setRegistryName("workbench"))
    }

    @SubscribeEvent
    def onRegisterBlock(e: RegistryEvent.Register[Block]): Unit = {
      e.getRegistry.register(workbench.WorkbenchBlock.setRegistryName("workbench"))
    }

    @SubscribeEvent
    def onRegisterParticleType(e: RegistryEvent.Register[ParticleType[_]]): Unit = {
      e.getRegistry.register(sprayer.SprayerParticles.HoneyType.setRegistryName("honey"))
      e.getRegistry.register(sprayer.SprayerParticles.LavaFluidType.setRegistryName("lava_fluid"))
      e.getRegistry.register(sprayer.SprayerParticles.WaterFluidType.setRegistryName("water_fluid"))
    }

    @SubscribeEvent
    def onRegisterContainerType(e: RegistryEvent.Register[ContainerType[_]]): Unit = {
      e.getRegistry.register(workbench.WorkbenchContainerType.setRegistryName("workbench"))
    }
  }

  @OnlyIn(Dist.CLIENT)
  // noinspection ScalaUnusedSymbol
  trait ClientListener {
    @SubscribeEvent
    def onClientSetup(e: FMLClientSetupEvent): Unit = {
      sprayer.SprayerWaterHUDManager.registerClientEvents()
      import workbench.{WorkbenchContainer => Container, WorkbenchContainerGui => Gui}
      ScreenManager.register[Container, Gui](workbench.WorkbenchContainerType, new Gui(_, _, _))
      e.enqueueWork[Unit](() => {
        val item = sprayer.SprayerItem
        import sprayer.SprayerParticles._
        ItemModelsProperties.register(item, new ResourceLocation("honey"), new Getter(HoneyType))
        ItemModelsProperties.register(item, new ResourceLocation("lava_fluid"), new Getter(LavaFluidType))
        ItemModelsProperties.register(item, new ResourceLocation("water_fluid"), new Getter(WaterFluidType))
      })
    }

    @SubscribeEvent
    def onRegisterParticleFactory(e: ParticleFactoryRegisterEvent): Unit = {
      import sprayer.SprayerParticles._
      val engine = Minecraft.getInstance.particleEngine
      engine.register(HoneyType, new Factory(Blocks.HONEY_BLOCK.defaultBlockState))
      engine.register(LavaFluidType, new Factory(Blocks.MAGMA_BLOCK.defaultBlockState))
      engine.register(WaterFluidType, new Factory(Blocks.PRISMARINE_BRICKS.defaultBlockState))
    }
  }
}
