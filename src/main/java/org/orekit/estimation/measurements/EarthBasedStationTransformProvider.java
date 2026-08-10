/* Copyright 2022-2026 Romain Serra
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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.FieldGeodeticPoint;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.data.BodiesElements;
import org.orekit.data.FundamentalNutationArguments;
import org.orekit.frames.FieldStaticTransform;
import org.orekit.frames.FieldTransform;
import org.orekit.frames.Frame;
import org.orekit.frames.StaticTransform;
import org.orekit.frames.TopocentricFrame;
import org.orekit.frames.Transform;
import org.orekit.frames.TransformProvider;
import org.orekit.models.earth.displacement.StationDisplacement;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.ParameterDriver;

/** Class modeling a Earth-based station frame transform.
 * <p>
 * This class considers a position offset parameter w.r.t. a base {@link TopocentricFrame
 * topocentric frame}.
 * </p>
 * <p>
 * This class also adds parameters for an additional polar motion
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
 * @author Romain Serra
 * @see EarthBasedStation
 * @since 14.0
 */
class EarthBasedStationTransformProvider implements TransformProvider {

    /**
     * Target frame.
     */
    private final Frame frame;

    /** Provider for Earth frame whose EOP parameters can be estimated. */
    private final EstimatedEarthFrameProvider estimatedEarthFrameProvider;

    /** Earth frame whose EOP parameters can be estimated. */
    private final Frame estimatedEarthFrame;

    /** Base frame associated with the station. */
    private final TopocentricFrame baseFrame;

    /** Fundamental nutation arguments. */
    private final FundamentalNutationArguments arguments;

    /** Displacement models. */
    private final StationDisplacement[] displacements;

    /** Driver for position offset along the East axis. */
    private final ParameterDriver eastOffsetDriver;

    /** Driver for position offset along the North axis. */
    private final ParameterDriver northOffsetDriver;

    /** Driver for position offset along the zenith axis. */
    private final ParameterDriver zenithOffsetDriver;

     /**
     * Constructor.
     * @param frame target frame
     * @param baseFrame     base frame associated with the station, without *any* parametric model (no station offset,
     *                      no polar motion, no meridian shift)
     * @param eastOffsetDriver driver for position offset along the East axis
     * @param northOffsetDriver driver for position offset along the North axis
     * @param zenithOffsetDriver driver for position offset along the zenith axis
     * @param estimatedEarthFrameProvider provider for Earth frame whose EOP parameters can be estimated
     * @param fundamentalNutationArguments fundamental nutation arguments
     * @param displacements ground station displacement model (tides, ocean loading, atmospheric loading, thermal
     *                      effects...)
     */
    EarthBasedStationTransformProvider(final Frame frame, final TopocentricFrame baseFrame,
                                       final ParameterDriver eastOffsetDriver,
                                       final ParameterDriver northOffsetDriver,
                                       final ParameterDriver zenithOffsetDriver,
                                       final EstimatedEarthFrameProvider estimatedEarthFrameProvider,
                                       final FundamentalNutationArguments fundamentalNutationArguments,
                                       final StationDisplacement... displacements) {
        this.frame = frame;
        this.baseFrame = baseFrame;

        this.estimatedEarthFrameProvider = estimatedEarthFrameProvider;
        this.estimatedEarthFrame = new Frame(baseFrame.getParent(), estimatedEarthFrameProvider,
                                             baseFrame.getParent() + "-estimated");
        this.arguments = fundamentalNutationArguments;

        this.displacements = displacements.clone();
        this.eastOffsetDriver   = eastOffsetDriver;
        this.northOffsetDriver  = northOffsetDriver;
        this.zenithOffsetDriver = zenithOffsetDriver;
    }

    /** {@inheritDoc} */
    @Override
    public Transform getTransform(final AbsoluteDate date) {
        // take Earth offsets into account
        final Transform intermediateToBody = estimatedEarthFrameProvider.getTransform(date).getInverse();

        // take station offsets into account
        final BodyShape baseShape = baseFrame.getParentShape();
        final Vector3D  origin    = getOrigin(date);

        final GeodeticPoint originGP = baseShape.transform(origin, baseShape.getBodyFrame(), date);
        final Transform offsetToIntermediate =
                        new Transform(date,
                                      new Transform(date,
                                                    new Rotation(Vector3D.PLUS_I, Vector3D.PLUS_K,
                                                                 originGP.getEast(), originGP.getZenith()),
                                                    Vector3D.ZERO),
                                      new Transform(date, origin));
        if (baseFrame.getParent() == frame) {
            return new Transform(date, offsetToIntermediate, intermediateToBody);
        }

        // combine all transforms together
        final Transform bodyToInert = baseFrame.getParent().getTransformTo(frame, date);

        return new Transform(date, offsetToIntermediate, new Transform(date, intermediateToBody, bodyToInert));
    }

    /** {@inheritDoc} */
    @Override
    public StaticTransform getStaticTransform(final AbsoluteDate date) {
        // take Earth offsets into account
        final StaticTransform intermediateToBody = estimatedEarthFrameProvider.getStaticTransform(date).getInverse();

        // take station offsets into account
        final BodyShape baseShape = baseFrame.getParentShape();
        final Vector3D  origin    = getOrigin(date);

        final GeodeticPoint originGP = baseShape.transform(origin, baseShape.getBodyFrame(), date);
        final StaticTransform offsetToIntermediate = StaticTransform.compose(date,
                        StaticTransform.of(date,
                                new Rotation(Vector3D.PLUS_I, Vector3D.PLUS_K,
                                        originGP.getEast(), originGP.getZenith())),
                        StaticTransform.of(date, origin));
        if (baseFrame.getParent() == frame) {
            return StaticTransform.compose(date, offsetToIntermediate, intermediateToBody);
        }

        // combine all transforms together
        final StaticTransform bodyToInert = baseFrame.getParent().getStaticTransformTo(frame, date);

        return StaticTransform.compose(date, offsetToIntermediate, StaticTransform.compose(date, intermediateToBody, bodyToInert));
    }

