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
package org.orekit.files.ccsds.definitions;

import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.orekit.data.DataContext;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.frames.LOFType;
import org.orekit.frames.Transform;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinatesProvider;

/** Facade in front of several frames types in CCSDS messages.
 *
 * @author Luc Maisonobe
 * @author Vincent Cucchietti
 * @since 11.0
 */
public class FrameFacade {

    /** Reference to node in Orekit frames tree. */
    private final Frame frame;

    /** Reference to celestial body centered frame. */
    private final CelestialBodyFrame celestialBodyFrame;

    /** Reference to orbit-relative frame. */
    private final OrbitRelativeFrame orbitRelativeFrame;

    /** Reference to spacecraft body frame. */
    private final SpacecraftBodyFrame spacecraftBodyFrame;

    /** Name of the frame. */
    private final String name;

    /**
     * Simple constructor.
     * <p>
     * At most one of {@code celestialBodyFrame}, {@code orbitRelativeFrame} or {@code spacecraftBodyFrame} may be non
     * null. They may all be null if frame is unknown, in which case only the name will be available.
     * </p>
     *
     * @param frame reference to node in Orekit frames tree (may be null)
     * @param celestialBodyFrame reference to celestial body centered frame (may be null)
     * @param orbitRelativeFrame reference to orbit-relative frame (may be null)
     * @param spacecraftBodyFrame reference to spacecraft body frame (may be null)
     * @param name name of the frame
     */
    public FrameFacade(final Frame frame,
                       final CelestialBodyFrame celestialBodyFrame,
                       final OrbitRelativeFrame orbitRelativeFrame,
                       final SpacecraftBodyFrame spacecraftBodyFrame,
                       final String name) {
        this.frame = frame;
        this.celestialBodyFrame = celestialBodyFrame;
        this.orbitRelativeFrame = orbitRelativeFrame;
        this.spacecraftBodyFrame = spacecraftBodyFrame;
        this.name = name;
    }

    /**
     * Get the associated frame tree node.
     *
     * @return associated frame tree node, or null if none exists
     */
    public Frame asFrame() {
        return frame;
    }

    /**
     * Get the associated {@link CelestialBodyFrame celestial body frame}.
     *
     * @return associated celestial body frame, or null if frame is associated to a
     * {@link #asOrbitRelativeFrame() orbit}, a {@link #asSpacecraftBodyFrame spacecraft} or is not supported
     */
    public CelestialBodyFrame asCelestialBodyFrame() {
        return celestialBodyFrame;
    }

    /**
     * Get the associated {@link OrbitRelativeFrame orbit relative frame}.
     *
     * @return associated orbit relative frame, or null if frame is associated to a
     * {@link #asCelestialBodyFrame() celestial body}, a {@link #asSpacecraftBodyFrame spacecraft} or is not supported
     */
    public OrbitRelativeFrame asOrbitRelativeFrame() {
        return orbitRelativeFrame;
    }

    /**
     * Get the associated {@link SpacecraftBodyFrame spacecraft body frame}.
     *
     * @return associated spacecraft body frame, or null if frame is associated to a
     * {@link #asCelestialBodyFrame() celestial body}, an {@link #asOrbitRelativeFrame orbit} or is not supported
     */
    public SpacecraftBodyFrame asSpacecraftBodyFrame() {
        return spacecraftBodyFrame;
    }

    /**
     * Get the CCSDS name for the frame.
     *
     * @return CCSDS name
     */
    public String getName() {
        return name;
    }

    /**
     * Map an Orekit frame to a CCSDS frame facade.
     *
     * @param frame a reference frame.
     * @return the CCSDS frame corresponding to the Orekit frame
     */
    public static FrameFacade map(final Frame frame) {
        final CelestialBodyFrame cbf = CelestialBodyFrame.map(frame);
        return new FrameFacade(frame, cbf, null, null, cbf.getName());
    }

    /**
     * Simple constructor.
     *
     * @param name name of the frame
     * @param conventions IERS conventions to use
     * @param simpleEOP if true, tidal effects are ignored when interpolating EOP
     * @param dataContext to use when creating the frame
     * @param allowCelestial if true, {@link CelestialBodyFrame} are allowed
     * @param allowOrbit if true, {@link OrbitRelativeFrame} are allowed
     * @param allowSpacecraft if true, {@link SpacecraftBodyFrame} are allowed
     * @return frame facade corresponding to the CCSDS name
     */
    public static FrameFacade parse(final String name,
                                    final IERSConventions conventions,
                                    final boolean simpleEOP,
                                    final DataContext dataContext,
                                    final boolean allowCelestial,
                                    final boolean allowOrbit,
                                    final boolean allowSpacecraft) {
        try {
            final CelestialBodyFrame cbf = CelestialBodyFrame.parse(name);
            if (allowCelestial) {
                return new FrameFacade(cbf.getFrame(conventions, simpleEOP, dataContext),
                                       cbf, null, null, cbf.getName());
            }
        } catch (IllegalArgumentException iaeC) {
            try {
                final OrbitRelativeFrame orf = OrbitRelativeFrame.valueOf(name.replace(' ', '_'));
                if (allowOrbit) {
                    return new FrameFacade(null, null, orf, null, orf.name());
                }
            } catch (IllegalArgumentException iaeO) {
                try {
                    final SpacecraftBodyFrame sbf = SpacecraftBodyFrame.parse(name.replace(' ', '_'));
                    if (allowSpacecraft) {
                        return new FrameFacade(null, null, null, sbf, sbf.toString());
                    }
                } catch (OrekitException | IllegalArgumentException e) {
                    // nothing to do here, use fallback below
                }
            }
        }

        // we don't know any frame with this name, just store the name itself
        return new FrameFacade(null, null, null, null, name);

    }

