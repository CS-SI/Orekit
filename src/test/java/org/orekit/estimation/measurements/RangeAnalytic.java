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
package org.orekit.estimation.measurements;

import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.analysis.differentiation.GradientField;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.frames.FieldTransform;
import org.orekit.frames.Transform;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.AngularCoordinates;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TimeStampedFieldPVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;
import org.orekit.utils.TimeSpanMap.Span;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

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
 * Difference with the Range class in src folder are:
 *  - The computation of the evaluation is made with analytic formulas
 *    instead of using auto-differentiation and derivative structures
 *    It is the implementation used in Orekit 8.0
 *  - A function evaluating the difference between analytical calculation
 *    and numerical calculation was added for validation
 * </p>
 * @author Thierry Ceolin
 * @author Luc Maisonobe
 * @author Maxime Journot
 * @since 9.0
 */
public class RangeAnalytic extends Range {

    /** Type of the measurement. */
    public static final String MEASUREMENT_TYPE = "RangeAnalytic";

    /** Constructor from parent Range class
     * @param range parent class
     */
    public RangeAnalytic(final Range range) {
        super(range.getStation(), true, range.getDate(), range.getObservedValue()[0],
              range.getTheoreticalStandardDeviation()[0],
              range.getBaseWeight()[0],
              new ObservableSatellite(0));
    }

