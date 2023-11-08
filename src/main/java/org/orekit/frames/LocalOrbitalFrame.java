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
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.PVCoordinatesProvider;

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

    /** Serializable UID. */
    private static final long serialVersionUID = -4469440345574964950L;

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
     */
    public LocalOrbitalFrame(final Frame parent, final LOF lof,
                             final PVCoordinatesProvider provider,
                             final String name)
        throws IllegalArgumentException {
        super(parent, new LocalProvider(lof, provider, parent), name, false);
    }

    /** Local provider for transforms. */
    private static class LocalProvider implements TransformProvider {

        /** Serializable UID. */
        private static final long serialVersionUID = 20170421L;

        /** Local orbital frame. */
        private final LOF lof;

        /** Provider used to compute frame motion. */
        private final PVCoordinatesProvider provider;

        /** Reference frame. */
        private final Frame reference;

        /** Simple constructor.
         * @param lof local orbital frame
         * @param provider provider used to compute frame motion
         * @param reference reference frame
         */
        LocalProvider(final LOF lof, final PVCoordinatesProvider provider,
                      final Frame reference) {
            this.lof       = lof;
            this.provider  = provider;
            this.reference = reference;
        }

        /** {@inheritDoc} */
        public Transform getTransform(final AbsoluteDate date) {
            return lof.transformFromInertial(date, provider.getPVCoordinates(date, reference));
        }

        /**
         * This method is not implemented.
         *
         * <p> {@inheritDoc}
         *
         * @throws UnsupportedOperationException always.
         */
        public <T extends CalculusFieldElement<T>> FieldTransform<T> getTransform(
                final FieldAbsoluteDate<T> date) throws UnsupportedOperationException {
            throw new UnsupportedOperationException(
                    new OrekitException(OrekitMessages.INTERNAL_ERROR, "https://forum.orekit.org"));
        }

    }

}
