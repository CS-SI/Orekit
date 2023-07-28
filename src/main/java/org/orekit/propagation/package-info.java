/* Copyright 2002-2023 CS GROUP
 * Licensed to CS GROUP (CS) under one or more
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
 * <h2> Propagation </h2>
 *
 * This package provides tools to propagate orbital states with different methods.
 *
 * <p>
 * Propagation is the prediction of the evolution of an initial state.
 * The initial state and the propagated states are represented in OREKIT by a
 * {@link org.orekit.propagation.SpacecraftState}, which is a simple container
 * for all needed information at a specific date : mass,
 * {@link org.orekit.utils.PVCoordinates kinematics},
 * {@link org.orekit.attitudes.Attitude attitude},
 * {@link org.orekit.time.AbsoluteDate date},
 * {@link org.orekit.frames.Frame frame}. The state provides basic interpolation
 * features allowing to shift it slightly to close dates. For more accurate and
 * farthest dates, several full-featured propagators are available to propagate
 * the state.
 * </p>
 *
 * <h3> Keplerian propagation </h3>
 *
 * <p> The {@link org.orekit.propagation.analytical.KeplerianPropagator}
 * implements the {@link org.orekit.propagation.Propagator}
 * interface, which ensures that we can obtain a propagated SpacecraftState
 * at any time once the instance is initialized with an initial state.
 * This extrapolation is not a problem with a simple
 * {@link org.orekit.orbits.EquinoctialOrbit}
 * representation: only the mean anomaly value changes.
 *
 * <h3> Eckstein-Hechler propagation </h3>
 *
 * <p> This analytical model is suited for near circular orbits and inclination
 * neither equatorial nor critical. It considers J2 to J6 potential
 * coefficients correctors, and uses mean parameters to compute the new
 * position. As the Keplerian propagator, it implements the
 * {@link org.orekit.propagation.Propagator} interface.
 *
 * <h3> TLE propagation </h3>
 *
 * <p> This analytical model allows propagating {org.orekit.propagation.analytical.tle.TLE}
 * data using SGP4 or SDP4 models. It is very easy to initialize, only the initial
 * TLE is needed. As the other analytical propagators, it implements the
 * {@link org.orekit.propagation.Propagator} interface.
 *
 * <h3> GNSS propagation </h3>
 *
 * <p> These analytical models allow propagating navigation messages such as
 * in GNSS almanacs available thanks to {@link org.orekit.gnss.SEMParser SEM}
 * or {@link org.orekit.gnss.YUMAParser YUMA} files. Each GNSS constellation
 * has its own propagation model availables in {@link org.orekit.propagation.analytical.gnss}
 * package.
 *
 * <h3> Numerical propagation </h3>
 *
 * <p> It is the most important part of the OREKIT project. Based on Hipparchus
 * integrators, the {@link org.orekit.propagation.numerical.NumericalPropagator}
 * class realizes the interface between space mechanics and mathematical
 * resolutions. If its utilization seems difficult on first sight, it is in
 * fact quite clear and intuitive.
 *
 * <p>
 * The mathematical problem to integrate is a 6 dimension time derivative
 * equations system. The six first equations are given by the Gauss equations
 * (expressed in {@link org.orekit.orbits.EquinoctialOrbit}).
 * This first order system is computed
 * by the {@link org.orekit.propagation.numerical.TimeDerivativesEquations}
 * class. It will be instanced by the propagator and then be modified at each
 * step (a fixed t value) by all the needed {@link
 * org.orekit.forces.ForceModel force models} which will add their contribution,
 * the perturbing acceleration.
 * </p>
 * <p>
 * The {@link org.hipparchus.ode.ODEIntegrator integrators}
 * provided by Hipparchus need the state vector at t0, the state vector first
 * time derivate at t0, and then calculates the next step state vector, and ask
 * for the next first time derivative, etc. until it reaches the final asked date.
 * </p>
 *
 * <h3> Semi-analytical propagation </h3>
 *
 * <p> Semi-analytical propagation in Orekit is based on Draper Semi-analytical
 * Satellite Theory (DSST), which is applicable to all orbit types. DSST divides
 * the computation of the osculating orbital elements into two contributions: the
 * mean orbital elements and the short-periodic terms. Both models are developed
 * in the equinoctial orbital elements via the Method of Averaging. Mean orbital
 * elements are computed numerically while short period motion is computed using
 * a combination of analytical and numerical techniques.
 *
 * @author Luc Maisonobe
 * @author Fabien Maussion
 * @author Pascal Parraud
 *
 */
package org.orekit.propagation;
