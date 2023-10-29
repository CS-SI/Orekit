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

# Overview

  OREKIT is a free low-level space dynamics library written in Java.

  It provides basic elements (orbits, dates, attitude, frames ...) and
  various algorithms to handle them (conversions, analytical and numerical
  propagation, pointing ...).

## Features

  * Time

    * high accuracy absolute dates
    * time scales (TAI, UTC, UT1, GPS, TT, TCG, TDB, TCB, GMST, GST, GLONASS, QZSS, BDT, IRNSS ...)
    * transparent handling of leap seconds
    * support for CCSDS time code standards

  * Geometry

    * frames hierarchy supporting fixed and time-dependent
      (or telemetry-dependent) frames
    * predefined frames (EME2000/J2000, ICRF, GCRF, all ITRF from 1988 to 2020
      and intermediate frames, TOD, MOD, GTOD and TOD frames, Veis, topocentric, TEME and PZ-90.11 frames,
      tnw and qsw local orbital frames, relative encounter frames, Moon, Sun, planets, solar system barycenter,
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
    * computation of Dilution Of Precision (DOP) with respect to GNSS constellations
    * projection of sensor Field Of View footprint on ground for any FoV shape

  * Spacecraft state

    * Cartesian, Keplerian (elliptic, parabolic, hyperbolic), circular and equinoctial parameters, with non-Keplerian
      derivatives if available
    * Two-Line Elements (TLE)
    * Two-Line Elements generation using Fixed-Point algorithm or Least Squares Fitting
    * transparent conversion between all parameters
    * automatic binding with frames
    * attitude state and derivative
    * Jacobians
    * mass management
    * user-defined associated state
      (for example battery status, or higher order derivatives, or anything else)

  * Covariance

    * covariance propagation using the state transition matrix
	* covariance extrapolation using a Keplerian model
    * covariance frame transformation (inertial, Earth fixed, and local orbital frames)
    * covariance type transformation (cartesian, keplerian, circular, and equinoctial)
    * covariance interpolation based on the blending model

  * Maneuvers

    * analytical models for small maneuvers without propagation
    * impulse maneuvers for any propagator type
    * continuous maneuvers for numerical propagator type
    * configurable low thrust maneuver model based on event detectors
    * used-defined propulsion models intended to be used with maneuver class (constant and piecewise polynomials already provided by the library)
    * user-friendly interface for the maneuver triggers

  * Propagation

    * analytical propagation models
        * Kepler
        * Eckstein-Heschler
        * Brouwer-Lyddane with Warren Phipps' correction for the critical inclination of 63.4°
          and the perturbative acceleration due to atmospheric drag
        * SDP4/SGP4 with 2006 corrections
        * GNSS: GPS, QZSS, Galileo, GLONASS, Beidou, IRNSS and SBAS
    * numerical propagators
        * central attraction
        * gravity models including time-dependent like trends and pulsations
          (automatic reading of ICGEM (new Eigen models), SHM (old Eigen models),
          EGM and GRGS gravity field files formats, even compressed)
        * atmospheric drag
        * third body attraction (with data for Sun, Moon and all solar systems planets)
        * radiation pressure with eclipses (multiple oblate spheroids occulting bodies, multiple coefficients for bow and wing models)
        * solid tides, with or without solid pole tide
        * ocean tides, with or without ocean pole tide
        * Earth's albedo and infrared
        * empirical accelerations to account for the unmodeled forces
        * general relativity (including Lense-Thirring and De Sitter corrections)
        * multiple maneuvers
        * state of the art ODE integrators (adaptive stepsize with error control,
          continuous output, switching functions, G-stop, step normalization ...)
        * serialization mechanism to store complete results on persistent storage for
          later use
        * propagation in non-inertial frames (e.g. for Lagrange point halo orbits)
    * semi-analytical propagation model (DSST)
        * central attraction
        * gravity models
        * J2-squared effect (Zeis model)
        * atmospheric drag
        * third body attraction
        * radiation pressure with eclipses
    * computation of Jacobians with respect to orbital parameters and selected
      model parameters for numerical, semi-analytical, and analytical propagation
      models
    * trajectories around Lagragian points using CR3BP model
    * tabulated ephemerides
        * file based
        * memory based
        * integration based
    * Taylor-algebra (or any other real field) version of most of the above propagators,
        with all force models, events detection, orbits types, coordinates types and frames
        allowing high order uncertainties and derivatives computation or very fast Monte-Carlo
        analyzes
    * unified interface above analytical/numerical/tabulated propagators for easy
      switch from coarse analysis to fine simulation with one line change
    * all propagators can manage the time loop by themselves and handle callback
      functions (called step handlers) from the calling application at each time step.
        * step handlers can be called at discrete time at regular time steps, which are
          independent of propagator time steps
        * step handlers can be called with interpolators valid throughout one propagator
          time step, which can have varying sizes
        * step handlers can be switched off completely, when only final state is desired
        * special step handlers are provided for a posteriori ephemeris generation: all
          intermediate results are stored during propagation and provided back to the application
          which can navigate at will through them, effectively using the propagated orbit as if
          it was analytical model, even if it really is a numerically propagated one, which
          is ideal for search and iterative algorithms
        * several step handlers can be used simultaneously, so it is possible to have a fine
          grained fixed time step to log state in a huge file, and have at the same time a
          coarse grained time step to display progress for user at a more human-friendly rate,
          this feature can also be used for debugging purpose, by setting up a temporary
          step handler alongside the operational ones
    * handling of discrete events during integration
      (models changes, G-stop, simple notifications ...)
    * predefined discrete events
        * eclipse (both umbra and penumbra)
        * ascending and descending node crossing
        * apogee and perigee crossing
        * alignment with some body in the orbital plane
          (with customizable threshold angle)
        * angular separation thresholds crossing between spacecraft and a beacon (typically the Sun)
          as seen from an observer (typically a ground station)
        * raising/setting with respect to a ground location
          (with customizable triggering elevation and ground mask, optionally considering refraction)
        * date and on-the-fly resetting countdown
        * date interval with parameter-driven boundaries
        * latitude, longitude, altitude crossing
        * latitude, longitude extremum
        * elevation extremum
        * anomaly, latitude argument, or longitude argument crossings, either true, mean or eccentric
        * moving target detection (with optional radius) in spacecraft sensor Field Of View (any shape, with special case for circular)
        * spacecraft detection in ground based Field Of View (any shape)
        * sensor Field Of View (any shape) overlapping complex geographic zone
        * complex geographic zones traversal
        * inter-satellites direct view (with customizable skimming altitude)
        * ground at night
        * impulse maneuvers occurrence
        * geomagnetic intensity
		* extremum approach for TCA (Time of Closest Approach) computing
    * possibility of slightly shifting events in time (for example to switch from
      solar pointing mode to something else a few minutes before eclipse entry and
      reverting to solar pointing mode a few minutes after eclipse exit)
    * events filtering based on their direction (for example to detect
      only eclipse entries and not eclipse exits)
    * events filtering  based on an external enabling function (for
      example to detect events only during selected orbits and not others)
    * events combination with boolean operators
    * ability to run several propagators in parallel and manage their states
      simultaneously throughout propagation

  * Attitude

    * extensible attitude evolution models
    * predefined laws
        * central body related attitude (nadir pointing, center pointing, target pointing, yaw compensation, yaw-steering),
        * orbit referenced attitudes (LOF aligned, offset on all axes),
        * space referenced attitudes (inertial, celestial body-pointed, spin-stabilized)
        * tabulated attitudes, either respective to inertial frame or respective to Local Orbital Frames
        * specific law for GNSS satellites: GPS (block IIA, block IIF, block IIF), GLONASS, GALILEO, BEIDOU (GEO, IGSO, MEO)
        * torque-free for general (non-symmetrical) body
    * loading and writing of CCSDS Attitude Data Messages (both AEM, APM and ACM types are supported, in both KVN and XML formats, standalone or in combined NDM)
    * exporting of attitude ephemeris in CCSDS AEM and ACM file format

  * Orbit determination
  
    * batch least squares fitting
        * optimizers choice (Levenberg-Marquardt or Gauss-Newton)
        * decomposition algorithms choice (QR, LU, SVD, Cholesky)
        * choice between forming normal equations or not
    * sequential batch least squares fitting
        * sequential Gauss-Newton optimizer
        * decomposition algorithms choice (QR, LU, SVD, Cholesky)
        * possibility to use an initial covariance matrix
    *  Kalman filtering
        * customizable process noise matrices providers
        * time dependent process noise provider
        * implementation of the Extended Kalman Filter
        * implementation of the Extended Semi-analytical Kalman Filter
        * implementation of the Unscented Kalman Filter
        * implementation of the Unscented Semi-analytical Kalman Filter
    * parameters estimation
        * orbital parameters estimation (or only a subset if desired)
        * force model parameters estimation (drag coefficients, radiation pressure coefficients,
          central attraction, maneuver thrust, flow rate or start/stop epoch)
        * measurements parameters estimation (biases, satellite clock offset, station clock offset,
          station position, pole motion and rate, prime meridian correction and rate, total zenith
          delay in tropospheric correction)
    * orbit determination can be performed with numerical, DSST, SDP4/SGP4, Eckstein-Hechler, Brouwer-Lyddane, or Keplerian propagators
    * ephemeris-based orbit determination to estimate measurement parameters like station biases or clock offsets
    * multi-satellites orbit determination
    * initial orbit determination methods (Gibbs, Gooding, Lambert and Laplace)
    * ground stations displacements due to solid tides
    * ground stations displacements due to ocean loading (based on Onsala Space Observatory files in BLQ format)
    * ground stations displacements due to plate tectonics
    * several predefined measurements
        * range
        * range rate (one way and two way)
        * turn-around range
        * azimuth/elevation
        * right ascension/declination
        * position-velocity
        * position
        * inter-satellites range (one way and two way)
        * inter-satellites GNSS phase
        * GNSS code
        * GNSS phase with integer ambiguity resolution and wind-up effect
        * Time Difference of Arrival (TDOA)
        * Frequency Difference of Arrival (FDOA)
        * Bi-static range and range rate
        * multiplexed
    * possibility to add custom measurements
    * loading of ILRS CRD laser ranging measurements file
    * loading and writing of CCSDS Tracking Data Messages (in both KVN and XML formats, standalone or in combined NDM)
    * several predefined modifiers
        * tropospheric effects
        * ionospheric effects
        * clock relativistic effects (including J2 correction)
        * station offsets
        * biases
        * delays
        * Antenna Phase Center
        * Shapiro relativistic effect
    * possibility to add custom measurement modifiers (even for predefined events)
    * combination of GNSS measurements
        * dual frequency combination of measurements
          (Geometry-free, Ionosphere-free, Narrow-lane, Wide-lane and Melbourne-Wübbena)
        * single frequency combination of measurements
          (Phase minus code and GRAPHIC)
    * possibility to parse CCSDS Tracking Data Message files
    * measurements generation
        * with measurements feasibility triggered by regular event detectors
          (ground visibility, ground at night, sunlit satellite, inter satellites
           direct view, boolean combination...)
        * with measurement scheduling as fixed step streams (optionally aligned with round UTC time)
        * with measurement scheduling as high rate bursts rest periods (optionally aligned with round UTC time)
        * possibility to customize measurement scheduling

  * GNSS

    * computation of Dilution Of Precision
    * loading of ANTEX antenna models file
    * loading and writing of RINEX observation files (version 2, 3, and 4)
    * loading of RINEX navigation files (version 2, 3, and 4)
    * support for Hatanaka compact RINEX format
    * loading of SINEX file (can load station positions, eccentricities, EOPs, and Differential Code Biases)
    * loading of RINEX clock files (version 2 and version 3)
    * parsing of IGS SSR messages for all constellations (version 1)
    * parsing of RTCM messages (both ephemeris and correction messages)
    * parsing of GPS RF link binary message
    * Hatch filters for GNSS measurements smoothing
    * implementation of Ntrip protocol
    * decoding of GPS navigation messages

  * Orbit file handling
  
    * loading and writing of SP3 orbit files (from version a to d)
    * loading and writing of CCSDS Orbit Data Messages (OPM, OEM, OMM and OCM types are supported, in both KVN and XML formats, standalone or in combined NDM)
    * loading of SEM and YUMA files for GPS constellation
    * exporting of ephemeris in CCSDS OEM and OCM file formats
    * loading of ILRS CPF orbit files
    * exporting of ephemeris in STK format

  * Earth models
  
    * atmospheric models (DTM2000, Jacchia-Bowman 2008, NRL MSISE 2000, Harris-Priester and simple exponential models), and Marshall solar Activity Future Estimation, optionally with lift component
    * support for CSSI space weather data
    * support for SOLFSMY and DTC data for JB2008 atmospheric model
    * tropospheric delay (modified Saastamoinen, estimated, fixed)
    * tropospheric mapping functions (Vienna 1, Vienna 3, Global, Niell)
    * tropospheric refraction correction angle (Recommendation ITU-R P.834-7 and Saemundssen's formula quoted by Meeus)
    * tropospheric model for laser ranging (Marini-Murray, Mendes-Pavlis)
    * Klobuchar ionospheric model (including parsing α and β coefficients from University of Bern Astronomical Institute files)
    * Global Ionospheric Map (GIM) model
    * NeQuick ionospheric model
    * VTEC estimated ionospheric model with Single Layer Model (SLM) ionospheric mapping function
    * Global Pression and Temperature models (GPT and GPT2)
    * geomagnetic field (WMM, IGRF)
    * geoid model from any gravity field
    * displacement of ground points due to tides
    * tessellation of zones of interest as tiles
    * sampling of zones of interest as grids of points
	* construction of trajectories using loxodromes (commonly, a rhumb line)

  * Collisions

    * loading and writing of CCSDS Conjunction Data Messages (CDM in both KVN and XML formats)
    * 2D probability of collision computing methods assuming short term encounter and spherical bodies :
      
      * Chan 1997
      * Alfriend 1999
      * Alfriend 1999 (maximum version)
      * Alfano 2005
      * Patera 2005 (custom Orekit implementation) (recommended)
      * Laas 2015 (recommended)

  * Customizable data loading

    * loading by exploring folders hierarchy on local disk
    * loading from explicit lists of files on local disk
    * loading from classpath
    * loading from network (even through internet proxies)
    * support for zip archives
    * automatic decompression of gzip compressed (.gz) files upon loading
    * automatic decompression of Unix compressed (.Z) files upon loading
    * automatic decompression of Hatanaka compressed files upon loading
    * plugin mechanism to add filtering like custom decompression algorithms, deciphering or monitoring
    * plugin mechanism to delegate loading to user defined database or data access library
    * possibility to have different data context (a way to separate sets of EOP, leap seconds, etc)

  * Localized in several languages

    * Danish
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

It is distributed under the [Apache License Version 2.0](./licenses.html). This
is a well known business-friendly license. This means anybody can use it to build
any application, free or not. There are no strings attached to your own code.

Everybody is encouraged to use Orekit as a common low level layer to improve
interoperability in space systems.

## Maintained library

Orekit has been in development since 2002 inside [CS GROUP](https://www.csgroup.eu/)
and is still used and maintained by its
experts and an open community. It is ruled by a meritocratic governance
model and the Project Management Committee involves actors from
industry (CS, Thales Alenia Space, Applied Defense Solutions), research
(Naval Research Laboratory), agencies (European Space Operations Centre,
European Space Research and Technology Centre) and academics (University
at Buffalo, Institut National Supérieur de l'Aéronautique et de l'Espace - Sup'Aéro).

Orekit has already been successfully used during the real time monitoring of the rendez-vous
phase between the Automated Transfer Vehicle (ATV) and the International Space Station (ISS)
by the Centre National d'Études Spatiales (CNES, the French space agency) and European Space
Agency (ESA).

Orekit has been selected in early 2011 by CNES to be the basis of its next generation space
flight dynamics systems, including operational systems, study systems and mission analysis
systems.

It has been used in numerous studies and for operational systems among the world.
