/* Copyright 2002-2025 CS GROUP
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
package org.orekit.bodies;


import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.Precision;
import org.orekit.frames.FieldStaticTransform;
import org.orekit.frames.FieldTransform;
import org.orekit.frames.Frame;
import org.orekit.frames.StaticTransform;
import org.orekit.frames.Transform;
import org.orekit.frames.TransformProvider;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.TimeStampedFieldPVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

/** Implementation of the {@link CelestialBody} interface using JPL or INPOP ephemerides.
 * @author Luc Maisonobe
 */
class JPLCelestialBody implements CelestialBody {

    /** Name of the body. */
    private final String name;

    /** Regular expression for supported files names. */
    private final String supportedNames;

    /** Ephemeris type to generate. */
    private final JPLEphemeridesLoader.EphemerisType generateType;

    /** Raw position-velocity provider. */
    private final transient JPLEphemeridesLoader.RawPVProvider rawPVProvider;

    /** Attraction coefficient of the body (m³/s²). */
    private final double gm;

    /** Scaling factor for position-velocity. */
    private final double scale;

    /** IAU pole. */
    private final IAUPole iauPole;

    /** Inertially oriented, body-centered frame. */
    private final Frame inertialFrame;

    /** Body oriented, body-centered frame. */
    private final Frame bodyFrame;

    /** Build an instance and the underlying frame.
     * @param name name of the body
     * @param supportedNames regular expression for supported files names
     * @param generateType ephemeris type to generate
     * @param rawPVProvider raw position-velocity provider
     * @param gm attraction coefficient (in m³/s²)
     * @param scale scaling factor for position-velocity
     * @param iauPole IAU pole implementation
     * @param definingFrameAlignedWithICRF frame in which celestial body coordinates are defined,
     * this frame <strong>must</strong> be aligned with ICRF
     * @param inertialFrameName name to use for inertial frame (if null a default name will be built)
     * @param bodyOrientedFrameName name to use for body-oriented frame (if null a default name will be built)
     */
    JPLCelestialBody(final String name, final String supportedNames,
                     final JPLEphemeridesLoader.EphemerisType generateType,
                     final JPLEphemeridesLoader.RawPVProvider rawPVProvider,
                     final double gm, final double scale,
                     final IAUPole iauPole, final Frame definingFrameAlignedWithICRF,
                     final String inertialFrameName, final String bodyOrientedFrameName) {
        this.name           = name;
        this.gm             = gm;
        this.scale          = scale;
        this.supportedNames = supportedNames;
        this.generateType   = generateType;
        this.rawPVProvider  = rawPVProvider;
        this.iauPole        = iauPole;
        this.inertialFrame  = new InertiallyOriented(definingFrameAlignedWithICRF, inertialFrameName);
        this.bodyFrame      = new BodyOriented(bodyOrientedFrameName);
    }

