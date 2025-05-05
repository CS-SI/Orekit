/* Copyright 2022-2025 Luc Maisonobe
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
package org.orekit.attitudes;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.analysis.differentiation.FieldUnivariateDerivative2;
import org.hipparchus.analysis.differentiation.UnivariateDerivative2;
import org.hipparchus.analysis.differentiation.UnivariateDerivative2Field;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.AngularCoordinates;
import org.orekit.utils.ExtendedPositionProvider;
import org.orekit.utils.FieldAngularCoordinates;
import org.orekit.utils.FieldPVCoordinatesProvider;
import org.orekit.utils.PVCoordinatesProvider;
import org.orekit.utils.TimeStampedFieldPVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

import java.util.HashMap;
import java.util.Map;

/**
 * Attitude provider with one satellite vector aligned and another one constrained to two targets.
 * @author Luc Maisonobe
 * @since 12.2
 */
public class AlignedAndConstrained implements AttitudeProvider
{

    /** Satellite vector for primary target. */
    private final FieldVector3D<UnivariateDerivative2> primarySat;

    /** Primary target. */
    private final TargetProvider primaryTarget;

    /** Satellite vector for secondary target. */
    private final FieldVector3D<UnivariateDerivative2> secondarySat;

    /** Secondary target. */
    private final TargetProvider secondaryTarget;

    /** Sun model. */
    private final ExtendedPositionProvider sun;

    /** Earth model. */
    private final OneAxisEllipsoid earth;

    /** Reference inertial frame. */
    private final Frame inertialFrame;

    /** Cached field-based satellite vectors. */
    private final transient Map<Field<? extends CalculusFieldElement<?>>, Cache<? extends CalculusFieldElement<?>>>
        cachedSatelliteVectors;

    /**
     * Simple constructor.
     * @param primarySat      satellite vector for primary target
     * @param primaryTarget   primary target
     * @param secondarySat    satellite vector for secondary target
     * @param secondaryTarget secondary target
     * @param inertialFrame   reference inertial frame
     * @param sun             Sun model
     * @param earth           Earth model
     * @since 13.0
     */
    public AlignedAndConstrained(final Vector3D primarySat, final TargetProvider primaryTarget,
                                 final Vector3D secondarySat, final TargetProvider secondaryTarget,
                                 final Frame inertialFrame, final ExtendedPositionProvider sun,
                                 final OneAxisEllipsoid earth) {
        if (!inertialFrame.isPseudoInertial()) {
            throw new OrekitException(OrekitMessages.NON_PSEUDO_INERTIAL_FRAME, inertialFrame.getName());
        }
        this.primarySat             = new FieldVector3D<>(UnivariateDerivative2Field.getInstance(), primarySat);
        this.primaryTarget          = primaryTarget;
        this.secondarySat           = new FieldVector3D<>(UnivariateDerivative2Field.getInstance(), secondarySat);
        this.secondaryTarget        = secondaryTarget;
        this.inertialFrame          = inertialFrame;
        this.sun                    = sun;
        this.earth                  = earth;
        this.cachedSatelliteVectors = new HashMap<>();
    }

    /**
     * Constructor with default inertial frame.
     * @param primarySat      satellite vector for primary target
     * @param primaryTarget   primary target
     * @param secondarySat    satellite vector for secondary target
     * @param secondaryTarget secondary target
     * @param sun             Sun model
     * @param earth           Earth model
     */
    @DefaultDataContext
    public AlignedAndConstrained(final Vector3D primarySat, final TargetProvider primaryTarget,
                                 final Vector3D secondarySat, final TargetProvider secondaryTarget,
                                 final ExtendedPositionProvider sun,
                                 final OneAxisEllipsoid earth)
    {
        this(primarySat, primaryTarget, secondarySat, secondaryTarget, FramesFactory.getGCRF(), sun, earth);
    }

    /** {@inheritDoc} */
    @Override
    public Rotation getAttitudeRotation(final PVCoordinatesProvider pvProv, final AbsoluteDate date, final Frame frame) {
        final TimeStampedPVCoordinates satPV = pvProv.getPVCoordinates(date, inertialFrame);

        // compute targets references at the specified date
        final Vector3D primaryDirection   = primaryTarget.getTargetDirection(sun, earth, satPV, inertialFrame);
        final Vector3D secondaryDirection = secondaryTarget.getTargetDirection(sun, earth, satPV, inertialFrame);

        // compute transform from inertial frame to satellite frame
        final Rotation rotation = new Rotation(primaryDirection, secondaryDirection, primarySat.toVector3D(),
                secondarySat.toVector3D());
        if (inertialFrame != frame) {
            // prepend transform from specified frame to inertial frame
            final Rotation prepended = frame.getStaticTransformTo(inertialFrame, date).getRotation();
            return rotation.compose(prepended, RotationConvention.VECTOR_OPERATOR);
        }
        return rotation;
    }

