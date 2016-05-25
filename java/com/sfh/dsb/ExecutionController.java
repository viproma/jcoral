package com.sfh.dsb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import com.sfh.dsb.DomainLocator;
import com.sfh.dsb.ExecutionLocator;
import com.sfh.dsb.Future;
import com.sfh.dsb.SimulationProgressMonitor;
import com.sfh.dsb.SlaveID;
import com.sfh.dsb.VariableSetting;


/**
 * Creates and controls an execution.
 * <p>
 * This class is used to control an execution, i.e. a single simulation run.
 * Its operation is divided into two phases/modes: <em>configuration mode</em>
 * and <em>simulation mode</em>.  The following functions may only be called
 * in configuration mode:
 * <ul>
 *      <li>{@link #setSimulationTime} (only before slaves have been added)</li>
 *      <li>{@link #addSlave}</li>
 *      <li>{@link #setVariables}</li>
 *      <li>{@link #endConfig} (to enter simulation mode)</li>
 * </ul>
 * <p>
 * Similarly, the following functions may only be called in simulation mode:
 * <ul>
 *      <li>{@link #step}</li>
 *      <li>{@link #acceptStep}</li>
 *      <li>{@link #simulate}</li>
 *      <li>{@link #beginConfig} (to return to configuration mode)</li>
 * </ul>
 * <p>
 * A new execution is created using {@link #spawnExecution}, and always starts
 * up in configuration mode.
 * <p>
 * Objects of this class should always be disposed of with {@link #close} when
 * they are no longer needed, to avoid resource leaks in the underlying native
 * code. (A nice, automated way to do this is to use
 * <a href="https://docs.oracle.com/javase/tutorial/essential/exceptions/tryResourceClose.html">the
 * try-with-resources statement</a>.)
 */
public final class ExecutionController implements AutoCloseable
{
    static
    {
        System.loadLibrary("jdsb");
    }

    /**
     * Creates a new execution on the given domain.
     *
     * @param domain
     *      The domain in which the participating slaves have been instantiated.
     * @param executionName
     *      A name for the execution.  If null, a default name is used.
     * @param commTimeout_s
     *      A communications timeout (in seconds) for all participants.  If this
     *      much time passes in which the slaves do not receive any data or
     *      commands (e.g. due to network failure) they will self-terminate.
     *
     * @return
     *      An <code>ExecutionController</code> object for controlling the
     *      execution.
     */
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

    /**
     * Forwards to {@link #spawnExecution}, using a default execution name
     * and a communications timeout of 1 hour.
     */
    public static ExecutionController spawnExecution(DomainLocator domain)
        throws Exception
    {
        return spawnExecution(domain, null, 3600);
    }

    /**
     * Terminates the execution and all slaves, and releases native resources
     * (such as memory) associated with this object.
     * <p>
     * After this function has been called, any attempt to use the object may
     * result in an {@link IllegalStateException}.
     */
    public void close() throws Exception
    {
        if (nativePtr_ != 0) {
            destroyNative(nativePtr_);
            nativePtr_ = 0;
        }
    }

    /** Enters configuration mode. */
    public void beginConfig() throws Exception
    {
        CheckSelf();
        beginConfigNative(nativePtr_);
    }

    /**
     * Leaves configuration mode and enters simulation mode.
     * <p>
     * If there are currently ongoing {@link #addSlave} or {@link #setVariables}
     * operations, this function will block until they are complete.
     * However, it will not throw an exception if such operations fail;
     * their return values must be used to check this.
     */
    public void endConfig() throws Exception
    {
        CheckSelf();
        endConfigNative(nativePtr_);
    }

    /**
     * Sets the start time of the simulation run, and indicates that the
     * end time is indefinite.
     * <p>
     * This function may only be called before slaves have been added.
     */
    public void setSimulationTime(double startTime) throws Exception
    {
        CheckSelf();
        setSimulationTimeNative(nativePtr_, startTime);
        simTime_ = startTime;
    }

