# probfilter
`probfilter` is a Java / Scala library for conflict-free replicated probabilistic filters.

## Data Types
`probfilter` includes the following filters implemented as state-based (aka convergent) CRDTs.

- `GBloomFilter`: grow-only replicated bloom filter
- `GCuckooFilter`: grow-only replicated cuckoo filter
- `ORCuckooFilter`: observed-remove replicated cuckoo filter

## Status
🚧 This library is currently under active development. Features and code may change frequently, and the API is not yet stable.
