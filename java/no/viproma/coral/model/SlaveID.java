/* Copyright 2014-2017, SINTEF Ocean.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
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
