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

# Bodies

The `org.orekit.bodies` package provides an interface to representation of the
position and geometry of space objects such as stars, planets or asteroids.
	
## Position

The position of celestial bodies is represented by the `CelestialBody` interface.
This interface provides the methods needed to either consider the body as an
external one for its gravity or lighting influence on spacecraft (typically in
perturbing force computations) or as an internal one with its own frame.

The `CelestialBodyFactory` class is a factory providing several predefined instances
implementing the `CelestialBody` interface for the main solar system bodies. The Sun,
the Moon, the eight planets and the Pluto dwarf planet are the supported bodies. In
addition to these real bodies, two points are supported for convenience as if they
were real bodies: the solar system barycenter and the Earth-Moon barycenter.
By default, the `CelestialBodyFactory` retrieve positions and velocities from binary
ephemerides files compatible with the the JPL DE formats like DE 405, DE 406, DE 423 ...
as well as the IMCCE Inpop ephemerides which share a similar format. This default
handling can be overridden by defining a user specific loader for JPL ephemerides.

![celestial bodies class diagram](../images/design/celestial-bodies-class-diagram.png)

As an example, computing the position of the Sun and the Moon, in the EME2000 frame,
is done as follows:

    CelestialBody sun  = CelestialBodyFactory.getSun();
    CelestialBody moon = CelestialBodyFactory.getMoon();
    Vector3D sunInEME2000 =
        sun.getPVCoordinates(date, FramesFactory.getEME2000()).getPosition();
    Vector3D moonInEME2000 =
        moon.getPVCoordinates(date, FramesFactory.getEME2000()).getPosition();

Since the supported bodies implement the `CelestialBody` interface, they all provide
their own body-centered inertially-oriented frame and their own body-centered
body-oriented frame, hence adding a few more frames to the ones
provided by the frames package. The body-oriented frames use the IAU pole and
prime meridian definition. Since the frames tree is rooted at an Earth-centered
frame, the solar system bodies frames tree does not seem to be in canonical form. This of
course is only a side effect of the arbitrary choice of GCRF as the root frame and has
no effect at all on computations. The solar system bodies frames tree is shown below:
 
![solar system frames](../images/solar-system-frames.png)

## Shape
 
The shape of celestial bodies is represented by the `BodyShape` interface. 

![body shapes class diagram](../images/design/bodyshape-class-diagram.png)

### Implementations

Only one implementation is provided by Orekit for now: the `OneAxisEllipsoid`
class, which represents the natural flattened shape of big space rotating bodies
like planets or the Sun.

For asteroids, it is expected that users will provide their own shape models, for example
based on tessellation. They should implement the `BodyShape` interface in order to
be used by Orekit.

### Geodetic coordinates

When using `OneAxisEllipsoid` body representation, points are generally described in 
associated body frame, by so-called _geodetic_ coordinates (longitude, latitude, altitude). 
The `GeodeticPoint` class allows handling of such coordinates. It is a simple container
that does not provide processing methods except basic computation of canonical topocentric
directions (Zenith/Nadir, East/West, North/South).
