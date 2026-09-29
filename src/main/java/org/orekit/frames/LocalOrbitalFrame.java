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
package org.orekit.frames;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.ExtendedPositionProvider;
import org.orekit.utils.PVCoordinatesProvider;
import org.orekit.utils.TimeStampedPVCoordinates;

/** Class for frames moving with an orbiting satellite.
 *
 * <p>There are several local orbital frames available. They are specified
 * by the {@link LOFType} enumerate.</p>
 *
 * <p> Do not use the {@link #getTransformTo(Frame, FieldAbsoluteDate)} method as it is
 * not implemented.
 *
 * @author Luc Maisonobe
 * @see org.orekit.propagation.SpacecraftState#toTransform()
 */
public class LocalOrbitalFrame extends Frame {

    /** Build a new instance.
     *
     * <p> It is highly recommended that {@code provider} use an analytic formulation and
     * not numerical integration as large integration errors may result from many short
     * propagations.
     *
     * @param parent parent frame (must be non-null)
     * @param lof local orbital frame
     * @param provider provider used to compute frame motion.
     * @param name name of the frame
     * @exception IllegalArgumentException if the parent frame is null
     * @since 14.0
     */
    public LocalOrbitalFrame(final Frame parent, final LOF lof,
                             final ExtendedPositionProvider provider,
                             final String name)
        throws IllegalArgumentException {
        super(parent, new LocalProvider(lof, provider, parent), name, false);
    }

    /**
     * Build a new instance.
     *
     * <p> It is highly recommended that {@code provider} use an analytic formulation and
     * not numerical integration as large integration errors may result from many short
     * propagations.
     *
     * <p>Calling {@link LocalOrbitalFrame#getTransformTo(Frame, FieldAbsoluteDate)} on
     * instances built with this constructor will throw an {@link OrekitException} if
     * {@link FieldAbsoluteDate#hasZeroField()} is false.
     *
     * @param parent parent frame (must be non-null)
     * @param lof local orbital frame
     * @param provider provider used to compute frame motion.
     * @param name name of the frame
     * @exception IllegalArgumentException if the parent frame is null
     */
    public LocalOrbitalFrame(final Frame parent,
                             final LOF lof,
                             final PVCoordinatesProvider provider,
                             final String name)
            throws IllegalArgumentException {
        this(parent, lof, new ExtendedPositionProvider() {

            /** {@inheritDoc} */
            @Override
            public Vector3D getPosition(final AbsoluteDate date, final Frame frame) {
                return provider.getPosition(date, frame);
            }

            /** {@inheritDoc} */
            @Override
            public Vector3D getVelocity(final AbsoluteDate date, final Frame frame) {
                return provider.getVelocity(date, frame);
            }

            /** {@inheritDoc} */
            @Override
            public TimeStampedPVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) {
                return provider.getPVCoordinates(date, frame);
            }

            /** {@inheritDoc} */
            @Override
            public <T extends CalculusFieldElement<T>> FieldVector3D<T> getPosition(final FieldAbsoluteDate<T> date, final Frame frame) {
                throw new OrekitException(OrekitMessages.FUNCTION_NOT_IMPLEMENTED);
            }
        },  name);
    }

    /** Local provider for transforms. */
    private static class LocalProvider implements TransformProvider {

        /** Local orbital frame. */
        private final LOF lof;

        /** Provider used to compute frame motion. */
        private final ExtendedPositionProvider provider;

        /** Reference frame. */
        private final Frame reference;

        /** Simple constructor.
         * @param lof local orbital frame
         * @param provider provider used to compute frame motion
         * @param reference reference frame
         */
        LocalProvider(final LOF lof, final ExtendedPositionProvider provider,
                      final Frame reference) {
            this.lof       = lof;
            this.provider  = provider;
            this.reference = reference;
        }

        /** {@inheritDoc} */
        public Transform getTransform(final AbsoluteDate date) {
            return lof.transformFromInertial(date, provider.getPVCoordinates(date, reference));
        }

        /** {@inheritDoc} */
        public <T extends CalculusFieldElement<T>> FieldTransform<T> getTransform(
                final FieldAbsoluteDate<T> date) {
            return lof.transformFromInertial(date, provider.getPVCoordinates(date, reference));
        }

    }

}
