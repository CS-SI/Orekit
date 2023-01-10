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
 *
 * This package provides interfaces and classes dealing with step handling during propagation.
 * <p>
 * It is used when a {@link org.orekit.propagation.Propagator Propagator} is run. The propagator
 * takes care of the time loop and application callback methods are called at each finalized step.
 * The callback methods must implement the {@link org.orekit.propagation.sampling.OrekitFixedStepHandler}
 * interface for fixed step sampling or the {@link org.orekit.propagation.sampling.OrekitStepHandler}
 * interface for variable step sampling.
 * </p>
 *
 * <p>
 * Both regular propagators using double numbers for state components and field based propagators
 * using any kind of {@link org.hipparchus.CalculusFieldElement field} are available. A typical
 * example is to use {@link org.hipparchus.analysis.differentiation.DerivativeStructure derivative
 * structure} objects to propagate orbits using Taylor Algebra, for either high order uncertainties
 * propagation or very fast Monte-Carlo simulations.
 * </p>
 *
 * <p>
 * The {@link org.orekit.propagation.PropagatorsParallelizer PropagatorsParallelizer} class can
 * be used to propagate orbits for several spacecrafts simultaneously, which can be useful for
 * formation flying or orbit determination for complete navigation constellations.
 * </p>
 *
 * <p>
 * The low level interfaces and classes (<code>OrekitXxx</code>)
 * are heavily based on classes with similar names in the ode package from the <a
 * href="https://hipparchus.org/">Hipparchus</a> library. The changes are mainly
 * adaptations of the signatures to space dynamics:
 * </p>
 * <ul>
 *  <li>the type of dependent variable t has been changed from <code>double</code>
 *      to {@link org.orekit.time.AbsoluteDate AbsoluteDate}</li>
 *  <li>the type of state vector y has been changed from <code>double[]</code>
 *      to {@link org.orekit.propagation.SpacecraftState SpacecraftState}</li>
 * </ul>
 *
 * @author Luc Maisonobe
 *
 */
package org.orekit.propagation.sampling;
