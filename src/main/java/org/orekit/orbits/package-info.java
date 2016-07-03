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
 * This package provides classes to represent orbits.
 *
 * <p>
 * It builds the base of all the other space mechanics tools.
 * It provides an abstract class, {@link org.orekit.orbits.Orbit}, extended
 * in four different ways corresponding to the different possible representations
 * of orbital parameters. As of version 3.0, {@link org.orekit.orbits.KeplerianOrbit
 * keplerian}, {@link org.orekit.orbits.CircularOrbit circular}, {@link
 * org.orekit.orbits.EquinoctialOrbit equinoctial} and {@link
 * org.orekit.orbits.CartesianOrbit cartesian} representations are supported.
 * </p>
 *
 * <h3>Design History</h3>
 * <p>
 * Early designs for the orbit package were much more complex than the current design.
 * Looking back at these designs, they tried to do far too much in a single class and
 * resulted in huge systems which were both difficult to understand, difficult to
 * use, difficult to maintain and difficult to reuse. They mixed several different notions:
 * <ul>
 *   <li>representation (keplerian, cartesian ...)</li>
 *   <li>kinematics (parameters) and dynamics (propagation)</li>
 *   <li>physical models (complete or simplified force models, often implicitly assumed with no real reference)</li>
 *   <li>filtering (osculating and centered or mean parameters, related to some models)</li>
 * </ul>
 * <p> They also often forgot all frames issues.
 *
 * <p>
 * The current design has been reached by progressively removing spurious layers and
 * setting them apart in dedicated packages. All these notions are now still handled,
 * but all in different classes that are cleanly linked to each other. Without knowing
 * it, we have followed Antoine de Saint Exup&eacute;ry's saying:
 * </p>
 * <p>
 * <i>It seems that perfection is reached not when there is nothing left to add, but
 * when there is nothing left to take away</i>.
 * </p>
 *
 * <p>
 * The current design is not perfect, of course, but it is easy to understand, easy to use,
 * easy to maintain and reusable.
 * </p>
 *
 * <h3>Current state versus evolving state</h3>
 * <p>
 * From the early design, the various orbit classes retained only the kinematical
 * notions at a single time. They basically represent the current state, and
 * serve as data holder. Most of methods provided by these orbits classes are
 * getters. Some of them (the non-ambiguous ones that must be always available) are
 * defined in the top {@link org.orekit.orbits.Orbit Orbit} abstract class
 * ({@link org.orekit.orbits.Orbit#getA() Orbit.getA()}, {@link
 * org.orekit.orbits.Orbit#getDate() Orbit.getDate()}, {@link
 * org.orekit.orbits.Orbit#getPVCoordinates() Orbit.getPVCoordinates()} ...). The
 * more specific ones depending on the type of representation are only defined
 * in the corresponding class ({@link
 * org.orekit.orbits.CircularOrbit#getCircularEx() CircularOrbit.getCircularEx()}
 * for example).
 * </p>
 *
 * <p>
 * It is important to note that some parameters are available even if they seem
 * out of place with regard to the representation. For example the semi-major axis is
 * available even in cartesian representation and the position/velocity even in
 * non-cartesian representation. This choice is a pragmatic one. These parameters
 * are really used in many places in algorithms, for computation related to
 * period (setting a convergence threshold or a search interval) or geometry
 * (computing swath or lighting). A side-effect of this choice is that all orbits
 * do include a value for μ, the acceleration coefficient of the central body.
 * This value is only used for the representation of the parameters and for conversion
 * purposes, it is <em>not</em> always the same as the value used for propagation (but
 * of course, using different values should be done with care).
 * </p>
 *
 * <p>
 * Orbits also include a reference to a defining frame. This allows transparent
 * conversions to other frames without having to externally preserve a mapping
 * between orbits and their frame: it is already done. As an example, getting
 * the position and velocity of a satellite given by a circular orbit in a ground
 * station frame is simply a matter of calling <tt>orbit.getPVCoordinates(stationFrame)</tt>,
 * regardless of the frame in which the orbit is defined (GCRF, EME2000, ...).
 * </p>
 *
 * <p>
 * Since orbits are used everywhere in space dynamics applications and since we
 * have chosen to restrict them to a simple state holder, all orbit classes are
 * guaranteed to be immutable. They are small objects and they are shared by
 * many parts. This change was done in 2006 and tremendously simplified the
 * library and the users applications that were previously forced to copy orbits
 * as an application of defensive programming, and that were plagued by difficult
 * to find bugs when they forgot to copy.
 * </p>
 *
 * <p>
 * For orbit evolution computation, this package is not sufficient. There is a
 * need to include notions of dynamics, forces models, propagation algorithms ...
 * The entry point for this is the {@link org.orekit.propagation.Propagator Propagator}
 * interface.
 * </p>
 *
 * <h3>Representations Conversions</h3>
 * <p>
 * All representations can be converted into all other ones. No error is triggered
 * if some conversion is ambiguous (like converting a perfectly circular orbit from
 * cartesian representation to keplerian representation, with an ambiguity on the
 * perigee argument). This design choice is the result of <strong>many</strong>
 * different attempts and pragmatic considerations. The rationale is that from a
 * physical point of view, there is no singularity. The singularity is only introduced
 * by a choice of <em>representations</em>. Even considering this, it appears that
 * rather than having a parameter with <em>no</em> realistic value, there is an
 * <em>infinite</em> possible number of values that all represent the same physical
 * orbit. Orekit simply does an arbitrary choice, often choosing simply the value 0.
 * In our example case, we would then get a converted orbit with a 0 perigee argument.
 * This choice is valid, just as any other choice (π/2, π, whatever ...) would
 * have been valid, in the sense that it <i>does</i> represent correctly the orbit
 * and when converted back to the original non-ambiguous representation it does give
 * the right result.
 * </p>
 *
 * <p>
 * We therefore consider it is the responsibility of the user to be aware of the correct
 * definition of the different representations and of the singularities relative to each
 * one of them. If the user really needs to do some conversion (for example to provide
 * an orbit as Two-Lines Elements later on, remembering than TLE do use keplerian-like
 * parameters), then he can do so.
 * </p>
 *
 * <p>
 * The way conversion is handled in OREKIT is very simple and allows easy and transparent
 * processing. All four parameters type extend the abstract class {@link org.orekit.orbits.Orbit},
 * and every propagation algorithm, be it analytical or numerical, use this abstract class as a
 * parameter, even if internally it relies on a specific representation. As an example, the
 * {@link org.orekit.propagation.analytical.EcksteinHechlerPropagator Eckstein-Hechler propagator}
 * is defined in terms of {@link org.orekit.orbits.CircularOrbit circular orbit} only. So
 * there is an implicit conversion done at propagator initialization time.
 * </p>
 *
 * @author Luc Maisonobe
 * @author Fabien Maussion
 * @author Véronique Pommier-Maurussane
 *
 */
package org.orekit.orbits;
