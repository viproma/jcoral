package no.viproma.coral.model;


/**
 * A property of a variable that defines its time dependency.
 * <p>
 * The causality defines the time instants when a variable's value may change
 * or be changed.  The different categories match those defined by FMI.  In
 * <i>FMI for co-simulation</i>, which is the relevant aspect of the standard
 * for our purposes, the categories <i>tunable</i>, <i>discrete</i> and
 * <i>continuous</i> are in principle more or less equivalent, in that they
 * all indicate that a variable's value may change at communication points.
 * However, convention dictates that <i>discrete</i> variables should be from
 * "real" sampled data systems and <i>continuous</i> variables should be
 * related to differential equations.
 */
public enum Variability
{
    /** The value of the variable never changes. */
    CONSTANT,

    /** The value of the variable is fixed after initialisation. */
    FIXED,

    /** The value of the variable may change at communication points. */
    TUNABLE,

    /**
     * The value of the variable may change at communication points,
     * and it is typically from a "real" sampled data system.
     */
    DISCRETE,

    /**
     * The value of the variable may change at communication points,
     * and it is typically related to differential equations.
     * <p>
     * Only real-valued variables (see {@link DataType#REAL}) may be
     * classified as continuous.
     */
    CONTINUOUS
}
