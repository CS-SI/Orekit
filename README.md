![Orekit logo](https://www.orekit.org/img/orekit-logo.png)

# Orekit

> An accurate and efficient core layer for space flight dynamics applications

[Orekit](https://www.orekit.org) is a low level space dynamics library written
in Java. Orekit is designed to be easily used in very different contexts, from
quick studies up to critical operations. As a library, Orekit provides basic
elements (orbits, dates, attitude, frames, ...) and various algorithms to
handle them (conversions, propagations, pointing, ...).

## Documentation

Project overview, architecture and development, detailed features list,
tutorials, Javadoc and a lot of other information is available on the
[Maven site](https://www.orekit.org/site-orekit-development/).

## Getting help

The main communication channel is our [forum](https://forum.orekit.org/). You
can report bugs and suggest new features in our
[issues tracking system](https://gitlab.orekit.org/orekit/orekit/issues).

## Contributing

If you are interested in participating in the development effort, subscribe to
the [forum](https://forum.orekit.org/) and step up to discuss it. The larger
the community is, the better Orekit will be. The main rule is that everything
intended to be included in Orekit core must be distributed under the Apache
License Version 2.0 (you will be asked to sign a contributor license
agreement).

More information is available in our
[development guidelines](https://www.orekit.org/site-orekit-development/guidelines.html).

## Building

Detailed information on how to build Orekit from source either using Maven or
Eclipse is provided in [building.md](src/site/markdown/building.md) file.

[Official Orekit artifacts](https://mvnrepository.com/artifact/org.orekit/orekit)
are available on Maven public repository and on the
[Orekit web site](http://orekit.org/download.html).

* The [src/main/java](src/main/java) directory contains the library sources.
* The [src/main/resources](src/main/resources) directory contains the library
  data.
* The [src/test/java](src/test/java) directory contains the tests sources.
* The [src/test/resources](src/test/resources) directory contains the tests
  data.
* The [src/tutorials/java](src/tutorials/java) directory contains sources for
  example use of the library.
* The [src/tutorials/resources](src/tutorials/resources) directory contains
  example data.
* The [src/design](src/design) directory contains pieces for a UML model of
  the library.
  
## Dependencies

Orekit relies on the following free software, all released under business
friendly free licenses.

### Compile-time/run-time dependencies

* [Hipparchus](https://hipparchus.org/) from the Hipparchus project released
  under the Apache Software License, version 2

### Test-time dependencies

* [JUnit 4](http://www.junit.org/) from Erich Gamma and Kent Beck released
  under the Eclipse Public License, version 1.0
  
* [Mockito](https://site.mockito.org/) from Szczepan Faber and others,
  released under MIT license.

More detailed information is available in the
[Maven site](https://www.orekit.org/site-orekit-development/dependencies.html).

## License

Orekit is licensed by [CS Systèmes d'Information](https://www.c-s.fr/) under
the [Apache License Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.html).
A copy of this license is provided in the [LICENSE.txt](LICENSE.txt) file.