    /** {@inheritDoc} */
    @Override
    public Attitude getAttitude(final PVCoordinatesProvider pvProv,
                                final AbsoluteDate date,
                                final Frame frame)
    {
        final TimeStampedPVCoordinates satPV = pvProv.getPVCoordinates(date, inertialFrame);

        // compute targets references at the specified date
        final FieldVector3D<UnivariateDerivative2> primaryDirection   = primaryTarget.getDerivative2TargetDirection(sun,
                earth, satPV, inertialFrame);
        final FieldVector3D<UnivariateDerivative2> secondaryDirection = secondaryTarget.getDerivative2TargetDirection(sun,
                earth, satPV, inertialFrame);

        // compute transform from inertial frame to satellite frame
        final FieldRotation<UnivariateDerivative2> inertToSatRotation =
            new FieldRotation<>(primaryDirection, secondaryDirection, primarySat, secondarySat);

        // build the angular coordinates
        final AngularCoordinates angularCoordinates = new AngularCoordinates(inertToSatRotation);
        final Attitude attitude = new Attitude(date, inertialFrame, angularCoordinates);
        return attitude.withReferenceFrame(frame);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldRotation<T> getAttitudeRotation(final FieldPVCoordinatesProvider<T> pvProv,
                                                                                    final FieldAbsoluteDate<T> date,
                                                                                    final Frame frame) {
        final TimeStampedFieldPVCoordinates<T> satPV = pvProv.getPVCoordinates(date, inertialFrame);

        // compute targets references at the specified date
        final FieldVector3D<T> primaryDirection   = primaryTarget.getTargetDirection(sun, earth, satPV, inertialFrame);
        final FieldVector3D<T> secondaryDirection = secondaryTarget.getTargetDirection(sun, earth, satPV, inertialFrame);

        // compute transform from inertial frame to satellite frame
        final Field<T> field = date.getField();
        final FieldRotation<T> rotation = new FieldRotation<>(primaryDirection, secondaryDirection,
                new FieldVector3D<>(field, primarySat.toVector3D()), new FieldVector3D<>(field, secondarySat.toVector3D()));
        if (inertialFrame != frame) {
            // prepend transform from specified frame to inertial frame
            final FieldRotation<T> prepended = frame.getStaticTransformTo(inertialFrame, date).getRotation();
            return rotation.compose(prepended, RotationConvention.VECTOR_OPERATOR);
        }
        return rotation;
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldAttitude<T> getAttitude(final FieldPVCoordinatesProvider<T> pvProv,
                                                                            final FieldAbsoluteDate<T> date,
                                                                            final Frame frame)
    {
        // get the satellite vectors for specified field
        @SuppressWarnings("unchecked")
        final Cache<T> satVectors =
            (Cache<T>) cachedSatelliteVectors.computeIfAbsent(date.getField(),
                                                              f -> new Cache<>(date.getField(), primarySat, secondarySat));

        final TimeStampedFieldPVCoordinates<T> satPV = pvProv.getPVCoordinates(date, inertialFrame);

        // compute targets references at the specified date
        final FieldVector3D<FieldUnivariateDerivative2<T>> primaryDirection   = primaryTarget.getDerivative2TargetDirection(sun,
                earth, satPV, inertialFrame);
        final FieldVector3D<FieldUnivariateDerivative2<T>> secondaryDirection = secondaryTarget.getDerivative2TargetDirection(sun,
                earth, satPV, inertialFrame);

        // compute transform from inertial frame to satellite frame
        final FieldRotation<FieldUnivariateDerivative2<T>> inertToSatRotation =
            new FieldRotation<>(primaryDirection, secondaryDirection, satVectors.primarySat, satVectors.secondarySat);

        // build the attitude
        final FieldAngularCoordinates<T> angularCoordinates = new FieldAngularCoordinates<>(inertToSatRotation);
        final FieldAttitude<T> attitude = new FieldAttitude<>(date, inertialFrame, angularCoordinates);
        return attitude.withReferenceFrame(frame);
    }

    /** Container for cached satellite vectors. */
    private static class Cache<T extends CalculusFieldElement<T>> {

        /** Satellite vector for primary target. */
        private final FieldVector3D<FieldUnivariateDerivative2<T>> primarySat;

        /** Satellite vector for primary target. */
        private final FieldVector3D<FieldUnivariateDerivative2<T>> secondarySat;

        /** Simple constructor.
         * @param field field to which the elements belong
         * @param primarySat satellite vector for primary target
         * @param secondarySat satellite vector for primary target
         */
        Cache(final Field<T> field,
              final FieldVector3D<UnivariateDerivative2> primarySat,
              final FieldVector3D<UnivariateDerivative2> secondarySat) {
            final FieldUnivariateDerivative2<T> zero =
                new FieldUnivariateDerivative2<>(field.getZero(), field.getZero(), field.getZero());
            this.primarySat   = new FieldVector3D<>(zero.newInstance(primarySat.getX().getValue()),
                                                    zero.newInstance(primarySat.getY().getValue()),
                                                    zero.newInstance(primarySat.getZ().getValue()));
            this.secondarySat = new FieldVector3D<>(zero.newInstance(secondarySat.getX().getValue()),
                                                    zero.newInstance(secondarySat.getY().getValue()),
                                                    zero.newInstance(secondarySat.getZ().getValue()));
        }

    }

}
