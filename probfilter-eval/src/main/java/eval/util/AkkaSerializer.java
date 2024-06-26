package eval.util;

import akka.cluster.ddata.protobuf.ReplicatedDataSerializer;
import akka.cluster.ddata.protobuf.msg.ReplicatorMessages;
import akka.remote.ByteStringUtils;
import akka.util.Unsafe;
import eval.int128.Int128;


/**
 * @implNote {@link akka.cluster.ddata.protobuf.ReplicatedDataSerializer#system()} is set to {@code null}.
 * Call only those methods that do not depend on it.
 */
public final class AkkaSerializer extends ReplicatedDataSerializer {
    /**
     * DO NOT CALL THIS CONSTRUCTOR
     */
    private AkkaSerializer() {
        super(null);
    }

    @Override
    public ReplicatorMessages.OtherMessage otherMessageToProto(Object msg) {
        if (msg instanceof Int128 int128) {
            return
                ReplicatorMessages.OtherMessage
                    .newBuilder()
                    .setEnclosedMessage(ByteStringUtils.toProtoByteStringUnsafe(int128.toBytes()))
                    .setSerializerId(0x42)
                    .build();
        }
        return super.otherMessageToProto(msg);
    }

    public static ReplicatedDataSerializer getInstance() {
        return Lazy.INSTANCE;
    }

    private static AkkaSerializer apply() {
        var unsafe = Unsafe.instance;
        try {
            return (AkkaSerializer) unsafe.allocateInstance(AkkaSerializer.class);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        }
    }

    private static class Lazy {
        private static final AkkaSerializer INSTANCE = apply();
    }
}
