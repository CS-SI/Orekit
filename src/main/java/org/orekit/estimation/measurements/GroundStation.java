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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.FieldGeodeticPoint;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.frames.FieldStaticTransform;
import org.orekit.frames.FieldTransform;
import org.orekit.frames.Frame;
import org.orekit.frames.KinematicTransform;
import org.orekit.frames.StaticTransform;
import org.orekit.frames.TopocentricFrame;
import org.orekit.frames.TopocentricTransformProvider;
import org.orekit.frames.Transform;
import org.orekit.models.earth.displacement.StationDisplacement;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.clocks.QuadraticClockModel;
import org.orekit.utils.AngularCoordinates;
import org.orekit.utils.FieldAngularCoordinates;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.FieldPVCoordinatesProvider;
import org.orekit.utils.PVCoordinatesProvider;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TimeStampedFieldPVCoordinates;

/** Class modeling a ground station that can perform some measurements.
 * <p>
 * This class adds a position offset parameter to a base {@link TopocentricFrame
 * topocentric frame}.
 * </p>
 * <p>
 * Since 9.3, this class also adds a station clock offset parameter, which manages
 * the value that must be subtracted from the observed measurement date to get the real
 * physical date at which the measurement was performed (i.e. the offset is negative
 * if the ground station clock is slow and positive if it is fast).
 * </p>
 * <ol>
 *   <li>station clock offset, controlled by {@link #getClockBiasDriver()}</li>
 *   <li>station position offset, controlled by {@link #getEastOffsetDriver()},
 *   {@link #getNorthOffsetDriver()} and {@link #getZenithOffsetDriver()}</li>
 * </ol>
 * @author Luc Maisonobe
 * @since 8.0
 */
public class GroundStation extends AbstractParticipant implements Observer {

    /** Position offsets scaling factor.
     * <p>
     * We use a power of 2 (in fact really 1.0 here) to avoid numeric noise introduction
     * in the multiplications/divisions sequences.
     * </p>
     */
    private static final double POSITION_OFFSET_SCALE = FastMath.scalb(1.0, 0);

    /** Base frame associated with the station. */
    private final TopocentricFrame baseFrame;

    /** Driver for position offset along the East axis. */
    private final ParameterDriver eastOffsetDriver;

    /** Driver for position offset along the North axis. */
    private final ParameterDriver northOffsetDriver;

    /** Driver for position offset along the zenith axis. */
    private final ParameterDriver zenithOffsetDriver;

    /**
     * Build a ground station ignoring {@link StationDisplacement station displacements}.
     * <p> The initial values for the station offset model
     * ({@link #getClockBiasDriver()}, {@link #getEastOffsetDriver()}, {@link #getNorthOffsetDriver()},
     * {@link #getZenithOffsetDriver()}) are set to 0. This implies that as long as these values are not changed, the
     * offset frame is the same as the {@link #getBaseFrame() base frame}. As soon as some of these models are changed,
     * the offset frame moves away from the {@link #getBaseFrame() base frame}.
     * </p>
     *
     * @param baseFrame base frame associated with the station, without *any* parametric model (no station offset)
     * @see #GroundStation(TopocentricFrame, QuadraticClockModel)
     * @since 13.0
     */
    public GroundStation(final TopocentricFrame baseFrame) {
        this(baseFrame, createEmptyQuadraticClock(baseFrame.getName()));
    }

