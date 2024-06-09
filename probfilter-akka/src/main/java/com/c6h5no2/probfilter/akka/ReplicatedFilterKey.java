package com.c6h5no2.probfilter.akka;

import akka.cluster.ddata.Key;

import java.io.Serial;
import java.io.Serializable;


public final class ReplicatedFilterKey extends Key<ReplicatedFilter> implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    public ReplicatedFilterKey(String id) {
        super(id);
    }

    public static ReplicatedFilterKey apply(String id) {
        return new ReplicatedFilterKey(id);
    }
}
