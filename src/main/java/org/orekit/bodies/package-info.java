/* Copyright 2002-2016 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * CS licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 *
 *  This package provides interface to represent the position and geometry of
 *  space objects such as stars, planets or asteroids.
 *
 *<p>
 *  The position of celestial bodies is represented by the {@link
 *  org.orekit.bodies.CelestialBody} interface. This interface provides the methods
 *  needed to either consider the body as an external one for its gravity or lighting
 *  influence on spacecraft (typically in perturbing force computation) or as an internal
 *  one with its own frame.
 *</p>
 *
 *<p>
 *  The {@link org.orekit.bodies.CelestialBodyFactory} class is a factory providing several
 *  predefined instances implementing the {@link org.orekit.bodies.CelestialBody}
 *  interface for the main solar system bodies. The Sun, the Moon, the eight planets and
 *  the Pluto dwarf planet are the supported bodies. In addition to these real bodies,
 *  two points are supported for convenience as if they were real bodies: the solar system
 *  barycenter and the Earth-Moon barycenter. The {@link org.orekit.bodies.CelestialBodyFactory}
 *  factory relies on the JPL DE 405, 406 or similar binary ephemerides files to compute all
 *  positions and velocities. Note that the binary files are used, not the ASCII ones,
 *  regardless of the processor endianness.
 *</p>
 *
 *<p>
 *  As an example, computing the position of the Sun and the Moon in the EME2000 frame,
 *  this done as follows:
 *</p>
 *
 *<pre>
 *  CelestialBody sun      = CelestialBodyFactory.getSun();
 *  CelestialBody moon     = CelestialBodyFactory.getMoon();
 *  Vector3D sunInEME2000  = sun.getPVCoordinates(date, Frame.getEME2000()).getPosition();
 *  Vector3D moonInEME2000 = moon.getPVCoordinates(date, Frame.getEME2000()).getPosition();
 *</pre>
 *
 *<p>
 *  Since the supported bodies implement the {@link org.orekit.bodies.CelestialBody}
 *  interface, they all provide their own body-centered inertial frame, hence adding a few
 *  more frames to the ones provided by the {@link org.orekit.frames} package. Since the
 *  frames tree is rooted at an Earth-centered frame, the solar system bodies frames tree
 *  does not seems in canonical shape. This of course is only a side effect of the
 *  arbitrary choice of GCRF as the root frame and has no effect at all on computations.
 *</p>
 *
 *<p>
 *  The shape of celestial bodies is represented by the {@link org.orekit.bodies.BodyShape}
 *  interface.
 *</p>
 *
 *<p>
 *  Only one implementation is provided by OREKIT for now: the {@link
 *  org.orekit.bodies.OneAxisEllipsoid} class which represents the natural flattened shape
 *  of big space rotating bodies like planets or the Sun.
 *</p>
 *
 *<p>
 *  For asteroids, it is expected that users provide their own shape models, for example
 *  based on triangulation. They should implement the {@link org.orekit.bodies.BodyShape}
 *  interface in order to be used by Orekit.
 *</p>
 *
 *<p>
 *  When using {@link org.orekit.bodies.OneAxisEllipsoid} body representation, points are
 *  generally described in associated body frame, by so-called <i>geodetic</i> coordinates
 *  (longitude, latitude, altitude). The {@link org.orekit.bodies.GeodeticPoint} class allows
 *  handling of such coordinates. It is a simple container that does not provide processing
 *  methods.
 *</p>
 *
 *@author L. Maisonobe
 *
 */
package org.orekit.bodies;
