package com.sfh.dsb;


/**
 * An opaque handle to a slave in an execution.
 *
 * @see ExecutionController#addSlave
 */
public class SlaveID
{
    SlaveID(int id) { this.id = id; }

    int getID() { return id; }

    private int id = -1;
}
