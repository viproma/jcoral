/* Copyright 2018-2018, SINTEF Ocean.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package no.viproma.coral;

import java.util.logging.Level;
import java.util.logging.Logger;


/** Logging facilities. */
public class Logging
{
    /** Returns the name of the logger used by this library. */
    public static String getLoggerName()
    {
        return "no.viproma.coral";
    }

    /** Returns the logger used by this library. */
    public static Logger getLogger()
    {
        return logger_;
    }

    /**
     *  Internal convenience method for logging a specific message.
     *  <p>
     *  This method is only designed for private use by this library.
     */
    public static void logNotClosedOnFinalization(Class<?> sourceClass)
    {
        logger_.logp(
            Level.WARNING,
            sourceClass.getName(),
            "finalize",
            "close() was not called before finalization, so resource was not released properly");
    }

    private static Logger logger_ = Logger.getLogger(getLoggerName());
}
