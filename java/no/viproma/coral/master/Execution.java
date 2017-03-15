/* Copyright 2014-2017, SINTEF Ocean.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package no.viproma.coral.master;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import no.viproma.coral.master.ExecutionOptions;
import no.viproma.coral.master.SimulationProgressMonitor;
import no.viproma.coral.model.SlaveID;
import no.viproma.coral.model.VariableSetting;


/**
 *  Creates and controls an execution.
 *  <p>
 *  This class is used to set up and control an execution, i.e. a single
 *  simulation run.  This includes connecting and initialising slaves and
 *  executing time steps.
 *  <p>
 *  Objects of this class should always be disposed of with {@link #close} when
 *  they are no longer needed, to avoid resource leaks in the underlying native
 *  code. (A nice, automated way to do this is to use
 *  <a href="https://docs.oracle.com/javase/tutorial/essential/exceptions/tryResourceClose.html">the
 * try-with-resources statement</a>.)
 */
public final class Execution implements AutoCloseable
{
    static
    {
        System.loadLibrary("jcoral");
    }

    /**
     *  Constructor which creates a new execution.
     *
     *  @param executionName
     *      A (preferably unique) name for the execution.
     *  @param options
     *      Configuration settings for the execution.
     */
    public Execution(String executionName, ExecutionOptions options)
        throws Exception
    {
        nativePtr_ = createNative(executionName, options);
        simTime_ = options.getStartTime();
    }

    /**
     * Forwards to {@link #Execution} using an {@link ExecutionOptions}
     * object with default values.
     */
    public Execution(String executionName) throws Exception
    {
        this(executionName, new ExecutionOptions());
    }

    /**
     * Terminates the execution and releases native
     * resources (such as memory) associated with this object.
     * <p>
     * After this function has been called, any attempt to use the object
     * will result in an {@link IllegalStateException}.
     */
    public void close() throws Exception
    {
        if (nativePtr_ != 0) {
            destroyNative(nativePtr_);
            nativePtr_ = 0;
        }
    }

    /**
     *  Adds new slaves to the execution.
     *  <p>
     *  On input, <code>slavesToAdd</code> must refer to a sequence of
     *  slaves to add, the name and location of each specified in an
     *  {@link AddedSlave} object.  When the function returns successfully,
     *  these objects will have been updated with the ID numbers assigned
     *  to the respective slaves, and {@link AddedSlave#getID} may be
     *  called to obtain these.
     *  <p>
     *  If the function throws an exception, and the error is related to
     *  one or more of the slaves, the {@link AddedSlave#getError} method
     *  of the corresponding <code>AddedSlave</code> objects will return
     *  textual descriptions of the errors.
     *
     *  @param slavesToAdd
     *      A sequence of slaves to add.  If empty, the function returns
     *      vacuously.  The <code>AddedSlave</code> object will have been
     *      updated with information about the slaves on return.
     *  @param timeout_ms
     *      The communications timeout used to detect loss of communication
     *      with slaves.  The value -1 means no timeout.
     */
    public void addSlaves(Iterable<AddedSlave> slavesToAdd, int timeout_ms)
        throws Exception
    {
        CheckSelf();
        addSlavesNative(nativePtr_, slavesToAdd, timeout_ms);
    }

    /**
     *  Sets input variable values and establishes connections between
     *  output and input variables.
     *  <p>
     *  On input, <code>slaveConfigs</code> must refer to a sequence of
     *  slaves whose variables are to be modified, (re)connected and/or
     *  disconnected.  It must contain exactly one {@link SlaveConfig}
     *  object for each slave whose configuration is to be changed.
     *  <p>
     *  When a connection is made between an output variable and an input
     *  variable, or such a connection is to be broken, this is specified
     *  in the <code>SlaveConfig</code> object for the slave which owns
     *  the <em>input</em> variable.
     *  <p>
     *  If the function throws an exception, and the error originates in
     *  one or more of the slaves, the {@link SlaveConfig#getError} method
     *  of the corresponding <code>SlaveConfig</code> objects will return
     *  textual descriptions of the errors.
     *
     *  @param slaveConfigs
     *      A sequence of slave configuration change specifications,
     *      at most one entry per slave.
     *  @param timeout_ms
     *      The communications timeout used to detect loss of communication
     *      with the slaves.  The value -1 means no timeout.
     */
    public void reconfigure(Iterable<SlaveConfig> slaveConfigs, int timeout_ms)
        throws Exception
    {
        CheckSelf();
        reconfigureNative(nativePtr_, slaveConfigs, timeout_ms);
    }

