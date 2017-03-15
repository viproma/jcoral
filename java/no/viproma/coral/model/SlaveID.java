package no.viproma.coral.model;


/**
 * An opaque handle to a slave in an execution.
 *
 * @see no.viproma.coral.master.Execution#addSlaves
 */
public class SlaveID
{
    SlaveID(int id) { this.id = id; }

    int getID() { return id; }

    private int id = -1;
}
