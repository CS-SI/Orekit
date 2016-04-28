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
package org.orekit.estimation.measurements;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Transform;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.AngularCoordinates;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;

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
 * @since 8.0
 */
public class Range extends AbstractMeasurement<Range> {

    /** Ground station from which measurement is performed. */
    private final GroundStation station;

    /** Simple constructor.
     * @param station ground station from which measurement is performed
     * @param date date of the measurement
     * @param range observed value
     * @param sigma theoretical standard deviation
     * @param baseWeight base weight
     * @exception OrekitException if a {@link org.orekit.utils.ParameterDriver}
     * name conflict occurs
     */
    public Range(final GroundStation station, final AbsoluteDate date,
                 final double range, final double sigma, final double baseWeight)
        throws OrekitException {
        super(date, range, sigma, baseWeight, station.getPositionOffsetDriver());
        this.station = station;
    }

    /** Get the ground station from which measurement is performed.
     * @return ground station from which measurement is performed
     */
    public GroundStation getStation() {
        return station;
    }

    /** {@inheritDoc} */
    @Override
    protected Evaluation<Range> theoreticalEvaluation(final int iteration, final int count,
                                                      final SpacecraftState state)
        throws OrekitException {

        // station position at signal arrival
        final Transform topoToInert =
                station.getOffsetFrame().getTransformTo(state.getFrame(), getDate());
        final PVCoordinates stationArrival = topoToInert.transformPVCoordinates(PVCoordinates.ZERO);

        // take propagation time into account
        // (if state has already been set up to pre-compensate propagation delay,
        //  we will have offset == downlinkDelay and transitState will be
        //  the same as state)
        final double          tauD         = station.downlinkTimeOfFlight(state, getDate());
        final double          delta        = getDate().durationFrom(state.getDate());
        final SpacecraftState transitState = state.shiftedBy(delta - tauD);
        final Vector3D        transit      = transitState.getPVCoordinates().getPosition();

        // station position at signal departure
        final double          tauU             = station.uplinkTimeOfFlight(transitState);
        final double          tau              = tauD + tauU;
        final PVCoordinates   stationDeparture =
                        topoToInert.shiftedBy(-tau).transformPVCoordinates(PVCoordinates.ZERO);

        // prepare the evaluation
        final Evaluation<Range> evaluation = new Evaluation<Range>(this, iteration, count, transitState);

        // range value
        final double cOver2 = 0.5 * Constants.SPEED_OF_LIGHT;
        evaluation.setValue(tau * cOver2);

        // partial derivatives with respect to state
        // The formulas below take into account the fact the measurement is at fixed reception date.
        // When spacecraft position is changed, the downlink delay is changed, and in order
        // to still have the measurement arrive at exactly the same date on ground, we must
        // take the spacecraft-station relative velocity into account.
        final Vector3D v         = state.getPVCoordinates().getVelocity();
        final Vector3D qv        = stationArrival.getVelocity();
        final Vector3D downInert = stationArrival.getPosition().subtract(transit);
        final double   dDown     = Constants.SPEED_OF_LIGHT * Constants.SPEED_OF_LIGHT * tauD -
                                   Vector3D.dotProduct(downInert, v);
        final Vector3D upInert   = transit.subtract(stationDeparture.getPosition());
        final double   dUp       = Constants.SPEED_OF_LIGHT * Constants.SPEED_OF_LIGHT * tauU -
                                   Vector3D.dotProduct(upInert, stationDeparture.getVelocity());

        // derivatives of the downlink time of flight
        final double dTauDdPx   = -downInert.getX() / dDown;
        final double dTauDdPy   = -downInert.getY() / dDown;
        final double dTauDdPz   = -downInert.getZ() / dDown;

        // derivatives of the on-board transit position
        final double[][] m = new double[][] {
            {
                downInert.getX() * v.getX() / dDown + 1.0,
                downInert.getY() * v.getX() / dDown,
                downInert.getZ() * v.getX() / dDown
            }, {
                downInert.getX() * v.getY() / dDown,
                downInert.getY() * v.getY() / dDown + 1.0,
                downInert.getZ() * v.getY() / dDown
            }, {
                downInert.getX() * v.getZ() / dDown,
                downInert.getY() * v.getZ() / dDown,
                downInert.getZ() * v.getZ() / dDown + 1.0
            }
        };

        // derivatives of the uplink time of flight
        final double dTauUdPsx = (upInert.getX() * (m[0][0] + qv.getX() * dTauDdPx) +
                                  upInert.getY() * (m[1][0] + qv.getY() * dTauDdPx) +
                                  upInert.getZ() * (m[2][0] + qv.getZ() * dTauDdPx)) / dUp;
        final double dTauUdPsy = (upInert.getX() * (m[0][1] + qv.getX() * dTauDdPy) +
                                  upInert.getY() * (m[1][1] + qv.getY() * dTauDdPy) +
                                  upInert.getZ() * (m[2][1] + qv.getZ() * dTauDdPy)) / dUp;
        final double dTauUdPsz = (upInert.getX() * (m[0][2] + qv.getX() * dTauDdPz) +
                                  upInert.getY() * (m[1][2] + qv.getY() * dTauDdPz) +
                                  upInert.getZ() * (m[2][2] + qv.getZ() * dTauDdPz)) / dUp;

        // derivatives of the range measurement
        final double dRdPx = (dTauDdPx + dTauUdPsx) * cOver2;
        final double dRdPy = (dTauDdPy + dTauUdPsy) * cOver2;
        final double dRdPz = (dTauDdPz + dTauUdPsz) * cOver2;
        final double dt     = delta - tauD;
        evaluation.setStateDerivatives(new double[] {
            dRdPx,      dRdPy,      dRdPz,
            dRdPx * dt, dRdPy * dt, dRdPz * dt
        });

        if (station.getPositionOffsetDriver().isEstimated()) {

            // donwlink partial derivatives
            // with respect to station position in inertial frame
            final double   dTauDdQIx = downInert.getX() / dDown;
            final double   dTauDdQIy = downInert.getY() / dDown;
            final double   dTauDdQIz = downInert.getZ() / dDown;

            // uplink partial derivatives
            // with respect to station position in inertial frame
            final AngularCoordinates ac = topoToInert.getAngular().revert();
            final Vector3D omega        = ac.getRotationRate();
            final double   dTauUdQIx    = (upInert.getX() * (-m[0][0]) +
                                           upInert.getY() * (-m[1][0] + tau * omega.getZ()) +
                                           upInert.getZ() * (-m[2][0] - tau * omega.getY())) / dUp;
            final double   dTauUdQIy    = (upInert.getX() * (-m[0][1] - tau * omega.getZ()) +
                                           upInert.getY() * (-m[1][1]) +
                                           upInert.getZ() * (-m[2][1] + tau * omega.getX())) / dUp;
            final double   dTauUdQIz    = (upInert.getX() * (-m[0][2] + tau * omega.getY()) +
                                           upInert.getY() * (-m[1][2] - tau * omega.getX()) +
                                           upInert.getZ() * (-m[2][2])) / dUp;

            // range partial derivatives
            // with respect to station position in inertial frame
            final Vector3D dRdQI = new Vector3D((dTauDdQIx + dTauUdQIx) * cOver2,
                                                (dTauDdQIy + dTauUdQIy) * cOver2,
                                                (dTauDdQIz + dTauUdQIz) * cOver2);

            // convert to topocentric frame, as the station position
            // offset parameter is expressed in this frame
            final Vector3D dRdQT = ac.getRotation().applyTo(dRdQI);

            evaluation.setParameterDerivatives(station.getPositionOffsetDriver(), dRdQT.toArray());

        }

        return evaluation;

    }

}
