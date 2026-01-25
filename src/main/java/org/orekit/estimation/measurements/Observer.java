/* Copyright 2002-2026 Brianna Aubin
 * Licensed to Hawkeye 360 (HE360) under one or more
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.analysis.differentiation.GradientField;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.estimation.measurements.signal.FieldSignalTravelTimeAdjustableEmitter;
import org.orekit.estimation.measurements.signal.SignalTravelTimeAdjustableEmitter;
import org.orekit.frames.FieldTransform;
import org.orekit.frames.Frame;
import org.orekit.frames.Transform;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.clocks.ClockOffset;
import org.orekit.time.clocks.FieldClockOffset;
import org.orekit.time.clocks.QuadraticFieldClockModel;
import org.orekit.utils.FieldPVCoordinatesProvider;
import org.orekit.utils.PVCoordinatesProvider;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TimeSpanMap.Span;
import org.orekit.utils.TimeStampedFieldPVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

/** Abstract interface that contains those methods necessary
 *  for both space and ground-based satellite observers.
 *
 * @author Brianna Aubin
 * @since 14.0
 */
public interface Observer extends MeasurementObject {

    enum ObserverType {
        /** Indicates a ground-based observation station. */
        GROUNDSTATION,

        /** Indicates a space-based observer. */
        SATELLITE;
    }

    /** Get the type of object being used in measurement observations.
     * @return string value
=     */
    ObserverType getObserverType();

    /** Return the PVCoordinatesProvider.
     * @return pos/vel coordinates provider
     */
    PVCoordinatesProvider getPVCoordinatesProvider();

    /** Return the FieldPVCoordinatesProvider.
     * @param freeParameters number of estimated parameters
     * @param parameterIndices indices of the estimated parameters in derivatives computations, must be driver
     * @return pos/vel coordinates provider for values with Gradient field
     * @since 14.0
     */
    FieldPVCoordinatesProvider<Gradient> getFieldPVCoordinatesProvider(int freeParameters,
                                                                       Map<String, Integer> parameterIndices);

    /** Return the time-stamped PV coordinates.
     * @param date date of output coordinates
     * @param frame desired frame for output coordinates
     * @return observer position vector
     * @since 14.0
     */
    default Vector3D getPosition(AbsoluteDate date, Frame frame) {
        return getPVCoordinates(date, frame).getPosition();
    }

    /** Return the time-stamped PV coordinates.
     * @param date date of output coordinates
     * @param frame desired frame for output coordinates
     * @return time-stamped observer pos/vel values
     * @since 14.0
     */
    default TimeStampedPVCoordinates getPVCoordinates(AbsoluteDate date, Frame frame) {
        return getPVCoordinatesProvider().getPVCoordinates(date, frame);
    }

    /** Get the transform between offset frame and inertial frame.
     * <p>
     * The offset frame takes the <em>current</em> position offset,
     * polar motion and the meridian shift into account. The frame
     * returned is disconnected from later changes in the parameters.
     * When the {@link ParameterDriver parameters} managing these
     * offsets are changed, the method must be called again to retrieve
     * a new offset frame.
     * </p>
     * @param inertial inertial frame to transform to
     * @param date date of the transform
     * @param clockOffsetAlreadyApplied if true, the specified {@code date} is as read
     * by the ground station clock (i.e. clock offset <em>not</em> compensated), if false,
     * the specified {@code date} was already compensated and is a physical absolute date
     * @return transform between offset frame and inertial frame, at <em>real</em> measurement
     * date (i.e. with clock, Earth and station offsets applied)
     */
    Transform getOffsetToInertial(Frame inertial, AbsoluteDate date, boolean clockOffsetAlreadyApplied);