    /**
     * Sets the start and stop time of the simulation run.
     * <p>
     * This function may only be called before slaves have been added.
     * <p>
     * Note that the stop time is only used as a hint to the slaves, e.g.
     * if they need to preallocate enough memory or signal that they cannot
     * support the given simulation interval.  It is not required that
     * the stop time be reached, nor is it checked that it is not passed.
     * The user is responsible for staying within the given bounds.
     */
    public void setSimulationTime(double startTime, double stopTime)
        throws Exception
    {
        CheckSelf();
        setSimulationTimeNative(nativePtr_, startTime, stopTime);
        simTime_ = startTime;
    }

    /**
     * Asynchronously adds a slave to the execution.
     * <p>
     * This function returns immediately, and the communication channel is
     * established in a background thread.  The returned Future may be queried
     * to determine the status of the operation.
     * <p>
     * This function may only be called in configuration mode, and the slave
     * must previously have been instantiated with {@link DomainController#instantiateSlave}.
     *
     * @param slaveLocator
     *      Information about how to connect to a slave.
     * @param slaveName
     *      A unique name which will be associated with the slave. This can
     *      only contain alphanumeric characters and underscores, and the
     *      first character must be a letter.  Null or an empty string may
     *      be given, in which case a default name will be used.
     * @param timeout_ms
     *      A timeout (in milliseconds) after which the command is assumed to
     *      have failed, e.g. because the slave is unreachable.
     *
     * @return
     *      An object which can be used to obtain the result of the asynchronous
     *      operation, including a unique ID for the slave.
     */
    public Future.SlaveID addSlave(
        SlaveLocator slaveLocator,
        String slaveName,
        int timeout_ms)
        throws Exception
    {
        CheckSelf();
        return new Future.SlaveID(addSlaveNative(
            nativePtr_, slaveLocator.getNativePtr(), slaveName, timeout_ms));
    }

    /** Forwards to {@link #addSlave} with <code>slaveName = null</code>. */
    public Future.SlaveID addSlave(SlaveLocator slaveLocator, int timeout_ms)
        throws Exception
    {
        return addSlave(slaveLocator, null, timeout_ms);
    }

    /**
     * Asynchronously sets the value(s) of one or more variables, and/or
     * establishes connections between variables, for a single slave.
     * <p>
     * This function returns immediately, and the operation is carried out
     * in a background thread.  The returned Future may be queried to determine
     * the status of the operation.
     * <p>
     * This function may only be called in configuration mode, and the slave
     * must previously have been added with {@link #addSlave}.
     *
     * @param slave
     *      The ID of a slave, as returned by <code>addSlave</code>.
     * @param variableSettings
     *      Variable values and connection specifications.
     * @param timeout_ms
     *      A timeout (in milliseconds) after which the command is assumed to
     *      have failed, e.g. because the slave is unreachable.
     *
     * @return
     *      An object which can be used to check the result of the asynchronous
     *      operation.
     */
    public Future.Void setVariables(
        SlaveID slave,
        Iterable<VariableSetting> variableSettings,
        int timeout_ms)
        throws Exception
    {
        CheckSelf();
        return new Future.Void(
            setVariablesNative(nativePtr_, slave.getID(), variableSettings, timeout_ms));
    }

    /**
     * Initiates a time step.
     * <p>
     * This function will request that the simulation be advanced with the
     * logical time specified by <code>stepSize</code>, and return a value
     * that specifies whether the time step succeeded.  If it succeeds, i.e.,
     * the result is {@link StepResult#COMPLETE}, the operation must be
     * confirmed and completed by calling {@link #acceptStep}.
     * <p>
     * The function may fail in two ways:
     * <ol>
     *  <li>It may return {@link StepResult#FAILED}, which means that one or
     *      more slaves failed to complete a time step of the given length,
     *      but that they might have succeeded with a shorter step length.</li>
     *  <li>It may throw an exception, which signals an irrecoverable error,
     *      e.g. network failure.</li>
     * </ol>
     * <p>
     * <strong>Currently, discarding and retrying time steps are not supported,
     * and both of the above must be considered irrecoverable failures.</strong>
     * In future versions, it will be possible to call a
     * <code>discardStep()</code> function in the first case, to thereafter call
     * <code>step()</code> again with a shorter step length. (This is the reason
     * why two method calls&mdash;<code>step()</code> and
     * <code>acceptStep()</code>&mdash;are required per time step.)
     *
     * @param stepSize
     *      How much the simulation should be advanced in time.
     * @param timeout_ms
     *      A timeout (in milliseconds) after which the command is assumed to
     *      have failed, e.g. because the slave is unreachable.
     *
     * @return
     *      Whether the operation was successful.
     */
    public StepResult step(double stepSize, int timeout_ms) throws Exception
    {
        CheckSelf();
        if (stepSize < 0.0) throw new IllegalArgumentException("Negative step size");
        if (timeout_ms <= 0) throw new IllegalArgumentException("Nonpositive timeout");
        boolean ok = stepNative(nativePtr_, stepSize, timeout_ms);
        lastStepSize_ = stepSize;
        return ok ? StepResult.COMPLETE : StepResult.FAILED;
    }

