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
import org.hipparchus.analysis.differentiation.GradientField;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.orekit.frames.Frame;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TimeSpanMap.Span;
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
public class AngularAzEl extends GroundReceiverMeasurement<AngularAzEl> {

    /** Type of the measurement. */
    public static final String MEASUREMENT_TYPE = "AngularAzEl";

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
        super(station, false, date, angular, sigma, baseWeight, satellite);
    }

    /** {@inheritDoc} */
    @Override
    protected EstimatedMeasurementBase<AngularAzEl> theoreticalEvaluationWithoutDerivatives(final int iteration,
                                                                                            final int evaluation,
                                                                                            final SpacecraftState[] states) {

        final GroundReceiverCommonParametersWithoutDerivatives common = computeCommonParametersWithout(states[0]);
        final TimeStampedPVCoordinates transitPV = common.getTransitPV();

        // Station topocentric frame (east-north-zenith) in inertial frame expressed as Gradient
        final Vector3D east   = common.getOffsetToInertialDownlink().transformVector(Vector3D.PLUS_I);
        final Vector3D north  = common.getOffsetToInertialDownlink().transformVector(Vector3D.PLUS_J);
        final Vector3D zenith = common.getOffsetToInertialDownlink().transformVector(Vector3D.PLUS_K);

        // Station-satellite vector expressed in inertial frame
        final Vector3D staSat = transitPV.getPosition().subtract(common.getStationDownlink().getPosition());

        // Compute azimuth/elevation
        final double baseAzimuth = FastMath.atan2(staSat.dotProduct(east), staSat.dotProduct(north));
        final double twoPiWrap   = MathUtils.normalizeAngle(baseAzimuth, getObservedValue()[0]) - baseAzimuth;
        final double azimuth     = baseAzimuth + twoPiWrap;
        final double elevation   = FastMath.asin(staSat.dotProduct(zenith) / staSat.getNorm());

        // Prepare the estimation
        final EstimatedMeasurementBase<AngularAzEl> estimated =
                        new EstimatedMeasurementBase<>(this, iteration, evaluation,
                                                       new SpacecraftState[] {
                                                           common.getTransitState()
                                                       }, new TimeStampedPVCoordinates[] {
                                                           transitPV,
                                                           common.getStationDownlink()
                                                       });

        // azimuth - elevation values
        estimated.setEstimatedValue(azimuth, elevation);

        return estimated;

    }

    /** {@inheritDoc} */
    @Override
    protected EstimatedMeasurement<AngularAzEl> theoreticalEvaluation(final int iteration, final int evaluation,
                                                                      final SpacecraftState[] states) {

        final SpacecraftState state = states[0];

        // Azimuth/elevation derivatives are computed with respect to spacecraft state in inertial frame
        // and station parameters
        // ----------------------
        //
        // Parameters:
        //  - 0..2 - Position of the spacecraft in inertial frame
        //  - 3..5 - Velocity of the spacecraft in inertial frame
        //  - 6..n - station parameters (clock offset, station offsets, pole, prime meridian...)
        final GroundReceiverCommonParametersWithDerivatives common = computeCommonParametersWithDerivatives(state);
        final TimeStampedFieldPVCoordinates<Gradient> transitPV = common.getTransitPV();

        // Station topocentric frame (east-north-zenith) in inertial frame expressed as Gradient
        final GradientField field = common.getTauD().getField();
        final FieldVector3D<Gradient> east   = common.getOffsetToInertialDownlink().transformVector(FieldVector3D.getPlusI(field));
        final FieldVector3D<Gradient> north  = common.getOffsetToInertialDownlink().transformVector(FieldVector3D.getPlusJ(field));
        final FieldVector3D<Gradient> zenith = common.getOffsetToInertialDownlink().transformVector(FieldVector3D.getPlusK(field));

        // Station-satellite vector expressed in inertial frame
        final FieldVector3D<Gradient> staSat = transitPV.getPosition().subtract(common.getStationDownlink().getPosition());

        // Compute azimuth/elevation
        final Gradient baseAzimuth = staSat.dotProduct(east).atan2(staSat.dotProduct(north));
        final double   twoPiWrap   = MathUtils.normalizeAngle(baseAzimuth.getReal(), getObservedValue()[0]) -
                                                baseAzimuth.getReal();
        final Gradient azimuth     = baseAzimuth.add(twoPiWrap);
        final Gradient elevation   = staSat.dotProduct(zenith).divide(staSat.getNorm()).asin();

        // Prepare the estimation
        final EstimatedMeasurement<AngularAzEl> estimated =
                        new EstimatedMeasurement<>(this, iteration, evaluation,
                                                   new SpacecraftState[] {
                                                       common.getTransitState()
                                                   }, new TimeStampedPVCoordinates[] {
                                                       transitPV.toTimeStampedPVCoordinates(),
                                                       common.getStationDownlink().toTimeStampedPVCoordinates()
                                                   });

        // azimuth - elevation values
        estimated.setEstimatedValue(azimuth.getValue(), elevation.getValue());

        // Partial derivatives of azimuth/elevation with respect to state
        // (beware element at index 0 is the value, not a derivative)
        final double[] azDerivatives = azimuth.getGradient();
        final double[] elDerivatives = elevation.getGradient();
        estimated.setStateDerivatives(0,
                                      Arrays.copyOfRange(azDerivatives, 0, 6), Arrays.copyOfRange(elDerivatives, 0, 6));

        // Set partial derivatives with respect to parameters
        // (beware element at index 0 is the value, not a derivative)
        for (final ParameterDriver driver : getParametersDrivers()) {

            for (Span<String> span = driver.getNamesSpanMap().getFirstSpan(); span != null; span = span.next()) {
                final Integer index = common.getIndices().get(span.getData());
                if (index != null) {
                    estimated.setParameterDerivatives(driver, span.getStart(), azDerivatives[index], elDerivatives[index]);
                }
            }
        }

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
