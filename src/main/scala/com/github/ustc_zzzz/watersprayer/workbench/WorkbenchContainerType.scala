package com.github.ustc_zzzz.watersprayer.workbench

import net.minecraft.inventory.container.ContainerType

object WorkbenchContainerType extends ContainerType[WorkbenchContainer](new WorkbenchContainer(_, _, None))
