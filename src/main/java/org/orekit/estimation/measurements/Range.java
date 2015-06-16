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
import org.orekit.frames.Transform;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;

/** Class modeling a range measurement from a ground station.
 * @author Thierry Ceolin
 * @since 7.1
 */
public class Range extends AbstractMeasurement {

    /** Ground station from which measurement is performed. */
    private final GroundStation station;

    /** Simple constructor.
     * @param station ground station from which measurement is performed
     * @param date date of the measurement
     * @param range observed value
     * @param sigma theoretical standard deviation
     * @exception OrekitException if a {@link org.orekit.estimation.Parameter}
     * name conflict occurs
     */
    public Range(final GroundStation station, final AbsoluteDate date,
                 final double range, final double sigma)
        throws OrekitException {
        super(date, range, sigma);
        this.station = station;
        addSupportedParameter(station);
    }

    /** Get the ground station from which measurement is performed.
     * @return ground station from which measurement is performed
     */
    public GroundStation getStation() {
        return station;
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

        // station position at signal arrival
        final Transform topoToInert =
                station.getOffsetFrame().getTransformTo(compensatedState.getFrame(),
                                                        getDate());
        final Vector3D stationPosition = topoToInert.transformPosition(Vector3D.ZERO);

        // range value
        final Vector3D spacecraftPosition = compensatedState.getPVCoordinates().getPosition();
        final Vector3D delta = spacecraftPosition.subtract(stationPosition);
        final double range = delta.getNorm();
        evaluation.setValue(range);

        // partial derivatives with respect to state
        final Vector3D gradientInInertial = new Vector3D(delta.getX() / range,
                                                         delta.getY() / range,
                                                         delta.getZ() / range);
        evaluation.setStateDerivatives(new double[] {
            gradientInInertial.getX(), gradientInInertial.getY(), gradientInInertial.getZ(),
            0, 0, 0
        });

        // partial derivatives with respect to parameter
        // the parameter has 3 Cartesian coordinates for station offset position
        final Vector3D gradientInTopo =
                topoToInert.getRotation().applyInverseTo(gradientInInertial);
        evaluation.setParameterDerivatives(station.getName(),
                                           new double[] {
                                               -gradientInTopo.getX(),
                                               -gradientInTopo.getY(),
                                               -gradientInTopo.getZ()
                                           });

        return evaluation;

    }

}
