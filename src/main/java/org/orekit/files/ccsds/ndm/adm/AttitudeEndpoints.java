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
package org.orekit.files.ccsds.ndm.adm;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.AttitudeBuilder;
import org.orekit.attitudes.FieldAttitude;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.definitions.FrameFacade;
import org.orekit.files.ccsds.definitions.OrbitRelativeFrame;
import org.orekit.frames.Frame;
import org.orekit.utils.AngularCoordinates;
import org.orekit.utils.FieldAngularCoordinates;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.FieldPVCoordinatesProvider;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.PVCoordinatesProvider;
import org.orekit.utils.TimeStampedAngularCoordinates;
import org.orekit.utils.TimeStampedFieldAngularCoordinates;

/** Endpoints for attitude definition.
 * <p>
 * This class provides a bridge between two different views of attitude definition.
 * In both views, there is an external frame, based on either celestial body or orbit-relative
 * and there is a spacecraft body frame.
 * <ul>
 *   <li>CCSDS ADM view: frames are labeled as A and B but nothing tells which is which
 *   and attitude can be defined in any direction</li>
 *   <li>{@link Attitude Orekit attitude} view: attitude is always from external to
 *   spacecraft body</li>
 * </ul>
 * @author Luc Maisonobe
 * @since 11.0
 */
public class AttitudeEndpoints implements AttitudeBuilder {

    /** Constant for A → B diraction. */
    public static final String A2B = "A2B";

    /** Constant for A ← B direction. */
    public static final String B2A = "B2A";

    /** Frame A. */
    private FrameFacade frameA;

    /** Frame B. */
    private FrameFacade frameB;

    /** Flag for frames direction. */
    private Boolean a2b;

    /** Empty constructor.
     * <p>
     * This constructor is not strictly necessary, but it prevents spurious
     * javadoc warnings with JDK 18 and later.
     * </p>
     * @since 12.0
     */
    public AttitudeEndpoints() {
        // nothing to do
    }

    /** Complain if a field is null.
     * @param field field to check
     * @param key key associated with the field
     */
    private void checkNotNull(final Object field, final Enum<?> key) {
        if (field == null) {
            throw new OrekitException(OrekitMessages.UNINITIALIZED_VALUE_FOR_KEY, key.name());
        }
    }
    /** Check external frame is properly initialized.
     * @param aKey key for frame A
     * @param bKey key for frame B
     */
    public void checkExternalFrame(final Enum<?> aKey, final Enum<?> bKey) {
        checkNotNull(frameA, aKey);
        checkNotNull(frameB, bKey);
        if (frameA.asSpacecraftBodyFrame() != null && frameB.asSpacecraftBodyFrame() != null) {
            // we cannot have two spacecraft body frames
            throw new OrekitException(OrekitMessages.CCSDS_INVALID_FRAME, frameB.getName());
        }
    }

    /** Check is mandatory entries <em>except external frame</em> have been initialized.
     * <p>
     * Either frame A or frame B must be initialized with a {@link
     * org.orekit.files.ccsds.definitions.SpacecraftBodyFrame spacecraft body frame}.
     * </p>
     * <p>
     * This method should throw an exception if some mandatory entry is missing
     * </p>
     * @param version format version
     * @param aKey key for frame A
     * @param bKey key for frame B
     * @param dirKey key for direction
     */
    public void checkMandatoryEntriesExceptExternalFrame(final double version,
                                                         final Enum<?> aKey, final Enum<?> bKey,
                                                         final Enum<?> dirKey) {

        if (frameA == null) {
            if (frameB == null || frameB.asSpacecraftBodyFrame() == null) {
                throw new OrekitException(OrekitMessages.UNINITIALIZED_VALUE_FOR_KEY, aKey.name());
            }
        } else if (frameA.asSpacecraftBodyFrame() == null) {
            if (frameB == null) {
                throw new OrekitException(OrekitMessages.UNINITIALIZED_VALUE_FOR_KEY, bKey.name());
            } else if (frameB.asSpacecraftBodyFrame() == null) {
                // at least one of the frame must be a spacecraft body frame
                throw new OrekitException(OrekitMessages.CCSDS_INVALID_FRAME, frameB.getName());
            }
        }

        if (version < 2.0) {
            // in ADM version 1, direction is mandatory
            checkNotNull(a2b, dirKey);
        } else if (!isA2b()) {
            // in ADM version 2, direction is always A → B
            throw new OrekitException(OrekitMessages.CCSDS_KEYWORD_NOT_ALLOWED_IN_VERSION,
                                      dirKey, version);
        }

    }

    /** Set frame A.
     * @param frameA frame A
     */
    public void setFrameA(final FrameFacade frameA) {
        this.frameA = frameA;
    }

    /** Get frame A.
     * @return frame A
     */
    public FrameFacade getFrameA() {
        return frameA;
    }

    /** Set frame B.
     * @param frameB frame B
     */
    public void setFrameB(final FrameFacade frameB) {
        this.frameB = frameB;
    }

    /** Get frame B.
     * @return frame B
     */
    public FrameFacade getFrameB() {
        return frameB;
    }

    /** Set rotation direction.
     * @param a2b if true, rotation is from {@link #getFrameA() frame A}
     * to {@link #getFrameB() frame B}
     */
    public void setA2b(final boolean a2b) {
        this.a2b = a2b;
    }

