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
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.errors.OrekitException;
import org.orekit.estimation.measurements.GroundStation.OffsetDerivatives;
import org.orekit.frames.Frame;
import org.orekit.frames.Transform;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.AngularCoordinates;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;
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
 * </p>
 * @author Thierry Ceolin
 * @author Luc Maisonobe
 * @author Maxime Journot
 *
 * @since 9.0
 */
public class TurnAroundRange extends AbstractMeasurement<TurnAroundRange> {

    /** Master ground station from which measurement is performed. */
    private final GroundStation masterStation;

    /** Slave ground station reflecting the signal. */
    private final GroundStation slaveStation;

    /** Factory for the DerivativeStructure instances. */
    private final DSFactory factory;


    /** Simple constructor.
     * @param masterStation ground station from which measurement is performed
     * @param slaveStation ground station reflecting the signal
     * @param date date of the measurement
     * @param turnAroundRange observed value
     * @param sigma theoretical standard deviation
     * @param baseWeight base weight
     * @exception OrekitException if a {@link org.orekit.utils.ParameterDriver}
     * name conflict occurs
     */
    public TurnAroundRange(final GroundStation masterStation, final GroundStation slaveStation,
                           final AbsoluteDate date, final double turnAroundRange,
                           final double sigma, final double baseWeight)
        throws OrekitException {
        super(date, turnAroundRange, sigma, baseWeight,
              masterStation.getEastOffsetDriver(),
              masterStation.getNorthOffsetDriver(),
              masterStation.getZenithOffsetDriver(),
              slaveStation.getEastOffsetDriver(),
              slaveStation.getNorthOffsetDriver(),
              slaveStation.getZenithOffsetDriver());
        this.masterStation = masterStation;
        this.slaveStation = slaveStation;
        this.factory = new DSFactory(12, 1);
    }

    /** Get the master ground station from which measurement is performed.
     * @return master ground station from which measurement is performed
     */
    public GroundStation getMasterStation() {
        return masterStation;
    }

    /** Get the slave ground station reflecting the signal.
     * @return slave ground station reflecting the signal
     */
    public GroundStation getSlaveStation() {
        return slaveStation;
    }