    /**
     * Retrieve station's position in body shape frame.
     * @param date date
     * @return origin position
     */
    private Vector3D getOrigin(final AbsoluteDate date) {
        final double    x          = eastOffsetDriver.getValue(date);
        final double    y          = northOffsetDriver.getValue(date);
        final double    z          = zenithOffsetDriver.getValue(date);
        final Frame bodyFrame = baseFrame.getParentShape().getBodyFrame();
        final StaticTransform staticTopoToBody = baseFrame.getStaticTransformTo(bodyFrame, date);
        final Vector3D        originBeforeDisplacement     = staticTopoToBody.transformPosition(new Vector3D(x, y, z));
        return originBeforeDisplacement.add(computeDisplacement(date, originBeforeDisplacement));
    }

    /** Get the station displacement.
     * @param date current date
     * @param position raw position of the station in Earth frame
     * before displacement is applied
     * @return station displacement
     */
    private Vector3D computeDisplacement(final AbsoluteDate date, final Vector3D position) {
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

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldTransform<T> getTransform(final FieldAbsoluteDate<T> date) {

        // take Earth offsets into account
        final FieldTransform<T> intermediateToBody = estimatedEarthFrameProvider.getTransform(date).getInverse();

        // take station offsets into account
        final FieldVector3D<T> origin = getOrigin(date);
        return getTransform(date, origin, intermediateToBody);

    }

    <T extends CalculusFieldElement<T>> FieldTransform<T> getTransform(final FieldAbsoluteDate<T> date,
                                                                       final FieldVector3D<T> origin,
                                                                       final FieldTransform<T> intermediateToBody) {
        final Field<T>         field = date.getField();
        final FieldVector3D<T> zero  = FieldVector3D.getZero(field);
        final FieldVector3D<T> plusI = FieldVector3D.getPlusI(field);
        final FieldVector3D<T> plusK = FieldVector3D.getPlusK(field);
        final FieldGeodeticPoint<T> originGP = baseFrame.getParentShape().transform(origin,
                baseFrame.getParentShape().getBodyFrame(), date);
        final FieldTransform<T> offsetToIntermediate =
                new FieldTransform<>(date,
                        new FieldTransform<>(date,
                                new FieldRotation<>(plusI, plusK,
                                        originGP.getEast(), originGP.getZenith()),
                                zero),
                        new FieldTransform<>(date, origin));

        // combine all transforms together
        if (baseFrame.getParent() == frame) {
            return new FieldTransform<>(date, offsetToIntermediate, intermediateToBody);
        }
        final FieldTransform<T> bodyToInert = baseFrame.getParent().getTransformTo(frame, date);

        return new FieldTransform<>(date, offsetToIntermediate,
                new FieldTransform<>(date, intermediateToBody, bodyToInert));

    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldStaticTransform<T> getStaticTransform(final FieldAbsoluteDate<T> date) {

        // take Earth offsets into account
        final FieldStaticTransform<T> intermediateToBody = estimatedEarthFrameProvider.getStaticTransform(date).getInverse();

        final FieldVector3D<T> origin = new FieldVector3D<>(date.getField(), getOrigin(date.toAbsoluteDate()));
        // take station offsets into account
        final Field<T>         field = date.getField();
        final FieldVector3D<T> plusI = FieldVector3D.getPlusI(field);
        final FieldVector3D<T> plusK = FieldVector3D.getPlusK(field);
        final FieldGeodeticPoint<T> originGP = baseFrame.getParentShape().transform(origin,
                baseFrame.getParentShape().getBodyFrame(), date);
        final FieldStaticTransform<T> offsetToIntermediate = FieldStaticTransform.compose(date,
                FieldStaticTransform.of(date,
                        new FieldRotation<>(plusI, plusK, originGP.getEast(), originGP.getZenith())),
                FieldStaticTransform.of(date, origin));

        // combine all transforms together
        if (baseFrame.getParent() == frame) {
            return FieldStaticTransform.compose(date, offsetToIntermediate, intermediateToBody);
        }
        final FieldStaticTransform<T> bodyToInert = baseFrame.getParent().getStaticTransformTo(frame, date);

        return FieldStaticTransform.compose(date, offsetToIntermediate,
                FieldStaticTransform.compose(date, intermediateToBody, bodyToInert));
    }

    /**
     * Retrieve station's position in body shape frame.
     * @param <T> field type
     * @param date date
     * @return origin position
     */
    private <T extends CalculusFieldElement<T>> FieldVector3D<T> getOrigin(final FieldAbsoluteDate<T> date) {
        final AbsoluteDate absoluteDate = date.toAbsoluteDate();
        final Field<T> field = date.getField();
        final T x          = field.getZero().newInstance(eastOffsetDriver.getValue(absoluteDate));
        final T                       y          = field.getZero().newInstance(northOffsetDriver.getValue(absoluteDate));
        final T                       z          = field.getZero().newInstance(zenithOffsetDriver.getValue(absoluteDate));
        final Frame bodyFrame = baseFrame.getParentShape().getBodyFrame();
        final FieldStaticTransform<T> staticTopoToBody = baseFrame.getStaticTransformTo(bodyFrame, date);
        final FieldVector3D<T>        originBeforeDisplacement     = staticTopoToBody.transformPosition(new FieldVector3D<>(x, y, z));
        return originBeforeDisplacement.add(computeDisplacement(absoluteDate, originBeforeDisplacement.toVector3D()));
    }

}
