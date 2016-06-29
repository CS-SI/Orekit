<!--- Copyright 2002-2016 CS Systèmes d'Information
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

# Frequently Asked Questions (FAQ)

## References

### Has Orekit already been used?

Yes, it has been used in successful operational missions.

The first operational use of Orekit was for the Automated Transfer
Vehicle (ATV) mission to the International Space station (ISS). Orekit
was used operationally during the real-time monitoring of the rendezvous
phase up to the docking. It continuously recomputed the relative geometry
of the two spacecraft using different sensors output to check its
consistency.

Orekit has been selected by CNES for its next-generation flight
dynamics systems (project Sirius) in early 2011, including operational
systems, study systems and mission analysis systems.

Orekit is used at Eumetsat for very long term mission analysis (up to
the full lifetime of a satellite) for both LEO and GEO missions.

Orekit is used underneath the [Rugged](https://www.orekit.org/rugged/) sensor-to-terrain mapping library in the
Sentinel-2 Image Processing Facility to process terabytes of data each day.

As Orekit is open-source, we cannot know about all uses as people are
not require to notify us of anything.

### Is Orekit validated?

Some parts are strongly validated, others are validated to a lesser extent.

The frames package is one of the best validated ones. The overall mechanism
(transforms, navigation between frames, kinematics ...) has really been challenged
a lot, both in theoretical tests and during real life operations. This part was
extensively used for Automated Transfer Vehicle (ATV) rendezvous with International
Space Station (ISS). It was part of an operational ground program performing real-time
monitoring of the rendezvous and docking phase. This part has been checked to
below millimeter-level precision for relative configuration. The reference frames part (ITRF
and the like) has been validated using public data down to meter-level precision for
version 3.1, and to centimeter-level precision for the version 4.0.

A new round of validation was done after Orekit 3.1 was published. Two
defects were identified and fixed: the J2000 frame was misaligned with real J2000
by a constant rotation bias of about 18 milliarcsceonds (it was really GCRF, not J2000)
and the ITRF2000B implementation was wrong by a time-dependent rotation leading to about
0.6 meters error for orbits with a semi-minor axis of about 10000km. These errors have
been fixed as of version 4.0. Our tests show the new frames are compliant with reference
cases to about 10mm for LEO and 60mm for GEO.

The TLE package is also quite well validated; it has been checked against some reference
data published by Vallado along with his revision of the original spacetrack report, where
he fixed some errors in the original Fortran implementation from NORAD.

The atmosphere models have also been validated the same way, using published data.

The time package has been validated by its unit tests only, but since the behavior is
simpler it can be checked by hand. The unit tests include a lot of borderline cases
(for example behavior during the introduction of a leap second).

The Sun and Moon classes for version 3.1 are very low precision and are defined in a
pseudo-inertial frame with loose definition. They can be compared only with very specific
software. Their accuracy is probably limited to about 10 arcseconds. They have been replaced
by accurate reference models in the version 4.0.

Numerical propagation has been validated by CNES independently of the Orekit team against
some very high accuracy propagators (Zoom). They told us the results were good (down to
centimeter level for simple force models), but we don't have a thorough breakdown of errors
for various orbits and force models.

Validation is a continuous task for us, we are always working on improving it. We would be
happy to also have other teams perform independent validation runs. We have already received
some feedback and new test cases after the first version was published.

### Is Orekit thread-safe?

Versions up to 5.X are *not* thread-safe. Note that simply wrapping Orekit calls
in synchronized blocks is not a solution as it completely breaks all data caching features,
so performances are reduced by a large factor.

As thread-safety was an important need for many people, this problem has been addressed and
starting with version 6.0 many Orekit classes are thread-safe. Note however that some parts
for which sequential access is natural (like numerical propagators) are <em>not</em> thread-safe.

## Installation

### What are Orekit dependencies?

Up to version 4.0, Orekit depended on features of Apache Commons Math which were not released
as of mid 2008, so the dependency was set to 2.0-SNAPSHOT development version.
This development version was available from Apache subversion repository. Starting
with version 4.1, and up to 7.2, Orekit depends only on officially released versions of
Apache Commons Math. Starting with version 8.0, Orekit has switched from Apache Commons
Math to Hipparchus

    version    |                             dependency
---------------|---------------------------------------------
  Orekit 4.1   | Apache Commons Math 2.0
  Orekit 5.0   | Apache Commons Math 2.1
  Orekit 5.0.3 | Apache Commons Math 2.2
  Orekit 6.0   | Apache Commons Math 3.2
  Orekit 6.1   | Apache Commons Math 3.2
  Orekit 7.0   | Apache Commons Math 3.4.1
  Orekit 7.1   | Apache Commons Math 3.6
  Orekit 7.2   | Apache Commons Math 3.6.1
  Orekit 8.0   | Hipparchus          1.0

### Maven failed to compile Orekit and complained about a missing artifact.

The released versions of Orekit always depend only on released Hipparchus
versions, but development Orekit versions may depend on unreleased Hipparchus
versions. Maven knows how to download the pre-built binary for
released Hipparchus versions but it cannot download
pre-built binaries for unreleased Hipparchus versions as none are
publicly available. In this case the maven command will end with an error message
like:

    [ERROR] Failed to execute goal on project orekit: Could not resolve dependencies for project org.orekit:orekit:jar:8.0-SNAPSHOT: Could not find artifact org.hipparchus:hipparchus-core:jar:1.0-SNAPSHOT

In this case, you should build the missing Hipparchus artifact and
install it in your local maven repository beforehand. This is done by cloning
the Hipparchus source from Hipparchus git repository at GitHub in some
temporary folder and install it with maven. This is done by
running the commands below (using Linux command syntax):

    git clone https://github.com/Hipparchus-Math/hipparchus.git
    cd hipparchus
    mvn install

Once the Hipparchus development version has been installed locally using
the previous commands, you can delete the cloned folder if you want. You can then
attempt again the mvn command at Orekit level, this time it should succeed as the
necessary artifact is now locally available.

### The orekit-data.zip file you provide is not up to date. Can you update it?

There is no regular update for this file. Data are provided only as an example, to allow quick
start for new users. For long-term use, data handling remains their own responsibility. The
configuration page points out the data sources that can be taken into account by Orekit, so you
can go visit that link to look for what you need.

Some difficulties may yet occur for very recent data. Indeed, the IERS once again changed its
file formats and stopped publishing the B Bulletins (see Earth Orientation Data page). As an
example, the last IAU 2000 B Bulletin published is number 263. IERS also stopped publishing
data for the IERS convention 2003, they have switched to IERS conventions 2010. The annual data
(EOP 05 C08 file) are still published. We advise then that you update these files regularly as
the IERS publish them.

Concerning UTC leap seconds, as of mid 2016, the last one was introduced at the end of June 2015.

## Runtime errors

### I get an error "no IERS UTC-TAI history data loaded" (or something similar in another language). What does it mean?

This error is probably *the* most frequent one, or at least it's the first one new users encounter.

Orekit needs some external data to be loaded in order to run. This includes UTC-TAI history for leap
seconds handling, Earth Orientation Parameters for transforms to and from Earth fixed frames, or planetary
ephemerides for Sun direction, for example.

The error message "no IERS UTC-TAI history data loaded" means the UTC-TAI history file which is used for leap
seconds management was not found. As leap seconds are used each time a UTC date is used, this message is
often seen very early and is the first one unsuspecting users experience. It often means the user forgot
to configure Orekit to load data. Orekit supports by default either the IERS UTC-TAI.history file or the
USNO tai-utc.dat file. If either file is found in the Orekit configuration, it will be automatically loaded
and the message should not appear.

Configuring data loading is explained in the configuration page For a start, the simplest configuration
is to download the orekit-data.zip file from the download page, to unzip it anywhere you want, note the
path of the orekit-data folder that will be created and add the following lines at the start of
your program:

    File orekitData = new File("/path/to/the/folder/orekit-data");
    DataProvidersManager manager = DataProvidersManager.getInstance();
    manager.addProvider(new DirectoryCrawler(orekitData));

Using a folder allows one to change the data in it after the initial download, e.g., adding new EOP files as they
are published by IERS.
