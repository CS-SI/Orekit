/* Copyright 2022-2025 Romain Serra
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
package org.orekit.orbits;

/** This interface represent orbit-like trajectory whose definition is based on a so-called position angle.
 *
 * @see    PositionAngleType
 * @see    KeplerianOrbit
 * @see    CircularOrbit
 * @see    EquinoctialOrbit
 * @see    FieldKeplerianOrbit
 * @see    FieldCircularOrbit
 * @see    FieldEquinoctialOrbit
 * @author Romain Serra
 * @since 12.0
 */
public interface PositionAngleBased<T> {

    /** Get the cached {@link PositionAngleType}.
     * @return cached type of position angle
     */
    PositionAngleType getCachedPositionAngleType();

    /** Tells whether the instance holds rates (first-order time derivatives) for dependent variables that are incompatible with Keplerian motion.
     * @return true if and only if holding non-Keplerian rates
     * @since 13.0
     */
    boolean hasNonKeplerianRates();

    /** Creates a new instance such that {@link #hasNonKeplerianRates()} is false.
     * @return new object without rates
     * @since 13.0
     */
    T withKeplerianRates();

    /**
     * Creates a new instance with the provided type used for caching.
     * @param positionAngleType position angle type to use for caching value
     * @return new object
     * @since 13.0
     */
    T withCachedPositionAngleType(PositionAngleType positionAngleType);
}
