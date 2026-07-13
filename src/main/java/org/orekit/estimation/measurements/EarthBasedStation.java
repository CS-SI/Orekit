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
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.data.BodiesElements;
import org.orekit.data.FundamentalNutationArguments;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.EOPHistory;
import org.orekit.frames.FieldStaticTransform;
import org.orekit.frames.FieldTransform;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.KinematicTransform;
import org.orekit.frames.TopocentricFrame;
import org.orekit.frames.Transform;
import org.orekit.frames.TransformProvider;
import org.orekit.models.earth.displacement.StationDisplacement;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.UT1Scale;
import org.orekit.time.clocks.QuadraticClockModel;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.FieldPVCoordinatesProvider;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.PVCoordinatesProvider;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TimeStampedFieldPVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

/** Class modeling an Earth-based station that can perform some measurements.
 * <p>
 * This class adds a position offset parameter to a base {@link TopocentricFrame
 * topocentric frame}.
 * </p>
 * <p>
 * Since 9.0, this class also adds parameters for an additional polar motion
 * and an additional prime meridian orientation. Since these parameters will
 * have the same name for all ground stations, they will be managed consistently
 * and allow to estimate Earth orientation precisely (this is needed for precise
 * orbit determination). The polar motion and prime meridian orientation will
 * be applied <em>after</em> regular Earth orientation parameters, so the value
 * of the estimated parameters will be correction to EOP, they will not be the
 * complete EOP values by themselves. Basically, this means that for Earth, the
 * following transforms are applied in order, between inertial frame and ground
 * station frame (for non-Earth based ground stations, different precession nutation
 * models and associated planet orientation parameters would be applied, if available):
 * </p>
 * <p>
 * This class also adds a station clock offset parameter, which manages
 * the value that must be subtracted from the observed measurement date to get the real
 * physical date at which the measurement was performed (i.e. the offset is negative
 * if the ground station clock is slow and positive if it is fast).
 * </p>
 * <ol>
 *   <li>precession/nutation, as theoretical model plus celestial pole EOP parameters</li>
 *   <li>body rotation, as theoretical model plus prime meridian EOP parameters</li>
 *   <li>polar motion, which is only from EOP parameters (no theoretical models)</li>
 *   <li>additional body rotation, controlled by {@link #getPrimeMeridianOffsetDriver()} and {@link #getPrimeMeridianDriftDriver()}</li>
 *   <li>additional polar motion, controlled by {@link #getPolarOffsetXDriver()}, {@link #getPolarDriftXDriver()},
 *   {@link #getPolarOffsetYDriver()} and {@link #getPolarDriftYDriver()}</li>
 *   <li>station clock offset, controlled by {@link #getClockBiasDriver()}</li>
 *   <li>station position offset, controlled by {@link #getEastOffsetDriver()},
 *   {@link #getNorthOffsetDriver()} and {@link #getZenithOffsetDriver()}</li>
 * </ol>
 * @author Luc Maisonobe
 * @author Romain Serra
 * @since 14.0
 */
public class EarthBasedStation extends GroundStation {

    /** Provider for Earth frame whose EOP parameters can be estimated. */
    private final EstimatedEarthFrameProvider estimatedEarthFrameProvider;

    /** Earth frame whose EOP parameters can be estimated. */
    private final Frame estimatedEarthFrame;

    /** Fundamental nutation arguments. */
    private final FundamentalNutationArguments arguments;

    /** Displacement models. */
    private final StationDisplacement[] displacements;

    /**
     * Build a ground station ignoring {@link StationDisplacement station displacements}.
     * <p>
     * The initial values for the pole and prime meridian parametric linear models
     * ({@link #getPrimeMeridianOffsetDriver()}, {@link #getPrimeMeridianDriftDriver()},
     * {@link #getPolarOffsetXDriver()}, {@link #getPolarDriftXDriver()}, {@link #getPolarOffsetYDriver()},
     * {@link #getPolarDriftYDriver()}) are set to 0. The initial values for the station offset model
     * ({@link #getClockBiasDriver()}, {@link #getEastOffsetDriver()}, {@link #getNorthOffsetDriver()},
     * {@link #getZenithOffsetDriver()}) are set to 0. This implies that as long as these values are not changed, the
     * offset frame is the same as the {@link #getBaseFrame() base frame}. As soon as some of these models are changed,
     * the offset frame moves away from the {@link #getBaseFrame() base frame}.
     * </p>
     *
     * @param baseFrame base frame associated with the station, without *any* parametric model
     *                  (no station offset, no polar motion, no meridian shift)
     * @see #EarthBasedStation(TopocentricFrame, EOPHistory, StationDisplacement...)
     * @since 14.0
     */
    public EarthBasedStation(final TopocentricFrame baseFrame) {
        this(baseFrame, FramesFactory.findEOP(baseFrame));
    }