    /**
     * Get the transform between {@link FrameFacade CCSDS frames}.
     * <p>
     * In case both input and output frames are {@link OrbitRelativeFrame orbit relative frame}, the returned transform
     * will only be composed of a {@link Rotation rotation}. Only {@link LOFType commonly used orbit relative frames}
     * will be recognized.
     * <p>
     * Note that if the input/output {@link FrameFacade CCSDS frame} is defined using a :
     * <ul>
     * <li><b>{@link CelestialBodyFrame celestial body frame}</b></li>
     * <li><b>{@link SpacecraftBodyFrame spacecraft body frame}</b></li>
     * </ul>
     * then <b>an exception will be thrown</b> (currently not supported).
     * <p>
     * Note that the pivot frame provided <b>must be inertial</b> and <b>consistent</b> to what you are working with
     * (i.e GCRF if around Earth for example).
     *
     * @param frameIn the input {@link FrameFacade CCSDS frame} to convert from
     * @param frameOut the output {@link FrameFacade CCSDS frame} to convert to
     * @param inertialPivotFrame <b>inertial</b> frame used as a pivot to create the transform
     * @param date the date for the transform
     * @param pv the position and velocity coordinates provider (required in case one of the frames is an
     * {@link OrbitRelativeFrame orbit relative frame})
     * @return the transform between {@link FrameFacade CCSDS frames}.
     */
    public static Transform getTransform(final FrameFacade frameIn, final FrameFacade frameOut,
                                         final Frame inertialPivotFrame,
                                         final AbsoluteDate date, final PVCoordinatesProvider pv) {

        if (inertialPivotFrame.isPseudoInertial()) {
            final Transform frameInToPivot = getTransformToPivot(frameIn, inertialPivotFrame, date, pv);

            final Transform pivotToFrameOut = getTransformToPivot(frameOut, inertialPivotFrame, date, pv).getInverse();

            return new Transform(date, frameInToPivot, pivotToFrameOut);
        }
        else {
            throw new OrekitException(OrekitMessages.NON_PSEUDO_INERTIAL_FRAME, inertialPivotFrame.getName());
        }

    }

    /**
     * Get the transform between input {@link FrameFacade CCSDS frame} and an <b>inertial</b>
     * {@link Frame Orekit frame}.
     *
     * @param frameIn the input {@link FrameFacade CCSDS frame} to convert from
     * @param inertialPivotFrame <b>inertial</b> {@link Frame Orekit frame} to convert to
     * @param date the date for the transform
     * @param pv the position and velocity coordinates provider (required in case the input
     * {@link FrameFacade CCSDS frame} is an {@link OrbitRelativeFrame orbit relative frame})
     * @return the transform between input {@link FrameFacade CCSDS frame} and an inertial {@link Frame Orekit frame}
     */
    private static Transform getTransformToPivot(final FrameFacade frameIn, final Frame inertialPivotFrame,
                                                 final AbsoluteDate date, final PVCoordinatesProvider pv) {
        final Transform frameInToPivot;

        // Orekit frame
        if (frameIn.asFrame() != null) {
            frameInToPivot = frameIn.asFrame().getTransformTo(inertialPivotFrame, date);
        }

        // Local orbital frame
        else if (frameIn.asOrbitRelativeFrame() != null) {

            final LOFType lofIn = frameIn.asOrbitRelativeFrame().getLofType();

            if (lofIn != null) {
                frameInToPivot =
                        lofIn.transformFromInertial(date, pv.getPVCoordinates(date, inertialPivotFrame)).getInverse();
            }
            else {
                throw new OrekitException(OrekitMessages.UNSUPPORTED_TRANSFORM, frameIn.getName(),
                                          inertialPivotFrame.getName());
            }
        }

        //Celestial body frame
        else if (frameIn.asCelestialBodyFrame() != null) {
            throw new OrekitException(OrekitMessages.UNSUPPORTED_TRANSFORM, frameIn.asCelestialBodyFrame().getName(),
                                      inertialPivotFrame.getName());
        }

        // Spacecraft body frame
        else {
            throw new OrekitException(OrekitMessages.UNSUPPORTED_TRANSFORM, frameIn.getName(),
                                      inertialPivotFrame.getName());
        }

        return frameInToPivot;
    }

}
