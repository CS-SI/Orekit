/* Copyright 2002-2020 CS GROUP
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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.hipparchus.Field;
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

/** Class modeling a turn-around range measurement using a master ground station and a slave ground station.
 * <p>
 * The measurement is considered to be a signal:
 * - Emitted from the master ground station
 * - Reflected on the spacecraft
 * - Reflected on the slave ground station
 * - Reflected on the spacecraft again
 * - Received on the master ground station
 * Its value is the elapsed time between emission and reception
 * divided by 2c were c is the speed of light.
 * The motion of the stations and the spacecraft
 * during the signal flight time are taken into account.
 * The date of the measurement corresponds to the
 * reception on ground of the reflected signal.
 * Difference with the TurnAroundRange class in src folder are:
 *  - The computation of the evaluation is made with analytic formulas
 *    instead of using auto-differentiation and derivative structures
 *  - A function evaluating the difference between analytical calculation
 *    and numerical calculation was added for validation
 * </p>
 * @author Thierry Ceolin
 * @author Luc Maisonobe
 * @author Maxime Journot
 * @since 9.0
 */
public class TurnAroundRangeAnalytic extends TurnAroundRange {

    /** Constructor from parent TurnAroundRange class
     * @param Range parent class
     */
    public TurnAroundRangeAnalytic(final TurnAroundRange turnAroundRange) {
        super(turnAroundRange.getMasterStation(), turnAroundRange.getSlaveStation(),
              turnAroundRange.getDate(), turnAroundRange.getObservedValue()[0],
              turnAroundRange.getTheoreticalStandardDeviation()[0],
              turnAroundRange.getBaseWeight()[0],
              new ObservableSatellite(0));
    }


