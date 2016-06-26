/* Copyright 2002-2016 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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

import java.io.Serializable;

import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.Precision;
import org.orekit.bodies.JPLEphemeridesLoader.EphemerisType;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitInternalError;
import org.orekit.frames.Frame;
import org.orekit.frames.Transform;
import org.orekit.frames.TransformProvider;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

/** Implementation of the {@link CelestialBody} interface using JPL or INPOP ephemerides.
 * @author Luc Maisonobe
 */
class JPLCelestialBody implements CelestialBody {

    /** Serializable UID. */
    private static final long serialVersionUID = 3809787672779740923L;

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
     */
    JPLCelestialBody(final String name, final String supportedNames,
                     final JPLEphemeridesLoader.EphemerisType generateType,
                     final JPLEphemeridesLoader.RawPVProvider rawPVProvider,
                     final double gm, final double scale,
                     final IAUPole iauPole, final Frame definingFrameAlignedWithICRF) {
        this.name           = name;
        this.gm             = gm;
        this.scale          = scale;
        this.supportedNames = supportedNames;
        this.generateType   = generateType;
        this.rawPVProvider  = rawPVProvider;
        this.iauPole        = iauPole;
        this.inertialFrame  = new InertiallyOriented(definingFrameAlignedWithICRF);
        this.bodyFrame      = new BodyOriented();
    }

