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

# Overview

  OREKIT (ORbits Extrapolation KIT) is a free low-level space dynamics library
  written in Java.

  It provides basic elements (orbits, dates, attitude, frames ...) and
  various algorithms to handle them (conversions, analytical and numerical
  propagation, pointing ...).

## Features

  * Time

    * high accuracy absolute dates
    * time scales (TAI, UTC, UT1, GPS, TT, TCG, TDB, TCB, GMST, GST, GLONASS, QZSS ...)
    * transparent handling of leap seconds

  * Geometry

    * frames hierarchy supporting fixed and time-dependent
      (or telemetry-dependent) frames
    * predefined frames (EME2000/J2000, ICRF, GCRF, ITRF93, ITRF97, ITRF2000, ITRF2005, ITRF2008
      and intermediate frames, TOD, MOD, GTOD and TOD frames, Veis, topocentric, tnw and qsw
      local orbital frames, Moon, Sun, planets, solar system barycenter,
      Earth-Moon barycenter, ecliptic)
    * user extensible (used operationally in real time with a set of about 60 frames on
      several spacecraft)
    * transparent handling of IERS Earth Orientation Parameters (for both new CIO-based frames
      following IERS 2010 conventions and old equinox-based frames)
    * transparent handling of JPL DE 4xx (405, 406 and more recent) and INPOP ephemerides
    * transforms including kinematic combination effects
    * composite transforms reduction and caching for efficiency
    * extensible central body shapes models (with predefined spherical and ellipsoidic shapes)
    * Cartesian and geodesic coordinates, kinematics
    * Computation of Dilution Of Precision (DOP) with respect to GNSS constellations

  * Spacecraft state

    * Cartesian, elliptical Keplerian, circular and equinoctial parameters
    * Two-Line Elements
    * transparent conversion between all parameters
    * automatic binding with frames
    * attitude state and derivative
    * Jacobians
    * mass management
    * user-defined associated state
      (for example battery status, or higher order derivatives, or anything else)

  * Maneuvers

    * analytical models for small maneuvers without propagation
    * impulse maneuvers for any propagator type
    * continuous maneuvers for numerical propagator type

  * Propagation

    * analytical propagation models
      (Kepler, Eckstein-Heschler, SDP4/SGP4 with 2006 corrections)
    * numerical propagators
      * central attraction
      * gravity models including time-dependent like trends and pulsations
        (automatic reading of ICGEM (new Eigen models), SHM (old Eigen models),
        EGM and GRGS gravity field files formats, even compressed)
      * atmospheric drag (DTM2000, Jacchia-Bowman 2006, Harris-Priester and simple exponential models),
        and Marshall solar Activity Future Estimation
      * third body attraction (with data for Sun, Moon and all solar systems planets)
      * radiation pressure with eclipses
      * solid tides, with or without solid pole tide
      * ocean tides, with or without ocean pole tide
      * general relativity
      * multiple maneuvers
      * state of the art ODE integrators (adaptive stepsize with error control,
        continuous output, switching functions, G-stop, step normalization ...)
      * computation of Jacobians with respect to orbital parameters and selected
        force models parameters
      * serialization mechanism to store complete results on persistent storage for
        later use
    * semi-analytical propagation model (DSST) with customizable force models
    * tabulated ephemerides
      * file based
      * memory based
      * integration based
    * specialized GPS propagation, using SEM or YUMA files
    * unified interface above analytical/numerical/tabulated propagators for easy
      switch from coarse analysis to fine simulation with one line change
    * all propagators can be used in several different modes
      * slave mode: propagator is driven by calling application
      * master mode: propagator drives application callback functions
      * ephemeris generation mode: all intermediate results are stored during
        propagation and provided back to the application which can navigate at will
        through them, effectively using the propagated orbit as if it was an
        analytical model, even if it really is a numerically propagated one, which
        is ideal for search and iterative algorithms
    * handling of discrete events during integration
      (models changes, G-stop, simple notifications ...)
    * predefined discrete events
      * eclipse (both umbra and penumbra)
      * ascending and descending node crossing
      * anomaly, latitude argument or longitude argument crossings,
        with either true, eccentric or mean angles
      * apogee and perigee crossing
      * alignment with some body in the orbital plane
        (with customizable threshold angle)
      * angular separation thresholds crossing between spacecraft and a beacon (typically the Sun)
        as seen from an observer (typically a ground station)
      * raising/setting with respect to a ground location
        (with customizable triggering elevation)
      * date
      * latitude, longitude, altitude crossing
      * latitude, longitude extremum
      * elevation extremum
      * anomaly, latitude argument, or longitude argument crossings, either true, mean or eccentric
      * moving target detection in spacecraft sensor Field Of View (any shape, with special case for circular)
      * spacecraft detection in ground based Field Of View (any shape)
      * sensor Field Of View (any shape) overlapping complex geographic zone
      * complex geographic zones traversal
      * impulse maneuvers occurrence
    * possibility of slightly shifting events in time (for example to switch from
      solar pointing mode to something else a few minutes before eclipse entry and
      reverting to solar pointing mode a few minutes after eclipse exit)
    * possibility of filtering events based on their direction (for example to detect
      only eclipse entries and not eclipse exits)
    * possibility of filtering events based on an external enabling function (for
      example to detect events only during selected orbits and not others)

  * Attitude

    * extensible attitude evolution models
    * predefined laws
      * central body related attitude (nadir pointing, center pointing, target pointing, yaw compensation, yaw-steering),
      * orbit referenced attitudes (LOF aligned, offset on all axes),
      * space referenced attitudes (inertial, celestial body-pointed, spin-stabilized)
      * tabulated attitudes, either respective to inertial frame or respective to Local Orbital Frames

  * Orbit determination
  
    * batch least squares fitting of orbit
    * several predefined measurements
      * range
      * range rate
      * azimuth
      * elevation
      * position-velocity
    * possibility to add custom measurements
    * several predefined modifiers
      * tropospheric effects
      * ionospheric effects
      * station offsets
      * biases
      * delays
    * possibility to add custom measurement modifiers (even for predefined events)

  * Orbit file handling
  
    * loading of SP3-a and SP3-c orbit files
    * loading of CCSDS Orbit Data Messages (both OPM, OEM, and OMM types are supported)
    * loading of SEM and YUMA files for GPS constellation

  * Earth models
  
    * tropospheric delay (modified Saastamoinen)
    * tropospheric refraction correction angle (Recommendation ITU-R P.834-7 and Saemundssen's formula quoted by Meeus)
    * geomagnetic field (WMM, IGRF)
    * geoid model from any gravity field
    * tessellation of zones of interest as tiles
    * sampling of zones of interest as grids of points
    
  * Customizable data loading

    * loading from local disk
    * loading from classpath
    * loading from network (even through internet proxies)
    * support for zip archives
    * support from gzip compressed files
    * plugin mechanism to delegate loading to user defined database or data access library

  * Localized in several languages

    * English
    * French
    * Galician
    * German
    * Greek
    * Italian
    * Norwegian
    * Romanian
    * Spanish

  * The top level packages provided by Orekit are the following one:

![Orekit top packages](./images/design/top-packages.png)

## Free software

Orekit is freely available both in source and binary formats, with all related
documentation and tests.

It is distributed under the [Apache License Version 2.0](./license.html). This
is a well known business-friendly license. This means anybody can use it to build
any application, free or not. There are no strings attached to your own code.

Everybody is encouraged to use Orekit as a common low level layer to improve
interoperability in space systems.

## Maintained library

Orekit has been in development since 2002 inside [CS Systèmes
d'Information](http://www.c-s.fr/) and is still used and maintained by its space dynamics experts.

Orekit has already been successfully used during the real time monitoring of the rendez-vous
phase between the Automated Transfer Vehicle (ATV) and the International Space Station (ISS)
by the Centre National d'Études Spatiales (CNES, the French space agency) and European Space
Agency (ESA).

Orekit has been selected in early 2011 by CNES to be the basis of its next generation space
flight dynamics systems, including operational systems, study systems and mission analysis
systems.

It has been used in numerous studies and for operational systems among the world.
