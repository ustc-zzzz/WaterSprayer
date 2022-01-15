package com.github.ustc_zzzz.watersprayer.sprayer

import net.minecraft.entity.projectile.ProjectileHelper
import net.minecraft.entity.{Entity, LivingEntity}
import net.minecraft.util.math._

import scala.collection.mutable
import scala.util.chaining._

final class SprayerRayTracer(entity: LivingEntity) {

  case class Traced(motion: Vec3d, pos: Vec3d)

  private var hit: Option[RayTraceResult] = None

  private val traced: Seq[Traced] = {
    val startPos = entity.getPositionVec.add(0.0, entity.getEyeHeight * 0.6, 0.0)
    val startMotion = entity.getLookVec.add(0.0, 0.2, 0.0).normalize
    val stack = mutable.Stack(Traced(startMotion, startPos))
    val blockMode = RayTraceContext.BlockMode.COLLIDER
    val fluidMode = RayTraceContext.FluidMode.NONE
    var continueRayTracing = true
    while (continueRayTracing) {
      val Traced(motion, pos) = stack.head
      val newMotion = motion.subtract(0.0, 0.1, 0.0).pipe(m => if (m.lengthSquared <= 1) m else m.normalize())
      var newPos = pos.add(motion).add(newMotion)
      val blockResult = entity.world.rayTraceBlocks(new RayTraceContext(pos, newPos, blockMode, fluidMode, entity))
      if (blockResult.getType != RayTraceResult.Type.MISS) {
        hit = Some(blockResult)
        continueRayTracing = false
        newPos = blockResult.getHitVec
      }
      val bbox = new AxisAlignedBB(pos, newPos).grow(1.0D)
      val entityResult = ProjectileHelper.rayTraceEntities(entity.world, entity, pos, newPos, bbox, (e: Entity) => {
        !e.isSpectator && e.canBeCollidedWith && !e.isEntityEqual(entity)
      })
      if (entityResult != null) {
        hit = Some(entityResult)
        continueRayTracing = false
        newPos = entityResult.getEntity.getBoundingBox.grow(0.3F).rayTrace(pos, newPos).orElse(newPos)
      }
      stack.push(Traced(newMotion, newPos))
      if (newPos.y < 0) continueRayTracing = false
    }
    stack.toSeq
  }

  def hitEntity: Option[Entity] = hit match {
    case Some(result: EntityRayTraceResult) => Some(result.getEntity)
    case _ => None
  }

  def discretized(factor: Int): Seq[Vec3d] = {
    val head :: tail = traced
    var previousPos = head.pos
    var previousMotion = head.motion
    val builder = IndexedSeq.newBuilder[Vec3d]
    for (Traced(nextMotion, nextPos) <- tail) {
      val max = factor * nextPos.subtract(previousPos).length / nextMotion.add(previousMotion).length
      for (i <- max.floor.toInt to 1 by -1) {
        val (alpha, beta) = (i / max, (max - i) / max)
        val alphaPos = nextPos.add(nextMotion.scale(alpha * max / factor))
        val betaPos = previousPos.subtract(previousMotion.scale(beta * max / factor))
        builder += betaPos.scale(alpha).add(alphaPos.scale(beta)) // quadratic bezier curve
      }
      previousPos = nextPos
      previousMotion = nextMotion
    }
    builder += previousPos
    builder.result
  }
}
