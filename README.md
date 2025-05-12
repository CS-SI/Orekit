<h1 align="center">
  <img src="https://www.orekit.org/img/orekit-logo.png" alt="Orekit">

<a href="https://www.orekit.org/doc-javadoc.html">Documentation</a> |
<a href="https://forum.orekit.org/">Forum</a>
</h1>

[![](http://img.shields.io/:license-apache-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0.html)
[![](https://sonar.orekit.org/api/project_badges/measure?project=orekit%3Aorekit&metric=alert_status)](https://sonar.orekit.org/dashboard?id=orekit%3Aorekit)
[![](https://sonar.orekit.org/api/project_badges/measure?project=orekit%3Aorekit&metric=coverage)](https://sonar.orekit.org/dashboard?id=orekit%3Aorekit)
[![DOI](https://zenodo.org/badge/DOI/10.5281/zenodo.7249096.svg)](https://doi.org/10.5281/zenodo.7249096)

> An accurate and efficient core layer for space flight dynamics applications

[Orekit](https://www.orekit.org) is a low level space dynamics library written
in Java. Orekit is designed to be easily used in very different contexts, from
quick studies up to critical operations.

As a library, Orekit provides basic
elements (orbits, dates, attitude, frames, ...) and various algorithms to
handle them (conversions, propagations, pointing, events detection, orbit determination ...).

# Features

- **Accurate Orbit Propagation:**  
  Supports analytical, semianalytical, numerical, and TLE-based propagation.

- **Flexible Orbit and Attitude Models:**  
  Easily switch between Cartesian, Keplerian, circular, and equinoctial orbit representations.
  Includes standard and customizable attitude laws (e.g., nadir, target pointing...).

- **Event Detection:**  
  Built-in detectors for eclipses, ground station visibility...

- **Maneuver Modeling:**  
  Supports impulse and continuous maneuvers with integration into propagation and event detection.

- **Robust Time and Reference Frames:**  
  High-precision time handling with multiple time scales and leap second support.
  Reference frames for Earth-centered and inertial calculations.

- **Orbit Determination:**  
  Tools for orbit fitting, parameter estimation, and measurement processing.

- **Reliable Earth and Environmental Models:**  
  Includes Earth shape and potential for realistic simulations.

- **Standard Format and Data Handling:**  
  Supports reading and writing common space data formats for easy integration and interoperability.

- **Open Source and Easy Integration:**  
  Thanks to its Apache License 2.0.

- [**And much more !**](https://www.orekit.org/site-orekit-development/index.html)

# Installation

## 1. Requirements

Before you begin, make sure you have the following installed:

| Requirement                                | Suggested link                        |
|--------------------------------------------|---------------------------------------|
| **Java Development Kit (JDK)** 8 or higher | https://openjdk.org/install/          |
| **Apache Maven** (build automation)        | https://maven.apache.org/download.cgi |

## 2. Add Orekit as a Dependency

For **Maven**, add the following to your `pom.xml` inside the `<dependencies>` section:

```xml
<!-- https://mvnrepository.com/artifact/org.orekit/orekit -->
<dependency>
    <groupId>org.orekit</groupId>
    <artifactId>orekit</artifactId>
    <version>VERSION_NUMBER</version>
</dependency>
```

> **Note:** You can find the available versions
> on [the maven repository](https://mvnrepository.com/artifact/org.orekit/orekit)

## 3. Download Orekit Data

Orekit requires external data files (Earth orientation parameters, leap seconds, etc.). To get these data, you can either:

* Download the `Physical Data` on the [Official Orekit website](https://www.orekit.org/download.html) and extract the archive
* Clone the [Official Orekit data repository](https://gitlab.orekit.org/orekit/orekit-data)

## 4. Load the Orekit data

Orekit will most likely require you to load previously downloaded Orekit data. A simple way to do this is to use the
following code snippet:

```java
      final File orekitData = new File("path/to/orekit/data/folder");
      final DataProvider dirCrawler = new DirectoryCrawler(orekitData);
      DataContext.getDefault().getDataProvidersManager().addProvider(dirCrawler);
```

Replace `/path/to/orekit-data` with the actual path to your unzipped data folder.

# Getting Started

## Keplerian propagation

In this section, you will learn the building blocks to create a ```KeplerianPropagator```.

> **Note:** It is assumed that the code necessary to load the Orekit data is written at the beginning of your script as
> explained in [Installation](#3-load-the-orekit-data)

### 1. Create an `Orbit`
The first step is to create an `Orbit`. Several types are available in Orekit:
* `KeplerianOrbit`
* `CartesianOrbit`
* `EquinoctialOrbit`
* `CircularOrbit`

For the sake of this example, we will build a `KeplerianOrbit`

```java
double sma = 7000e3; // Semi-major axis [m]
double ecc = 0.001; // Eccentricity [-]
double inc = FastMath.toRadians(15); // Inclination [rad]
double pa = FastMath.toRadians(30); // Perigee Argument [rad]
double raan = FastMath.toRadians(45); // Right Ascension of the Ascending Node[rad]
double anomaly = FastMath.toRadians(60); // Inclination [rad]

PositionAngleType positionAngleType = PositionAngleType.MEAN; // Type of anomaly angle used (MEAN, TRUE, ECCENTRIC)
Frame inertialFrame = FramesFactory.getGCRF(); // Earth-Centered Inertial frame
AbsoluteDate date = new AbsoluteDate(2002, 1, 1, 0, 0, 0, TimeScalesFactory.getUTC()); // Date of the orbit
double mu = Constants.EIGEN5C_EARTH_MU; // Earth's standard gravitational parameter used in EIGEN-5C gravity field model

Orbit orbit = new KeplerianOrbit(sma, ecc, inc, pa, raan, anomaly,
                                 positionAngleType, inertialFrame, date, mu);
```
<details>
<summary>Output</summary>

Displaying this orbit using:
```java
System.out.println(orbit);
```
should output:
```text
Keplerian parameters: {a: 7000000.0; e: 0.001; i: 15.000000000000002; pa: 30.000000000000004; raan: 45.0; v: 60.09930121319573;}
```
</details>

### 2. Create a `Propagator`

Now that we have defined an orbit, we can create a `Propagator` to specify how the orbit will be propagated through
time. 

In this case we will create a basic `KeplerianPropagator`:
```java
Propagator propagator = new KeplerianPropagator(orbit);
```

### 3. Propagate !
You are now ready to propagate this orbit through time. To do so you can specify:
* The initial and final propagation dates, the propagator will propagate in this time interval
* The final propagation date only, the propagator will propagate between internal state date and this final propagation date

We will propagate for one `Constants.JULIAN_DAY` starting from the initial orbit date:
```java
AbsoluteDate targetDate = date.shiftedBy(Constants.JULIAN_DAY);
SpacecraftState propagatedState = propagator.propagate(targetDate);
```
> **Note:** The propagator outputs a `SpacecraftState` which holds the propagated `Orbit`

<details>
<summary>Output</summary>

Displaying final state's orbit using:
```java
System.out.println(propagatedState.getOrbit());
```
should output:
```text
Keplerian parameters: {a: 7000000.0; e: 0.001; i: 15.000000000000002; pa: 30.000000000000004; raan: 45.0; v: 5396.513788732658;}
```
</details>

<details>
<summary>Full java code</summary>

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

    Orbit orbit = new KeplerianOrbit(sma, ecc, inc, perigeeArgument, raan, anomaly, 
                                     positionAngleType, inertialFrame, date, mu);

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
</details>

# Going further

## Tutorials

For more advanced usage of Orekit, check out
the [Official Orekit tutorials repository](https://gitlab.orekit.org/orekit/orekit-tutorials).

## Documentation

The following documentation is available:

* [Latest API documentation](https://www.orekit.org/site-orekit-development/apidocs/index.html)
* [Maven site](https://www.orekit.org/site-orekit-development/) for the project overview, architecture and development,
  detailed features list, Javadoc and a lot of other information

## Getting help

The main communication channel is our [forum](https://forum.orekit.org/). You
can report bugs and suggest new features in our
[issues tracking system](https://gitlab.orekit.org/orekit/orekit/issues). 

> **Note:** When
reporting security issues check the "This issue is confidential" box.

## Build & Run locally
Want to include your own modifications to Orekit rather than simply relying on it as a dependency ? Please check
out [building.md](src/site/markdown/building.md)

## Python wrapper

If interested in the official python wrapper of Orekit, please check
out https://gitlab.orekit.org/orekit-labs/python-wrapper/-/wikis/home

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
