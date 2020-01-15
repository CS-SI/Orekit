<!--- Copyright 2002-2019 CS Systèmes d'Information
  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->

# Data contexts

The Orekit library relies on some external data for physical models. Typical data
are the Earth Orientation Parameters and the leap seconds history, both being
provided by the IERS or the planetary ephemerides provided by JPL or IMCCE. Such data
can be stored in text or binary files with specific formats that Orekit knows how to
find and read, or can be stored in a database to which user application can connect.
These data can also be completely embedded within an application built on top of Orekit.

One set of consistent data is referred to as a data context.

Regular standalone applications will use only one data context, i.e. one set of EOP,
one set of JPL ephemerides, one history of leap seconds... But only one data context
is not enough for some needs such as:

  - server applications that may answer to request from completely different clients
    with different needs,
  - research or analysis programs that need to compare computation results using
    different assumptions, for example different sets of EOP
  - converters that need to modify data computed in one context so it can be used in
    another context, for example to update coordinates computed with rapid EOP when
    final EOP become available

Orekit must be configured appropriately to select the proper context for each task.

## Contextualized data vs. fixed data

Data that is only a few tens of kilobytes and can be assumed to never change like
the constants for predefined precession-nutation models are already embedded in the core
library, they are not associated with any data context. Small and simple data sets are
defined by setting constants in the code itself. This is the case for the 32.184 seconds
offset between Terrestrial Time and International Atomic Time scales for example. Large
or complex fixed data are embedded by copying the corresponding resource files as is inside
the compiled jar, under the assets directory. This is the case for the IAU-2000
precession-nutation model tables for example. There is nothing to configure for these
fixed data sets as they are embedded within the library, so users may ignore them completely.

Other types of data sets correspond to either huge or changing data sets. These cannot
realistically be embedded within a specific version of the library in the long run. A
typical example for such data set is Earth Orientation Parameters which are mandatory for
accurate frames conversions. Another example is planetary ephemerides. IERS regularly
publishes new Earth Orientation Parameter files covering new time ranges whereas planetary
ephemerides represent megabytes-sized files. These data sets must be provided by users
through a data context and Orekit must be configured to find and load them at run time.

## Context handling in Orekit

The following class diagram shows how contexts are handled in Orekit.

![data class diagram](../images/design/data-context-class-diagram.png)

The top level interface is `DataContext`. It gathers links to all the factories Orekit uses
to create the physical models that themselves require loading data. There is one
factory for `TimeScales` that will need UTC-TAI history data for creating the `UTC` time
scale, and EOP data for creating the `UT1` time scale. There is one factory for frames
that will need EOP data for Earth frame. There is one factory for celestial bodies that
will need planetary ephemerides data for all bodies.

The `DataContext` interface has one implementation `LazyLoadedDataContext` that loads data
on the fly from external resources (typically files) when first accessed and relies on a
`DataProvidersManager` in order to find how to locate and parse these resources.

Users can provide their own implementation of `DataContext`, for example if all the data
is stored in a mission-specific database, which would not fit with the resources or
files-based `LazyLoadedDataContext` and `DataProvidersManager`.

Providing an implementation of `DataContext` consists in creating custom factories and
setting up one class to get a single access point to all of them. The predefined `LazyLoadedDataContext`
uses `LazyLoadedTimeScales`, `LazyLoadedFrames`, `LazyLoadedEOP` and `LazyLoadedCelestialBodies`
for example, all of them relying on the same `DataProvidersManager` to seek and load data. In
this implementation, some factories depend on other ones as one data context must provide
consistent data. The EOP data for example is used by both `LazyLoadedFrames` (for Earth frames)
and by `LazyLoadedTimeScales` (for `UT1`). The `LazyLoadedFrames` also depends on the `CelestialBodies`
interface (for `ICRF`).

There are some static methods in the various factories (`TimeScales.of(...)`, `Frames.of(...)`
that create factories with preloaded constant data. These method are useful for
users who need to implement their own data context.

## Using a context

An application that needs to use a specific data context must specify it. If the use is
a direct reference to a context-dependent physical model (say the `ITRF` Earth frame), then
a reference to the model is retrieved from the `Frames` factory that is associated with
the desired context:

    Frame itrf = myDataContext.getFrames().getITRF();

Sometimes, the reference is implicit and hidden within Orekit code. In this case, a default
context is used. As an example, creating the builder for a Galileo specialized propagator with
only the `GalileoOrbitalElements` will automatically retrieve both the EME2000 and the ITRF
frames from a default context. If the default context is not the one the user wants to use,
then the builder must be created by giving it explicitly the `Frames` factory to use. There
are therefore many constructors and method that have two signatures, one with no `DataContext`
nor factory, and one with either a `DataContext` or a factory and users must select the
proper constructor/method to call depending on their needs.

This feature is mostly interesting for applications that need multiple contexts.

