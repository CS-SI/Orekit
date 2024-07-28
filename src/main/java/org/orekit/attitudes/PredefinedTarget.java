/* Copyright 2002-2024 Luc Maisonobe
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
import org.hipparchus.analysis.differentiation.FieldUnivariateDerivative2Field;
import org.hipparchus.analysis.differentiation.UnivariateDerivative2;
import org.hipparchus.analysis.differentiation.UnivariateDerivative2Field;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.orekit.bodies.FieldGeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.frames.FieldTransform;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.ExtendedPVCoordinatesProvider;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.TimeStampedFieldPVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

/**
 * Predefined targets for {@link AlignedAndConstrained}.
 * @author Luc Maisonobe
 * @since 12.2
 */
public enum PredefinedTarget implements TargetProvider
{

    /** Sun direction. */
    SUN {

        /** {@inheritDoc} */
        @Override
        public FieldVector3D<UnivariateDerivative2> getTargetDirection(final ExtendedPVCoordinatesProvider sun,
                                                                       final OneAxisEllipsoid earth,
                                                                       final TimeStampedPVCoordinates pv,
                                                                       final Frame frame) {
            return new PVCoordinates(pv, sun.getPVCoordinates(pv.getDate(), frame)).
                   toUnivariateDerivative2Vector().
                   normalize();
        }

        /** {@inheritDoc} */
        @Override
        public <T extends CalculusFieldElement<T>> FieldVector3D<FieldUnivariateDerivative2<T>>
        getTargetDirection(final ExtendedPVCoordinatesProvider sun,
                           final OneAxisEllipsoid earth,
                           final TimeStampedFieldPVCoordinates<T> pv,
                           final Frame frame) {
            return new FieldPVCoordinates<>(pv, sun.getPVCoordinates(pv.getDate(), frame)).
                   toUnivariateDerivative2Vector().
                   normalize();
        }

    },

    /** Earth direction. */
    EARTH {

        /** {@inheritDoc} */
        @Override
        public FieldVector3D<UnivariateDerivative2> getTargetDirection(final ExtendedPVCoordinatesProvider sun,
                                                                       final OneAxisEllipsoid earth,
                                                                       final TimeStampedPVCoordinates pv,
                                                                       final Frame frame) {
            return pv.toUnivariateDerivative2Vector().negate().normalize();
        }

        /** {@inheritDoc} */
        @Override
        public <T extends CalculusFieldElement<T>> FieldVector3D<FieldUnivariateDerivative2<T>>
        getTargetDirection(final ExtendedPVCoordinatesProvider sun,
                           final OneAxisEllipsoid earth,
                           final TimeStampedFieldPVCoordinates<T> pv,
                           final Frame frame) {
            return pv.toUnivariateDerivative2Vector().negate().normalize();
        }

    },

    /** Nadir. */
    NADIR {

        /** {@inheritDoc} */
        @Override
        public FieldVector3D<UnivariateDerivative2> getTargetDirection(final ExtendedPVCoordinatesProvider sun,
                                                                       final OneAxisEllipsoid earth,
                                                                       final TimeStampedPVCoordinates pv,
                                                                       final Frame frame) {
            final FieldTransform<UnivariateDerivative2> inert2Earth = inert2Earth(earth, pv.getDate(), frame);
            final FieldGeodeticPoint<UnivariateDerivative2> gp = toGeodeticPoint(earth, pv, inert2Earth);
            return inert2Earth.getInverse().transformVector(gp.getNadir());
        }

        /** {@inheritDoc} */
        @Override
        public <T extends CalculusFieldElement<T>> FieldVector3D<FieldUnivariateDerivative2<T>>
        getTargetDirection(final ExtendedPVCoordinatesProvider sun,
                           final OneAxisEllipsoid earth,
                           final TimeStampedFieldPVCoordinates<T> pv,
                           final Frame frame) {
            final FieldTransform<FieldUnivariateDerivative2<T>> inert2Earth = inert2Earth(earth, pv.getDate(), frame);
            final FieldGeodeticPoint<FieldUnivariateDerivative2<T>> gp = toGeodeticPoint(earth, pv, inert2Earth);
            return inert2Earth.getInverse().transformVector(gp.getNadir());
        }

    },

