package abtesstr

import java.time.Instant

case class ABModelDTO(
    version: String,
    hashFunc: "sha256" | "md5" | "murmur3",
    experiments: List[ABModelDTO.Experiment],
)

object ABModelDTO {

  case class Experiment(
      testSpaceId: String,
      experimentId: String,
      startIncl: Instant,
      endExcl: Option[Instant],
      bucketStart: Long,
      bucketEnd: Long,
  )
}
