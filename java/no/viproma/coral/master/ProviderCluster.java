/* Copyright 2014-2017, SINTEF Ocean.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package no.viproma.coral.master;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Arrays;

import no.viproma.coral.Logging;
import no.viproma.coral.model.SlaveTypeDescription;
import no.viproma.coral.model.VariableDescription;
import no.viproma.coral.net.SlaveLocator;


/**
 *  A common communication interface to a cluster of slave providers
 *  <p>
 *  This class represents a common interface to several slave providers in a
 *  network.  It can be used to get information about the available slave types
 *  and to instantiate slaves on specific providers.  When an object of this
 *  class is created, it will spawn a background thread that performs the actual
 *  communication with the slave providers.
 *  <p>
 *  Slave providers are discovered automatically by listening for UDP
 *  broadcast messages that they broadcast periodically.
 *  </p>
 *  Objects of this class should always be disposed of with {@link #close} when
 *  they are no longer needed, to avoid resource leaks in the underlying native
 *  code. (A nice, automated way to do this is to use
 *  <a href="https://docs.oracle.com/javase/tutorial/essential/exceptions/tryResourceClose.html">the
 *  try-with-resources statement</a>.)
 *
 *  @see Execution
 */
public class ProviderCluster implements AutoCloseable
{
    static
    {
        System.loadLibrary("jcoral");
    }

    /** Information about a slave type. */
    public static class SlaveType
    {
        /**
         *  Returns the slave type's name.
         *
         *  @deprecated
         *      Use {@code getSlaveTypeDescription().getName()} instead.
         */
        @Deprecated
        public String getName() { return getSlaveTypeDescription().getName(); }

        /**
         *  Returns the slave type's unique identifier.
         *
         *  @deprecated
         *      Use {@code getSlaveTypeDescription().getUUID()} instead.
         */
        @Deprecated
        public String getUUID() { return getSlaveTypeDescription().getUUID(); }

        /**
         *  Returns a textual description of the slave type.
         *
         *  @deprecated
         *      Use {@code getSlaveTypeDescription().getDescription()} instead.
         */
        @Deprecated
        public String getDescription() { return getSlaveTypeDescription().getDescription(); }

        /**
         *  Returns author information.
         *
         *  @deprecated
         *      Use {@code getSlaveTypeDescription().getAuthor()} instead.
         */
        @Deprecated
        public String getAuthor() { return getSlaveTypeDescription().getAuthor(); }

        /**
         *  Returns the particular version of this slave type.
         *
         *  @deprecated
         *      Use {@code getSlaveTypeDescription().getVersion()} instead.
         */
        @Deprecated
        public String getVersion() { return getSlaveTypeDescription().getVersion(); }

        /**
         *  Returns descriptions of each of the slave type's variables.
         *
         *  @deprecated
         *      Use {@code getSlaveTypeDescription().getVariables()} instead.
         */
        @Deprecated
        public Iterable<VariableDescription> getVariables() { return getSlaveTypeDescription().getVariables(); }

        /** Returns a description of this slave type. */
        public SlaveTypeDescription getSlaveTypeDescription()
        {
            return description_;
        }

        /** Returns the IDs of slave providers that provide this slave type. */
        public Iterable<String> getProviders()
        {
            return Arrays.asList(providers_);
        }

        SlaveType(
            SlaveTypeDescription description,
            String[] providers)
        {
            description_ = description;
            providers_ = providers;
        }

        private SlaveTypeDescription description_;
        private String[] providers_;
    }

    /**
     *  Constructor.
     *
     *  @param discoveryEndpoint
     *      The address of the network interface that should be used (which
     *      may be the wildcard address) and the port used for discovering
     *      other entities such as slave providers.  Only IPv4 addresses
     *      are currently supported by Coral.
     */
    public ProviderCluster(InetSocketAddress discoveryEndpoint)
        throws Exception
    {
        nativePtr_ = createNative(
            discoveryEndpoint.getAddress().getHostAddress(),
            discoveryEndpoint.getPort());
    }

