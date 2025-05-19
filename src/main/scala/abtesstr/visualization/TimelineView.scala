package abtesstr.visualization

import abtesstr.{ABModelDTO, SpaceSize}

import java.time.Instant

/** A data structure more suitable for visualization than ABModelDTO. It groups experiments by space ID and sums up bucket ranges for the same
  * experiment ID.
  */
case class TimelineView(spaces: Map[String, TimelineView.Space])

object TimelineView {
  case class Space(id: String, experiments: List[Experiment])

  case class Experiment(id: String, startIncl: Instant, endExcl: Option[Instant], spacePercentage: Double)

  def fromDTO(model: ABModelDTO): TimelineView = TimelineView(
    model.experiments
      .groupBy(_.testSpaceId)
      .map { case (spaceId, exps) =>
        val experiments = exps
          .groupBy(_.experimentId)
          .flatMap { case (expId, versions) =>
            val sorted   = versions.sortBy(_.startIncl)
            val times    = sorted.flatMap(e => Seq(e.startIncl) ++ e.endExcl).distinct.sorted
            val segments = times.sliding(2).collect { case Seq(s, e) => (s, Some(e)) }.toSeq
            val allSegs  = if (sorted.exists(_.endExcl.isEmpty) && times.nonEmpty) segments :+ (times.last, None) else segments

            allSegs.flatMap { case (start, end) =>
              val segEnd = end.getOrElse(Instant.MAX)
              val active = sorted.filter { v =>
                v.startIncl.isBefore(segEnd) && v.endExcl.getOrElse(Instant.MAX).isAfter(start)
              }
              if (active.nonEmpty) {
                val totalRange = active.map(v => v.bucketEnd - v.bucketStart).sum
                Some(Experiment(expId, start, end, totalRange.toDouble * 100 / SpaceSize.toDouble))
              } else None
            }
          }
          .toList
        spaceId -> Space(spaceId, experiments)
      },
  )
}