    /** Get the transform between offset frame and inertial frame with derivatives.
     * <p>
     * As the East and North vectors are not well defined at pole, the derivatives
     * of these two vectors diverge to infinity as we get closer to the pole.
     * So this method should not be used for stations less than 0.0001 degree from
     * either poles.
     * </p>
     * @param inertial inertial frame to transform to
     * @param clockDate date of the transform, clock offset and its derivatives already compensated
     * @param freeParameters total number of free parameters in the gradient
     * @param indices indices of the estimated parameters in derivatives computations, must be driver
     * span name in map, not driver name or will not give right results (see {@link ParameterDriver#getValue(int, Map)})
     * @return transform between offset frame and inertial frame, at specified date
     */
    default FieldTransform<Gradient> getOffsetToInertial(Frame inertial, AbsoluteDate clockDate,
                                                         int freeParameters, Map<String, Integer> indices) {
        // take clock offset into account
        final Gradient offset = getClockOffsetDriver().getValue(freeParameters, indices, clockDate);
        final FieldAbsoluteDate<Gradient> offsetCompensatedDate = new FieldAbsoluteDate<>(clockDate, offset.negate());

        return getOffsetToInertial(inertial, offsetCompensatedDate, freeParameters, indices);
    }

    /** Get the transform between offset frame and inertial frame with derivatives.
     * <p>
     * As the East and North vectors are not well defined at pole, the derivatives
     * of these two vectors diverge to infinity as we get closer to the pole.
     * So this method should not be used for stations less than 0.0001 degree from
     * either poles.
     * </p>
     * @param inertial inertial frame to transform to
     * @param offsetCompensatedDate date of the transform, clock offset and its derivatives already compensated
     * @param freeParameters total number of free parameters in the gradient
     * @param indices indices of the estimated parameters in derivatives computations, must be driver
     * span name in map, not driver name or will not give right results (see {@link ParameterDriver#getValue(int, Map)})
     * @return transform between offset frame and inertial frame, at specified date
     */
    FieldTransform<Gradient> getOffsetToInertial(Frame inertial, FieldAbsoluteDate<Gradient> offsetCompensatedDate,
                                                 int freeParameters, Map<String, Integer> indices);

    /** Create a map of the free parameter values.
     * @param states list of ObservableSatellite measurement states
     * @param parameterDrivers list of all parameter values for the measurement
     * @return map of the free parameter values
     */
    default Map<String, Integer> getParamaterIndices(SpacecraftState[] states,
                                                     List<ParameterDriver> parameterDrivers) {

        // measurement derivatives are computed with respect to spacecraft state in inertial frame
        // Parameters:
        //  - 6k..6k+2 - Position of spacecraft k (counting k from 0 to nbSat-1) in inertial frame
        //  - 6k+3..6k+5 - Velocity of spacecraft k (counting k from 0 to nbSat-1) in inertial frame
        //  - 6nbSat..n - measurements parameters (clock offset, etc)
        int nbParams = 6 * states.length;
        final Map<String, Integer> paramIndices = new HashMap<>();
        for (ParameterDriver measurementDriver : parameterDrivers) {
            if (measurementDriver.isSelected()) {
                for (Span<String> span = measurementDriver.getNamesSpanMap().getFirstSpan(); span != null; span = span.next()) {
                    paramIndices.put(span.getData(), nbParams++);
                }
            }
        }
        return paramIndices;
    }

