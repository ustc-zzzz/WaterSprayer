package com.github.ustc_zzzz.watersprayer.sprayer

import com.github.ustc_zzzz.watersprayer.WaterSprayerMod
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.client.renderer.{Tessellator, WorldVertexBufferUploader}
import net.minecraft.entity.player.ServerPlayerEntity
import net.minecraft.network.PacketBuffer
import net.minecraft.util.ResourceLocation
import net.minecraftforge.api.distmarker.{Dist, OnlyIn}
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.TickEvent
import net.minecraftforge.fml.network.simple.SimpleChannel
import net.minecraftforge.fml.network.{NetworkEvent, NetworkRegistry, PacketDistributor}

import scala.util.chaining._

object SprayerWaterHUDManager {
  private val name: ResourceLocation = new ResourceLocation("watersprayer:water_hud")

  private val texture: ResourceLocation = new ResourceLocation("textures/misc/underwater.png")

  private val tickLeft: java.util.concurrent.atomic.AtomicInteger = new java.util.concurrent.atomic.AtomicInteger(0)

  private val channel: SimpleChannel = NetworkRegistry.newSimpleChannel(name, () => "1", _ == "1", _ == "1").tap { c =>
    c.registerMessage[Msg.type](0, Msg.runtimeClass, Msg.encode, Msg.decode, Msg.callback).asInstanceOf[AnyRef]
  }

  // noinspection ScalaUnusedSymbol
  object Msg {
    def decode(packet: PacketBuffer): Msg.type = Msg

    def encode(msg: Msg.type, packet: PacketBuffer): Unit = ()

    def runtimeClass: Class[Msg.type] = Msg.getClass.asInstanceOf[Class[Msg.type]]

    def callback(msg: Msg.type, ctx: java.util.function.Supplier[NetworkEvent.Context]): Unit = tickLeft.set(25)
  }

  def notify(player: ServerPlayerEntity): Unit = channel.send(PacketDistributor.PLAYER.`with`(() => player), Msg)

  def registerCommonEvents(): Unit = ()

  @OnlyIn(Dist.CLIENT)
  def registerClientEvents(): Unit = {
    MinecraftForge.EVENT_BUS.addListener((e: TickEvent.ClientTickEvent) => if (e.phase == TickEvent.Phase.END) {
      var local = tickLeft.get()
      if (local > 0) WaterSprayerMod.logger.info("Local tick left: " + local)
      while (!tickLeft.compareAndSet(local, 0.max(local - 1))) local = tickLeft.get()
    })

    MinecraftForge.EVENT_BUS.addListener((e: RenderWorldLastEvent) => {
      val t = tickLeft.get()
      if (t > 0) {
        RenderSystem.enableBlend()
        RenderSystem.defaultBlendFunc()
        try {
          val mc = Minecraft.getInstance
          val c = mc.player.getBrightness
          val u = -mc.player.yRot / 64.0F
          val v = mc.player.xRot / 64.0F
          val a = (t - e.getPartialTicks).min(24.0F) / 64.0F
          val m = e.getMatrixStack.last.pose.tap(_.setIdentity)
          WorldVertexBufferUploader end Tessellator.getInstance.getBuilder.tap { builder =>
            mc.getTextureManager.bind(texture)
            builder.begin(7, DefaultVertexFormats.POSITION_COLOR_TEX)
            builder.vertex(m, -1.0F, -1.0F, -0.5F).color(c, c, c, a).uv(4.0F + u, 4.0F + v).endVertex()
            builder.vertex(m, 1.0F, -1.0F, -0.5F).color(c, c, c, a).uv(0.0F + u, 4.0F + v).endVertex()
            builder.vertex(m, 1.0F, 1.0F, -0.5F).color(c, c, c, a).uv(0.0F + u, 0.0F + v).endVertex()
            builder.vertex(m, -1.0F, 1.0F, -0.5F).color(c, c, c, a).uv(4.0F + u, 0.0F + v).endVertex()
            builder.end()
          }
        } finally {
          RenderSystem.disableBlend()
        }
      }
    })
  }
}
