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

import java.util.Arrays;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Transform;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.AngularCoordinates;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.ParameterDriver;

/** Class modeling one-way or two-way range rate measurement between two vehicles.
 * One-way range rate (or Doppler) measurements generally apply to specific satellites
 * (e.g. GNSS, DORIS), where a signal is transmitted from a satellite to a
 * measuring station.
 * Two-way range rate measurements are applicable to any system. The signal is
 * transmitted to the (non-spinning) satellite and returned by a transponder
 * (or reflected back)to the same measuring station.
 * The Doppler measurement can be obtained by multiplying the velocity by (fe/c), where
 * fe is the emission frequency.
 *
 * @author Thierry Ceolin
 * @author Joris Olympio
 * @since 8.0
 */
public class RangeRate extends AbstractMeasurement<RangeRate> {

    /** Ground station from which measurement is performed. */
    private final GroundStation station;

    /** Flag indicating whether it is a two-way measurement. */
    private final boolean twoway;

    /** Simple constructor.
     * @param station ground station from which measurement is performed
     * @param date date of the measurement
     * @param rangeRate observed value, m/s
     * @param sigma theoretical standard deviation
     * @param baseWeight base weight
     * @param twoway if true, this is a two-way measurement
     * @exception OrekitException if a {@link org.orekit.utils.ParameterDriver}
     * name conflict occurs
     */
    public RangeRate(final GroundStation station, final AbsoluteDate date,
                     final double rangeRate,
                     final double sigma,
                     final double baseWeight,
                     final boolean twoway)
        throws OrekitException {
        super(date, rangeRate, sigma, baseWeight,
              station.getEastOffsetDriver(),
              station.getNorthOffsetDriver(),
              station.getZenithOffsetDriver());
        this.station = station;
        this.twoway  = twoway;
    }

    /** Get the ground station from which measurement is performed.
     * @return ground station from which measurement is performed
     */
    public GroundStation getStation() {
        return station;
    }

    /** {@inheritDoc} */
    @Override
    protected Evaluation<RangeRate> theoreticalEvaluation(final int iteration, final int count,
                                                          final SpacecraftState state)
        throws OrekitException {

        // one-way (downlink) light time correction
        // (if state has already been set up to pre-compensate propagation delay,
        //  we will have offset == downlinkDelay and compensatedState will be
        //  the same as state)
        final double          downlinkDelay    = station.downlinkTimeOfFlight(state, getDate());
        final double          offset           = getDate().durationFrom(state.getDate());
        final SpacecraftState compensatedState = state.shiftedBy(offset - downlinkDelay);

        final Evaluation<RangeRate> evaluation = oneWayTheoreticalEvaluation(iteration, count,
                                                                             state.getDate(), compensatedState);
        if (twoway) {
            // one-way (uplink) light time correction
            final double uplinkDelay = station.uplinkTimeOfFlight(compensatedState);
            final AbsoluteDate date = compensatedState.getDate().shiftedBy(uplinkDelay);
            final Evaluation<RangeRate> evalOneWay2 = oneWayTheoreticalEvaluation(iteration, count,
                                                                                  date, compensatedState);

            //evaluation
            evaluation.setValue(0.5 * (evaluation.getValue()[0] + evalOneWay2.getValue()[0]));
            final double[][] sd1 = evaluation.getStateDerivatives();
            final double[][] sd2 = evalOneWay2.getStateDerivatives();
            final double[][] sd = new double[sd1.length][sd1[0].length];
            for (int i = 0; i < sd.length; ++i) {
                for (int j = 0; j < sd[0].length; ++j) {
                    sd[i][j] = 0.5 * (sd1[i][j] + sd2[i][j]);
                }
            }
            evaluation.setStateDerivatives(sd);

            for (final ParameterDriver driver : Arrays.asList(station.getEastOffsetDriver(),
                                                              station.getNorthOffsetDriver(),
                                                              station.getZenithOffsetDriver())) {
                if (driver.isSelected()) {
                    final double[] pd1 = evaluation.getParameterDerivatives(driver);
                    final double[] pd2 = evalOneWay2.getParameterDerivatives(driver);
                    final double[] pd = new double[pd1.length];
                    for (int i = 0; i < pd.length; ++i) {
                        pd[i] = 0.5 * (pd1[i] + pd2[i]);
                    }
                    evaluation.setParameterDerivatives(driver, pd);
                }
            }
        }

        return evaluation;
    }