    /** Compute common estimation parameters in case where measured object is the
     * receiver of the signal value (e.g. GNSS to ObservableSatellite).
     * @param states state(s) of all measured spacecraft
     * @param localSat satellite whose state is being estimated
     * @param measurementDate date when measurement was taken
     * @param receiverClockOffsetAlreadyApplied if true, the specified {@code date} is as read
     * by the receiver clock (i.e. clock offset <em>not</em> compensated), if false,
     * the specified {@code date} was already compensated and is a physical absolute date
     * @return common parameters
     */
    default CommonParametersWithoutDerivatives computeLocalParametersWithout(SpacecraftState[] states,
                                                                             ObservableSatellite localSat,
                                                                             AbsoluteDate measurementDate,
                                                                             boolean receiverClockOffsetAlreadyApplied) {

        // Coordinates of the observed spacecraft
        final Frame                    frame            = states[0].getFrame();
        final TimeStampedPVCoordinates pvaLocal         = states[0].getPVCoordinates(frame);

        // Clock values of the observed spacecraft and signal receiver
        final ClockOffset              localClock       = localSat.getQuadraticClockModel().getOffset(measurementDate);
        final double                   localClockOffset = localClock.getOffset();

        // take clock offset of receiver (in this case, ObservableSatellite) into account
        final AbsoluteDate arrivalDate = receiverClockOffsetAlreadyApplied ? measurementDate : measurementDate.shiftedBy(-localClockOffset);

        // Coordinates provider of the Observer object providing the signal information
        final PVCoordinatesProvider remotePV = getPVCoordinatesProvider();

        // Downlink delay / determine time-of-emission of signal information from remote object
        final double deltaT = arrivalDate.durationFrom(states[0]);
        final TimeStampedPVCoordinates pvaDownlink = pvaLocal.shiftedBy(deltaT);
        final SignalTravelTimeAdjustableEmitter signalTimeOfFlight = new SignalTravelTimeAdjustableEmitter(remotePV);
        final double tauD = signalTimeOfFlight.computeDelay(arrivalDate, pvaDownlink.getPosition(), arrivalDate, frame);

        // Remote object pos/vel at time of signal emission
        final AbsoluteDate emissionDate = arrivalDate.shiftedBy(-tauD);
        final ClockOffset  remoteClock  = getQuadraticClockModel().getOffset(emissionDate);

        return new CommonParametersWithoutDerivatives(states[0], tauD,
                                                      localClock, remoteClock,
                                                      states[0].shiftedBy(deltaT),
                                                      pvaDownlink,
                                                      remotePV.getPVCoordinates(emissionDate, frame));

    }

    /** Compute common estimation parameters with derivatives when the measured object is the
     * receiver of the signal sent by the Observer.
     * @param states state(s) of all measured spacecraft
     * @param localSat satellite whose state is being estimated
     * @param measurementDate date when measurement was taken
     * @param receiverClockOffsetAlreadyApplied if true, the specified {@code date} is as read
     * @param parameterDrivers list of parameter drivers associated with measurement
     * by the receiver clock (i.e. clock offset <em>not</em> compensated), if false,
     * the specified {@code date} was already compensated and is a physical absolute date
     * @return common parameters
     */
    default CommonParametersWithDerivatives computeLocalParametersWith(SpacecraftState[] states,
                                                                       ObservableSatellite localSat,
                                                                       AbsoluteDate measurementDate,
                                                                       boolean receiverClockOffsetAlreadyApplied,
                                                                       List<ParameterDriver> parameterDrivers)  {
        // Create the parameter indices map
        final Frame                frame        = states[0].getFrame();
        final Map<String, Integer> paramIndices = getParamaterIndices(states, parameterDrivers);
        final int                  nbParams     = 6 * states.length + paramIndices.size();

        // Turn measurement date into FieldAbsoluteDate<Gradient>
        final FieldAbsoluteDate<Gradient> gDate = new FieldAbsoluteDate<>(GradientField.getField(nbParams), measurementDate);

        // Measured satellite object data
        final TimeStampedFieldPVCoordinates<Gradient> pvaLocal         = AbstractMeasurement.getCoordinates(states[0], 0, nbParams);
        final QuadraticFieldClockModel<Gradient>      localClock       = localSat.getQuadraticClockModel().
                                                                         toGradientModel(nbParams, paramIndices, measurementDate);
        final FieldClockOffset<Gradient>              localClockOffset = localClock.getOffset(gDate);

        // take clock offset into account for arrival date
        final FieldAbsoluteDate<Gradient> arrivalDate = receiverClockOffsetAlreadyApplied ?
                                                        gDate : gDate.shiftedBy(localClockOffset.getOffset().negate());

        // Coords provider for observer object that is sending signal
        final FieldPVCoordinatesProvider<Gradient> remotePV = getFieldPVCoordinatesProvider(nbParams, paramIndices);

        // Downlink delay
        final Gradient deltaT = arrivalDate.durationFrom(states[0].getDate());
        final TimeStampedFieldPVCoordinates<Gradient> pvaDownlink = pvaLocal.shiftedBy(deltaT);
        final FieldSignalTravelTimeAdjustableEmitter<Gradient> fieldComputer =
                                        new FieldSignalTravelTimeAdjustableEmitter<>(remotePV);
        final Gradient tauD = fieldComputer.computeDelay(arrivalDate, pvaDownlink.getPosition(), arrivalDate, frame);

        // Remote observer at signal emission time
        final FieldAbsoluteDate<Gradient> emissionDate = arrivalDate.shiftedBy(tauD.negate());
        final QuadraticFieldClockModel<Gradient> remoteClock = getQuadraticFieldClock(nbParams, emissionDate.toAbsoluteDate(), paramIndices);
        final FieldClockOffset<Gradient>  remoteClockOffset = remoteClock.getOffset(emissionDate);

        return new CommonParametersWithDerivatives(states[0], paramIndices, tauD,
                                                   localClockOffset, remoteClockOffset,
                                                   states[0].shiftedBy(deltaT.getValue()),
                                                   pvaDownlink,
                                                   remotePV.getPVCoordinates(emissionDate, frame));

    }

