/* Copyright 2014-2018, SINTEF Ocean.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package no.viproma.coral.net;

/**
 * Contains the information needed to communicate with a slave.
 */
public final class SlaveLocator
{
    /**
     *  Constructor.
     *
     *  @param controlEndpoint
     *      The slave's endpoint for communication with the master.
     *  @param dataPubEndpoint
     *      The slave's endpoint for communication with other slaves.
     */
    public SlaveLocator(String controlEndpoint, String dataPubEndpoint)
    {
        controlEndpoint_ = controlEndpoint;
        dataPubEndpoint_ = dataPubEndpoint;
    }

    /** Returns the slave's endpoint for communication with the master. */
    public String getControlEndpoint() { return controlEndpoint_; }

    /** Returns the slave's endpoint for communication with other slaves. */
    public String getDataPubEndpoint() { return dataPubEndpoint_; }

    String controlEndpoint_;
    String dataPubEndpoint_;
}
