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
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.estimation.measurements.model.RaDecModel;
import org.orekit.frames.Frame;
import org.orekit.propagation.SpacecraftState;
import org.orekit.signal.FieldSignalReceptionCondition;
import org.orekit.signal.SignalReceptionCondition;
import org.orekit.signal.SignalTravelTimeModel;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldPVCoordinatesProvider;
import org.orekit.utils.PVCoordinatesProvider;
import org.orekit.utils.TimeStampedFieldPVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

/** Class modeling a Right Ascension - Declination measurement from an optical sensor, typically via astrometry.
 * The angles are given using the axes of an inertial reference frame.
 * The date of the measurement corresponds to the reception of the reflected signal.
 *
 * @author Thierry Ceolin
 * @author Maxime Journot
 * @since 9.0
 */
public class AngularRaDec extends AngularMeasurement<AngularRaDec> {

    /** Type of the measurement. */
    public static final String MEASUREMENT_TYPE = "AngularRaDec";

    /** Reference frame in which the right ascension - declination angles are given. */
    private final Frame referenceFrame;

    /** Observer that receives signal from satellite. */
    private final Observer observer;

    /** Perfect measurement model. */
    private final RaDecModel measurementModel;

    /** Simple constructor using default light time delay.
     * @param observer sensor from which measurement is performed
     * @param referenceFrame Reference frame in which the right ascension - declination angles are given
     * @param date date of the measurement
     * @param angular observed value
     * @param sigma theoretical standard deviation
     * @param baseWeight base weight
     * @param satellite satellite related to this measurement
     * @since 9.3
     */
    public AngularRaDec(final Observer observer, final Frame referenceFrame, final AbsoluteDate date,
                        final double[] angular, final double[] sigma, final double[] baseWeight,
                        final ObservableSatellite satellite) {
        this(observer, referenceFrame, date, angular, new MeasurementQuality(sigma, baseWeight), new SignalTravelTimeModel(), satellite);
    }

    /** Constructor.
     * @param observer sensor from which measurement is performed
     * @param referenceFrame Reference frame in which the right ascension - declination angles are given
     * @param date date of the measurement
     * @param angular observed value
     * @param measurementQuality measurement quality as used in estimation (in Orekit, the crossed-terms
     *                           of the covariance matrix are only used by Kalman filters, not least squares)
     * @param signalTravelTimeModel signal travel time model
     * @param satellite satellite related to this measurement
     * @since 14.0
     */
    public AngularRaDec(final Observer observer, final Frame referenceFrame, final AbsoluteDate date,
                        final double[] angular, final MeasurementQuality measurementQuality,
                        final SignalTravelTimeModel signalTravelTimeModel, final ObservableSatellite satellite) {
        super(date, angular, measurementQuality, signalTravelTimeModel, satellite);
        this.referenceFrame = referenceFrame;
        this.measurementModel = new RaDecModel(referenceFrame, getSignalTravelTimeModel().getWarmedUpModel());
        this.observer = observer;
        observer.getParametersDrivers().forEach(this::addParameterDriver);
    }

    /**
     * Getter for the observer.
     * @return observer
     * @since 14.0
     */
    public Observer getObserver() {
        return observer;
    }

    /**
     * Getter for the ground station.
     * @return station
     * @deprecated as of 14.0
     */
    @Deprecated
    public GroundStation getStation() {
        if (observer instanceof GroundStation station) {
            return station;
        } else {
            return null;
        }
    }

    /** Get the reference frame in which the right ascension - declination angles are given.
     * @return reference frame in which the right ascension - declination angles are given
     */
    public Frame getReferenceFrame() {
        return referenceFrame;
    }