    /**
     * Analytical version of the function theoreticalEvalution in TurnAroundRange class
     * The derivative structures are not used
     * For now only the value of turn-around range and not its derivatives are available
     * @param iteration
     * @param evaluation
     * @param initialState
     * @param state
     * @return
     */
    protected EstimatedMeasurement<TurnAroundRange> theoreticalEvaluationAnalytic(final int iteration, final int evaluation,
                                                                                  final SpacecraftState initialState,
                                                                                  final SpacecraftState state) {

        // Stations attributes from parent Range class
        final GroundStation masterGroundStation = this.getMasterStation();
        final GroundStation slaveGroundStation  = this.getSlaveStation();

        // Compute propagation times:
        //
        // The path of the signal is divided in two legs.
        // Leg1: Emission from master station to satellite in masterTauU seconds
        //     + Reflection from satellite to slave station in slaveTauD seconds
        // Leg2: Reflection from slave station to satellite in slaveTauU seconds
        //     + Reflection from satellite to master station in masterTaudD seconds
        // The measurement is considered to be time stamped at reception on ground
        // by the master station. All times are therefore computed as backward offsets
        // with respect to this reception time.
        //
        // Two intermediate spacecraft states are defined:
        //  - transitStateLeg2: State of the satellite when it bounced back the signal
        //                      from slave station to master station during the 2nd leg
        //  - transitStateLeg1: State of the satellite when it bounced back the signal
        //                      from master station to slave station during the 1st leg

        // Compute propagation time for the 2nd leg of the signal path
        // --

        // Master station PV at measurement date
        final AbsoluteDate measurementDate = this.getDate();
        final Transform masterTopoToInert =
                        masterGroundStation.getOffsetToInertial(state.getFrame(), measurementDate);
        final TimeStampedPVCoordinates masterArrival =
                        masterTopoToInert.transformPVCoordinates(new TimeStampedPVCoordinates(measurementDate,
                                                                                              PVCoordinates.ZERO));

        // Downlink time of flight from master station at t to spacecraft at t'
        final double tMd    = signalTimeOfFlight(state.getPVCoordinates(), masterArrival.getPosition(), measurementDate);

        // Time difference between t (date of the measurement) and t' (date tagged in spacecraft state)
        // (if state has already been set up to pre-compensate propagation delay, delta = masterTauD + slaveTauU)
        final double delta  = getDate().durationFrom(state.getDate());

        // Transit state from which the satellite reflected the signal from slave to master station
        final SpacecraftState transitStateLeg2  = state.shiftedBy(delta - tMd);
        final AbsoluteDate    transitDateLeg2   = transitStateLeg2.getDate();

        // Slave station PV at transit state leg2 date
        final Transform slaveTopoToInertTransitLeg2 =
                        slaveGroundStation.getOffsetToInertial(state.getFrame(), transitDateLeg2);
        final TimeStampedPVCoordinates QSlaveTransitLeg2PV = slaveTopoToInertTransitLeg2.
                        transformPVCoordinates(new TimeStampedPVCoordinates(transitDateLeg2, PVCoordinates.ZERO));

        // Uplink time of flight from slave station to transit state leg2
        final double tSu    = signalTimeOfFlight(QSlaveTransitLeg2PV,
                                                 transitStateLeg2.getPVCoordinates().getPosition(),
                                                 transitDateLeg2);

        // Total time of flight for leg 2
        final double t2     = tMd + tSu;


        // Compute propagation time for the 1st leg of the signal path
        // --

        // Absolute date of arrival of the signal to slave station
        final AbsoluteDate slaveStationArrivalDate = measurementDate.shiftedBy(-t2);

        // Slave station position in inertial frame at date slaveStationArrivalDate
        final Transform slaveTopoToInertArrivalDate =
                        slaveGroundStation.getOffsetToInertial(state.getFrame(), slaveStationArrivalDate);
        final TimeStampedPVCoordinates slaveRebound =
                        slaveTopoToInertArrivalDate.transformPVCoordinates(new TimeStampedPVCoordinates(slaveStationArrivalDate,
                                                                                                        PVCoordinates.ZERO));

        // Dowlink time of flight from transitStateLeg1 to slave station at slaveStationArrivalDate
        final double tSd = signalTimeOfFlight(transitStateLeg2.getPVCoordinates(), slaveRebound.getPosition(), slaveStationArrivalDate);

        // Transit state from which the satellite reflected the signal from master to slave station
        final SpacecraftState transitStateLeg1  = state.shiftedBy(delta -tMd -tSu -tSd);
        final AbsoluteDate    transitDateLeg1   = transitStateLeg1.getDate();

        // Master station PV at transit state date of leg1
        final Transform masterTopoToInertTransitLeg1 =
                        masterGroundStation.getOffsetToInertial(state.getFrame(), transitDateLeg1);
        final TimeStampedPVCoordinates QMasterTransitLeg1PV = masterTopoToInertTransitLeg1.
                        transformPVCoordinates(new TimeStampedPVCoordinates(transitDateLeg1, PVCoordinates.ZERO));

        // Uplink time of flight from master station to transit state leg1
        final double tMu = signalTimeOfFlight(QMasterTransitLeg1PV,
                                              transitStateLeg1.getPVCoordinates().getPosition(),
                                              transitDateLeg1);
        final AbsoluteDate emissionDate = transitDateLeg1.shiftedBy(-tMu);
        final TimeStampedPVCoordinates masterDeparture =
                        masterTopoToInertTransitLeg1.shiftedBy(emissionDate.durationFrom(masterTopoToInertTransitLeg1.getDate())).
                        transformPVCoordinates(new TimeStampedPVCoordinates(emissionDate, PVCoordinates.ZERO));
        // Total time of flight for leg 1
        final double t1 = tSd + tMu;

        // Prepare the evaluation & evaluate
        // --

        // The state we use to define the estimated measurement is a middle ground between the two transit states
        // This is done to avoid calling "SpacecraftState.shiftedBy" function on long duration
        // Thus we define the state at the date t" = date of arrival of the signal to the slave station
        // Or t" = t -masterTauD -slaveTauU
        // The iterative process in the estimation ensures that, after several iterations, the date stamped in the
        // state S in input of this function will be close to t"
        // Therefore we will shift state S by:
        //  - +slaveTauU to get transitStateLeg2
        //  - -slaveTauD to get transitStateLeg1
        final EstimatedMeasurement<TurnAroundRange> estimated =
                        new EstimatedMeasurement<>(this, iteration, evaluation,
                                                   new SpacecraftState[] {
                                                       transitStateLeg2.shiftedBy(-tSu)
                                                   }, new TimeStampedPVCoordinates[] {
                                                       masterDeparture,
                                                       transitStateLeg1.getPVCoordinates(),
                                                       slaveRebound,
                                                       transitStateLeg2.getPVCoordinates(),
                                                       masterArrival
                                                   });

        // Turn-around range value = Total time of flight for the 2 legs divided by 2
        final double cOver2 = 0.5 * Constants.SPEED_OF_LIGHT;
        final double tau    = t1 + t2;
        estimated.setEstimatedValue(tau * cOver2);


        // TAR derivatives w/r state
        // -------------------------

        // tMd derivatives / state
        // -----------------------

        // QMt = Master station position at tmeas = t = signal arrival at master station
        final Vector3D vel      = state.getPVCoordinates().getVelocity();
        final Transform FMt     = masterGroundStation.getOffsetToInertial(state.getFrame(), getDate());
        final PVCoordinates QMt = FMt.transformPVCoordinates(PVCoordinates.ZERO);
        final Vector3D QMt_V    = QMt.getVelocity();
        final Vector3D pos2     = transitStateLeg2.getPVCoordinates().getPosition();
        final Vector3D P2_QMt   = QMt.getPosition().subtract(pos2);
        final double   dMDown   = Constants.SPEED_OF_LIGHT * Constants.SPEED_OF_LIGHT * tMd -
                        Vector3D.dotProduct(P2_QMt, vel);

        // Derivatives w/r state
        final double dtMddPx   = -P2_QMt.getX() / dMDown;
        final double dtMddPy   = -P2_QMt.getY() / dMDown;
        final double dtMddPz   = -P2_QMt.getZ() / dMDown;

        final double dt        = delta - tMd;
        final double dtMddVx   = dtMddPx*dt;
        final double dtMddVy   = dtMddPy*dt;
        final double dtMddVz   = dtMddPz*dt;


        // tSu derivatives / state
        // -----------------------

        // QSt = slave station position at tmeas = t = signal arrival at master station
        final Transform FSt     = slaveGroundStation.getOffsetToInertial(state.getFrame(), getDate());
        final PVCoordinates QSt = FSt.transformPVCoordinates(PVCoordinates.ZERO);
        final Vector3D QSt_V    = QSt.getVelocity();

        // QSt2 = slave station position at t-t2 = signal arrival at slave station
        final Transform FSt2     = slaveGroundStation.getOffsetToInertial(state.getFrame(), getDate().shiftedBy(-t2));
        final PVCoordinates QSt2 = FSt2.transformPVCoordinates(PVCoordinates.ZERO);

        final Vector3D QSt2_P2   = pos2.subtract(QSt2.getPosition());
        final double   dSUp      = Constants.SPEED_OF_LIGHT * Constants.SPEED_OF_LIGHT * tSu -
                        Vector3D.dotProduct(QSt2_P2, QSt_V);

        // Derivatives w/r state
        final double alphaSu = 1./dSUp*QSt2_P2.dotProduct(QSt_V.subtract(vel));
        final double dtSudPx = 1./dSUp*QSt2_P2.getX() + alphaSu*dtMddPx;
        final double dtSudPy = 1./dSUp*QSt2_P2.getY() + alphaSu*dtMddPy;
        final double dtSudPz = 1./dSUp*QSt2_P2.getZ() + alphaSu*dtMddPz;
        final double dtSudVx = dtSudPx*dt;
        final double dtSudVy = dtSudPy*dt;
        final double dtSudVz = dtSudPz*dt;


        // t2 derivatives / state
        // -----------------------

        double dt2dPx = dtSudPx + dtMddPx;
        double dt2dPy = dtSudPy + dtMddPy;
        double dt2dPz = dtSudPz + dtMddPz;
        double dt2dVx = dtSudVx + dtMddVx;
        double dt2dVy = dtSudVy + dtMddVy;
        double dt2dVz = dtSudVz + dtMddVz;


        // tSd derivatives / state
        // -----------------------

        final Vector3D pos1       = transitStateLeg1.getPVCoordinates().getPosition();
        final Vector3D P1_QSt2   = QSt2.getPosition().subtract(pos1);
        final double   dSDown    = Constants.SPEED_OF_LIGHT * Constants.SPEED_OF_LIGHT * tSd -
                        Vector3D.dotProduct(P1_QSt2, vel);

        // derivatives w/r to state
        final double alphaSd = 1./dSDown*P1_QSt2.dotProduct(vel.subtract(QSt_V));
        final double dtSddPx = -1./ dSDown*P1_QSt2.getX() + alphaSd*dt2dPx;
        final double dtSddPy = -1./ dSDown*P1_QSt2.getY() + alphaSd*dt2dPy;
        final double dtSddPz = -1./ dSDown*P1_QSt2.getZ() + alphaSd*dt2dPz;

        final double dt2     = delta - t2 - tSd;
        final double dtSddVx = -dt2/ dSDown*P1_QSt2.getX()+alphaSd*dt2dVx;
        final double dtSddVy = -dt2/ dSDown*P1_QSt2.getY()+alphaSd*dt2dVy;
        final double dtSddVz = -dt2/ dSDown*P1_QSt2.getZ()+alphaSd*dt2dVz;

        // tMu derivatives / state
        // -----------------------

        // QMt1 = Master station position at t1 = t - tau = signal departure from master station
        final Transform FMt1     = masterGroundStation.getOffsetToInertial(state.getFrame(), getDate().shiftedBy(-t1-t2));
        final PVCoordinates QMt1 = FMt1.transformPVCoordinates(PVCoordinates.ZERO);

        final Vector3D QMt1_P1   = pos1.subtract(QMt1.getPosition());
        final double   dMUp      = Constants.SPEED_OF_LIGHT * Constants.SPEED_OF_LIGHT * tMu -
                        Vector3D.dotProduct(QMt1_P1, QMt_V);

        // derivatives w/r to state
        final double alphaMu = 1./dMUp*QMt1_P1.dotProduct(QMt_V.subtract(vel));
        final double dtMudPx = 1./dMUp*QMt1_P1.getX() + alphaMu*(dt2dPx+dtSddPx);
        final double dtMudPy = 1./dMUp*QMt1_P1.getY() + alphaMu*(dt2dPy+dtSddPy);
        final double dtMudPz = 1./dMUp*QMt1_P1.getZ() + alphaMu*(dt2dPz+dtSddPz);

        final double dtMudVx = dt2/dMUp*QMt1_P1.getX() + alphaMu*(dt2dVx+dtSddVx);
        final double dtMudVy = dt2/dMUp*QMt1_P1.getY() + alphaMu*(dt2dVy+dtSddVy);
        final double dtMudVz = dt2/dMUp*QMt1_P1.getZ() + alphaMu*(dt2dVz+dtSddVz);


        // t1 derivatives / state
        // t1 = tauLeg1
        // -----------------------

        // t1 = Time leg 1
        double dt1dPx = dtSddPx + dtMudPx;
        double dt1dPy = dtSddPy + dtMudPy;
        double dt1dPz = dtSddPz + dtMudPz;
        double dt1dVx = dtSddVx + dtMudVx;
        double dt1dVy = dtSddVy + dtMudVy;
        double dt1dVz = dtSddVz + dtMudVz;


        // TAR derivatives / state
        // -----------------------

        // R = TAR
        double dRdPx = (dt1dPx + dt2dPx)*cOver2;
        double dRdPy = (dt1dPy + dt2dPy)*cOver2;
        double dRdPz = (dt1dPz + dt2dPz)*cOver2;
        double dRdVx = (dt1dVx + dt2dVx)*cOver2;
        double dRdVy = (dt1dVy + dt2dVy)*cOver2;
        double dRdVz = (dt1dVz + dt2dVz)*cOver2;

        estimated.setStateDerivatives(0, new double[] {dRdPx, dRdPy, dRdPz, // dROndP
                                                    dRdVx, dRdVy, dRdVz  // dROndV
        });


        // TAR derivatives w/r stations' position in topocentric frames
        // ------------------------------------------------------------

        if (masterGroundStation.getEastOffsetDriver().isSelected()  ||
            masterGroundStation.getNorthOffsetDriver().isSelected() ||
            masterGroundStation.getZenithOffsetDriver().isSelected()||
            slaveGroundStation.getEastOffsetDriver().isSelected()  ||
            slaveGroundStation.getNorthOffsetDriver().isSelected() ||
            slaveGroundStation.getZenithOffsetDriver().isSelected()) {

            // tMd derivatives / stations
            // --------------------------

            // Master station rotation and angular speed at tmeas
            final AngularCoordinates acM = FMt.getAngular().revert();
            final Rotation rotationMasterTopoToInert = acM.getRotation();
            final Vector3D OmegaM = acM.getRotationRate();

            // Slave station rotation and angular speed at tmeas
            final AngularCoordinates acS = FSt.getAngular().revert();
            final Rotation rotationSlaveTopoToInert = acS.getRotation();
            final Vector3D OmegaS = acS.getRotationRate();

            // Master station - Inertial frame
            final double dtMddQMx_I   = P2_QMt.getX() / dMDown;
            final double dtMddQMy_I   = P2_QMt.getY() / dMDown;
            final double dtMddQMz_I   = P2_QMt.getZ() / dMDown;

            // Slave station - Inertial frame
            final double dtMddQSx_I   = 0.;
            final double dtMddQSy_I   = 0.;
            final double dtMddQSz_I   = 0.;


            // Topo frames
            final Vector3D dtMddQM = rotationMasterTopoToInert.
                            applyTo(new Vector3D(dtMddQMx_I,
                                                 dtMddQMy_I,
                                                 dtMddQMz_I));
            final Vector3D dtMddQS = rotationSlaveTopoToInert.
                            applyTo(new Vector3D(dtMddQSx_I,
                                                 dtMddQSy_I,
                                                 dtMddQSz_I));

            // tSu derivatives / stations
            // --------------------------

            // Master station - Inertial frame
            final double dtSudQMx_I   = dtMddQMx_I*alphaSu;
            final double dtSudQMy_I   = dtMddQMy_I*alphaSu;
            final double dtSudQMz_I   = dtMddQMz_I*alphaSu;

            // Slave station - Inertial frame
            final double dtSudQSx_I   = 1./dSUp
                            *QSt2_P2.dotProduct(Vector3D.MINUS_I
                                                .add(OmegaS.crossProduct(Vector3D.PLUS_I).scalarMultiply(t2)));
            final double dtSudQSy_I   = 1./dSUp
                            *QSt2_P2.dotProduct(Vector3D.MINUS_J
                                                .add(OmegaS.crossProduct(Vector3D.PLUS_J).scalarMultiply(t2)));
            final double dtSudQSz_I   = 1./dSUp
                            *QSt2_P2.dotProduct(Vector3D.MINUS_K
                                                .add(OmegaS.crossProduct(Vector3D.PLUS_K).scalarMultiply(t2)));

            // Topo frames
            final Vector3D dtSudQM = rotationMasterTopoToInert.
                            applyTo(new Vector3D(dtSudQMx_I,
                                                 dtSudQMy_I,
                                                 dtSudQMz_I));
            final Vector3D dtSudQS = rotationSlaveTopoToInert.
                            applyTo(new Vector3D(dtSudQSx_I,
                                                 dtSudQSy_I,
                                                 dtSudQSz_I));

            // t2 = tauLeg2 derivatives / stations
            // --------------------------

            final double dt2dQSx_I = dtMddQSx_I + dtSudQSx_I;
            final double dt2dQSy_I = dtMddQSy_I + dtSudQSy_I;
            final double dt2dQSz_I = dtMddQSz_I + dtSudQSz_I;
            final double dt2dQMx_I = dtMddQMx_I + dtSudQMx_I;
            final double dt2dQMy_I = dtMddQMy_I + dtSudQMy_I;
            final double dt2dQMz_I = dtMddQMz_I + dtSudQMz_I;

            final Vector3D dt2dQM = dtSudQM.add(dtMddQM);
            final Vector3D dt2dQS = dtSudQS.add(dtMddQS);


            // tSd derivatives / stations
            // --------------------------

            // Master station - Inertial frame
            final double dtSddQMx_I   = dt2dQMx_I*alphaSd;
            final double dtSddQMy_I   = dt2dQMy_I*alphaSd;
            final double dtSddQMz_I   = dt2dQMz_I*alphaSd;

            // Slave station - Inertial frame
            final double dtSddQSx_I   = dt2dQSx_I*alphaSd + 1./dSDown
                            *P1_QSt2.dotProduct(Vector3D.PLUS_I
                                                .subtract(OmegaS.crossProduct(Vector3D.PLUS_I).scalarMultiply(t2)));
            final double dtSddQSy_I   = dt2dQSy_I*alphaSd + 1./dSDown
                            *P1_QSt2.dotProduct(Vector3D.PLUS_J
                                                .subtract(OmegaS.crossProduct(Vector3D.PLUS_J).scalarMultiply(t2)));

            final double dtSddQSz_I   = dt2dQSz_I*alphaSd + 1./dSDown
                            *P1_QSt2.dotProduct(Vector3D.PLUS_K
                                                .subtract(OmegaS.crossProduct(Vector3D.PLUS_K).scalarMultiply(t2)));

            // Topo frames
            final Vector3D dtSddQM = rotationMasterTopoToInert.
                            applyTo(new Vector3D(dtSddQMx_I,
                                                 dtSddQMy_I,
                                                 dtSddQMz_I));
            final Vector3D dtSddQS = rotationSlaveTopoToInert.
                            applyTo(new Vector3D(dtSddQSx_I,
                                                 dtSddQSy_I,
                                                 dtSddQSz_I));

            // tMu derivatives / stations
            // --------------------------

            // Master station - Inertial frame
            final double dtMudQMx_I = alphaMu*(dt2dQMx_I+dtSddQMx_I) + 1/dMUp*
                            QMt1_P1.dotProduct(Vector3D.MINUS_I
                                               .add(OmegaM.crossProduct(Vector3D.PLUS_I).scalarMultiply(tau)));
            final double dtMudQMy_I = alphaMu*(dt2dQMy_I+dtSddQMy_I) + 1/dMUp*
                            QMt1_P1.dotProduct(Vector3D.MINUS_J
                                               .add(OmegaM.crossProduct(Vector3D.PLUS_J).scalarMultiply(tau)));
            final double dtMudQMz_I = alphaMu*(dt2dQMz_I+dtSddQMz_I) + 1/dMUp*
                            QMt1_P1.dotProduct(Vector3D.MINUS_K
                                               .add(OmegaM.crossProduct(Vector3D.PLUS_K).scalarMultiply(tau)));

            // Slave station - Inertial frame
            final double dtMudQSx_I = alphaMu*(dt2dQSx_I+dtSddQSx_I);
            final double dtMudQSy_I = alphaMu*(dt2dQSy_I+dtSddQSy_I);
            final double dtMudQSz_I = alphaMu*(dt2dQSz_I+dtSddQSz_I);


            // Topo frames
            final Vector3D dtMudQM = rotationMasterTopoToInert.
                            applyTo(new Vector3D(dtMudQMx_I,
                                                 dtMudQMy_I,
                                                 dtMudQMz_I));
            final Vector3D dtMudQS = rotationSlaveTopoToInert.
                            applyTo(new Vector3D(dtMudQSx_I,
                                                 dtMudQSy_I,
                                                 dtMudQSz_I));

            // t1 derivatives / stations
            // --------------------------

            final Vector3D dt1dQM = dtMudQM.add(dtSddQM);
            final Vector3D dt1dQS = dtMudQS.add(dtSddQS);

            // TAR derivatives / stations
            // --------------------------

            final Vector3D dRdQM = (dt1dQM.add(dt2dQM)).scalarMultiply(cOver2);
            final Vector3D dRdQS = (dt1dQS.add(dt2dQS)).scalarMultiply(cOver2);

            // Master station drivers
            if (masterGroundStation.getEastOffsetDriver().isSelected()) {
                estimated.setParameterDerivatives(masterGroundStation.getEastOffsetDriver(), dRdQM.getX());
            }
            if (masterGroundStation.getNorthOffsetDriver().isSelected()) {
                estimated.setParameterDerivatives(masterGroundStation.getNorthOffsetDriver(), dRdQM.getY());
            }
            if (masterGroundStation.getZenithOffsetDriver().isSelected()) {
                estimated.setParameterDerivatives(masterGroundStation.getZenithOffsetDriver(), dRdQM.getZ());
            }

            // Slave station drivers
            if (slaveGroundStation.getEastOffsetDriver().isSelected()) {
                estimated.setParameterDerivatives(slaveGroundStation.getEastOffsetDriver(), dRdQS.getX());
            }
            if (slaveGroundStation.getNorthOffsetDriver().isSelected()) {
                estimated.setParameterDerivatives(slaveGroundStation.getNorthOffsetDriver(), dRdQS.getY());
            }
            if (slaveGroundStation.getZenithOffsetDriver().isSelected()) {
                estimated.setParameterDerivatives(slaveGroundStation.getZenithOffsetDriver(), dRdQS.getZ());
            }
        }

        return estimated;

    }



