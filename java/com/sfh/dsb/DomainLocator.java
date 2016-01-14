package com.sfh.dsb;


/**
 * An opaque class which acquires and contains the information needed to
 * communicate on a domain.
 * <p>
 * Objects of this class should always be disposed of with {@link #close} when
 * they are no longer needed, to avoid resource leaks in the underlying native
 * code. (A nice, automated way to do this is to use
 * <a href="https://docs.oracle.com/javase/tutorial/essential/exceptions/tryResourceClose.html">the
 * try-with-resources statement</a>.)
 */
public final class DomainLocator implements AutoCloseable
{
    static
    {
        System.loadLibrary("jdsb");
    }

    /**
     * Constructor which contacts the domain broker to obtain information
     * about the domain.
     *
     * @param domainAddress
     *      The address of the domain, typically on the form
     *      <code>tcp://hostname[:port]</code>.
     */
    public DomainLocator(String domainAddress) throws Exception
    {
        nativePtr_ = createNative(domainAddress);
    }

    /**
     * Releases native resources (such as memory) associated with this object.
     * <p>
     * After this function has been called, any attempt to use the class may
     * result in an error.
     */
    public void close()
    {
        if (nativePtr_ != 0) {
            destroyNative(nativePtr_);
            nativePtr_ = 0;
        }
    }

    // =========================================================================

    long getNativePtr() { return nativePtr_; }

    private static native long createNative(String domainAddress) throws Exception;
    private static native void destroyNative(long selfPtr);

    private long nativePtr_;
}
