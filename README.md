![Orekit logo](https://www.orekit.org/img/orekit-logo.png)

# Orekit

> An accurate and efficient core layer for space flight dynamics applications

[Orekit](https://www.orekit.org) is a low level space dynamics library written
in Java. Orekit is designed to be easily used in very different contexts, from
quick studies up to critical operations. As a library, Orekit provides basic
elements (orbits, dates, attitude, frames, ...) and various algorithms to
handle them (conversions, propagations, pointing, ...).

## Download

### Official releases

[Official Orekit releases](https://gitlab.orekit.org/orekit/orekit/releases)
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
tutorials, Javadoc and a lot of other information is available on the
[Maven site](https://www.orekit.org/site-orekit-development/).

## Getting help

The main communication channel is our [forum](https://forum.orekit.org/). You
can report bugs and suggest new features in our
[issues tracking system](https://gitlab.orekit.org/orekit/orekit/issues).

## Contributing

Orekit exists thanks to the contribution of
[many people](https://gitlab.orekit.org/orekit/orekit/graphs/develop).

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
