package abtesstr.visualization.html

import abtesstr.ABModelDTO
import java.time.Instant

object HtmlRenderer {
  private case class GanttTask(
      id: String,
      name: String,
      start: String,
      end: String,
      progress: Double,
      customClass: String,
  )

  private def preprocessExperiments(model: ABModelDTO): List[GanttTask] = {
    val LONG_MAX = BigInt("9223372036854775807")

    model.experiments.zipWithIndex.map { case (exp, idx) =>
      val progress = ((BigInt(exp.bucketEnd) - BigInt(exp.bucketStart)) * 100 / LONG_MAX).toDouble
      val endDate  = exp.endExcl.getOrElse(Instant.now().plusSeconds(5 * 24 * 60 * 60).toString)

      GanttTask(
        id = s"exp-$idx",
        name = s"${exp.testSpaceId}: ${exp.experimentId}${if (exp.endExcl.isEmpty) " (ongoing)" else ""}",
        start = exp.startIncl.toString,
        end = endDate.toString,
        progress = progress,
        customClass = if (exp.endExcl.isEmpty) "indefinite-task" else "",
      )
    }
  }

  def render(model: ABModelDTO): String = {
    val tasks     = preprocessExperiments(model)
    val tasksJson = tasks
      .map { task =>
        s"""{
           |  "id": "${escapeJsonString(task.id)}",
           |  "name": "${escapeJsonString(task.name)}",
           |  "start": "${task.start}",
           |  "end": "${task.end}",
           |  "progress": ${task.progress},
           |  "custom_class": "${escapeJsonString(task.customClass)}"
           |}""".stripMargin
      }
      .mkString("[", ",", "]")

    val html = {
      // language=html
      s"""<!DOCTYPE html>
         |<html>
         |<head>
         |  <meta charset="UTF-8">
         |  <title>ABModel Gantt</title>
         |  <script src="https://cdn.jsdelivr.net/npm/frappe-gantt@1.0.3/dist/frappe-gantt.umd.min.js"></script>
         |  <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/frappe-gantt@1.0.3/dist/frappe-gantt.min.css">
         |  <style>
         |    body { font-family: sans-serif; padding: 20px; }
         |    #gantt { border: 1px solid #ccc; overflow: auto; }
         |  </style>
         |</head>
         |<body>
         |  <h2>ABModel Experiment Timeline</h2>
         |  <div id="gantt"></div>
         |
         |  <script>
         |    const tasks = $tasksJson;
         |
         |    const LONG_MAX = BigInt("9223372036854775807");
         |
         |    new Gantt("#gantt", tasks, {
         |      view_mode: "Week",
         |      date_format: "YYYY-MM-DD",
         |      view_mode_select: true,
         |    });
         |
         |    document.querySelectorAll('.gantt-container').forEach(el => {
         |      const currentInlineHeight = el.style.height || window.getComputedStyle(el).height;
         |      const current = parseInt(currentInlineHeight);
         |      el.style.height = (current + 15) + 'px';
         |    });
         |  </script>
         |</body>
         |</html>""".stripMargin
    }
    html
  }

  private def escapeJsonString(str: String): String =
    str.flatMap {
      case '"'  => "\\\""
      case '\\' => "\\\\"
      case '/'  => "\\/" // optional
      case '\b' => "\\b"
      case '\f' => "\\f"
      case '\n' => "\\n"
      case '\r' => "\\r"
      case '\t' => "\\t"
      case c    => c.toString
    }

}
