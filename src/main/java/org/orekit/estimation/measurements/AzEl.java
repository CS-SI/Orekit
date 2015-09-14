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
import org.apache.commons.math3.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Transform;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;

/** Class modeling an Azimuth-Elevation measurement from a ground station.
 * @author Thierry Ceolin
 * @since 7.1
 */
public class AzEl extends AbstractMeasurement {

    /** Ground station from which measurement is performed. */
    private final GroundStation station;

    /** Simple constructor.
     * @param station ground station from which measurement is performed
     * @param date date of the measurement
     * @param azel observed value
     * @param sigma theoretical standard deviation
     * @param baseWeight base weight
     * @exception OrekitException if a {@link org.orekit.estimation.Parameter}
     * name conflict occurs
     */
    public AzEl(final GroundStation station, final AbsoluteDate date,
                final double[] azel, final double[] sigma, final double[] baseWeight)
        throws OrekitException {
        super(date, azel, sigma, baseWeight);
        this.station = station;
        addSupportedParameter(station);
    }

    /** {@inheritDoc} */
    @Override
    protected Evaluation theoreticalEvaluation(final int iteration, final SpacecraftState state)
        throws OrekitException {

        // take propagation time into account
        final SpacecraftState compensatedState =
                station.compensatePropagationDelay(state, getDate());

        // prepare the evaluation
        final Evaluation evaluation    = new Evaluation(this, iteration, compensatedState);
        final double[]   azel          = new double[2];
        final double[][] gradientState = new double[2][];
        final double[][] gradientParam = new double[2][];

        // station position at signal arrival
        final Transform topoToInert =
                station.getOffsetFrame().getTransformTo(compensatedState.getFrame(),
                                                        getDate());
        final Vector3D stationPosition = topoToInert.transformPosition(Vector3D.ZERO);

        // range value in inertial frame
        final Vector3D spacecraftPosition = compensatedState.getPVCoordinates().getPosition();
        final Vector3D delta = spacecraftPosition.subtract(stationPosition);

        // Azimuth and Elevation value
        final Transform InertToTopo =
                        compensatedState.getFrame().getTransformTo(station.getOffsetFrame(), getDate());
        final Vector3D  rangeInTopo = InertToTopo.transformPosition(delta);

        azel[0] = station.getOffsetFrame().getAzimuth(rangeInTopo, station.getOffsetFrame(), getDate());
        azel[1] = station.getOffsetFrame().getElevation(rangeInTopo, station.getOffsetFrame(), getDate());
        evaluation.setValue(azel);

        // partial derivatives of azimuth with respect to state
        final double rhoInXY2 = rangeInTopo.getX() * rangeInTopo.getX() + rangeInTopo.getY() * rangeInTopo.getY();
        final Vector3D gradientAzInTopo = new Vector3D( rangeInTopo.getX() / rhoInXY2,
                                                       -rangeInTopo.getY() / rhoInXY2,
                                                        0.);
        final Vector3D gradientAzInInertial =
                        topoToInert.getRotation().applyTo(gradientAzInTopo);

        gradientState[0] = new double[] {
                         gradientAzInInertial.getX(), gradientAzInInertial.getY(), gradientAzInInertial.getZ(), 0., 0., 0.
            };

        // partial derivatives of Azimuth with respect to parameter
        // the parameter has 3 Cartesian coordinates for station offset position
        gradientParam[0] = new double[] {
            -gradientAzInTopo.getX(),
            -gradientAzInTopo.getY(),
            -gradientAzInTopo.getZ()
            };

        // partial derivatives of elevation with respect to state
        final double rhoInXY = FastMath.sqrt(rhoInXY2);
        final Vector3D gradientElInTopo = new Vector3D( -rangeInTopo.getX() * rangeInTopo.getZ() / rhoInXY2,
                                                        -rangeInTopo.getY() * rangeInTopo.getZ() / rhoInXY2,
                                                         rhoInXY);
        final Vector3D gradientElInInertial =
                        topoToInert.getRotation().applyTo(gradientElInTopo);
        gradientState[1] = new double[] {
                                    gradientElInInertial.getX(), gradientElInInertial.getY(), gradientElInInertial.getZ(), 0., 0., 0.
            };

        // partial derivatives of Elevation with respect to parameter
        // the parameter has 3 Cartesian coordinates for station offset position
        gradientParam[1] = new double[] {
            -rangeInTopo.getX() * rangeInTopo.getZ() / rhoInXY2,
            -rangeInTopo.getY() * rangeInTopo.getZ() / rhoInXY2,
            rhoInXY
            };

        // partial derivatives
        evaluation.setStateDerivatives(gradientState);
        evaluation.setParameterDerivatives(station.getName(), gradientParam);
        return evaluation;
    }

}