    /**
     *  Initiates a time step.
     *  <p>
     *  This function requests that the simulation be advanced with the
     *  logical time specified by <code>stepSize</code>.  It returns a value
     *  that specifies whether the slaves succeeded in performing their
     *  calculations for the time step.  If the step was successful,
     *  i.e., the result is {@link StepResult#COMPLETE}, the operation may
     *  be confirmed and completed by calling {@link #acceptStep}.
     *  <p>
     *  The function may fail in two ways:
     *  <ol>
     *   <li>It may return {@link StepResult#FAILED}, which means that one
     *       or more slaves failed to complete a time step of the given
     *       length, but that they might have succeeded with a shorter step
     *       length.</li>
     *   <li>It may throw an exception, which signals an irrecoverable error,
     *       e.g. network failure.</li>
     *  </ol>
     *  <p>
     *  <strong>Currently, discarding and retrying time steps are not
     *  supported, and both of the above must be considered irrecoverable
     *  failures.</strong> In future versions, it will be possible to call
     *  a <code>discardStep()</code> function in the first case, to
     *  thereafter call <code>step()</code> again with a shorter step
     *  length. (This is the reason why two function calls,
     *  <code>step()</code> and <code>acceptStep()</code>, are required
     *  per time step.)
     *
     *  @param stepSize
     *      How much the simulation should be advanced in time.
     *      This must be a positive number.
     *  @param timeout_ms
     *      The communications timeout used to detect loss of communication
     *      with slaves.  The value -1 means no timeout.
     *
     *  @return
     *      Whether the operation was successful.
     */
    public StepResult step(double stepSize, int timeout_ms) throws Exception
    {
        CheckSelf();
        if (stepSize < 0.0) throw new IllegalArgumentException("Negative step size");
        boolean ok = stepNative(nativePtr_, stepSize, timeout_ms);
        lastStepSize_ = stepSize;
        return ok ? StepResult.COMPLETE : StepResult.FAILED;
    }

    /**
     *  The result of a {@link #step} method call.
     *  <p>
     *  See the <code>step()</code> documentation for details.
     */
    public enum StepResult { COMPLETE, FAILED }

    /**
     *  Confirms and completes a time step.
     *  <p>
     *  This method must be called after a successful {@link #step} call,
     *  before any other operations are performed.
     *  See the <code>step()</code> documentation for details.
     *
     *  @param timeout_ms
     *      The communications timeout used to detect loss of communication
     *      with slaves.  The value -1 means no timeout.
     */
    public void acceptStep(int timeout_ms) throws Exception
    {
        CheckSelf();
        acceptStepNative(nativePtr_, timeout_ms);
        simTime_ += lastStepSize_;
    }

    /** Returns the current simulation time. */
    public double currentTime()
    {
        return simTime_;
    }

    /**
     *  Convenience method for performing multiple time steps in sequence.
     *  <p>
     *  This function will advance the simulation time with the amount specified
     *  by <code>duration</code> by repeatedly calling {@link #step} followed by
     *  {@link #acceptStep}, using time steps of length <code>stepSize</code>.
     *  <p>
     *  The <code>scenario</code> parameter may be used to specify actions that
     *  take place at certain time points.  The events in the queue should be
     *  correctly ordered in time.  Leading elements whose event time is less
     *  than the current simulation time will be skipped.  Events will be
     *  removed from the queue as they occur (or are skipped).
     *  <p>
     *  If the time between events, or the time between the last event and the
     *  time at which the simulation is set to stop, is not an exact multiple of
     *  <code>stepSize</code>, the last time step before an event or before
     *  stopping will be shorter. If any of the steps fail for any reason, an
     *  exception will be thrown.
     *  <p>
     *  The progress of the simulation may be monitored, and optionally aborted,
     *  by a {@link SimulationProgressMonitor} object.
     *
     *  @param duration
     *      How much the simulation time should be advanced.
     *      This must be a positive number.
     *  @param stepSize
     *      The time step size.  This must be a positive number.
     *  @param scenario
     *      A queue of time-ordered events, or <code>null</code> if there are
     *      none.  The queue may not contain <code>null</code> elements.
     *  @param stepTimeout_ms
     *      A value which will be used for the <code>timeout_ms</code> argument
     *      to <code>step()</code>.
     *  @param otherTimeout_ms
     *      A value which will be used for the <code>timeout_ms</code> argument
     *      to commands other than <code>step()</code> (e.g.
     *      <code>acceptStep()</code>).
     *  @param progressMonitor
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

            Map<SlaveID, List<VariableSetting>> varSettings =
                new HashMap<SlaveID, List<VariableSetting>>();

            do {
                ScenarioEvent event = scenario.element();
                List<VariableSetting> vs = varSettings.get(event.getSlaveID());
                if (vs == null) {
                    vs = new ArrayList<VariableSetting>();
                    varSettings.put(event.getSlaveID(), vs);
                }
                vs.add(event.getVariableSetting());
                scenario.remove();
            } while (!scenario.isEmpty() &&
                     scenario.element().getTimePoint() < nextStop + epsilon);

            List<SlaveConfig> slaveConfigs = new ArrayList<SlaveConfig>();
            for (Map.Entry<SlaveID, List<VariableSetting>> svs :
                    varSettings.entrySet()) {
                slaveConfigs.add(new SlaveConfig(svs.getKey(), svs.getValue()));
            }
            reconfigure(slaveConfigs, otherTimeout_ms);
        }
    }

    /**
     *  Forwards to {@link #simulate}, with <code>scenario = null</code> and
     *  <code>progressMonitor = null</code>.
     */
    public void simulate(
        double duration,
        double stepSize,
        int stepTimeout_ms,
        int acceptStepTimeout_ms)
        throws Exception
    {
        simulate(duration, stepSize, null,
                 stepTimeout_ms, acceptStepTimeout_ms, null);
    }


    // =========================================================================

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

    private static native long createNative(
        String executionName, ExecutionOptions options)
        throws Exception;
    private static native void destroyNative(long selfPtr) throws Exception;
    private static native void addSlavesNative(
        long selfPtr, Iterable<AddedSlave> slavesToAdd, int commTimeout_ms)
        throws Exception;
    private static native void reconfigureNative(
        long selfPtr, Iterable<SlaveConfig> slaveConfigs, int commTimeout_ms)
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
