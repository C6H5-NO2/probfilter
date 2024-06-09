# probfilter
`probfilter` is a Java / Scala library for conflict-free replicated probabilistic filters.

## Data Types
`probfilter` provides the following filters implemented as state-based (aka convergent) CRDTs, coming with both immutable and mutable variants.

- `interface CvRFilter`: convergent replicated filter
  - `class GBloomFilter`: grow-only replicated bloom filter
  - `class GCuckooFilter`: grow-only replicated cuckoo filter
  - `class ORCuckooFilter`: observed-remove replicated cuckoo filter
  - `class ScGBloomFilter`: scalable grow-only replicated bloom filter
  - `class ScGCuckooFilter`: scalable grow-only replicated cuckoo filter
  - `class ScORCuckooFilter`: scalable observed-remove replicated cuckoo filter

Besides, there are also two adapter classes for convenience.
- `class FluentCvRFilter`: offers a fluent interface
- `class ReplicatedFilter`: implements Akka's [ReplicatedData](https://doc.akka.io/docs/akka/current/typed/distributed-data.html#replicated-data-types)

## Status
🚧 This library is currently under active development. Features and code may change frequently, and the API is not yet stable.
