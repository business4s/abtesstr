package abtesstr.internal

import abtesstr.*
import abtesstr.internal.Error.{EmptyExperiment, ExperimentStartInThePast, NotEnoughSpace, TestSpaceAlreadyExists, TestSpaceDoesntExist, TestSpaceHasActiveExperiments}
import org.scalacheck.Gen
import org.scalatest.EitherValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import java.time.{Clock, Instant, ZoneId}

type Id[T] = T

class RawABModelSpec extends AnyFreeSpec with ScalaCheckPropertyChecks with Generators with EitherValues {

  // Fixed clock for deterministic testing
  val fixedInstant = Instant.parse("2023-01-01T00:00:00Z")
  val fixedClock   = Clock.fixed(fixedInstant, ZoneId.systemDefault())

  // Helper method to create a test model
  def createTestModel(): ABModel[Id, Either] = {
    ABModel.empty(timeHandler = APITimeHandler.WithClock(fixedClock))
  }

  "ABModelImpl" - {
    "when empty" - {
      val emptyModel = createTestModel()

      "should have no test spaces" in {
        assert(emptyModel.listTestSpaces().isEmpty)
      }

      "should have no experiments" in {
        assert(emptyModel.listExperiments().isEmpty)
      }

      "should evaluate to empty set for any user" in {
        assert(emptyModel.evaluate(UserId("user1"), None).isEmpty)
      }

      "should convert to DTO correctly" in {
        val dto = emptyModel.toDTO
        assert(dto == ABModelDTO("1", "sha256", List.empty))
      }
    }

    "test space management" - {
      "addTestSpace" - {
        "should add a new test space" in {
          val model       = createTestModel()
          val testSpaceId = TestSpaceId("test-space")

          val updatedModel = model.addTestSpace(testSpaceId).value
          assert(updatedModel.listTestSpaces() == List(testSpaceId))
        }

        "should raise duplicate existing test space" in {
          val model       = createTestModel()
          val testSpaceId = TestSpaceId("test-space")

          val updated = model
            .addTestSpace(testSpaceId)
            .value
            .addTestSpace(testSpaceId)

          assert(updated == Left(TestSpaceAlreadyExists(testSpaceId)))
        }
      }

      "removeTestSpace" - {
        "should remove empty test space" in {
          val model       = createTestModel()
          val testSpaceId = TestSpaceId("test-space")

          val updatedModel = model.addTestSpace(testSpaceId).value.removeTestSpace(testSpaceId).value
          assert(updatedModel.listTestSpaces() == List())
        }
        "should fail to remove non-existing" in {
          val model       = createTestModel()
          val testSpaceId = TestSpaceId("test-space")

          val updatedModel = model.addTestSpace(testSpaceId).value.removeTestSpace(testSpaceId).value
          assert(updatedModel.listTestSpaces() == List())
        }
        "should fail to remove space with experiments" in {
          val model        = createTestModel()
          val testSpaceId  = TestSpaceId("test-space")
          val experimentId = ExperimentId("exp1")

          val result = model
            .addTestSpace(testSpaceId)
            .value
            .addExperiment(testSpaceId, experimentId, SpaceFraction(0.5), None, None)
            .value
            .removeTestSpace(testSpaceId)

          assert(result == Left(TestSpaceHasActiveExperiments(testSpaceId, List(experimentId))))
        }
      }

      case class ExperimentInput(id: ExperimentId, size: SpaceFraction)

      val expGen: Gen[List[ExperimentInput]] = genSpaceSplit().flatMap(fragments =>
        Gen.sequence(
          fragments.map(a => genExperimentId.map(b => ExperimentInput(b, a))),
        ),
      )

      "availableSpace" - {
        "should return the available space in the test space" in {
          val testSpaceId = TestSpaceId("test-space")
          val model       = createTestModel()
            .addTestSpace(testSpaceId)
            .value
          forAll(expGen)((experiments: Seq[ExperimentInput]) => {
            val result = experiments.foldLeft(model) { (model, experiment) =>
              model.addExperiment(testSpaceId, experiment.id, experiment.size, None, None).value
            }
            assert(result.availableSpace(testSpaceId).value == 1 - experiments.map(_.size).sum)
          })
        }
      }
    }

    "experiment management" - {
      "addExperiment" - {
        "should add experiment to the existing test space" in {
          val testSpaceId  = TestSpaceId("test-space")
          val model        = createTestModel().addTestSpace(testSpaceId).value
          val experimentId = ExperimentId("exp1")
          val size         = SpaceFraction(0.5)

          val updated = model.addExperiment(testSpaceId, experimentId, size, None, None).value

          assert(
            updated.listExperiments() == List(
              ExperimentRun(
                experimentId,
                TimePeriod(fixedInstant, None),
                SpaceFragment.ofFraction(Point.zero, size),
              ),
            ),
          )
        }
        "should raise error if test space doesn't exist" in {
          val testSpaceId = TestSpaceId("test-space")
          val result      =
            createTestModel()
              .addExperiment(testSpaceId, ExperimentId("exp1"), SpaceFraction(0.5))
          assert(result == Left(TestSpaceDoesntExist(testSpaceId)))
        }
        "should raise error if experiment is empty" in {
          val testSpaceId = TestSpaceId("test-space")
          val result      =
            createTestModel()
              .addTestSpace(testSpaceId)
              .value
              .addExperiment(testSpaceId, ExperimentId("exp1"), SpaceFraction(0.0))
          assert(result == Left(EmptyExperiment()))
        }
        "should raise error if experiment is in the past" in {
          val testSpaceId = TestSpaceId("test-space")
          val start       = fixedInstant.minusSeconds(100)
          val result      =
            createTestModel()
              .addTestSpace(testSpaceId)
              .value
              .addExperiment(testSpaceId, ExperimentId("exp1"), SpaceFraction(0.1), start = Some(start))
          assert(result == Left(ExperimentStartInThePast(start, fixedInstant)))
        }
        "should raise error if not enough space" in {
          val testSpaceId = TestSpaceId("test-space")

          val result =
            createTestModel()
              .addTestSpace(testSpaceId)
              .value
              .addExperiment(testSpaceId, ExperimentId("exp1"), SpaceFraction(0.5))
              .value
              .addExperiment(testSpaceId, ExperimentId("exp2"), SpaceFraction(0.6))

          assert(result == Left(NotEnoughSpace(SpaceFraction(0.6), SpaceFraction(0.5))))
        }
      }

      "finishExperiment" - {
        "should finish an active experiment" in {
          fail()
        }

        "should finish all active experiment fragments" in {
          fail()
        }

        "should not modify an already finished experiment" in {
          fail()
        }
      }

      "trim" - {
        "should remove experiments that ended before the cutoff" in {
          fail()
        }
      }

      "changeSize" - {
        "should finish the current experiment and add a new one with the new size" in {
          fail()
        }
      }

      "evaluate" - {
        "should return experiments that contain the user's hash point" in {
          fail()
        }
      }
    }

    "DTO conversion" - {
      "fromDTO" - {
        "should create a model from DTO" in {
          val dto = ABModelDTO(
            version = "1",
            hashFunc = "sha256",
            experiments = List(
              ABModelDTO.Experiment(
                testSpaceId = "test-space",
                experimentId = "exp1",
                start = fixedInstant.minusSeconds(100),
                end = None,
                bucketStart = 0L,
                bucketEnd = SpaceSize / 2,
              ),
            ),
          )

          val model = ABModel.fromDTO(dto)

          // Verify the model was created correctly
          val testSpaces = model.listTestSpaces()
          assert(testSpaces.size == 1)
          assert(testSpaces.head == TestSpaceId("test-space"))

          val experiments = model.listExperiments()
          assert(experiments.size == 1)
          val experiment  = experiments.head
          assert(experiment.experimentId == ExperimentId("exp1"))
          assert(experiment.period.start == fixedInstant.minusSeconds(100))
          assert(experiment.period.end == None)
          assert(experiment.bucket.start == 0L)
          assert(experiment.bucket.end == SpaceSize / 2)
        }

        "should throw exception for unsupported hash functions" in {
          val dto = ABModelDTO(
            version = "1",
            hashFunc = "md5", // Unsupported
            experiments = List(),
          )

          assertThrows[NotImplementedError] {
            RawABModel.fromDTO(dto)
          }
        }
      }

      "toDTO" - {
        "should convert model to DTO" in {
          // Setup a model with a test space and an experiment
          val testSpaceId  = TestSpaceId("test-space")
          val experimentId = ExperimentId("exp1")
          val start        = fixedInstant.minusSeconds(100)
          val period       = TimePeriod(start, None)
          val bucket       = SpaceFragment.ofFraction(Point.zero, SpaceFraction(0.5))
          val experiment   = ExperimentRun(experimentId, period, bucket)
          val testSpace    = TestSpace(testSpaceId, List(experiment))

          val model = RawABModel(Map(testSpaceId -> testSpace), Hasher.sha256)

          // Convert to DTO
          val dto = model.toDTO

          // Verify the DTO
          assert(dto.version == "1")
          assert(dto.hashFunc == "sha256")
          assert(dto.experiments.size == 1)

          val dtoExperiment = dto.experiments.head
          assert(dtoExperiment.testSpaceId == testSpaceId)
          assert(dtoExperiment.experimentId == experimentId)
          assert(dtoExperiment.start == start)
          assert(dtoExperiment.end == None)
          assert(dtoExperiment.bucketStart == 0L)
          assert(dtoExperiment.bucketEnd == SpaceSize / 2)
        }
      }
    }
  }
}
