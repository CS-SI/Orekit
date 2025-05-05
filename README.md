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

# Installation

## 0. Prerequisites

- Java Development Kit (JDK) installed (8 or higher)
- Maven (recommended) or Gradle for dependency management

## 1. Add Orekit as a Dependency

For **Maven**, add the following to your `pom.xml` inside the `<dependencies>` section:

```xml
<!-- https://mvnrepository.com/artifact/org.orekit/orekit -->
<dependency>
    <groupId>org.orekit</groupId>
    <artifactId>orekit</artifactId>
    <version>VERSION_NUMBER</version>
</dependency>
```

For **Gradle**, add this to your `build.gradle`:

```text
implementation 'org.orekit:orekit:VERSION_NUMBER'
```

> **Note:** You can find the available versions
> on [maven repository](https://mvnrepository.com/artifact/org.orekit/orekit)

## 2. Download Orekit Data

Orekit requires external data files (Earth orientation parameters, leap seconds, etc.). To get these data, you can either:

* Download the `Physical Data` on the [Official Orekit website](https://www.orekit.org/download.html) and extract the archive
* Clone the [Official Orekit data repository](https://gitlab.orekit.org/orekit/orekit-data)

## 3. Load the Orekit data

Orekit will most likely require you to load previously downloaded Orekit data. A simple way to do this is to use the
following code snippet:

```java
      final File orekitData = new File("path/to/orekit/data/folder");
      final DataProvider dirCrawler = new DirectoryCrawler(orekitData);
      DataContext.getDefault().getDataProvidersManager().addProvider(dirCrawler);
```

Replace `/path/to/orekit-data` with the actual path to your unzipped data folder.

# Usage

## Keplerian propagation
You will find below an example of a Keplerian propagation :
```java
import org.hipparchus.util.FastMath;
import org.orekit.data.DataContext;
import org.orekit.data.DataProvider;
import org.orekit.data.DirectoryCrawler;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;

import java.io.File;

public class KeplerianPropagation {
  public static void main(String[] args) {

    // Load the Orekit data
    final File orekitData = new File("/path/to/orekit/data/folder");
    if (!orekitData.exists()) {
      System.err.format("Orekit data file not found: %s\n", orekitData.getAbsolutePath());
    }
    final DataProvider dirCrawler = new DirectoryCrawler(orekitData);
    DataContext.getDefault().getDataProvidersManager().addProvider(dirCrawler);

    // Define an arbitrary orbit
    double            sma               = 7000e3; // SMA [m]
    double            ecc               = 0.001;
    double            inc               = FastMath.toRadians(15); //rad
    double            perigeeArgument   = FastMath.toRadians(30); //rad
    double            raan              = FastMath.toRadians(45); //rad
    double            anomaly           = FastMath.toRadians(60); //rad
    PositionAngleType positionAngleType = PositionAngleType.MEAN;
    Frame             inertialFrame     = FramesFactory.getGCRF();
    AbsoluteDate      date              = new AbsoluteDate(2002, 1, 1, 0, 0, 0, TimeScalesFactory.getUTC());
    double            mu                = Constants.EIGEN5C_EARTH_MU;

    Orbit orbit = new KeplerianOrbit(sma, ecc, inc, perigeeArgument, raan, anomaly, positionAngleType, inertialFrame, date,
                                     mu);

    System.out.println(orbit);

    // Set up sma Keplerian propagator
    Propagator propagator = new KeplerianPropagator(orbit);

    // Propagate to one day later
    AbsoluteDate    targetDate      = date.shiftedBy(Constants.JULIAN_DAY);
    SpacecraftState propagatedState = propagator.propagate(targetDate);

    System.out.println(propagatedState.getOrbit());
  }

}
```

You should get the following output when running this example:
```text
Keplerian parameters: {a: 7000000.0; e: 0.001; i: 15.000000000000002; pa: 30.000000000000004; raan: 45.0; v: 60.09930121319573;}
Keplerian parameters: {a: 7000000.0; e: 0.001; i: 15.000000000000002; pa: 30.000000000000004; raan: 45.0; v: 5396.513788732658;}
```

# Going further

## Documentation

Project overview, architecture and development, detailed features list,
Javadoc and a lot of other information is available on the
[Maven site](https://www.orekit.org/site-orekit-development/).

## Getting help

The main communication channel is our [forum](https://forum.orekit.org/). You
can report bugs and suggest new features in our
[issues tracking system](https://gitlab.orekit.org/orekit/orekit/issues). When
reporting security issues check the "This issue is confidential" box.

## Build & Run locally
Want to include your own modifications to Orekit rather than simply relying on it as a dependency ? Please check
out [building.md](src/site/markdown/building.md)

## Python wrapper


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

## Contributing

Orekit exists thanks to the contribution of
[many people](https://gitlab.orekit.org/orekit/orekit/graphs/develop).
Please take a look at our
[contributing guidelines](src/site/markdown/contributing.md) if you're
interested in helping!

## Dependencies

Orekit relies on the following
[Free and Open-Source Software](https://en.wikipedia.org/wiki/Free_and_open-source_software) libraries,
all released under business friendly FOSS licenses.

### Compile-time/run-time dependencies

* [Hipparchus](https://hipparchus.org/), a mathematics library released under
  the Apache License, version 2.0.

### Test-time dependencies

* [JUnit 5](http://www.junit.org/), a widely used unit test framework released
  under the Eclipse Public License, version 1.0.

* [Mockito](https://site.mockito.org/), a mocking framework for unit tests,
  released under MIT license.

More detailed information is available in the
[Maven site](https://www.orekit.org/site-orekit-development/dependencies.html).

# License

Orekit is licensed by [CS GROUP](https://www.cs-soprasteria.com/) under
the [Apache License, version 2.0](http://www.apache.org/licenses/LICENSE-2.0.html).
A copy of this license is provided in the [LICENSE.txt](LICENSE.txt) file.
