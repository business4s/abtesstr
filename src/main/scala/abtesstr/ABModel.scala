package abtesstr

import abtesstr.internal.Error.AddTestSpaceError
import abtesstr.internal.TemporalContext

import java.time.{Clock, Instant}

trait ABModel[WithTime[A], WithError[Err, A]] {
  type Self = ABModel[WithTime, WithError]

  def addTestSpace(id: TestSpaceId): WithError[AddTestSpaceError, Self] // TODO error

  def removeTestSpace(id: TestSpaceId): ABModel // TODO error

  def listTestSpaces(): List[TestSpaceId]

  def availableSpace(testSpaceId: TestSpaceId): SpaceFraction

  def addExperiment(
      testSpaceId: TestSpaceId,
      experimentId: ExperimentId,
      size: SpaceFraction,
      start: Option[Instant] = None,
      end: Option[Instant] = None,
  ): ABModel // TODO error: unique id, start in future

  def finishExperiment(testSpaceId: TestSpaceId, experimentId: ExperimentId, at: Option[Instant]): ABModel // TODO error: already started

  def trim(before: Option[Instant]): ABModel

  def changeSize(
      testSpaceId: TestSpaceId,
      experimentId: ExperimentId,
      size: SpaceFraction,
      start: Option[Instant],
      end: Option[Instant],
  ): ABModel //

  def listExperiments(): List[ExperimentRun]

  def evaluate(userId: UserId, at: Option[Instant]): Set[ExperimentRun]

  def toDTO: ABModelDTO

}

object ABModel {
  def fromDTO(dto: ABModelDTO): ABModel = ABModelImpl.fromDTO(dto, Clock.systemUTC())
  // TODO select hashing
  def empty: ABModel                    = ABModelImpl(Map.empty, Clock.systemUTC(), Hasher.sha256)
}
