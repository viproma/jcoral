package com.sfh.dsb;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.sfh.dsb.DomainLocator;
import com.sfh.dsb.SlaveLocator;
import com.sfh.dsb.VariableDescription;


public class DomainController implements AutoCloseable
{
    static
    {
        System.loadLibrary("jdsb");
    }

    public static class SlaveType
    {
        public String getName() { return name; }
        public String getUUID() { return uuid; }
        public String getDescription() { return description; }
        public String getAuthor() { return author; }
        public String getVersion() { return version; }

        public Iterator<VariableDescription> getVariables()
        {
            return Arrays.asList(variables).iterator();
        }

        public Iterator<String> getProviders()
        {
            return Arrays.asList(providers).iterator();
        }

        private String name;
        private String uuid;
        private String description;
        private String author;
        private String version;
        private VariableDescription[] variables;
        private String[] providers;
    }

    public DomainController(DomainLocator locator)
    {
        nativePtr = ctorNative(locator.getNativePtr());
    }

    public void close()
    {
        closeNative(nativePtr);
        nativePtr = 0;
    }

    public Set<SlaveType> getSlaveTypes()
    {
        return new HashSet<SlaveType>(
            Arrays.asList(getSlaveTypesNative(nativePtr)));
    }

    public SlaveLocator instantiateSlave(
        SlaveType slaveType, int timeout_ms, String provider)
    {
        return new SlaveLocator(instantiateSlaveNative(
            slaveType.getName(), timeout_ms, provider));
    }

    public SlaveLocator instantiateSlave(SlaveType slaveType, int timeout_ms)
    {
        return instantiateSlave(slaveType, timeout_ms, "");
    }

    // -------------------------------------------------------------------------

    private static native long ctorNative(long domainLocatorPtr);
    private static native void closeNative(long selfPtr);
    private static native SlaveType[] getSlaveTypesNative(long selfPtr);
    private static native long instantiateSlaveNative(String slaveUUID, int timeout_ms, String provider);

    private long nativePtr;
}
