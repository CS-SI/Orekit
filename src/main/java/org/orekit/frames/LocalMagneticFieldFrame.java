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
package org.orekit.frames;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.models.earth.GeoMagneticField;
import org.orekit.models.earth.ReferenceEllipsoid;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.PVCoordinates;

/**
 * This class handles a magnetic field variation attitude provider.
 * <p>
 * It was designed to be used as a Bdot attitude pointing law which align a specific body axis with Earth magnetic field
 * vector.
 * <p>
 * Attitude control thought the magnetic field is called Bdot as it follows the sinusoidal variation of the Earth magnetic
 * field vector, along the orbit. Magnetorquers are used on board to align the instrument, as so the satellite, with the
 * planet magnetic field, producing a sinusoidal torque along the orbit.
 *
 * @author Alberto Ferrero
 * @author Vincent Cucchietti
 */
public class LocalMagneticFieldFrame implements LOF {

    /** Inertial frame in which position-velocity coordinates will be given when computing transform and rotation. */
    private final Frame inertialFrame;

    /** Vector used to define the local orbital frame. */
    private final LOFBuilderVector lofBuilderVector;

    /** Body shape. */
    private final OneAxisEllipsoid wgs84BodyShape;

    /** Earth's magnetic field. */
    private final GeoMagneticField magneticField;

    /**
     * Constructor with default definition of the local orbital frame:
     * <ul>
     *     <li> x: Magnetic field</li>
     *     <li> y: Completes orthonormal frame</li>
     *     <li> z: Cross product of the magnetic field with the orbital momentum</li>
     * </ul>.
     * <b>BEWARE : Do not use this constructor if it is planned to be used with an equatorial orbit as the magnetic field and
     * orbital momentum vectors will be parallel and cause an error to be thrown</b>
     *
     * @param inertialFrame inertial frame in which position-velocity coordinates will be given when computing transform and
     * rotation
     * @param magneticField Earth magnetic field model
     * @param bodyFrame body frame related to body shape
     */
    public LocalMagneticFieldFrame(final Frame inertialFrame,
                                   final GeoMagneticField magneticField,
                                   final Frame bodyFrame) {
        this(inertialFrame, magneticField, LOFBuilderVector.PLUS_MOMENTUM, bodyFrame);
    }

    /**
     * Constructor with custom definition of the local orbital frame:
     * <ul>
     *     <li> x: Magnetic field</li>
     *     <li> y: Completes orthonormal frame</li>
     *     <li> z: Cross product of the magnetic field with chosen {@link LOFBuilderVector vector}</li>
     * </ul>
     * For near-polar orbits, it is suggested to use the {@link LOFBuilderVector orbital momentum} to define the local
     * orbital frame. However, for near-equatorial orbits, it is advised to use either the
     * {@link LOFBuilderVector position or the velocity}.
     *
     * @param inertialFrame inertial frame in which position-velocity coordinates will be given when computing transform and
     * rotation
     * @param magneticField Earth magnetic field model
     * @param lofBuilderVector vector used to define the local orbital frame
     * @param bodyFrame body frame related to body shape
     */
    public LocalMagneticFieldFrame(final Frame inertialFrame, final GeoMagneticField magneticField,
                                   final LOFBuilderVector lofBuilderVector,
                                   final Frame bodyFrame) {
        this.inertialFrame    = inertialFrame;
        this.magneticField    = magneticField;
        this.lofBuilderVector = lofBuilderVector;
        // Default WGS84 body shape as this is the one used by default in GeoMagneticField
        this.wgs84BodyShape = ReferenceEllipsoid.getWgs84(bodyFrame);
    }

    /**
     * {@inheritDoc} Direction as X axis aligned with magnetic field vector, Y axis aligned with the cross product of the
     * magnetic field vector with chosen {@link LOFBuilderVector vector type}.
     * <p>
     * <b>BEWARE: In this implementation, the method simply fieldify the normal rotation with given field.
     * Hence all derivatives are lost.</b>
     */
    @Override
    public <T extends CalculusFieldElement<T>> FieldRotation<T> rotationFromInertial(final Field<T> field,
                                                                                     final FieldAbsoluteDate<T> date,
                                                                                     final FieldPVCoordinates<T> pv) {
        // TODO Implement field equivalent of "calculateField" method in GeoMagneticField
        final Rotation rotation = rotationFromInertial(date.toAbsoluteDate(), pv.toPVCoordinates());
        return new FieldRotation<>(field, rotation);
    }

