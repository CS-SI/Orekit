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

# Utils

The `org.orekit.utils` package provides methods for managing mathematical or geometrical objects.

## Utils Presentation

The `PVCoordinates` class is a mere container for a position and a velocity vector.
The `TimeStampedPVCoordinates` class is a simple extension adding a.
It is a very low level object which ought to be used associated with other objects.
In particular, it does not hold by itself any reference to the frame in which it is
define and the time at which it has this value. These data should be hold elsewhere.
  
The `PVCoordinatesProvider` is probably the most ubiquitous interface provided by Orekit.
It is used to represent almost anything that moves, be it a spacecraft (orbit propagators
extend this interface), some specific frames (spacecraft frame and topocentric frames
both implement this interface) or celestial bodies that can be retrieved from ephemerides.

The `Constants` interface only defines useful constants like Julian day duration,
standard gravity or Earth physical parameters for several models, it does not define
any processing method.

The `SecularAndHarmonic` class is a utility used for fitting orbital parameters
to linear combination of polynomials and periodic functions, and to either retrieve
the fitting parameters or compute osculating or mean values (including first and
second derivatives).

The `IERSConventions` enumerate gather all models that are defined by IERS and change
as conventions are updated, like precession-nutation models. It is mainly used as
a configuration parameter for frames, allonwing the user the choose which precession
nutation model to use when creating a Mean Of Date frame, simply by providing either
IERS_1996, IERS_2003 or IERS_2010 as the conventions parameters to the FrameFactory
`getMOD` factory method.

## Package organization

![utils class diagram](../images/design/utils-class-diagram.png)
