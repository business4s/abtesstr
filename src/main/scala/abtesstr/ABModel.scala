package abtesstr

import abtesstr.internal.Error.{
  AddExperimentError,
  AddTestSpaceError,
  FinishExperimentError,
  GetAvailableSpaceError,
  RemoveTestSpaceError,
  ResizeExperimentError,
}
import abtesstr.internal.{APIErrorHandler, APITimeHandler, MappedABModel, RawABModel, TemporalContext}

import java.time.{Clock, Instant}

trait ABModel[WithTime[A], WithError[Err, A]] {
  type Self = ABModel[WithTime, WithError]

  def addTestSpace(id: TestSpaceId): WithError[AddTestSpaceError, Self]

  def removeTestSpace(id: TestSpaceId): WithTime[WithError[RemoveTestSpaceError, Self]]

  def listTestSpaces(): List[TestSpaceId]

  def availableSpace(testSpaceId: TestSpaceId): WithTime[WithError[GetAvailableSpaceError, SpaceFraction]]

  def addExperiment(
      testSpaceId: TestSpaceId,
      experimentId: ExperimentId,
      size: SpaceFraction,
      start: Option[Instant] = None,
      end: Option[Instant] = None,
  ): WithTime[WithError[AddExperimentError, Self]]

  def finishExperiment(testSpaceId: TestSpaceId, experimentId: ExperimentId, at: Option[Instant]): WithTime[WithError[FinishExperimentError, Self]]

  def trim(before: Option[Instant]): WithTime[Self]

  def resizeExperiment(
      testSpaceId: TestSpaceId,
      experimentId: ExperimentId,
      size: SpaceFraction,
      start: Option[Instant],
      end: Option[Instant],
  ): WithTime[WithError[ResizeExperimentError, Self]]

  def listExperiments(): List[ExperimentRun]

  def evaluate(userId: UserId, at: Option[Instant]): WithTime[Set[ExperimentRun]]

  def toDTO: ABModelDTO

}

object ABModel {
  type Raw = ABModel[TemporalContext => *, Either]

  def empty[WithTime[_], WithError[_, _]](
      timeHandler: APITimeHandler[WithTime] = APITimeHandler.Raw,
      errorHandler: APIErrorHandler[WithError] = APIErrorHandler.Raw,
  ): ABModel[WithTime, WithError] = MappedABModel[WithTime, WithError](RawABModel(Map.empty, Hasher.sha256), errorHandler, timeHandler)

  def fromDTO[WithTime[_], WithError[_, _]](
      dto: ABModelDTO,
      timeHandler: APITimeHandler[WithTime] = APITimeHandler.Raw,
      errorHandler: APIErrorHandler[WithError] = APIErrorHandler.Raw,
  ): ABModel[WithTime, WithError] = MappedABModel[WithTime, WithError](RawABModel.fromDTO(dto), errorHandler, timeHandler)

}
