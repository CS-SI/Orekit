/* Copyright 2002-2026 CS GROUP
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

import java.util.Map;

import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.MathUtils;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.FieldGeodeticPoint;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.estimation.measurements.model.TopocentricAzElModel;
import org.orekit.frames.Frame;
import org.orekit.propagation.SpacecraftState;
import org.orekit.signal.SignalTravelTimeModel;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldPVCoordinatesProvider;
import org.orekit.utils.PVCoordinatesProvider;
import org.orekit.utils.TimeStampedFieldPVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

/** Class modeling an Azimuth-Elevation measurement from a ground station.
 * The motion of the spacecraft during the signal flight time is taken into
 * account. The date of the measurement corresponds to the reception on
 * ground of the reflected signal.
 *
 * @author Thierry Ceolin
 * @since 8.0
 */
public class AngularAzEl extends GroundBasedAngularMeasurement<AngularAzEl> {

    /** Type of the measurement. */
    public static final String MEASUREMENT_TYPE = "AngularAzEl";

    /** Simple constructor.
     * @param station ground station from which measurement is performed
     * @param date date of the measurement
     * @param angular observed value
     * @param sigma theoretical standard deviation
     * @param baseWeight base weight
     * @param signalTravelTimeModel signal travel time model
     * @param satellite satellite related to this measurement
     * @since 14.0
     */
    public AngularAzEl(final GroundStation station, final AbsoluteDate date,
                       final double[] angular, final double[] sigma, final double[] baseWeight,
                       final SignalTravelTimeModel signalTravelTimeModel, final ObservableSatellite satellite) {
        super(station, date, angular, sigma, baseWeight, signalTravelTimeModel, satellite);
    }

    /** Simple constructor.
     * @param station ground station from which measurement is performed
     * @param date date of the measurement
     * @param angular observed value
     * @param sigma theoretical standard deviation
     * @param baseWeight base weight
     * @param satellite satellite related to this measurement
     * @since 9.3
     */
    public AngularAzEl(final GroundStation station, final AbsoluteDate date,
                       final double[] angular, final double[] sigma, final double[] baseWeight,
                       final ObservableSatellite satellite) {
        this(station, date, angular, sigma, baseWeight, new SignalTravelTimeModel(), satellite);
    }

    /** {@inheritDoc} */
    @Override
    protected EstimatedMeasurementBase<AngularAzEl> theoreticalEvaluationWithoutDerivatives(final int iteration,
                                                                                            final int evaluation,
                                                                                            final SpacecraftState[] states) {
        // Compute emission date
        final AbsoluteDate receptionDate = getStation().getCorrectedReceptionDate(getDate());
        final PVCoordinatesProvider receiverPVProvider = getStation().getPVCoordinatesProvider();
        final SpacecraftState state = states[0];
        final Frame frame = state.getFrame();
        final PVCoordinatesProvider emitter = AbstractParticipant.extractPVCoordinatesProvider(state, state.getPVCoordinates());
        final AbsoluteDate emissionDate = computeEmissionDate(frame, receiverPVProvider, receptionDate, emitter);

        // Compute azimuth and elevation
        final BodyShape bodyShape = getStation().getBaseFrame().getParentShape();
        final TimeStampedPVCoordinates receiverPV = receiverPVProvider.getPVCoordinates(receptionDate, frame);
        final GeodeticPoint geodeticPoint = bodyShape.transform(receiverPV.getPosition(), frame, receptionDate);
        final TopocentricAzElModel measurementModel = new TopocentricAzElModel(frame, bodyShape,
                getSignalTravelTimeModel().getWarmedUpModel());
        final double[] azEl = measurementModel.value(geodeticPoint, receptionDate, emitter, emissionDate);

        // Prepare the estimation
        final double shift = emissionDate.durationFrom(state);
        final SpacecraftState shiftedState = state.shiftedBy(shift);
        final EstimatedMeasurementBase<AngularAzEl> estimated = new EstimatedMeasurementBase<>(this, iteration, evaluation,
                new SpacecraftState[] { shiftedState },
                new TimeStampedPVCoordinates[] { shiftedState.getPVCoordinates(), receiverPV });
        estimated.setEstimatedValue(wrapFirstAngle(azEl[0]), azEl[1]);
        return estimated;
    }

