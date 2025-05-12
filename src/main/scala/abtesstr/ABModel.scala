package abtesstr

import abtesstr.internal.ABModelImpl

import java.time.{Clock, Instant}

trait ABModel {

  def addTestSpace(id: TestSpaceId): ABModel // TODO error

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

  def listExperiments(): List[Experiment]

  def evaluate(userId: UserId, at: Option[Instant]): Set[Experiment]

  def toDTO: ABModelDTO

}

object ABModel {
  def fromDTO(dto: ABModelDTO): ABModel = ABModelImpl.fromDTO(dto, Clock.systemUTC())
  // TODO select hashing
  def empty: ABModel                    = ABModelImpl(Map.empty, Clock.systemUTC(), Hasher.sha256)
}