    /** Evaluate measurement in one-way.
     * @param iteration iteration number
     * @param count evaluations counter
     * @param date date at which signal is on ground station
     * @param compensatedState orbital state used for measurement
     * @return theoretical value
     * @exception OrekitException if value cannot be computed
     * @see #evaluate(SpacecraftStatet)
     */
    private Evaluation<RangeRate> oneWayTheoreticalEvaluation(final int iteration, final int count, final AbsoluteDate date,
                                                              final SpacecraftState compensatedState)
        throws OrekitException {
        // prepare the evaluation
        final Evaluation<RangeRate> evaluation =
                        new Evaluation<RangeRate>(this, iteration, count, compensatedState);

        // station coordinates at date in state frame
        final PVCoordinates pvStation = station.getOffsetFrame().getPVCoordinates(date, compensatedState.getFrame());

        // range rate value
        final Vector3D stationPosition = pvStation.getPosition();
        final Vector3D relativePosition = compensatedState.getPVCoordinates().getPosition().subtract(stationPosition);

        final Vector3D stationVelocity = pvStation.getVelocity();
        final Vector3D relativeVelocity = compensatedState.getPVCoordinates().getVelocity()
                                                .subtract(stationVelocity);
        // line of sight direction
        final Vector3D      lineOfSight      = relativePosition.normalize();
        // range rate
        final double rr = Vector3D.dotProduct(relativeVelocity, lineOfSight);

        evaluation.setValue(rr);

        // compute partial derivatives of (rr) with respect to spacecraft state Cartesian coordinates.
        final double relnorm = relativePosition.getNorm();
        final double den1 = relnorm; //relativePosition.getNorm();
        final double den2 = FastMath.pow(relativePosition.getNorm(), 2);
        final double fRx = 1. / den2 * relativeVelocity.dotProduct(Vector3D.PLUS_I.scalarMultiply(relnorm).subtract( relativePosition.scalarMultiply(relativePosition.getX() / den1)));
        final double fRy = 1. / den2 * relativeVelocity.dotProduct(Vector3D.PLUS_J.scalarMultiply(relnorm).subtract( relativePosition.scalarMultiply(relativePosition.getY() / den1)));
        final double fRz = 1. / den2 * relativeVelocity.dotProduct(Vector3D.PLUS_K.scalarMultiply(relnorm).subtract( relativePosition.scalarMultiply(relativePosition.getZ() / den1)));
        final double fVx = lineOfSight.getX();
        final double fVy = lineOfSight.getY();
        final double fVz = lineOfSight.getZ();
        evaluation.setStateDerivatives(new double[] {
            fRx, fRy, fRz,
            fVx, fVy, fVz
        });

        // compute sensitivity wrt station position when station bias needs
        // to be estimated
        if (station.getEastOffsetDriver().isSelected()  ||
            station.getNorthOffsetDriver().isSelected() ||
            station.getZenithOffsetDriver().isSelected()) {

            // station position at signal arrival
            final Transform topoToInert =
                            station.getOffsetFrame().getTransformTo(compensatedState.getFrame(), getDate());

            final AngularCoordinates ac = topoToInert.getAngular().revert();

            //
            final Transform tt = compensatedState.getFrame().getTransformTo(station.getBaseFrame().getParent(), date);
            final Vector3D omega        = tt.getRotationRate(); // earth angular velocity

            // derivative of lineOfSight wrt rSta,
            //    d (relPos / ||relPos||) / d rStation
            //
            final double relnorm2 = relnorm * relnorm;
            final double[][] m = new double[][] {
                {
                    relativePosition.getX() * relativePosition.getX() / relnorm2 - 1.0,
                    relativePosition.getY() * relativePosition.getX() / relnorm2,
                    relativePosition.getZ() * relativePosition.getX() / relnorm2
                }, {
                    relativePosition.getX() * relativePosition.getY() / relnorm2,
                    relativePosition.getY() * relativePosition.getY() / relnorm2 - 1.0,
                    relativePosition.getZ() * relativePosition.getY() / relnorm2
                }, {
                    relativePosition.getX() * relativePosition.getZ() / relnorm2,
                    relativePosition.getY() * relativePosition.getZ() / relnorm2,
                    relativePosition.getZ() * relativePosition.getZ() / relnorm2 - 1.0
                }
            };

            // derivatives of the dot product (relVelocity * lineOfSight) wrt rStation
            // Note: relVelocity = rstation x omega
            final Vector3D v = relativeVelocity;
            final double dVUdPstax = (-omega.getZ() * lineOfSight.getY() + omega.getY() * lineOfSight.getZ()) +
                                   (v.getX() * m[0][0] + v.getY() * m[1][0] + v.getZ() * m[2][0]) / relnorm;
            final double dVUdPstay = ( omega.getZ() * lineOfSight.getX() - omega.getX() * lineOfSight.getZ()) +
                                   (v.getX() * m[0][1] + v.getY() * m[1][1] + v.getZ() * m[2][1]) / relnorm;
            final double dVUdPstaz = (-omega.getY() * lineOfSight.getX() + omega.getX() * lineOfSight.getY()) +
                                   (v.getX() * m[0][2] + v.getY() * m[1][2] + v.getZ() * m[2][2]) / relnorm;

            // range rate partial derivatives
            // with respect to station position in inertial frame
            final Vector3D dRdQI1 = new Vector3D(dVUdPstax, dVUdPstay, dVUdPstaz);


            // convert to topocentric frame, as the station position
            // offset parameter is expressed in this frame
            final Vector3D dRdQT = ac.getRotation().applyTo(dRdQI1);

            // partial derivatives with respect to parameter
            // the parameter has 3 Cartesian coordinates for station offset position
            // at measure time
            if (station.getEastOffsetDriver().isSelected()) {
                evaluation.setParameterDerivatives(station.getEastOffsetDriver(), dRdQT.getX());
            }
            if (station.getNorthOffsetDriver().isSelected()) {
                evaluation.setParameterDerivatives(station.getNorthOffsetDriver(), dRdQT.getY());
            }
            if (station.getZenithOffsetDriver().isSelected()) {
                evaluation.setParameterDerivatives(station.getZenithOffsetDriver(), dRdQT.getZ());
            }

        }
        return evaluation;
    }

}
