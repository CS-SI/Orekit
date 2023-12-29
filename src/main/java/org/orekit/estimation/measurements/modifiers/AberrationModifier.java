/* Copyright 2023 Mark Rutten
 * Licensed to CS GROUP (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * Mark Rutten licenses this file to You under the Apache License, Version 2.0
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
package org.orekit.estimation.measurements.modifiers;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hipparchus.Field;
import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.analysis.differentiation.GradientField;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.DataContext;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.estimation.measurements.AngularRaDec;
import org.orekit.estimation.measurements.EstimatedMeasurement;
import org.orekit.estimation.measurements.EstimatedMeasurementBase;
import org.orekit.estimation.measurements.EstimationModifier;
import org.orekit.estimation.measurements.GroundStation;
import org.orekit.frames.FieldTransform;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TimeSpanMap;
import org.orekit.utils.TimeStampedFieldPVCoordinates;


/**
 * Class modifying theoretical angular measurement with (the inverse of) stellar aberration.
 * <p>
 * This class implements equation 3.252-3 from Seidelmann, "Explanatory Supplement to the Astronmical Almanac", 1992.
 *
 * @author Mark Rutten
 */
public class AberrationModifier implements EstimationModifier<AngularRaDec> {

    /** Data context. */
    private final DataContext dataContext;

    /** Empty constructor.
     * <p>
     * This constructor uses the {@link DefaultDataContext default data context}
     * </p>
     * @since 12.0
     * @see #AberrationModifier(DataContext)
     */
    @DefaultDataContext
    public AberrationModifier() {
        this(DataContext.getDefault());
    }

    /**
     * Constructor.
     * @param dataContext data context
     * @since 12.0.1
     */
    public AberrationModifier(final DataContext dataContext) {
        this.dataContext = dataContext;
    }

    /**
     * Natural to proper correction for aberration of light.
     *
     * @param naturalRaDec the "natural" direction (in barycentric coordinates)
     * @param station      the observer ground station
     * @param date         the date of the measurement
     * @param frame        the frame of the measurement
     * @return the "proper" direction (station-relative coordinates)
     */
    @DefaultDataContext
    public static double[] naturalToProper(final double[] naturalRaDec, final GroundStation station,
                                           final AbsoluteDate date, final Frame frame) {
        return naturalToProper(naturalRaDec, station, date, frame, DataContext.getDefault());
    }

    /**
     * Natural to proper correction for aberration of light.
     *
     * @param naturalRaDec the "natural" direction (in barycentric coordinates)
     * @param station      the observer ground station
     * @param date         the date of the measurement
     * @param frame        the frame of the measurement
     * @param context      the data context
     * @return the "proper" direction (station-relative coordinates)
     * @since 12.0.1
     */
    public static double[] naturalToProper(final double[] naturalRaDec, final GroundStation station,
                                           final AbsoluteDate date, final Frame frame, final DataContext context) {

        ensureFrameIsPseudoInertial(frame);

        // Velocity of station relative to barycentre (units of c)
        final PVCoordinates baryPV = context.getCelestialBodies().getSolarSystemBarycenter().getPVCoordinates(date, frame);
        final PVCoordinates stationPV = station.getBaseFrame().getPVCoordinates(date, frame);
        final Vector3D stationBaryVel = stationPV.getVelocity()
                .subtract(baryPV.getVelocity())
                .scalarMultiply(1.0 / Constants.SPEED_OF_LIGHT);

        // Delegate to private method
        return lorentzVelocitySum(naturalRaDec, stationBaryVel);
    }

    /**
     * Proper to natural correction for aberration of light.
     *
     * @param properRaDec the "proper" direction (station-relative coordinates)
     * @param station     the observer ground station
     * @param date        the date of the measurement
     * @param frame       the frame of the measurement
     * @return the "natural" direction (in barycentric coordinates)
     */
    @DefaultDataContext
    public static double[] properToNatural(final double[] properRaDec, final GroundStation station,
                                           final AbsoluteDate date, final Frame frame) {
        return properToNatural(properRaDec, station, date, frame, DataContext.getDefault());
    }

