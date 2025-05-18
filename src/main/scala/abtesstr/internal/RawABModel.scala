package abtesstr.internal

import abtesstr.*
import abtesstr.internal.Error.*

import java.time.{Clock, Instant}

trait TemporalContext {
  def now(): Instant
}

object TemporalContext {
  def fixed(t: Instant): TemporalContext = () => t
  def live(t: Clock): TemporalContext = () => t.instant()
}

type Result[E, A] = TemporalContext ?=> Either[E, A]

object Error {

  type AddTestSpaceError = TestSpaceAlreadyExists

  type RemoveTestSpaceError = TestSpaceDoesntExist | TestSpaceHasActiveExperiments

  type GetAvailableSpaceError = TestSpaceDoesntExist

  type AddExperimentError = TestSpaceDoesntExist | EmptyExperiment | ExperimentStartInThePast | NotEnoughSpace

  type FinishExperimentError = TestSpaceDoesntExist | ExperimentDoesntExist | ExperimentAlreadyFinished | ExperimentEndInThePast

  type ResizeExperimentError = AddExperimentError | FinishExperimentError

  case class TestSpaceAlreadyExists(id: TestSpaceId) extends Exception(s"Test space ${id} already exists")

  case class TestSpaceDoesntExist(id: TestSpaceId) extends Exception(s"Test space ${id} doesn't exist")

  case class TestSpaceHasActiveExperiments(id: TestSpaceId, active: List[ExperimentId])
      extends Exception("Can't remove a space with active experiments")

  case class EmptyExperiment()                                          extends Exception("An experiment can't be defined for any empty space fragment")
  case class ExperimentStartInThePast(requested: Instant, now: Instant) extends Exception("An experiment cannot start in the past")
  case class NotEnoughSpace(requested: SpaceFraction, available: SpaceFraction)
      extends Exception(s"Not enough space for experiment. Requested ${requested}, available ${available}")

  case class ExperimentDoesntExist(id: ExperimentId) extends Exception(s"Experiment ${id} doesn't exist")

  case class ExperimentEndInThePast(requested: Instant, now: Instant) extends Exception("An experiment cannot finish in the past")
  case class ExperimentAlreadyFinished()                              extends Exception("An experiment has already been finished")

}

