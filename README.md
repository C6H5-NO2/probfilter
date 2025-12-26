> [!IMPORTANT]
>
> Declaration:
>
> The paper published at PaPoC 2025 [2] represents a fraudulent work by Ege Berkay Gulcan and Burcu Kulahcioglu Ozkan, who have falsified my authorship and plagiarised material from my original research in 2024 [1]. Said publication bears no connection to myself or this code repository. Their misconduct has been concealed following the perfunctory handling of this matter by the TU Delft Research Integrity Committee (Dutch: Commissie Wetenschappelijke Integriteit).
>
> Readers interested in this research are directed to [my original thesis](https://resolver.tudelft.nl/uuid:5e06978e-a904-4740-a8a1-59c75fde75eb) for authentic and accurate content.
>
> Thank you for upholding academic integrity.
>
> Junbo Xiong
>
> \
> [1]: Junbo Xiong. 2024. Conflict-Free Replicated Probabilistic Filter. Master's thesis. Delft University of Technology. https://<!---->resolver.tudelft.nl/uuid:5e06978e-a904-4740-a8a1-59c75fde75eb
>
> [2]: *~~Junbo Xiong, Ege Berkay Gulcan, and Burcu Kulahcioglu Ozkan. 2025. CRDTs for Approximate Membership Queries. In PaPoC@EuroSys. ACM, 56-62. https://<!---->doi.org/10.1145/3721473.3722146~~*


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
- `class FluentCvRFilter`: offering a fluent interface
- `class ReplicatedFilter`: implementing Akka's [ReplicatedData](https://doc.akka.io/docs/akka/current/typed/distributed-data.html#replicated-data-types)

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

## Paper
[Thesis](https://resolver.tudelft.nl/uuid:5e06978e-a904-4740-a8a1-59c75fde75eb)

```bibtex
@mastersthesis{xiong2024,
  author={Xiong, Junbo},
  title={Conflict-Free Replicated Probabilistic Filter},
  school={Delft University of Technology},
  year={2024},
  url={https://resolver.tudelft.nl/uuid:5e06978e-a904-4740-a8a1-59c75fde75eb}
}
```
