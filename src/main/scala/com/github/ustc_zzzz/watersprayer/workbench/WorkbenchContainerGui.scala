package com.github.ustc_zzzz.watersprayer.workbench

import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.gui.screen.inventory.ContainerScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.util.ResourceLocation
import net.minecraft.util.text.ITextComponent

import scala.util.chaining._

final class WorkbenchContainerGui(container: WorkbenchContainer, inv: PlayerInventory, name: ITextComponent) extends ContainerScreen[WorkbenchContainer](container, inv, name) { gui =>
  private val texture: ResourceLocation = new ResourceLocation("watersprayer:textures/gui/container/workbench.png")

  override def render(mouseX: Int, mouseY: Int, partialTicks: Float): Unit = {
    gui.tap(_.renderBackground).tap(_ => super.render(mouseX, mouseY, partialTicks)).renderHoveredToolTip(mouseX, mouseY)
  }

  override def drawGuiContainerForegroundLayer(mouseX: Int, mouseY: Int): Unit = {
    title.getFormattedText.pipe(text => font.drawString(text, (xSize - font.getStringWidth(text)) / 2.0F, 6.0F, 0x404040))
  }

  override def drawGuiContainerBackgroundLayer(partialTicks: Float, mouseX: Int, mouseY: Int): Unit = {
    RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F)
    minecraft.getTextureManager.bindTexture(texture)
    blit(guiLeft, (height - ySize) / 2, 0, 0, xSize, ySize)
  }
}
