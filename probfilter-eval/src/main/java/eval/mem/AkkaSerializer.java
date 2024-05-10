package eval.mem;

import akka.cluster.ddata.protobuf.ReplicatedDataSerializer;
import akka.cluster.ddata.protobuf.msg.ReplicatorMessages;
import akka.remote.ByteStringUtils;
import akka.util.Unsafe;
import eval.int128.Int128;


public final class AkkaSerializer extends ReplicatedDataSerializer {
    /**
     * DO NOT CALL THIS CONSTRUCTOR
     */
    private AkkaSerializer() {
        super(null);
    }

    private static class Lazy {
        private static final AkkaSerializer INSTANCE = create();
    }

    private static AkkaSerializer create() {
        var unsafe = Unsafe.instance;
        try {
            return (AkkaSerializer) unsafe.allocateInstance(AkkaSerializer.class);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        }
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
}
