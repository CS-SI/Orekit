/* Copyright 2002-2022 CS GROUP
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

import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.definitions.FrameFacade;
import org.orekit.files.ccsds.definitions.OrbitRelativeFrame;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.PVCoordinatesProvider;

/**
 * Utility class providing tools to convert from/to Orekit frames and CCSDS frames.
 *
 * @author Pascal Parraud
 * @author Vincent Cucchietti
 */
public class FrameTools {

    /** Private constructor for this utility class. */
    private FrameTools() {
        // Do nothing
    }

    /**
     * Computes the transform from one CCSDS {@link FrameFacade frame} to another.
     *
     * @param fIn  the initial frame to convert from
     * @param fOut the final frame to convert to
     * @param date the date for the transform
     * @param pv   the PV coordinates provider (required when one of the frames is a LOF)
     * @return the transform from initial to final frame
     */
    public static Transform getTransform(final FrameFacade fIn, final FrameFacade fOut,
                                         final AbsoluteDate date, final PVCoordinatesProvider pv) {
        // Gets transform according to the types of the input frames
        try {
            if (fIn.asFrame() != null) {
                return getTransform(fIn.asFrame(), fOut, date, pv);
            } else if (fIn.asOrbitRelativeFrame() != null) {
                return getTransform(fIn.asOrbitRelativeFrame(), fOut, date, pv);
            }
            // Transform cannot be get from these 2 frames
            throw new OrekitIllegalArgumentException(OrekitMessages.INVALID_TRANSFORM,fIn.getName(), fOut.getName());

        } catch (OrekitIllegalArgumentException exception) {
            // Transform cannot be get from these 2 frames
            throw new OrekitIllegalArgumentException(OrekitMessages.INVALID_TRANSFORM,fIn.getName(), fOut.getName());
        }
    }

    /**
     * Computes the transform from an Orekit {@link Frame} to a CCSDS {@link FrameFacade frame}.
     *
     * @param fIn  the initial frame to convert from
     * @param fOut the final frame facade to convert to
     * @param date the date for the transform
     * @param pv   the PV coordinates provider (required when one of the frames is a LOF)
     * @return the transform from initial to final frame
     */
    public static Transform getTransform(final Frame fIn, final FrameFacade fOut,
                                         final AbsoluteDate date, final PVCoordinatesProvider pv) {

        // Gets transform according to the types of the input frames
        if (fOut.asFrame() != null) {
            return fIn.getTransformTo(fOut.asFrame(), date);
        } else if (fOut.asOrbitRelativeFrame() != null) {
            final LOFType lofOut = fOut.asOrbitRelativeFrame().getLofType();
            if (lofOut != null) {
                return lofOut.transformFromInertial(date, pv.getPVCoordinates(date, fIn));
            }
        }
        // Transform cannot be gotten from these 2 frames
        throw new OrekitIllegalArgumentException(OrekitMessages.INVALID_TRANSFORM,fIn.getName(), fOut.getName());
    }

    /**
     * Computes the transform from an Orekit {@link OrbitRelativeFrame orbit relative frame} to a CCSDS
     * {@link FrameFacade frame}.
     *
     * @param fIn  the initial frame to convert from
     * @param fOut the final frame facade to convert to
     * @param date the date for the transform
     * @param pv   the PV coordinates provider (required when one of the frames is a LOF)
     * @return the transform from initial to final frame
     */
    public static Transform getTransform(final OrbitRelativeFrame fIn, final FrameFacade fOut,
                                         final AbsoluteDate date, final PVCoordinatesProvider pv) {

        final LOFType lofIn = fIn.getLofType();

        if (lofIn != null) {
            if (fOut.asFrame() != null) {
                return getTransform(lofIn, fOut.asFrame(), date, pv);

            } else if (fOut.asOrbitRelativeFrame() != null) {
                final LOFType lofOut = fOut.asOrbitRelativeFrame().getLofType();

                if (lofOut != null) {
                    // Uses a pivot pv in some arbitrary inertial frame
                    final PVCoordinates pivot = pv.getPVCoordinates(date, FramesFactory.getGCRF());
                    // First transform from input lof to inertial
                    final Transform first = lofIn.transformFromInertial(date, pivot).getInverse();
                    // Second transform from inertial to output lof
                    final Transform second = lofOut.transformFromInertial(date, pivot);
                    // Returns the composed transform
                    return new Transform(date, first, second);
                }
            }
        }
        // Transform cannot be gotten from these 2 frames
        throw new OrekitIllegalArgumentException(OrekitMessages.INVALID_TRANSFORM, "undefined relative orbit frame", fOut.getName());
    }

    /**
     * Computes the transform from an Orekit {@link LOFType local orbit frame} to an Orekit {@link Frame frame}.
     *
     * @param lofIn local orbital frame input
     * @param fOut  the final frame facade to convert to
     * @param date  the date for the transform
     * @param pv    the PV coordinates provider (required when one of the frames is a LOF)
     * @return the transform from initial to final frame
     */
    public static Transform getTransform(final LOFType lofIn, final Frame fOut,
                                         final AbsoluteDate date, final PVCoordinatesProvider pv) {

        return lofIn.transformFromInertial(date, pv.getPVCoordinates(date, fOut)).getInverse();
    }

    /**
     * Converts a covariance matrix from one frame to another.
     *
     * @param covIn   covariance matrix in some initial frame to convert
     * @param inToOut transform from the initial to a final frame
     * @return the covariance matrix in the final frame
     */
    public static RealMatrix convertCovFrame(final RealMatrix covIn,
                                             final Transform inToOut) {
        // Gets the rotation from the transform
        final double[][] rotInToOut = inToOut.getRotation().getMatrix();
        // Builds the matrix to perform covariance transformation
        final RealMatrix matInToOut = MatrixUtils.createRealMatrix(6, 6);
        // Fills in the upper left and lower right blocks with the rotation
        matInToOut.setSubMatrix(rotInToOut, 0, 0);
        matInToOut.setSubMatrix(rotInToOut, 3, 3);
        // Returns the covariance matrix converted to frameOut
        return matInToOut.multiply(covIn.multiplyTransposed(matInToOut));
    }
}
