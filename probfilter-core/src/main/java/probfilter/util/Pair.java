package probfilter.util;


/**
 * - "How is returning multiple values normally done in Java?"
 * - "Painfully."
 */
@JavaFriendly(scalaDelegate = "scala.Tuple2")
public record Pair<T1, T2>(T1 _1, T2 _2) {}