    /** {@inheritDoc} */
    @Override
    protected EstimatedMeasurement<TurnAroundRange> theoreticalEvaluation(final int iteration, final int evaluation,
                                                                          final SpacecraftState state)
        throws OrekitException {

        /* Turn around range derivatives are computed with respect to:
         * - Spacecraft state in inertial frame
         * - Master station position in master station's offset frame
         * - Slave station position in slave station's offset frame
         * -------
         *
         * Parameters:
         *  - 0..2  - Px, Py, Pz      : Position of the spacecraft in inertial frame
         *  - 3..5  - Vx, Vy, Vz      : Velocity of the spacecraft in inertial frame
         *  - 6..8  - QMTx, QMTy, QMTz: Position of the master station in station's offset topocentric frame
         *  - 9..11 - QSTx, QSTy, QSTz: Position of the slave station in station's offset topocentric frame
         */

        // PV coordinates of the spacecraft at time t'
        final PVCoordinates statePV = state.getPVCoordinates();

        // Position of the spacecraft expressed as a derivative structure
        // The components of the position are the 3 first derivative parameters
        final Vector3D stateP = statePV.getPosition();
        final FieldVector3D<DerivativeStructure> pDS = new FieldVector3D<DerivativeStructure>(
                        factory.variable(0, stateP.getX()),
                        factory.variable(1, stateP.getY()),
                        factory.variable(2, stateP.getZ()));

        // Velocity of the spacecraft expressed as a derivative structure
        // The components of the velocity are the 3 second derivative parameters
        final Vector3D stateV = statePV.getVelocity();
        final FieldVector3D<DerivativeStructure> vDS = new FieldVector3D<DerivativeStructure>(
                        factory.variable(3, stateV.getX()),
                        factory.variable(4, stateV.getY()),
                        factory.variable(5, stateV.getZ()));

        // Acceleration of the spacecraft
        // The components of the acceleration are not derivative parameters
        final Vector3D stateA = state.getPVCoordinates().getAcceleration();
        final FieldVector3D<DerivativeStructure> aDS = new FieldVector3D<DerivativeStructure>(
                        factory.constant(stateA.getX()),
                        factory.constant(stateA.getY()),
                        factory.constant(stateA.getZ()));

        // Place the derivative structures in a time-stamped PV
        final TimeStampedFieldPVCoordinates<DerivativeStructure> pvaDS =
                        new TimeStampedFieldPVCoordinates<DerivativeStructure>(state.getDate(), pDS, vDS, aDS);

        // Master station topocentric frame (east-north-zenith) in master station parent frame expressed as DerivativeStructures
        // The components of master station's position in offset frame are the 3 third derivative parameters
        final OffsetDerivatives masterOd = masterStation.getOffsetDerivatives(factory, 6, 7, 8);

        // Slave station topocentric frame (east-north-zenith) in slave station parent frame expressed as DerivativeStructures
        // The components of slave station's position in offset frame are the 3 last derivative parameters
        final OffsetDerivatives slaveOd = slaveStation.getOffsetDerivatives(factory, 9, 10, 11);

        // Master station body frame
        final Frame masterBodyFrame = masterStation.getOffsetFrame().getParentShape().getBodyFrame();

        // Master station PV in inertial frame at measurement date
        final AbsoluteDate measurementDate = this.getDate();
        final Transform masterBodyToInert = masterBodyFrame.getTransformTo(state.getFrame(), measurementDate);
        final TimeStampedFieldPVCoordinates<DerivativeStructure> QMaster =
                        masterBodyToInert.transformPVCoordinates(new TimeStampedFieldPVCoordinates<>(
                                        measurementDate,
                                        masterOd.getOrigin(),
                                        masterOd.getZero(),
                                        masterOd.getZero()));

        // Slave station body frame
        final Frame slaveBodyFrame = slaveStation.getOffsetFrame().getParentShape().getBodyFrame();

        // Slave station PV in inertial frame at measurement date
        final Transform slaveBodyToInert = slaveBodyFrame.getTransformTo(state.getFrame(), measurementDate);
        final TimeStampedFieldPVCoordinates<DerivativeStructure> QSlave =
                        slaveBodyToInert.transformPVCoordinates(new TimeStampedFieldPVCoordinates<>(
                                        measurementDate,
                                        slaveOd.getOrigin(),
                                        slaveOd.getZero(),
                                        slaveOd.getZero()));

        // Compute propagation times
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

        // Time difference between t (date of the measurement) and t' (date tagged in spacecraft state)
        // (if state has already been set up to pre-compensate propagation delay,
        // we will have delta = masterTauD + slaveTauU)
        final double delta = getDate().durationFrom(state.getDate());

        final DerivativeStructure masterTauD = masterStation.
                        signalTimeOfFlight(pvaDS, QMaster.getPosition(), measurementDate);


        // Elapsed time between state date t' and signal arrival to the transit state of the 2nd leg
        final DerivativeStructure dtLeg2 = masterTauD.negate().add(delta);

        // Transit state where the satellite reflected the signal from slave to master station
        final SpacecraftState transitStateLeg2   = state.shiftedBy(dtLeg2.getValue());

        // Transit state pv of leg2 (re)computed with derivative structures
        final TimeStampedFieldPVCoordinates<DerivativeStructure> transitStateLeg2PV = pvaDS.shiftedBy(dtLeg2);

        // Slave station at transit state date of leg2 (derivatives of masterTauD taken into account)
        final TimeStampedFieldPVCoordinates<DerivativeStructure> QSlaveAtTransitLeg2 =
                        QSlave.shiftedBy(masterTauD.negate());

        // Uplink time of flight from slave station to transit state of leg2
        final DerivativeStructure slaveTauU = slaveStation.
                        signalTimeOfFlight(QSlaveAtTransitLeg2,
                                           transitStateLeg2PV.getPosition(),
                                           transitStateLeg2.getDate());

        // Total time of flight for leg 2
        final DerivativeStructure tauLeg2 = masterTauD.add(slaveTauU);


        // Compute propagation time for the 1st leg of the signal path
        // --

        // Absolute date of arrival/departure of the signal to slave station
        final AbsoluteDate slaveStationArrivalDate = measurementDate.shiftedBy(-tauLeg2.getValue());

        // Slave station PV in inertial frame at date slaveStationArrivalDate
        final TimeStampedFieldPVCoordinates<DerivativeStructure> QSlaveArrivalDate =
                        QSlave.shiftedBy(tauLeg2.negate());

        // Dowlink time of flight from transitStateLeg1 to slave station at slaveStationArrivalDate
        final DerivativeStructure slaveTauD = slaveStation.
                        signalTimeOfFlight(transitStateLeg2PV, QSlaveArrivalDate.getPosition(), slaveStationArrivalDate);


        // Elapsed time between state date t' and signal arrival to the transit state of the 1st leg
        final DerivativeStructure dtLeg1 = dtLeg2.subtract(slaveTauU).subtract(slaveTauD);

        // Transit state from which the satellite reflected the signal from master to slave station
        final SpacecraftState transitStateLeg1   = state.shiftedBy(dtLeg1.getValue());

        // Transit state pv of leg2 (re)computed with derivative structures
        final TimeStampedFieldPVCoordinates<DerivativeStructure> transitStateLeg1PV = pvaDS.shiftedBy(dtLeg1);

        // Master station at transit state date of leg1 (derivatives of masterTauD, slaveTauU and slaveTauD taken into account)
        final TimeStampedFieldPVCoordinates<DerivativeStructure> QMasterAtTransitLeg1 =
                        QMaster.shiftedBy(tauLeg2.negate().subtract(slaveTauD));

        // Uplink time of flight from master station to transit state of leg1
        final DerivativeStructure masterTauU = masterStation.
                        signalTimeOfFlight(QMasterAtTransitLeg1,
                                           transitStateLeg1PV.getPosition(),
                                           transitStateLeg1.getDate());

        // Total time of flight for leg 1
        final DerivativeStructure tauLeg1    = slaveTauD.add(masterTauU);


        // --
        // Evaluate the turn-around range value and its derivatives
        // --------------------------------------------------------

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
                        new EstimatedMeasurement<TurnAroundRange>(this,
                                        iteration, evaluation,
                                        transitStateLeg2.shiftedBy(-slaveTauU.getValue()));

        // Turn-around range value = Total time of flight for the 2 legs divided by 2 and multiplied by c
        final double cOver2 = 0.5 * Constants.SPEED_OF_LIGHT;
        final DerivativeStructure turnAroundRange = (tauLeg2.add(tauLeg1)).multiply(cOver2);
        estimated.setEstimatedValue(turnAroundRange.getValue());


        // Turn-around range partial derivatives with respect to state
        estimated.setStateDerivatives(new double[] {turnAroundRange.getPartialDerivative(1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0), // dROndPx
                                                    turnAroundRange.getPartialDerivative(0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0), // dROndPy
                                                    turnAroundRange.getPartialDerivative(0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0), // dROndPz
                                                    turnAroundRange.getPartialDerivative(0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0), // dROndVx
                                                    turnAroundRange.getPartialDerivative(0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0), // dROndVy
                                                    turnAroundRange.getPartialDerivative(0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0), // dROndVz
        });


        // Set parameter drivers partial derivatives with respect to stations' position in stations'offset topocentric frame
        // Master station
        if (masterStation.getEastOffsetDriver().isSelected()) {
            estimated.setParameterDerivatives(masterStation.getEastOffsetDriver(),
                                              turnAroundRange.getPartialDerivative(0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0)); // dROndQMTx
        }
        if (masterStation.getNorthOffsetDriver().isSelected()) {
            estimated.setParameterDerivatives(masterStation.getNorthOffsetDriver(),
                                              turnAroundRange.getPartialDerivative(0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0)); // dROndQMTy
        }
        if (masterStation.getZenithOffsetDriver().isSelected()) {
            estimated.setParameterDerivatives(masterStation.getZenithOffsetDriver(),
                                              turnAroundRange.getPartialDerivative(0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0)); // dROndQMTz
        }

        // Slave station
        if (slaveStation.getEastOffsetDriver().isSelected()) {
            estimated.setParameterDerivatives(slaveStation.getEastOffsetDriver(),
                                              turnAroundRange.getPartialDerivative(0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0)); // dROndQSTx
        }
        if (slaveStation.getNorthOffsetDriver().isSelected()) {
            estimated.setParameterDerivatives(slaveStation.getNorthOffsetDriver(),
                                              turnAroundRange.getPartialDerivative(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0)); // dROndQSTy
        }
        if (slaveStation.getZenithOffsetDriver().isSelected()) {
            estimated.setParameterDerivatives(slaveStation.getZenithOffsetDriver(),
                                              turnAroundRange.getPartialDerivative(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1)); // dROndQSTz
        }

        return estimated;

    }

