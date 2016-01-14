package com.sfh.dsb;

import java.util.Date;
import com.sfh.dsb.SlaveID;


/**
 * A mechanism to access the result of an asynchronous operation.
 * <p>
 * Futures provide a way to share the result of an operation between the thread
 * that performed the operation and the thread that initiated it and therefore
 * should receive the result of it.  This result, whether or not it is ready
 * yet, is referred to as the <em>shared state</em> in the following
 * documentation.  If the asynchronous operation failed, an exception may be
 * stored in the shared state, and any attempt to retrieve the result will cause
 * this exception to be thrown.
 * <p>
 * The various nested subclasses of Future represent the different supported
 * result types.  Each subclass contains a <code>get()</code> method which
 * may be used to retrieve the result (and/or check for an exception).
 * This function may only be called once for each Future object.
 * <p>
 * This class is a wrapper around the various <code>std::future</code> objects
 * used in the C++ API.  For more details on what futures are and how they are
 * used, we refer to the
 * <a href="http://en.cppreference.com/w/cpp/thread/future">C++ reference</a>.
 * <p>
 * Objects of this class should always be disposed of with {@link #close} when
 * they are no longer needed, to avoid resource leaks in the underlying native
 * code. (A nice, automated way to do this is to use
 * <a href="https://docs.oracle.com/javase/tutorial/essential/exceptions/tryResourceClose.html">the
 * try-with-resources statement</a>.)
 */
public abstract class Future implements AutoCloseable
{
    static
    {
        System.loadLibrary("jdsb");
    }

    /**
     * A future with no particular result type/value.
     * <p>
     * This object may be used to figure out simply whether the asynchronous
     * operation succeeded or not, by calling {@link #get}.
     */
    public static class Void extends Future
    {
        Void(long ptr) { super(ptr); }

        /**
         * Blocks until the asynchronous operation completes, and throws an
         * exception if it failed.
         * <p>
         * This function may only be called once.
         *
         * @throws Exception if the asynchronous operation failed and an
         *      exception was stored in the shared state.
         */
        public void get() throws Exception
        {
            if (getNativePtr() == 0) throw new IllegalStateException();
            try {
                getValueNative(getNativePtr());
            } finally {
                close();
            }
        }

        private static native void getValueNative(long selfPtr) throws Exception;
    }

    /** A future result of type {@link com.sfh.dsb.SlaveID}. */
    public static class SlaveID extends Future
    {
        SlaveID(long ptr) { super(ptr); }

        /**
         * Blocks until the future has a valid result and retrieves it.
         * <p>
         * This function may only be called once.
         *
         * @throws Exception if the asynchronous operation failed and an
         *      exception was stored in the shared state.
         */
        public com.sfh.dsb.SlaveID get() throws Exception
        {
            if (getNativePtr() == 0) throw new IllegalStateException();
            try {
                return new com.sfh.dsb.SlaveID(getValueNative(getNativePtr()));
            } finally {
                close();
            }
        }

        private static native int getValueNative(long selfPtr) throws Exception;
    }

    protected Future(long nativePtr)
    {
        if (nativePtr == 0) throw new IllegalArgumentException();
        this.nativePtr = nativePtr;
    }

    /**
     * Releases native resources (such as memory) associated with this object.
     * <p>
     * After this function has been called, any attempt to use the class may
     * result in an {@link IllegalStateException}.
     */
    public void close()
    {
        if (nativePtr != 0) {
            destroyNative(nativePtr);
            nativePtr = 0;
        }
    }

    /**
     * Blocks until the result becomes available.
     * <p>
     * After this, it is guaranteed that <code>get()</code> does not block.
     * <p>
     * This function may only be called <em>before</em> the result has been
     * retrieved with <code>get()</code>.
     */
    public void waitForResult()
    {
        if (nativePtr == 0) throw new IllegalStateException();
        waitForResultNative(nativePtr);
    }

    /**
     * Blocks until the result becomes available or the specified number of
     * milliseconds have passed, whichever happens first.
     * <p>
     * This function may only be called <em>before</em> the result has been
     * retrieved with <code>get()</code>.
     *
     * @return Whether the result is available.
     */
    public boolean waitForResult(int timeout_ms)
    {
        if (nativePtr == 0) throw new IllegalStateException();
        return waitForResultNative(nativePtr, timeout_ms);
    }

    // =========================================================================

    protected long getNativePtr() { return nativePtr; }

    private static native void destroyNative(long selfPtr);
    private static native void waitForResultNative(long selfPtr);
    private static native boolean waitForResultNative(long selfPtr, int milliseconds);

    private long nativePtr = 0;
}
