package com.sfh.dsb;

import com.sfh.dsb.DomainLocator;
import com.sfh.dsb.ExecutionLocator;
import com.sfh.dsb.Future;
import com.sfh.dsb.SlaveID;


public final class ExecutionController implements AutoCloseable
{
    static
    {
        System.loadLibrary("jdsb");
    }

    public static ExecutionController spawnExecution(
        DomainLocator domain, String executionName, int commTimeout_s)
        throws Exception
    {
        try (ExecutionLocator loc = new ExecutionLocator(spawnExecutionNative(
            domain.getNativePtr(), executionName, commTimeout_s)))
        {
            return new ExecutionController(loc);
        }
    }

    public static ExecutionController spawnExecution(DomainLocator domain)
        throws Exception
    {
        return spawnExecution(domain, null, 3600);
    }

    ExecutionController(ExecutionLocator locator) throws Exception
    {
        nativePtr = createNative(locator.getNativePtr());
    }

    public void close() throws Exception
    {
        if (nativePtr != 0) {
            destroyNative(nativePtr);
            nativePtr = 0;
        }
    }

    public void beginConfig() throws Exception
    {
        beginConfigNative(nativePtr);
    }

    public void endConfig() throws Exception
    {
        endConfigNative(nativePtr);
    }

    public void setSimulationTime(double startTime) throws Exception
    {
        setSimulationTimeNative(nativePtr, startTime);
    }

    public void setSimulationTime(double startTime, double stopTime)
        throws Exception
    {
        setSimulationTimeNative(nativePtr, startTime, stopTime);
    }

    public Future.SlaveID addSlave(SlaveLocator slaveLocator, int commTimeout_ms)
        throws Exception
    {
        return new Future.SlaveID(
            addSlaveNative(nativePtr, slaveLocator.getNativePtr(), commTimeout_ms));
    }


    // =========================================================================

    private static native long spawnExecutionNative(
        long domainLocatorPtr, String executionName, int commTimeout_s)
        throws Exception;
    private static native long createNative(long locatorPtr) throws Exception;
    private static native void destroyNative(long selfPtr) throws Exception;
    private static native void beginConfigNative(long selfPtr) throws Exception;
    private static native void endConfigNative(long selfPtr) throws Exception;
    private static native void setSimulationTimeNative(
        long selfPtr, double startTime)
        throws Exception;
    private static native void setSimulationTimeNative(
        long selfPtr, double startTime, double stopTime)
        throws Exception;
    private static native long addSlaveNative(
        long selfPtr, long slaveLocatorPtr, int commTimeout_ms)
        throws Exception;

    long nativePtr = 0;
}