    /** {@inheritDoc} */
    @Override
    protected EstimatedMeasurement<AngularAzEl> theoreticalEvaluation(final int iteration, final int evaluation,
                                                                      final SpacecraftState[] states) {
        // Azimuth/elevation derivatives are computed with respect to spacecraft state in inertial frame
        // and station parameters
        // ----------------------
        //
        // Parameters:
        //  - 0..2 - Position of the spacecraft in inertial frame
        //  - 3..5 - Velocity of the spacecraft in inertial frame
        //  - 6..n - station parameters (clock offset, station offsets, pole, prime meridian...)

        // Create the parameter indices map
        final Map<String, Integer> paramIndices = getParameterIndices(states);
        final int nbParams = 6 * states.length + paramIndices.size();
        final SpacecraftState state = states[0];
        final TimeStampedFieldPVCoordinates<Gradient> pva = AbstractMeasurement.getCoordinates(state, 0, nbParams);

        // Compute emission date
        final FieldPVCoordinatesProvider<Gradient> receiverPVProvider = getStation().getFieldPVCoordinatesProvider(nbParams,
                paramIndices);
        final Frame frame = state.getFrame();
        final FieldAbsoluteDate<Gradient> receptionDate = getStation().getCorrectedReceptionDateField(getDate(), nbParams, paramIndices);
        final TimeStampedFieldPVCoordinates<Gradient> receiverPV = receiverPVProvider.getPVCoordinates(receptionDate, frame);
        final FieldPVCoordinatesProvider<Gradient> emitter = AbstractParticipant.extractFieldPVCoordinatesProvider(state, pva);
        final Gradient signalTravelTime = getSignalTravelTimeModel().getFieldAdjustableEmitterComputer(receptionDate.getField(),
                        emitter).computeDelay(receptionDate, receiverPV.getPosition(), receptionDate, frame);
        final FieldAbsoluteDate<Gradient> emissionDate = receptionDate.shiftedBy(signalTravelTime.negate());

        // Compute azimuth and elevation
        final BodyShape bodyShape = getStation().getBaseFrame().getParentShape();
        final FieldGeodeticPoint<Gradient> geodeticPoint = bodyShape.transform(receiverPV.getPosition(), frame, receptionDate);
        final TopocentricAzElModel measurementModel = new TopocentricAzElModel(frame, bodyShape,
                getSignalTravelTimeModel().getWarmedUpModel());
        final Gradient[] azEl = measurementModel.value(geodeticPoint, receptionDate, emitter, emissionDate);

        // Prepare the estimation
        final double shift = emissionDate.toAbsoluteDate().durationFrom(state);
        final SpacecraftState shiftedState = state.shiftedBy(shift);
        final EstimatedMeasurement<AngularAzEl> estimated = new EstimatedMeasurement<>(this, iteration, evaluation,
                new SpacecraftState[] { shiftedState },
                new TimeStampedPVCoordinates[] {shiftedState.getPVCoordinates(), receiverPV.toTimeStampedPVCoordinates() });
        fillEstimatedMeasurement(azEl[0], azEl[1], paramIndices, estimated);
        return estimated;
    }

    /** Calculate the Line Of Sight of the given measurement.
     * @param outputFrame output frame of the line of sight vector
     * @return Vector3D the line of Sight of the measurement
     */
    public Vector3D getObservedLineOfSight(final Frame outputFrame) {
        return getStation().getBaseFrame().getStaticTransformTo(outputFrame, getDate())
            .transformVector(new Vector3D(MathUtils.SEMI_PI - getObservedValue()[0], getObservedValue()[1]));
    }

}
