package probfilter.pdsa.cuckoo


sealed trait FingerprintExtractor extends Serializable {
  def extract(data: Any): Short

  def extract(data: Any, bits: Int): Short = (extract(data) & ((1 << bits) - 1)).asInstanceOf[Short]
}


object FingerprintExtractor {
  private object ByteExtractor extends FingerprintExtractor {
    override def extract(data: Any): Short = (data.asInstanceOf[Byte] & 0xff).toShort
  }

  private object ShortExtractor extends FingerprintExtractor {
    override def extract(data: Any): Short = data.asInstanceOf[Short]
  }

  private object IntVersionedExtractor extends FingerprintExtractor {
    override def extract(data: Any): Short = IntVersionedEntry.extract(data.asInstanceOf[Int])
  }

  private object LongVersionedExtractor extends FingerprintExtractor {
    override def extract(data: Any): Short = LongVersionedEntry.extract(data.asInstanceOf[Long])
  }

  def create(strategy: CuckooStrategy[_]): FingerprintExtractor = strategy.storageType() match {
    case EntryStorageType.SIMPLE_BYTE => ByteExtractor
    case EntryStorageType.SIMPLE_SHORT => ShortExtractor
    case EntryStorageType.VERSIONED_INT => IntVersionedExtractor
    case EntryStorageType.VERSIONED_LONG => LongVersionedExtractor
  }
}
