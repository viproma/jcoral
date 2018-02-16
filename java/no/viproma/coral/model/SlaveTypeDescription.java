/* Copyright 2018-2018, SINTEF Ocean.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package no.viproma.coral.model;

import java.util.Arrays;
import no.viproma.coral.model.VariableDescription;


/** Information about a slave type. */
public class SlaveTypeDescription
{
    /** Returns the slave type's name. */
    public String getName() { return name_; }

    /** Returns the slave type's unique identifier. */
    public String getUUID() { return uuid_; }

    /** Returns a textual description of the slave type. */
    public String getDescription() { return description_; }

    /** Returns author information. */
    public String getAuthor() { return author_; }

    /** Returns the particular version of this slave type. */
    public String getVersion() { return version_; }

    /** Returns descriptions of each of the slave type's variables. */
    public Iterable<VariableDescription> getVariables()
    {
        return Arrays.asList(variables_);
    }

    SlaveTypeDescription(
        String name,
        String uuid,
        String description,
        String author,
        String version,
        VariableDescription[] variables)
    {
        name_ = name;
        uuid_ = uuid;
        description_ = description;
        author_ = author;
        version_ = version;
        variables_ = variables;
    }

    private String name_;
    private String uuid_;
    private String description_;
    private String author_;
    private String version_;
    private VariableDescription[] variables_;
}