    /**
     * Build a ground station ignoring {@link StationDisplacement station displacements}.
     * <p>
     * The initial values for the pole and prime meridian parametric linear models
     * ({@link #getPrimeMeridianOffsetDriver()}, {@link #getPrimeMeridianDriftDriver()},
     * {@link #getPolarOffsetXDriver()}, {@link #getPolarDriftXDriver()}, {@link #getPolarOffsetYDriver()},
     * {@link #getPolarDriftYDriver()}) are set to 0. The initial values for the station offset model
     * ({@link #getClockBiasDriver()}, {@link #getEastOffsetDriver()}, {@link #getNorthOffsetDriver()},
     * {@link #getZenithOffsetDriver()}) are set to 0. This implies that as long as these values are not changed, the
     * offset frame is the same as the {@link #getBaseFrame() base frame}. As soon as some of these models are changed,
     * the offset frame moves away from the {@link #getBaseFrame() base frame}.
     * </p>
     *
     * @param baseFrame base frame associated with the station, without *any* parametric model
     *                  (no station offset, no polar motion, no meridian shift)
     * @param clock         new quadratic clock model with user-supplied displacements
     * @see #EarthBasedStation(TopocentricFrame, EOPHistory, StationDisplacement...)
     */
    public EarthBasedStation(final TopocentricFrame baseFrame, final QuadraticClockModel clock) {
        this(baseFrame, FramesFactory.findEOP(baseFrame), clock);
    }

    /**
     * Simple constructor.
     * <p>
     * The initial values for the pole and prime meridian parametric linear models
     * ({@link #getPrimeMeridianOffsetDriver()}, {@link #getPrimeMeridianDriftDriver()},
     * {@link #getPolarOffsetXDriver()}, {@link #getPolarDriftXDriver()}, {@link #getPolarOffsetYDriver()},
     * {@link #getPolarDriftYDriver()}) are set to 0. The initial values for the station offset model
     * ({@link #getClockBiasDriver()}, {@link #getEastOffsetDriver()}, {@link #getNorthOffsetDriver()},
     * {@link #getZenithOffsetDriver()}) are set to 0. This implies that as long as
     * these values are not changed, the offset frame is the same as the {@link #getBaseFrame() base frame}. As soon as
     * some of these models are changed, the offset frame moves away from the {@link #getBaseFrame() base frame}.
     * </p>
     *
     * @param baseFrame     base frame associated with the station, without *any* parametric model (no station offset,
     *                      no polar motion, no meridian shift)
     * @param eopHistory    EOP history associated with Earth frames
     * @param displacements ground station displacement model (tides, ocean loading, atmospheric loading, thermal
     *                      effects...)
     */
    public EarthBasedStation(final TopocentricFrame baseFrame, final EOPHistory eopHistory,
                             final StationDisplacement... displacements) {
        this(baseFrame, eopHistory, createEmptyQuadraticClock(baseFrame.getName()), displacements);
    }

