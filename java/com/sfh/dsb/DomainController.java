package com.sfh.dsb;

import java.util.Arrays;

import com.sfh.dsb.DomainLocator;
import com.sfh.dsb.SlaveLocator;
import com.sfh.dsb.VariableDescription;


/**
 * Performs communication with the slave providers in a domain.
 * <p>
 * This class may be used to acquire information about the slave types which are
 * available in a domain, and to create instances of them for an execution.
 * Use {@link ExecutionController} to create and control an execution.
 * <p>
 * Objects of this class should always be disposed of with {@link #close} when
 * they are no longer needed, to avoid resource leaks in the underlying native
 * code. (A nice, automated way to do this is to use
 * <a href=https://docs.oracle.com/javase/tutorial/essential/exceptions/tryResourceClose.html">the
 * try-with-resources statement</a>.)
 *
 * @see ExecutionController
 */
public class DomainController implements AutoCloseable
{
    static
    {
        System.loadLibrary("jdsb");
    }

    /** Information about a slave type. */
    public static class SlaveType
    {
        /** Returns the slave type's name. */
        public String getName() { return name; }

        /** Returns the slave type's unique identifier. */
        public String getUUID() { return uuid; }

        /** Returns a textual description of the slave type. */
        public String getDescription() { return description; }

        /** Returns author information. */
        public String getAuthor() { return author; }

        /** Returns the particular version of this slave type. */
        public String getVersion() { return version; }

        /** Returns descriptions of each of the slave type's variables. */
        public Iterable<VariableDescription> getVariables()
        {
            return Arrays.asList(variables);
        }

        /** Returns the IDs of slave providers that provide this slave type. */
        public Iterable<String> getProviders()
        {
            return Arrays.asList(providers);
        }

        private String name;
        private String uuid;
        private String description;
        private String author;
        private String version;
        private VariableDescription[] variables;
        private String[] providers;
    }

    /**
     * Constructs an object to communicate with slave providers in the given
     * domain.
     * <p>
     * The domain controller will start listening for slave providers from
     * the moment the object has been created.  However, it may take some
     * time before all providers have been discovered, and
     * {@link #getSlaveTypes()} may therefore return an empty list if it is
     * called immediately after construction.  Slave providers typically
     * announce themselves once every second, so a 2-second wait should be
     * sufficient.
     *
     * @param locator
     *      An object which contains information about the domain.
     */
    public DomainController(DomainLocator locator) throws Exception
    {
        nativePtr_ = createNative(locator.getNativePtr());
    }

    /**
     * Closes the connection to the domain and releases native resources (such
     * as memory) associated with this object.
     * <p>
     * After this function has been called, any attempt to use the class may
     * result in an {@link IllegalStateException}.
     */
    public void close() throws Exception
    {
        if (nativePtr_ != 0) {
            destroyNative(nativePtr_);
            nativePtr_ = 0;
        }
    }

    /**
     * Returns a list of the slave types discovered so far.
     * <p>
     * See {@linkplain #DomainController the constructor documentation} for a
     * warning about calling this function too soon after construction.
     */
    public Iterable<SlaveType> getSlaveTypes() throws Exception
    {
        CheckSelf();
        return Arrays.asList(getSlaveTypesNative(nativePtr_));
    }

    /**
     * Requests that a slave be instantiated.
     * <p>
     * After instantiation, the slave will wait to be added to an execution.
     * Use {@link ExecutionController#addSlave} with the returned slave locator
     * to do this.
     *
     * @param slaveType
     *      The slave type of which an instance should be created.
     *      This must be an object acquired by calling {@link #getSlaveTypes}
     *      on the same <code>DomainController</code> object.
     * @param timeout_ms
     *      A time (in milliseconds) after which the slave instantiation is
     *      assumed to have failed.  This could for example happen due to
     *      network issues, but also because the slave itself hung or crashed
     *      during startup.
     * @param provider
     *      The ID of a particular provider which should be used to instantiate
     *      the slave (as returned by {@link SlaveType#getProviders}).  If this
     *      is null, an arbitrary slave provider is used.
     */
    public SlaveLocator instantiateSlave(
        SlaveType slaveType, int timeout_ms, String provider)
        throws Exception
    {
        CheckSelf();
        return new SlaveLocator(instantiateSlaveNative(
            nativePtr_, slaveType.getUUID(), timeout_ms, provider));
    }

    /**
     * Forwards to {@link #instantiateSlave} with <code>provider = null</code>.
     */
    public SlaveLocator instantiateSlave(SlaveType slaveType, int timeout_ms)
        throws Exception
    {
        CheckSelf();
        return instantiateSlave(slaveType, timeout_ms, "");
    }

    // -------------------------------------------------------------------------

    private void CheckSelf()
    {
        if (nativePtr_ == 0) {
            throw new IllegalStateException("ExecutionController has been closed");
        }
    }

    private static native long createNative(long domainLocatorPtr)
        throws Exception;
    private static native void destroyNative(long selfPtr)
        throws Exception;
    private static native SlaveType[] getSlaveTypesNative(long selfPtr)
        throws Exception;
    private static native long instantiateSlaveNative(
        long selfPtr, String slaveUUID, int timeout_ms, String provider)
        throws Exception;

    private long nativePtr_;
}
