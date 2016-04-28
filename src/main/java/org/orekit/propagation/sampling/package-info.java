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
 * This package provides interfaces and classes dealing with step handling during propagation.
 * It is used when a {@link org.orekit.propagation.Propagator Propagator} is run in
 * {@link org.orekit.propagation.Propagator#MASTER_MODE master mode}. In this mode, the
 * (master) propagator integration loop calls (slave) application callback methods at each
 * finalized step. The callback methods must implement the {@link
 * org.orekit.propagation.sampling.OrekitFixedStepHandler} interface for fixed step sampling or
 * the {@link org.orekit.propagation.sampling.OrekitStepHandler} interface for variable step
 * sampling.
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