case class RawABModel(spaces: Map[TestSpaceId, TestSpace], hasher: Hasher) extends ABModel[[t] =>> TemporalContext => t, Either] {

  def addTestSpace(id: TestSpaceId): Either[AddTestSpaceError, RawABModel] = {
    if (spaces.contains(id)) Left(TestSpaceAlreadyExists(id))
    else Right(this.copy(spaces = spaces.updated(id, TestSpace(id, List()))))
  }

  def removeTestSpace(id: TestSpaceId): TemporalContext => Either[RemoveTestSpaceError, RawABModel] = ctx => {
    spaces.get(id) match {
      case None        => Left(TestSpaceDoesntExist(id))
      case Some(space) =>
        val active = space.activeExperiments(ctx.now()).map(_.experimentId)
        if (active.nonEmpty) Left(TestSpaceHasActiveExperiments(id, active))
        else Right(this.copy(spaces = spaces.removed(id)))
    }
  }

  def listTestSpaces(): List[TestSpaceId] = spaces.keys.toList

  def availableSpace(testSpaceId: TestSpaceId): TemporalContext => Either[GetAvailableSpaceError, SpaceFraction] = ctx => {
    spaces
      .get(testSpaceId)
      .map(x => Right(x.availableSpace(ctx.now())))
      .getOrElse(Left(TestSpaceDoesntExist(testSpaceId)))
  }

  def addExperiment(
      testSpaceId: TestSpaceId,
      experimentId: ExperimentId,
      size: SpaceFraction,
      start: Option[Instant],
      end: Option[Instant],
  ): TemporalContext => Either[AddExperimentError, RawABModel] = ctx => {
    for {
      _       <- Either.cond(size != SpaceFraction.zero, (), EmptyExperiment())
      now      = ctx.now()
      _       <- Either.cond(!start.exists(_.isBefore(now)), (), ExperimentStartInThePast(start.get, now))
      space   <- spaces.get(testSpaceId).toRight(TestSpaceDoesntExist(testSpaceId))
      buckets <- space.findFit(size, now).toRight(NotEnoughSpace(size, space.availableSpace(now)))
      period   = TimePeriod(start.getOrElse(now), end)
      newExps  = buckets.map(spaceFragment => ExperimentRun(experimentId, period, spaceFragment))
      updated  = spaces.updated(testSpaceId, space.copy(experiments = space.experiments ++ newExps))
    } yield this.copy(spaces = updated)
  }

  def finishExperiment(
      testSpaceId: TestSpaceId,
      experimentId: ExperimentId,
      at: Option[Instant],
  ): TemporalContext => Either[FinishExperimentError, RawABModel] = ctx => {
    val now        = ctx.now()
    val finishTime = at.getOrElse(now)
    for {
      _         <- Either.cond(!at.exists(_.isBefore(now)), (), ExperimentEndInThePast(at.get, now))
      space     <- spaces.get(testSpaceId).toRight(TestSpaceDoesntExist(testSpaceId))
      exps       = space.experiments.filter(_.experimentId == experimentId)
      activeExps = exps.filter(_.period.isActive(finishTime))
      _         <- Either.cond(exps.nonEmpty, (), ExperimentDoesntExist(experimentId))
      _         <- Either.cond(activeExps.nonEmpty, (), ExperimentAlreadyFinished())
      updated    = spaces.updated(
                     testSpaceId,
                     space.copy(experiments = space.experiments.map {
                       case e if e.experimentId == experimentId && e.period.isActive(finishTime) =>
                         e.copy(period = e.period.copy(end = Some(finishTime)))
                       case e                                                                    => e
                     }),
                   )
    } yield this.copy(spaces = updated)
  }

  def trim(before: Option[Instant]): TemporalContext => RawABModel = ctx => {
    val cutoff  = before.getOrElse(ctx.now())
    val updated = spaces.view
      .mapValues { ns => ns.copy(experiments = ns.experiments.filter(e => e.period.end.forall(_.isAfter(cutoff)))) }
      .toMap
      .filter((_, space) => space.experiments.nonEmpty)

    this.copy(spaces = updated)
  }

  def resizeExperiment(
      testSpaceId: TestSpaceId,
      experimentId: ExperimentId,
      size: SpaceFraction,
      start: Option[Instant],
      end: Option[Instant],
  ): TemporalContext => Either[ResizeExperimentError, RawABModel] = ctx => {
    val fixedCtx       = TemporalContext.fixed(ctx.now())
    val effectiveStart = start.getOrElse(fixedCtx.now())
    for {
      finished <- finishExperiment(testSpaceId, experimentId, Some(effectiveStart))(fixedCtx)
      started  <- finished.addExperiment(testSpaceId, experimentId, size, Some(effectiveStart), end)(fixedCtx)
    } yield started
  }

  def listExperiments(): List[ExperimentRun] = spaces.values.flatMap(_.experiments).toList

  def evaluate(userId: UserId, at: Option[Instant]): TemporalContext => Set[ExperimentRun] = ctx => {
    val nowAt = at.getOrElse(ctx.now())
    spaces.values.flatMap { ns =>
      val SpaceFragmentPoint = hasher.hash(ns.id + "|" + userId)
      ns.experiments.find(e =>
        e.period.isActive(nowAt) &&
          e.bucket.contains(SpaceFragmentPoint),
      )
    }.toSet
  }

  def toDTO: ABModelDTO = {
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

object RawABModel {
  // TODO errors
  def fromDTO(dto: ABModelDTO): RawABModel = {
    val spaces: Map[TestSpaceId, TestSpace] =
      dto.experiments
        .groupMap(x => TestSpaceId(x.testSpaceId))(e => {
          val period = TimePeriod(e.start, e.end)
          val bucket = SpaceFragment(Point(e.bucketStart), Point(e.bucketEnd))
          ExperimentRun(ExperimentId(e.experimentId), period, bucket)
        })
        .map((spaceId, exps) => spaceId -> TestSpace(spaceId, exps))
    val hasher                              = dto.hashFunc match {
      case "sha256"  => Hasher.sha256
      case "md5"     => ??? // TODO
      case "murmur3" => ???
    }
    RawABModel(spaces, hasher)
  }
}
