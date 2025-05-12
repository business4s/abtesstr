package abtesstr.internal

import abtesstr.*

import java.time.{Clock, Instant}

case class ABModelImpl(spaces: Map[TestSpaceId, TestSpace], clock: Clock, hasher: Hasher) extends ABModel {
  private def now: Instant = clock.instant()

  override def addTestSpace(id: TestSpaceId): ABModel = {
    if (spaces.contains(id)) this
    else this.copy(spaces = spaces.updated(id, TestSpace(id, List())))
  }

  override def removeTestSpace(id: TestSpaceId): ABModel = {
    // TODO error if started experiments exist
    ???
  }

  override def listTestSpaces(): List[TestSpaceId] = spaces.keys.toList

  override def availableSpace(testSpaceId: TestSpaceId): SpaceFraction = {
    spaces
      .get(testSpaceId)
      .map(x => x.availableSpace(now))
      .getOrElse(???) // TODO error is testspace doesnt exist
  }

  override def addExperiment(
      testSpaceId: TestSpaceId,
      experimentId: ExperimentId,
      size: SpaceFraction,
      start: Option[Instant],
      end: Option[Instant],
  ): ABModel = {
    val sizeSpaceFragments = SpaceFraction(size)
    val period             = TimePeriod(start.getOrElse(now), end)
    spaces.get(testSpaceId).toList.flatMap(_.findFit(sizeSpaceFragments, now)) match {
      case List()  => throw new IllegalArgumentException("Not enough contiguous space for experiment")
      case buckets =>
        val newExps = buckets.map(SpaceFragment => Experiment(experimentId, period, SpaceFragment))
        val updated = spaces.map {
          case (nsId, ns) if nsId == testSpaceId => nsId -> ns.copy(experiments = ns.experiments ++ newExps)
          case ns                                => ns
        }
        this.copy(spaces = updated)
    }
  }

  override def finishExperiment(testSpaceId: TestSpaceId, experimentId: ExperimentId, at: Option[Instant]): ABModel = {
    val finishTime = at.getOrElse(now)
    val updated    = spaces.updatedWith(testSpaceId)(
      _.map(ns =>
        ns.copy(experiments = ns.experiments.map {
          case e if e.experimentId == experimentId && e.period.isActive(finishTime) => e.copy(period = e.period.copy(end = Some(finishTime)))
          case e                                                                    => e // TODO error
        }),
      ),
    )
    this.copy(spaces = updated)
  }

  override def trim(before: Option[Instant]): ABModel = {
    val cutoff  = before.getOrElse(now)
    val updated = spaces.view.mapValues { ns => ns.copy(experiments = ns.experiments.filter(e => e.period.end.forall(_.isAfter(cutoff)))) }.toMap
    this.copy(spaces = updated)
  }

  override def changeSize(
      testSpaceId: TestSpaceId,
      experimentId: ExperimentId,
      size: SpaceFraction,
      start: Option[Instant],
      end: Option[Instant],
  ): ABModel = {
    val effectiveStart = start.getOrElse(now)
    finishExperiment(testSpaceId, experimentId, Some(effectiveStart))
      .addExperiment(testSpaceId, experimentId, size, Some(effectiveStart), end)
  }

  override def listExperiments(): List[Experiment] = spaces.values.flatMap(_.experiments).toList

  override def evaluate(userId: UserId, at: Option[Instant]): Set[Experiment] = {
    val nowAt = at.getOrElse(now)
    spaces.values.flatMap { ns =>
      val SpaceFragmentPoint = hasher.hash(ns.id + "|" + userId)
      ns.experiments.find(e =>
        e.period.isActive(nowAt) &&
          e.bucket.contains(SpaceFragmentPoint),
      )
    }.toSet
  }

  override def toDTO: ABModelDTO = {
    val nsDTO = spaces.values.flatMap { ns =>
      ns.experiments.map { e =>
        ABModelDTO.Experiment(
          testSpaceId = ns.id,
          experimentId = e.experimentId,
          start = e.period.start,
          end = e.period.end,
          bucketStart = e.bucket.start,
          bucketEnd = e.bucket.end,
        )
      }
    }.toList
    ABModelDTO("1", "sha256", nsDTO)
  }
}

object ABModelImpl {
  def fromDTO(dto: ABModelDTO, clock: Clock): ABModel = {
    val spaces: Map[TestSpaceId, TestSpace] =
      dto.experiments
        .groupMap(x => TestSpaceId(x.testSpaceId))(e => {
          val period = TimePeriod(e.start, e.end)
          val bucket = SpaceFragment(Point(e.bucketStart), Point(e.bucketEnd))
          Experiment(ExperimentId(e.experimentId), period, bucket)
        })
        .map((spaceId, exps) => spaceId -> TestSpace(spaceId, exps))
    val hasher                              = dto.hashFunc match {
      case "sha256"  => Hasher.sha256
      case "md5"     => ??? // TODO
      case "murmur3" => ???
    }
    ABModelImpl(spaces, clock, hasher)
  }
}
