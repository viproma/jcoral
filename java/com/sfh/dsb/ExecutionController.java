package com.sfh.dsb;

import com.sfh.dsb.DomainLocator;
import com.sfh.dsb.ExecutionLocator;
import com.sfh.dsb.Future;
import com.sfh.dsb.SimulationProgressMonitor;
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
        simTime = startTime;
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
        boolean ok = stepNative(nativePtr, stepSize, timeout_ms);
        lastStepSize = stepSize;
        return ok ? StepResult.COMPLETE : StepResult.FAILED;
    }

    public void acceptStep(int timeout_ms) throws Exception
    {
        CheckSelf();
        if (timeout_ms <= 0) throw new IllegalArgumentException("Nonpositive timeout");
        acceptStepNative(nativePtr, timeout_ms);
        simTime += lastStepSize;
    }

    public void simulate(
        double duration,
        double stepSize,
        int stepTimeout_ms,
        int acceptStepTimeout_ms,
        SimulationProgressMonitor progressMonitor)
        throws Exception
    {
        if (duration <= 0.0) {
            throw new IllegalArgumentException("Nonpositive duration");
        }
        if (duration > 0.0 && stepSize <= 0.0) {
            throw new IllegalArgumentException("Nonpositive step size");
        }
        if (stepTimeout_ms <= 0.0 || acceptStepTimeout_ms <= 0) {
            throw new IllegalArgumentException("Nonpositive timeout");
        }

        if (progressMonitor != null && !progressMonitor.progress(simTime, 0.0)) {
            return;
        }
        double t = 0.0;
        while (t+stepSize < duration) {
            forceStep(stepSize, stepTimeout_ms, acceptStepTimeout_ms);
            t += stepSize;
            if (progressMonitor != null && !progressMonitor.progress(simTime, t/duration)) {
                return;
            }
        }
        forceStep(duration-t, stepTimeout_ms, acceptStepTimeout_ms);
        if (progressMonitor != null && !progressMonitor.progress(simTime, 1.0)) {
            return;
        }
    }

    public void simulate(
        double duration,
        double stepSize,
        int stepTimeout_ms,
        int acceptStepTimeout_ms)
        throws Exception
    {
        simulate(duration, stepSize, stepTimeout_ms, acceptStepTimeout_ms, null);
    }

    // =========================================================================

    private void CheckSelf()
    {
        if (nativePtr == 0) {
            throw new IllegalStateException("ExecutionController has been closed");
        }
    }

    private void forceStep(
        double stepSize,
        int stepTimeout_ms,
        int acceptStepTimeout_ms) throws Exception
    {
        if (step(stepSize, stepTimeout_ms) != StepResult.COMPLETE) {
            throw new Exception(
                "The simulation was aborted at t=" + simTime
                + " because one or more slaves failed to complete a time step of length dt="
                + stepSize);
        }
        acceptStep(acceptStepTimeout_ms);
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

    private long nativePtr = 0;

    private double simTime = 0.0;
    private double lastStepSize = 0.0;
}
