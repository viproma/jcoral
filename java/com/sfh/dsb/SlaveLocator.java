package com.sfh.dsb;

/**
 * An opaque class which contains the information needed to communicate with
 * a slave.
 * <p>
 * Objects of this class should always be disposed of with {@link #close} when
 * they are no longer needed, to avoid resource leaks in the underlying native
 * code. (A nice, automated way to do this is to use
 * <a href=https://docs.oracle.com/javase/tutorial/essential/exceptions/tryResourceClose.html">the
 * try-with-resources statement</a>.)
 */
public final class SlaveLocator implements AutoCloseable
{
    static
    {
        System.loadLibrary("jdsb");
    }

    /**
     * Releases native resources (such as memory) associated with this object.
     * <p>
     * After this function has been called, any attempt to use the class may
     * result in an error.
     */
    public void close() {
        if (nativePtr != 0) {
            destroyNative(nativePtr);
            nativePtr = 0;
        }
    }

    SlaveLocator(long nativePtr)
    {
        this.nativePtr = nativePtr;
    }

    long getNativePtr() { return nativePtr; }

    private static native void destroyNative(long selfPtr);

    private long nativePtr = 0;
}
