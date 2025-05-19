package abtesstr.visualization.mermaid

import java.time.format.DateTimeFormatter
import java.time.{Instant, ZoneOffset}
import abtesstr.{ABModelDTO, SpaceSize}

object MermaidRenderer {

  private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC)

  private def formatPercent(start: Long, end: Long): String = {
    val percent = ((end - start).toDouble / Long.MaxValue) * 100
    f"${percent}%.4f%%"
  }

  private def formatDate(i: Instant): String = formatter.format(i)

  def render(dto: ABModelDTO): String = {
    val experimentsBySpace = dto.experiments.groupBy(_.testSpaceId)

    val lines = scala.collection.mutable.ListBuffer[String]()
    lines += "gantt"
    lines += "    title A/B Experiments Timeline with Bucket Usage"
    lines += "    dateFormat  YYYY-MM-DD"
    lines += "    axisFormat  %b %d"

    for ((spaceId, exps) <- experimentsBySpace) {
      lines += s""
      lines += s"    section $spaceId"
      for (exp <- exps.sortBy(_.startIncl)) {
        val percent = formatPercent(exp.bucketStart, exp.bucketEnd)
        val label = s"${exp.experimentId} ($percent)"
        val taskId = s"${spaceId}_${exp.experimentId}".replaceAll("[^a-zA-Z0-9_]", "_")
        val start = formatDate(exp.startIncl)
        val end = exp.endExcl.map(formatDate).getOrElse("3d")
        lines += s"    $label :$taskId, $start, $end"
      }
    }

    lines.mkString("\n")
  }
}
