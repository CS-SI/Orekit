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

import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.Transform;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.TimeStampedFieldPVCoordinates;

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
        super(date, range, sigma, baseWeight,
              station.getEastOffsetDriver(),
              station.getNorthOffsetDriver(),
              station.getZenithOffsetDriver());
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
    protected EstimatedMeasurement<Range> theoreticalEvaluation(final int iteration, final int evaluation,
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
        final int parameters = 9;
        final int order      = 1;

        // Position of the spacecraft expressed as a derivative structure
        // The components of the position are the 3 first derivative parameters
        final Vector3D stateP = state.getPVCoordinates().getPosition();
        final FieldVector3D<DerivativeStructure> pDS =
                        new FieldVector3D<DerivativeStructure>(new DerivativeStructure(parameters, order, 0, stateP.getX()),
                                                               new DerivativeStructure(parameters, order, 1, stateP.getY()),
                                                               new DerivativeStructure(parameters, order, 2, stateP.getZ()));

        // Velocity of the spacecraft expressed as a derivative structure
        // The components of the velocity are the 3 second derivative parameters
        final Vector3D stateV = state.getPVCoordinates().getVelocity();
        final FieldVector3D<DerivativeStructure> vDS =
                        new FieldVector3D<DerivativeStructure>(new DerivativeStructure(parameters, order, 3, stateV.getX()),
                                                               new DerivativeStructure(parameters, order, 4, stateV.getY()),
                                                               new DerivativeStructure(parameters, order, 5, stateV.getZ()));

        // Acceleration of the spacecraft
        // The components of the acceleration are not derivative parameters
        final Vector3D stateA = state.getPVCoordinates().getAcceleration();
        final FieldVector3D<DerivativeStructure> aDS =
                        new FieldVector3D<DerivativeStructure>(new DerivativeStructure(parameters, order, stateA.getX()),
                                                               new DerivativeStructure(parameters, order, stateA.getY()),
                                                               new DerivativeStructure(parameters, order, stateA.getZ()));

        final TimeStampedFieldPVCoordinates<DerivativeStructure> pvaDS =
                        new TimeStampedFieldPVCoordinates<DerivativeStructure>(state.getDate(), pDS, vDS, aDS);

        // Station position in body frame, expressed as a derivative structure
        // The components of station's position in offset frame are the 3 last derivative parameters
        final GroundStation.OffsetDerivatives od = station.getOffsetDerivatives(parameters, 6, 7, 8);
        final Frame bodyframe = station.getOffsetFrame().getParentShape().getBodyFrame();

        // Station position in inertial frame at end of the downlink leg
        final AbsoluteDate downlinkDate = getDate();
        final Transform bodyToInertDownlink = bodyframe.getTransformTo(state.getFrame(), downlinkDate);
        final FieldVector3D<DerivativeStructure> stationDownlink = bodyToInertDownlink.transformPosition(od.getOrigin());

        // Compute propagation times
        // (if state has already been set up to pre-compensate propagation delay,
        //  we will have offset == downlinkDelay and transitState will be
        //  the same as state)

        // Downlink delay
        final DerivativeStructure tauD = station.signalTimeOfFlight(pvaDS, stationDownlink, downlinkDate);

        // Transit state
        final double                delta        = downlinkDate.durationFrom(state.getDate());
        final DerivativeStructure   tauDMDelta   = tauD.negate().add(delta);
        final SpacecraftState       transitState = state.shiftedBy(tauDMDelta.getValue());

        // Transit state position (re)computed with derivative structures
        final FieldVector3D<DerivativeStructure> transitStatePosition = pvaDS.shiftedBy(tauDMDelta).getPosition();

        // Station at start of the uplink leg
        final double cOver2 = 0.5 * Constants.SPEED_OF_LIGHT;
        final AbsoluteDate uplinkDate = downlinkDate.shiftedBy(-getObservedValue()[0] / cOver2);
        final Transform bodyToInertUplink =
                        bodyframe.getTransformTo(state.getFrame(), uplinkDate);
        final TimeStampedFieldPVCoordinates<DerivativeStructure> stationUplink =
                        bodyToInertUplink.transformPVCoordinates(new TimeStampedFieldPVCoordinates<>(uplinkDate,
                                                                                                     od.getOrigin(),
                                                                                                     od.getZero(),
                                                                                                     od.getZero()));

        // Uplink delay
        final DerivativeStructure tauU = station.signalTimeOfFlight(stationUplink,
                                                                    transitStatePosition,
                                                                    transitState.getDate());

        // Prepare the evaluation
        final EstimatedMeasurement<Range> estimated =
                        new EstimatedMeasurement<Range>(this, iteration, evaluation, transitState);

        // Range value
        final DerivativeStructure tau = tauD.add(tauU);
        final DerivativeStructure range = tau.multiply(cOver2);
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

}
