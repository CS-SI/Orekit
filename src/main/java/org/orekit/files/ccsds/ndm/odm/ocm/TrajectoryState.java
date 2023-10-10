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

package org.orekit.files.ccsds.ndm.odm.ocm;

import java.util.List;

import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeStamped;
import org.orekit.utils.CartesianDerivativesFilter;
import org.orekit.utils.TimeStampedPVCoordinates;
import org.orekit.utils.units.Unit;

/** Trajectory state entry.
 * @author Luc Maisonobe
 * @since 11.0
 */
public class TrajectoryState implements TimeStamped {

    /** Type of the elements. */
    private final OrbitElementsType type;

    /** Entry date. */
    private final AbsoluteDate date;

    /** Trajectory elements. */
    private final double[] elements;

    /** Simple constructor.
     * @param type type of the elements
     * @param date entry date
     * @param fields trajectory elements
     * @param first index of first field to consider when parsing
     * @param units units to use for parsing
     */
    public TrajectoryState(final OrbitElementsType type, final AbsoluteDate date,
                           final String[] fields, final int first, final List<Unit> units) {
        this.type     = type;
        this.date     = date;
        this.elements = new double[units.size()];
        for (int i = 0; i < elements.length; ++i) {
            elements[i] = units.get(i).toSI(Double.parseDouble(fields[first + i]));
        }
    }

    /** Simple constructor.
     * @param type type of the elements
     * @param date entry date
     * @param elements trajectory elements in SI units
     * @since 12.0
     */
    public TrajectoryState(final OrbitElementsType type, final AbsoluteDate date,
                           final double[] elements) {
        this.type     = type;
        this.date     = date;
        this.elements = elements.clone();
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getDate() {
        return date;
    }

    /** Get trajectory elements.
     * @return trajectory elements
     */
    public double[] getElements() {
        return elements.clone();
    }

    /** Get the type of the elements.
     * @return type of the elements
     */
    public OrbitElementsType getType() {
        return type;
    }

    /** Get which derivatives of position are available in this state.
     * @return a value indicating if the file contains velocity and/or acceleration
      */
    public CartesianDerivativesFilter getAvailableDerivatives() {
        return type ==  OrbitElementsType.CARTP ?
                        CartesianDerivativesFilter.USE_P :
                        (type == OrbitElementsType.CARTPVA ?
                         CartesianDerivativesFilter.USE_PVA :
                         CartesianDerivativesFilter.USE_PV);
    }

    /** Convert to Cartesian coordinates.
     * @param body central body
     * (may be null if {@link #getType() type} is <em>not</em> {@link OrbitElementsType#GEODETIC})
     * @param mu gravitational parameter in m³/s²
     * @return Cartesian coordinates
     */
    public TimeStampedPVCoordinates toCartesian(final OneAxisEllipsoid body, final double mu) {
        return type.toCartesian(date, elements, body, mu);
    }

}
