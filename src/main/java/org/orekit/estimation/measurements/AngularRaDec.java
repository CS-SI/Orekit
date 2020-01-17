/* Copyright 2002-2020 CS Group
 * Licensed to CS Group (CS) under one or more
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
import org.hipparchus.analysis.differentiation.DSFactory;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.util.MathUtils;
import org.orekit.frames.FieldTransform;
import org.orekit.frames.Frame;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TimeStampedFieldPVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

/** Class modeling an Right Ascension - Declination measurement from a ground point (station, telescope).
 * The angles are given in an inertial reference frame.
 * The motion of the spacecraft during the signal flight time is taken into
 * account. The date of the measurement corresponds to the reception on
 * ground of the reflected signal.
 *
 * @author Thierry Ceolin
 * @author Maxime Journot
 * @since 9.0
 */
public class AngularRaDec extends AbstractMeasurement<AngularRaDec> {

    /** Ground station from which measurement is performed. */
    private final GroundStation station;

    /** Reference frame in which the right ascension - declination angles are given. */
    private final Frame referenceFrame;

    /** Simple constructor.
     * @param station ground station from which measurement is performed
     * @param referenceFrame Reference frame in which the right ascension - declination angles are given
     * @param date date of the measurement
     * @param angular observed value
     * @param sigma theoretical standard deviation
     * @param baseWeight base weight
     * @param satellite satellite related to this measurement
     * @since 9.3
     */
    public AngularRaDec(final GroundStation station, final Frame referenceFrame, final AbsoluteDate date,
                        final double[] angular, final double[] sigma, final double[] baseWeight,
                        final ObservableSatellite satellite) {
        super(date, angular, sigma, baseWeight, Arrays.asList(satellite));
        addParameterDriver(station.getClockOffsetDriver());
        addParameterDriver(station.getEastOffsetDriver());
        addParameterDriver(station.getNorthOffsetDriver());
        addParameterDriver(station.getZenithOffsetDriver());
        addParameterDriver(station.getPrimeMeridianOffsetDriver());
        addParameterDriver(station.getPrimeMeridianDriftDriver());
        addParameterDriver(station.getPolarOffsetXDriver());
        addParameterDriver(station.getPolarDriftXDriver());
        addParameterDriver(station.getPolarOffsetYDriver());
        addParameterDriver(station.getPolarDriftYDriver());
        this.station        = station;
        this.referenceFrame = referenceFrame;
    }

    /** Get the ground station from which measurement is performed.
     * @return ground station from which measurement is performed
     */
    public GroundStation getStation() {
        return station;
    }

    /** Get the reference frame in which the right ascension - declination angles are given.
     * @return reference frame in which the right ascension - declination angles are given
     */
    public Frame getReferenceFrame() {
        return referenceFrame;
    }

