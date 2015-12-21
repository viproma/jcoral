package com.sfh.dsb;

/**
 * An opaque class which contains the information needed to communicate with
 * a slave.
 */
public final class SlaveLocator implements AutoCloseable
{
    static
    {
        System.loadLibrary("jdsb");
    }

    SlaveLocator(long nativePtr)
    {
        this.nativePtr = nativePtr;
    }

    public void close() {
        if (nativePtr != 0) {
            destroyNative(nativePtr);
            nativePtr = 0;
        }
    }

    long getNativePtr() { return nativePtr; }

    private static native void destroyNative(long selfPtr);

    private long nativePtr = 0;
}