    /** {@inheritDoc} */
    public TimeStampedPVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame)
        throws OrekitException {

        // apply the scale factor to raw position-velocity
        final PVCoordinates rawPV    = rawPVProvider.getRawPV(date);
        final TimeStampedPVCoordinates scaledPV = new TimeStampedPVCoordinates(date, scale, rawPV);

        // the raw PV are relative to the parent of the body centered inertially oriented frame
        final Transform transform = getInertiallyOrientedFrame().getParent().getTransformTo(frame, date);

        // convert to requested frame
        return transform.transformPVCoordinates(scaledPV);

    }

    /** Replace the instance with a data transfer object for serialization.
     * <p>
     * This intermediate class serializes the files supported names, the ephemeris type
     * and the body name.
     * </p>
     * @return data transfer object that will be serialized
     */
    private Object writeReplace() {
        return new DTOCelestialBody(supportedNames, generateType, name);
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

        /** Serializable UID. */
        private static final long serialVersionUID = -8849993808761896559L;

        /** Suffix for inertial frame name. */
        private static final String INERTIAL_FRAME_SUFFIX = "/inertial";

        /** Simple constructor.
         * @param definingFrame frame in which celestial body coordinates are defined
         */
        InertiallyOriented(final Frame definingFrame) {
            super(definingFrame, new TransformProvider() {

                /** Serializable UID. */
                private static final long serialVersionUID = -8610328386110652400L;

                /** {@inheritDoc} */
                public Transform getTransform(final AbsoluteDate date) throws OrekitException {

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
                    Vector3D qNode = Vector3D.crossProduct(Vector3D.PLUS_K, pole);
                    if (qNode.getNormSq() < Precision.SAFE_MIN) {
                        qNode = Vector3D.PLUS_I;
                    }
                    final Transform rotation =
                            new Transform(date, new Rotation(pole, qNode, Vector3D.PLUS_K, Vector3D.PLUS_I));

                    // update transform from parent to self
                    return new Transform(date, translation, rotation);

                }

            }, name + INERTIAL_FRAME_SUFFIX, true);
        }

        /** Replace the instance with a data transfer object for serialization.
         * <p>
         * This intermediate class serializes the files supported names, the ephemeris type
         * and the body name.
         * </p>
         * @return data transfer object that will be serialized
         */
        private Object writeReplace() {
            return new DTOInertialFrame(supportedNames, generateType, name);
        }

    }

    /** Body oriented body centered frame. */
    private class BodyOriented extends Frame {

        /** Serializable UID. */
        private static final long serialVersionUID = -2286454820784307274L;

        /** Suffix for body frame name. */
        private static final String BODY_FRAME_SUFFIX = "/rotating";

        /** Simple constructor.
         */
        BodyOriented() {
            super(inertialFrame, new TransformProvider() {

                /** Serializable UID. */
                private static final long serialVersionUID = 5973062576520917181L;

                /** {@inheritDoc} */
                public Transform getTransform(final AbsoluteDate date) throws OrekitException {
                    final double dt = 10.0;
                    final double w0 = iauPole.getPrimeMeridianAngle(date);
                    final double w1 = iauPole.getPrimeMeridianAngle(date.shiftedBy(dt));
                    return new Transform(date,
                                         new Rotation(Vector3D.PLUS_K, w0, RotationConvention.FRAME_TRANSFORM),
                                         new Vector3D((w1 - w0) / dt, Vector3D.PLUS_K));
                }

            }, name + BODY_FRAME_SUFFIX, false);
        };

        /** Replace the instance with a data transfer object for serialization.
         * <p>
         * This intermediate class serializes the files supported names, the ephemeris type
         * and the body name.
         * </p>
         * @return data transfer object that will be serialized
         */
        private Object writeReplace() {
            return new DTOBodyFrame(supportedNames, generateType, name);
        }

    }

    /** Internal class used only for serialization. */
    private abstract static class DataTransferObject implements Serializable {

        /** Serializable UID. */
        private static final long serialVersionUID = 674742836536072422L;

        /** Regular expression for supported files names. */
        private final String supportedNames;

        /** Ephemeris type to generate. */
        private final EphemerisType generateType;

        /** Name of the body. */
        private final String name;

        /** Simple constructor.
         * @param supportedNames regular expression for supported files names
         * @param generateType ephemeris type to generate
         * @param name name of the body
         */
        DataTransferObject(final String supportedNames, final EphemerisType generateType, final String name) {
            this.supportedNames = supportedNames;
            this.generateType   = generateType;
            this.name           = name;
        }

        /** Get the body associated with the serialized data.
         * @return body associated with the serialized data
         */
        protected JPLCelestialBody getBody() {

            try {
                // first try to use the factory, in order to avoid building a new instance
                // each time we deserialize and have the object properly cached
                final CelestialBody factoryProvided = CelestialBodyFactory.getBody(name);
                if (factoryProvided instanceof JPLCelestialBody) {
                    final JPLCelestialBody jplBody = (JPLCelestialBody) factoryProvided;
                    if (supportedNames.equals(jplBody.supportedNames) && generateType == jplBody.generateType) {
                        // the factory created exactly the object we needed, just return it
                        return jplBody;
                    }
                }

                // the factory does not return the object we want
                // we create a new one from scratch and don't cache it
                return (JPLCelestialBody) new JPLEphemeridesLoader(supportedNames, generateType).loadCelestialBody(name);

            } catch (OrekitException oe) {
                throw new OrekitInternalError(oe);
            }

        }

    }

    /** Specialization of the data transfer object for complete celestial body serialization. */
    private static class DTOCelestialBody extends DataTransferObject {

        /** Serializable UID. */
        private static final long serialVersionUID = -8287341529741045958L;

        /** Simple constructor.
         * @param supportedNames regular expression for supported files names
         * @param generateType ephemeris type to generate
         * @param name name of the body
         */
        DTOCelestialBody(final String supportedNames, final EphemerisType generateType, final String name) {
            super(supportedNames, generateType, name);
        }

        /** Replace the deserialized data transfer object with a {@link JPLCelestialBody}.
         * @return replacement {@link JPLCelestialBody}
         */
        private Object readResolve() {
            return getBody();
        }

    }

    /** Specialization of the data transfer object for inertially oriented frame serialization. */
    private static class DTOInertialFrame extends DataTransferObject {

        /** Serializable UID. */
        private static final long serialVersionUID = 7915071664444154948L;

        /** Simple constructor.
         * @param supportedNames regular expression for supported files names
         * @param generateType ephemeris type to generate
         * @param name name of the body
         */
        DTOInertialFrame(final String supportedNames, final EphemerisType generateType, final String name) {
            super(supportedNames, generateType, name);
        }

        /** Replace the deserialized data transfer object with a {@link Frame}.
         * @return replacement {@link Frame}
         */
        private Object readResolve() {
            return getBody().inertialFrame;
        }

    }

    /** Specialization of the data transfer object for body oriented frame serialization. */
    private static class DTOBodyFrame extends DataTransferObject {

        /** Serializable UID. */
        private static final long serialVersionUID = -3194195019557081000L;

        /** Simple constructor.
         * @param supportedNames regular expression for supported files names
         * @param generateType ephemeris type to generate
         * @param name name of the body
         */
        DTOBodyFrame(final String supportedNames, final EphemerisType generateType, final String name) {
            super(supportedNames, generateType, name);
        }

        /** Replace the deserialized data transfer object with a {@link Frame}.
         * @return replacement {@link Frame}
         */
        private Object readResolve() {
            return getBody().bodyFrame;
        }

    }

}