    /** Check if rotation direction is from {@link #getFrameA() frame A} to {@link #getFrameB() frame B}.
     * @return true if rotation direction is from {@link #getFrameA() frame A} to {@link #getFrameB() frame B}
     */
    public boolean isA2b() {
        return a2b == null ? true : a2b;
    }

    /** Get the external frame.
     * @return external frame
     */
    public FrameFacade getExternalFrame() {
        return frameA.asSpacecraftBodyFrame() == null ? frameA : frameB;
    }

    /** Get the spacecraft body frame.
     * @return spacecraft body frame
     */
    public FrameFacade getSpacecraftBodyFrame() {
        return frameA.asSpacecraftBodyFrame() == null ? frameB : frameA;
    }

    /** Check if attitude is from external frame to spacecraft body frame.
     * <p>
     * {@link #checkMandatoryEntriesExceptExternalFrame(double, Enum, Enum, Enum)
     * Mandatory entries} must have been initialized properly to non-null
     * values before this method is called, otherwise {@code NullPointerException}
     * will be thrown.
     * </p>
     * @return true if attitude is from external frame to spacecraft body frame
     */
    public boolean isExternal2SpacecraftBody() {
        return isA2b() ^ frameB.asSpacecraftBodyFrame() == null;
    }

    /** Check if a endpoint is compatible with another one.
     * <p>
     * Endpoins are compatible if they refer o the same frame names,
     * in the same order and in the same direction.
     * </p>
     * @param other other endpoints to check against
     * @return true if both endpoints are compatible with each other
     */
    public boolean isCompatibleWith(final AttitudeEndpoints other) {
        return frameA.getName().equals(other.frameA.getName()) &&
               frameB.getName().equals(other.frameB.getName()) &&
               a2b.equals(other.a2b);
    }

    /**  {@inheritDoc} */
    @Override
    public Attitude build(final Frame frame, final PVCoordinatesProvider pvProv,
                          final TimeStampedAngularCoordinates rawAttitude) {

        // attitude converted to Orekit conventions
        final TimeStampedAngularCoordinates att =
                        isExternal2SpacecraftBody() ? rawAttitude : rawAttitude.revert();

        final FrameFacade        external = getExternalFrame();
        final OrbitRelativeFrame orf      = external.asOrbitRelativeFrame();
        if (orf != null) {
            // this is an orbit-relative attitude
            if (orf.getLofType() == null) {
                throw new OrekitException(OrekitMessages.UNSUPPORTED_LOCAL_ORBITAL_FRAME, external.getName());
            }

            // construction of the local orbital frame, using PV from reference frame
            final PVCoordinates pv = pvProv.getPVCoordinates(rawAttitude.getDate(), frame);
            final AngularCoordinates frame2Lof =
                            orf.isQuasiInertial() ?
                            new AngularCoordinates(orf.getLofType().rotationFromInertial(pv), Vector3D.ZERO) :
                            orf.getLofType().transformFromInertial(att.getDate(), pv).getAngular();

            // compose with APM
            return new Attitude(frame, att.addOffset(frame2Lof));

        } else {
            // this is an absolute attitude
            if (external.asFrame() == null) {
                // unknown frame
                throw new OrekitException(OrekitMessages.CCSDS_INVALID_FRAME, external.getName());
            }
            final Attitude attitude = new Attitude(external.asFrame(), att);
            return frame == null ? attitude : attitude.withReferenceFrame(frame);
        }

    }

    /**  {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>>
        FieldAttitude<T> build(final Frame frame, final FieldPVCoordinatesProvider<T> pvProv,
                               final TimeStampedFieldAngularCoordinates<T> rawAttitude) {

        // attitude converted to Orekit conventions
        final TimeStampedFieldAngularCoordinates<T> att =
                        isExternal2SpacecraftBody() ? rawAttitude : rawAttitude.revert();

        final FrameFacade        external = getExternalFrame();
        final OrbitRelativeFrame orf      = external.asOrbitRelativeFrame();
        if (orf != null) {
            // this is an orbit-relative attitude
            if (orf.getLofType() == null) {
                throw new OrekitException(OrekitMessages.UNSUPPORTED_LOCAL_ORBITAL_FRAME, external.getName());
            }

            // construction of the local orbital frame, using PV from reference frame
            final FieldPVCoordinates<T> pv = pvProv.getPVCoordinates(rawAttitude.getDate(), frame);
            final Field<T> field = rawAttitude.getDate().getField();
            final FieldAngularCoordinates<T> referenceToLof =
                            orf.isQuasiInertial() ?
                            new FieldAngularCoordinates<>(orf.getLofType().rotationFromInertial(field, pv),
                                                          FieldVector3D.getZero(field)) :
                            orf.getLofType().transformFromInertial(att.getDate(), pv).getAngular();

            // compose with APM
            return new FieldAttitude<>(frame, att.addOffset(referenceToLof));

        } else {
            // this is an absolute attitude
            if (external.asFrame() == null) {
                // this should never happen as all CelestialBodyFrame have an Orekit mapping
                throw new OrekitException(OrekitMessages.CCSDS_INVALID_FRAME, external.getName());
            }
            final FieldAttitude<T> attitude = new FieldAttitude<>(external.asFrame(), att);
            return frame == null ? attitude : attitude.withReferenceFrame(frame);
        }

    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return frameA.getName() + (isA2b() ? " → " : " ← ") + frameB.getName();
    }

}