    /**
     * {@inheritDoc} Direction as X axis aligned with magnetic field vector, Z axis aligned with the cross product of the
     * magnetic field vector with chosen {@link LOFBuilderVector vector type}.
     */
    @Override
    public Rotation rotationFromInertial(final AbsoluteDate date, final PVCoordinates pv) {
        // Express satellite coordinates in body frame
        final StaticTransform inertialToBodyFrame = inertialFrame.getStaticTransformTo(wgs84BodyShape.getBodyFrame(), date);
        final Vector3D        posBody             = inertialToBodyFrame.transformPosition(pv.getPosition());

        // Compute satellite coordinates LLA and magnetic field vector in body frame
        final double   lat            = posBody.getDelta();
        final double   lng            = posBody.getAlpha();
        final double   alt            = posBody.getNorm() - wgs84BodyShape.getEquatorialRadius();
        final Vector3D magnVectorBody = magneticField.calculateField(lat, lng, alt).getFieldVector();

        // Compute magnetic field in inertial frame
        final StaticTransform bodyToInertialFrame = inertialToBodyFrame.getInverse();
        final Vector3D        magnVector          = bodyToInertialFrame.transformVector(magnVectorBody);

        return new Rotation(magnVector, magnVector.crossProduct(lofBuilderVector.getVector(pv)),
                            Vector3D.PLUS_I, Vector3D.PLUS_K);
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return "LOCAL_MAGNETIC_FIELD_FRAME";
    }

    /** Get interlai frame.
     * @return inertial frame
     */
    public Frame getInertialFrame() {
        return inertialFrame;
    }

    /** Get geomagnetid field.
     * @return geo magnetic field
     */
    public GeoMagneticField getMagneticField() {
        return magneticField;
    }

    /**
     * Enum defining how the +j axis of the local orbital frame will be defined.
     * <p>
     * For example, if {@code MINUS_MOMENTUM} is chosen, +j aligned as the cross product of the momentum vector  and the
     * magnetic field vector. The resulting body frame will be +x aligned with magnetic field, +z aligned with negative
     * momentum, +y orthonormal.
     */
    public enum LOFBuilderVector {

        /** Positive position vector. */
        PLUS_POSITION {
            /** {@inheritDoc} */
            @Override
            Vector3D getVector(final PVCoordinates pv) {
                return pv.getPosition();
            }
        },

        /** Positive velocity vector. */
        PLUS_VELOCITY {
            /** {@inheritDoc} */
            @Override
            Vector3D getVector(final PVCoordinates pv) {
                return pv.getVelocity();
            }
        },

        /** Positive orbital momentum vector. */
        PLUS_MOMENTUM {
            /** {@inheritDoc} */
            @Override
            Vector3D getVector(final PVCoordinates pv) {
                return pv.getMomentum();
            }
        },

        /** Negative position vector. */
        MINUS_POSITION {
            /** {@inheritDoc} */
            @Override
            Vector3D getVector(final PVCoordinates pv) {
                return pv.getPosition().negate();
            }
        },

        /** Negative velocity vector. */
        MINUS_VELOCITY {
            /** {@inheritDoc} */
            @Override
            Vector3D getVector(final PVCoordinates pv) {
                return pv.getVelocity().negate();
            }
        },

        /** Negative orbital momentum vector. */
        MINUS_MOMENTUM {
            /** {@inheritDoc} */
            @Override
            Vector3D getVector(final PVCoordinates pv) {
                return pv.getMomentum().negate();
            }
        };

        /**
         * @param pv position-velocity coordinates expressed in the instance inertial frame
         *
         * @return Vector used to define the local orbital frame by computing the cross product of the magnetic field with
         * this
         */
        abstract Vector3D getVector(PVCoordinates pv);
    }

}
