/* Copyright 2017-2018, SINTEF Ocean.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

/*
 * Type definitions that ensure consistent use of certain types across
 * implementations of native methods for different Java classes, which
 * are typically in different C++ source files.  This is especially
 * useful when a native object is created in one file and deleted in
 * another.
 */
#ifndef JCORAL_COMMON_TYPES_HPP
#define JCORAL_COMMON_TYPES_HPP

#include <memory>


// Forward declarations, so we don't need the Coral headers here.
namespace coral
{
    namespace fmi
    {
        class FMU;
    }
    namespace slave
    {
        class Instance;
    }
}


namespace jcoral
{

using FMU = std::shared_ptr<coral::fmi::FMU>;
using SlaveInstance = std::shared_ptr<coral::slave::Instance>;

} // namespace
#endif // header guard