    /**
     * Proper to natural correction for aberration of light.
     *
     * @param properRaDec the "proper" direction (station-relative coordinates)
     * @param station     the observer ground station
     * @param date        the date of the measurement
     * @param frame       the frame of the measurement
     * @param context     the data context
     * @return the "natural" direction (in barycentric coordinates)
     * @since 12.0.1
     */
    public static double[] properToNatural(final double[] properRaDec, final GroundStation station,
                                           final AbsoluteDate date, final Frame frame, final DataContext context) {

        // Check measurement frame is inertial
        ensureFrameIsPseudoInertial(frame);

        // Velocity of barycentre relative to station (units of c)
        final PVCoordinates baryPV = context.getCelestialBodies().getSolarSystemBarycenter().getPVCoordinates(date, frame);
        final PVCoordinates stationPV = station.getBaseFrame().getPVCoordinates(date, frame);
        final Vector3D baryStationVel = baryPV.getVelocity()
                .subtract(stationPV.getVelocity())
                .scalarMultiply(1.0 / Constants.SPEED_OF_LIGHT);

        // Delegate to private method
        return lorentzVelocitySum(properRaDec, baryStationVel);
    }

    /**
     * Relativistic sum of velocities.
     * This is based on equation 3.252-3 from Seidelmann, "Explanatory Supplement to the Astronmical Almanac", 1992.
     *
     * @param raDec    the direction to transform
     * @param velocity the velocity (units of c)
     * @return the transformed direction
     */
    private static double[] lorentzVelocitySum(final double[] raDec, final Vector3D velocity) {

        // Measurement as unit vector
        final Vector3D direction = new Vector3D(raDec[0], raDec[1]);

        // Coefficients for calculations
        final double inverseBeta = FastMath.sqrt(1.0 - velocity.getNormSq());
        final double velocityScale = 1.0 + direction.dotProduct(velocity) / (1.0 + inverseBeta);

        // From Seidelmann, equation 3.252-3 (unnormalised)
        final Vector3D transformDirection = (direction.scalarMultiply(inverseBeta))
                .add(velocity.scalarMultiply(velocityScale));
        return new double[] {transformDirection.getAlpha(), transformDirection.getDelta()};
    }

    /**
     * Natural to proper correction for aberration of light.
     *
     * @param naturalRaDec      the "natural" direction (in barycentric coordinates)
     * @param stationToInertial the transform from station to inertial coordinates
     * @param frame             the frame of the measurement
     * @return the "proper" direction (station-relative coordinates)
     */
    @DefaultDataContext
    public static Gradient[] fieldNaturalToProper(final Gradient[] naturalRaDec,
                                                  final FieldTransform<Gradient> stationToInertial,
                                                  final Frame frame) {
        return fieldNaturalToProper(naturalRaDec, stationToInertial, frame, DataContext.getDefault());
    }

    /**
     * Natural to proper correction for aberration of light.
     *
     * @param naturalRaDec      the "natural" direction (in barycentric coordinates)
     * @param stationToInertial the transform from station to inertial coordinates
     * @param frame             the frame of the measurement
     * @param context           the data context
     * @return the "proper" direction (station-relative coordinates)
     * @since 12.0.1
     */
    public static Gradient[] fieldNaturalToProper(final Gradient[] naturalRaDec,
                                                  final FieldTransform<Gradient> stationToInertial,
                                                  final Frame frame,
                                                  final DataContext context) {

        // Check measurement frame is inertial
        ensureFrameIsPseudoInertial(frame);

        // Set up field
        final Field<Gradient> field = naturalRaDec[0].getField();
        final FieldVector3D<Gradient> zero = FieldVector3D.getZero(field);
        final FieldAbsoluteDate<Gradient> date = stationToInertial.getFieldDate();

        // Barycentre in inertial coordinates
        final FieldPVCoordinates<Gradient> baryPV = context.getCelestialBodies().getSolarSystemBarycenter().getPVCoordinates(date, frame);

        // Station in inertial coordinates
        final TimeStampedFieldPVCoordinates<Gradient> stationPV =
                stationToInertial.transformPVCoordinates(new TimeStampedFieldPVCoordinates<>(date, zero, zero, zero));

        // Velocity of station relative to barycentre (units of c)
        final FieldVector3D<Gradient> stationBaryVel = stationPV.getVelocity()
                .subtract(baryPV.getVelocity())
                .scalarMultiply(1.0 / Constants.SPEED_OF_LIGHT);

        return fieldLorentzVelocitySum(naturalRaDec, stationBaryVel);
    }

