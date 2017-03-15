/* Copyright 2014-2017, SINTEF Ocean.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package no.viproma.coral.net;

/**
 * An opaque class which contains the information needed to communicate with
 * a slave.
 */
public final class SlaveLocator
{
    SlaveLocator(String controlEndpoint, String dataPubEndpoint)
    {
        controlEndpoint_ = controlEndpoint;
        dataPubEndpoint_ = dataPubEndpoint;
    }

    String getControlEndpoint() { return controlEndpoint_; }
    String getDataPubEndpoint() { return dataPubEndpoint_; }

    String controlEndpoint_;
    String dataPubEndpoint_;
}
