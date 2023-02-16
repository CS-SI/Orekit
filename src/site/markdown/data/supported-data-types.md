<!--- Copyright 2002-2022 CS GROUP
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

# Supported data types

The data types supported by Orekit are described in the following table, where the `#`
character represents any digit, `(m/p)` represents either the m character or the p
character and `*` represents any character sequence. The `[.gz|.Z]` part at the end of all
naming patterns means that optional `.gz` (resp. `.Z`) suffixes can be appended, in which
case the data are considered to be compressed with gzip (resp. Unix compress). Decompression
is performed on the fly in memory by the library upon data loading when using the
[default setup](./default-configuration.html#Default_setup), but can be used explicitly
with any other setup, if users call the [filtering](./filtering.html) capabilities by
themselves.

Some of the data in the table are loaded automatically by the library itself for its
internal needs (for example Earth Orientation parameters, leap seconds history or
planetary ephemerides), and some must be loaded explicitly by applications (for example
CCSDS orbit, attitude, tracking or navigation messages).

Earth Orientation Parameters are provided by observatories in many different formats
(Bulletin A, several different formats of Bulletin B, EOP C04, finals file combining
both Bulletin A and Bulletin B information ...). They are also provided for different
precession-nutation models (IAU-1980 and IAU-2006/2000A). Orekit supports all of these
formats and supports both precession-nutation models. Two different
naming patterns for Bulletin B are supported by default. Both the old Bulletin B format
used up to 2009 and the new format used since 2010 are supported. The supported formats
for `finals2000A` files for IAU-2006/2000A and the finals files for IAU-1980 are both
the XML format and the columns format.

|                          data type                                                       |       format                                  |          default naming pattern                        |                                                                    source                                                                          |
|------------------------------------------------------------------------------------------|-----------------------------------------------|--------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------|
| leap seconds introduction history                                                        | USNO tai-utc                                  | tai-utc.dat[.gz|.Z]                                    | [http://maia.usno.navy.mil/ser7/tai-utc.dat](http://maia.usno.navy.mil/ser7/tai-utc.dat) offline as of 2021                                        |
| leap seconds introduction history                                                        | IERS history                                  | UTC-TAI.history[.gz|.Z]                                | [http://hpiers.obspm.fr/eoppc/bul/bulc/UTC-TAI.history](http://hpiers.obspm.fr/eoppc/bul/bulc/UTC-TAI.history)                                     |
| weekly Earth Orientation Parameters, IAU-1980 and IAU-2000, rapid service and prediction | IERS Bulletin A                               | bulletina-xxxx-\#\#\#.txt[.gz|.Z]                      | [https://www.iers.org/IERS/EN/DataProducts/EarthOrientationData/eop.html](https://www.iers.org/IERS/EN/DataProducts/EarthOrientationData/eop.html) |
| monthly Earth Orientation Parameters model IAU 2006/2000A, final values                  | IERS Bulletin B                               | bulletinb.\#\#\#[.gz|.Z]                               | [ftp://ftp.iers.org/products/eop/bulletinb/format_2009/](ftp://ftp.iers.org/products/eop/bulletinb/format_2009/)                                   |
| yearly Earth Orientation Parameters model IAU 2006/2000A for ITRF 20xx                   | IERS EOP xx C04                               | eopc04\_\#\#\_IAU2000.\#\#[.gz|.Z]                     | [https://hpiers.obspm.fr/eoppc/eop/](https://hpiers.obspm.fr/eoppc/eop/)                                                                           |
| yearly Earth Orientation Parameters model IAU 1980 for ITRF 20xx                         | IERS EOP xx C04                               | eopc04\_\#\#.\#\#[.gz|.Z]                              | [https://hpiers.obspm.fr/eoppc/eop/](https://hpiers.obspm.fr/eoppc/eop/)                                                                           |
| Earth Orientation Parameters model IAU 2006/2000A                                        | IERS standard EOP                             | finals2000A.\*.[.gz|.Z]                                | [https://datacenter.iers.org/data/9/finals2000A.all](https://datacenter.iers.org/data/9/finals2000A.all)                                           |
| Earth Orientation Parameters  model IAU 1980                                             | IERS standard EOP                             | finals.\*.[.gz|.Z]                                     | [https://datacenter.iers.org/data/7/finals.all](https://datacenter.iers.org/data/7/finals.all)                                                     |
| Earth Orientation Parameters model IAU 2006/2000A                                        | IERS standard EOP                             | finals2000A.\*.xml[.gz|.Z]                             | [ftp://ftp.iers.org/products/eop/rapid/standard/xml/finals2000A.all.xml](ftp://ftp.iers.org/products/eop/rapid/standard/xml/finals2000A.all.xml)   |
| Earth Orientation Parameters model IAU 1980                                              | IERS standard EOP                             | finals.\*.xml[.gz|.Z]                                  | [ftp://ftp.iers.org/products/eop/rapid/standard/xml/finals.all.xml](ftp://ftp.iers.org/products/eop/rapid/standard/xml/finals.all.xml)             |
| JPL DE 4xx planets ephemerides                                                           | DE 4xx binary                                 | (l/u)nx(m/p)\#\#\#\#.4\#\#[.gz|.Z]                     | [ftp://ssd.jpl.nasa.gov/pub/eph/planets/Linux/](ftp://ssd.jpl.nasa.gov/pub/eph/planets/Linux/)                                                     |
| IMCCE inpop planets ephemerides                                                          | DE 4xx binary                                 | inpop\*_m\#\#\#\#_p\#\#\#\#*.dat[.gz|.Z]               | [ftp://ftp.imcce.fr/pub/ephem/planets/](ftp://ftp.imcce.fr/pub/ephem/planets/)                                                                     |
| Eigen gravity field (old format)                                                         | SHM format                                    | eigen\_\*\_coef[.gz|.Z]                                | [http://op.gfz-potsdam.de/grace/results/main\_RESULTS.html#gravity](http://op.gfz-potsdam.de/grace/results/main_RESULTS.html#gravity)              |
| gravity fields from International Centre for Global Earth Models                         | ICGEM format                                  | \*.gfc, g\#\#\#\_eigen\_\*\_coef[.gz|.Z]               | [http://icgem.gfz-potsdam.de/tom_longtime](http://icgem.gfz-potsdam.de/tom_longtime)                                               |
| EGM gravity field                                                                        | EGM format                                    | egm\#\#\_to\#\*[.gz|.Z]                                | [ftp://cddis.gsfc.nasa.gov/pub/egm96/general\_info](ftp://cddis.gsfc.nasa.gov/pub/egm96/general_info)                                              |
| Marshall Solar Activity Future Estimation                                                | MSAFE format                                  | jan\#\#\#\#f10.txt to dec\#\#\#\#f10[_prd].txt[.gz|.Z] | [https://www.nasa.gov/msfcsolar/archivedforecast](https://www.nasa.gov/msfcsolar/archivedforecast)                                                 |
| Klobuchar coefficients                                                                   | Bern Astronomical Institute format            | CGIM\#\#\#0.\#\#N [.gz|.Z]                             | [ftp://ftp.aiub.unibe.ch/CODE/](ftp://ftp.aiub.unibe.ch/CODE/)                                                                           |
| Vienna Mapping Function                                                                  | VMF                                           | VMF\*.\#\#H                                            | [http://vmf.geo.tuwien.ac.at/trop_products/GRID/](http://vmf.geo.tuwien.ac.at/trop_products/GRID/)                                                 |
| Global Ionosphere Map                                                                    | ionex                                         | \*\.\#\#i                                              | CDDIS                                                                                                                                              |
| space weather                                                                            | CSSI format                                   |SpaceWeather-All-v1.2.txt                               | [ftp://ftp.agi.com/pub/DynamicEarthData/SpaceWeather-All-v1.2.txt](ftp://ftp.agi.com/pub/DynamicEarthData/SpaceWeather-All-v1.2.txt)               |
| JB2008 SOLFSMY                                                                           | Space Environment format                      |SOLFSMY.TXT                               | [https://sol.spacenvironment.net/JB2008/indices/SOLFSMY.TXT](https://sol.spacenvironment.net/JB2008/indices/SOLFSMY.TXT)               |
| JB2008 DTC                                                                               | Space Environment format                      |DTCFILE.TXT                               | [https://sol.spacenvironment.net/JB2008/indices/DTCFILE.TXT](https://sol.spacenvironment.net/JB2008/indices/DTCFILE.TXT)               |
| EOP files to ITRF versions mapping                                                       | Orekit itrf-versions.conf                     | itrf-versions.conf                                     | [Orekit Physical Data Archive](https://gitlab.orekit.org/orekit/orekit-data/-/archive/master/orekit-data-master.zip)                               |
| CCSDS Attitude Data Message                                                              | CCSDS ADM V1 (KVN and XML)                    | none (must be loaded explicitly)                       | various, can be produced by Orekit itself                                                                                                          |
| CCSDS Orbit Data Message                                                                 | CCSDS ODM V2 and V3 (KVN and XML)             | none (must be loaded explicitly)                       | various, can be produced by Orekit itself                                                                                                          |
| CCSDS Tracking Data Message                                                              | CCSDS TDM V1 (KVN and XML)                    | none (must be loaded explicitly)                       | various, can be produced by Orekit itself                                                                                                          |
| CCSDS Navigation Data Message                                                            | CCSDS NDM V1 and V3 (KVN and XML)             | none (must be loaded explicitly)                       | various, can be produced by Orekit itself                                                                                                          |
| CCSDS Conjunction Data Message                                                           | CCSDS CDM V1 (KVN and XML)                    | none (must be loaded explicitly)                       | various, can be produced by Orekit itself                                                                                                          |
| GNSS Antenna data                                                                        | Antex                                         | \*.atx                                                 | various, mainly [IGS](https://files.igs.org/pub/station/general/igs14.atx)                                                                         |
| GNSS measurements                                                                        | Receiver Independant EXchange Format 2 and 3  | \*.\"\#{od}\|\*.{crx\|rnx}[.gz|.Z]                     | various                                                                                                                                            |
| GNSS clock                                                                               | rinex clock 3                                 | \*.clk[.gz|.Z]                                         | various                                                                                                                                            |
| GNSS solutions                                                                           | Solution Independant EXchange Format 2        | \*.snx[.gz|.Z]                                         | various                                                                                                                                            |
| GNSS orbits                                                                              | SP3 a, c and d                                | \*.sp3[.gz|.Z]                                         | various                                                                                                                                            |
| GNSS navigation                                                                          | based on RINEX 2 and 3                        | \*.n[.gz|.Z]                                           | various                                                                                                                                            |
| GNSS almanach                                                                            | SEM and YUMA                                  |                                                        | various                                                                                                                                            |
| GNSS real time (navigation, clock...)                                                    | IGS SSR messages, through RTCM and NTRIP      | none (streaming data)                                  | various, sourcetable usually from [BKG](https://products.igs-ip.net/home)                                                                          |
| laser ranging prediction file                                                            | CPF format                                    |                                                        | various, mainly [CDDIS](https://cddis.nasa.gov)                                                                                                                             |
| laser ranging data                                                                       | CRD format                                    |                                                        | various, mainly [CDDIS](https://cddis.nasa.gov)                                                                                                    |
| Ocean loading coefficients                                                               | BLQ format                                    | *.blq                                                  | [Onsala Space Observatory](http://holt.oso.chalmers.se/loading/)                                                                                                    |
Supported data types
