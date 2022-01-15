package com.github.ustc_zzzz.watersprayer.workbench

import com.mojang.blaze3d.matrix.MatrixStack
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.gui.AbstractGui
import net.minecraft.client.gui.screen.inventory.ContainerScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.util.ResourceLocation
import net.minecraft.util.text.ITextComponent

final class WorkbenchContainerGui(container: WorkbenchContainer, inv: PlayerInventory, name: ITextComponent) extends ContainerScreen[WorkbenchContainer](container, inv, name) {
  workbench =>
  private val texture: ResourceLocation = new ResourceLocation("watersprayer:textures/gui/container/workbench.png")

  override def render(stack: MatrixStack, mouseX: Int, mouseY: Int, partialTicks: Float): Unit = {
    workbench.renderBackground(stack)
    super.render(stack, mouseX, mouseY, partialTicks)
    RenderSystem.disableBlend()
    workbench.renderFg(stack)
    workbench.renderTooltip(stack, mouseX, mouseY)
  }

  override def renderBg(stack: MatrixStack, partialTicks: Float, mouseX: Int, mouseY: Int): Unit = {
    // noinspection ScalaDeprecation
    RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F)
    minecraft.getTextureManager.bind(texture)
    blit(stack, (width - imageWidth) / 2, (height - imageHeight) / 2, 0, 0, imageWidth, imageHeight)
  }

  private def renderFg(stack: MatrixStack): Unit = {
    AbstractGui.drawCenteredString(stack, font, title, width / 2, 6, 0x404040)
  }
}
