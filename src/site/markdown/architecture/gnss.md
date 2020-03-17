<!--- Copyright 2002-2020 CS Group
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

* GPSBlockIIA, which represents an attitude providers for GPS block IIR
  navigation satellites.

* GPSBlockIIF, which represents an attitude providers for GPS block IIF
  navigation satellites.

* GPSBlockIIR, which represents an attitude providers for GPS block IIR
  navigation satellites.

## GNSS data

Several classes have been implemented in order to load and handle classical navigation files.

### Supported navigation files

Several file formats are supported in Orekit.

* RinexLoader, which represents a loader for Rinex measurements files.
  The supported versions are: 2.00, 2.10, 2.11, 2.12, 2.20,  3.00, 3.01, 3.02, and 3.03.
  Versions 2.12 and 2.20 are unofficial versions, whereas the other are official version.

![gnss rinex class diagram](../images/design/gnss-rinex-class-diagram.png)

* AntexLoader, which represents a loader for antex files.

* SEMParser, which represents a loader for SEM almanac files. This class provides
  the `GPSAlmanac` used to build the `GPSPropagator`.

* YUMAParser, which represents a loader for YUMA almanac files. Such as for the SEMParser,
  this class provides the `GPSAlmanac` used to build the `GPSPropagator`.

### Data management

Several classes have been implemented to provide a link between navigation files
and the specialized orbit propagators for GNSS constellations. These classes,
are the base classes to build the specialized GNSS orbit propagators.

## Antenna

Orekit provides classes related to receiver and satellites antenna modeling.

![gnss antenna class diagram](../images/design/gnss-antenna-class-diagram.png)

