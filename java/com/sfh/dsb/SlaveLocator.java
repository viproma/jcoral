package com.sfh.dsb;

/**
 * An opaque class which contains the information needed to communicate with
 * a slave.
 *
 * Remember to call close() once the object is no longer needed, to avoid
 * memory/resource leaks in the underlying native code.
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

    /**
     * Releases memory and any other resources associated with the object.
     */
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
