![Orekit logo](https://www.orekit.org/img/orekit-logo.png)

# Orekit

> An accurate and efficient core layer for space flight dynamics applications

[Orekit](https://www.orekit.org) is a low level space dynamics library written
in Java. Orekit is designed to be easily used in very different contexts, from
quick studies up to critical operations. As a library, Orekit provides basic
elements (orbits, dates, attitude, frames, ...) and various algorithms to
handle them (conversions, propagations, pointing, events detection, orbit determination ...).

[![](http://img.shields.io/:license-apache-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0.html)
[![](https://sonar.orekit.org/api/project_badges/measure?project=orekit%3Aorekit&metric=alert_status)](https://sonar.orekit.org/dashboard?id=orekit%3Aorekit)
[![](https://sonar.orekit.org/api/project_badges/measure?project=orekit%3Aorekit&metric=coverage)](https://sonar.orekit.org/dashboard?id=orekit%3Aorekit)
[![DOI](https://zenodo.org/badge/DOI/10.5281/zenodo.7249096.svg)](https://doi.org/10.5281/zenodo.7249096)



## Download

### Official releases

[Official Orekit releases](https://gitlab.orekit.org/orekit/orekit/-/releases)
are available on our [Gitlab instance](https://gitlab.orekit.org/). They are
also available in the
[Maven repository](https://mvnrepository.com/artifact/org.orekit/orekit).

### Development version

To get the latest development version, please clone our official repository
and checkout the `develop` branch:

```bash
git clone -b develop https://gitlab.orekit.org/orekit/orekit.git
```
__Note:__ Our official repository is
[mirrored on Github](https://github.com/CS-SI/Orekit).

## Documentation

Project overview, architecture and development, detailed features list,
Javadoc and a lot of other information is available on the
[Maven site](https://www.orekit.org/site-orekit-development/).

## Getting help

The main communication channel is our [forum](https://forum.orekit.org/). You
can report bugs and suggest new features in our
[issues tracking system](https://gitlab.orekit.org/orekit/orekit/issues). When
reporting security issues check the "This issue is confidential" box.

## Contributing

Orekit exists thanks to the contribution of
[many people](https://gitlab.orekit.org/orekit/orekit/graphs/develop).
Please take a look at our
[contributing guidelines](src/site/markdown/contributing.md) if you're
interested in helping!

## Building

Detailed information on how to build Orekit from source either using Maven or
Eclipse is provided in [building.md](src/site/markdown/building.md) file.

## Dependencies

Orekit relies on the following
[FOSS](https://en.wikipedia.org/wiki/Free_and_open-source_software) libraries,
all released under business friendly FOSS licenses.

### Compile-time/run-time dependencies

* [Hipparchus](https://hipparchus.org/), a mathematics library released under
  the Apache License, version 2.1.

### Test-time dependencies

* [JUnit 5](http://www.junit.org/), a widely used unit test framework released
  under the Eclipse Public License, version 1.0.

* [Mockito](https://site.mockito.org/), a mocking framework for unit tests,
  released under MIT license.

More detailed information is available in the
[Maven site](https://www.orekit.org/site-orekit-development/dependencies.html).

## License

Orekit is licensed by [CS GROUP](https://www.c-s.fr/) under
the [Apache License, version 2.0](http://www.apache.org/licenses/LICENSE-2.0.html).
A copy of this license is provided in the [LICENSE.txt](LICENSE.txt) file.