    /** North direction. */
    NORTH {

        /** {@inheritDoc} */
        @Override
        public FieldVector3D<UnivariateDerivative2> getTargetDirection(final ExtendedPVCoordinatesProvider sun,
                                                                       final OneAxisEllipsoid earth,
                                                                       final TimeStampedPVCoordinates pv,
                                                                       final Frame frame) {
            final FieldTransform<UnivariateDerivative2> inert2Earth = inert2Earth(earth, pv.getDate(), frame);
            final FieldGeodeticPoint<UnivariateDerivative2> gp = toGeodeticPoint(earth, pv, inert2Earth);
            return inert2Earth.getInverse().transformVector(gp.getNorth());
        }

        /** {@inheritDoc} */
        @Override
        public <T extends CalculusFieldElement<T>> FieldVector3D<FieldUnivariateDerivative2<T>>
        getTargetDirection(final ExtendedPVCoordinatesProvider sun,
                           final OneAxisEllipsoid earth,
                           final TimeStampedFieldPVCoordinates<T> pv,
                           final Frame frame) {
            final FieldTransform<FieldUnivariateDerivative2<T>> inert2Earth = inert2Earth(earth, pv.getDate(), frame);
            final FieldGeodeticPoint<FieldUnivariateDerivative2<T>> gp = toGeodeticPoint(earth, pv, inert2Earth);
            return inert2Earth.getInverse().transformVector(gp.getNorth());
        }

    },

    /** East direction. */
    EAST {

        /** {@inheritDoc} */
        @Override
        public FieldVector3D<UnivariateDerivative2> getTargetDirection(final ExtendedPVCoordinatesProvider sun,
                                                                       final OneAxisEllipsoid earth,
                                                                       final TimeStampedPVCoordinates pv,
                                                                       final Frame frame) {
            final FieldTransform<UnivariateDerivative2> inert2Earth = inert2Earth(earth, pv.getDate(), frame);
            final FieldGeodeticPoint<UnivariateDerivative2> gp = toGeodeticPoint(earth, pv, inert2Earth);
            return inert2Earth.getInverse().transformVector(gp.getEast());
        }

        /** {@inheritDoc} */
        @Override
        public <T extends CalculusFieldElement<T>> FieldVector3D<FieldUnivariateDerivative2<T>>
        getTargetDirection(final ExtendedPVCoordinatesProvider sun,
                           final OneAxisEllipsoid earth,
                           final TimeStampedFieldPVCoordinates<T> pv,
                           final Frame frame) {
            final FieldTransform<FieldUnivariateDerivative2<T>> inert2Earth = inert2Earth(earth, pv.getDate(), frame);
            final FieldGeodeticPoint<FieldUnivariateDerivative2<T>> gp = toGeodeticPoint(earth, pv, inert2Earth);
            return inert2Earth.getInverse().transformVector(gp.getEast());
        }

    },

    /** Satellite velocity. */
    VELOCITY {

        /** {@inheritDoc} */
        @Override
        public FieldVector3D<UnivariateDerivative2> getTargetDirection(final ExtendedPVCoordinatesProvider sun,
                                                                       final OneAxisEllipsoid earth,
                                                                       final TimeStampedPVCoordinates pv,
                                                                       final Frame frame) {
            return pv.toUnivariateDerivative2PV().getVelocity().normalize();
        }

        /** {@inheritDoc} */
        @Override
        public <T extends CalculusFieldElement<T>> FieldVector3D<FieldUnivariateDerivative2<T>>
        getTargetDirection(final ExtendedPVCoordinatesProvider sun,
                           final OneAxisEllipsoid earth,
                           final TimeStampedFieldPVCoordinates<T> pv,
                           final Frame frame) {
            return pv.toUnivariateDerivative2PV().getVelocity().normalize();
        }

    },