    /**
     * Analytical version of the previous function.
     * The derivative structures are not used
     * For now only the value of turn-around range and not its derivatives are available
     * @param iteration iteration number
     * @param evaluation evaluation number
     * @param state orbital state at measurement date
     * @return theoretical value
     * @exception OrekitException if value cannot be computed
     */
    protected EstimatedMeasurement<TurnAroundRange> theoreticalEvaluationAnalytic(final int iteration, final int evaluation,
                                                                                  final SpacecraftState state)
        throws OrekitException {

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
                        masterStation.getOffsetFrame().getTransformTo(state.getFrame(), measurementDate);
        final Vector3D QMaster = masterTopoToInert.transformPosition(Vector3D.ZERO);

        // Downlink time of flight from master station at t to spacecraft at t'
        final double tMd    = masterStation.
                        signalTimeOfFlight(state.getPVCoordinates(), QMaster, measurementDate);

        // Time difference between t (date of the measurement) and t' (date tagged in spacecraft state)
        // (if state has already been set up to pre-compensate propagation delay, delta = masterTauD + slaveTauU)
        final double delta  = getDate().durationFrom(state.getDate());

        // Transit state from which the satellite reflected the signal from slave to master station
        final SpacecraftState transitStateLeg2  = state.shiftedBy(delta - tMd);
        final AbsoluteDate    transitDateLeg2   = transitStateLeg2.getDate();

        // Slave station PV at transit state leg2 date
        final Transform slaveTopoToInertTransitLeg2 =
                        slaveStation.getOffsetFrame().getTransformTo(state.getFrame(), transitDateLeg2);
        final TimeStampedPVCoordinates QSlaveTransitLeg2PV = slaveTopoToInertTransitLeg2.
                        transformPVCoordinates(new TimeStampedPVCoordinates(transitDateLeg2, PVCoordinates.ZERO));

        // Uplink time of flight from slave station to transit state leg2
        final double tSu    = slaveStation.signalTimeOfFlight(QSlaveTransitLeg2PV,
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
                        slaveStation.getOffsetFrame().getTransformTo(state.getFrame(), slaveStationArrivalDate);
        final Vector3D QSlaveArrivalDate = slaveTopoToInertArrivalDate.transformPosition(Vector3D.ZERO);

        // Dowlink time of flight from transitStateLeg1 to slave station at slaveStationArrivalDate
        final double tSd = slaveStation.
                        signalTimeOfFlight(transitStateLeg2.getPVCoordinates(), QSlaveArrivalDate, slaveStationArrivalDate);

        // Transit state from which the satellite reflected the signal from master to slave station
        final SpacecraftState transitStateLeg1  = state.shiftedBy(delta - tMd - tSu - tSd);
        final AbsoluteDate    transitDateLeg1   = transitStateLeg1.getDate();

        // Master station PV at transit state date of leg1
        final Transform masterTopoToInertTransitLeg1 =
                        masterStation.getOffsetFrame().getTransformTo(state.getFrame(), transitDateLeg1);
        final TimeStampedPVCoordinates QMasterTransitLeg1PV = masterTopoToInertTransitLeg1.
                        transformPVCoordinates(new TimeStampedPVCoordinates(transitDateLeg1, PVCoordinates.ZERO));

        // Uplink time of flight from master station to transit state leg1
        final double tMu = masterStation.signalTimeOfFlight(QMasterTransitLeg1PV,
                                                            transitStateLeg1.getPVCoordinates().getPosition(),
                                                            transitDateLeg1);
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
                        new EstimatedMeasurement<TurnAroundRange>(this,
                                        iteration, evaluation,
                                        transitStateLeg2.shiftedBy(-tSu));

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
        final Transform FMt     = masterStation.getOffsetFrame().getTransformTo(state.getFrame(), getDate());
        final PVCoordinates QMt = FMt.transformPVCoordinates(PVCoordinates.ZERO);
        final Vector3D QMt_V    = QMt.getVelocity();
        final Vector3D pos2       = transitStateLeg2.getPVCoordinates().getPosition();
        final Vector3D P2_QMt   = QMt.getPosition().subtract(pos2);
        final double   dMDown    = Constants.SPEED_OF_LIGHT * Constants.SPEED_OF_LIGHT * tMd -
                        Vector3D.dotProduct(P2_QMt, vel);

        // Derivatives w/r state
        final double dtMddPx   = -P2_QMt.getX() / dMDown;
        final double dtMddPy   = -P2_QMt.getY() / dMDown;
        final double dtMddPz   = -P2_QMt.getZ() / dMDown;

        final double dt     = delta - tMd;
        final double dtMddVx   = dtMddPx * dt;
        final double dtMddVy   = dtMddPy * dt;
        final double dtMddVz   = dtMddPz * dt;


        // tSu derivatives / state
        // -----------------------

        // QSt = slave station position at tmeas = t = signal arrival at master station
        final Transform FSt     = slaveStation.getOffsetFrame().getTransformTo(state.getFrame(), getDate());
        final PVCoordinates QSt = FSt.transformPVCoordinates(PVCoordinates.ZERO);
        final Vector3D QSt_V    = QSt.getVelocity();

        // QSt2 = slave station position at t-t2 = signal arrival at slave station
        final Transform FSt2     = slaveStation.getOffsetFrame().getTransformTo(state.getFrame(), getDate().shiftedBy(-t2));
        final PVCoordinates QSt2 = FSt2.transformPVCoordinates(PVCoordinates.ZERO);

        final Vector3D QSt2_P2   = pos2.subtract(QSt2.getPosition());
        final double   dSUp    = Constants.SPEED_OF_LIGHT * Constants.SPEED_OF_LIGHT * tSu -
                        Vector3D.dotProduct(QSt2_P2, QSt_V);

        // Derivatives w/r state
        final double alphaSu = 1. / dSUp * QSt2_P2.dotProduct(QSt_V.subtract(vel));
        final double dtSudPx = 1. / dSUp * QSt2_P2.getX() + alphaSu * dtMddPx;
        final double dtSudPy = 1. / dSUp * QSt2_P2.getY() + alphaSu * dtMddPy;
        final double dtSudPz = 1. / dSUp * QSt2_P2.getZ() + alphaSu * dtMddPz;
        final double dtSudVx   = dtSudPx * dt;
        final double dtSudVy   = dtSudPy * dt;
        final double dtSudVz   = dtSudPz * dt;


        // t2 derivatives / state
        // -----------------------

        final double dt2dPx = dtSudPx + dtMddPx;
        final double dt2dPy = dtSudPy + dtMddPy;
        final double dt2dPz = dtSudPz + dtMddPz;
        final double dt2dVx = dtSudVx + dtMddVx;
        final double dt2dVy = dtSudVy + dtMddVy;
        final double dt2dVz = dtSudVz + dtMddVz;


        // tSd derivatives / state
        // -----------------------

        final Vector3D pos1       = transitStateLeg1.getPVCoordinates().getPosition();
        final Vector3D P1_QSt2   = QSt2.getPosition().subtract(pos1);
        final double   dSDown    = Constants.SPEED_OF_LIGHT * Constants.SPEED_OF_LIGHT * tSd -
                        Vector3D.dotProduct(P1_QSt2, vel);

        // derivatives w/r to state
        final double alphaSd = 1. / dSDown * P1_QSt2.dotProduct(vel.subtract(QSt_V));
        final double dtSddPx   = -1. / dSDown * P1_QSt2.getX() + alphaSd * dt2dPx;
        final double dtSddPy   = -1. / dSDown * P1_QSt2.getY() + alphaSd * dt2dPy;
        final double dtSddPz   = -1. / dSDown * P1_QSt2.getZ() + alphaSd * dt2dPz;

        final double dt2     = delta - t2 - tSd;
        final double dtSddVx   = -dt2 / dSDown * P1_QSt2.getX() + alphaSd * dt2dVx;
        final double dtSddVy   = -dt2 / dSDown * P1_QSt2.getY() + alphaSd * dt2dVy;
        final double dtSddVz   = -dt2 / dSDown * P1_QSt2.getZ() + alphaSd * dt2dVz;

        // tMu derivatives / state
        // -----------------------

        // QMt1 = Master station position at t1 = t - tau = signal departure from master station
        final Transform FMt1     = masterStation.getOffsetFrame().getTransformTo(state.getFrame(), getDate().shiftedBy(-(t1 + t2)));
        final PVCoordinates QMt1 = FMt1.transformPVCoordinates(PVCoordinates.ZERO);

        final Vector3D QMt1_P1   = pos1.subtract(QMt1.getPosition());
        final double   dMUp    = Constants.SPEED_OF_LIGHT * Constants.SPEED_OF_LIGHT * tMu -
                        Vector3D.dotProduct(QMt1_P1, QMt_V);

        // derivatives w/r to state
        final double alphaMu = 1. / dMUp * QMt1_P1.dotProduct(QMt_V.subtract(vel));
        final double dtMudPx = 1. / dMUp * QMt1_P1.getX() + alphaMu * (dt2dPx + dtSddPx);
        final double dtMudPy = 1. / dMUp * QMt1_P1.getY() + alphaMu * (dt2dPy + dtSddPy);
        final double dtMudPz = 1. / dMUp * QMt1_P1.getZ() + alphaMu * (dt2dPz + dtSddPz);

        final double dtMudVx = dt2 / dMUp * QMt1_P1.getX() + alphaMu * (dt2dVx + dtSddVx);
        final double dtMudVy = dt2 / dMUp * QMt1_P1.getY() + alphaMu * (dt2dVy + dtSddVy);
        final double dtMudVz = dt2 / dMUp * QMt1_P1.getZ() + alphaMu * (dt2dVz + dtSddVz);


        // t1 derivatives / state
        // t1 = tauLeg1
        // -----------------------

        // t1 = Time leg 1
        final double dt1dPx = dtSddPx + dtMudPx;
        final double dt1dPy = dtSddPy + dtMudPy;
        final double dt1dPz = dtSddPz + dtMudPz;
        final double dt1dVx = dtSddVx + dtMudVx;
        final double dt1dVy = dtSddVy + dtMudVy;
        final double dt1dVz = dtSddVz + dtMudVz;


        // TAR derivatives / state
        // -----------------------

        // R = TAR
        final double dRdPx = (dt1dPx + dt2dPx) * cOver2;
        final double dRdPy = (dt1dPy + dt2dPy) * cOver2;
        final double dRdPz = (dt1dPz + dt2dPz) * cOver2;
        final double dRdVx = (dt1dVx + dt2dVx) * cOver2;
        final double dRdVy = (dt1dVy + dt2dVy) * cOver2;
        final double dRdVz = (dt1dVz + dt2dVz) * cOver2;

        estimated.setStateDerivatives(new double[] {dRdPx, dRdPy, dRdPz, // dROndP
                                                    dRdVx, dRdVy, dRdVz  // dROndV
        });


        // TAR derivatives w/r stations' position in topocentric frames
        // ------------------------------------------------------------

        if (masterStation.getEastOffsetDriver().isSelected()   ||
            masterStation.getNorthOffsetDriver().isSelected()  ||
            masterStation.getZenithOffsetDriver().isSelected() ||
            slaveStation.getEastOffsetDriver().isSelected()    ||
            slaveStation.getNorthOffsetDriver().isSelected()   ||
            slaveStation.getZenithOffsetDriver().isSelected()) {

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
            final double dtSudQMx_I   = dtMddQMx_I * alphaSu;
            final double dtSudQMy_I   = dtMddQMy_I * alphaSu;
            final double dtSudQMz_I   = dtMddQMz_I * alphaSu;

            // Slave station - Inertial frame
            final double dtSudQSx_I   = QSt2_P2.dotProduct(new Vector3D(1.0, Vector3D.MINUS_I,
                                                                        t2, OmegaS.crossProduct(Vector3D.PLUS_I))) / dSUp;
            final double dtSudQSy_I   = QSt2_P2.dotProduct(new Vector3D(1.0, Vector3D.MINUS_J,
                                                                        t2, OmegaS.crossProduct(Vector3D.PLUS_J))) / dSUp;
            final double dtSudQSz_I   = QSt2_P2.dotProduct(new Vector3D(1.0, Vector3D.MINUS_K,
                                                                        t2, OmegaS.crossProduct(Vector3D.PLUS_K))) / dSUp;

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
            final double dtSddQMx_I   = dt2dQMx_I * alphaSd;
            final double dtSddQMy_I   = dt2dQMy_I * alphaSd;
            final double dtSddQMz_I   = dt2dQMz_I * alphaSd;

            // Slave station - Inertial frame
            final double dtSddQSx_I   = dt2dQSx_I * alphaSd +
                                        P1_QSt2.dotProduct(new Vector3D(1.0, Vector3D.PLUS_I,
                                                                        -t2, OmegaS.crossProduct(Vector3D.PLUS_I))) / dSDown;
            final double dtSddQSy_I   = dt2dQSy_I * alphaSd +
                                        P1_QSt2.dotProduct(new Vector3D(1.0, Vector3D.PLUS_J,
                                                                        -t2, OmegaS.crossProduct(Vector3D.PLUS_J))) / dSDown;

            final double dtSddQSz_I   = dt2dQSz_I * alphaSd +
                                        P1_QSt2.dotProduct(new Vector3D(1.0, Vector3D.PLUS_K,
                                                                        -t2, OmegaS.crossProduct(Vector3D.PLUS_K))) / dSDown;

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
            final double dtMudQMx_I = alphaMu * (dt2dQMx_I + dtSddQMx_I) +
                                      QMt1_P1.dotProduct(new Vector3D(1.0, Vector3D.MINUS_I,
                                                                      tau, OmegaM.crossProduct(Vector3D.PLUS_I))) / dMUp;
            final double dtMudQMy_I = alphaMu * (dt2dQMy_I + dtSddQMy_I) +
                                      QMt1_P1.dotProduct(new Vector3D(1.0, Vector3D.MINUS_J,
                                                                      tau, OmegaM.crossProduct(Vector3D.PLUS_J))) / dMUp;
            final double dtMudQMz_I = alphaMu * (dt2dQMz_I + dtSddQMz_I) +
                                      QMt1_P1.dotProduct(new Vector3D(1.0, Vector3D.MINUS_K,
                                                                      tau, OmegaM.crossProduct(Vector3D.PLUS_K))) / dMUp;

            // Slave station - Inertial frame
            final double dtMudQSx_I = alphaMu * (dt2dQSx_I + dtSddQSx_I);
            final double dtMudQSy_I = alphaMu * (dt2dQSy_I + dtSddQSy_I);
            final double dtMudQSz_I = alphaMu * (dt2dQSz_I + dtSddQSz_I);


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
            if (masterStation.getEastOffsetDriver().isSelected()) {
                estimated.setParameterDerivatives(masterStation.getEastOffsetDriver(), dRdQM.getX());
            }
            if (masterStation.getNorthOffsetDriver().isSelected()) {
                estimated.setParameterDerivatives(masterStation.getNorthOffsetDriver(), dRdQM.getY());
            }
            if (masterStation.getZenithOffsetDriver().isSelected()) {
                estimated.setParameterDerivatives(masterStation.getZenithOffsetDriver(), dRdQM.getZ());
            }

            // Slave station drivers
            if (slaveStation.getEastOffsetDriver().isSelected()) {
                estimated.setParameterDerivatives(slaveStation.getEastOffsetDriver(), dRdQS.getX());
            }
            if (slaveStation.getNorthOffsetDriver().isSelected()) {
                estimated.setParameterDerivatives(slaveStation.getNorthOffsetDriver(), dRdQS.getY());
            }
            if (slaveStation.getZenithOffsetDriver().isSelected()) {
                estimated.setParameterDerivatives(slaveStation.getZenithOffsetDriver(), dRdQS.getZ());
            }
        }

        return estimated;

    }

}