     /**
     * Simple constructor.
     * <p>
     * The initial values for the station offset model
     * ({@link #getClockBiasDriver()}, {@link #getEastOffsetDriver()}, {@link #getNorthOffsetDriver()},
     * {@link #getZenithOffsetDriver()}, {@link #getClockBiasDriver()}) are set to 0. This implies that as long as
     * these values are not changed, the offset frame is the same as the {@link #getBaseFrame() base frame}. As soon as
     * some of these models are changed, the offset frame moves away from the {@link #getBaseFrame() base frame}.
     * </p>
     *
     * @param baseFrame     base frame associated with the station, without *any* parametric model (no station offset)
     * @param clock         new quadratic clock model with user-supplied displacements
     * @since 12.1
     */
    public GroundStation(final TopocentricFrame baseFrame, final QuadraticClockModel clock) {
        super(baseFrame.getName(), clock);
        this.baseFrame = baseFrame;

        this.eastOffsetDriver = new ParameterDriver(baseFrame.getName() + OFFSET_SUFFIX + "-East",
                                                    0.0, POSITION_OFFSET_SCALE,
                                                    Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);

        this.northOffsetDriver = new ParameterDriver(baseFrame.getName() + OFFSET_SUFFIX + "-North",
                                                     0.0, POSITION_OFFSET_SCALE,
                                                     Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);

        this.zenithOffsetDriver = new ParameterDriver(baseFrame.getName() + OFFSET_SUFFIX + "-Zenith",
                                                      0.0, POSITION_OFFSET_SCALE,
                                                      Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);

        // Add the ground station parameters to the master list.
        addParameterDriver(this.eastOffsetDriver);
        addParameterDriver(this.northOffsetDriver);
        addParameterDriver(this.zenithOffsetDriver);

    }

    /** {@inheritDoc} */
    @Override
    public final boolean isSpaceBased() {
        return false;
    }

    /** Get a driver allowing to change station position along East axis.
     * @return driver for station position offset along East axis
     */
    public ParameterDriver getEastOffsetDriver() {
        return eastOffsetDriver;
    }

    /** Get a driver allowing to change station position along North axis.
     * @return driver for station position offset along North axis
     */
    public ParameterDriver getNorthOffsetDriver() {
        return northOffsetDriver;
    }

    /** Get a driver allowing to change station position along Zenith axis.
     * @return driver for station position offset along Zenith axis
     */
    public ParameterDriver getZenithOffsetDriver() {
        return zenithOffsetDriver;
    }

    /** Get the base frame associated with the station.
     * <p>
     * The base frame corresponds to a null position offset, null
     * polar motion, null meridian shift
     * </p>
     * @return base frame associated with the station
     */
    public TopocentricFrame getBaseFrame() {
        return baseFrame;
    }

    /** Get the station displacement.
     * @param date current date
     * @param position raw position of the station in Earth frame
     * before displacement is applied
     * @return station displacement
     * @since 9.1
     */
    protected Vector3D computeDisplacement(final AbsoluteDate date, final Vector3D position) {
        return Vector3D.ZERO;
    }

    /** Get the geodetic point at the center of the offset frame.
     * @param date current date (may be null if displacements are ignored)
     * @return geodetic point at the center of the offset frame
     * @since 9.1
     */
    public GeodeticPoint getOffsetGeodeticPoint(final AbsoluteDate date) {

        // take station offset into account
        final double    x          = eastOffsetDriver.getValue();
        final double    y          = northOffsetDriver.getValue();
        final double    z          = zenithOffsetDriver.getValue();
        final BodyShape baseShape  = baseFrame.getParentShape();
        final StaticTransform baseToBody = baseFrame.getStaticTransformTo(baseShape.getBodyFrame(), date);
        Vector3D        origin     = baseToBody.transformPosition(new Vector3D(x, y, z));

        if (date != null) {
            origin = origin.add(computeDisplacement(date, origin));
        }

        return baseShape.transform(origin, baseShape.getBodyFrame(), date);

    }

    /** Get the geodetic point at the center of the offset frame.
     * @param <T> type of the field elements
     * @param date current date(<em>must</em> be non-null, which is a more stringent condition
     *      *                    than in {@link #getOffsetGeodeticPoint(AbsoluteDate)}
     * @return geodetic point at the center of the offset frame
     * @since 12.1
     */
    public <T extends CalculusFieldElement<T>> FieldGeodeticPoint<T> getOffsetGeodeticPoint(final FieldAbsoluteDate<T> date) {

        // take station offset into account
        final double    x          = eastOffsetDriver.getValue();
        final double    y          = northOffsetDriver.getValue();
        final double    z          = zenithOffsetDriver.getValue();
        final BodyShape baseShape  = baseFrame.getParentShape();
        final FieldStaticTransform<T> baseToBody = baseFrame.getStaticTransformTo(baseShape.getBodyFrame(), date);
        FieldVector3D<T> origin    = baseToBody.transformPosition(new Vector3D(x, y, z));
        origin = origin.add(computeDisplacement(date.toAbsoluteDate(), origin.toVector3D()));

        return baseShape.transform(origin, baseShape.getBodyFrame(), date);

    }