    /**
     * Added for validation
     * @param iteration
     * @param evaluation
     * @param state
     * @return
     */
    protected EstimatedMeasurement<TurnAroundRange> theoreticalEvaluationValidation(final int iteration, final int evaluation,
                                                                                    final SpacecraftState state) {
        // Stations & DSFactory attributes from parent TurnArounsRange class
        final GroundStation masterGroundStation       = getMasterStation();
        final GroundStation slaveGroundStation        = getSlaveStation();
        int nbParams = 6;
        final Map<String, Integer> indices = new HashMap<>();
        for (ParameterDriver driver : getParametersDrivers()) {
            // we have to check for duplicate keys because master and slave station share
            // pole and prime meridian parameters names that must be considered
            // as one set only (they are combined together by the estimation engine)
            if (driver.isSelected() && !indices.containsKey(driver.getName())) {
                indices.put(driver.getName(), nbParams++);
            }
        }
        final Field<Gradient>         field     = GradientField.getField(nbParams);
        final FieldVector3D<Gradient> zero      = FieldVector3D.getZero(field);

        // Coordinates of the spacecraft expressed as a gradient
        final TimeStampedFieldPVCoordinates<Gradient> pvaDS = getCoordinates(state, 0, nbParams);

        // The path of the signal is divided in two legs.
        // Leg1: Emission from master station to satellite in masterTauU seconds
        //     + Reflection from satellite to slave station in slaveTauD seconds
        // Leg2: Reflection from slave station to satellite in slaveTauU seconds
        //     + Reflection from satellite to master station in masterTaudD seconds
        // The measurement is considered to be time stamped at reception on ground
        // by the master station. All times are therefore computed as backward offsets
        // with respect to this reception time.
        //
        // Two intermediate spacecraft states are defined:
        //  - transitStateLeg2: State of the satellite when it bounced back the signal
        //                      from slave station to master station during the 2nd leg
        //  - transitStateLeg1: State of the satellite when it bounced back the signal
        //                      from master station to slave station during the 1st leg

        // Compute propagation time for the 2nd leg of the signal path
        // --

        // Time difference between t (date of the measurement) and t' (date tagged in spacecraft state)
        // (if state has already been set up to pre-compensate propagation delay,
        // we will have delta = masterTauD + slaveTauU)
        final AbsoluteDate measurementDate = getDate();
        final FieldAbsoluteDate<Gradient> measurementDateDS = new FieldAbsoluteDate<>(field, measurementDate);
        final double delta = measurementDate.durationFrom(state.getDate());

        // transform between master station topocentric frame (east-north-zenith) and inertial frame expressed as gradients
        // The components of master station's position in offset frame are the 3 third derivative parameters
        final FieldTransform<Gradient> masterToInert =
                        masterGroundStation.getOffsetToInertial(state.getFrame(), measurementDate, nbParams, indices);

        // Master station PV in inertial frame at measurement date
        final FieldVector3D<Gradient> QMaster = masterToInert.transformPosition(zero);

        // Compute propagation times
        final Gradient masterTauD = signalTimeOfFlight(pvaDS, QMaster, measurementDateDS);

        // Elapsed time between state date t' and signal arrival to the transit state of the 2nd leg
        final Gradient dtLeg2 = masterTauD.negate().add(delta);

        // Transit state where the satellite reflected the signal from slave to master station
        final SpacecraftState transitStateLeg2 = state.shiftedBy(dtLeg2.getValue());

        // Transit state pv of leg2 (re)computed with gradients
        final TimeStampedFieldPVCoordinates<Gradient> transitStateLeg2PV = pvaDS.shiftedBy(dtLeg2);

        // transform between slave station topocentric frame (east-north-zenith) and inertial frame expressed as gradients
        // The components of slave station's position in offset frame are the 3 last derivative parameters
        final FieldAbsoluteDate<Gradient> approxReboundDate = measurementDateDS.shiftedBy(-delta);
        final FieldTransform<Gradient> slaveToInertApprox =
                        slaveGroundStation.getOffsetToInertial(state.getFrame(), approxReboundDate, nbParams, indices);

        // Slave station PV in inertial frame at approximate rebound date on slave station
        final TimeStampedFieldPVCoordinates<Gradient> QSlaveApprox =
                        slaveToInertApprox.transformPVCoordinates(new TimeStampedFieldPVCoordinates<>(approxReboundDate,
                                                                                                      zero, zero, zero));

        // Uplink time of flight from slave station to transit state of leg2
        final Gradient slaveTauU =
                        signalTimeOfFlight(QSlaveApprox,
                                           transitStateLeg2PV.getPosition(),
                                           transitStateLeg2PV.getDate());

        // Total time of flight for leg 2
        final Gradient tauLeg2 = masterTauD.add(slaveTauU);

        // Compute propagation time for the 1st leg of the signal path
        // --

        // Absolute date of rebound of the signal to slave station
        final FieldAbsoluteDate<Gradient> reboundDateDS = measurementDateDS.shiftedBy(tauLeg2.negate());
        final FieldTransform<Gradient> slaveToInert =
                        slaveGroundStation.getOffsetToInertial(state.getFrame(), reboundDateDS, nbParams, indices);

        // Slave station PV in inertial frame at rebound date on slave station
        final FieldVector3D<Gradient> QSlave = slaveToInert.transformPosition(zero);

        // Downlink time of flight from transitStateLeg1 to slave station at rebound date
        final Gradient slaveTauD = signalTimeOfFlight(transitStateLeg2PV, QSlave, reboundDateDS);


        // Elapsed time between state date t' and signal arrival to the transit state of the 1st leg
        final Gradient dtLeg1 = dtLeg2.subtract(slaveTauU).subtract(slaveTauD);

        // Transit state pv of leg2 (re)computed with derivative structures
        final TimeStampedFieldPVCoordinates<Gradient> transitStateLeg1PV = pvaDS.shiftedBy(dtLeg1);

        // transform between master station topocentric frame (east-north-zenith) and inertial frame expressed as gradients
        // The components of master station's position in offset frame are the 3 third derivative parameters
        final FieldAbsoluteDate<Gradient> approxEmissionDate =
                        measurementDateDS.shiftedBy(-2 * (slaveTauU.getValue() + masterTauD.getValue()));
        final FieldTransform<Gradient> masterToInertApprox =
                        masterGroundStation.getOffsetToInertial(state.getFrame(), approxEmissionDate, nbParams, indices);

        // Master station PV in inertial frame at approximate emission date
        final TimeStampedFieldPVCoordinates<Gradient> QMasterApprox =
                        masterToInertApprox.transformPVCoordinates(new TimeStampedFieldPVCoordinates<>(approxEmissionDate,
                                                                                                       zero, zero, zero));

        // Uplink time of flight from master station to transit state of leg1
        final Gradient masterTauU = signalTimeOfFlight(QMasterApprox,
                                                                  transitStateLeg1PV.getPosition(),
                                                                  transitStateLeg1PV.getDate());

        // Total time of flight for leg 1
        final Gradient tauLeg1 = slaveTauD.add(masterTauU);


        // --
        // Evaluate the turn-around range value and its derivatives
        // --------------------------------------------------------

        // The state we use to define the estimated measurement is a middle ground between the two transit states
        // This is done to avoid calling "SpacecraftState.shiftedBy" function on long duration
        // Thus we define the state at the date t" = date of rebound of the signal at the slave station
        // Or t" = t -masterTauD -slaveTauU
        // The iterative process in the estimation ensures that, after several iterations, the date stamped in the
        // state S in input of this function will be close to t"
        // Therefore we will shift state S by:
        //  - +slaveTauU to get transitStateLeg2
        //  - -slaveTauD to get transitStateLeg1
        final EstimatedMeasurement<TurnAroundRange> estimated =
                        new EstimatedMeasurement<>(this, iteration, evaluation,
                                                   new SpacecraftState[] {
                                                       transitStateLeg2.shiftedBy(-slaveTauU.getValue())
                                                   }, null);

        // Turn-around range value = Total time of flight for the 2 legs divided by 2 and multiplied by c
        final double cOver2 = 0.5 * Constants.SPEED_OF_LIGHT;
        final Gradient turnAroundRange = (tauLeg2.add(tauLeg1)).multiply(cOver2);
        estimated.setEstimatedValue(turnAroundRange.getValue());


        // Turn-around range partial derivatives with respect to state
        final double[] derivatives = turnAroundRange.getGradient();
        estimated.setStateDerivatives(0, Arrays.copyOfRange(derivatives, 0, 6));

        // set partial derivatives with respect to parameters
        // (beware element at index 0 is the value, not a derivative)
        for (final ParameterDriver driver : getParametersDrivers()) {
            final Integer index = indices.get(driver.getName());
            if (index != null) {
                estimated.setParameterDerivatives(driver, derivatives[index]);
            }
        }

        // ----------
        // VALIDATION: Using analytical version to compare
        //-----------

        // Computation of the value without Gradients
        // ----------------------------------

        // Time difference between t (date of the measurement) and t' (date tagged in spacecraft state)
        // (if state has already been set up to pre-compensate propagation delay,
        // we will have delta = masterTauD + slaveTauU)

        // Master station PV at measurement date
        final Transform masterTopoToInert =
                        masterGroundStation.getOffsetToInertial(state.getFrame(), measurementDate);
        final TimeStampedPVCoordinates QMt = masterTopoToInert.
                        transformPVCoordinates(new TimeStampedPVCoordinates(measurementDate, PVCoordinates.ZERO));

        // Slave station PV at measurement date
        final Transform slaveTopoToInert =
                        slaveGroundStation.getOffsetToInertial(state.getFrame(), measurementDate);
        final TimeStampedPVCoordinates QSt = slaveTopoToInert.
                        transformPVCoordinates(new TimeStampedPVCoordinates(measurementDate, PVCoordinates.ZERO));

        // Downlink time of flight from master station at t to spacecraft at t'
        final double tMd    = signalTimeOfFlight(state.getPVCoordinates(), QMt.getPosition(), measurementDate);

        // Transit state from which the satellite reflected the signal from slave to master station
        final SpacecraftState state2            = state.shiftedBy(delta - tMd);
        final AbsoluteDate    transitDateLeg2   = transitStateLeg2.getDate();

        // Slave station PV at transit state leg2 date
        final Transform slaveTopoToInertTransitLeg2 =
                      slaveGroundStation.getOffsetToInertial(state.getFrame(), transitDateLeg2);
        final TimeStampedPVCoordinates QSdate2PV = slaveTopoToInertTransitLeg2.
                      transformPVCoordinates(new TimeStampedPVCoordinates(transitDateLeg2, PVCoordinates.ZERO));

        // Uplink time of flight from slave station to transit state leg2
        final double tSu    = signalTimeOfFlight(QSdate2PV,
                                                 state2.getPVCoordinates().getPosition(),
                                                 transitDateLeg2);

        // Total time of flight for leg 2
        final double t2     = tMd + tSu;


        // Compute propagation time for the 1st leg of the signal path
        // --

        // Absolute date of arrival of the signal to slave station
        final AbsoluteDate    tQSA = measurementDate.shiftedBy(-t2);

        // Slave station position in inertial frame at date tQSA
        final Transform slaveTopoToInertArrivalDate =
                        slaveGroundStation.getOffsetToInertial(state.getFrame(), tQSA);
        final Vector3D QSA = slaveTopoToInertArrivalDate.transformPosition(Vector3D.ZERO);

        // Dowlink time of flight from transitStateLeg1 to slave station at slaveStationArrivalDate
        final double tSd = signalTimeOfFlight(state2.getPVCoordinates(), QSA, tQSA);


        // Transit state from which the satellite reflected the signal from master to slave station
        final SpacecraftState state1            = state.shiftedBy(delta -tMd -tSu -tSd);
        final AbsoluteDate    transitDateLeg1   = transitStateLeg1PV.getDate().toAbsoluteDate();

        // Master station PV at transit state date of leg1
        final Transform masterTopoToInertTransitLeg1 =
                      masterGroundStation.getOffsetToInertial(state.getFrame(), transitDateLeg1);
        final TimeStampedPVCoordinates QMdate1PV = masterTopoToInertTransitLeg1.
                      transformPVCoordinates(new TimeStampedPVCoordinates(transitDateLeg1, PVCoordinates.ZERO));

        // Uplink time of flight from master station to transit state leg1
        final double tMu = signalTimeOfFlight(QMdate1PV,
                                              state1.getPVCoordinates().getPosition(),
                                              transitDateLeg1);

        // Total time of flight for leg 1
        final double          t1           = tSd + tMu;

        // Total time of flight
        final double          t = t1+t2;

        // Turn-around range value
        final double          TAR = t*cOver2;

        // Diff with DS
        final double dTAR = turnAroundRange.getValue() - TAR;

        // tMd derivatives / state
        // -----------------------

        // QMt_PV = Master station PV at tmeas = t = signal arrival at master station
        final Vector3D vel         = state.getPVCoordinates().getVelocity();
        final PVCoordinates QMt_PV = masterTopoToInert.transformPVCoordinates(PVCoordinates.ZERO);
        final Vector3D QMt_V       = QMt_PV.getVelocity();
        final Vector3D pos2        = state2.getPVCoordinates().getPosition();
        final Vector3D P2_QMt      = QMt_PV.getPosition().subtract(pos2);
        final double   dMDown      = Constants.SPEED_OF_LIGHT * Constants.SPEED_OF_LIGHT * tMd -
                        Vector3D.dotProduct(P2_QMt, vel);

        // derivatives of the downlink time of flight
        final double dtMddPx   = -P2_QMt.getX() / dMDown;
        final double dtMddPy   = -P2_QMt.getY() / dMDown;
        final double dtMddPz   = -P2_QMt.getZ() / dMDown;

        final double dt     = delta - tMd;
        final double dtMddVx   = dtMddPx*dt;
        final double dtMddVy   = dtMddPy*dt;
        final double dtMddVz   = dtMddPz*dt;

        // From the DS
        final double dtMddPxDS = masterTauD.getPartialDerivative(0);
        final double dtMddPyDS = masterTauD.getPartialDerivative(1);
        final double dtMddPzDS = masterTauD.getPartialDerivative(2);
        final double dtMddVxDS = masterTauD.getPartialDerivative(3);
        final double dtMddVyDS = masterTauD.getPartialDerivative(4);
        final double dtMddVzDS = masterTauD.getPartialDerivative(5);

        // Difference
        final double d_dtMddPx = dtMddPxDS-dtMddPx;
        final double d_dtMddPy = dtMddPyDS-dtMddPy;
        final double d_dtMddPz = dtMddPzDS-dtMddPz;
        final double d_dtMddVx = dtMddVxDS-dtMddVx;
        final double d_dtMddVy = dtMddVyDS-dtMddVy;
        final double d_dtMddVz = dtMddVzDS-dtMddVz;


        // tSu derivatives / state
        // -----------------------

        // QSt = slave station PV at tmeas = t = signal arrival at master station
//        final Transform FSt     = slaveStation.getOffsetFrame().getTransformTo(state.getFrame(), measurementDate);
//        final PVCoordinates QSt = FSt.transformPVCoordinates(PVCoordinates.ZERO);
        final Vector3D QSt_V    = QSt.getVelocity();

        // QSt2 = slave station PV at t-t2 = signal arrival at slave station
        final PVCoordinates QSt2 = slaveTopoToInertArrivalDate.transformPVCoordinates(PVCoordinates.ZERO);

        final Vector3D QSt2_P2   = pos2.subtract(QSt2.getPosition());
        final double   dSUp    = Constants.SPEED_OF_LIGHT * Constants.SPEED_OF_LIGHT * tSu -
                        Vector3D.dotProduct(QSt2_P2, QSt_V);

        final double alphaSu = 1./dSUp*QSt2_P2.dotProduct(QSt_V.subtract(vel));

        final double dtSudPx = 1./dSUp*QSt2_P2.getX() + alphaSu*dtMddPx;
        final double dtSudPy = 1./dSUp*QSt2_P2.getY() + alphaSu*dtMddPy;
        final double dtSudPz = 1./dSUp*QSt2_P2.getZ() + alphaSu*dtMddPz;

        final double dtSudVx   = dtSudPx*dt;
        final double dtSudVy   = dtSudPy*dt;
        final double dtSudVz   = dtSudPz*dt;


        // From the DS
        final double dtSudPxDS = slaveTauU.getPartialDerivative(0);
        final double dtSudPyDS = slaveTauU.getPartialDerivative(1);
        final double dtSudPzDS = slaveTauU.getPartialDerivative(2);
        final double dtSudVxDS = slaveTauU.getPartialDerivative(3);
        final double dtSudVyDS = slaveTauU.getPartialDerivative(4);
        final double dtSudVzDS = slaveTauU.getPartialDerivative(5);

        // Difference
        final double d_dtSudPx = dtSudPxDS-dtSudPx;
        final double d_dtSudPy = dtSudPyDS-dtSudPy;
        final double d_dtSudPz = dtSudPzDS-dtSudPz;
        final double d_dtSudVx = dtSudVxDS-dtSudVx;
        final double d_dtSudVy = dtSudVyDS-dtSudVy;
        final double d_dtSudVz = dtSudVzDS-dtSudVz;


        // t2 derivatives / state
        // -----------------------

        // t2 = Time leg 2
        double dt2dPx = dtSudPx + dtMddPx;
        double dt2dPy = dtSudPy + dtMddPy;
        double dt2dPz = dtSudPz + dtMddPz;
        double dt2dVx = dtSudVx + dtMddVx;
        double dt2dVy = dtSudVy + dtMddVy;
        double dt2dVz = dtSudVz + dtMddVz;

        // With DS
        double dt2dPxDS = tauLeg2.getPartialDerivative(0);
        double dt2dPyDS = tauLeg2.getPartialDerivative(1);
        double dt2dPzDS = tauLeg2.getPartialDerivative(2);
        double dt2dVxDS = tauLeg2.getPartialDerivative(3);
        double dt2dVyDS = tauLeg2.getPartialDerivative(4);
        double dt2dVzDS = tauLeg2.getPartialDerivative(5);

        // Diff
        final double d_dt2dPx = dt2dPxDS-dt2dPx;
        final double d_dt2dPy = dt2dPyDS-dt2dPy;
        final double d_dt2dPz = dt2dPzDS-dt2dPz;
        final double d_dt2dVx = dt2dVxDS-dt2dVx;
        final double d_dt2dVy = dt2dVyDS-dt2dVy;
        final double d_dt2dVz = dt2dVzDS-dt2dVz;


        // tSd derivatives / state
        // -----------------------

        final Vector3D pos1       = state1.getPVCoordinates().getPosition();
        final Vector3D P1_QSt2   = QSt2.getPosition().subtract(pos1);
        final double   dSDown    = Constants.SPEED_OF_LIGHT * Constants.SPEED_OF_LIGHT * tSd -
                        Vector3D.dotProduct(P1_QSt2, vel);

        // derivatives w/r to state
        final double alphaSd = 1./dSDown*P1_QSt2.dotProduct(vel.subtract(QSt_V));
        final double dtSddPx   = -1./ dSDown*P1_QSt2.getX() + alphaSd*dt2dPx;
        final double dtSddPy   = -1./ dSDown*P1_QSt2.getY() + alphaSd*dt2dPy;
        final double dtSddPz   = -1./ dSDown*P1_QSt2.getZ() + alphaSd*dt2dPz;

        final double dt2     = delta - t2 - tSd;
        final double dtSddVx   = -dt2/ dSDown*P1_QSt2.getX()+alphaSd*dt2dVx;
        final double dtSddVy   = -dt2/ dSDown*P1_QSt2.getY()+alphaSd*dt2dVy;
        final double dtSddVz   = -dt2/ dSDown*P1_QSt2.getZ()+alphaSd*dt2dVz;

        // From the DS
        final double dtSddPxDS = slaveTauD.getPartialDerivative(0);
        final double dtSddPyDS = slaveTauD.getPartialDerivative(1);
        final double dtSddPzDS = slaveTauD.getPartialDerivative(2);
        final double dtSddVxDS = slaveTauD.getPartialDerivative(3);
        final double dtSddVyDS = slaveTauD.getPartialDerivative(4);
        final double dtSddVzDS = slaveTauD.getPartialDerivative(5);

        // Difference
        final double d_dtSddPx = dtSddPxDS-dtSddPx;
        final double d_dtSddPy = dtSddPyDS-dtSddPy;
        final double d_dtSddPz = dtSddPzDS-dtSddPz;
        final double d_dtSddVx = dtSddVxDS-dtSddVx;
        final double d_dtSddVy = dtSddVyDS-dtSddVy;
        final double d_dtSddVz = dtSddVzDS-dtSddVz;


        // tMu derivatives / state
        // -----------------------

        // QMt1 = Master station position at t1 = t - tau = signal departure from master station
        final Transform FMt1     = masterGroundStation.getOffsetToInertial(state.getFrame(), measurementDate.shiftedBy(-t1-t2));
        final PVCoordinates QMt1 = FMt1.transformPVCoordinates(PVCoordinates.ZERO);

        final Vector3D QMt1_P1   = pos1.subtract(QMt1.getPosition());
        final double   dMUp    = Constants.SPEED_OF_LIGHT * Constants.SPEED_OF_LIGHT * tMu -
                        Vector3D.dotProduct(QMt1_P1, QMt_V);

        // derivatives w/r to state
        final double alphaMu = 1./dMUp*QMt1_P1.dotProduct(QMt_V.subtract(vel));
        final double dtMudPx = 1./dMUp*QMt1_P1.getX() + alphaMu*(dt2dPx+dtSddPx);
        final double dtMudPy = 1./dMUp*QMt1_P1.getY() + alphaMu*(dt2dPy+dtSddPy);
        final double dtMudPz = 1./dMUp*QMt1_P1.getZ() + alphaMu*(dt2dPz+dtSddPz);

        final double dtMudVx = dt2/dMUp*QMt1_P1.getX() + alphaMu*(dt2dVx+dtSddVx);
        final double dtMudVy = dt2/dMUp*QMt1_P1.getY() + alphaMu*(dt2dVy+dtSddVy);
        final double dtMudVz = dt2/dMUp*QMt1_P1.getZ() + alphaMu*(dt2dVz+dtSddVz);

        // From the DS
        final double dtMudPxDS = masterTauU.getPartialDerivative(0);
        final double dtMudPyDS = masterTauU.getPartialDerivative(1);
        final double dtMudPzDS = masterTauU.getPartialDerivative(2);
        final double dtMudVxDS = masterTauU.getPartialDerivative(3);
        final double dtMudVyDS = masterTauU.getPartialDerivative(4);
        final double dtMudVzDS = masterTauU.getPartialDerivative(5);

        // Difference
        final double d_dtMudPx = dtMudPxDS-dtMudPx;
        final double d_dtMudPy = dtMudPyDS-dtMudPy;
        final double d_dtMudPz = dtMudPzDS-dtMudPz;
        final double d_dtMudVx = dtMudVxDS-dtMudVx;
        final double d_dtMudVy = dtMudVyDS-dtMudVy;
        final double d_dtMudVz = dtMudVzDS-dtMudVz;


        // t1 derivatives / state
        // -----------------------

        // t1 = Time leg 1
        double dt1dPx = dtSddPx + dtMudPx;
        double dt1dPy = dtSddPy + dtMudPy;
        double dt1dPz = dtSddPz + dtMudPz;
        double dt1dVx = dtSddVx + dtMudVx;
        double dt1dVy = dtSddVy + dtMudVy;
        double dt1dVz = dtSddVz + dtMudVz;

        // With DS
        double dt1dPxDS = tauLeg1.getPartialDerivative(0);
        double dt1dPyDS = tauLeg1.getPartialDerivative(1);
        double dt1dPzDS = tauLeg1.getPartialDerivative(2);
        double dt1dVxDS = tauLeg1.getPartialDerivative(3);
        double dt1dVyDS = tauLeg1.getPartialDerivative(4);
        double dt1dVzDS = tauLeg1.getPartialDerivative(5);

        // Diff
        final double d_dt1dPx = dt1dPxDS-dt1dPx;
        final double d_dt1dPy = dt1dPyDS-dt1dPy;
        final double d_dt1dPz = dt1dPzDS-dt1dPz;
        final double d_dt1dVx = dt1dVxDS-dt1dVx;
        final double d_dt1dVy = dt1dVyDS-dt1dVy;
        final double d_dt1dVz = dt1dVzDS-dt1dVz;


        // TAR derivatives / state
        // -----------------------

        // R = TAR
        double dRdPx = (dt1dPx + dt2dPx)*cOver2;
        double dRdPy = (dt1dPy + dt2dPy)*cOver2;
        double dRdPz = (dt1dPz + dt2dPz)*cOver2;
        double dRdVx = (dt1dVx + dt2dVx)*cOver2;
        double dRdVy = (dt1dVy + dt2dVy)*cOver2;
        double dRdVz = (dt1dVz + dt2dVz)*cOver2;

        // With DS
        double dRdPxDS = turnAroundRange.getPartialDerivative(0);
        double dRdPyDS = turnAroundRange.getPartialDerivative(1);
        double dRdPzDS = turnAroundRange.getPartialDerivative(2);
        double dRdVxDS = turnAroundRange.getPartialDerivative(3);
        double dRdVyDS = turnAroundRange.getPartialDerivative(4);
        double dRdVzDS = turnAroundRange.getPartialDerivative(5);

        // Diff
        final double d_dRdPx = dRdPxDS-dRdPx;
        final double d_dRdPy = dRdPyDS-dRdPy;
        final double d_dRdPz = dRdPzDS-dRdPz;
        final double d_dRdVx = dRdVxDS-dRdVx;
        final double d_dRdVy = dRdVyDS-dRdVy;
        final double d_dRdVz = dRdVzDS-dRdVz;


        // tMd derivatives / stations
        // --------------------------

        // Master station rotation and angular speed at tmeas
        final AngularCoordinates acM = masterTopoToInert.getAngular().revert();
        final Rotation rotationMasterTopoToInert = acM.getRotation();
        final Vector3D OmegaM = acM.getRotationRate();

        // Slave station rotation and angular speed at tmeas
        final AngularCoordinates acS = slaveTopoToInert.getAngular().revert();
        final Rotation rotationSlaveTopoToInert = acS.getRotation();
        final Vector3D OmegaS = acS.getRotationRate();

        // Master station - Inertial frame
        final double dtMddQMx_I   = P2_QMt.getX() / dMDown;
        final double dtMddQMy_I   = P2_QMt.getY() / dMDown;
        final double dtMddQMz_I   = P2_QMt.getZ() / dMDown;

        // Slave station - Inertial frame
        final double dtMddQSx_I   = 0.;
        final double dtMddQSy_I   = 0.;
        final double dtMddQSz_I   = 0.;


        // Topo frames
        final Vector3D dtMddQM = rotationMasterTopoToInert.
                        applyTo(new Vector3D(dtMddQMx_I,
                                             dtMddQMy_I,
                                             dtMddQMz_I));
        final Vector3D dtMddQS = rotationSlaveTopoToInert.
                        applyTo(new Vector3D(dtMddQSx_I,
                                             dtMddQSy_I,
                                             dtMddQSz_I));

        // With DS
        double dtMddQMx_DS = masterTauD.getPartialDerivative(6);
        double dtMddQMy_DS = masterTauD.getPartialDerivative(7);
        double dtMddQMz_DS = masterTauD.getPartialDerivative(8);

        double dtMddQSx_DS = masterTauD.getPartialDerivative(9);
        double dtMddQSy_DS = masterTauD.getPartialDerivative(10);
        double dtMddQSz_DS = masterTauD.getPartialDerivative(11);


        // Diff
        final double d_dtMddQMx = dtMddQMx_DS-dtMddQM.getX();
        final double d_dtMddQMy = dtMddQMy_DS-dtMddQM.getY();
        final double d_dtMddQMz = dtMddQMz_DS-dtMddQM.getZ();

        final double d_dtMddQSx = dtMddQSx_DS-dtMddQS.getX();
        final double d_dtMddQSy = dtMddQSy_DS-dtMddQS.getY();
        final double d_dtMddQSz = dtMddQSz_DS-dtMddQS.getZ();



        // tSu derivatives / stations
        // --------------------------

        // Master station - Inertial frame
        final double dtSudQMx_I   = dtMddQMx_I*alphaSu;
        final double dtSudQMy_I   = dtMddQMy_I*alphaSu;
        final double dtSudQMz_I   = dtMddQMz_I*alphaSu;

        // Slave station - Inertial frame
        final double dtSudQSx_I   = 1./dSUp*QSt2_P2
                        .dotProduct(Vector3D.MINUS_I
                                    .add(OmegaS.crossProduct(Vector3D.PLUS_I).scalarMultiply(t2)));
        final double dtSudQSy_I   = 1./dSUp*QSt2_P2
                        .dotProduct(Vector3D.MINUS_J
                                    .add(OmegaS.crossProduct(Vector3D.PLUS_J).scalarMultiply(t2)));
        final double dtSudQSz_I   = 1./dSUp*QSt2_P2
                        .dotProduct(Vector3D.MINUS_K
                                    .add(OmegaS.crossProduct(Vector3D.PLUS_K).scalarMultiply(t2)));

        // Topo frames
        final Vector3D dtSudQM = rotationMasterTopoToInert.
                        applyTo(new Vector3D(dtSudQMx_I,
                                             dtSudQMy_I,
                                             dtSudQMz_I));
        final Vector3D dtSudQS = rotationSlaveTopoToInert.
                        applyTo(new Vector3D(dtSudQSx_I,
                                             dtSudQSy_I,
                                             dtSudQSz_I));
        // With DS
        double dtSudQMx_DS = slaveTauU.getPartialDerivative(6);
        double dtSudQMy_DS = slaveTauU.getPartialDerivative(7);
        double dtSudQMz_DS = slaveTauU.getPartialDerivative(8);

        double dtSudQSx_DS = slaveTauU.getPartialDerivative(9);
        double dtSudQSy_DS = slaveTauU.getPartialDerivative(10);
        double dtSudQSz_DS = slaveTauU.getPartialDerivative(11);


        // Diff
        final double d_dtSudQMx = dtSudQMx_DS-dtSudQM.getX();
        final double d_dtSudQMy = dtSudQMy_DS-dtSudQM.getY();
        final double d_dtSudQMz = dtSudQMz_DS-dtSudQM.getZ();
        final double d_dtSudQSx = dtSudQSx_DS-dtSudQS.getX();
        final double d_dtSudQSy = dtSudQSy_DS-dtSudQS.getY();
        final double d_dtSudQSz = dtSudQSz_DS-dtSudQS.getZ();


        // t2 derivatives / stations
        // --------------------------

        final double dt2dQMx_I = dtMddQMx_I + dtSudQMx_I;
        final double dt2dQMy_I = dtMddQMy_I + dtSudQMy_I;
        final double dt2dQMz_I = dtMddQMz_I + dtSudQMz_I;
        final double dt2dQSx_I = dtMddQSx_I + dtSudQSx_I;
        final double dt2dQSy_I = dtMddQSy_I + dtSudQSy_I;
        final double dt2dQSz_I = dtMddQSz_I + dtSudQSz_I;

        final Vector3D dt2dQM = dtSudQM.add(dtMddQM);
        final Vector3D dt2dQS = dtSudQS.add(dtMddQS);

        // With DS
        double dt2dQMx_DS = tauLeg2.getPartialDerivative(6);
        double dt2dQMy_DS = tauLeg2.getPartialDerivative(7);
        double dt2dQMz_DS = tauLeg2.getPartialDerivative(8);
        double dt2dQSx_DS = tauLeg2.getPartialDerivative(9);
        double dt2dQSy_DS = tauLeg2.getPartialDerivative(10);
        double dt2dQSz_DS = tauLeg2.getPartialDerivative(11);


        // Diff
        final double d_dt2dQMx = dt2dQMx_DS-dt2dQM.getX();
        final double d_dt2dQMy = dt2dQMy_DS-dt2dQM.getY();
        final double d_dt2dQMz = dt2dQMz_DS-dt2dQM.getZ();
        final double d_dt2dQSx = dt2dQSx_DS-dt2dQS.getX();
        final double d_dt2dQSy = dt2dQSy_DS-dt2dQS.getY();
        final double d_dt2dQSz = dt2dQSz_DS-dt2dQS.getZ();


        // tSd derivatives / stations
        // --------------------------

        // Master station - Inertial frame
        final double dtSddQMx_I   = dt2dQMx_I*alphaSd;
        final double dtSddQMy_I   = dt2dQMy_I*alphaSd;
        final double dtSddQMz_I   = dt2dQMz_I*alphaSd;

        // Slave station - Inertial frame
        final double dtSddQSx_I   = dt2dQSx_I*alphaSd + 1./dSDown
                        *P1_QSt2.dotProduct(Vector3D.PLUS_I
                                            .subtract(OmegaS.crossProduct(Vector3D.PLUS_I).scalarMultiply(t2)));
        final double dtSddQSy_I   = dt2dQSy_I*alphaSd + 1./dSDown
                        *P1_QSt2.dotProduct(Vector3D.PLUS_J
                                            .subtract(OmegaS.crossProduct(Vector3D.PLUS_J).scalarMultiply(t2)));

        final double dtSddQSz_I   = dt2dQSz_I*alphaSd + 1./dSDown
                        *P1_QSt2.dotProduct(Vector3D.PLUS_K
                                            .subtract(OmegaS.crossProduct(Vector3D.PLUS_K).scalarMultiply(t2)));

        // Topo frames
        final Vector3D dtSddQM = rotationMasterTopoToInert.
                        applyTo(new Vector3D(dtSddQMx_I,
                                             dtSddQMy_I,
                                             dtSddQMz_I));
        final Vector3D dtSddQS = rotationSlaveTopoToInert.
                        applyTo(new Vector3D(dtSddQSx_I,
                                             dtSddQSy_I,
                                             dtSddQSz_I));
        // With DS
        double dtSddQMx_DS = slaveTauD.getPartialDerivative(6);
        double dtSddQMy_DS = slaveTauD.getPartialDerivative(7);
        double dtSddQMz_DS = slaveTauD.getPartialDerivative(8);
        double dtSddQSx_DS = slaveTauD.getPartialDerivative(9);
        double dtSddQSy_DS = slaveTauD.getPartialDerivative(10);
        double dtSddQSz_DS = slaveTauD.getPartialDerivative(11);


        // Diff
        final double d_dtSddQMx = dtSddQMx_DS-dtSddQM.getX();
        final double d_dtSddQMy = dtSddQMy_DS-dtSddQM.getY();
        final double d_dtSddQMz = dtSddQMz_DS-dtSddQM.getZ();
        final double d_dtSddQSx = dtSddQSx_DS-dtSddQS.getX();
        final double d_dtSddQSy = dtSddQSy_DS-dtSddQS.getY();
        final double d_dtSddQSz = dtSddQSz_DS-dtSddQS.getZ();


        // tMu derivatives / stations
        // --------------------------

        // Master station - Inertial frame
        final double dtMudQMx_I = -QMt1_P1.getX()/dMUp + alphaMu*(dt2dQMx_I+dtSddQMx_I)
                        + t/dMUp*QMt1_P1.dotProduct(OmegaM.crossProduct(Vector3D.PLUS_I));

        final double dtMudQMy_I = -QMt1_P1.getY()/dMUp + alphaMu*(dt2dQMy_I+dtSddQMy_I)
                        + t/dMUp*QMt1_P1.dotProduct(OmegaM.crossProduct(Vector3D.PLUS_J));

        final double dtMudQMz_I = -QMt1_P1.getZ()/dMUp + alphaMu*(dt2dQMz_I+dtSddQMz_I)
                        + t/dMUp*QMt1_P1.dotProduct(OmegaM.crossProduct(Vector3D.PLUS_K));


        // Slave station - Inertial frame
        final double dtMudQSx_I = alphaMu*(dt2dQSx_I+dtSddQSx_I);
        final double dtMudQSy_I = alphaMu*(dt2dQSy_I+dtSddQSy_I);
        final double dtMudQSz_I = alphaMu*(dt2dQSz_I+dtSddQSz_I);


        // Topo frames
        final Vector3D dtMudQM = rotationMasterTopoToInert.
                        applyTo(new Vector3D(dtMudQMx_I,
                                             dtMudQMy_I,
                                             dtMudQMz_I));
        final Vector3D dtMudQS = rotationSlaveTopoToInert.
                        applyTo(new Vector3D(dtMudQSx_I,
                                             dtMudQSy_I,
                                             dtMudQSz_I));
        // With DS
        double dtMudQMx_DS = masterTauU.getPartialDerivative(6);
        double dtMudQMy_DS = masterTauU.getPartialDerivative(7);
        double dtMudQMz_DS = masterTauU.getPartialDerivative(8);
        double dtMudQSx_DS = masterTauU.getPartialDerivative(9);
        double dtMudQSy_DS = masterTauU.getPartialDerivative(10);
        double dtMudQSz_DS = masterTauU.getPartialDerivative(11);

        // Diff
        final double d_dtMudQMx = dtMudQMx_DS-dtMudQM.getX();
        final double d_dtMudQMy = dtMudQMy_DS-dtMudQM.getY();
        final double d_dtMudQMz = dtMudQMz_DS-dtMudQM.getZ();
        final double d_dtMudQSx = dtMudQSx_DS-dtMudQS.getX();
        final double d_dtMudQSy = dtMudQSy_DS-dtMudQS.getY();
        final double d_dtMudQSz = dtMudQSz_DS-dtMudQS.getZ();


        // t1 derivatives / stations
        // --------------------------

        final Vector3D dt1dQM = dtMudQM.add(dtSddQM);
        final Vector3D dt1dQS = dtMudQS.add(dtSddQS);

        // With DS
        double dt1dQMx_DS = tauLeg1.getPartialDerivative(6);
        double dt1dQMy_DS = tauLeg1.getPartialDerivative(7);
        double dt1dQMz_DS = tauLeg1.getPartialDerivative(8);
        double dt1dQSx_DS = tauLeg1.getPartialDerivative(9);
        double dt1dQSy_DS = tauLeg1.getPartialDerivative(10);
        double dt1dQSz_DS = tauLeg1.getPartialDerivative(11);


        // Diff
        final double d_dt1dQMx = dt1dQMx_DS-dt1dQM.getX();
        final double d_dt1dQMy = dt1dQMy_DS-dt1dQM.getY();
        final double d_dt1dQMz = dt1dQMz_DS-dt1dQM.getZ();
        final double d_dt1dQSx = dt1dQSx_DS-dt1dQS.getX();
        final double d_dt1dQSy = dt1dQSy_DS-dt1dQS.getY();
        final double d_dt1dQSz = dt1dQSz_DS-dt1dQS.getZ();


        // TAR derivatives / stations
        // --------------------------

        final Vector3D dRdQM = (dt1dQM.add(dt2dQM)).scalarMultiply(cOver2);
        final Vector3D dRdQS = (dt1dQS.add(dt2dQS)).scalarMultiply(cOver2);

        // With DS
        double dRdQMx_DS = turnAroundRange.getPartialDerivative(6);
        double dRdQMy_DS = turnAroundRange.getPartialDerivative(7);
        double dRdQMz_DS = turnAroundRange.getPartialDerivative(8);
        double dRdQSx_DS = turnAroundRange.getPartialDerivative(9);
        double dRdQSy_DS = turnAroundRange.getPartialDerivative(10);
        double dRdQSz_DS = turnAroundRange.getPartialDerivative(11);


        // Diff
        final double d_dRdQMx = dRdQMx_DS-dRdQM.getX();
        final double d_dRdQMy = dRdQMy_DS-dRdQM.getY();
        final double d_dRdQMz = dRdQMz_DS-dRdQM.getZ();
        final double d_dRdQSx = dRdQSx_DS-dRdQS.getX();
        final double d_dRdQSy = dRdQSy_DS-dRdQS.getY();
        final double d_dRdQSz = dRdQSz_DS-dRdQS.getZ();


        // Print results to avoid warning
        final boolean printResults = false;

        if (printResults) {
            System.out.println("dTAR = " + dTAR);

            System.out.println("d_dtMddPx = " + d_dtMddPx);
            System.out.println("d_dtMddPy = " + d_dtMddPy);
            System.out.println("d_dtMddPz = " + d_dtMddPz);
            System.out.println("d_dtMddVx = " + d_dtMddVx);
            System.out.println("d_dtMddVy = " + d_dtMddVy);
            System.out.println("d_dtMddVz = " + d_dtMddVz);

            System.out.println("d_dtSudPx = " + d_dtSudPx);
            System.out.println("d_dtSudPy = " + d_dtSudPy);
            System.out.println("d_dtSudPz = " + d_dtSudPz);
            System.out.println("d_dtSudVx = " + d_dtSudVx);
            System.out.println("d_dtSudVy = " + d_dtSudVy);
            System.out.println("d_dtSudVz = " + d_dtSudVz);

            System.out.println("d_dt2dPx = " + d_dt2dPx);
            System.out.println("d_dt2dPy = " + d_dt2dPy);
            System.out.println("d_dt2dPz = " + d_dt2dPz);
            System.out.println("d_dt2dVx = " + d_dt2dVx);
            System.out.println("d_dt2dVy = " + d_dt2dVy);
            System.out.println("d_dt2dVz = " + d_dt2dVz);

            System.out.println("d_dtSddPx = " + d_dtSddPx);
            System.out.println("d_dtSddPy = " + d_dtSddPy);
            System.out.println("d_dtSddPz = " + d_dtSddPz);
            System.out.println("d_dtSddVx = " + d_dtSddVx);
            System.out.println("d_dtSddVy = " + d_dtSddVy);
            System.out.println("d_dtSddVz = " + d_dtSddVz);

            System.out.println("d_dtMudPx = " + d_dtMudPx);
            System.out.println("d_dtMudPy = " + d_dtMudPy);
            System.out.println("d_dtMudPz = " + d_dtMudPz);
            System.out.println("d_dtMudVx = " + d_dtMudVx);
            System.out.println("d_dtMudVy = " + d_dtMudVy);
            System.out.println("d_dtMudVz = " + d_dtMudVz);

            System.out.println("d_dt1dPx = " + d_dt1dPx);
            System.out.println("d_dt1dPy = " + d_dt1dPy);
            System.out.println("d_dt1dPz = " + d_dt1dPz);
            System.out.println("d_dt1dVx = " + d_dt1dVx);
            System.out.println("d_dt1dVy = " + d_dt1dVy);
            System.out.println("d_dt1dVz = " + d_dt1dVz);

            System.out.println("d_dRdPx = " + d_dRdPx);
            System.out.println("d_dRdPy = " + d_dRdPy);
            System.out.println("d_dRdPz = " + d_dRdPz);
            System.out.println("d_dRdVx = " + d_dRdVx);
            System.out.println("d_dRdVy = " + d_dRdVy);
            System.out.println("d_dRdVz = " + d_dRdVz);

            System.out.println("d_dtMddQMx = " + d_dtMddQMx);
            System.out.println("d_dtMddQMy = " + d_dtMddQMy);
            System.out.println("d_dtMddQMz = " + d_dtMddQMz);
            System.out.println("d_dtMddQSx = " + d_dtMddQSx);
            System.out.println("d_dtMddQSy = " + d_dtMddQSy);
            System.out.println("d_dtMddQSz = " + d_dtMddQSz);

            System.out.println("d_dtSudQMx = " + d_dtSudQMx);
            System.out.println("d_dtSudQMy = " + d_dtSudQMy);
            System.out.println("d_dtSudQMz = " + d_dtSudQMz);
            System.out.println("d_dtSudQSx = " + d_dtSudQSx);
            System.out.println("d_dtSudQSy = " + d_dtSudQSy);
            System.out.println("d_dtSudQSz = " + d_dtSudQSz);

            System.out.println("d_dt2dQMx = " + d_dt2dQMx);
            System.out.println("d_dt2dQMy = " + d_dt2dQMy);
            System.out.println("d_dt2dQMz = " + d_dt2dQMz);
            System.out.println("d_dt2dQSx = " + d_dt2dQSx);
            System.out.println("d_dt2dQSy = " + d_dt2dQSy);
            System.out.println("d_dt2dQSz = " + d_dt2dQSz);

            System.out.println("d_dtSddQMx = " + d_dtSddQMx);
            System.out.println("d_dtSddQMy = " + d_dtSddQMy);
            System.out.println("d_dtSddQMz = " + d_dtSddQMz);
            System.out.println("d_dtSddQSx = " + d_dtSddQSx);
            System.out.println("d_dtSddQSy = " + d_dtSddQSy);
            System.out.println("d_dtSddQSz = " + d_dtSddQSz);

            System.out.println("d_dtMudQMx = " + d_dtMudQMx);
            System.out.println("d_dtMudQMy = " + d_dtMudQMy);
            System.out.println("d_dtMudQMz = " + d_dtMudQMz);
            System.out.println("d_dtMudQSx = " + d_dtMudQSx);
            System.out.println("d_dtMudQSy = " + d_dtMudQSy);
            System.out.println("d_dtMudQSz = " + d_dtMudQSz);

            System.out.println("d_dt1dQMx = " + d_dt1dQMx);
            System.out.println("d_dt1dQMy = " + d_dt1dQMy);
            System.out.println("d_dt1dQMz = " + d_dt1dQMz);
            System.out.println("d_dt1dQSx = " + d_dt1dQSx);
            System.out.println("d_dt1dQSy = " + d_dt1dQSy);
            System.out.println("d_dt1dQSz = " + d_dt1dQSz);

            System.out.println("d_dRdQMx = " + d_dRdQMx);
            System.out.println("d_dRdQMy = " + d_dRdQMy);
            System.out.println("d_dRdQMz = " + d_dRdQMz);
            System.out.println("d_dRdQSx = " + d_dRdQSx);
            System.out.println("d_dRdQSy = " + d_dRdQSy);
            System.out.println("d_dRdQSz = " + d_dRdQSz);


        }

        // Dummy return
        return estimated;


    }
}
