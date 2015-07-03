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
import org.orekit.frames.Frame;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;

/** Class modeling one-way or two-way range rate measurement between two vehicles.
 * One-way range rate (or Doppler) measurements generally apply to specific satellites
 * (e.g. GNSS, DORIS), where a signal is transmitted from a satellite to a
 * measuring station.
 * Two-way range rate measurements are applicable to any system. The signal is
 * transmitted to the (non-spinning) satellite and returned by a transponder
 * (or reflected back)to the same measuring station.
 *
 * @author Thierry Ceolin
 * @author Joris Olympio
 * @since 7.1
 */
public class RangeRate extends AbstractMeasurement {

    /** Ground station from which measurement is performed. */
    private final GroundStation station;

    /** Flag indicating a ionospheric correction is performed. */
    private final boolean withIonoCorrection;

    /** Flag indicating a tropospheric correction is performed. */
    private final boolean withTropoCorrection;

    /** Flag indicating whether it is a two-way measurement. */
    private final boolean twoway;

    /** Simple constructor.
     * @param station ground station from which measurement is performed
     * @param date date of the measurement
     * @param rangeRate observed value, m/s
     * @param sigma theoretical standard deviation
     * @param baseWeight base weight
     * @param twoway if true, this is a two-way measurement
     * @param withIonoCorrection use ionospheric correction
     * @param withTropoCorrection use tropospheric correction
     * @exception OrekitException if a {@link org.orekit.estimation.Parameter}
     * name conflict occurs
     */
    public RangeRate(final GroundStation station, final AbsoluteDate date,
                     final double rangeRate, final double sigma, final double baseWeight,
                     final boolean twoway,
                     final boolean withIonoCorrection, final boolean withTropoCorrection)
        throws OrekitException {
        super(date, rangeRate, sigma, baseWeight);
        this.station = station;
        this.withIonoCorrection = withIonoCorrection;
        this.withTropoCorrection = withTropoCorrection;
        this.twoway = twoway;
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

        // one-way (downlink) light time correction
        // (if state has already been set up to pre-compensate propagation delay,
        //  we will have offset == downlinkDelay and compensatedState will be
        //  the same as state)
        final double          downlinkDelay    = station.downlinkTimeOfFlight(state, getDate());
        final double          offset           = getDate().durationFrom(state.getDate());
        final SpacecraftState compensatedState = state.shiftedBy(offset - downlinkDelay);

        final Evaluation evaluation = oneWayTheoreticalEvaluation(iteration, state.getDate(), compensatedState);
        if (twoway) {
            // one-way (uplink) light time correction
            final double uplinkDelay = downlinkDelay;
            final AbsoluteDate date = compensatedState.getDate().shiftedBy(offset - uplinkDelay);
            final Evaluation evalOneWay2 = oneWayTheoreticalEvaluation(iteration, date, compensatedState);

            //evaluation
            evaluation.setValue(0.5 * (evaluation.getValue()[0] + evalOneWay2.getValue()[0]));
            final double[][] sd1 = evaluation.getStateDerivatives();
            final double[][] sd2 = evalOneWay2.getStateDerivatives();
            final double[][] sd = sd1.clone();
            for (int i = 0; i < sd.length; ++i) {
                for (int j = 0; j < sd[0].length; ++j) {
                    sd[i][j] += 0.5 * (sd1[i][j] + sd2[i][j]);
                }
            }
            evaluation.setStateDerivatives(sd);

            if (station.isEstimated()) {
                final double[][] pd1 = evaluation.getParameterDerivatives(station.getName());
                final double[][] pd2 = evalOneWay2.getParameterDerivatives(station.getName());
                final double[][] pd = pd1.clone();
                for (int i = 0; i < pd.length; ++i) {
                    for (int j = 0; j < pd[0].length; ++j) {
                        sd[i][j] += 0.5 * (pd1[i][j] + pd2[i][j]);
                    }
                }
                evaluation.setParameterDerivatives(station.getName(), pd);
            }
        }

        return evaluation;
    }

    /** Evaluate measurement in one-way.
     * @param iteration iteration number
     * @param date date at which signal is on ground station
     * @param compensatedState orbital state used for measurement
     * @return theoretical value
     * @exception OrekitException if value cannot be computed
     * @see #evaluate(SpacecraftStatet)
     */
    private Evaluation oneWayTheoreticalEvaluation(final int iteration, final AbsoluteDate date,
                                                   final SpacecraftState compensatedState)
        throws OrekitException {
        // prepare the evaluation
        final Evaluation evaluation = new Evaluation(this, iteration, compensatedState);

        // range rate value
        final Frame frame = compensatedState.getFrame(); // inertial frame
        final Vector3D stationPosition = station.getBaseFrame().getPVCoordinates(date, frame).getPosition();
        final Vector3D relativePosition = compensatedState.getPVCoordinates().getPosition().subtract(stationPosition);

        final Vector3D stationVelocity = station.getBaseFrame().getPVCoordinates(date, frame).getVelocity();
        final Vector3D relativeVelocity = compensatedState.getPVCoordinates().getVelocity()
                                        .subtract(stationVelocity);
        final Vector3D      lineOfSight      = relativePosition.normalize();
        //
        final double rr = Vector3D.dotProduct(relativeVelocity, lineOfSight);

        // FIXME There are modifier apparently to handle those corrections.
        // However, which state do they use? compensatedState?
        evaluation.setValue(rr);

        // compute partial derivatives with respect to spacecraft state Cartesian coordinates.
        final double norm = relativePosition.getNorm();
        final double den1 = norm; //relativePosition.getNorm();
        final double den2 = FastMath.pow(relativePosition.getNorm(), 2);
        final double fRx = 1. / den2 * relativeVelocity.dotProduct(Vector3D.PLUS_I.scalarMultiply(norm).subtract( relativePosition.scalarMultiply(relativePosition.getX() / den1)));
        final double fRy = 1. / den2 * relativeVelocity.dotProduct(Vector3D.PLUS_J.scalarMultiply(norm).subtract( relativePosition.scalarMultiply(relativePosition.getY() / den1)));
        final double fRz = 1. / den2 * relativeVelocity.dotProduct(Vector3D.PLUS_K.scalarMultiply(norm).subtract( relativePosition.scalarMultiply(relativePosition.getZ() / den1)));
        final double fVx = lineOfSight.getX();
        final double fVy = lineOfSight.getY();
        final double fVz = lineOfSight.getZ();
        evaluation.setStateDerivatives(new double[] {
            fRx,
            fRy,
            fRz,
            fVx,
            fVy,
            fVz
        });

        if (station.isEstimated()) {
            // partial derivatives with respect to parameter
            // the parameter has 3 Cartesian coordinates for station offset position
            evaluation.setParameterDerivatives(station.getName(),
                    new double[] {
                        -fRx,
                        -fRy,
                        -fRz,
                    });
        }
        return evaluation;
    }

}
