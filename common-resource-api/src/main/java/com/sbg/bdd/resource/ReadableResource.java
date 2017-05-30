package com.sbg.bdd.resource;

import java.io.IOException;

public interface ReadableResource extends Resource {
    byte[] read();
    WritableResource asWritable();
    boolean canWrite();
}
