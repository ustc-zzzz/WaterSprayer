package com.github.ustc_zzzz.watersprayer

import com.github.ustc_zzzz.watersprayer.sprayer.SprayerWaterHUDManager
import net.minecraft.block.{Block, Blocks}
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.ScreenManager
import net.minecraft.inventory.container.ContainerType
import net.minecraft.item.{BlockItem, Item, ItemGroup}
import net.minecraft.particles.ParticleType
import net.minecraftforge.api.distmarker.{Dist, OnlyIn}
import net.minecraftforge.client.event.ParticleFactoryRegisterEvent
import net.minecraftforge.event.RegistryEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.event.lifecycle.{FMLClientSetupEvent, FMLCommonSetupEvent}
import net.minecraftforge.fml.{DistExecutor, ModLoadingContext}
import net.minecraftforge.scorge.lang.ScorgeModLoadingContext
import org.apache.logging.log4j.LogManager

final class WaterSprayerMod {
  ScorgeModLoadingContext.get.getModEventBus.register {
    DistExecutor.runForDist[AnyRef](() => () => new AnyRef with CommonListener with ClientListener, () => () => new AnyRef with CommonListener)
  }

  trait CommonListener {
    @SubscribeEvent
    def onCommonSetup(e: FMLCommonSetupEvent): Unit = {
      SprayerWaterHUDManager.registerCommonEvents()
      val info = ModLoadingContext.get.getActiveContainer.getModInfo
      LogManager.getLogger(classOf[WaterSprayerMod]).info(s"Hello ${info.getDescription} (version ${info.getVersion})!")
    }

    @SubscribeEvent
    def onRegisterItem(e: RegistryEvent.Register[Item]): Unit = {
      e.getRegistry.register(sprayer.SprayerItem.setRegistryName("sprayer"))
      e.getRegistry.register(bottle.LavaBottleItem.setRegistryName("lava_bottle"))
      e.getRegistry.register(new BlockItem(workbench.WorkbenchBlock, new Item.Properties().group(ItemGroup.MISC)).setRegistryName("workbench"))
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
  trait ClientListener {
    @SubscribeEvent
    def onClientSetup(e: FMLClientSetupEvent): Unit = {
      SprayerWaterHUDManager.registerClientEvents()
      import workbench.{WorkbenchContainer => Container, WorkbenchContainerGui => Gui}
      ScreenManager.registerFactory[Container, Gui](workbench.WorkbenchContainerType, new Gui(_, _, _))
    }

    @SubscribeEvent
    def onRegisterParticleFactory(e: ParticleFactoryRegisterEvent): Unit = {
      import sprayer.SprayerParticles._
      Minecraft.getInstance.particles.registerFactory(HoneyType, new Factory(Blocks.HONEY_BLOCK.getDefaultState))
      Minecraft.getInstance.particles.registerFactory(LavaFluidType, new Factory(Blocks.MAGMA_BLOCK.getDefaultState))
      Minecraft.getInstance.particles.registerFactory(WaterFluidType, new Factory(Blocks.PRISMARINE_BRICKS.getDefaultState))
    }
  }
}