     /**
     * Simple constructor.
     * <p>
     * The initial values for the pole and prime meridian parametric linear models
     * ({@link #getPrimeMeridianOffsetDriver()}, {@link #getPrimeMeridianDriftDriver()},
     * {@link #getPolarOffsetXDriver()}, {@link #getPolarDriftXDriver()}, {@link #getPolarOffsetYDriver()},
     * {@link #getPolarDriftYDriver()}) are set to 0. The initial values for the station offset model
     * ({@link #getClockBiasDriver()}, {@link #getEastOffsetDriver()}, {@link #getNorthOffsetDriver()},
     * {@link #getZenithOffsetDriver()}) are set to 0. This implies that as long as
     * these values are not changed, the offset frame is the same as the {@link #getBaseFrame() base frame}. As soon as
     * some of these models are changed, the offset frame moves away from the {@link #getBaseFrame() base frame}.
     * </p>
     *
     * @param baseFrame     base frame associated with the station, without *any* parametric model (no station offset,
     *                      no polar motion, no meridian shift)
     * @param eopHistory    EOP history associated with Earth frames
     * @param clock         new quadratic clock model with user-supplied displacements
     * @param displacements ground station displacement model (tides, ocean loading, atmospheric loading, thermal
     *                      effects...)
     */
    public EarthBasedStation(final TopocentricFrame baseFrame, final EOPHistory eopHistory,
                             final QuadraticClockModel clock, final StationDisplacement... displacements) {
        super(baseFrame, clock);

        if (eopHistory == null) {
            throw new OrekitException(OrekitMessages.NO_EARTH_ORIENTATION_PARAMETERS);
        }

        final UT1Scale baseUT1 = eopHistory.getTimeScales()
                .getUT1(eopHistory.getConventions(), eopHistory.isSimpleEop());
        this.estimatedEarthFrameProvider = new EstimatedEarthFrameProvider(baseUT1);
        this.estimatedEarthFrame = new Frame(baseFrame.getParent(), estimatedEarthFrameProvider,
                                             baseFrame.getParent() + "-estimated");

        if (displacements.length == 0) {
            arguments = null;
        } else {
            arguments = eopHistory.getConventions().getNutationArguments(estimatedEarthFrameProvider.getEstimatedUT1(),
                    eopHistory.getTimeScales());
        }

        this.displacements = displacements.clone();

        // Add the ground station parameters to the master list.
        addParameterDriver(this.estimatedEarthFrameProvider.getPrimeMeridianOffsetDriver());
        addParameterDriver(this.estimatedEarthFrameProvider.getPrimeMeridianDriftDriver());
        addParameterDriver(this.estimatedEarthFrameProvider.getPolarOffsetXDriver());
        addParameterDriver(this.estimatedEarthFrameProvider.getPolarDriftXDriver());
        addParameterDriver(this.estimatedEarthFrameProvider.getPolarOffsetYDriver());
        addParameterDriver(this.estimatedEarthFrameProvider.getPolarDriftYDriver());

    }

    /** Get the displacement models.
     * @return displacement models (empty if no model has been set up)
     */
    public StationDisplacement[] getDisplacements() {
        return displacements.clone();
    }

    /** Get a driver allowing to add a prime meridian rotation.
     * <p>
     * The parameter is an angle in radians. In order to convert this
     * value to a DUT1 in seconds, the value must be divided by
     * {@code ave = 7.292115146706979e-5} (which is the nominal Angular Velocity
     * of Earth from the TIRF model).
     * </p>
     * @return driver for prime meridian rotation
     */
    public ParameterDriver getPrimeMeridianOffsetDriver() {
        return estimatedEarthFrameProvider.getPrimeMeridianOffsetDriver();
    }

    /** Get a driver allowing to add a prime meridian rotation rate.
     * <p>
     * The parameter is an angle rate in radians per second. In order to convert this
     * value to a LOD in seconds, the value must be multiplied by -86400 and divided by
     * {@code ave = 7.292115146706979e-5} (which is the nominal Angular Velocity
     * of Earth from the TIRF model).
     * </p>
     * @return driver for prime meridian rotation rate
     */
    public ParameterDriver getPrimeMeridianDriftDriver() {
        return estimatedEarthFrameProvider.getPrimeMeridianDriftDriver();
    }

    /** Get a driver allowing to add a polar offset along X.
     * <p>
     * The parameter is an angle in radians
     * </p>
     * @return driver for polar offset along X
     */
    public ParameterDriver getPolarOffsetXDriver() {
        return estimatedEarthFrameProvider.getPolarOffsetXDriver();
    }

    /** Get a driver allowing to add a polar drift along X.
     * <p>
     * The parameter is an angle rate in radians per second
     * </p>
     * @return driver for polar drift along X
     */
    public ParameterDriver getPolarDriftXDriver() {
        return estimatedEarthFrameProvider.getPolarDriftXDriver();
    }

