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
import org.hipparchus.analysis.differentiation.FieldUnivariateDerivative2;
import org.hipparchus.analysis.differentiation.UnivariateDerivative2;
import org.hipparchus.analysis.differentiation.UnivariateDerivative2Field;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.bodies.FieldGeodeticPoint;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.frames.FieldStaticTransform;
import org.orekit.frames.Frame;
import org.orekit.frames.StaticTransform;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.ExtendedPositionProvider;
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
        public FieldVector3D<UnivariateDerivative2> getDerivative2TargetDirection(final ExtendedPositionProvider sun,
                                                                                  final OneAxisEllipsoid earth,
                                                                                  final TimeStampedPVCoordinates pv,
                                                                                  final Frame frame) {
            return new PVCoordinates(pv, sun.getPVCoordinates(pv.getDate(), frame)).
                   toUnivariateDerivative2Vector().
                   normalize();
        }

        /** {@inheritDoc} */
        @Override
        public Vector3D getTargetDirection(final ExtendedPositionProvider sun, final OneAxisEllipsoid earth,
                                           final TimeStampedPVCoordinates pv, final Frame frame) {
            return sun.getPosition(pv.getDate(), frame).subtract(pv.getPosition()).normalize();
        }

        /** {@inheritDoc} */
        @Override
        public <T extends CalculusFieldElement<T>> FieldVector3D<T> getTargetDirection(final ExtendedPositionProvider sun,
                                                                                       final OneAxisEllipsoid earth,
                                                                                       final TimeStampedFieldPVCoordinates<T> pv,
                                                                                       final Frame frame) {
            return sun.getPosition(pv.getDate(), frame).subtract(pv.getPosition()).normalize();
        }
    },

    /** Earth direction (assumes the frame is Earth centered). */
    EARTH {

        /** {@inheritDoc} */
        @Override
        public FieldVector3D<UnivariateDerivative2> getDerivative2TargetDirection(final ExtendedPositionProvider sun,
                                                                                  final OneAxisEllipsoid earth,
                                                                                  final TimeStampedPVCoordinates pv,
                                                                                  final Frame frame) {
            return pv.toUnivariateDerivative2Vector().negate().normalize();
        }

        /** {@inheritDoc} */
        @Override
        public Vector3D getTargetDirection(final ExtendedPositionProvider sun, final OneAxisEllipsoid earth,
                                           final TimeStampedPVCoordinates pv, final Frame frame) {
            return pv.getPosition().negate().normalize();
        }

        /** {@inheritDoc} */
        @Override
        public <T extends CalculusFieldElement<T>> FieldVector3D<FieldUnivariateDerivative2<T>> getDerivative2TargetDirection(final ExtendedPositionProvider sun,
                                                                                                                              final OneAxisEllipsoid earth,
                                                                                                                              final TimeStampedFieldPVCoordinates<T> pv,
                                                                                                                              final Frame frame) {
            return pv.toUnivariateDerivative2Vector().negate().normalize();
        }

        /** {@inheritDoc} */
        @Override
        public <T extends CalculusFieldElement<T>> FieldVector3D<T> getTargetDirection(final ExtendedPositionProvider sun,
                                                                                       final OneAxisEllipsoid earth,
                                                                                       final TimeStampedFieldPVCoordinates<T> pv,
                                                                                       final Frame frame) {
            return pv.getPosition().negate().normalize();
        }
    },

    /** Nadir. */
    NADIR {

        /** {@inheritDoc} */
        @Override
        public FieldVector3D<UnivariateDerivative2> getDerivative2TargetDirection(final ExtendedPositionProvider sun,
                                                                                  final OneAxisEllipsoid earth,
                                                                                  final TimeStampedPVCoordinates pv,
                                                                                  final Frame frame) {
            final FieldStaticTransform<UnivariateDerivative2> inert2Earth = inert2Earth(earth, pv.getDate(), frame);
            final FieldGeodeticPoint<UnivariateDerivative2> gp = toGeodeticPoint(earth, pv, inert2Earth);
            return inert2Earth.getStaticInverse().transformVector(gp.getNadir());
        }

        /** {@inheritDoc} */
        @Override
        public Vector3D getTargetDirection(final ExtendedPositionProvider sun, final OneAxisEllipsoid earth,
                                           final TimeStampedPVCoordinates pv, final Frame frame) {
            final StaticTransform inert2Earth = frame.getStaticTransformTo(earth.getBodyFrame(), pv.getDate());
            final GeodeticPoint geodeticPoint = earth.transform(inert2Earth.transformPosition(pv.getPosition()),
                    earth.getBodyFrame(), pv.getDate());
            return inert2Earth.getStaticInverse().transformVector(geodeticPoint.getNadir());
        }

        /** {@inheritDoc} */
        @Override
        public <T extends CalculusFieldElement<T>> FieldVector3D<T> getTargetDirection(final ExtendedPositionProvider sun,
                                                                                       final OneAxisEllipsoid earth,
                                                                                       final TimeStampedFieldPVCoordinates<T> pv,
                                                                                       final Frame frame) {
            final FieldStaticTransform<T> inert2Earth = frame.getStaticTransformTo(earth.getBodyFrame(), pv.getDate());
            final FieldGeodeticPoint<T> geodeticPoint = earth.transform(inert2Earth.transformPosition(pv.getPosition()),
                    earth.getBodyFrame(), pv.getDate());
            return inert2Earth.getStaticInverse().transformVector(geodeticPoint.getNadir());
        }
    },

    /** North direction. */
    NORTH {

        /** {@inheritDoc} */
        @Override
        public FieldVector3D<UnivariateDerivative2> getDerivative2TargetDirection(final ExtendedPositionProvider sun,
                                                                                  final OneAxisEllipsoid earth,
                                                                                  final TimeStampedPVCoordinates pv,
                                                                                  final Frame frame) {
            final FieldStaticTransform<UnivariateDerivative2> inert2Earth = inert2Earth(earth, pv.getDate(), frame);
            final FieldGeodeticPoint<UnivariateDerivative2> gp = toGeodeticPoint(earth, pv, inert2Earth);
            return inert2Earth.getStaticInverse().transformVector(gp.getNorth());
        }

        /** {@inheritDoc} */
        @Override
        public Vector3D getTargetDirection(final ExtendedPositionProvider sun, final OneAxisEllipsoid earth,
                                           final TimeStampedPVCoordinates pv, final Frame frame) {
            final StaticTransform inert2Earth = frame.getStaticTransformTo(earth.getBodyFrame(), pv.getDate());
            final GeodeticPoint geodeticPoint = earth.transform(inert2Earth.transformPosition(pv.getPosition()),
                    earth.getBodyFrame(), pv.getDate());
            return inert2Earth.getStaticInverse().transformVector(geodeticPoint.getNorth());
        }

        /** {@inheritDoc} */
        @Override
        public <T extends CalculusFieldElement<T>> FieldVector3D<T> getTargetDirection(final ExtendedPositionProvider sun,
                                                                                       final OneAxisEllipsoid earth,
                                                                                       final TimeStampedFieldPVCoordinates<T> pv,
                                                                                       final Frame frame) {
            final FieldStaticTransform<T> inert2Earth = frame.getStaticTransformTo(earth.getBodyFrame(), pv.getDate());
            final FieldGeodeticPoint<T> geodeticPoint = earth.transform(inert2Earth.transformPosition(pv.getPosition()),
                    earth.getBodyFrame(), pv.getDate());
            return inert2Earth.getStaticInverse().transformVector(geodeticPoint.getNorth());
        }
    },

    /** East direction. */
    EAST {

        /** {@inheritDoc} */
        @Override
        public FieldVector3D<UnivariateDerivative2> getDerivative2TargetDirection(final ExtendedPositionProvider sun,
                                                                                  final OneAxisEllipsoid earth,
                                                                                  final TimeStampedPVCoordinates pv,
                                                                                  final Frame frame) {
            final FieldStaticTransform<UnivariateDerivative2> inert2Earth = inert2Earth(earth, pv.getDate(), frame);
            final FieldGeodeticPoint<UnivariateDerivative2> gp = toGeodeticPoint(earth, pv, inert2Earth);
            return inert2Earth.getStaticInverse().transformVector(gp.getEast());
        }

        /** {@inheritDoc} */
        @Override
        public Vector3D getTargetDirection(final ExtendedPositionProvider sun, final OneAxisEllipsoid earth,
                                           final TimeStampedPVCoordinates pv, final Frame frame) {
            final StaticTransform inert2Earth = frame.getStaticTransformTo(earth.getBodyFrame(), pv.getDate());
            final GeodeticPoint geodeticPoint = earth.transform(inert2Earth.transformPosition(pv.getPosition()),
                    earth.getBodyFrame(), pv.getDate());
            return inert2Earth.getStaticInverse().transformVector(geodeticPoint.getEast());
        }

        /** {@inheritDoc} */
        @Override
        public <T extends CalculusFieldElement<T>> FieldVector3D<T> getTargetDirection(final ExtendedPositionProvider sun,
                                                                                       final OneAxisEllipsoid earth,
                                                                                       final TimeStampedFieldPVCoordinates<T> pv,
                                                                                       final Frame frame) {
            final FieldStaticTransform<T> inert2Earth = frame.getStaticTransformTo(earth.getBodyFrame(), pv.getDate());
            final FieldGeodeticPoint<T> geodeticPoint = earth.transform(inert2Earth.transformPosition(pv.getPosition()),
                    earth.getBodyFrame(), pv.getDate());
            return inert2Earth.getStaticInverse().transformVector(geodeticPoint.getEast());
        }
    },

    /** Satellite velocity. */
    VELOCITY {

        /** {@inheritDoc} */
        @Override
        public FieldVector3D<UnivariateDerivative2> getDerivative2TargetDirection(final ExtendedPositionProvider sun,
                                                                                  final OneAxisEllipsoid earth,
                                                                                  final TimeStampedPVCoordinates pv,
                                                                                  final Frame frame) {
            return pv.toUnivariateDerivative2PV().getVelocity().normalize();
        }

        /** {@inheritDoc} */
        @Override
        public Vector3D getTargetDirection(final ExtendedPositionProvider sun, final OneAxisEllipsoid earth,
                                                final TimeStampedPVCoordinates pv, final Frame frame) {
            return pv.getVelocity().normalize();
        }

        /** {@inheritDoc} */
        @Override
        public <T extends CalculusFieldElement<T>> FieldVector3D<FieldUnivariateDerivative2<T>> getDerivative2TargetDirection(final ExtendedPositionProvider sun,
                                                                                                                              final OneAxisEllipsoid earth,
                                                                                                                              final TimeStampedFieldPVCoordinates<T> pv,
                                                                                                                              final Frame frame) {
            return pv.toUnivariateDerivative2PV().getVelocity().normalize();
        }

        /** {@inheritDoc} */
        @Override
        public <T extends CalculusFieldElement<T>> FieldVector3D<T> getTargetDirection(final ExtendedPositionProvider sun,
                                                                                       final OneAxisEllipsoid earth,
                                                                                       final TimeStampedFieldPVCoordinates<T> pv,
                                                                                       final Frame frame) {
            return pv.getVelocity().normalize();
        }
    },

    /** Satellite orbital momentum. */
    MOMENTUM {

        /** {@inheritDoc} */
        @Override
        public FieldVector3D<UnivariateDerivative2> getDerivative2TargetDirection(final ExtendedPositionProvider sun,
                                                                                  final OneAxisEllipsoid earth,
                                                                                  final TimeStampedPVCoordinates pv,
                                                                                  final Frame frame) {
            return pv.toUnivariateDerivative2PV().getMomentum().normalize();
        }

        /** {@inheritDoc} */
        @Override
        public Vector3D getTargetDirection(final ExtendedPositionProvider sun, final OneAxisEllipsoid earth,
                                           final TimeStampedPVCoordinates pv, final Frame frame) {
            return pv.getMomentum().normalize();
        }

        /** {@inheritDoc} */
        @Override
        public <T extends CalculusFieldElement<T>> FieldVector3D<FieldUnivariateDerivative2<T>> getDerivative2TargetDirection(final ExtendedPositionProvider sun,
                                                                                                                              final OneAxisEllipsoid earth,
                                                                                                                              final TimeStampedFieldPVCoordinates<T> pv,
                                                                                                                              final Frame frame) {
            return pv.toUnivariateDerivative2PV().getMomentum().normalize();
        }

        /** {@inheritDoc} */
        @Override
        public <T extends CalculusFieldElement<T>> FieldVector3D<T> getTargetDirection(final ExtendedPositionProvider sun,
                                                                                       final OneAxisEllipsoid earth,
                                                                                       final TimeStampedFieldPVCoordinates<T> pv,
                                                                                       final Frame frame) {
            return pv.getMomentum().normalize();
        }
    };

    /** Get transform from inertial frame to Earth frame.
     * @param earth Earth model
     * @param date  date
     * @param frame inertial frame
     * @return geodetic point with derivatives
     */
    private static FieldStaticTransform<UnivariateDerivative2> inert2Earth(final OneAxisEllipsoid earth,
                                                                     final AbsoluteDate date,
                                                                     final Frame frame) {
        final FieldAbsoluteDate<UnivariateDerivative2> dateU2 =
            new FieldAbsoluteDate<>(UnivariateDerivative2Field.getInstance(), date).
            shiftedBy(new UnivariateDerivative2(0.0, 1.0, 0.0));
        return frame.getStaticTransformTo(earth.getBodyFrame(), dateU2);
    }

    /** Convert to geodetic point with derivatives.
     * @param earth       Earth model
     * @param pv          spacecraft position and velocity
     * @param inert2Earth transform from inertial frame to Earth frame
     * @return geodetic point with derivatives
     */
    private static FieldGeodeticPoint<UnivariateDerivative2> toGeodeticPoint(final OneAxisEllipsoid earth,
                                                                             final TimeStampedPVCoordinates pv,
                                                                             final FieldStaticTransform<UnivariateDerivative2> inert2Earth) {
        return earth.transform(inert2Earth.transformPosition(pv.toUnivariateDerivative2Vector()),
                               earth.getBodyFrame(), inert2Earth.getFieldDate());
    }
}
