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

import java.util.Arrays;

import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.MathUtils;
import org.orekit.frames.FieldStaticTransform;
import org.orekit.frames.Frame;
import org.orekit.frames.StaticTransform;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TimeSpanMap.Span;
import org.orekit.utils.TimeStampedFieldPVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

/** Class modeling a Right Ascension - Declination measurement from a ground point (station, telescope).
 * The angles are given in an inertial reference frame.
 * The motion of the spacecraft during the signal flight time is taken into
 * account. The date of the measurement corresponds to the reception on
 * ground of the reflected signal.
 *
 * @author Thierry Ceolin
 * @author Maxime Journot
 * @since 9.0
 */
public class AngularRaDec extends GroundReceiverMeasurement<AngularRaDec> {

    /** Type of the measurement. */
    public static final String MEASUREMENT_TYPE = "AngularRaDec";

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
        super(station, false, date, angular, sigma, baseWeight, satellite);
        this.referenceFrame = referenceFrame;
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
                                                                                             final SpacecraftState[] states) {

        final GroundReceiverCommonParametersWithoutDerivatives common = computeCommonParametersWithout(states[0]);
        final TimeStampedPVCoordinates transitPV = common.getTransitPV();

        // Station-satellite vector expressed in inertial frame
        final Vector3D staSatInertial = transitPV.getPosition().subtract(common.getStationDownlink().getPosition());

        // Field transform from inertial to reference frame at station's reception date
        final StaticTransform inertialToReferenceDownlink = common.getState().getFrame().
                                                            getStaticTransformTo(referenceFrame, common.getStationDownlink().getDate());

        // Station-satellite vector in reference frame
        final Vector3D staSatReference = inertialToReferenceDownlink.transformVector(staSatInertial);

        // Compute right ascension and declination
        final double baseRightAscension = staSatReference.getAlpha();
        final double twoPiWrap          = MathUtils.normalizeAngle(baseRightAscension, getObservedValue()[0]) - baseRightAscension;
        final double rightAscension     = baseRightAscension + twoPiWrap;
        final double declination        = staSatReference.getDelta();

        // Prepare the estimation
        final EstimatedMeasurementBase<AngularRaDec> estimated =
                        new EstimatedMeasurementBase<>(this, iteration, evaluation,
                                                       new SpacecraftState[] {
                                                           common.getTransitState()
                                                       }, new TimeStampedPVCoordinates[] {
                                                           transitPV,
                                                           common.getStationDownlink()
                                                       });

        // azimuth - elevation values
        estimated.setEstimatedValue(rightAscension, declination);

        return estimated;

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
        final GroundReceiverCommonParametersWithDerivatives common = computeCommonParametersWithDerivatives(state);
        final TimeStampedFieldPVCoordinates<Gradient> transitPV = common.getTransitPV();

        // Station-satellite vector expressed in inertial frame
        final FieldVector3D<Gradient> staSatInertial = transitPV.getPosition().subtract(common.getStationDownlink().getPosition());

        // Field transform from inertial to reference frame at station's reception date
        final FieldStaticTransform<Gradient> inertialToReferenceDownlink =
                        state.getFrame().getStaticTransformTo(referenceFrame, common.getStationDownlink().getDate());

        // Station-satellite vector in reference frame
        final FieldVector3D<Gradient> staSatReference = inertialToReferenceDownlink.transformVector(staSatInertial);

        // Compute right ascension and declination
        final Gradient baseRightAscension = staSatReference.getAlpha();
        final double   twoPiWrap          = MathUtils.normalizeAngle(baseRightAscension.getReal(),
                                                                                getObservedValue()[0]) - baseRightAscension.getReal();
        final Gradient rightAscension     = baseRightAscension.add(twoPiWrap);
        final Gradient declination        = staSatReference.getDelta();

        // Prepare the estimation
        final EstimatedMeasurement<AngularRaDec> estimated =
                        new EstimatedMeasurement<>(this, iteration, evaluation,
                                                   new SpacecraftState[] {
                                                       common.getTransitState()
                                                   }, new TimeStampedPVCoordinates[] {
                                                       transitPV.toTimeStampedPVCoordinates(),
                                                       common.getStationDownlink().toTimeStampedPVCoordinates()
                                                   });

        // azimuth - elevation values
        estimated.setEstimatedValue(rightAscension.getValue(), declination.getValue());

        // Partial derivatives of right ascension/declination in reference frame with respect to state
        // (beware element at index 0 is the value, not a derivative)
        final double[] raDerivatives  = rightAscension.getGradient();
        final double[] decDerivatives = declination.getGradient();
        estimated.setStateDerivatives(0,
                                      Arrays.copyOfRange(raDerivatives, 0, 6), Arrays.copyOfRange(decDerivatives, 0, 6));

        // Partial derivatives with respect to parameters
        // (beware element at index 0 is the value, not a derivative)
        for (final ParameterDriver driver : getParametersDrivers()) {
            for (Span<String> span = driver.getNamesSpanMap().getFirstSpan(); span != null; span = span.next()) {
                final Integer index = common.getIndices().get(span.getData());
                if (index != null) {
                    estimated.setParameterDerivatives(driver, span.getStart(), raDerivatives[index], decDerivatives[index]);
                }
            }
        }

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
