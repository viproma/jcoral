package com.sfh.dsb;


/**
 * An opaque class which contains the information needed to communicate on
 * a domain.
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
     */
    public DomainLocator(String domainAddress)
    {
        nativePtr = createNative(domainAddress);
    }

    /**
     * Frees all resources associated with the DomainLocator object.
     */
    public void close()
    {
        destroyNative(nativePtr);
        nativePtr = 0;
    }

    long getNativePtr() { return nativePtr; }

    private DomainLocator(long nativePtr)
    {
        this.nativePtr = nativePtr;
    }

    private static native long createNative(String domainAddress);
    private static native void destroyNative(long ptr);

    private long nativePtr;
}