    /** Compute common estimation parameters when remote object is the receiver
     * of the signal (e.g. ObservableSatellite sends signal to measuring ground station).
     * @param states state(s) of all measured spacecraft
     * @param localSat satellite whose state is being estimated
     * @param measurementDate date when measurement was taken
     * @param receiverClockOffsetAlreadyApplied if true, the specified {@code date} is as read
     * by the receiver clock (i.e. clock offset <em>not</em> compensated), if false,
     * the specified {@code date} was already compensated and is a physical absolute date
     * @return common parameters
     */
    default CommonParametersWithoutDerivatives computeRemoteParametersWithout(SpacecraftState[] states,
                                                                              ObservableSatellite localSat,
                                                                              AbsoluteDate measurementDate,
                                                                              boolean receiverClockOffsetAlreadyApplied) {

        // Coordinates of the measured spacecraft
        final Frame                    frame = states[0].getFrame();
        final TimeStampedPVCoordinates pva   = states[0].getPVCoordinates();

        // transform between remote observer frame and inertial frame
        final Transform    offsetToInertialDownlink = getOffsetToInertial(frame, measurementDate, receiverClockOffsetAlreadyApplied);
        final AbsoluteDate downlinkDate             = offsetToInertialDownlink.getDate();
        final ClockOffset  localClockOffset         = localSat.getQuadraticClockModel().getOffset(measurementDate);

        // Observer position in inertial frame at end of the downlink leg
        final TimeStampedPVCoordinates origin = new TimeStampedPVCoordinates(downlinkDate,
                                                                             Vector3D.ZERO, Vector3D.ZERO, Vector3D.ZERO);
        final TimeStampedPVCoordinates satelliteDownlink = offsetToInertialDownlink.transformPVCoordinates(origin);

        // Coordinates provider for emitting object (observed spacecraft)
        final PVCoordinatesProvider pvCoordinatesProvider = AbstractMeasurementObject.extractPVCoordinatesProvider(states[0], pva);

        // Downlink delay / determine time of emission of signal by ObservableSatellite
        final SignalTravelTimeAdjustableEmitter signalTimeOfFlight = new SignalTravelTimeAdjustableEmitter(pvCoordinatesProvider);
        final double tauD = signalTimeOfFlight.computeDelay(pva.getDate(), satelliteDownlink.getPosition(), downlinkDate, frame);

        // Transit state & Transit state (re)computed with gradients
        final double          delta             = downlinkDate.durationFrom(states[0].getDate());
        final double          deltaMTauD        = delta - tauD;
        final SpacecraftState transitState      = states[0].shiftedBy(deltaMTauD);
        final ClockOffset     remoteClockOffset = getQuadraticClockModel().getOffset(measurementDate);

        return new CommonParametersWithoutDerivatives(states[0], tauD,
                                                      localClockOffset, remoteClockOffset,
                                                      transitState, transitState.getPVCoordinates(),
                                                      satelliteDownlink);
    }