    /**
     * Proper to natural correction for aberration of light.
     *
     * @param properRaDec       the "proper" direction (station-relative coordinates)
     * @param stationToInertial the transform from station to inertial coordinates
     * @param frame             the frame of the measurement
     * @return the "natural" direction (in barycentric coordinates)
     */
    @DefaultDataContext
    public static Gradient[] fieldProperToNatural(final Gradient[] properRaDec,
                                                  final FieldTransform<Gradient> stationToInertial,
                                                  final Frame frame) {
        return fieldProperToNatural(properRaDec, stationToInertial, frame, DataContext.getDefault());
    }

    /**
     * Proper to natural correction for aberration of light.
     *
     * @param properRaDec       the "proper" direction (station-relative coordinates)
     * @param stationToInertial the transform from station to inertial coordinates
     * @param frame             the frame of the measurement
     * @param context           the data context
     * @return the "natural" direction (in barycentric coordinates)
     * @since 12.0.1
     */
    public static Gradient[] fieldProperToNatural(final Gradient[] properRaDec,
                                                  final FieldTransform<Gradient> stationToInertial,
                                                  final Frame frame,
                                                  final DataContext context) {

        // Check measurement frame is inertial
        ensureFrameIsPseudoInertial(frame);

        // Set up field
        final Field<Gradient> field = properRaDec[0].getField();
        final FieldVector3D<Gradient> zero = FieldVector3D.getZero(field);
        final FieldAbsoluteDate<Gradient> date = stationToInertial.getFieldDate();

        // Barycentre in inertial coordinates
        final FieldPVCoordinates<Gradient> baryPV = context.getCelestialBodies().getSolarSystemBarycenter().getPVCoordinates(date, frame);

        // Station in inertial coordinates
        final TimeStampedFieldPVCoordinates<Gradient> stationPV =
                stationToInertial.transformPVCoordinates(new TimeStampedFieldPVCoordinates<>(date, zero, zero, zero));

        // Velocity of barycentre relative to station (units of c)
        final FieldVector3D<Gradient> stationBaryVel = stationPV.getVelocity()
                .negate()
                .add(baryPV.getVelocity())
                .scalarMultiply(1.0 / Constants.SPEED_OF_LIGHT);

        return fieldLorentzVelocitySum(properRaDec, stationBaryVel);
    }

