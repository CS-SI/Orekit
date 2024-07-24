/* Copyright 2022-2024 Romain Serra
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
 * This package provides routines to perform so-called indirect optimal control within the frame of orbital mechanics.
 * <br>
 * Indirect means that optimality conditions are obtained first, before attempting to solve them, usually numerically by way of some discretization.
 * A common theorem to derive such conditions for optimality is the Pontryagin's Maximum Principle and its variants. It introduces so-called adjoint variables which are closely linked to the optimal solution.
 * This is in contrast with direct methods, which consist in performing discretization first, before resorting to finite-dimension, local optimization techniques.
 * <br>
 * <br>
 * Some references:
 * <ul>
 * <br>
 * - CERF, Max. Optimization Techniques II: Discrete and Functional Optimization. In : Optimization Techniques II. EDP Sciences, 2023.
 * <br>
 * - TRÉLAT, Emmanuel. Optimal control and applications to aerospace: some results and challenges. Journal of Optimization Theory and Applications, 2012, vol. 154, p. 713-758.
 * <br>
 * - COLASURDO, Guido and CASALINO, Lorenzo. Indirect methods for the optimization of spacecraft trajectories. Modeling and Optimization in Space Engineering, 2013, p. 141-158.
 * <br>
 * - MAREC, Jean-Pierre. Optimal space trajectories. Elsevier, 2012.
 * </ul>
 *
 * @author Romain Serra
 * @since 12.2
 *
 */
package org.orekit.control.indirect;
