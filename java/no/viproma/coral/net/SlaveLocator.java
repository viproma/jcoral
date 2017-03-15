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
