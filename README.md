# abtesstr

**abtesstr** is a minimal, deterministic A/B testing helper written in Scala.
It allows you to build a model that assigns users to experiments.
Model definition happens in Scala while evaluation can happen in any other 
language as the model is prepared for serialization.

> [!WARNING]  
> This is a Proof Of Concept for now.

---

## ✨ Features

- Deterministic user assignment based on hashing
- Bucket-based fractional allocation with `Long` precision
- Immutable Scala API for defining and modifying experiments
- Supports multiple test spaces with non-overlapping experiments
- Time-bounded experiments with safe evaluation

---

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

---

## 📦 Roadmap

* SQL-based evaluator
* Python evaluator
* Json schema for serializaiton stability
