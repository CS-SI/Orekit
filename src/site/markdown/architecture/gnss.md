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

# GNSS

The `org.orekit.gnss` is an independent package providing classes to handle classical GNSS files
(SEM and YUMA almanacs, Rinex and Antex) and attitude providers for navigation satellites.

## Attitudes

Several classes have been implemented in order to represent navigation satellites attitude modeling.

### Beidou attitude

* BeidouGEO, which represents an attitude providers for Beidou geostationary
  orbit navigation satellites.

* BeidouIGSO, which represents an attitude providers for Beidou inclined
  geosynchronous orbit navigation satellites. This mode is in fact similar
  to Beidou MEO.

* BeidouMeo, which represents an attitude providers for Beidou
  Medium Earth Orbit navigation satellites.

### Galileo attitude

* Galileo, which represents an attitude providers for Galileo
  navigation satellites.

### Generic GNSS attitude

* GenericGNSS, this attitude mode can be used for navigation satellites for which
  no specialized model is known.

### Glonass attitude

* Glonass, which represents an attitude providers for Glonass
  navigation satellites.

### GPS attitude

* GPSBlockIIA, which represents an attitude providers for GPS block IIA
  navigation satellites.

* GPSBlockIIF, which represents an attitude providers for GPS block IIF
  navigation satellites.

* GPSBlockIIR, which represents an attitude providers for GPS block IIR
  navigation satellites.

## GNSS data

Several classes have been implemented in order to load and handle classical navigation data.

### Supported formats

Several file formats are supported in Orekit.

* RinexObservationLoader, which represents a loader for Rinex measurements files.
  The supported versions are: 2.00, 2.10, 2.11, 2.12, 2.20,  3.00, 3.01, 3.02, and 3.03.
  Versions 2.12 and 2.20 are unofficial versions, whereas the other are official version.

![gnss rinex class diagram](../images/design/gnss-rinex-class-diagram.png)

* AntexLoader, which represents a loader for ANTEX files.

![gnss antenna class diagram](../images/design/gnss-antenna-class-diagram.png)

* SEMParser, which represents a loader for SEM almanac files. This class provides
  the `GPSAlmanac` used to build the `GNSSPropagator`.

* YUMAParser, which represents a loader for YUMA almanac files. Such as for the SEMParser,
  this class provides the `GPSAlmanac` used to build the `GNSSPropagator`.

* RinexClockParser for loading station and satellite clock solutions.

* RinexNavigationParser, which represents a loader for Rinex navigation files.
  The supported versions are from 3.00 to 3.05, all the GNSS constellations are supported.

### IGS SSR Format

Since version 11.0, Orekit is able to read and handle the IGS SSR format. This format is
an open standard for dissemination of real-time products to support the IGS Real-Time
Service and the wider community. The messages supported in Orekit are multi-GNSS and
include corrections for orbits, clocks, DCBs, phase-biases and ionospheric delays. The
architecture of SSR format handling in Orekit is represented on the following image.

![IGS SSR handling](../images/design/metric-parser-class-diagram.png)

Furthermore, in order to access the SSR messages from IGS casters, Orekit implements the
Networked Transport of RTCM via Internet Protocol (Ntrip). Ntrip stands for an application
level protocol streaming GNSS data over the Internet. The architecture is represented on
the image below.

![Ntrip](../images/design/metric-ntrip-class-diagram.png)

### Data management

Several classes have been implemented to provide a link between navigation files
and the specialized orbit propagators for GNSS constellations. These classes,
are the base classes to build the `GNSSPropagator`.

