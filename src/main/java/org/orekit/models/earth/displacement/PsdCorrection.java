/* Copyright 2022-2025 Thales Alenia Space
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
package org.orekit.models.earth.displacement;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.time.AbsoluteDate;

/** Model for post-seismic deformation corrections.
 * @since 12.1
 * @author Luc Maisonobe
 */
public class PsdCorrection {

    /** Correction axis. */
    private final Axis axis;

    /** Time evolution. */
    private final TimeEvolution evolution;

    /** Earthquake date. */
    private final AbsoluteDate earthquakeDate;

    /** Amplitude. */
    private final double amplitude;

    /** Relaxation time. */
    private final double relaxationTime;

    /** Simple constructor.
     * @param axis correction axis
     * @param evolution time evolution
     * @param earthquakeDate earthquake date
     * @param amplitude amplitude
     * @param relaxationTime relaxation time
     */
    public PsdCorrection(final Axis axis,
                         final TimeEvolution evolution,
                         final AbsoluteDate earthquakeDate,
                         final double amplitude,
                         final double relaxationTime) {
        this.axis           = axis;
        this.evolution      = evolution;
        this.earthquakeDate = earthquakeDate;
        this.amplitude      = amplitude;
        this.relaxationTime = relaxationTime;
    }

    /** Get correction axis.
     * @return correction axis
     */
    public Axis getAxis() {
        return axis;
    }

    /** Get time evolution.
     * @return time evolution
     */
    public TimeEvolution getEvolution() {
        return evolution;
    }

    /** Get earthquake date.
     * @return earthquake date
     */
    public AbsoluteDate getEarthquakeDate() {
        return earthquakeDate;
    }

    /** Get amplitude.
     * @return amplitude
     */
    public double getAmplitude() {
        return amplitude;
    }

    /** Get relaxation time.
     * @return relaxation time
     */
    public double getRelaxationTime() {
        return relaxationTime;
    }

    /** Compute displacement.
     * @param date date
     * @param base base point
     * @return displacement vector in Earth frame
     */
    public Vector3D displacement(final AbsoluteDate date, final GeodeticPoint base) {
        final double scaledTime = date.durationFrom(earthquakeDate) / relaxationTime;
        final double timeFactor = evolution.timeFactor(scaledTime);
        return new Vector3D(amplitude * timeFactor, axis.vector(base));
    }

    /** Enumerate for correction axis. */
    public enum Axis {
        /** East axis. */
        EAST {
            public Vector3D vector(final GeodeticPoint base) {
                return base.getEast();
            }
        },

        /** North axis. */
        NORTH {
            public Vector3D vector(final GeodeticPoint base) {
                return base.getNorth();
            }
        },

        /** Up axis. */
        UP {
            public Vector3D vector(final GeodeticPoint base) {
                return base.getZenith();
            }
        };

        /** Get axis unit vector.
         * @param base base point
         * @return direction in Earth frame
         */
        public abstract Vector3D vector(GeodeticPoint base);
    }

    /** Enumerate for correction time evolution. */
    public enum TimeEvolution {

        /** Exponential evolution. */
        EXP {
            public double timeFactor(final double scaledTime) {
                return 1 - FastMath.exp(-scaledTime);
            }
        },

        /** Logarithmic evolution. */
        LOG {
            public double timeFactor(final double scaledTime) {
                return FastMath.log(1 + scaledTime);
            }
        };

        /** Evaluate correction time factor.
         * @param scaledTime scaled time since earthquake
         * @return correction time factor
         */
        public abstract double timeFactor(double scaledTime);

    }

}