    /** {@inheritDoc} */
    @Override
    public PVCoordinatesProvider getPVCoordinatesProvider() {
        final GeodeticPoint offsetPoint = getOffsetGeodeticPoint(AbsoluteDate.ARBITRARY_EPOCH);
        return new TopocentricFrame(baseFrame.getParentShape(), offsetPoint, "offset");
    }

    /** {@inheritDoc} */
    @Override
    public FieldPVCoordinatesProvider<Gradient> getFieldPVCoordinatesProvider(final int freeParameters,
                                                                              final Map<String, Integer> parameterIndices) {
        return new FieldPVCoordinatesProvider<>() {
            @Override
            public TimeStampedFieldPVCoordinates<Gradient> getPVCoordinates(final FieldAbsoluteDate<Gradient> date,
                                                                            final Frame frame) {
                // take station offsets into account
                final FieldVector3D<Gradient> origin = getOrigin(date, parameterIndices);

                // body-fixed body-centered to target (with linear approximation for performance)
                final Transform bodyToInertNonField = baseFrame.getParent().getTransformTo(frame, date.toAbsoluteDate());
                final FieldTransform<Gradient> bodyToInert = new FieldTransform<>(date.getField(),
                        bodyToInertNonField).shiftedBy(date.durationFrom(date.toAbsoluteDate()));

                final TimeStampedFieldPVCoordinates<Gradient> zeroPV = new TimeStampedFieldPVCoordinates<>(date,
                        new FieldPVCoordinates<>(origin, FieldVector3D.getZero(date.getField())));
                return bodyToInert.transformPVCoordinates(zeroPV);
            }

            @Override
            public FieldVector3D<Gradient> getPosition(final FieldAbsoluteDate<Gradient> date, final Frame frame) {
                // take station offsets into account
                final FieldVector3D<Gradient> origin = getOrigin(date, parameterIndices);

                // body-fixed body-centered to target (with linear approximation for performance)
                final KinematicTransform bodyToInertNonField = baseFrame.getParent().getKinematicTransformTo(frame,
                        date.toAbsoluteDate());
                final FieldStaticTransform<Gradient> bodyToInert = shiftKinematicTransform(bodyToInertNonField,
                        date.durationFrom(date.toAbsoluteDate()));

                // combine by hand for performance reasons
                return bodyToInert.getRotation().applyTo(bodyToInert.getTranslation().add(origin));
            }
        };
    }

    /**
     * Retrieve station's position in body shape frame.
     * @param date date
     * @param indices mapping from parameters' name to derivatives' index.
     * @return origin position
     */
    protected FieldVector3D<Gradient> getOrigin(final FieldAbsoluteDate<Gradient> date,
                                                final Map<String, Integer> indices) {
        // compute position in topocentric frame
        final int freeParameters = date.getField().getZero().getFreeParameters();
        final AbsoluteDate absoluteDate = date.toAbsoluteDate();
        final Gradient x          = eastOffsetDriver.getValue(freeParameters, indices, absoluteDate);
        final Gradient                       y          = northOffsetDriver.getValue(freeParameters, indices, absoluteDate);
        final Gradient                       z          = zenithOffsetDriver.getValue(freeParameters, indices, absoluteDate);
        final FieldVector3D<Gradient> position = new FieldVector3D<>(x, y, z);
        // approximate linearly (for performance) static transform from topocentric to body shape frame
        final Frame bodyFrame = baseFrame.getParentShape().getBodyFrame();
        final KinematicTransform kinematicTopoToBody = baseFrame.getKinematicTransformTo(bodyFrame, absoluteDate);
        final FieldStaticTransform<Gradient> staticTopoToBody = shiftKinematicTransform(kinematicTopoToBody,
                date.durationFrom(absoluteDate));
        // apply transform and displacement
        final FieldVector3D<Gradient>        originBeforeDisplacement     = staticTopoToBody.transformPosition(position);
        return originBeforeDisplacement.add(computeDisplacement(absoluteDate, originBeforeDisplacement.toVector3D()));
    }

