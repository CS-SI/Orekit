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

# Filtering

Orekit provides an automatic filtering feature for data. This feature is activated
automatically with an initial set of predefined filters for data loaded through a
`DataProvidersManager` instance when using the [default configuration](./default-configuration.html),
and can be used for [explicit loading](./application-data#Explicit_loading) of application data.

All filters implement the `DataFilter` interface and can be registered to a `FiltersManager`
instance. In the [default configuration](./default-configuration.html), three filters are
registered to the `FiltersManager` contained in the `DataProvidersManager`:

  - `GzipFilter` which uncompresses files compressed with the gzip algorithm
  - `UnixCompressFilter` which uncompresses files compressed with the Unix compress algorithm
  - `HatanakaCompressFilter` which uncompresses RINEX files compressed with the Hatanaka method

Upon load time, all filters that can be applied to a set of data will
be applied. If for example a file is both encrypted and compressed
(in any order) and filters exist for uncompression and for deciphering,
then both filters will be applied in the right order to the data retrieved
by the `DataProvider` before being fed to the `DataLoader` (or the parsers set up by
users in [explicit loading](./application-data#Explicit_loading) of application data).

The following class diagrams shows the main classes and interfaces involved
in this feature.

![data filtering class diagram](../images/design/data-filtering-class-diagram.png)

## Filters stack

The filtering principle is based on a stack of `DataSource` instances, with at the bottom
an instance (created by a `DataProvider` when using `DataProvidersManager`, or created
manually when loading data explicitly). The instance at the bottom of the stack will read
bytes or characters directly from storage. Upwards in the stack, one will find instances added
by the `FiltersManager.applyRelevantFilters` method as needed, each one reading data from the
underlying stack element and providing filtered data to the next element upward.

In the `DataProvidersManager` case, if at the end the name part of the `DataSource` matches the
name that the`DataLoader` instance expects, then the data stream of the top of the stack is opened.
This is were the lazy opening occurs, and it generally ends up with all the intermediate bytes or
characters streams being opened as well. The opened stream is then passed to the `DataLoader` to be
parsed. If on the other hand the name part of the `DataSource` does not match the name that the
`DataLoader` instance expects, then neither the data stream is *not* opened, the full stack is discarded
and the next resource/file from the `DataProvider` is considered for filtering and loading.

In the explicit loading case, application can decide on its own to open or discard the top
level `DataSource`, or select the appropriate parser based on the source name without having
to bother about extensions like '.gz' as they would already have been handled by lower level
filters.

## Example with default configuration

One example will explain this method more clearly. Consider a `DirectoryCrawler`
configured to look into a directories tree containing files `tai-utc.dat` and
`MSAFE/may2019f10_prd.txt.gz`, consider one of the defaults filters: `GzipFilter`
that uncompresses files with the `.gz` extension (the defaults filters also include
`UnixCompressFilter` and `HatanakaCompressFilter`, they are omitted for clarity), and
consider `MarshallSolarActivityFutureEstimation` which implements `DataLoader` and can
load files whose name follow a pattern mmmyyyyf10_prd.txt (among others).

![data filtering sequence diagram](../images/design/data-filtering-sequence-diagram.png)

When the `tai-utc.dat` file is considered by the `DirectoryCrawler`, a `DataSource` is created
for it. Then the filters are checked (only one filter shown in the diagram), and all of them
decline to act on the file, so they all return the same `DataSource` that was created for the
raw file. At the end of the filters loop, the name (which is still `tai-utc.dat`) is checked
against the pattern expected by the data loader. As it does not match, the stack composed of
only one `DataSource` is discarded. During all checks, the file has not been opened at all,
only its name has been considered.

The `DirectoryCrawler` then considers the next directory, and in this directory the next
file which is `may2019f10_prd.txt.gz`. A new `DataSource` is created for it and the filters are
checked. As the extension is `.gz`, the `GzipFilter` filter considers it can act on the file
and it creates and returns a new `DataSource`, with name set to `may2019f10_prd.txt` (it has removed
the `.gz` extension) and lazy stream opener set to insert an uncompressing algorithm between the raw file bytes
stream and the uncompressed bytes stream it will provide. The loop is restarted, but no other
filter applies so at the end the stack contains two `DataSource`, the bottom one reading from
storage and providing gzip compressed data, and the top one reading the gzip compressed data,
uncompressing it and providing uncompressed data. As the name of the top instance matches the
expected pattern for MSAFE data, the `MarshallSolarActivityFutureEstimation` will be able to
load it. At this stage, the `DirectoryCrawler` calls the method to open the bytes stream at the
top level of the stack. This method then asks the underlying `DataSource` to open its stream
(which it the raw file data), feeds the gzip uncompression algorithm with this data and provides
the output uncompressed data as a newly opened bytes stream. The data loader then parses the data,
without knowing that is is uncompressed on the fly.

## Example with explicit loading

When loading data explicitly, the application is responsible to set up the `FiltersManager`
and call it. The following example shows for example how to load CCSDS Orbit Ephemeris Messages
from files selected by users in a graphical interface, where some files may have been
compressed using either gzip or with Unix compress, and may have been ciphered on disk as
they contain sensitive information (and they may be both compressed and ciphered, in any
order): 

    // set up filters manager, this may be done only once at application startup if needed
    FiltersManager manager = new FiltersManager();
    filtersManager.addFilter(new GzipFilter());
    filtersManager.addFilter(new UnixCompressFilter());
    filtersManager.addFilter(new MyOwnDecipheringFilter(secretKey));

    // set up builder for CCSDS files parsers
    ParserBuilder parserBuilder = new ParserBuilder(dataContext);

    // parse files
    for (final File file : userInterface.getFilesToProcess()) {

        // set up raw data source, which may be compressed and/or ciphered
        DataSource rawSource = new DataSource(file.getName(), () -> new FileInputStream(file));

        // apply relevant filters
        DataSource filteredSource = manager.applyRelevantFilters(rawSource);

        // parse the file, which is now known to be uncompressed and deciphered
        OEM oem = parserBuidler.buildOemParser().parse(filteredSource);

        // process the ephemeris
        ...

    }
