package com.sbg.bdd.resource;

import java.io.IOException;

public interface WritableResource extends Resource{
    void write(byte[] data) ;
    boolean canRead();
    ReadableResource asReadable();
}
