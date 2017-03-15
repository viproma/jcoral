package no.viproma.coral.master;


/**
 * An interface for classes that may monitor and possibly interrupt the
 * progress of {@link Execution#simulate}.
 */
public interface SimulationProgressMonitor
{
    /**
     * Monitors and/or interrupts the progress of {@link Execution#simulate}.
     * <p>
     * This function is called once per time step.
     *
     * @param t
     *      The current logical time
     *
     * @return Whether the simulation should continue.
     */
    public boolean progress(double t) throws Exception;
}
