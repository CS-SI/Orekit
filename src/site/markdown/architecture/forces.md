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

# Forces

The `org.orekit.forces` package provides the interface for the force models to be used by a 
`NumericalPropagator`.
  
## Forces presentation

Objects implementing the force model interface are intended to be added to a
numerical propagator before the propagation is started.
  
The propagator will call at each step the force model contribution computation
method, to be added to its time derivative equations. The force model instance 
will extract all the state data it needs (date, position, velocity, frame, attitude, 
mass) from the SpacecraftState parameter. 
From these state data, it will compute the perturbing acceleration. It
will then add this acceleration to the second parameter which will take this
contribution into account and will use the Gauss equations to evaluate its impact
on the global state derivative.

Force models that create discontinuous acceleration patterns (typically for maneuvers
start/stop or solar eclipses entry/exit) must provide one or more events detectors to 
the propagator thanks to their `getEventsDetectors()` method. This method
is called once just before propagation starts. The events states will be checked by
the propagator to ensure accurate propagation and proper events handling.


## Available force models

The force models implemented are as follows:

* atmospheric drag forces, taking attitude into account if spacecraft shape is defined,
  
* central gravity forces, including time-dependent parts (linear trends and
  pulsation at several different periods). Our implementation is based on
  S. A. Holmes and W. E. Featherstone (Department of Spatial Sciences,
  Curtin University of Technology, Perth, Australia) 2002 paper:
  _A unified approach to the Clenshaw summation and the recursive computation
   of very high degree and order normalised associated Legendre functions_
  (Journal of Geodesy (2002) 76: 279–299).

* third body gravity force. Data for all solar system bodies is available,
  based on JPL DE ephemerides or IMCCE INPOP ephemerides,

* solar radiation pressure force, taking into account force reduction in
  penumbra and no force at all during complete eclipse, and taking attitude
  into account if spacecraft shape is defined ; several occulting bodies
  can be defined as oblate spheroids

* Earth Albedo and IR emission force model. Our implementation is based on
  paper: _EARTH RADIATION PRESSURE EFFECTS ON SATELLITES", 1988, by
  P. C. Knocke, J. C. Ries, and B. D. Tapley_.

* solid tides, with or without solid pole tide,

* ocean tides, with or without ocean pole tide,

* post-Newtonian correction due to general relativity with
  De Sitter and Lense-Thirring terms,

* forces induced by maneuvers. At present, only constant thrust maneuvers 
  are implemented, with the possibility to define an impulse maneuver, thanks 
  to the event detector mechanism.

* parametric accelerations, to model lesser-known forces, estimating a few
  defining parameters from a parametric function using orbit determination.
  Typical parametric functions are polynomial (often limited to a constant term)
  and harmonic (often with either orbital period or half orbital period).
  An important operational example is the infamous GPS Y-bias.

## Spacecraft shapes

Surface forces like atmospheric drag or radiation pressure can use either
a simple spherical shape using the various `Isotropic` classes or a more
accurate `BoxAndSolarArraySpacraft` shape.

The spherical shape will be independent of attitude.

The box and solar array will consider the contribution of all box panels facing
the flux as computed from the current attitude, and also the contribution of a
pivoting solar array, whose orientation is a combination of the spacecraft body
attitude and either the true Sun direction or a regularized rotation angle.
The box can have any number of panels, and they can have any orientation as long
as the body remains convex. The coefficients (drag, lift, absorption, reflection)
are panel-dependent. As of 12.0, the box and solar array does not compute
yet shadowing effects.

All these shapes define various `ParameterDrivers` that can be used to control
dynamic parameters like drag coefficient or absorption coefficient. Several
conventions are available. For estimation purposes, it is possible to use a global
multiplication factor that is applied to the acceleration rather than attempting
to estimate several coefficients at once like absorption and specular reflection
for solar radiation pressure. For `BoxAndSolarArraySpacraft` shape, as each
panel has its own set of coefficients and this would not be observable, the
coefficients are fixed and only the global multiplication factor is available
and can be estimated. For `Isotropic` shapes, it is possible to estimate
either the coefficients or the global multiplication factor. Of course in
order to avoid ill-conditioned systems, users should not attempt to estimate
both a coefficient and a global multiplication factor at the same time in
`Isotropic` cases; they should select one parameter to estimate and let the
other one fixed.
