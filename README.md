# probfilter
`probfilter` is a Java / Scala library for conflict-free replicated probabilistic filters.

## Data Types
`probfilter` includes the following filters implemented as state-based (aka convergent) CRDTs.

- `interface CvFilter`: convergent replicated filter
  - `class GBloomFilter`: grow-only replicated bloom filter
  - `class GCuckooFilter`: grow-only replicated cuckoo filter
  - `class ORCuckooFilter`: observed-remove replicated cuckoo filter
  - `abstract class CvFilterSeries`: scalable filter series
    - `class ScGBloomFilter`: scalable grow-only replicated bloom filter
    - `class ScGCuckooFilter`: scalable grow-only replicated cuckoo filter
    - `class ScORCuckooFilter`: scalable observed-remove replicated cuckoo filter

## Status
🚧 This library is currently under active development. Features and code may change frequently, and the API is not yet stable.
