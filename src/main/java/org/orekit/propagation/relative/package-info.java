/* Copyright 2002-2026 CS GROUP
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
 * Top level package for relative motion propagation.
 * <p>
 *    This package provides relative providers that allow analytical propagation of a chaser S/C relative to a target
 *    S/C.
 *    Relative providers are implemented as {@link org.orekit.propagation.AdditionalDataProvider} and provide the chaser
 *    S/C state as an additional data to the main propagator, which is the target S/C.
 *    The chaser S/C state is provided in the Local Orbital Frame of the target.
 * </p>
 * <p>
 *    Two implementations of the relative provider interface are available:
 *    <ol>
 *        <li>{@link org.orekit.propagation.relative.clohessywiltshire}: Hill-Clohessy-Wiltshire equations for target
 *        circular orbits, based on chapter 7 of book [1].</li>
 *        <li>{@link org.orekit.propagation.relative.yamanakaankersen}; Yamanaka-Ankersen equations for elliptical
 *        orbits, based on [2]</li>
 *    </ol>
 * </p>
 * <p>
 *    Package {@link org.orekit.propagation.relative.maneuver} provides relative maneuvers applied to the chase S/C.
 *    These are impulse maneuvers implemented as EventDetector added to the main propagator (i.e. the target S/C).
 *    They can be used for Relative Proximity Operations modeling (see package {@link org.orekit.control.relative})
 * </p>
 * <p>
 *    References:
 *    <ul>
 *       <li>[1]. Orbital Mechanics For Engineering Students: Howard. D. Curtis, Elsevier Aerospace Engineering Series,
 *       2010.</li>
 *       <li>[2]. New State Transition Matrix For Relative Motion On An Arbitrary Elliptical Orbit: Koji Yamanaka,
 *       Finn Ankersen, Journal of Guidance, Control, and Dynamics, Vol. 25, No. 1, January-February 2002.</li>
 *    </ul>
 *
 * </p>
 */
package org.orekit.propagation.relative;
