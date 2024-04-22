package probfilter.pdsa.cuckoo


sealed trait FingerprintExtractor {
  def extract(data: Any): Short

  def extract(data: Any, bits: Int): Short = (extract(data) & ((1 << bits) - 1)).asInstanceOf[Short]
}


object FingerprintExtractor {
  private object ByteExtractor extends FingerprintExtractor {
    override def extract(data: Any): Short = (data.asInstanceOf[Long] & 0xffL).toShort
  }

  private object ShortExtractor extends FingerprintExtractor {
    override def extract(data: Any): Short = data.asInstanceOf[Short]
  }

  private object VersionedExtractor extends FingerprintExtractor {
    override def extract(data: Any): Short = VersionedEntry.extract(data.asInstanceOf[Long])
  }

  def create(strategy: CuckooStrategy[_]): FingerprintExtractor = strategy.storageType() match {
    case EntryStorageType.BYTE => ByteExtractor
    case EntryStorageType.SHORT => ShortExtractor
    case EntryStorageType.LONG => VersionedExtractor
  }
}
