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

# Custom data formats

Orekit supports a large set of data formats natively. For example, leap seconds information
can be loaded from file `tai-utc.dat` provided by USNO or from file `UTC-TAI.history` provided
by IERS. EOP data can be retrieved from weekly Bulletin A, monthly Bulletin B, yearly EOPC04,
rapid data `finals` files. Gravity fields can be loaded from files in EGM, GRGS, ICGEM or SHM
formats.

This large number of supported formats is made possible thanks to a, extensible low level
data loading architecture. New formats are added to the list regularly using this architecture,
but users may benefit from it and add support for their own formats in their applications.
It is therefore possible to use Orekit in a system that already has some established
mission-specific data formats.

Orekit also supports on-the-fly filtering of data during the loading process. Filtering
can be used for example if data is stored on disk in compressed or enciphered form and
should be uncompressed or deciphered at load time. Predefined filters readily available
in Orekit allow to uncompress files using gzip (files ending in `.gz`), Unix compress
(files ending in `.Z`), or Hatanaka method for RINEX files (files with a specific pattern,
ending with either `.##d` where `##` is a two-digits year for RINEX 2 files or `.crx`
for RINEX 3 files).

Users may also benefit from the filtering architecture and add support for their own
filters in their applications, for example to decipher sensitive data.

## Low level data handling in Orekit

The following diagram shows the data handling in Orekit at lowest level.

![data class diagram](./images/design/data-class-diagram.png)

When some data is read in Orekit (say JPL DE405 or DE406 ephemerides which are needed by
the `CelestialBodyFactory` class), an implementation of the `DataLoader` interface is used.
By default, it will be `JPLEphemeridesLoader` (this can be customized). This implementation knows the type
of files it can handle based on their names (unxp1950.405, unxp2000.405 ...). It also
knows the file format and what to do with the data. The data loader does not know where
the data is and does not open the file itself.

The task to locate and fetch the data is performed by classes implementing the
`DataProvider` class. Each implementation is dedicated to one storage type (disk,
classpath, direct download on network, access to a database, delegation to a user defined
library ...). The providers crawl their storage medium and for each stored file ask the
data loader if it supports the file according to its name. If the data loader supports it,
then the provider will fetch the data from the storage medium and feed the loader with it.

Which data loader to use is straightforward. The `CelestialBodyFactory` class for example
can only handle JPL ephemerides so only one data loader can be used. It is hardcoded in
the `CelestialBodyFactory` class as the default loader. Which data provider to use is customizable. The
`DataProvidersManager` from one data context manages all the providers that should be used for
data loading in this context. The manager will typically be configured at application initialization time,
depending on the use case and perhaps configuration data (environment variables, Java
properties, users preferences ...). If the manager is not configured, a default configuration
is set up.

In Orekit versions up to 10.0, the `DataProvidersManager` class was a singleton, so only
one context of data could be used in an application. Starting with Orekit version 10.1,
several different context can be set up and have their own `DataProvidersManager`, which
is not a singleton anymore. This is explained in the [configuration](../configuration.html)
page.

## Parsing custom data formats

If some data format is not supported by Orekit, then a
specialized `DataLoader` implementation can be set up by users.

The most important method in this class is the `loadData` method. This method takes
as a parameter an already open `InputStream` from where data can be read using regular
Java API. The second parameter is the name of the data file being loaded, it is only
intended to generate meaningful error messages to end users if the file happens to be
corrupted. The `InputStream` is already open and has already been passed through
all applicable filters. This means that users only need to take care of the parsing itself,
and their custom data loader will be automatically able to manage data coming from
files, from the network, compressed or not as all these features have already been
taken care of by the data providers and the data filters.

The way the data loaded is provided back to Orekit is not specified in the
API. In fact, it depends on the data type. One recommended way to manage this
is to create a dedicated container class for the data (`ContainerWithNestedCustomParser` in
the following diagram) extending the `AbstractSelfFeedingLoader` class, and have a nested
class `Parser` inside the container that implements `DataLoader`.

![custom parser class diagram](./images/design/custom-parser-class-diagram.png)

As the `Parser` is nested within the container, it can populate it while parsing.
In order to have the nested parser being fed, a protected method `feed` in the
`AbstractSelfFeedingLoader` is called internally with the `Parser` in argument,
and this method will trigger the configured `DataProvidersManager` to feed itself.
Users can look at the implementation of `BulletinAFilesLoader` to see how it works.

This can be used either to load data that is already known to Orekit (like EOP), but
only the file format is unknown, or it can be used to load custom data.


