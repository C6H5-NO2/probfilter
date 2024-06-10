package eval.util


/** An immutable collection of evaluation records. */
final class EvalRecord private(record: Map[String, Seq[Number]]) {
  def this(statistics: String*) = this(Map.apply(statistics.map((_, Seq.empty[Number])): _*))

  def this(statistics: Array[String]) = this(statistics: _*)

  /** Appends to `statistic` a newly observed `value`. */
  def appended(statistic: String, value: Number): EvalRecord = {
    new EvalRecord(record.updated(statistic, record.apply(statistic).appended(value)))
  }

  /** Sets `statistic` to `value`. */
  def updated(statistic: String, value: Number): EvalRecord = {
    if (record.apply(statistic).length > 1) {
      throw new IllegalArgumentException()
    }
    new EvalRecord(record.updated(statistic, Seq.apply(value)))
  }

  def averaged: EvalRecord = {
    val length = record.valuesIterator.map(_.length).max
    val newMap = record.view.mapValues { seq =>
      if (seq.isEmpty) {
        throw new IllegalArgumentException()
      } else if (seq.length == 1) {
        seq
      } else {
        if (seq.length != length) {
          throw new IllegalArgumentException()
        }
        Seq.apply[Number](seq.foldLeft(0.0)((sum, num) => sum + num.doubleValue()) / length)
      }
    }.toMap
    new EvalRecord(newMap)
  }

  def get(statistic: String): Number = {
    val seq = record.apply(statistic)
    if (seq.length == 1)
      seq.head
    else
      throw new IllegalArgumentException()
  }

  def getInt(statistic: String): Int = get(statistic).intValue()

  def getDouble(statistic: String): Double = get(statistic).doubleValue()
}
