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
 * This package provides routines to model the adjoint dynamics as in the Pontryagin Maximum Principle, as used
 * in indirect control. There is one adjoint variable for each dependent variable in the equations of motion (a.k.a. the state variables),
 * so in orbital mechanics that is typically the position-velocity vector (or its equivalent e.g. orbital elements) and mass.
 * The adjoint vector is the solution to a differential system coupled with the state vector.
 *
 * @author Romain Serra
 * @since 12.2
 *
 */
package org.orekit.control.indirect.adjoint;
