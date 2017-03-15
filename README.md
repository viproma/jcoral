JCoral
======
JCoral is a Java interface to [Coral], a C++ library for performing
distributed co-simulations.  The names and structure of JCoral's packages
and classes closely matches those of the C++ API.

JCoral does not yet have full support for all functionality in Coral,
but this is in the works.  Currently, it is primarily the master API
which has been implemented.

However, JCoral adds some very convenient functionality which is not
(yet!) found in Coral, most notably the `no.viproma.coral.master.ModelBuilder`
and `no.viproma.coral.master.ScenarioBuilder` classes, which significantly
simplify the process of setting up new simulations.

Getting involved
----------------
If you have a question, a bug report or an enhancement request, please
use the [GitHub issue tracker].  We appreciate if you do a quick search
first to see if anyone has already brought the issue up, and we will
also be very happy if you label your issue appropriately.

Contributions are very welcome, and should be submitted as
[pull requests on GitHub].

Requirements
------------
To build JCoral, you first and foremost need the Coral library and
its compile-time dependencies.  Furthermore, the following tools
are needed:

  - Java SE Development Kit 7 or newer
  - CMake 3.0 or newer
  - Windows: Visual Studio 2013 or newer
  - Linux: GCC 4.9 or newer

Note that the Coral library will be linked statically into the shared
library which forms the native part of JCoral.  For Visual Studio, this
means that the two must be compiled using the same compiler configuration.

Licence
-------
JCoral is subject to the terms of the [Mozilla Public License, v. 2.0].
For easily-understandable information about what this means for you,
check out the [MPL 2.0 FAQ].

[Coral]: https://github.com/viproma/coral
[GitHub issue tracker]: https://github.com/viproma/jcoral/issues
[pull requests on GitHub]: https://github.com/viproma/jcoral/pulls
[Mozilla Public License, v. 2.0]: https://www.mozilla.org/MPL/2.0/
[MPL 2.0 FAQ]: https://www.mozilla.org/MPL/2.0/FAQ/
