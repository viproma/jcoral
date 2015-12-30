package com.sfh.dsb;

/**
 * An opaque class which contains the information needed to connect to an
 * execution.
 *
 * Remember to call close() once the object is no longer needed, to avoid
 * memory/resource leaks in the underlying native code.
 */
public final class ExecutionLocator implements AutoCloseable
{
    static
    {
        System.loadLibrary("jdsb");
    }

    ExecutionLocator(long nativePtr)
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