    /** Get a driver allowing to add a polar offset along Y.
     * <p>
     * The parameter is an angle in radians
     * </p>
     * @return driver for polar offset along Y
     */
    public ParameterDriver getPolarOffsetYDriver() {
        return estimatedEarthFrameProvider.getPolarOffsetYDriver();
    }

    /** Get a driver allowing to add a polar drift along Y.
     * <p>
     * The parameter is an angle rate in radians per second
     * </p>
     * @return driver for polar drift along Y
     */
    public ParameterDriver getPolarDriftYDriver() {
        return estimatedEarthFrameProvider.getPolarDriftYDriver();
    }

    /** Get the estimated Earth frame, including the estimated linear models for pole and prime meridian.
     * <p>
     * This frame is bound to the {@link #getPrimeMeridianOffsetDriver() driver for prime meridian offset},
     * {@link #getPrimeMeridianDriftDriver() driver prime meridian drift},
     * {@link #getPolarOffsetXDriver() driver for polar offset along X},
     * {@link #getPolarDriftXDriver() driver for polar drift along X},
     * {@link #getPolarOffsetYDriver() driver for polar offset along Y},
     * {@link #getPolarDriftYDriver() driver for polar drift along Y}, so its orientation changes when
     * the {@link ParameterDriver#setValue(double) setValue} methods of the drivers are called.
     * </p>
     * @return estimated Earth frame
     */
    public Frame getEstimatedEarthFrame() {
        return estimatedEarthFrame;
    }

    /** Get the estimated UT1 scale, including the estimated linear models for prime meridian.
     * <p>
     * This time scale is bound to the {@link #getPrimeMeridianOffsetDriver() driver for prime meridian offset},
     * and {@link #getPrimeMeridianDriftDriver() driver prime meridian drift}, so its offset from UTC changes when
     * the {@link ParameterDriver#setValue(double) setValue} methods of the drivers are called.
     * </p>
     * @return estimated UT1 scale
     */
    public UT1Scale getEstimatedUT1() {
        return estimatedEarthFrameProvider.getEstimatedUT1();
    }

    /** Get the station displacement.
     * @param date current date
     * @param position raw position of the station in Earth frame
     * before displacement is applied
     * @return station displacement
     */
    @Override
    protected Vector3D computeDisplacement(final AbsoluteDate date, final Vector3D position) {
        Vector3D displacement = Vector3D.ZERO;
        if (arguments != null) {
            final BodiesElements elements = arguments.evaluateAll(date);
            for (final StationDisplacement sd : displacements) {
                // we consider all displacements apply to the same initial position,
                // i.e. they apply simultaneously, not according to some order
                displacement = displacement.add(sd.displacement(elements, estimatedEarthFrame, position));
            }
        }
        return displacement;
    }

    /**
     * Get the transform provider associated with the station.
     * @param frame target frame for the transform provider
     * @return transform provider
     */
    private EarthBasedStationTransformProvider getTransformProvider(final Frame frame) {
        return new EarthBasedStationTransformProvider(frame, getBaseFrame(), getEastOffsetDriver(), getNorthOffsetDriver(),
                getZenithOffsetDriver(), estimatedEarthFrameProvider, arguments, displacements);
    }

