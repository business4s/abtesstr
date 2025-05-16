# abtesstr

**abtesstr** is a minimal, deterministic A/B testing helper written in Scala.
It allows you to build a model that assigns users to experiments.
Model definition happens in Scala while evaluation can happen in any other 
language as the model is prepared for serialization.

> [!WARNING]  
> This is a Proof Of Concept for now.

## ✨ Features

- Deterministic user assignment based on hashing
- Bucket-based fractional allocation with `Long` precision
- Immutable Scala API for defining and modifying experiments
- Supports multiple test spaces with non-overlapping experiments
- Time-bounded experiments with safe evaluation

## 🛠️ Example

```scala
val model = ABModel.empty
  .addNamespace("checkout")
  .addExperiment(
    namespaceId = NamespacxeId("checkout"),
    experimentId = ExperimentId("btn_test_v1"),
    size = SpaceFraction(0.1), // 10%
    start = Instant.now(),
    end = None
  )

val assignment: Set[Experiment] = model.evaluate(userId = "abc-123")
```

## 📦 Roadmap

* SQL-based evaluator
* Python evaluator
* Json schema for serializaiton stability

---

## AB Testing 101

Below is the general dissection of the AB Testing domain explaining how abtesstr fits into it.

A complete A/B testing system consists of several independent but composable layers:

| Area                      | Role                                         | abtesstr Handles                        | abtesstr Doesn't Handle                    |
|---------------------------|----------------------------------------------|-----------------------------------------|--------------------------------------------|
| Targeting & Participation | Define & control test entry                  | ✅ Rollout fractions, exclusivity        | ❌ Custom targeting logic                   |
| Assignment                | Allocate users to variants deterministically | ✅ Hashing, space allocation             | ❌ Learning algorithms, adaptive assignment |
| Execution & Exposure      | Change behavior, log exposures               | -                                       | ❌ Feature flag switching, exposure logging |
| Analysis                  | Measure impact of variants                   | 🟡 Reconstructing historical assignment | ❌ Metrics, statistical analysis, reporting |

### 🎯 Targeting & Participation

**Who can be included and when?**

- Based on static properties: platform, user group, geography, etc.
- Dynamic controls: ramp-ups, staged rollouts, consent checks
- **abtesstr handles:**
    - Deterministic rollout and sampling
    - Mutual exclusivity between experiments (via namespaces)
- **abtesstr doesn't handle:**
    - Complex targeting logic (e.g. geo, behavior) — up to the caller

### 🎲 Assignment

**Which variant should be presented to the user see?**

This is where **abtesstr** shines

- Assigns users via stable hashing into precise, non-overlapping ranges
- Ensures reproducibility across languages and time
- **abtesstr doesn't handle:**
    - Adaptive assignment (e.g. bandits), personalization, or learning

### ⚙️ Execution & Exposure

**How does the system behave differently?**

- Variant toggling via feature flags or conditional logic
- Exposure logged when the variant is actually shown
- **abtesstr doesn't handle this part**

### 📊 Analysis

**What was the impact of the experiment?**

- Metric collection and statistical testing
- Segmentation, significance, and effect size reporting
- **abtesstr handles:**
    - Historical assignment evaluation for validation and approximate backfill
- **abtesstr doesn't handle:**
    - Metrics collection, statistical testing, or visualization

--- 

## 🌱 Future Ideas

The following ideas highlight potential directions for expanding the project while preserving its declarative, composable nature.

- **Adaptive learning**  
  Allow an external layer to analyze experiment results and propose declarative patches to the model —  
  e.g. increase allocation, remove underperformers, or promote a variant.  
  Patches can be reviewed (rendered) and applied via standard model operations.

- **Participation control**  
  Introduce a minimal, serializable DSL for user-level targeting beyond `userId`.  
  Could be used to model eligibility, consent, rollout groups, etc.

- **Debugging support**  
  Provide a mechanism to explain why a user was (or wasn't) assigned to an experiment —  
  useful for QA, testing, or audit trails.

- **Event sourcing for model changes**  
  Track model evolution as a stream of immutable events —  
  enables full auditability, versioning, and change replay.

- **Visualization**  
  Render model history and namespace usage over time (e.g. as HTML timeline or Gantt chart).