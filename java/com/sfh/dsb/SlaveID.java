package com.sfh.dsb;


/**
 * An opaque handle to a slave in an execution.
 */
public class SlaveID
{
    SlaveID(int id) { this.id = id; }

    public int getID() { return id; }

    private int id = -1;
}
