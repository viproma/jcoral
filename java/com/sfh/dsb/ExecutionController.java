package com.sfh.dsb;

import com.sfh.dsb.DomainLocator;
import com.sfh.dsb.ExecutionLocator;
import com.sfh.dsb.Future;
import com.sfh.dsb.SlaveID;
import com.sfh.dsb.VariableSetting;


public final class ExecutionController implements AutoCloseable
{
    static
    {
        System.loadLibrary("jdsb");
    }

    public enum StepResult { COMPLETE, FAILED }

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
        CheckSelf();
        beginConfigNative(nativePtr);
    }

    public void endConfig() throws Exception
    {
        CheckSelf();
        endConfigNative(nativePtr);
    }

    public void setSimulationTime(double startTime) throws Exception
    {
        CheckSelf();
        setSimulationTimeNative(nativePtr, startTime);
    }

    public void setSimulationTime(double startTime, double stopTime)
        throws Exception
    {
        CheckSelf();
        setSimulationTimeNative(nativePtr, startTime, stopTime);
    }

    public Future.SlaveID addSlave(SlaveLocator slaveLocator, int commTimeout_ms)
        throws Exception
    {
        CheckSelf();
        return new Future.SlaveID(
            addSlaveNative(nativePtr, slaveLocator.getNativePtr(), commTimeout_ms));
    }

    public Future.Void setVariables(
        SlaveID slave,
        Iterable<VariableSetting> variableSettings,
        int timeout_ms)
        throws Exception
    {
        CheckSelf();
        return new Future.Void(
            setVariablesNative(nativePtr, slave.getID(), variableSettings, timeout_ms));
    }

    public StepResult step(double stepSize, int timeout_ms) throws Exception
    {
        CheckSelf();
        if (stepSize < 0.0) throw new IllegalArgumentException("Negative step size");
        if (timeout_ms <= 0) throw new IllegalArgumentException("Nonpositive timeout");
        return stepNative(nativePtr, stepSize, timeout_ms)
            ? StepResult.COMPLETE
            : StepResult.FAILED;
    }

    public void acceptStep(int timeout_ms) throws Exception
    {
        CheckSelf();
        if (timeout_ms <= 0) throw new IllegalArgumentException("Nonpositive timeout");
        acceptStepNative(nativePtr, timeout_ms);
    }


    // =========================================================================

    private void CheckSelf()
    {
        if (nativePtr == 0) {
            throw new IllegalStateException("ExecutionController has been closed");
        }
    }

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
    private static native long setVariablesNative(
        long selfPtr,
        int slaveID,
        Iterable<VariableSetting> variableSettings,
        int timeout_ms)
        throws Exception;
    private static native boolean stepNative(
        long selfPtr, double stepSize, int timeout_ms)
        throws Exception;
    private static native void acceptStepNative(long selfPtr, int timeout_ms)
        throws Exception;

    long nativePtr = 0;
}
