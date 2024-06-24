/* Copyright 2002-2024 Luc Maisonobe
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

/** Container for one satellite slot in a {@link WalkerConstellation Wlaker constellation}.
 * <p>
 * The {@link #getSatellite()} satellite index for regular satellites is an integer,
 * but it is allowed to have non-integer indices to create slots for in-orbit spare
 * satellites between the regular satellites. As an example, one can consider a 24/3/1
 * Walker constellation with 8 operational satellites in each of the 3 planes at satellites
 * indices 0, 1, 2, 3, 4, 5, 6 and 7, and put for example 2 additional spares in each plane
 * (hence having a total of 30 satellites), by affecting them to intermediate slots 0.5
 * and 4.5.
 * </p>
 * @param <O> type of the orbit
 * @author Luc Maisonobe
 * @since 12.1
 */
public class WalkerConstellationSlot<O extends Orbit> {

    /** Constellation. */
    private final WalkerConstellation constellation;

    /** Index of the plane. */
    private final int plane;

    /** Satellite index in plane. */
    private final double satellite;

    /** Orbit. */
    private final O orbit;

    /** Simple constructor.
     * @param constellation constellation
     * @param plane plane index
     * @param satellite satellite index in plane
     * @param orbit orbit
     */
    WalkerConstellationSlot(final WalkerConstellation constellation,
                            final int plane, final double satellite, final O orbit) {
        this.constellation = constellation;
        this.plane         = plane;
        this.satellite     = satellite;
        this.orbit         = orbit;
    }

    /** Get the constellation.
     * @return constellation
     */
    public WalkerConstellation getConstellation() {
        return constellation;
    }

    /** Get the plane index.
     * @return plane index
     */
    public int getPlane() {
        return plane;
    }

    /** Get the satellite index in plane.
     * <p>
     * Not that the index may be non-integer, for example to deal with
     * in-orbit spare satellites
     * </p>
     * @return satellite index in plane
     */
    public double getSatellite() {
        return satellite;
    }

    /** Get the orbit.
     * @return orbit
     */
    public O getOrbit() {
        return orbit;
    }

}
