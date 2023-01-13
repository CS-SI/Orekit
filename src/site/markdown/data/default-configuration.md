<!--- Copyright 2002-2023 CS GROUP
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

# Default configuration

The default configuration for data handling is to have one [data context](./contexts.html)
that loads data lazily, i.e. on the fly only when each piece of data is first needed.
This was the only possible configuration up to Orekit 10.0, as multiple data contexts
were introduced in version 10.1.

Lazy loading is managed by a `DataProvidersManager` class. This was a singleton up
to Orekit 10.0. Since Orekit 10.1, it is a regular class stored in the `LazyLoadedDataContext`
that corresponds to the default data context.

The `DataProvidersManager` class separates data loading in two phases: retrieving the raw
data located on some storage media, and parsing the data format.

## Data storage

Data sets must be stored at locations where the Orekit library will find them. This may
be simply a directories tree on a disk, but may be almost anything else as this simple
solution would not fit all uses of the library.

The following use cases show different possible data storage strategies. All of them
can be handled by Orekit plugin mechanism. Most of the plugins are already available
in the library itself.

  * Application used from a few operator's desks in a control center without external
    network connections

    In this case, data may be stored in the main operational database, relying on the
    existing administration procedures (updates, backup, redundancy ...).

  * Simulation tool on a desk computer for everyday studies

    For everyday local use of a tool, data will mainly be stored in the user environment.
    A traditional architecture will involve two main data stores, one on a network shared
    disk for large general data sets handled at department level and another one as simple
    files on the local user disk where he or she can put custom data sets for specific
    purposes. The local data files may be set up in order to override system-level values for
    special cases.

  * New program added to an existing tools suite

    If a program using Orekit is integrated in an existing environment with its own
    established data management system, the library must be configured to use this existing
    system to retrieve the existing data instead of using Orekit's own internal system. This
    enables smoother integration. It also simplifies system administration of the complete
    suite and avoids data duplication.

  * Standalone application on a small networked device

    An application used on a small device with network access (say a mobile phone), may
    be simpler to set up and use if it does not store the data at all on the device
    itself but retrieves it on the fly from the web when needed.

  * Computation service in an application server

    A service installed on an application server may be simpler to configure if, rather
    than using explicit files locations on the server, one stores the data in the
    application classpath where it will be managed by the application server together
    with the application code itself.

## Data formats

