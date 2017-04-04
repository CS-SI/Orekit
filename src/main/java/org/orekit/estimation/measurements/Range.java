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

import org.hipparchus.analysis.differentiation.DSFactory;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.Transform;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.AngularCoordinates;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.TimeStampedFieldPVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

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
 * @author Maxime Journot
 * @since 8.0
 */
public class Range extends AbstractMeasurement<Range> {

    /** Ground station from which measurement is performed. */
    private final GroundStation station;

    /** Factory for the DerivativeStructure instances. */
    private final DSFactory factory;

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
        super(date, range, sigma, baseWeight,
              station.getEastOffsetDriver(),
              station.getNorthOffsetDriver(),
              station.getZenithOffsetDriver());
        this.station = station;
        this.factory = new DSFactory(9, 1);
    }

    /** Get the ground station from which measurement is performed.
     * @return ground station from which measurement is performed
     */
    public GroundStation getStation() {
        return station;
    }

    /** {@inheritDoc} */
    @Override
    protected EstimatedMeasurement<Range> theoreticalEvaluation(final int iteration,
                                                                final int evaluation,
                                                                final SpacecraftState state)
        throws OrekitException {


        // Range derivatives are computed with respect to spacecraft state in inertial frame
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
                        new FieldVector3D<DerivativeStructure>(factory.variable(0, stateP.getX()),
                                                               factory.variable(1, stateP.getY()),
                                                               factory.variable(2, stateP.getZ()));

        // Velocity of the spacecraft expressed as a derivative structure
        // The components of the velocity are the 3 second derivative parameters
        final Vector3D stateV = state.getPVCoordinates().getVelocity();
        final FieldVector3D<DerivativeStructure> vDS =
                        new FieldVector3D<DerivativeStructure>(factory.variable(3, stateV.getX()),
                                                               factory.variable(4, stateV.getY()),
                                                               factory.variable(5, stateV.getZ()));

        // Acceleration of the spacecraft
        // The components of the acceleration are not derivative parameters
        final Vector3D stateA = state.getPVCoordinates().getAcceleration();
        final FieldVector3D<DerivativeStructure> aDS =
                        new FieldVector3D<DerivativeStructure>(factory.constant(stateA.getX()),
                                                               factory.constant(stateA.getY()),
                                                               factory.constant(stateA.getZ()));

        final TimeStampedFieldPVCoordinates<DerivativeStructure> pvaDS =
                        new TimeStampedFieldPVCoordinates<DerivativeStructure>(state.getDate(), pDS, vDS, aDS);

        // Station position in body frame, expressed as a derivative structure
        // The components of station's position in offset frame are the 3 last derivative parameters
        final GroundStation.OffsetDerivatives od = station.getOffsetDerivatives(factory, 6, 7, 8);
        final Frame bodyframe = station.getOffsetFrame().getParentShape().getBodyFrame();

        // Station position in inertial frame at end of the downlink leg
        final AbsoluteDate downlinkDate = getDate();
        final Transform bodyToInertDownlink = bodyframe.getTransformTo(state.getFrame(), downlinkDate);
        final TimeStampedFieldPVCoordinates<DerivativeStructure> stationDownlink =
                        bodyToInertDownlink.transformPVCoordinates(new TimeStampedFieldPVCoordinates<>(
                                        downlinkDate,
                                        od.getOrigin(),
                                        od.getZero(),
                                        od.getZero()));

        // Compute propagation times
        // (if state has already been set up to pre-compensate propagation delay,
        //  we will have offset == downlinkDelay and transitState will be
        //  the same as state)

        // Downlink delay
        final DerivativeStructure tauD = station.signalTimeOfFlight(pvaDS, stationDownlink.getPosition(), downlinkDate);

        // Transit state
        final double                delta        = downlinkDate.durationFrom(state.getDate());
        final DerivativeStructure   tauDMDelta   = tauD.negate().add(delta);
        final SpacecraftState       transitState = state.shiftedBy(tauDMDelta.getValue());


        // Transit state position (re)computed with derivative structures
        final FieldVector3D<DerivativeStructure> transitStatePosition = pvaDS.shiftedBy(tauDMDelta).getPosition();

        // Station at transit state date (derivatives of tauD taken into account)
        final TimeStampedFieldPVCoordinates<DerivativeStructure> stationAtTransitDate =
                        stationDownlink.shiftedBy(tauD.negate());

        // Uplink delay
        final DerivativeStructure tauU = station.signalTimeOfFlight(stationAtTransitDate,
                                                                    transitStatePosition,
                                                                    transitState.getDate());
        // Prepare the evaluation
        final EstimatedMeasurement<Range> estimated =
                        new EstimatedMeasurement<Range>(this, iteration, evaluation, transitState);

        // Range value
        final double              cOver2 = 0.5 * Constants.SPEED_OF_LIGHT;
        final DerivativeStructure tau    = tauD.add(tauU);
        final DerivativeStructure range  = tau.multiply(cOver2);
        estimated.setEstimatedValue(range.getValue());

        // Range partial derivatives with respect to state
        estimated.setStateDerivatives(new double[] {
                                          range.getPartialDerivative(1, 0, 0, 0, 0, 0, 0, 0, 0), // dROndPx
                                          range.getPartialDerivative(0, 1, 0, 0, 0, 0, 0, 0, 0), // dROndPy
                                          range.getPartialDerivative(0, 0, 1, 0, 0, 0, 0, 0, 0), // dROndPz
                                          range.getPartialDerivative(0, 0, 0, 1, 0, 0, 0, 0, 0), // dROndVx
                                          range.getPartialDerivative(0, 0, 0, 0, 1, 0, 0, 0, 0), // dROndVy
                                          range.getPartialDerivative(0, 0, 0, 0, 0, 1, 0, 0, 0)  // dROndVz
        });


        // Set parameter drivers partial derivatives with respect to station position in offset topocentric frame
        if (station.getEastOffsetDriver().isSelected()) {
            estimated.setParameterDerivatives(station.getEastOffsetDriver(),
                                              range.getPartialDerivative(0, 0, 0, 0, 0, 0, 1, 0, 0)); // dROndQTx
        }
        if (station.getNorthOffsetDriver().isSelected()) {
            estimated.setParameterDerivatives(station.getNorthOffsetDriver(),
                                              range.getPartialDerivative(0, 0, 0, 0, 0, 0, 0, 1, 0)); // dROndQTy
        }
        if (station.getZenithOffsetDriver().isSelected()) {
            estimated.setParameterDerivatives(station.getZenithOffsetDriver(),
                                              range.getPartialDerivative(0, 0, 0, 0, 0, 0, 0, 0, 1)); // dROndQTz
        }

        return estimated;

    }


    /**
     * Analytical version of the theoretical evaluation function
     * The derivative structures are not used, an analytical computation is used instead.
     * @param iteration current LS estimator iteration
     * @param evaluation current LS estimator evaluation
     * @param state spacecraft state. At measurement date on first iteration then close to emission date on further iterations
     * @return theoretical value
     * @throws OrekitException if value cannot be computed
     */
    protected EstimatedMeasurement<Range> theoreticalEvaluationAnalytic(final int iteration, final int evaluation,
                                                                        final SpacecraftState state)
        throws OrekitException {

        // Station position at signal arrival
        final AbsoluteDate downlinkDate = getDate();
        final Transform topoToInertDownlink =
                        station.getOffsetFrame().getTransformTo(state.getFrame(), downlinkDate);
        final PVCoordinates stationDownlink = topoToInertDownlink.
                        transformPVCoordinates(PVCoordinates.ZERO);

        // Take propagation time into account
        // (if state has already been set up to pre-compensate propagation delay,
        //  we will have offset == downlinkDelay and transitState will be
        //  the same as state)
        // Downlink time of flight
        final double          tauD         = station.signalTimeOfFlight(state.getPVCoordinates(),
                                                                        stationDownlink.getPosition(),
                                                                        downlinkDate);
        final double          delta        = downlinkDate.durationFrom(state.getDate());
        final double          dt           = delta - tauD;

        // Transit state position
        final SpacecraftState transitState = state.shiftedBy(dt);
        final AbsoluteDate    transitDate  = transitState.getDate();
        final Vector3D        transitP     = transitState.getPVCoordinates().getPosition();

        // Station position at transit state date
        final Transform topoToInertAtTransitDate =
                      station.getOffsetFrame().getTransformTo(state.getFrame(), transitDate);
        final TimeStampedPVCoordinates stationAtTransitDate = topoToInertAtTransitDate.
                      transformPVCoordinates(new TimeStampedPVCoordinates(transitDate, PVCoordinates.ZERO));

        // Uplink time of flight
        final double          tauU             = station.signalTimeOfFlight(stationAtTransitDate,
                                                                            transitP,
                                                                            transitDate);
        final double          tau              = tauD + tauU;

        // Real date and position of station at signal departure
        final AbsoluteDate             uplinkDate    = downlinkDate.shiftedBy(-tau);
        final TimeStampedPVCoordinates stationUplink = topoToInertDownlink.shiftedBy(-tau).
                        transformPVCoordinates(new TimeStampedPVCoordinates(uplinkDate, PVCoordinates.ZERO));

        // Prepare the evaluation
        final EstimatedMeasurement<Range> estimated =
                        new EstimatedMeasurement<Range>(this, iteration, evaluation, transitState);

        // Set range value
        final double cOver2 = 0.5 * Constants.SPEED_OF_LIGHT;
        estimated.setEstimatedValue(tau * cOver2);

        // Partial derivatives with respect to state
        // The formulas below take into account the fact the measurement is at fixed reception date.
        // When spacecraft position is changed, the downlink delay is changed, and in order
        // to still have the measurement arrive at exactly the same date on ground, we must
        // take the spacecraft-station relative velocity into account.
        final Vector3D v         = state.getPVCoordinates().getVelocity();
        final Vector3D qv        = stationDownlink.getVelocity();
        final Vector3D downInert = stationDownlink.getPosition().subtract(transitP);
        final double   dDown     = Constants.SPEED_OF_LIGHT * Constants.SPEED_OF_LIGHT * tauD -
                        Vector3D.dotProduct(downInert, v);
        final Vector3D upInert   = transitP.subtract(stationUplink.getPosition());

        //test
        //     final double   dUp       = Constants.SPEED_OF_LIGHT * Constants.SPEED_OF_LIGHT * tauU -
        //                     Vector3D.dotProduct(upInert, qv);
        //test
        final double   dUp       = Constants.SPEED_OF_LIGHT * Constants.SPEED_OF_LIGHT * tauU -
                        Vector3D.dotProduct(upInert, stationUplink.getVelocity());


        // derivatives of the downlink time of flight
        final double dTauDdPx   = -downInert.getX() / dDown;
        final double dTauDdPy   = -downInert.getY() / dDown;
        final double dTauDdPz   = -downInert.getZ() / dDown;


        // Derivatives of the uplink time of flight
        final Vector3D qvMv = qv.subtract(v);
        final double dTauUdPx = upInert.dotProduct(new Vector3D(1.0, Vector3D.PLUS_I, dTauDdPx, qvMv)) / dUp;
        final double dTauUdPy = upInert.dotProduct(new Vector3D(1.0, Vector3D.PLUS_J, dTauDdPy, qvMv)) / dUp;
        final double dTauUdPz = upInert.dotProduct(new Vector3D(1.0, Vector3D.PLUS_K, dTauDdPz, qvMv)) / dUp;


        // derivatives of the range measurement
        final double dRdPx = (dTauDdPx + dTauUdPx) * cOver2;
        final double dRdPy = (dTauDdPy + dTauUdPy) * cOver2;
        final double dRdPz = (dTauDdPz + dTauUdPz) * cOver2;
        estimated.setStateDerivatives(new double[] {
            dRdPx,      dRdPy,      dRdPz,
            dRdPx * dt, dRdPy * dt, dRdPz * dt
        });

        if (station.getEastOffsetDriver().isSelected()  ||
                        station.getNorthOffsetDriver().isSelected() ||
                        station.getZenithOffsetDriver().isSelected()) {

            // Downlink tme of flight derivatives / station position in topocentric frame
            final AngularCoordinates ac = topoToInertDownlink.getAngular().revert();
            //final Rotation rotTopoToInert = ac.getRotation();
            final Vector3D omega        = ac.getRotationRate();

            // Inertial frame
            final double dTauDdQIx = downInert.getX() / dDown;
            final double dTauDdQIy = downInert.getY() / dDown;
            final double dTauDdQIz = downInert.getZ() / dDown;

            // Uplink tme of flight derivatives / station position in topocentric frame
            // Inertial frame
            final double dTauUdQIx = 1 / dUp * upInert
                            .dotProduct(Vector3D.MINUS_I
                                        .add((qv.subtract(v)).scalarMultiply(dTauDdQIx))
                                        .subtract(Vector3D.PLUS_I.crossProduct(omega).scalarMultiply(tau)));
            final double dTauUdQIy = 1 / dUp * upInert
                            .dotProduct(Vector3D.MINUS_J
                                        .add((qv.subtract(v)).scalarMultiply(dTauDdQIy))
                                        .subtract(Vector3D.PLUS_J.crossProduct(omega).scalarMultiply(tau)));
            final double dTauUdQIz = 1 / dUp * upInert
                            .dotProduct(Vector3D.MINUS_K
                                        .add((qv.subtract(v)).scalarMultiply(dTauDdQIz))
                                        .subtract(Vector3D.PLUS_K.crossProduct(omega).scalarMultiply(tau)));


            // Range partial derivatives
            // with respect to station position in inertial frame
            final Vector3D dRdQI = new Vector3D((dTauDdQIx + dTauUdQIx) * cOver2,
                                                (dTauDdQIy + dTauUdQIy) * cOver2,
                                                (dTauDdQIz + dTauUdQIz) * cOver2);

            // convert to topocentric frame, as the station position
            // offset parameter is expressed in this frame
            final Vector3D dRdQT = ac.getRotation().applyTo(dRdQI);

            if (station.getEastOffsetDriver().isSelected()) {
                estimated.setParameterDerivatives(station.getEastOffsetDriver(), dRdQT.getX());
            }
            if (station.getNorthOffsetDriver().isSelected()) {
                estimated.setParameterDerivatives(station.getNorthOffsetDriver(), dRdQT.getY());
            }
            if (station.getZenithOffsetDriver().isSelected()) {
                estimated.setParameterDerivatives(station.getZenithOffsetDriver(), dRdQT.getZ());
            }

        }

        return estimated;

    }

}