    /**
     * Shift a kinematic transform by a Gradient time into a FieldStaticTransform.
     * @param kinematicTransform kinematic transform to shift
     * @param dt time to shift by
     * @return Field static transform shifted by dt
     * @since 14.0
     */
    protected FieldStaticTransform<Gradient> shiftKinematicTransform(final KinematicTransform kinematicTransform,
                                                                     final Gradient dt) {
        // shift translation
        final Field<Gradient> field = dt.getField();
        final AbsoluteDate date = kinematicTransform.getDate();
        final FieldVector3D<Gradient> fieldVelocity = new FieldVector3D<>(field, kinematicTransform.getVelocity());
        final FieldVector3D<Gradient> shiftedTranslation = fieldVelocity.scalarMultiply(dt).add(kinematicTransform.getTranslation());
        // shift rotation
        final FieldAngularCoordinates<Gradient> fieldAngularCoordinates = new FieldAngularCoordinates<>(field,
                new AngularCoordinates(kinematicTransform.getRotation(), kinematicTransform.getRotationRate()));
        final FieldVector3D<Gradient> rotationRate = fieldAngularCoordinates.getRotationRate();
        final Gradient rate = rotationRate.getNorm();
        final FieldRotation<Gradient> shiftedRotation = (rate.getReal() == 0.0) ?
                fieldAngularCoordinates.getRotation() :
                new FieldRotation<>(rotationRate, rate.multiply(dt), RotationConvention.FRAME_TRANSFORM)
                        .compose(fieldAngularCoordinates.getRotation(), RotationConvention.VECTOR_OPERATOR);
        return FieldStaticTransform.of(new FieldAbsoluteDate<>(field, date).shiftedBy(dt), shiftedTranslation,
                shiftedRotation);
    }

    /** {@inheritDoc} */
    @Override
    public Transform getOffsetToInertial(final Frame inertial, final AbsoluteDate date,
                                         final boolean clockOffsetAlreadyApplied) {

        // take clock offset into account
        final AbsoluteDate offsetCompensatedDate = clockOffsetAlreadyApplied ?
                date :
                new AbsoluteDate(date, -getOffsetValue(date));

        final TopocentricFrame topocentricFrame = (TopocentricFrame) getPVCoordinatesProvider();
        return topocentricFrame.getTransformTo(inertial, offsetCompensatedDate);
    }

    /** {@inheritDoc} */
    @Override
    public FieldTransform<Gradient> getOffsetToInertial(final Frame inertial,
                                                        final FieldAbsoluteDate<Gradient> offsetCompensatedDate,
                                                        final int freeParameters,
                                                        final Map<String, Integer> indices) {
        // take station offsets into account
        final FieldVector3D<Gradient> origin = getOrigin(offsetCompensatedDate, indices);
        final FieldGeodeticPoint<Gradient> originGP = baseFrame.getParentShape().transform(origin, baseFrame.getParent(),
                offsetCompensatedDate);
        final FieldStaticTransform<Gradient> staticOffsetToBody = TopocentricTransformProvider.getTransform(baseFrame.getParentShape(),
                offsetCompensatedDate, originGP).getStaticInverse();
        final FieldTransform<Gradient> offsetToBody = new FieldTransform<>(offsetCompensatedDate,
                staticOffsetToBody.getTranslation(), staticOffsetToBody.getRotation());

        // Body-fixed, body-centered frame to target one
        final FieldTransform<Gradient> bodyToInert = baseFrame.getParent().getTransformTo(inertial, offsetCompensatedDate);

        // combine all transforms together
        return new FieldTransform<>(offsetCompensatedDate, offsetToBody, bodyToInert);
    }

}
