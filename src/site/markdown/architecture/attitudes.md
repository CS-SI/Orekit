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

# Attitudes

The `org.orekit.attitudes` package provides classes to represent simple attitudes.
	
## Attitudes Presentation
 
Some force models, such as the atmospheric drag for maneuvers, need to
know the spacecraft orientation in an inertial frame. Orekit uses a simple
container for Attitude which includes both the geometric part (i.e. rotation) 
and the kinematic part (i.e. the instant spin axis). The components 
held by this container allow to convert vectors from inertial frame to spacecraft 
frame along with their derivatives. This container is similar in spirit to the various
extensions of the abstract Orbit class: it represents a state at a specific instant.

In order to represent attitude evolution in time, the AttitudeProvider interface 
is available. 
At a higher level, attitude laws defined by a ground pointing law are also available. 
This corresponds to a _real_ situation where satellite attitude law is defined in order
to perform a mission, i.e. pointing a specified point/area. All these laws are collected
under an abstract class called "GroundPointing".
Finally, there exist attitude laws that wrap a "base" attitude law, and add to this 
base attitude law a complementary rotation in order to fulfill specific mission constraints.

## Description of attitudes providers

### Basic attitude laws

* InertialProvider, which represents an inertial attitude law, perfectly 
  aligned with the EME2000 frame.
  
* FixedRate, which represents a rotation around a fixed axis at
  constant rate.
  
* CelestialBodyPointed, i.e satellite pointing axis directed towards given 
  celestial body.
    
* SpinStabilized, which is handled as a wrapper for an underlying
  non-rotating law. This underlying law is typically an instance
  of CelestialBodyPointed with the pointing axis equal to
  the rotation axis, but can in fact be anything...
  
* LofOffset, defined as a given angular offset around three axes from local orbital 
  frame at given date.

* TabulatedLofOffset, defined by interpolating within a user-provided ephemerides
  with resepct to a local orbital frame,  using any number of interpolation points and
  either using or ignoring the tabulated rotation rates.

* TabulatedProvider, defined by interpolating within a user-provided ephemerides
  with respect to an inertial frame,  using any number of interpolation points and
  either using or ignoring the tabulated rotation rates.

### Ground pointing laws

These classes are designed to represent attitude laws used to fulfill pointing missions.
Several pointing laws are modelized :
  
* BodyCenterPointing, where satellite pointing axis is directed towards 
  reference body frame center.
    
* LofOffsetPointing, defined by a lof offset simple attitude law 
  and completed with ground pointing corresponding functions.
    
* NadirPointing, where satellite pointing axis is aligned on subtrack 
  point vertical direction.
    
* TargetPointing, where satellite pointing axis is directed towards given 
  point on reference body shape.

* GroundPointingWrapper, which is an abstract class used for complex pointing 
  laws described herebelow.

  All these ground pointing laws are relative to corresponding body frame,
  which is used for their construction. Depending on their nature, each ground pointing 
  law also have its own specific construction arguments.
  For each of these laws, satellite attitude state at any time in any given frame 
  can be computed, as well as the observed ground point, or a target in the body frame.

### Complex pointing laws

Several classes have been implemented in order to represent attitude laws in which a
_base_ attitude law is used, and a _complementary_ rotation is added in order to fulfill specific 
mission constraints. They are gathered under abstract class `GroundPointingWrapper`.
At this point, implemented laws of this kind are:
  
* YawCompensation: this law is used to fulfill ground observation constraints
  that reduce geometrical distortion. Yaw angle is changed a little from 
  the basic ground pointing attitude, so that the apparent motion of ground points is 
  along a prescribed axis (orthogonal to the optical sensors rows), taking into account 
  all effects. It is the impact of earth proper rotation on ground points that is 
  compensated.

* YawSteering: this law is mainly used for low Earth orbiting satellites 
  with no mission-related constraints on yaw angle. It sets the yaw angle in
  such a way that the solar arrays have maximal lighting without changing the
  roll and pitch.

### Attitude sequence

The `AttitudeSequence` class manages a sequence of different attitude laws activated
in rows according to switching events. Only one attitude law in the sequence is in
an active state. When one of the switch events associated with the active law occurs,
the active law becomes the one specified with the event.

It is possible to have perpetually alternating laws, for example when eclipse entry
triggers a switch from a day light attitude to a night attitude and eclipse exit
triggers the reverse (possibly with intermediate transition modes).

### Package organization

![attitude class diagram](../images/design/attitude-class-diagram.png)
