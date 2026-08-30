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
package org.orekit.bodies;


import org.hipparchus.CalculusFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.Precision;
import org.orekit.frames.FieldKinematicTransform;
import org.orekit.frames.FieldStaticTransform;
import org.orekit.frames.FieldTransform;
import org.orekit.frames.Frame;
import org.orekit.frames.KinematicTransform;
import org.orekit.frames.StaticTransform;
import org.orekit.frames.Transform;
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
    private final JPLEphemeridesLoader.RawPVProvider rawPVProvider;

    /** Attraction coefficient of the body (m³/s²). */
    private final double gm;

    /** Scaling factor for position-velocity. */
    private final double scale;

    /** IAU pole. */
    private final IAUPole iauPole;

    /** Body's PV coordinates are defined in this frame. */
    private final Frame definingFrameAlignedWithIcrf;

    /** Body centered frame aligned with ICRF. */
    private final Frame icrfAlignedFrame;

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
     * @since 14.0
     */
    JPLCelestialBody(final String name, final String supportedNames,
                     final JPLEphemeridesLoader.EphemerisType generateType,
                     final JPLEphemeridesLoader.RawPVProvider rawPVProvider,
                     final double gm, final double scale,
                     final IAUPole iauPole,
                     final Frame definingFrameAlignedWithICRF) {
        this.name           = name;
        this.gm             = gm;
        this.scale          = scale;
        this.supportedNames = supportedNames;
        this.generateType   = generateType;
        this.rawPVProvider  = rawPVProvider;
        this.iauPole        = iauPole;
        this.definingFrameAlignedWithIcrf = definingFrameAlignedWithICRF;
        if (rawPVProvider instanceof ZeroRawPVProvider) {
            // no translation or rotation needed, use directly
            // might be better to have a method instead of using "instanceof"
            // but the classes are tightly coupled and package private
            this.icrfAlignedFrame = definingFrameAlignedWithICRF;
        } else {
            // translation needed
            final String icrfName;
            if (EphemerisType.SOLAR_SYSTEM_BARYCENTER == generateType) {
                // in Orekit FramesFactory.getICRF() is implemented by
                // CelestialBodyFactor.getSsb().getInertiallyOrientedFrame()
                // so have to match Predefined.ICRF
                icrfName = Predefined.ICRF.getName();
            } else {
                icrfName = name + "/ICRF";
            }
            this.icrfAlignedFrame = new Frame(
                    definingFrameAlignedWithICRF,
                    new OriginTransformProvider(this, definingFrameAlignedWithICRF),
                    icrfName,
                    true);
        }
        if (iauPole == null || iauPole.isGcrfAligned()) {
            // Body "fixed" and inertial frames are GCRF aligned.
            this.inertialFrame = icrfAlignedFrame;
            this.bodyFrame = icrfAlignedFrame;
        } else {
            this.inertialFrame  = new InertiallyOriented(new JPLInertialTransformProvider(definingFrameAlignedWithICRF, this, iauPole),
                    inertialFrameName);
            this.bodyFrame      = new BodyOriented(bodyOrientedFrameName, new JPLRotatingTransformProvider(iauPole));
        }

    }

    /** {@inheritDoc} */
    @Override
    public TimeStampedPVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) {

        // apply the scale factor to raw position-velocity
        final PVCoordinates rawPV    = rawPVProvider.getRawPV(date);
        final TimeStampedPVCoordinates scaledPV = new TimeStampedPVCoordinates(date, scale, rawPV);

        // the raw PV are relative to the parent of the body centered inertially oriented frame
        final Transform transform = definingFrameAlignedWithIcrf.getTransformTo(frame, date);

        // convert to requested frame
        return transform.transformPVCoordinates(scaledPV);

    }

    /** Get the {@link FieldPVCoordinates} of the body in the selected frame.
     * @param date current date
     * @param frame the frame where to define the position
     * @param <T> type of the field elements
     * @return time-stamped position/velocity of the body (m and m/s)
     */
    @Override
    public <T extends CalculusFieldElement<T>> TimeStampedFieldPVCoordinates<T> getPVCoordinates(final FieldAbsoluteDate<T> date,
                                                                                                 final Frame frame) {

        // apply the scale factor to raw position-velocity
        final FieldPVCoordinates<T>            rawPV    = rawPVProvider.getRawPV(date);
        final TimeStampedFieldPVCoordinates<T> scaledPV = new TimeStampedFieldPVCoordinates<>(date, scale, rawPV);

        // the raw PV are relative to the parent of the body centered inertially oriented frame
        final FieldTransform<T> transform = definingFrameAlignedWithIcrf.getTransformTo(frame, date);

        // convert to requested frame
        return transform.transformPVCoordinates(scaledPV);

    }

    /** {@inheritDoc} */
    @Override
    public Vector3D getVelocity(final AbsoluteDate date, final Frame frame) {

        // apply the scale factor to raw position-velocity
        final PVCoordinates rawPV    = rawPVProvider.getRawPV(date);
        final TimeStampedPVCoordinates scaledPV = new TimeStampedPVCoordinates(date, scale, rawPV);

        // the raw PV are relative to the parent of the body centered inertially oriented frame
        final KinematicTransform transform = definingFrameAlignedWithIcrf.getKinematicTransformTo(frame, date);

        // convert to requested frame
        return transform.transformOnlyPV(scaledPV).getVelocity();

    }

    /** {@inheritDoc} */
    @Override
    public Vector3D getPosition(final AbsoluteDate date, final Frame frame) {

        // apply the scale factor to raw position
        final Vector3D rawPosition    = rawPVProvider.getRawPosition(date);
        final Vector3D scaledPosition = rawPosition.scalarMultiply(scale);

        // the raw position is relative to the parent of the body centered inertially oriented frame
        final StaticTransform transform = definingFrameAlignedWithIcrf.getStaticTransformTo(frame, date);

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
        final FieldStaticTransform<T> transform = definingFrameAlignedWithIcrf.getStaticTransformTo(frame, date);

        // convert to requested frame
        return transform.transformPosition(scaledPosition);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldVector3D<T> getVelocity(final FieldAbsoluteDate<T> date, final Frame frame) {
        // apply the scale factor to raw position-velocity
        final FieldPVCoordinates<T> rawPV    = rawPVProvider.getRawPV(date);
        final TimeStampedFieldPVCoordinates<T> scaledPV = new TimeStampedFieldPVCoordinates<>(date, scale, rawPV);

        // the raw PV are relative to the parent of the body centered inertially oriented frame
        final FieldKinematicTransform<T> transform = definingFrameAlignedWithIcrf.getKinematicTransformTo(frame, date);

        // convert to requested frame
        return transform.transformOnlyPV(scaledPV).getVelocity();
    }

    /** {@inheritDoc} */
    public String getName() {
        return name;
    }

    /** {@inheritDoc} */
    public double getGM() {
        return gm;
    }

    @Override
    public Frame getIcrfAlignedFrame() {
        return icrfAlignedFrame;
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
         * @param transformProvider transform provider to frame in which celestial body coordinates are defined
         * @param frameName name to use (if null a default name will be built)
         */
        InertiallyOriented(final JPLInertialTransformProvider transformProvider, final String frameName) {
            super(transformProvider.getDefiningFrame(), transformProvider, frameName == null ? name + INERTIAL_FRAME_SUFFIX : frameName, true);
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
         * @param transformProvider transform provider to body-fixed frame
         */
        BodyOriented(final String frameName, final JPLRotatingTransformProvider transformProvider) {
            super(inertialFrame, transformProvider, frameName == null ? name + BODY_FRAME_SUFFIX : frameName, false);
        }
    }
}
