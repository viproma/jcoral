package no.viproma.coral.master;


/**
 *  Configuration options for an execution.
 *  <p>
 *  An object of this type may be passed to {@link Execution#Execution}.
 */
public class ExecutionOptions
{
    /**
     *  Constructs an object with default settings.
     */
    public ExecutionOptions()
    {
        startTime_ = 0.0;
        maxTime_ = Double.POSITIVE_INFINITY;
        slaveVariableRecvTimeout_ms_ = 1000;
    }

    /**
     *  Gets the start time of the simulation.
     *  <p>
     *  If not set with {@link #setSimTime}, the default value is zero.
     */
    public double getStartTime() { return startTime_; }

    /**
     *  Gets the maximum simulation time point.
     *  <p>
     *  This may have the special value {@link Double#POSITIVE_INFINITY}
     *  which means that there is no predefined maximum time.
     *  This is also the default value if not set with
     *  {@link #setSimTime}.
     */
    public double getMaxTime() { return maxTime_; }

    /**
     *  Sets the start time and maximum time.
     *  <p>
     *  The maximum time is currently not used by Coral itself,
     *  but may be used by some slaves, e.g. to pre-allocate resources
     *  such as memory.
     *
     *  @param startTime
     *      The start time of the simulation.  This may be any finite
     *      number, as long as it's less than <code>maxTime</code>.
     *  @param maxTime
     *      The maximum simulation time.  This must be greater than
     *      <code>startTime</code>.  The special value
     *      {@link Double#POSITIVE_INFINITY} signifies that there is
     *      no predefined maximum time.
     *
     *  @throws IllegalArgumentException
     *      If <code>startTime</code> is greater than
     *      <code>maxTime</code>, or if either has an illegal value.
     */
    public void setSimTime(double startTime, double maxTime)
    {
        if (Double.NEGATIVE_INFINITY < startTime && startTime < maxTime) {
            startTime_ = startTime;
            maxTime_ = maxTime;
        } else {
            throw new IllegalArgumentException("Invalid simulation time interval");
        }
    }

    /**
     *  Gets the timeout used by the slaves to detect loss of communication
     *  with other slaves.
     *  <p>
     *  This is used when slaves exchange variable values among themselves.
     *  The default is one second.
     */
    public int getSlaveVariableRecvTimeout_ms()
    {
        return slaveVariableRecvTimeout_ms_;
    }

    /**
     *  Sets the timeout used by the slaves to detect loss of communication
     *  with other slaves.
     *  <p>
     *  This is used when slaves exchange variable values among themselves.
     *  The value -1 means no timeout.
     */
    public void setSlaveVariableRecvTimeout_ms(int value)
    {
        if (value >= 0) {
            slaveVariableRecvTimeout_ms_ = value;
        } else {
            throw new IllegalArgumentException("Timeout is negative");
        }
    }

    private double startTime_;
    private double maxTime_;
    private int slaveVariableRecvTimeout_ms_;
}
