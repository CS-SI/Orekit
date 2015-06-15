/* Copyright 2002-2015 CS Systèmes d'Information
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
package org.orekit.estimation.measurements;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.orekit.errors.OrekitException;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;

/** Class modeling a range measurement from a ground station.
 * @author Thierry Ceolin
 * @since 7.1
 */
public class RangeRate extends AbstractMeasurement {

    /** Ground station from which measurement is performed. */
    private final GroundStation station;

    /** Simple constructor.
     * @param station ground station from which measurement is performed
     * @param date date of the measurement
     * @param rangeRate observed value
     * @param sigma theoretical standard deviation
     * @exception OrekitException if a {@link org.orekit.estimation.Parameter}
     * name conflict occurs
     */
    public RangeRate(final GroundStation station, final AbsoluteDate date,
                     final double rangeRate, final double sigma)
        throws OrekitException {
        super(date, rangeRate, sigma);
        this.station = station;
        addSupportedParameter(station);
    }

    /** {@inheritDoc} */
    @Override
    protected Evaluation theoreticalEvaluation(final SpacecraftState state)
        throws OrekitException {

        // take propagation time into account
        final SpacecraftState compensatedState =
                station.compensatePropagationDelay(state, getDate());

        // prepare the evaluation
        final Evaluation evaluation = new Evaluation(this, compensatedState);

        // range rate value
        final PVCoordinates scInStationFrame = state.getPVCoordinates(station.getOffsetFrame());
        final Vector3D      lineOfSight      = scInStationFrame.getPosition().normalize();
        evaluation.setValue(Vector3D.dotProduct(scInStationFrame.getVelocity(), lineOfSight));

        // TODO compute partial derivatives

        return evaluation;

    }

}