    /**
     *  Forwards to {@link #ProviderCluster(InetSocketAddress)} using the
     *  given network interface address and a default port number of 10272.
     *  <p>
     *  Only IPv4 addresses are currently supported.
     */
    public ProviderCluster(InetAddress bindAddress)
        throws Exception
    {
        this(new InetSocketAddress(bindAddress, 10272));
    }

    @Override
    protected void finalize()
    {
        if (nativePtr_ != 0) Logging.logNotClosedOnFinalization(getClass());
    }

    /**
     *  Shuts down the communication interface and releases native resources
     *  (such as memory) associated with this object.
     *  <p>
     *  After this function has been called, any attempt to use the class will
     *  result in an {@link IllegalStateException}.
     */
    @Override
    public void close() throws Exception
    {
        if (nativePtr_ != 0) {
            destroyNative(nativePtr_);
            nativePtr_ = 0;
        }
    }

    /**
     *  Returns the slave types which are offered by all slave providers
     *  discovered so far.
     *  <p>
     *  <strong>Warning:</strong>
     *  After an object of this class has been constructed, it may
     *  take some time for it to discover all slave providers.
     *
     *  @param timeout_ms
     *      The communications timeout used to detect loss of communication
     *      with slave providers.  The value -1 means no timeout.
     *
     *  @return
     *      A sequence of all discovered slave types.
     */
    public Iterable<SlaveType> getSlaveTypes(int timeout_ms) throws Exception
    {
        CheckSelf();
        return Arrays.asList(getSlaveTypesNative(nativePtr_, timeout_ms));
    }

    /**
     *  Requests that a slave be spawned by a specific slave provider.
     *  <p>
     *  <code>timeout_ms</code> specifies how long the slave provider should
     *  wait for the slave to start up before assuming it has crashed or frozen.
     *  The function will wait twice as long as this for the slave provider
     *  to report that the slave has been successfully instantiated before
     *  it assumes the slave provider itself has crashed or the connection
     *  has been lost.  In both cases, an exception is thrown.
     *
     *  @param slaveProviderID
     *      The ID of the slave provider that should instantiate the slave.
     *  @param slaveTypeUUID
     *      The UUID that identifies the type of the slave that is to be
     *      instantiated.
     *  @param timeout_ms
     *      How much time the slave gets to start up.
     *      The value -1 means no limit.
     *
     *  @return
     *      An object that contains the information needed to connect to
     *      the slave, which can be passed to {@link Execution#addSlaves}.
     */
    public SlaveLocator instantiateSlave(
        String slaveProviderID, String slaveTypeUUID, int timeout_ms)
        throws Exception
    {
        CheckSelf();
        return instantiateSlaveNative(
            nativePtr_,
            slaveProviderID,
            slaveTypeUUID,
            timeout_ms);
    }

    /**
     * Forwards to {@link #instantiateSlave}, extracting the slave provider
     * ID and slave type UUID from the given slaveType object.
     * <p>
     * An unspecified slave provider is selected from the list returned
     * by {@link SlaveType#getProviders}.
     */
    public SlaveLocator instantiateSlave(SlaveType slaveType, int timeout_ms)
        throws Exception
    {
        return instantiateSlave(
            slaveType.getProviders().iterator().next(),
            slaveType.getUUID(),
            timeout_ms);
    }


    // -------------------------------------------------------------------------

    private void CheckSelf()
    {
        if (nativePtr_ == 0) {
            throw new IllegalStateException("Object has been closed");
        }
    }

    private static native long createNative(
        String networkInterface, int discoveryPort)
        throws Exception;
    private static native void destroyNative(long selfPtr)
        throws Exception;
    private static native SlaveType[] getSlaveTypesNative(
        long selfPtr, int timeout_ms)
        throws Exception;
    private static native SlaveLocator instantiateSlaveNative(
        long selfPtr, String slaveProviderID, String slaveTypeUUID, int timeout_ms)
        throws Exception;

    private long nativePtr_;
}
