package abtesstr.visualization.mermaid

import abtesstr.ABModelDTO
import abtesstr.ABModelDTO.Experiment
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.funsuite.AnyFunSuiteLike

import java.time.Instant

class MermaidRendererTest extends AnyFreeSpec {

  "render example" in {
    val dto = ABModelDTO(
      version = "1",
      hashFunc = "sha256",
      experiments = List(
        Experiment(
          testSpaceId = "search-ui",
          experimentId = "highlight-title",
          startIncl = Instant.parse("2024-06-01T00:00:00Z"),
          endExcl = Some(Instant.parse("2024-06-20T00:00:00Z")),
          bucketStart = 0,
          bucketEnd = 1000
        ),
        Experiment(
          testSpaceId = "search-ui",
          experimentId = "bold-snippet-v1",
          startIncl = Instant.parse("2024-06-10T00:00:00Z"),
          endExcl = Some(Instant.parse("2024-06-20T00:00:00Z")),
          bucketStart = 1000,
          bucketEnd = 2000
        ),
        Experiment(
          testSpaceId = "search-ui",
          experimentId = "bold-snippet-v2",
          startIncl = Instant.parse("2024-06-20T00:00:00Z"),
          endExcl = Some(Instant.parse("2024-06-30T00:00:00Z")),
          bucketStart = 1000,
          bucketEnd = 1500
        ),
        Experiment(
          testSpaceId = "search-ui",
          experimentId = "ad-positioning",
          startIncl = Instant.parse("2024-06-15T00:00:00Z"),
          endExcl = None,
          bucketStart = 2000,
          bucketEnd = 3000
        ),
        Experiment(
          testSpaceId = "checkout-flow",
          experimentId = "single-page-checkout",
          startIncl = Instant.parse("2024-06-05T00:00:00Z"),
          endExcl = Some(Instant.parse("2024-06-25T00:00:00Z")),
          bucketStart = 0,
          bucketEnd = 1000
        ),
        Experiment(
          testSpaceId = "checkout-flow",
          experimentId = "express-button",
          startIncl = Instant.parse("2024-06-18T00:00:00Z"),
          endExcl = None,
          bucketStart = 1000,
          bucketEnd = 1500
        )
      )
    )

    println(MermaidRenderer.render(dto))
  }

}
