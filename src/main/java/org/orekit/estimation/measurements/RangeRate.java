/* Copyright 2002-2017 CS Systèmes d'Information
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

import org.hipparchus.Field;
import org.hipparchus.analysis.differentiation.DSFactory;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.errors.OrekitException;
import org.orekit.frames.FieldTransform;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TimeStampedFieldPVCoordinates;

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

    /** Factory for the DerivativeStructure instances. */
    private final DSFactory factory;

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
        this.factory = new DSFactory(9, 1);
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
    protected EstimatedMeasurement<RangeRate> theoreticalEvaluation(final int iteration, final int evaluation,
                                                                    final SpacecraftState state)
        throws OrekitException {

        final Field<DerivativeStructure> field = factory.getDerivativeField();
        final FieldVector3D<DerivativeStructure> zero = FieldVector3D.getZero(field);

        // Range-rate derivatives are computed with respect to spacecraft state in inertial frame
        // and station position in station's offset frame
        // -------
        //
        // Parameters:
        //  - 0..2 - Px, Py, Pz   : Position of the spacecraft in inertial frame
        //  - 3..5 - Vx, Vy, Vz   : Velocity of the spacecraft in inertial frame
        //  - 6..8 - QTx, QTy, QTz: Position of the station in station's offset frame

        // Position of the spacecraft expressed as a derivative structure
        // The components of the position are the 3 first derivative parameters
        final Vector3D stateP = state.getPVCoordinates().getPosition();
        final FieldVector3D<DerivativeStructure> pDS =
                        new FieldVector3D<>(factory.variable(0, stateP.getX()),
                                            factory.variable(1, stateP.getY()),
                                            factory.variable(2, stateP.getZ()));

        // Velocity of the spacecraft expressed as a derivative structure
        // The components of the velocity are the 3 second derivative parameters
        final Vector3D stateV = state.getPVCoordinates().getVelocity();
        final FieldVector3D<DerivativeStructure> vDS =
                        new FieldVector3D<>(factory.variable(3, stateV.getX()),
                                            factory.variable(4, stateV.getY()),
                                            factory.variable(5, stateV.getZ()));

        // Acceleration of the spacecraft
        // The components of the acceleration are not derivative parameters
        final Vector3D stateA = state.getPVCoordinates().getAcceleration();
        final FieldVector3D<DerivativeStructure> aDS =
                        new FieldVector3D<>(factory.constant(stateA.getX()),
                                            factory.constant(stateA.getY()),
                                            factory.constant(stateA.getZ()));

        final TimeStampedFieldPVCoordinates<DerivativeStructure> pvaDS =
                        new TimeStampedFieldPVCoordinates<>(state.getDate(), pDS, vDS, aDS);

        // transform between station and inertial frame, expressed as a derivative structure
        // The components of station's position in offset frame are the 3 last derivative parameters
        final AbsoluteDate downlinkDate = getDate();
        final FieldAbsoluteDate<DerivativeStructure> downlinkDateDS =
                        new FieldAbsoluteDate<>(field, downlinkDate);
        final FieldTransform<DerivativeStructure> offsetToInertialDownlink =
                        station.getOffsetToInertial(state.getFrame(), downlinkDateDS, factory, 6, 7, 8);

        // Station position in inertial frame at end of the downlink leg
        final TimeStampedFieldPVCoordinates<DerivativeStructure> stationDownlink =
                        offsetToInertialDownlink.transformPVCoordinates(new TimeStampedFieldPVCoordinates<>(downlinkDateDS,
                                                                                                            zero, zero, zero));

        // Compute propagation times
        // (if state has already been set up to pre-compensate propagation delay,
        //  we will have delta == tauD and transitState will be the same as state)

        // Downlink delay
        final DerivativeStructure tauD = station.signalTimeOfFlight(pvaDS, stationDownlink.getPosition(), downlinkDateDS);

        // Transit state
        final double                delta        = downlinkDate.durationFrom(state.getDate());
        final DerivativeStructure   deltaMTauD   = tauD.negate().add(delta);
        final SpacecraftState       transitState = state.shiftedBy(deltaMTauD.getValue());

        // Transit state (re)computed with derivative structures
        final TimeStampedFieldPVCoordinates<DerivativeStructure> transitPV = pvaDS.shiftedBy(deltaMTauD);

        // one-way (downlink) range-rate
        final EstimatedMeasurement<RangeRate> estimated =
                        oneWayTheoreticalEvaluation(iteration, evaluation, stationDownlink, transitPV, transitState);
        if (twoway) {
            // one-way (uplink) light time correction
            final AbsoluteDate approxUplinkDate = downlinkDate.shiftedBy(-2 * tauD.getValue());
            final FieldAbsoluteDate<DerivativeStructure> approxUplinkDateDS = new FieldAbsoluteDate<>(field, approxUplinkDate);
            final FieldTransform<DerivativeStructure> offsetToInertialApproxUplink =
                            station.getOffsetToInertial(state.getFrame(), approxUplinkDateDS, factory, 6, 7, 8);

            final TimeStampedFieldPVCoordinates<DerivativeStructure> stationApproxUplink =
                            offsetToInertialApproxUplink.transformPVCoordinates(new TimeStampedFieldPVCoordinates<>(approxUplinkDateDS,
                                                                                                                    zero, zero, zero));

            final DerivativeStructure tauU = station.signalTimeOfFlight(stationApproxUplink,
                                                                        transitPV.getPosition(),
                                                                        transitPV.getDate());

            final TimeStampedFieldPVCoordinates<DerivativeStructure> stationUplink =
                            stationApproxUplink.shiftedBy(transitPV.getDate().durationFrom(approxUplinkDateDS).subtract(tauU));

            final EstimatedMeasurement<RangeRate> evalOneWay2 =
                            oneWayTheoreticalEvaluation(iteration, evaluation, stationUplink, transitPV, transitState);

            // combine uplink and downlink
            estimated.setEstimatedValue(0.5 * (estimated.getEstimatedValue()[0] + evalOneWay2.getEstimatedValue()[0]));
            final double[][] sd1 = estimated.getStateDerivatives();
            final double[][] sd2 = evalOneWay2.getStateDerivatives();
            final double[][] sd = new double[sd1.length][sd1[0].length];
            for (int i = 0; i < sd.length; ++i) {
                for (int j = 0; j < sd[0].length; ++j) {
                    sd[i][j] = 0.5 * (sd1[i][j] + sd2[i][j]);
                }
            }
            estimated.setStateDerivatives(sd);

            for (final ParameterDriver driver : Arrays.asList(station.getEastOffsetDriver(),
                                                              station.getNorthOffsetDriver(),
                                                              station.getZenithOffsetDriver())) {
                if (driver.isSelected()) {
                    final double[] pd1 = estimated.getParameterDerivatives(driver);
                    final double[] pd2 = evalOneWay2.getParameterDerivatives(driver);
                    final double[] pd = new double[pd1.length];
                    for (int i = 0; i < pd.length; ++i) {
                        pd[i] = 0.5 * (pd1[i] + pd2[i]);
                    }
                    estimated.setParameterDerivatives(driver, pd);
                }
            }
        }

        return estimated;
    }

    /** Evaluate measurement in one-way.
     * @param iteration iteration number
     * @param evaluation evaluations counter
     * @param stationPV station coordinates when signal is at station
     * @param transitPV spacecraft coordinates at onboard signal transit
     * @param transitState orbital state at onboard signal transit
     * @return theoretical value
     * @exception OrekitException if value cannot be computed
     * @see #evaluate(SpacecraftStatet)
     */
    private EstimatedMeasurement<RangeRate> oneWayTheoreticalEvaluation(final int iteration, final int evaluation,
                                                                        final TimeStampedFieldPVCoordinates<DerivativeStructure> stationPV,
                                                                        final TimeStampedFieldPVCoordinates<DerivativeStructure> transitPV,
                                                                        final SpacecraftState transitState)
        throws OrekitException {
        // prepare the evaluation
        final EstimatedMeasurement<RangeRate> estimated =
                        new EstimatedMeasurement<RangeRate>(this, iteration, evaluation, transitState);

        // range rate value
        final FieldVector3D<DerivativeStructure> stationPosition  = stationPV.getPosition();
        final FieldVector3D<DerivativeStructure> relativePosition = stationPosition.subtract(transitPV.getPosition());

        final FieldVector3D<DerivativeStructure> stationVelocity  = stationPV.getVelocity();
        final FieldVector3D<DerivativeStructure> relativeVelocity = stationVelocity.subtract(transitPV.getVelocity());

        // radial direction
        final FieldVector3D<DerivativeStructure> lineOfSight      = relativePosition.normalize();

        // range rate
        final DerivativeStructure rangeRate = FieldVector3D.dotProduct(relativeVelocity, lineOfSight);

        estimated.setEstimatedValue(rangeRate.getValue());

        // compute partial derivatives of (rr) with respect to spacecraft state Cartesian coordinates.
        estimated.setStateDerivatives(new double[] {
            rangeRate.getPartialDerivative(1, 0, 0, 0, 0, 0, 0, 0, 0), // dRROndPx
            rangeRate.getPartialDerivative(0, 1, 0, 0, 0, 0, 0, 0, 0), // dRROndPy
            rangeRate.getPartialDerivative(0, 0, 1, 0, 0, 0, 0, 0, 0), // dRROndPz
            rangeRate.getPartialDerivative(0, 0, 0, 1, 0, 0, 0, 0, 0), // dRROndVx
            rangeRate.getPartialDerivative(0, 0, 0, 0, 1, 0, 0, 0, 0), // dRROndVy
            rangeRate.getPartialDerivative(0, 0, 0, 0, 0, 1, 0, 0, 0)  // dRROndVz
        });

        // Set parameter drivers partial derivatives with respect to station position in offset topocentric frame
        if (station.getEastOffsetDriver().isSelected()) {
            estimated.setParameterDerivatives(station.getEastOffsetDriver(),
                                              rangeRate.getPartialDerivative(0, 0, 0, 0, 0, 0, 1, 0, 0)); // dRROndQTx
        }
        if (station.getNorthOffsetDriver().isSelected()) {
            estimated.setParameterDerivatives(station.getNorthOffsetDriver(),
                                              rangeRate.getPartialDerivative(0, 0, 0, 0, 0, 0, 0, 1, 0)); // dRROndQTy
        }
        if (station.getZenithOffsetDriver().isSelected()) {
            estimated.setParameterDerivatives(station.getZenithOffsetDriver(),
                                              rangeRate.getPartialDerivative(0, 0, 0, 0, 0, 0, 0, 0, 1)); // dRROndQTz
        }

        return estimated;

    }

}
