package abtesstr.visualization

import abtesstr.ABModelDTO
import java.time.Instant

/**
 * A data structure more suitable for visualization than ABModelDTO.
 * It groups experiments by space ID and sums up bucket ranges for the same experiment ID.
 */
case class TimelineView(
    spaces: Map[String, TimelineView.Space]
)

object TimelineView {

  case class Space(
      id: String,
      experiments: List[Experiment]
  )

  case class Experiment(
      id: String,
      start: Instant,
      end: Option[Instant],
      spacePercentage: Double
  )

  def fromABModelDTO(model: ABModelDTO): TimelineView = {
    val experimentsBySpace = model.experiments.groupBy(_.testSpaceId)
    val spaces = experimentsBySpace.map { case (spaceId, exps) =>
      val experimentsByIdInSpace = exps.groupBy(_.experimentId)
      val processedExperiments = experimentsByIdInSpace.flatMap { case (expId, expVersions) =>
        // Sort versions by start time
        val sortedVersions = expVersions.sortBy(_.start)

        // Create time segments where the set of active experiments changes
        val timePoints = (sortedVersions.map(_.start) ++ sortedVersions.flatMap(_.end)).distinct.sorted

        // Handle open-ended experiments
        val hasOpenEnded = sortedVersions.exists(_.end.isEmpty)

        // Create pairs of time points for segments
        val timeSegmentPairs = if (timePoints.size <= 1) {
          if (timePoints.isEmpty) {
            Seq.empty[(Instant, Option[Instant])]
          } else if (hasOpenEnded) {
            // Single time point with open-ended experiment
            Seq((timePoints.head, None))
          } else {
            Seq.empty[(Instant, Option[Instant])]
          }
        } else {
          // Create pairs of consecutive time points
          timePoints.sliding(2).map {
            case Seq(start, end) => (start, Some(end))
          }.toSeq
        }

        // Add an open-ended segment if needed
        val allSegments = if (hasOpenEnded && timePoints.nonEmpty) {
          timeSegmentPairs :+ (timePoints.last, None)
        } else {
          timeSegmentPairs
        }

        allSegments.flatMap { case (segmentStart, segmentEndOpt) =>
          // Find experiments active during this segment
          val activeExperiments = sortedVersions.filter { exp =>
            val isAfterStart = exp.start.compareTo(segmentStart) <= 0 || 
                              (segmentEndOpt.isDefined && exp.start.compareTo(segmentEndOpt.get) < 0)

            val isBeforeEnd = exp.end.isEmpty || 
                             (segmentEndOpt.isEmpty) || 
                             (exp.end.isDefined && exp.end.get.compareTo(segmentStart) > 0)

            isAfterStart && isBeforeEnd
          }

          if (activeExperiments.nonEmpty) {
            // Sum up the bucket ranges for active experiments in this segment
            val totalBucketSize = activeExperiments.map { exp => 
              BigInt(exp.bucketEnd) - BigInt(exp.bucketStart)
            }.sum

            // Calculate the percentage of space used
            val spacePercentage = (totalBucketSize.toDouble * 100.0 / BigInt(Long.MaxValue).toDouble)

            // Create the Experiment object for this segment
            Some(Experiment(
              id = expId,
              start = segmentStart,
              end = segmentEndOpt,
              spacePercentage = spacePercentage
            ))
          } else {
            None
          }
        }
      }.toList

      // Create the Space object
      spaceId -> Space(
        id = spaceId,
        experiments = processedExperiments
      )
    }

    // Create the TimelineView
    TimelineView(spaces = spaces)
  }
}
