# probfilter
`probfilter` is a Java / Scala library for conflict-free replicated probabilistic filters.

## Usage
To add `probfilter` as dependency, using Maven for instance, include the following snippet:
```xml
<dependency>
    <groupId>com.c6h5no2</groupId>
    <artifactId>probfilter-core</artifactId>
    <version>0.1.1</version>
</dependency>
```

If you need to work with [Akka Cluster](https://doc.akka.io/docs/akka/current/typed/index-cluster.html), include also the adapters:
```xml
<dependency>
    <groupId>com.c6h5no2</groupId>
    <artifactId>probfilter-akka</artifactId>
    <version>0.1.1</version>
</dependency>
```

Substitute `version` if necessary.

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

## Example
```java
int elem = 42;
var filter1 = new ORCuckooFilter.Immutable<>(strategy, (short) 1).asFluent();
var filter2 = new ORCuckooFilter.Immutable<>(strategy, (short) 2).asFluent();

filter1 = filter1.add(elem);
assert filter1.contains(elem);

filter2 = filter2.merge(filter1);
assert filter2.contains(elem);

filter2 = filter2.remove(elem);
assert !filter2.contains(elem);

filter1 = filter1.merge(filter2);
assert !filter1.contains(elem);
```