    /** Compute common estimation parameters with derivative when remote object
     * is the receiver of the signal.
     * @param states state(s) of all measured spacecraft
     * @param localSat satellite whose state is being estimated
     * @param measurementDate date when measurement was taken
     * @param parameterDrivers list of parameter drivers associated with measurement
     * @return common parameters
     */
    default CommonParametersWithDerivatives computeRemoteParametersWith(SpacecraftState[] states,
                                                                        ObservableSatellite localSat,
                                                                        AbsoluteDate measurementDate,
                                                                        List<ParameterDriver> parameterDrivers) {

        // Create the parameter indices map
        final Frame                frame        = states[0].getFrame();
        final Map<String, Integer> paramIndices = getParamaterIndices(states, parameterDrivers);
        final int                  nbParams     = 6 * states.length + paramIndices.size();

        // Coordinates of the spacecraft expressed as a gradient
        final TimeStampedFieldPVCoordinates<Gradient> pva = AbstractMeasurement.getCoordinates(states[0], 0, nbParams);

        // transform between Observer object and inertial frame, expressed as a gradient
        // The components of the Observer's position in offset frame are the 3 last derivative parameters
        final FieldTransform<Gradient> offsetToInertialDownlink =
                        getOffsetToInertial(frame, measurementDate, nbParams, paramIndices);
        final FieldAbsoluteDate<Gradient> downlinkDate = offsetToInertialDownlink.getFieldDate();

        // Get local satellite clock offset
        final QuadraticFieldClockModel<Gradient> localClock = localSat.getQuadraticClockModel().
                                                              toGradientModel(nbParams, paramIndices, measurementDate);
        final FieldClockOffset<Gradient>   localClockOffset = localClock.getOffset(downlinkDate);

        // Observer position in inertial frame at end of the downlink leg
        final GradientField           field = GradientField.getField(nbParams);
        final FieldVector3D<Gradient> zero  = FieldVector3D.getZero(field);
        final TimeStampedFieldPVCoordinates<Gradient> satelliteDownlink =
                        offsetToInertialDownlink.transformPVCoordinates(new TimeStampedFieldPVCoordinates<>(downlinkDate,
                                                                                                            zero, zero, zero));

        // Form coordinates provider
        final FieldPVCoordinatesProvider<Gradient> fieldPVCoordinatesProvider = AbstractMeasurementObject.extractFieldPVCoordinatesProvider(states[0], pva);

        // Downlink delay
        final FieldSignalTravelTimeAdjustableEmitter<Gradient> fieldComputer = new FieldSignalTravelTimeAdjustableEmitter<>(fieldPVCoordinatesProvider);
        final Gradient tauD = fieldComputer.computeDelay(pva.getDate(), satelliteDownlink.getPosition(), downlinkDate, frame);

        // Transit state & Transit state (re)computed with gradients
        final Gradient        delta        = downlinkDate.durationFrom(states[0].getDate());
        final Gradient        deltaMTauD   = tauD.negate().add(delta);
        final SpacecraftState transitState = states[0].shiftedBy(deltaMTauD.getValue());
        final FieldAbsoluteDate<Gradient> fieldDate = new FieldAbsoluteDate<>(field, states[0].getDate()).shiftedBy(deltaMTauD);
        final TimeStampedFieldPVCoordinates<Gradient> transitPV = fieldPVCoordinatesProvider.getPVCoordinates(fieldDate, frame);

        // Get remote clock offset at time of measurement reception
        final QuadraticFieldClockModel<Gradient> remoteClock       = getQuadraticFieldClock(nbParams, downlinkDate.toAbsoluteDate(), paramIndices);
        final FieldClockOffset<Gradient>         remoteClockOffset = remoteClock.getOffset(downlinkDate);

        return new CommonParametersWithDerivatives(states[0], paramIndices, tauD,
                                                   localClockOffset, remoteClockOffset,
                                                   transitState, transitPV, satelliteDownlink);
    }

}