    /** {@inheritDoc} */
    @Override
    public PVCoordinatesProvider getPVCoordinatesProvider() {
        return new PVCoordinatesProvider() {
            @Override
            public TimeStampedPVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) {
                final TransformProvider transformProvider = getTransformProvider(frame);
                return transformProvider.getTransform(date)
                        .transformPVCoordinates(new TimeStampedPVCoordinates(date, PVCoordinates.ZERO));
            }

            @Override
            public Vector3D getVelocity(final AbsoluteDate date, final Frame frame) {
                final TransformProvider transformProvider = getTransformProvider(frame);
                return transformProvider.getKinematicTransform(date).transformOnlyPV(PVCoordinates.ZERO).getVelocity();
            }

            @Override
            public Vector3D getPosition(final AbsoluteDate date, final Frame frame) {
                final TransformProvider transformProvider = getTransformProvider(frame);
                return transformProvider.getStaticTransform(date).transformPosition(Vector3D.ZERO);
            }
        };
    }

    /** {@inheritDoc} */
    @Override
    public FieldPVCoordinatesProvider<Gradient> getFieldPVCoordinatesProvider(final int freeParameters,
                                                                              final Map<String, Integer> parameterIndices) {
        return new FieldPVCoordinatesProvider<>() {
            @Override
            public TimeStampedFieldPVCoordinates<Gradient> getPVCoordinates(final FieldAbsoluteDate<Gradient> date,
                                                                            final Frame frame) {
                // take Earth offsets into account
                final FieldTransform<Gradient> intermediateToBody = estimatedEarthFrameProvider.getTransform(date,
                        freeParameters, parameterIndices).getInverse();

                // take station offsets into account
                final FieldVector3D<Gradient> origin = getOrigin(date, parameterIndices);

                // Earth-fixed Earth-centered to target (with linear approximation for performance)
                final Transform bodyToInertNonField = getBaseFrame().getParent().getTransformTo(frame, date.toAbsoluteDate());
                final FieldTransform<Gradient> bodyToInert = new FieldTransform<>(date.getField(),
                        bodyToInertNonField).shiftedBy(date.durationFrom(date.toAbsoluteDate()));

                final TimeStampedFieldPVCoordinates<Gradient> zeroPV = new TimeStampedFieldPVCoordinates<>(date,
                        new FieldPVCoordinates<>(origin, FieldVector3D.getZero(date.getField())));
                return new FieldTransform<>(date, intermediateToBody, bodyToInert).transformPVCoordinates(zeroPV);
            }

            @Override
            public FieldVector3D<Gradient> getPosition(final FieldAbsoluteDate<Gradient> date, final Frame frame) {
                // take Earth offsets into account
                final FieldRotation<Gradient> bodyToIntermediateRotation = estimatedEarthFrameProvider.getStaticTransform(date,
                        freeParameters, parameterIndices).getRotation();

                // take station offsets into account
                final FieldVector3D<Gradient> origin = getOrigin(date, parameterIndices);

                // Earth-fixed Earth-centered to target (with linear approximation for performance)
                final KinematicTransform bodyToInertNonField = getBaseFrame().getParent().getKinematicTransformTo(frame,
                        date.toAbsoluteDate());
                final FieldStaticTransform<Gradient> bodyToInert = shiftKinematicTransform(bodyToInertNonField,
                        date.durationFrom(date.toAbsoluteDate()));

                // combine by hand for performance reasons
                final FieldRotation<Gradient> rotation = bodyToIntermediateRotation.composeInverse(bodyToInert.getRotation(),
                        RotationConvention.FRAME_TRANSFORM);
                return rotation.applyTo(bodyToInert.getTranslation().add(origin));
            }
        };
    }

    /** {@inheritDoc} */
    @Override
    public Transform getOffsetToInertial(final Frame inertial, final AbsoluteDate date, final boolean clockOffsetAlreadyApplied) {

        // take clock offset into account
        final AbsoluteDate offsetCompensatedDate = clockOffsetAlreadyApplied ?
                date :
                new AbsoluteDate(date, -getOffsetValue(date));

        final EarthBasedStationTransformProvider transformProvider = getTransformProvider(inertial);
        return transformProvider.getTransform(offsetCompensatedDate);
    }

    /** {@inheritDoc} */
    @Override
    public FieldTransform<Gradient> getOffsetToInertial(final Frame inertial,
                                                        final FieldAbsoluteDate<Gradient> offsetCompensatedDate,
                                                        final int freeParameters,
                                                        final Map<String, Integer> indices) {
        // take Earth offsets into account
        final FieldTransform<Gradient> intermediateToBody =
                estimatedEarthFrameProvider.getTransform(offsetCompensatedDate, freeParameters, indices).getInverse();

        // take station offsets into account
        final FieldVector3D<Gradient> origin = getOrigin(offsetCompensatedDate, indices);

        final EarthBasedStationTransformProvider transformProvider = getTransformProvider(inertial);
        return transformProvider.getTransform(offsetCompensatedDate, origin, intermediateToBody);
    }

}
