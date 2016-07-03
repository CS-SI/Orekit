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
 * <h1> Propagation </h1>
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
 * <h2> Keplerian propagation </h2>
 *
 * <p> The {@link org.orekit.propagation.analytical.KeplerianPropagator}
 * implements the {@link org.orekit.propagation.Propagator}
 * interface, which ensures that we can obtain a propagated SpacecraftState
 * at any time once the instance is initialized with an initial state.
 * This extrapolation is not a problem with a simple
 * {@link org.orekit.orbits.EquinoctialOrbit}
 * representation: only the mean anomaly value changes.
 *
 * <h2> Eckstein-Hechler propagation </h2>
 *
 * <p> This analytical model is suited for near circular orbits and inclination
 * neither equatorial nor critical. It considers J2 to J6 potential
 * coefficients correctors, and uses mean parameters to compute the new
 * position. As the keplerian propagator, it implements the
 * {@link org.orekit.propagation.Propagator} interface.
 *
 * <h2> Numerical propagation </h2>
 *
 * <p> It is the most important part of the OREKIT project. Based on Hipparchus
 * integrators, the {@link org.orekit.propagation.numerical.NumericalPropagator}
 * class realizes the interface between space mechanics and mathematical
 * resolutions. If its utilization seems difficult on first sight, it is in
 * fact quite clear and intuitive.
 *
 * <p>
 * The mathematical problem to integrate is a seven dimension time derivative
 * equations system. The six first equations are given by the Gauss equations
 * (expressed in {@link org.orekit.orbits.EquinoctialOrbit}) and the seventh
 * is simply the flow rate and mass equation. This first order system is computed
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
 * @author Luc Maisonobe
 * @author Fabien Maussion
 * @author Pascal Parraud
 *
 */
package org.orekit.propagation;