    /**
     * The result of a {@link #step} method call.
     * <p>
     * See the <code>step()</code> documentation for details.
     */
    public enum StepResult { COMPLETE, FAILED }

    /**
     * Confirms and completes a time step.
     * <p>
     * This method must be called after a {@link #step} call, before any
     * other operations are performed. 
     * See the <code>step()</code> documentation for details.
     */
    public void acceptStep(int timeout_ms) throws Exception
    {
        CheckSelf();
        if (timeout_ms <= 0) throw new IllegalArgumentException("Nonpositive timeout");
        acceptStepNative(nativePtr_, timeout_ms);
        simTime_ += lastStepSize_;
    }

    /** Returns the current simulation time. */
    public double currentTime()
    {
        return simTime_;
    }

    /**
     * Convenience method for performing multiple time steps in sequence.
     * <p>
     * This function will advance the simulation time with the amount specified
     * by <code>duration</code> by repeatedly calling {@link #step} followed by
     * {@link #acceptStep}, using time steps of length <code>stepSize</code>.
     * <p>
     * The <code>scenario</code> parameter may be used to specify actions that
     * take place at certain time points.  The events in the queue should be
     * correctly ordered in time.  Leading elements whose event time is less
     * than the current simulation time will be skipped.  Events will be
     * removed from the queue as they occur (or are skipped).
     * <p>
     * If the time between events, or the time between the last event and the
     * time at which the simulation is set to stop, is not an exact multiple of
     * <code>stepSize</code>, the last time step before an event or before
     * stopping will be shorter. If any of the steps fail for any reason, an
     * exception will be thrown.
     * <p>
     * The progress of the simulation may be monitored, and optionally aborted,
     * by a {@link SimulationProgressMonitor} object.
     *
     * @param duration
     *      How much the simulation time should be advanced.  Must be positive.
     * @param stepSize
     *      The time step size.  Must be positive.
     * @param scenario
     *      A queue of time-ordered events, or <code>null</code> if there are
     *      none.  The queue may not contain <code>null</code> elements.
     * @param stepTimeout_ms
     *      A value which will be used for the <code>timeout_ms</code> argument
     *      to <code>step()</code>.  Must be positive.
     * @param otherTimeout_ms
     *      A value which will be used for the <code>timeout_ms</code> argument
     *      to commands other than <code>step()</code> (e.g.
     *      <code>acceptStep()</code>).
     * @param progressMonitor
     *      An object for monitoring the simulation. May be null if this
     *      functionality is not needed.
     */
    public void simulate(
        double duration,
        double stepSize,
        Queue<ScenarioEvent> scenario,
        int stepTimeout_ms,
        int otherTimeout_ms,
        SimulationProgressMonitor progressMonitor)
        throws Exception
    {
        if (duration <= 0.0) {
            throw new IllegalArgumentException("Nonpositive duration");
        }
        if (stepSize <= 0.0) {
            throw new IllegalArgumentException("Nonpositive step size");
        }
        if (stepTimeout_ms <= 0 || otherTimeout_ms <= 0) {
            throw new IllegalArgumentException("Nonpositive timeout");
        }

        if (progressMonitor != null && !progressMonitor.progress(simTime_)) {
            return;
        }

        if (scenario != null) {
            while (!scenario.isEmpty() &&
                    scenario.element().getTimePoint() < currentTime()) {
                scenario.remove();
            }
        }

        final double endTime = currentTime() + duration;
        final double epsilon = stepSize * 1e-6;
        while (true) {
            final double nextStop = (scenario == null || scenario.isEmpty())
                ? endTime
                : Math.min(scenario.element().getTimePoint(), endTime);
            simulateUntil(
                nextStop, stepSize,
                stepTimeout_ms, otherTimeout_ms,
                progressMonitor);
            if (nextStop >= endTime) break;

            assert(scenario != null);
            beginConfig();
            Map<SlaveID, List<VariableSetting>> settings =
                new HashMap<SlaveID, List<VariableSetting>>();

            do {
                ScenarioEvent event = scenario.element();
                List<VariableSetting> slaveSettings =
                    settings.get(event.getSlaveID());
                if (slaveSettings == null) {
                    slaveSettings = new ArrayList<VariableSetting>();
                    settings.put(event.getSlaveID(), slaveSettings);
                }
                slaveSettings.add(event.getVariableSetting());
                scenario.remove();
            } while (!scenario.isEmpty() &&
                     scenario.element().getTimePoint() < nextStop + epsilon);

            List<Future.Void> setVariablesStatuses = new ArrayList<Future.Void>();
            for (Map.Entry<SlaveID, List<VariableSetting>> entry : settings.entrySet()) {
                setVariablesStatuses.add(setVariables(
                    entry.getKey(),
                    entry.getValue(),
                    otherTimeout_ms));
            }
            for (Future.Void status : setVariablesStatuses) status.get();
            endConfig();
        }
    }