In order to simplify data updates for users and to avoid transformations errors, Orekit
uses each [supported data](./supported-data-types.html) set in the native format in which
it is publicly available. So if a user wants to take into account the Earth Orientation
Parameters for a given year, for example, he or she will simply download the corresponding
file from IERS server at
[http://www.iers.org/IERS/EN/DataProducts/EarthOrientationData/eop.html](http://www.iers.org/IERS/EN/DataProducts/EarthOrientationData/eop.html)
and drop it in the data storage system Orekit is configured to use, without any change
to the data file itself.

Custom data formats can be used, see the [Application data](./application-data.html) page.

## Low level data handling in Orekit

The following diagram shows the data handling in Orekit at lowest level.

![data class diagram](../images/design/data-loaders-data-providers-class-diagram.png)

When some data is read in Orekit (say JPL DE405 or DE430 ephemerides which are needed by
the `LazyLoadedCelestialBody` class), an implementation of the `DataLoader` interface is
used. By default, it will be `JPLEphemeridesLoader` (this can be customized). This
implementation knows the type of files it can handle based on their names (unxp1950.405,
lnxp1990.430 ...). It also knows the file format and what to do with the data. The data
loader does not know where the data is and does not open the file itself.

The task to locate and fetch the data is performed by classes implementing the
`DataProvider` class. Each implementation is dedicated to one storage type (disk,
classpath, direct download on network, access to a database, delegation to a user defined
library ...). The providers crawl their storage medium and for each stored file ask the
data loader if it supports the file according to its name. If the data loader supports it,
then the provider will fetch the data from the storage medium and feed the loader with it.

Which data loader to use is straightforward. The `LazyLoadedCelestialBody` class for example
can only handle JPL ephemerides so only one data loader can be used. It is hardcoded in
the `LazyLoadedCelestialBody` class as the default loader. Which data provider to use is customizable. The
`DataProvidersManager` from one data context manages all the providers that should be used for
data loading in this context. The manager will typically be configured at application initialization time,
depending on the use case and perhaps configuration data (environment variables, Java
properties, users preferences ...). If the manager is not configured, a default configuration
is set up.

## Default setup

The default setup is based on a single Java property named `orekit.data.path`.
This property should be set to a list of directories trees (recommended) or zip/jar archives
(not recommended) containing the data files Orekit can use. The property is set up according
to operating system conventions, i.e. the list elements are colon-separated on Linux and Unix
type operating systems, and semicolon-separated on Windows type operating systems.

This default setup only uses static local storage (or network shared disk). It doesn't
connect to anything, neither for downloading a regular file nor to extract a bunch of bytes from
a database. It also does not look in the classpath for data. If such needs arise, then a custom
configuration must be set up.

Any number of directories trees or zip/jar archives may be used, each list element simply adds one
new location to look for data. The list elements are used in the order in which they are defined, one
at a time. If one location contains the piece of data Orekit is looking after, then the loop over the
locations is stopped and the remaining list elements are ignored. Data that can be spread over different
files (like the JPL ephemerides or the Earth Orientation Parameters for example) will be loaded from
one location only. This implies that if for example EOP data is split into yearly EOPC04 files in
directory `eop/yearly` and into weekly BulletinA files in directory `eop/weekly`, then users
should not configure separately the two locations `eop/yearly` and `eop/weekly` as this would lead
only the first configured location to be used and the second one being ignored. This directories
organization is perfectly acceptable, but it should be configured as only one location specifying
the top level directory `eop` (sub-directories are searched recursively automatically). This design choice
allows setting up configurations where users provide their own subsets of data (for example Earth
Orientation Parameters only) and prevent the system wide configuration to be used for this subset
while still using the rest of the data (for example JPL ephemerides) from the system tree. Users do so
by putting their own directories in front of the big system-level directories in the property. The
simplest configuration is however to put all data into an `orekit-data` top level directory and specify
this single directory as the unique location to use.

Directories trees or zip/jar archives may be used interchangeably. They both basically represent
container for files or other directories trees or zip/jar archives. Orekit opens zip/jar archives
on the fly and crawls into them as if they were regular directories, without writing anything to
disk. Zip/jar archives are however not recommended because extracting a file from such an archive
implies reading all the file (because the zip format puts a central directory at the end of the
archive). So if a zip archive contains both large planetary ephemerides, long term EOP data and
large gravity fields, loading the few hundreds of bytes corresponding to a UTC-TAI file still
implies reading all the other data that will be ignored, just to finally find the central directory
to locate the small desired file, and rewind everything to recover its data (either by reading a
second time the file, or by having preserved everything in memory). There is much less overhead in
expanding the zip file as a directories tree beforehand and point Orekit to the location of the top
directory.

Data files may also be compressed using gzip or Unix compress to save some disk space. Compressed files
are uncompressed directly during parsing, with only the current needed compressed and uncompressed
blocks being kept in memory. Compressing text-based files like Bulletin B, EOPC04 or RINEX
saves a lot of disk space, but compressing the JPL binary files saves very little space.
Using compressed files inside a zip archive is also irrelevant as zip/jar files are themselves
compressed and stacking compression algorithms only slows down reading speed without saving any disk
space (except for the specialized Hatanaka compression for RINEX files which as it remains text is
often stacked with Unix or gzip compression). The filtering feature is explained in the
[filtering](./filtering.html) page.

Since nothing is ever written to disk (there are no temporary files), user provided data sets may
be stored on non-writable media like disk partitions with restricted access or CD/DVD media.

![directories tree](../images/directories-tree.png)

There is no mandatory layout within the data directories trees or zip/jar archives. Orekit
navigates through them and their sub-directories when looking for data files. Files are
identified by pattern matching rules on their names. Files that don't match the rules are
silently ignored. This allows the user to share the data directories trees with other tools
which need a specific layout or additional files. The layout presented in the figure above
is a simple example.

As with any other Java property, `orekit.data.path` can be initialized at application launch time by
the user (for example using the -D flag of the virtual machine) or from within the application
by calling the `System.setProperty` method. In the latter case, rather than the literal string
constants `orekit.data.path`, the `OREKIT_DATA_PATH` static field from the `DataProvidersManager`
class can be used. If the property is set up by the application, it must be done before any Orekit
feature is called, since some data are initialized very early (mainly frame and time related data
like leap seconds for UTC).

## Setting up a customized configuration

If the default setup doesn't suit users needs, a custom configuration must be set up.
This happens for example if data must be embedded within the application and loaded from
the classpath. This also happens if the data must be retrieved from a dynamic or virtual
storage medium like a database, a web site or a local data handling library.

The custom configuration may be set up by using a dedicated [data context](./contexts.html),
for example if a mission-dedicated database is used, or by configuration the default
`DataProvidersManager` if the data storage remains mainly resource/files oriented.

Configuration a `DataProvidersManager` involves purging it (if it already existed) and adding
specific data providers in an appropriate order.

The data providers predefined by the Orekit library are the following ones:

  * `DirectoryCrawler` for loading files in a directory tree specified by its root
  * `ZipJarCrawler` for loading files stored in a zip or jar archive
  * `ClasspathCrawler` for loading files stored as resources in the classpath
  * `NetworkCrawler` for downloading files from remote hosts (it can be directly from
    internet sites through a corporate proxy server)

Users can also add their own implementations of the `DataProvider` interface and
register them to the `DataProvidersManager` instance.

## Quick recommended setup

For convenience, the simplest configuration
is to download the [orekit-data-master.zip](https://gitlab.orekit.org/orekit/orekit-data/-/archive/master/orekit-data-master.zip)
file from the forge, to unzip it anywhere users want, rename the `orekit-data-master` folder that will be created
into `orekit-data` and add the following lines at the start of users programs:


    File orekitData = new File("/path/to/the/folder/orekit-data");
    DataProvidersManager manager = DataContext.getDefault().getDataProvidersManager();
    manager.addProvider(new DirectoryCrawler(orekitData));

This zip file contains JPL DE 440 ephemerides from 1990
to 2149, IERS Earth orientation parameters from 1973 (both IAU-1980
and IAU-2000), UTC-TAI history from 1972,
Marshall Solar Activity Futur Estimation from 1999,
the Eigen 06S gravity field and the FES 2004 ocean tides model and space weather data with
observed data from 1957 with predicted data up to 22 years in the future.