    /** {@inheritDoc} */
    public TimeStampedPVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) {

        // apply the scale factor to raw position-velocity
        final PVCoordinates rawPV    = rawPVProvider.getRawPV(date);
        final TimeStampedPVCoordinates scaledPV = new TimeStampedPVCoordinates(date, scale, rawPV);

        // the raw PV are relative to the parent of the body centered inertially oriented frame
        final Transform transform = getInertiallyOrientedFrame().getParent().getTransformTo(frame, date);

        // convert to requested frame
        return transform.transformPVCoordinates(scaledPV);

    }

    /** Get the {@link FieldPVCoordinates} of the body in the selected frame.
     * @param date current date
     * @param frame the frame where to define the position
     * @param <T> type of the field elements
     * @return time-stamped position/velocity of the body (m and m/s)
     */
    public <T extends CalculusFieldElement<T>> TimeStampedFieldPVCoordinates<T> getPVCoordinates(final FieldAbsoluteDate<T> date,
                                                                                                 final Frame frame) {

        // apply the scale factor to raw position-velocity
        final FieldPVCoordinates<T>            rawPV    = rawPVProvider.getRawPV(date);
        final TimeStampedFieldPVCoordinates<T> scaledPV = new TimeStampedFieldPVCoordinates<>(date, scale, rawPV);

        // the raw PV are relative to the parent of the body centered inertially oriented frame
        final FieldTransform<T> transform = getInertiallyOrientedFrame().getParent().getTransformTo(frame, date);

        // convert to requested frame
        return transform.transformPVCoordinates(scaledPV);

    }

    /** {@inheritDoc} */
    @Override
    public Vector3D getPosition(final AbsoluteDate date, final Frame frame) {

        // apply the scale factor to raw position
        final Vector3D rawPosition    = rawPVProvider.getRawPosition(date);
        final Vector3D scaledPosition = rawPosition.scalarMultiply(scale);

        // the raw position is relative to the parent of the body centered inertially oriented frame
        final StaticTransform transform = getInertiallyOrientedFrame().getParent().getStaticTransformTo(frame, date);

        // convert to requested frame
        return transform.transformPosition(scaledPosition);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldVector3D<T> getPosition(final FieldAbsoluteDate<T> date, final Frame frame) {

        // apply the scale factor to raw position
        final FieldVector3D<T> rawPosition     = rawPVProvider.getRawPosition(date);
        final FieldVector3D<T> scaledPosition  = rawPosition.scalarMultiply(scale);

        // the raw position is relative to the parent of the body centered inertially oriented frame
        final FieldStaticTransform<T> transform = getInertiallyOrientedFrame().getParent().getStaticTransformTo(frame, date);

        // convert to requested frame
        return transform.transformPosition(scaledPosition);
    }

    /** {@inheritDoc} */
    public String getName() {
        return name;
    }

    /** {@inheritDoc} */
    public double getGM() {
        return gm;
    }

    /** {@inheritDoc} */
    public Frame getInertiallyOrientedFrame() {
        return inertialFrame;
    }

    /** {@inheritDoc} */
    public Frame getBodyOrientedFrame() {
        return bodyFrame;
    }

    /** Inertially oriented body centered frame. */
    private class InertiallyOriented extends Frame {

        /** Suffix for inertial frame name. */
        private static final String INERTIAL_FRAME_SUFFIX = "/inertial";

        /** Simple constructor.
         * @param definingFrame frame in which celestial body coordinates are defined
         * @param frameName name to use (if null a default name will be built)
         */
        InertiallyOriented(final Frame definingFrame, final String frameName) {
            super(definingFrame, new TransformProvider() {

                /** {@inheritDoc} */
                public Transform getTransform(final AbsoluteDate date) {

                    // compute translation from parent frame to self
                    final PVCoordinates pv = getPVCoordinates(date, definingFrame);
                    final Transform translation = new Transform(date, pv.negate());

                    // compute rotation from ICRF frame to self,
                    // as per the "Report of the IAU/IAG Working Group on Cartographic
                    // Coordinates and Rotational Elements of the Planets and Satellites"
                    // These definitions are common for all recent versions of this report
                    // published every three years, the precise values of pole direction
                    // and W angle coefficients may vary from publication year as models are
                    // adjusted. These coefficients are not in this class, they are in the
                    // specialized classes that do implement the getPole and getPrimeMeridianAngle
                    // methods
                    final Vector3D pole  = iauPole.getPole(date);
                    final Vector3D qNode = iauPole.getNode(date);
                    final Transform rotation =
                                    new Transform(date, new Rotation(pole, qNode, Vector3D.PLUS_K, Vector3D.PLUS_I));

                    // update transform from parent to self
                    return new Transform(date, translation, rotation);

                }

                @Override
                public StaticTransform getStaticTransform(final AbsoluteDate date) {
                    // compute translation from parent frame to self
                    final Vector3D position = getPosition(date, definingFrame);

                    // compute rotation from ICRF frame to self,
                    // as per the "Report of the IAU/IAG Working Group on Cartographic
                    // Coordinates and Rotational Elements of the Planets and Satellites"
                    // These definitions are common for all recent versions of this report
                    // published every three years, the precise values of pole direction
                    // and W angle coefficients may vary from publication year as models are
                    // adjusted. These coefficients are not in this class, they are in the
                    // specialized classes that do implement the getPole and getPrimeMeridianAngle
                    // methods
                    final Vector3D pole  = iauPole.getPole(date);
                    final Vector3D qNode = iauPole.getNode(date);
                    final Rotation rotation =
                                    new Rotation(pole, qNode, Vector3D.PLUS_K, Vector3D.PLUS_I);

                    // update transform from parent to self
                    return StaticTransform.of(date, position.negate(), rotation);
                }

                /** {@inheritDoc} */
                public <T extends CalculusFieldElement<T>> FieldTransform<T> getTransform(final FieldAbsoluteDate<T> date) {

                    // compute translation from parent frame to self
                    final FieldPVCoordinates<T> pv = getPVCoordinates(date, definingFrame);
                    final FieldTransform<T> translation = new FieldTransform<>(date, pv.negate());

                    // compute rotation from ICRF frame to self,
                    // as per the "Report of the IAU/IAG Working Group on Cartographic
                    // Coordinates and Rotational Elements of the Planets and Satellites"
                    // These definitions are common for all recent versions of this report
                    // published every three years, the precise values of pole direction
                    // and W angle coefficients may vary from publication year as models are
                    // adjusted. These coefficients are not in this class, they are in the
                    // specialized classes that do implement the getPole and getPrimeMeridianAngle
                    // methods
                    final FieldVector3D<T> pole  = iauPole.getPole(date);
                    FieldVector3D<T> qNode = FieldVector3D.crossProduct(Vector3D.PLUS_K, pole);
                    if (qNode.getNormSq().getReal() < Precision.SAFE_MIN) {
                        qNode = FieldVector3D.getPlusI(date.getField());
                    }
                    final FieldTransform<T> rotation =
                                    new FieldTransform<>(date,
                                                    new FieldRotation<>(pole,
                                                                    qNode,
                                                                    FieldVector3D.getPlusK(date.getField()),
                                                                    FieldVector3D.getPlusI(date.getField())));

                    // update transform from parent to self
                    return new FieldTransform<>(date, translation, rotation);

                }

                @Override
                public <T extends CalculusFieldElement<T>> FieldStaticTransform<T> getStaticTransform(final FieldAbsoluteDate<T> date) {
                    // field
                    final Field<T> field = date.getField();
                    // compute translation from parent frame to self
                    final FieldVector3D<T> position = getPosition(date, definingFrame);

                    // compute rotation from ICRF frame to self,
                    // as per the "Report of the IAU/IAG Working Group on Cartographic
                    // Coordinates and Rotational Elements of the Planets and Satellites"
                    // These definitions are common for all recent versions of this report
                    // published every three years, the precise values of pole direction
                    // and W angle coefficients may vary from publication year as models are
                    // adjusted. These coefficients are not in this class, they are in the
                    // specialized classes that do implement the getPole and getPrimeMeridianAngle
                    // methods
                    final FieldVector3D<T> pole  = iauPole.getPole(date);
                    final FieldVector3D<T> qNode = iauPole.getNode(date);
                    final FieldRotation<T> rotation =
                                    new FieldRotation<>(pole, qNode, FieldVector3D.getPlusK(field), FieldVector3D.getPlusI(field));

                    // update transform from parent to self
                    return FieldStaticTransform.of(date, position.negate(), rotation);
                }

            }, frameName == null ? name + INERTIAL_FRAME_SUFFIX : frameName, true);
        }

    }

    /** Body oriented body centered frame. */
    private class BodyOriented extends Frame {

        /**
         * Suffix for body frame name.
         */
        private static final String BODY_FRAME_SUFFIX = "/rotating";

        /**
         * Simple constructor.
         *
         * @param frameName name to use (if null a default name will be built)
         */
        BodyOriented(final String frameName) {
            super(inertialFrame, new TransformProvider() {

                /** {@inheritDoc} */
                public Transform getTransform(final AbsoluteDate date) {
                    final double dt = 10.0;
                    final double w0 = iauPole.getPrimeMeridianAngle(date);
                    final double w1 = iauPole.getPrimeMeridianAngle(date.shiftedBy(dt));
                    return new Transform(date,
                            new Rotation(Vector3D.PLUS_K, w0, RotationConvention.FRAME_TRANSFORM),
                            new Vector3D((w1 - w0) / dt, Vector3D.PLUS_K));
                }

                /** {@inheritDoc} */
                public <T extends CalculusFieldElement<T>> FieldTransform<T> getTransform(final FieldAbsoluteDate<T> date) {
                    final double dt = 10.0;
                    final T w0 = iauPole.getPrimeMeridianAngle(date);
                    final T w1 = iauPole.getPrimeMeridianAngle(date.shiftedBy(dt));
                    return new FieldTransform<>(date,
                            new FieldRotation<>(FieldVector3D.getPlusK(date.getField()), w0,
                                    RotationConvention.FRAME_TRANSFORM),
                            new FieldVector3D<>(w1.subtract(w0).divide(dt), Vector3D.PLUS_K));
                }

            }, frameName == null ? name + BODY_FRAME_SUFFIX : frameName, false);
        }
    }
}
