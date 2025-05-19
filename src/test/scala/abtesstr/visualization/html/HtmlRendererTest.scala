package abtesstr.visualization.html

import abtesstr.ABModelDTO
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.funsuite.AnyFunSuiteLike

import java.time.Instant

class HtmlRendererTest extends AnyFreeSpec {

  "example" in {

    import java.nio.file._
    val html = HtmlRenderer.render(example)
    Files.writeString(Paths.get("abmodel-gantt.html"), html)
  }

  val example = ABModelDTO(
    version = "1",
    hashFunc = "sha256",
    experiments = List(
      // ───── search-ui ─────
      ABModelDTO.Experiment(
        testSpaceId = "search-ui",
        experimentId = "highlight-title",
        startIncl = Instant.parse("2024-06-01T00:00:00Z"),
        endExcl = Some(Instant.parse("2024-06-15T00:00:00Z")),
        bucketStart = 0,
        bucketEnd = Long.MaxValue
      ),
      ABModelDTO.Experiment(
        testSpaceId = "search-ui",
        experimentId = "highlight-title", // same ID, later version
        startIncl = Instant.parse("2024-06-16T00:00:00Z"),
        endExcl = Some(Instant.parse("2024-06-30T00:00:00Z")),
        bucketStart = 1000,
        bucketEnd = 2000
      ),
      ABModelDTO.Experiment(
        testSpaceId = "search-ui",
        experimentId = "bold-snippet",
        startIncl = Instant.parse("2024-06-05T00:00:00Z"),
        endExcl = Some(Instant.parse("2024-06-25T00:00:00Z")),
        bucketStart = 2000,
        bucketEnd = 3000
      ),

      // ───── checkout-flow ─────
      ABModelDTO.Experiment(
        testSpaceId = "checkout-flow",
        experimentId = "single-page-checkout",
        startIncl = Instant.parse("2024-06-01T00:00:00Z"),
        endExcl = Some(Instant.parse("2024-06-20T00:00:00Z")),
        bucketStart = 0,
        bucketEnd = 1500
      ),
      ABModelDTO.Experiment(
        testSpaceId = "checkout-flow",
        experimentId = "single-page-checkout", // same ID, new config
        startIncl = Instant.parse("2024-06-21T00:00:00Z"),
        endExcl = Some(Instant.parse("2024-07-10T00:00:00Z")),
        bucketStart = 1500,
        bucketEnd = 2500
      ),
      ABModelDTO.Experiment(
        testSpaceId = "checkout-flow",
        experimentId = "sticky-summary-box",
        startIncl = Instant.parse("2024-06-10T00:00:00Z"),
        endExcl = None, // open-ended
        bucketStart = 2500,
        bucketEnd = 3500
      )
    )
  )


}
