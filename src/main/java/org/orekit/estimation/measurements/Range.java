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
import org.orekit.utils.Constants;

/** Class modeling a range measurement from a ground station.
 * <p>
 * The measurement is considered to be a signal emitted from
 * a ground station, reflected on spacecraft, and received
 * on the same ground station. Its value is the elapsed time
 * between emission and reception divided by 2c were c is the
 * speed of light. The motion of both the station and the
 * spacecraft during the signal flight time are taken into
 * account. The date of the measurement corresponds to the
 * reception on ground of the reflected signal.
 * </p>
 * @author Thierry Ceolin
 * @author Luc Maisonobe
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
     * @param baseWeight base weight
     * @exception OrekitException if a {@link org.orekit.estimation.Parameter}
     * name conflict occurs
     */
    public Range(final GroundStation station, final AbsoluteDate date,
                 final double range, final double sigma, final double baseWeight)
        throws OrekitException {
        super(date, range, sigma, baseWeight);
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
    protected Evaluation theoreticalEvaluation(final int iteration, final SpacecraftState state)
        throws OrekitException {

        // station position at signal arrival
        final Transform topoToInertArrival =
                station.getOffsetFrame().getTransformTo(state.getFrame(), getDate());
        final Vector3D stationArrival = topoToInertArrival.transformPosition(Vector3D.ZERO);

        // take propagation time into account
        // (if state has already been set up to pre-compensate propagation delay,
        //  we will have offset == downlinkDelay and transitState will be
        //  the same as state)
        final double          downlinkDelay = station.downlinkDelay(state, getDate());
        final double          offset        = getDate().durationFrom(state.getDate());
        final SpacecraftState transitState  = state.shiftedBy(offset - downlinkDelay);
        final Vector3D        position      = transitState.getPVCoordinates().getPosition();
        final Vector3D        velocity      = transitState.getPVCoordinates().getVelocity();

        // station position at signal departure
        final double          uplinkDelay    = station.uplinkDelay(transitState);
        final Transform topoToInertDeparture =
                station.getOffsetFrame().getTransformTo(state.getFrame(),
                                                        getDate().shiftedBy(-(downlinkDelay + uplinkDelay)));
        final Vector3D stationDeparture = topoToInertDeparture.transformPosition(Vector3D.ZERO);

        // prepare the evaluation
        final Evaluation evaluation = new Evaluation(this, iteration, transitState);

        // range value
        final Vector3D downInert  = position.subtract(stationArrival);
        final double   dDownInert = downInert.getNorm();
        final Vector3D upInert    = position.subtract(stationDeparture);
        final double   dUpInert   = upInert.getNorm();
        evaluation.setValue(0.5 * (dDownInert + dUpInert));

        // partial derivatives with respect to state
        final double radialVelD = Vector3D.dotProduct(downInert, velocity) / dDownInert;
        final double fD         = 0.5 / dDownInert * (1 - radialVelD / Constants.SPEED_OF_LIGHT);
        final double radialVelU = Vector3D.dotProduct(upInert, velocity) / dUpInert;
        final double fU         = 0.5 / dUpInert * (1 - radialVelU / Constants.SPEED_OF_LIGHT);
        evaluation.setStateDerivatives(new double[] {
            fD * downInert.getX() + fU * upInert.getX(),
            fD * downInert.getY() + fU * upInert.getY(),
            fD * downInert.getZ() + fU * upInert.getZ(),
            0,
            0,
            0
        });

        if (station.isEstimated()) {
            // partial derivatives with respect to parameter
            // the parameter has 3 Cartesian coordinates for station offset position
            final Vector3D downTopo = topoToInertArrival.getRotation().applyInverseTo(downInert);
            final Vector3D upTopo   = topoToInertDeparture.getRotation().applyInverseTo(upInert);
            evaluation.setParameterDerivatives(station.getName(),
                                               new double[] {
                                                   -fD * downTopo.getX() - fU * upTopo.getX(),
                                                   -fD * downTopo.getY() - fU * upTopo.getY(),
                                                   -fD * downTopo.getZ() - fU * upTopo.getZ(),
                                               });
        }

        return evaluation;

    }

}
