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
package org.orekit.forces.gravity.potential;


/** Enumerate for tie systems.
 * <p>
 * Tide-systems are used to identify if the permanent tide is already present in
 * the gravity field or if it should be handled when computing the solid tides
 * force model.
 * </p>
 * @see SphericalHarmonicsProvider
 * @author Luc Maisonobe
 * @since 6.0
 */
public enum TideSystem {

    /** Constant for tide-free gravity fields.
     * <p>
     * Tide-free fields don't include the permanent tide,
     * so it must be taken care of when computing the solid tides effects.
     * </p>
     */
    TIDE_FREE,

    /** Constant for zero-tide gravity fields.
     * <p>
     * Zero-tide systems already include the permanent tide,
     * so the solid tides effects must not add them, to avoid it been counted twice.
     * </p>
     */
    ZERO_TIDE,

    /** Constant for unknown tide system.
     */
    UNKNOWN;

}
