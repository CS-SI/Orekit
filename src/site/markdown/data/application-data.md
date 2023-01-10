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

# Application data

Applications based on Orekit will often need to load explicitly application-specific
data, in addition to the data implicitly loaded by the library itself using the
[data contexts](./contexts.html). This may include for example orbit and
attitude ephemeris files, measurements, ground stations, reference GNSS solutions...

## Merging application and library data

Some of the mechanisms originally created for Orekit data loading needs
like separation between [data storage](./default-configuration.html#Data_storage) and
[data formats](./default-configuration.html#Data_formats) or [filtering](./filtering.html)
may be used for application data too.

The simplest way to reuse all mechanisms transparently is to merge application data
and library data using the [default configuration](./default-configuration.html) with
the `DataProvidersManager` and put the files to be read either at the same location
as the library (for example an `orekit-data` folder in home directory) or at an
application-specific location for which one `DataProvider` (typically a `DirectoryCrawler`)
has been set up and registered to the `DataProvidersManager`.

### Custom data formats
Orekit supports a large set of data formats natively. For example, leap seconds information
can be loaded from file `tai-utc.dat` provided by USNO or from file `UTC-TAI.history` provided
by IERS. EOP data can be retrieved from weekly Bulletin A, monthly Bulletin B, yearly EOPC04,
rapid data `finals` files. Gravity fields can be loaded from files in EGM, GRGS, ICGEM or SHM
formats.

This large number of supported formats is made possible thanks to an extensible low level
data loading architecture. New formats are added to the list regularly using this architecture,
but users may also benefit from it and add support for their own formats in their applications.
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

### Parsing custom data formats

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

![custom parser class diagram](../images/design/custom-parser-class-diagram.png)

As the `Parser` is nested within the container, it can populate it while parsing.
In order to have the nested parser being fed, a protected method `feed` in the
`AbstractSelfFeedingLoader` is called internally with the `Parser` in argument,
and this method will trigger the configured `DataProvidersManager` to feed itself.
Users can look at the implementations of `BulletinAFilesLoader` and `YUMAParser`
to see how it works.

This can be used either to load data that is already known to Orekit (like EOP), but
only the file format is unknown, or it can be used to load application data.

## Explicit loading

The default configuration may however not suit specific needs for application data,
for example when only one specific ephemeris must be loaded in a directory containing
many similar files, depending on users selections within a list.

In this case, it may still be advantageous to rely on the lower level mechanisms
like [filtering](./filtering.html) by configuring an application-specific `FiltersManager`.
In this case, rather than implementing a `DataLoader`, users may create their own parser
from scratch (or use one of the ephemeris loaders for standard formats supported by Orekit)
in such a way they use a `DataSource` to specify where the bytes or characters to be parsed
come from, rather than using directly a `File`, an `InputStream` or a `Reader`.
