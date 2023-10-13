/* Copyright 2023 Thales Alenia Space
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
package org.orekit.estimation.measurements.gnss;

import org.hipparchus.geometry.euclidean.threed.Vector3D;

/** Dipole configuration for satellite-to-ground and inter-satellites wind-up effects.
 * <p>
 * The dipole configuration is given by two vectors.
 * </p>
 * @see WindUp
 * @see InterSatellitesWindUp
 * @author Luc Maisonobe
 * @since 12.0
 */
public class Dipole {

    /** Canonical dipole, with primary vector set to {@link Vector3D#PLUS_I}
     * and secondary vector set to {@link Vector3D#PLUS_J}.
     */
    public static final Dipole CANONICAL_I_J = new Dipole(Vector3D.PLUS_I, Vector3D.PLUS_J);

    /** Primary dipole vector. */
    private final Vector3D primary;

    /** Secondary dipole vector. */
    private final Vector3D secondary;

    /** Simple constructor.
     * @param primary primary dipole vector
     * @param secondary secondary dipole vector
     */
    Dipole(final Vector3D primary, final Vector3D secondary) {
        this.primary   = primary;
        this.secondary = secondary;
    }

    /** Get the primary dipole vector.
     * @return primary dipole vector
     */
    public Vector3D getPrimary() {
        return primary;
    }

    /** Get the secondary dipole vector.
     * @return secondary dipole vector
     */
    public Vector3D getSecondary() {
        return secondary;
    }

}