    /** {@inheritDoc} */
    @Override
    protected EstimatedMeasurement<AngularRaDec> theoreticalEvaluation(final int iteration, final int evaluation,
                                                                       final SpacecraftState[] states) {

        final SpacecraftState state = states[0];

        // Right Ascension/elevation (in reference frame )derivatives are computed with respect to spacecraft state in inertial frame
        // and station parameters
        // ----------------------
        //
        // Parameters:
        //  - 0..2 - Position of the spacecraft in inertial frame
        //  - 3..5 - Velocity of the spacecraft in inertial frame
        //  - 6..n - station parameters (clock offset, station offsets, pole, prime meridian...)

        // Get the number of parameters used for derivation
        // Place the selected drivers into a map
        int nbParams = 6;
        final Map<String, Integer> indices = new HashMap<>();
        for (ParameterDriver driver : getParametersDrivers()) {
            if (driver.isSelected()) {
                indices.put(driver.getName(), nbParams++);
            }
        }
        final DSFactory                          factory = new DSFactory(nbParams, 1);
        final Field<DerivativeStructure>         field   = factory.getDerivativeField();
        final FieldVector3D<DerivativeStructure> zero    = FieldVector3D.getZero(field);

        // Coordinates of the spacecraft expressed as a derivative structure
        final TimeStampedFieldPVCoordinates<DerivativeStructure> pvaDS = getCoordinates(state, 0, factory);

        // Transform between station and inertial frame, expressed as a derivative structure
        // The components of station's position in offset frame are the 3 last derivative parameters
        final FieldTransform<DerivativeStructure> offsetToInertialDownlink =
                        station.getOffsetToInertial(state.getFrame(), getDate(), factory, indices);
        final FieldAbsoluteDate<DerivativeStructure> downlinkDateDS =
                        offsetToInertialDownlink.getFieldDate();

        // Station position/velocity in inertial frame at end of the downlink leg
        final TimeStampedFieldPVCoordinates<DerivativeStructure> stationDownlink =
                        offsetToInertialDownlink.transformPVCoordinates(new TimeStampedFieldPVCoordinates<>(downlinkDateDS,
                                                                                                            zero, zero, zero));

        // Compute propagation times
        // (if state has already been set up to pre-compensate propagation delay,
        //  we will have delta == tauD and transitState will be the same as state)

        // Downlink delay
        final DerivativeStructure tauD = signalTimeOfFlight(pvaDS, stationDownlink.getPosition(), downlinkDateDS);

        // Transit state
        final DerivativeStructure   delta        = downlinkDateDS.durationFrom(state.getDate());
        final DerivativeStructure   deltaMTauD   = tauD.negate().add(delta);
        final SpacecraftState       transitState = state.shiftedBy(deltaMTauD.getValue());

        // Transit state (re)computed with derivative structures
        final TimeStampedFieldPVCoordinates<DerivativeStructure> transitStateDS = pvaDS.shiftedBy(deltaMTauD);

        // Station-satellite vector expressed in inertial frame
        final FieldVector3D<DerivativeStructure> staSatInertial = transitStateDS.getPosition().subtract(stationDownlink.getPosition());

        // Field transform from inertial to reference frame at station's reception date
        final FieldTransform<DerivativeStructure> inertialToReferenceDownlink =
                        state.getFrame().getTransformTo(referenceFrame, downlinkDateDS);

        // Station-satellite vector in reference frame
        final FieldVector3D<DerivativeStructure> staSatReference = inertialToReferenceDownlink.transformPosition(staSatInertial);

        // Compute right ascension and declination
        final DerivativeStructure baseRightAscension = staSatReference.getAlpha();
        final double              twoPiWrap          = MathUtils.normalizeAngle(baseRightAscension.getReal(),
                                                                                getObservedValue()[0]) - baseRightAscension.getReal();
        final DerivativeStructure rightAscension     = baseRightAscension.add(twoPiWrap);
        final DerivativeStructure declination        = staSatReference.getDelta();

        // Prepare the estimation
        final EstimatedMeasurement<AngularRaDec> estimated =
                        new EstimatedMeasurement<>(this, iteration, evaluation,
                                                   new SpacecraftState[] {
                                                       transitState
                                                   }, new TimeStampedPVCoordinates[] {
                                                       transitStateDS.toTimeStampedPVCoordinates(),
                                                       stationDownlink.toTimeStampedPVCoordinates()
                                                   });

        // azimuth - elevation values
        estimated.setEstimatedValue(rightAscension.getValue(), declination.getValue());

        // Partial derivatives of right ascension/declination in reference frame with respect to state
        // (beware element at index 0 is the value, not a derivative)
        final double[] raDerivatives  = rightAscension.getAllDerivatives();
        final double[] decDerivatives = declination.getAllDerivatives();
        estimated.setStateDerivatives(0,
                                      Arrays.copyOfRange(raDerivatives, 1, 7), Arrays.copyOfRange(decDerivatives, 1, 7));

        // Partial derivatives with respect to parameters
        // (beware element at index 0 is the value, not a derivative)
        for (final ParameterDriver driver : getParametersDrivers()) {
            final Integer index = indices.get(driver.getName());
            if (index != null) {
                estimated.setParameterDerivatives(driver, raDerivatives[index + 1], decDerivatives[index + 1]);
            }
        }

        return estimated;
    }
}
