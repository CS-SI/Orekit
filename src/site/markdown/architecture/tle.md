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

# TLE

The `org.orekit.propagation.analytical.tle` package provides classes to read, and
extrapolate from, orbital elements in Two-Line Elements (TLE) format.

## TLE Presentation

TLE orbits are supplied in two-line element format, then converted 
into an internal format for easier retrieval and future extrapolation.
All the values provided by a TLE only make sense when translated by the corresponding
propagator. Even when no extrapolation in time is needed, state information 
(position and velocity coordinates) can only be computed through the propagator. 
Untreated values like inclination, RAAN, mean Motion, etc. can't be used by 
themselves without loss of precision.

The implemented TLE model conforms to new 2006 corrected model.
More information on the TLE format can be found on the
[CelesTrak](http://www.celestrak.com/) website.

## Evolution

The TLE orbit representation is de-correlated from other orbit representations 
provided by `Orbits` package. It is due to the fact that TLE representation depends on 
a very specific dynamic model, which is not compatible with `Orbits` models in present 
architecture.

It is considered to be closer to a propagation model than to an orbit representation and
has been moved within the `org.orekit.propagation` package since 6.0. This move allowed the
rich features from the global propagation framework (master/slave/ephemeris mode, events
handling) to be used with TLE.