    /**
     * Relativistic sum of velocities.
     * This is based on equation 3.252-3 from Seidelmann, "Explanatory Supplement to the Astronmical Almanac", 1992.
     *
     * @param raDec    the direction to transform
     * @param velocity the velocity (units of c)
     * @return the transformed direction
     */
    private static Gradient[] fieldLorentzVelocitySum(final Gradient[] raDec,
                                                      final FieldVector3D<Gradient> velocity) {

        // Measurement as unit vector
        final FieldVector3D<Gradient> direction = new FieldVector3D<>(raDec[0], raDec[1]);

        // Coefficients for calculations
        final Gradient inverseBeta = (velocity.getNormSq().negate().add(1.0)).sqrt();
        final Gradient velocityScale = (direction.dotProduct(velocity)).divide(inverseBeta.add(1.0)).add(1.0);

        // From Seidelmann, equation 3.252-3 (unnormalised)
        final FieldVector3D<Gradient> transformDirection = (direction.scalarMultiply(inverseBeta))
                .add(velocity.scalarMultiply(velocityScale));
        return new Gradient[] {transformDirection.getAlpha(), transformDirection.getDelta()};
    }


    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        return Collections.emptyList();
    }

    /** {@inheritDoc} */
    @Override
    public void modifyWithoutDerivatives(final EstimatedMeasurementBase<AngularRaDec> estimated) {

        // Observation date
        final AbsoluteDate date = estimated.getDate();

        // Observation station
        final GroundStation station = estimated.getObservedMeasurement().getStation();

        // Observation frame
        final Frame frame = estimated.getObservedMeasurement().getReferenceFrame();

        // Convert measurement to natural direction
        final double[] estimatedRaDec = estimated.getEstimatedValue();
        final double[] naturalRaDec = properToNatural(estimatedRaDec, station, date, frame, dataContext);

        // Normalise RA
        final double[] observed           = estimated.getObservedValue();
        final double   baseRightAscension = naturalRaDec[0];
        final double   twoPiWrap          = MathUtils.normalizeAngle(baseRightAscension, observed[0]) - baseRightAscension;
        final double   rightAscension     = baseRightAscension + twoPiWrap;

        // New estimated values
        estimated.setEstimatedValue(rightAscension, naturalRaDec[1]);

    }

    /** {@inheritDoc} */
    @Override
    public void modify(final EstimatedMeasurement<AngularRaDec> estimated) {

        // Observation date
        final AbsoluteDate date = estimated.getDate();

        // Observation frame
        final Frame frame = estimated.getObservedMeasurement().getReferenceFrame();

        // Extract RA/Dec parameters (no state derivatives)
        int nbParams = 6;
        final Map<String, Integer> indices = new HashMap<>();
        for (ParameterDriver driver : estimated.getObservedMeasurement().getParametersDrivers()) {
            if (driver.isSelected()) {
                for (TimeSpanMap.Span<String> span = driver.getNamesSpanMap().getFirstSpan(); span != null; span = span.next()) {
                    if (!indices.containsKey(span.getData())) {
                        indices.put(span.getData(), nbParams++);
                    }
                }
            }
        }
        final Field<Gradient> field = GradientField.getField(nbParams);

        // Observation location
        final FieldTransform<Gradient> stationToInertial =
                estimated.getObservedMeasurement().getStation().getOffsetToInertial(frame, date, nbParams, indices);

        // Convert measurement to natural direction
        final double[] estimatedRaDec = estimated.getEstimatedValue();
        final Gradient[] estimatedRaDecDS = new Gradient[] {
                field.getZero().add(estimatedRaDec[0]),
                field.getZero().add(estimatedRaDec[1])
        };
        final Gradient[] naturalRaDec = fieldProperToNatural(estimatedRaDecDS, stationToInertial, frame, dataContext);

        // Normalise RA
        final double[] observed = estimated.getObservedValue();
        final Gradient baseRightAscension = naturalRaDec[0];
        final double twoPiWrap = MathUtils.normalizeAngle(baseRightAscension.getReal(),
                observed[0]) - baseRightAscension.getReal();
        final Gradient rightAscension = baseRightAscension.add(twoPiWrap);

        // New estimated values
        estimated.setEstimatedValue(rightAscension.getValue(), naturalRaDec[1].getValue());

        // Derivatives (only parameter, no state)
        final double[] raDerivatives = rightAscension.getGradient();
        final double[] decDerivatives = naturalRaDec[1].getGradient();

        for (final ParameterDriver driver : estimated.getObservedMeasurement().getParametersDrivers()) {
            for (TimeSpanMap.Span<String> span = driver.getNamesSpanMap().getFirstSpan(); span != null; span = span.next()) {
                final Integer index = indices.get(span.getData());
                if (index != null) {
                    final double[] parameterDerivative = estimated.getParameterDerivatives(driver);
                    parameterDerivative[0] += raDerivatives[index];
                    parameterDerivative[1] += decDerivatives[index];
                    estimated.setParameterDerivatives(driver, span.getStart(), parameterDerivative[0], parameterDerivative[1]);
                }
            }
        }
    }

    /**
     * Check that given frame is pseudo-inertial. Throws an error otherwise.
     *
     * @param frame to check
     *
     * @throws OrekitException if given frame is not pseudo-inertial
     */
    private static void ensureFrameIsPseudoInertial(final Frame frame) {
        // Check measurement frame is inertial
        if (!frame.isPseudoInertial()) {
            throw new OrekitException(OrekitMessages.NON_PSEUDO_INERTIAL_FRAME, frame.getName());
        }
    }

}