    /**
     * Analytical version of the theoreticalEvaluation function in Range class
     * The derivative structures are not used, an analytical computation is used instead.
     * @param iteration current LS estimator iteration
     * @param evaluation current LS estimator evaluation
     * @param state spacecraft state. At measurement date on first iteration then close to emission date on further iterations
     * @return
     */
    protected EstimatedMeasurement<Range> theoreticalEvaluationAnalytic(final int iteration, final int evaluation,
                                                                        final SpacecraftState state) {

        // Station attribute from parent Range class
        final GroundStation groundStation = this.getStation();

        // Station position at signal arrival
        final AbsoluteDate downlinkDate = getDate();
        final Transform topoToInertDownlink =
                        groundStation.getOffsetToInertial(state.getFrame(), downlinkDate, false);
        final TimeStampedPVCoordinates stationDownlink =
                        topoToInertDownlink.transformPVCoordinates(new TimeStampedPVCoordinates(downlinkDate,
                                                                                                PVCoordinates.ZERO));

        // Take propagation time into account
        // (if state has already been set up to pre-compensate propagation delay,
        //  we will have offset == downlinkDelay and transitState will be
        //  the same as state)
        // Downlink time of flight
        final double          tauD         = signalTimeOfFlight(state.getPVCoordinates(),
                                                                stationDownlink.getPosition(),
                                                                downlinkDate);
        final double          delta        = downlinkDate.durationFrom(state.getDate());
        final double          dt           = delta - tauD;

        // Transit state position
        final SpacecraftState transitState = state.shiftedBy(dt);
        final AbsoluteDate    transitDate  = transitState.getDate();
        final Vector3D        transitP     = transitState.getPosition();

        // Station position at transit state date
        final Transform topoToInertAtTransitDate =
                      groundStation.getOffsetToInertial(state.getFrame(), transitDate, false);
        final TimeStampedPVCoordinates stationAtTransitDate = topoToInertAtTransitDate.
                      transformPVCoordinates(new TimeStampedPVCoordinates(transitDate, PVCoordinates.ZERO));

        // Uplink time of flight
        final double          tauU         = signalTimeOfFlight(stationAtTransitDate, transitP, transitDate);
        final double          tau          = tauD + tauU;

        // Real date and position of station at signal departure
        final AbsoluteDate             uplinkDate    = downlinkDate.shiftedBy(-tau);
        final TimeStampedPVCoordinates stationUplink = topoToInertDownlink.shiftedBy(-tau).
                        transformPVCoordinates(new TimeStampedPVCoordinates(uplinkDate, PVCoordinates.ZERO));

        // Prepare the evaluation
        final EstimatedMeasurement<Range> estimated =
                        new EstimatedMeasurement<Range>(this, iteration, evaluation,
                                                        new SpacecraftState[] {
                                                            transitState
                                                        }, new TimeStampedPVCoordinates[] {
                                                            stationUplink,
                                                            transitState.getPVCoordinates(),
                                                            stationDownlink
                                                        });

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
        final double dTauUdPx = 1./dUp*upInert
                        .dotProduct(Vector3D.PLUS_I
                                    .add((qv.subtract(v))
                                         .scalarMultiply(dTauDdPx)));
        final double dTauUdPy = 1./dUp*upInert
                        .dotProduct(Vector3D.PLUS_J
                                    .add((qv.subtract(v))
                                         .scalarMultiply(dTauDdPy)));
        final double dTauUdPz = 1./dUp*upInert
                        .dotProduct(Vector3D.PLUS_K
                                    .add((qv.subtract(v))
                                         .scalarMultiply(dTauDdPz)));


        // derivatives of the range measurement
        final double dRdPx = (dTauDdPx + dTauUdPx) * cOver2;
        final double dRdPy = (dTauDdPy + dTauUdPy) * cOver2;
        final double dRdPz = (dTauDdPz + dTauUdPz) * cOver2;
        estimated.setStateDerivatives(0, new double[] {
                                                    dRdPx,      dRdPy,      dRdPz,
                                                    dRdPx * dt, dRdPy * dt, dRdPz * dt
        });

        if (groundStation.getClockOffsetDriver().isSelected() ||
            groundStation.getEastOffsetDriver().isSelected()  ||
            groundStation.getNorthOffsetDriver().isSelected() ||
            groundStation.getZenithOffsetDriver().isSelected()) {

            // Downlink tme of flight derivatives / station position in topocentric frame
            final AngularCoordinates ac = topoToInertDownlink.getAngular().revert();
            //final Rotation rotTopoToInert = ac.getRotation();
            final Vector3D omega        = ac.getRotationRate();

            // Inertial frame
            final double dTauDdQIx = downInert.getX()/dDown;
            final double dTauDdQIy = downInert.getY()/dDown;
            final double dTauDdQIz = downInert.getZ()/dDown;

            // Uplink tme of flight derivatives / station position in topocentric frame
            // Inertial frame
            final double dTauUdQIx = 1/dUp*upInert
                            .dotProduct(Vector3D.MINUS_I
                                        .add((qv.subtract(v)).scalarMultiply(dTauDdQIx))
                                        .subtract(Vector3D.PLUS_I.crossProduct(omega).scalarMultiply(tau)));
            final double dTauUdQIy = 1/dUp*upInert
                            .dotProduct(Vector3D.MINUS_J
                                        .add((qv.subtract(v)).scalarMultiply(dTauDdQIy))
                                        .subtract(Vector3D.PLUS_J.crossProduct(omega).scalarMultiply(tau)));
            final double dTauUdQIz = 1/dUp*upInert
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

            if (groundStation.getEastOffsetDriver().isSelected()) {
                estimated.setParameterDerivatives(groundStation.getEastOffsetDriver(), new AbsoluteDate(), dRdQT.getX());
            }
            if (groundStation.getNorthOffsetDriver().isSelected()) {
                estimated.setParameterDerivatives(groundStation.getNorthOffsetDriver(), new AbsoluteDate(), dRdQT.getY());
            }
            if (groundStation.getZenithOffsetDriver().isSelected()) {
                estimated.setParameterDerivatives(groundStation.getZenithOffsetDriver(), new AbsoluteDate(), dRdQT.getZ());
            }

        }

        return estimated;

    }