    /**
     * Forwards to {@link #simulate}, with <code>scenario = null</code>.
     *
     * @deprecated  Replaced by {@link #simulate}.
     */
    @Deprecated
    public void simulate(
        double duration,
        double stepSize,
        int stepTimeout_ms,
        int acceptStepTimeout_ms,
        SimulationProgressMonitor progressMonitor)
        throws Exception
    {
        simulate(
            duration, stepSize, null,
            stepTimeout_ms, acceptStepTimeout_ms,
            progressMonitor);
    }

    /** Forwards to {@link #simulate}, with <code>progressMonitor = null</code>. */
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

    private ExecutionController(ExecutionLocator locator) throws Exception
    {
        nativePtr_ = createNative(locator.getNativePtr());
    }

    private void CheckSelf()
    {
        if (nativePtr_ == 0) {
            throw new IllegalStateException("ExecutionController has been closed");
        }
    }

    // Advances the simulation with the given duration and returns false iff
    // the simulation was aborted by the progress monitor.
    private boolean simulateUntil(
        double targetTime,
        double stepSize,
        int stepTimeout_ms,
        int acceptStepTimeout_ms,
        SimulationProgressMonitor progressMonitor)
        throws Exception
    {
        if (targetTime == currentTime()) return true;
        assert(targetTime > currentTime());
        assert(stepSize > 0.0);
        assert(stepTimeout_ms > 0);
        assert(acceptStepTimeout_ms > 0);

        while (currentTime() + stepSize < targetTime) {
            forceStep(stepSize, stepTimeout_ms, acceptStepTimeout_ms);
            if (progressMonitor != null && !progressMonitor.progress(currentTime())) {
                return false;
            }
        }
        forceStep(targetTime - currentTime(), stepTimeout_ms, acceptStepTimeout_ms);
        if (progressMonitor != null && !progressMonitor.progress(currentTime())) {
            return false;
        }
        return true;
    }

    // Combines step and acceptStep into one operation and throws an exception
    // if a step fails.
    private void forceStep(
        double stepSize,
        int stepTimeout_ms,
        int acceptStepTimeout_ms) throws Exception
    {
        if (step(stepSize, stepTimeout_ms) != StepResult.COMPLETE) {
            throw new Exception(
                "The simulation was aborted at t=" + simTime_
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
        long selfPtr, long slaveLocatorPtr, String slaveName, int commTimeout_ms)
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

    private long nativePtr_ = 0;

    private double simTime_ = 0.0;
    private double lastStepSize_ = 0.0;
}