    /** {@inheritDoc} */
    @Override
    protected EstimatedMeasurementBase<AngularRaDec> theoreticalEvaluationWithoutDerivatives(final int iteration,
                                                                                             final int evaluation,
                                                                                             final SpacecraftState[] states,
                                                                                             final boolean fillParticipants) {
        // Compute emission date
        final AbsoluteDate receptionDate = observer.getCorrectedReceptionDate(getDate());
        final PVCoordinatesProvider receiver = observer.getPVCoordinatesProvider();
        final SpacecraftState state = states[0];
        final PVCoordinatesProvider emitter = AbstractParticipant.extractPVCoordinatesProvider(state, state.getPVCoordinates());
        final Frame frame = state.getFrame();
        final Vector3D receiverPosition = receiver.getPosition(receptionDate, frame);
        final SignalReceptionCondition receptionCondition = new SignalReceptionCondition(receptionDate,
                receiverPosition, frame);
        final AbsoluteDate emissionDate = computeEmissionDate(receptionCondition, emitter);

        // Evaluate angular measurement model (use state frame to avoid rounding error in case reference one is not Earth-centered)
        final double[] raDec = measurementModel.value(receptionCondition, emitter, emissionDate);

        // Prepare the estimation
        final double shift = emissionDate.durationFrom(state);
        final SpacecraftState shiftedState = state.shiftedBy(shift);
        final EstimatedMeasurementBase<AngularRaDec> estimated = new EstimatedMeasurementBase<>(this, iteration, evaluation,
                new SpacecraftState[] { shiftedState }, fillParticipants ? new TimeStampedPVCoordinates[] {
                        shiftedState.getPVCoordinates(), receiver.getPVCoordinates(receptionDate, frame) } :
                new TimeStampedPVCoordinates[0]);
        estimated.setEstimatedValue(wrapFirstAngle(raDec[0]), raDec[1]);
        return estimated;
    }

    /** {@inheritDoc} */
    @Override
    protected EstimatedMeasurement<AngularRaDec> theoreticalEvaluation(final int iteration, final int evaluation,
                                                                       final SpacecraftState[] states) {
        // Right Ascension/declination (in reference frame) derivatives are computed with respect to spacecraft state in inertial frame
        // and station parameters
        // ----------------------
        //
        // Parameters:
        //  - 0..2 - Position of the spacecraft in inertial frame
        //  - 3..5 - Velocity of the spacecraft in inertial frame
        //  - 6..n - station parameters (clock offset, station offsets, pole, prime meridian...)

        // Create the parameter indices map
        final Map<String, Integer> paramIndices = getParameterIndices(states);
        final int                  nbParams     = 6 * states.length + paramIndices.size();
        final SpacecraftState state = states[0];
        final TimeStampedFieldPVCoordinates<Gradient> pva = AbstractMeasurement.getCoordinates(state, 0, nbParams);

        // Compute emission date
        final FieldAbsoluteDate<Gradient> receptionDate = observer.getCorrectedReceptionDateField(getDate(), nbParams, paramIndices);
        final FieldPVCoordinatesProvider<Gradient> receiver = observer.getFieldPVCoordinatesProvider(nbParams, paramIndices);
        final FieldPVCoordinatesProvider<Gradient> emitter = AbstractParticipant.extractFieldPVCoordinatesProvider(state, pva);
        final Frame                frame        = states[0].getFrame();
        final FieldVector3D<Gradient> receiverPosition = receiver.getPosition(receptionDate, frame);
        final FieldAbsoluteDate<Gradient> emissionDate = computeEmissionDateField(referenceFrame, receiverPosition, receptionDate, emitter);

        // Evaluate angular measurement model (use state frame to avoid rounding error in case reference one is not Earth-centered)
        final FieldSignalReceptionCondition<Gradient> receptionCondition = new FieldSignalReceptionCondition<>(receptionDate,
                receiverPosition, frame);
        final Gradient[] raDec = measurementModel.value(receptionCondition, emitter, emissionDate);

        // Prepare the estimation
        final double shift = emissionDate.toAbsoluteDate().durationFrom(state);
        final SpacecraftState shiftedState = state.shiftedBy(shift);
        final EstimatedMeasurement<AngularRaDec> estimated = new EstimatedMeasurement<>(this, iteration, evaluation,
                new SpacecraftState[] { shiftedState },
                new TimeStampedPVCoordinates[] { shiftedState.getPVCoordinates(),
                getObserver().getPVCoordinatesProvider().getPVCoordinates(receptionDate.toAbsoluteDate(), frame)});
        fillEstimatedMeasurement(raDec[0], raDec[1], paramIndices, estimated);
        return estimated;
    }

    /** Calculate the Line Of Sight of the given measurement.
     * @param outputFrame output frame of the line of sight vector
     * @return Vector3D the line of Sight of the measurement
     * @since 12.0
     */
    public Vector3D getObservedLineOfSight(final Frame outputFrame) {
        return referenceFrame.getStaticTransformTo(outputFrame, getDate())
            .transformVector(new Vector3D(getObservedValue()[0], getObservedValue()[1]));
    }
}
