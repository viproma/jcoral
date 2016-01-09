package com.sfh.dsb;


/**
 * An interface for classes that may monitor and possibly interrupt the
 * progress of {@link ExecutionController#simulate}.
 */
public interface SimulationProgressMonitor
{
    /**
     * Monitors and/or interrupts the progress of <code>simulate()</code>.
     * <p>
     * This function is called once per time step.
     *
     * @param t
     *      The current logical time
     * @param p
     *      The progress in the current <code>simulate()</code> call, expressed
     *      as a number from 0 to 1.
     *
     * @return Whether the simulation should continue.
     */
    public boolean progress(double t, double p) throws Exception;
}
