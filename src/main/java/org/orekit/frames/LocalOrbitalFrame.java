/* Copyright 2002-2020 CS Group
 * Licensed to CS Group (CS) under one or more
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

import org.hipparchus.RealFieldElement;
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
     * @param type frame type
     * @param provider provider used to compute frame motion.
     * @param name name of the frame
     * @exception IllegalArgumentException if the parent frame is null
     */
    public LocalOrbitalFrame(final Frame parent, final LOFType type,
                             final PVCoordinatesProvider provider,
                             final String name)
        throws IllegalArgumentException {
        super(parent, new LocalProvider(type, provider, parent, name), name, false);
    }

    /** Local provider for transforms. */
    private static class LocalProvider implements TransformProvider {

        /** Serializable UID. */
        private static final long serialVersionUID = 20170421L;

        /** Frame type. */
        private final LOFType type;

        /** Provider used to compute frame motion. */
        private final PVCoordinatesProvider provider;

        /** Reference frame. */
        private final Frame reference;

        /** Name of the frame. */
        private final String name;

        /** Simple constructor.
         * @param type frame type
         * @param provider provider used to compute frame motion
         * @param reference reference frame
         * @param name name of the frame
         */
        LocalProvider(final LOFType type, final PVCoordinatesProvider provider,
                      final Frame reference, final String name) {
            this.type           = type;
            this.provider       = provider;
            this.reference      = reference;
            this.name           = name;
        }

        /** {@inheritDoc} */
        public Transform getTransform(final AbsoluteDate date) {
            return type.transformFromInertial(date, provider.getPVCoordinates(date, reference));
        }

        /**
         * This method is not implemented.
         *
         * <p> {@inheritDoc}
         *
         * @throws UnsupportedOperationException always.
         */
        public <T extends RealFieldElement<T>> FieldTransform<T> getTransform(
                final FieldAbsoluteDate<T> date) throws UnsupportedOperationException {
            throw new UnsupportedOperationException(
                    "FieldTransforms are not supported for a LocalOrbitalFrame: " + name +
                            ". Please contact orekit-developers@orekit.org if you " +
                            "would like to add this feature.");
        }

    }

}