    /**
     * Added for validation
     * Compares directly numeric and analytic computations
     * @param iteration
     * @param evaluation
     * @param state
     * @return
     */
    protected EstimatedMeasurement<Range> theoreticalEvaluationValidation(final int iteration, final int evaluation,
                                                                          final SpacecraftState state) {

        // Station & DSFactory attributes from parent Range class
        final GroundStation groundStation             =  getStation();

        // get the number of parameters used for derivation
        int nbParams = 6;
        final Map<String, Integer> indices = new HashMap<>();
        for (ParameterDriver driver : getParametersDrivers()) {
            for (Span<String> span = driver.getNamesSpanMap().getFirstSpan(); span != null; span = span.next()) {

                if (driver.isSelected()) {
                    indices.put(span.getData(), nbParams++);
                }
            }
        }
        final FieldVector3D<Gradient> zero = FieldVector3D.getZero(GradientField.getField(nbParams));

        // Range derivatives are computed with respect to spacecraft state in inertial frame
        // and station position in station's offset frame
        // -------
        //
        // Parameters:
        //  - 0..2 - Px, Py, Pz   : Position of the spacecraft in inertial frame
        //  - 3..5 - Vx, Vy, Vz   : Velocity of the spacecraft in inertial frame
        //  - 6..8 - QTx, QTy, QTz: Position of the station in station's offset frame

        // Coordinates of the spacecraft expressed as a gradient
        final TimeStampedFieldPVCoordinates<Gradient> pvaDS = getCoordinates(state, 0, nbParams);

        // transform between station and inertial frame, expressed as a gradient
        // The components of station's position in offset frame are the 3 last derivative parameters
        final AbsoluteDate downlinkDate = getDate();
        final FieldTransform<Gradient> offsetToInertialDownlink =
                        groundStation.getOffsetToInertial(state.getFrame(), downlinkDate, nbParams, indices);
        final FieldAbsoluteDate<Gradient> downlinkDateDS =
                        offsetToInertialDownlink.getFieldDate();

        // Station position in inertial frame at end of the downlink leg
        final TimeStampedFieldPVCoordinates<Gradient> stationDownlink =
                        offsetToInertialDownlink.transformPVCoordinates(new TimeStampedFieldPVCoordinates<>(downlinkDateDS,
                                                                                                            zero, zero, zero));

        // Compute propagation times
        // (if state has already been set up to pre-compensate propagation delay,
        //  we will have offset == downlinkDelay and transitState will be
        //  the same as state)

        // Downlink delay
        final Gradient tauD = signalTimeOfFlight(pvaDS, stationDownlink.getPosition(), downlinkDateDS);

        // Transit state
        final double                delta        = downlinkDate.durationFrom(state.getDate());
        final Gradient              tauDMDelta   = tauD.negate().add(delta);
        final SpacecraftState       transitState = state.shiftedBy(tauDMDelta.getValue());

        // Transit state position (re)computed with gradients
        final TimeStampedFieldPVCoordinates<Gradient> transitStateDS = pvaDS.shiftedBy(tauDMDelta);

        // Station at transit state date (derivatives of tauD taken into account)
        final TimeStampedFieldPVCoordinates<Gradient> stationAtTransitDate =
                        stationDownlink.shiftedBy(tauD.negate());
        // Uplink delay
        final Gradient tauU =
                        signalTimeOfFlight(stationAtTransitDate, transitStateDS.getPosition(), transitStateDS.getDate());

        // Prepare the evaluation
        final EstimatedMeasurement<Range> estimated =
                        new EstimatedMeasurement<Range>(this, iteration, evaluation,
                                                        new SpacecraftState[] {
                                                            transitState
                                                        }, null);

        // Range value
        final Gradient tau    = tauD.add(tauU);
        final double   cOver2 = 0.5 * Constants.SPEED_OF_LIGHT;
        final Gradient range  = tau.multiply(cOver2);
        estimated.setEstimatedValue(range.getValue());

        // Range partial derivatives with respect to state
        final double[] derivatives = range.getGradient();
        estimated.setStateDerivatives(0, Arrays.copyOfRange(derivatives, 0, 6));

        // set partial derivatives with respect to parameters
        // (beware element at index 0 is the value, not a derivative)
        for (final ParameterDriver driver : getParametersDrivers()) {
            for (Span<String> span = driver.getNamesSpanMap().getFirstSpan(); span != null; span = span.next()) {

                final Integer index = indices.get(span.getData());
                if (index != null) {
                    estimated.setParameterDerivatives(driver, span.getStart(), derivatives[index]);
                }
            }
        }

        // ----------
        // VALIDATION
        //-----------

        // Computation of the value without Gradient
        // ----------------------------------

        // Time difference between t (date of the measurement) and t' (date tagged in spacecraft state)

        // Station position at signal arrival
        final Transform topoToInertDownlink =
                        groundStation.getOffsetToInertial(state.getFrame(), downlinkDate, false);
        final PVCoordinates QDownlink = topoToInertDownlink.
                        transformPVCoordinates(PVCoordinates.ZERO);

        // Downlink time of flight from spacecraft to station
        final double td = signalTimeOfFlight(state.getPVCoordinates(), QDownlink.getPosition(), downlinkDate);
        final double dt = delta - td;

        // Transit state position
        final AbsoluteDate    transitT = state.getDate().shiftedBy(dt);
        final SpacecraftState transit  = state.shiftedBy(dt);
        final Vector3D        transitP = transitState.getPosition();

        // Station position at signal departure
        // First guess
//        AbsoluteDate uplinkDate = downlinkDate.shiftedBy(-getObservedValue()[0] / cOver2);
//        final Transform topoToInertUplink =
//                        station.getOffsetFrame().getTransformTo(state.getFrame(), uplinkDate);
//        TimeStampedPVCoordinates QUplink = topoToInertUplink.
//                        transformPVCoordinates(new TimeStampedPVCoordinates(uplinkDate, PVCoordinates.ZERO));

        // Station position at transit state date
        final Transform topoToInertAtTransitDate =
                      groundStation.getOffsetToInertial(state.getFrame(), transitT, false);
        TimeStampedPVCoordinates QAtTransitDate = topoToInertAtTransitDate.
                      transformPVCoordinates(new TimeStampedPVCoordinates(transitT, PVCoordinates.ZERO));

        // Uplink time of flight
        final double tu = signalTimeOfFlight(QAtTransitDate, transitP, transitT);

        // Total time of flight
        final double t = td + tu;

        // Real date and position of station at signal departure
        AbsoluteDate uplinkDate    = downlinkDate.shiftedBy(-t);

        TimeStampedPVCoordinates QUplink = topoToInertDownlink.shiftedBy(-t).
                        transformPVCoordinates(new TimeStampedPVCoordinates(uplinkDate, PVCoordinates.ZERO));


        // Range value
        double r = t * cOver2;
        double dR = r-range.getValue();



        // td derivatives / state
        // -----------------------

        // Qt = Primary station position at tmeas = t = signal arrival at primary station
        final Vector3D vel     = state.getPVCoordinates().getVelocity();
        final Vector3D Qt_V    = QDownlink.getVelocity();
        final Vector3D Ptr     = transit.getPosition();
        final Vector3D Ptr_Qt  = QDownlink.getPosition().subtract(Ptr);
        final double   dDown   = Constants.SPEED_OF_LIGHT * Constants.SPEED_OF_LIGHT * td -
                        Vector3D.dotProduct(Ptr_Qt, vel);

        // Derivatives of the downlink time of flight
        final double dtddPx   = -Ptr_Qt.getX() / dDown;
        final double dtddPy   = -Ptr_Qt.getY() / dDown;
        final double dtddPz   = -Ptr_Qt.getZ() / dDown;

        final double dtddVx   = dtddPx*dt;
        final double dtddVy   = dtddPy*dt;
        final double dtddVz   = dtddPz*dt;

        // From the DS
        final double dtddPxDS = tauD.getPartialDerivative(0);
        final double dtddPyDS = tauD.getPartialDerivative(1);
        final double dtddPzDS = tauD.getPartialDerivative(2);
        final double dtddVxDS = tauD.getPartialDerivative(3);
        final double dtddVyDS = tauD.getPartialDerivative(4);
        final double dtddVzDS = tauD.getPartialDerivative(5);

        // Difference
        final double d_dtddPx = dtddPxDS-dtddPx;
        final double d_dtddPy = dtddPyDS-dtddPy;
        final double d_dtddPz = dtddPzDS-dtddPz;
        final double d_dtddVx = dtddVxDS-dtddVx;
        final double d_dtddVy = dtddVyDS-dtddVy;
        final double d_dtddVz = dtddVzDS-dtddVz;


        // tu derivatives / state
        // -----------------------



        final Vector3D Qt2_Ptr  = Ptr.subtract(QUplink.getPosition());
        final double   dUp      = Constants.SPEED_OF_LIGHT * Constants.SPEED_OF_LIGHT * tu -
                        Vector3D.dotProduct(Qt2_Ptr, Qt_V);

        //test
//        // Speed of the station at tmeas-t
//        // Note: Which one to use in the calculation of dUp ???
//        final Vector3D Qt2_V    = QUplink.getVelocity();
//        final double   dUp      = Constants.SPEED_OF_LIGHT * Constants.SPEED_OF_LIGHT * tu -
//                        Vector3D.dotProduct(Qt2_Ptr, Qt2_V);
        //test


        // tu derivatives
        final double dtudPx = 1./dUp*Qt2_Ptr
                        .dotProduct(Vector3D.PLUS_I
                                    .add((Qt_V.subtract(vel))
                                         .scalarMultiply(dtddPx)));
        final double dtudPy = 1./dUp*Qt2_Ptr
                        .dotProduct(Vector3D.PLUS_J
                                    .add((Qt_V.subtract(vel))
                                         .scalarMultiply(dtddPy)));
        final double dtudPz = 1./dUp*Qt2_Ptr
                        .dotProduct(Vector3D.PLUS_K
                                    .add((Qt_V.subtract(vel))
                                         .scalarMultiply(dtddPz)));

        final double dtudVx   = dtudPx*dt;
        final double dtudVy   = dtudPy*dt;
        final double dtudVz   = dtudPz*dt;


        // From the DS
        final double dtudPxDS = tauU.getPartialDerivative(0);
        final double dtudPyDS = tauU.getPartialDerivative(1);
        final double dtudPzDS = tauU.getPartialDerivative(2);
        final double dtudVxDS = tauU.getPartialDerivative(3);
        final double dtudVyDS = tauU.getPartialDerivative(4);
        final double dtudVzDS = tauU.getPartialDerivative(5);

        // Difference
        final double d_dtudPx = dtudPxDS-dtudPx;
        final double d_dtudPy = dtudPyDS-dtudPy;
        final double d_dtudPz = dtudPzDS-dtudPz;
        final double d_dtudVx = dtudVxDS-dtudVx;
        final double d_dtudVy = dtudVyDS-dtudVy;
        final double d_dtudVz = dtudVzDS-dtudVz;


        // Range derivatives / state
        // -----------------------

        // R = Range
        double dRdPx = (dtddPx + dtudPx)*cOver2;
        double dRdPy = (dtddPy + dtudPy)*cOver2;
        double dRdPz = (dtddPz + dtudPz)*cOver2;
        double dRdVx = (dtddVx + dtudVx)*cOver2;
        double dRdVy = (dtddVy + dtudVy)*cOver2;
        double dRdVz = (dtddVz + dtudVz)*cOver2;

        // With DS
        double dRdPxDS = range.getPartialDerivative(0);
        double dRdPyDS = range.getPartialDerivative(1);
        double dRdPzDS = range.getPartialDerivative(2);
        double dRdVxDS = range.getPartialDerivative(3);
        double dRdVyDS = range.getPartialDerivative(4);
        double dRdVzDS = range.getPartialDerivative(5);

        // Diff
        final double d_dRdPx = dRdPxDS-dRdPx;
        final double d_dRdPy = dRdPyDS-dRdPy;
        final double d_dRdPz = dRdPzDS-dRdPz;
        final double d_dRdVx = dRdVxDS-dRdVx;
        final double d_dRdVy = dRdVyDS-dRdVy;
        final double d_dRdVz = dRdVzDS-dRdVz;


        // td derivatives / station
        // -----------------------

        final AngularCoordinates ac = topoToInertDownlink.getAngular().revert();
        final Rotation rotTopoToInert = ac.getRotation();
        final Vector3D omega        = ac.getRotationRate();

        final Vector3D dtddQI = Ptr_Qt.scalarMultiply(1./dDown);
        final double dtddQIx = dtddQI.getX();
        final double dtddQIy = dtddQI.getY();
        final double dtddQIz = dtddQI.getZ();

        final Vector3D dtddQ = rotTopoToInert.applyTo(dtddQI);

        // With DS
        double dtddQxDS = tauD.getPartialDerivative(6);
        double dtddQyDS = tauD.getPartialDerivative(7);
        double dtddQzDS = tauD.getPartialDerivative(8);

        // Diff
        final double d_dtddQx = dtddQxDS-dtddQ.getX();
        final double d_dtddQy = dtddQyDS-dtddQ.getY();
        final double d_dtddQz = dtddQzDS-dtddQ.getZ();


        // tu derivatives / station
        // -----------------------

        // Inertial frame
        final double dtudQIx = 1/dUp*Qt2_Ptr
                        .dotProduct(Vector3D.MINUS_I
                                    .add((Qt_V.subtract(vel)).scalarMultiply(dtddQIx))
                                    .subtract(Vector3D.PLUS_I.crossProduct(omega).scalarMultiply(t)));
        final double dtudQIy = 1/dUp*Qt2_Ptr
                        .dotProduct(Vector3D.MINUS_J
                                    .add((Qt_V.subtract(vel)).scalarMultiply(dtddQIy))
                                    .subtract(Vector3D.PLUS_J.crossProduct(omega).scalarMultiply(t)));
        final double dtudQIz = 1/dUp*Qt2_Ptr
                        .dotProduct(Vector3D.MINUS_K
                                    .add((Qt_V.subtract(vel)).scalarMultiply(dtddQIz))
                                    .subtract(Vector3D.PLUS_K.crossProduct(omega).scalarMultiply(t)));

//        // test
//        final double dtudQIx = 1/dUp*Qt2_Ptr
////                        .dotProduct(Vector3D.MINUS_I);
////                                    .dotProduct((Qt_V.subtract(vel)).scalarMultiply(dtddQIx));
//                                    .dotProduct(Vector3D.MINUS_I.crossProduct(omega).scalarMultiply(t));
//        final double dtudQIy = 1/dUp*Qt2_Ptr
////                        .dotProduct(Vector3D.MINUS_J);
////                                    .dotProduct((Qt_V.subtract(vel)).scalarMultiply(dtddQIy));
//                                    .dotProduct(Vector3D.MINUS_J.crossProduct(omega).scalarMultiply(t));
//        final double dtudQIz = 1/dUp*Qt2_Ptr
////                        .dotProduct(Vector3D.MINUS_K);
////                                    .dotProduct((Qt_V.subtract(vel)).scalarMultiply(dtddQIz));
//                                    .dotProduct(Vector3D.MINUS_K.crossProduct(omega).scalarMultiply(t));
//
//        double dtu_dQxDS = tauU.getPartialDerivative(0, 0, 0, 0, 0, 0, 1, 0, 0);
//        double dtu_dQyDS = tauU.getPartialDerivative(0, 0, 0, 0, 0, 0, 0, 1, 0);
//        double dtu_dQzDS = tauU.getPartialDerivative(0, 0, 0, 0, 0, 0, 0, 0, 1);
//        final Vector3D dtudQDS = new Vector3D(dtu_dQxDS, dtu_dQyDS, dtu_dQzDS);
//        final Vector3D dtudQIDS = rotTopoToInert.applyInverseTo(dtudQDS);
//        double dtudQIxDS = dtudQIDS.getX();
//        double dtudQIyDS = dtudQIDS.getY();
//        double dtudQIxzS = dtudQIDS.getZ();
//        // test

        // Topocentric frame
        final Vector3D dtudQI = new Vector3D(dtudQIx, dtudQIy, dtudQIz);
        final Vector3D dtudQ = rotTopoToInert.applyTo(dtudQI);


        // With DS
        double dtudQxDS = tauU.getPartialDerivative(6);
        double dtudQyDS = tauU.getPartialDerivative(7);
        double dtudQzDS = tauU.getPartialDerivative(8);

        // Diff
        final double d_dtudQx = dtudQxDS-dtudQ.getX();
        final double d_dtudQy = dtudQyDS-dtudQ.getY();
        final double d_dtudQz = dtudQzDS-dtudQ.getZ();


        // Range derivatives / station
        // -----------------------

        double dRdQx = (dtddQ.getX() + dtudQ.getX())*cOver2;
        double dRdQy = (dtddQ.getY() + dtudQ.getY())*cOver2;
        double dRdQz = (dtddQ.getZ() + dtudQ.getZ())*cOver2;

        // With DS
        double dRdQxDS = range.getPartialDerivative(6);
        double dRdQyDS = range.getPartialDerivative(7);
        double dRdQzDS = range.getPartialDerivative(8);

        // Diff
        final double d_dRdQx = dRdQxDS-dRdQx;
        final double d_dRdQy = dRdQyDS-dRdQy;
        final double d_dRdQz = dRdQzDS-dRdQz;


        // Print results to avoid warning
        final boolean printResults = false;

        if (printResults) {
            System.out.println("dR = " + dR);

            System.out.println("d_dtddPx = " + d_dtddPx);
            System.out.println("d_dtddPy = " + d_dtddPy);
            System.out.println("d_dtddPz = " + d_dtddPz);
            System.out.println("d_dtddVx = " + d_dtddVx);
            System.out.println("d_dtddVy = " + d_dtddVy);
            System.out.println("d_dtddVz = " + d_dtddVz);

            System.out.println("d_dtudPx = " + d_dtudPx);
            System.out.println("d_dtudPy = " + d_dtudPy);
            System.out.println("d_dtudPz = " + d_dtudPz);
            System.out.println("d_dtudVx = " + d_dtudVx);
            System.out.println("d_dtudVy = " + d_dtudVy);
            System.out.println("d_dtudVz = " + d_dtudVz);

            System.out.println("d_dRdPx = " + d_dRdPx);
            System.out.println("d_dRdPy = " + d_dRdPy);
            System.out.println("d_dRdPz = " + d_dRdPz);
            System.out.println("d_dRdVx = " + d_dRdVx);
            System.out.println("d_dRdVy = " + d_dRdVy);
            System.out.println("d_dRdVz = " + d_dRdVz);

            System.out.println("d_dtddQx = " + d_dtddQx);
            System.out.println("d_dtddQy = " + d_dtddQy);
            System.out.println("d_dtddQz = " + d_dtddQz);

            System.out.println("d_dtudQx = " + d_dtudQx);
            System.out.println("d_dtudQy = " + d_dtudQy);
            System.out.println("d_dtudQz = " + d_dtudQz);

            System.out.println("d_dRdQx = " + d_dRdQx);
            System.out.println("d_dRdQy = " + d_dRdQy);
            System.out.println("d_dRdQz = " + d_dRdQz);

        }

        // Dummy return
        return estimated;

    }
}