    /** Satellite orbital momentum. */
    MOMENTUM {

        /** {@inheritDoc} */
        @Override
        public FieldVector3D<UnivariateDerivative2> getTargetDirection(final ExtendedPVCoordinatesProvider sun,
                                                                       final OneAxisEllipsoid earth,
                                                                       final TimeStampedPVCoordinates pv,
                                                                       final Frame frame) {
            return pv.toUnivariateDerivative2PV().getMomentum().normalize();
        }

        /** {@inheritDoc} */
        @Override
        public <T extends CalculusFieldElement<T>> FieldVector3D<FieldUnivariateDerivative2<T>>
        getTargetDirection(final ExtendedPVCoordinatesProvider sun,
                           final OneAxisEllipsoid earth,
                           final TimeStampedFieldPVCoordinates<T> pv,
                           final Frame frame) {
            return pv.toUnivariateDerivative2PV().getMomentum().normalize();
        }

    };

    /** Get transform from inertial frame to Earth frame.
     * @param earth Earth model
     * @param date  date
     * @param frame inertial frame
     * @return geodetic point with derivatives
     */
    private static FieldTransform<UnivariateDerivative2> inert2Earth(final OneAxisEllipsoid earth,
                                                                     final AbsoluteDate date,
                                                                     final Frame frame) {
        final FieldAbsoluteDate<UnivariateDerivative2> dateU2 =
            new FieldAbsoluteDate<>(UnivariateDerivative2Field.getInstance(), date).
            shiftedBy(new UnivariateDerivative2(0.0, 1.0, 0.0));
        return frame.getTransformTo(earth.getBodyFrame(), dateU2);
    }

    /** Convert to geodetic point with derivatives.
     * @param earth       Earth model
     * @param pv          spacecraft position and velocity
     * @param inert2Earth transform from inertial frame to Earth frame
     * @return geodetic point with derivatives
     */
    private static FieldGeodeticPoint<UnivariateDerivative2> toGeodeticPoint(final OneAxisEllipsoid earth,
                                                                             final TimeStampedPVCoordinates pv,
                                                                             final FieldTransform<UnivariateDerivative2> inert2Earth) {
        return earth.transform(inert2Earth.transformPosition(pv.toUnivariateDerivative2Vector()),
                               earth.getBodyFrame(), inert2Earth.getFieldDate());
    }

    /** Get transform from inertial frame to Earth frame.
     * @param <T>   type of the field element
     * @param earth Earth model
     * @param date  date
     * @param frame inertial frame
     * @return geodetic point with derivatives
     */
    private static <T extends CalculusFieldElement<T>> FieldTransform<FieldUnivariateDerivative2<T>>
    inert2Earth(final OneAxisEllipsoid earth, final FieldAbsoluteDate<T> date, final Frame frame) {
        final Field<T> field = date.getField();
        final FieldAbsoluteDate<FieldUnivariateDerivative2<T>> dateU2 =
            new FieldAbsoluteDate<>(FieldUnivariateDerivative2Field.getUnivariateDerivative2Field(field),
                                    date.toAbsoluteDate()).
            shiftedBy(new FieldUnivariateDerivative2<>(field.getZero(), field.getOne(), field.getZero()));
        return frame.getTransformTo(earth.getBodyFrame(), dateU2);
    }

    /** Convert to geodetic point with derivatives.
     * @param <T>         type of the field element
     * @param earth       Earth model
     * @param pv          spacecraft position and velocity
     * @param inert2Earth transform from inertial frame to Earth frame
     * @return geodetic point with derivatives
     */
    private static <T extends CalculusFieldElement<T>> FieldGeodeticPoint<FieldUnivariateDerivative2<T>>
    toGeodeticPoint(final OneAxisEllipsoid earth,
                    final TimeStampedFieldPVCoordinates<T> pv,
                    final FieldTransform<FieldUnivariateDerivative2<T>> inert2Earth) {
        return earth.transform(inert2Earth.transformPosition(pv.toUnivariateDerivative2Vector()),
                               earth.getBodyFrame(), inert2Earth.getFieldDate());
    }

}
