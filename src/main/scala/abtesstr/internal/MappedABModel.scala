package abtesstr.internal

import abtesstr.{ABModel, ABModelDTO, ExperimentId, ExperimentRun, SpaceFraction, TestSpaceId, UserId}
import abtesstr.internal.Error.{
  AddExperimentError,
  AddTestSpaceError,
  FinishExperimentError,
  GetAvailableSpaceError,
  RemoveTestSpaceError,
  ResizeExperimentError,
}

import java.time.{Clock, Instant}

type Id[T] = T

trait APIErrorHandler[F[_, _]] {
  def transformError[E <: Exception, A](base: Either[E, A]): F[E, A]
}
object APIErrorHandler         {
  object Raw      extends APIErrorHandler[Either]       {
    override def transformError[E, A](base: Either[E, A]): Either[E, A] = base
  }
  object Throwing extends APIErrorHandler[[e, a] =>> a] {
    override def transformError[E <: Exception, A](base: Either[E, A]): A = base.fold(throw _, identity)
  }
}

trait APITimeHandler[F[_]] {
  def transformTime[A](base: TemporalContext => A): F[A]
}
object APITimeHandler      {
  object Raw extends APITimeHandler[TemporalContext => *] {
    override def transformTime[A](base: TemporalContext => A): TemporalContext => A = base
  }

  case class WithClock(clock: Clock) extends APITimeHandler[Id] {
    private val ctx                                                  = TemporalContext.live(clock)
    override def transformTime[A](base: TemporalContext => A): Id[A] = base(ctx)
  }
}

case class MappedABModel[WithTime[A], WithError[E, A]](
    delegate: ABModel.Raw,
    errorHandler: APIErrorHandler[WithError],
    timeHandler: APITimeHandler[WithTime],
) extends ABModel[WithTime, WithError] {
  private type Self = ABModel[WithTime, WithError]

  override def addTestSpace(id: TestSpaceId): WithError[AddTestSpaceError, Self] =
    errorHandler.transformError(delegate.addTestSpace(id).map(preserveMapping))

  override def removeTestSpace(id: TestSpaceId): WithTime[WithError[RemoveTestSpaceError, Self]] =
    timeHandler.transformTime(delegate.removeTestSpace(id).andThen(x => errorHandler.transformError(x.map(preserveMapping))))

  override def listTestSpaces(): List[TestSpaceId] = delegate.listTestSpaces()

  override def availableSpace(testSpaceId: TestSpaceId): WithTime[WithError[GetAvailableSpaceError, SpaceFraction]] =
    timeHandler.transformTime(delegate.availableSpace(testSpaceId).andThen(errorHandler.transformError))

  override def addExperiment(
      testSpaceId: TestSpaceId,
      experimentId: ExperimentId,
      size: SpaceFraction,
      start: Option[Instant],
      end: Option[Instant],
  ): WithTime[WithError[AddExperimentError, Self]] =
    timeHandler.transformTime(
      delegate
        .addExperiment(testSpaceId, experimentId, size, start, end)
        .andThen(x => errorHandler.transformError(x.map(preserveMapping))),
    )

  override def finishExperiment(
      testSpaceId: TestSpaceId,
      experimentId: ExperimentId,
      at: Option[Instant],
  ): WithTime[WithError[FinishExperimentError, Self]] =
    timeHandler.transformTime(
      delegate.finishExperiment(testSpaceId, experimentId, at).andThen(x => errorHandler.transformError(x.map(preserveMapping))),
    )

  override def trim(before: Option[Instant]): WithTime[Self] =
    timeHandler.transformTime(delegate.trim(before).andThen(preserveMapping))

  override def resizeExperiment(
      testSpaceId: TestSpaceId,
      experimentId: ExperimentId,
      size: SpaceFraction,
      start: Option[Instant],
      end: Option[Instant],
  ): WithTime[WithError[ResizeExperimentError, Self]] =
    timeHandler.transformTime(
      delegate.resizeExperiment(testSpaceId, experimentId, size, start, end).andThen(x => errorHandler.transformError(x.map(preserveMapping))),
    )

  override def listExperiments(): List[ExperimentRun] = delegate.listExperiments()

  override def evaluate(userId: UserId, at: Option[Instant]): WithTime[Set[ExperimentRun]] =
    timeHandler.transformTime(delegate.evaluate(userId, at))

  override def toDTO: ABModelDTO = delegate.toDTO

  private def preserveMapping(raw: ABModel.Raw): Self = this.copy(delegate = raw)
}
