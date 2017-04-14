/* Copyright 2002-2017 CS Systèmes d'Information
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
package org.orekit.frames;

import java.util.HashMap;
import java.util.Map;

import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldPVCoordinatesProvider;
import org.orekit.utils.PVCoordinatesProvider;

/** Class for frames moving with an orbiting satellite.
 * <p>
 * After construction, <em>no</em> provider is set up for field-based PV coordinates,
 * hence {@link #getTransformTo(Frame, FieldAbsoluteDate) getTransformTo()} will
 * <em>not</em> work out of the box. The {@link #addFieldProvider(Field, FieldPVCoordinatesProvider)}
 * method must be called beforehand.
 * </p>
 * <p>There are several local orbital frames available. They are specified
 * by the {@link LOFType} enumerate.</p>
 * @author Luc Maisonobe
 * @deprecated as of 9.0, this class is considered ill-designed. Instead of using this
 * class and its {@link #getTransformTo(Frame, AbsoluteDate) getTransformTo} method,
 * which will cause an orbit propagation to be performed under the hood, a better
 * alternative is to use {@link org.orekit.propagation.SpacecraftState#toTransform()}
 * from within the orbit propagator, at each time step.
 */
@Deprecated
public class LocalOrbitalFrame extends Frame {

    /** Serializable UID. */
    private static final long serialVersionUID = -4469440345574964950L;

    /** Build a new instance.
     * <p>
     * After construction, <em>no</em> provider is set up for field-based PV coordinates,
     * hence {@link #getTransformTo(Frame, FieldAbsoluteDate) getTransformTo()} will
     * <em>not</em> work out of the box. The {@link #addFieldProvider(Field, FieldPVCoordinatesProvider)}
     * method must be called beforehand.
     * </p>
     * @param parent parent frame (must be non-null)
     * @param type frame type
     * @param provider provider used to compute frame motion
     * @param name name of the frame
     * @exception IllegalArgumentException if the parent frame is null
     */
    public LocalOrbitalFrame(final Frame parent, final LOFType type,
                             final PVCoordinatesProvider provider,
                             final String name)
        throws IllegalArgumentException {
        super(parent, new LocalProvider(type, provider, parent, name), name, false);
    }

    /** Add a provider for a field.
     * @param field field to which the elements belong
     * @param fieldProvider provider for field-based PV coordinates
     * @param <T> type of the field elements
     * @since 9.0
     */
    public <T extends RealFieldElement<T>> void addFieldProvider(final Field<T> field,
                                                                 final FieldPVCoordinatesProvider<T> fieldProvider) {
        ((LocalProvider) getTransformProvider()).addFieldProvider(field, fieldProvider);
    }

    /** Local provider for transforms. */
    private static class LocalProvider implements TransformProvider {

        /** Serializable UID. */
        private static final long serialVersionUID = 20170110L;

        /** Frame type. */
        private final LOFType type;

        /** Provider used to compute frame motion. */
        private final PVCoordinatesProvider provider;

        /** Cached field-based transforms. */
        private transient Map<Field<? extends RealFieldElement<?>>, FieldPVCoordinatesProvider<? extends RealFieldElement<?>>> fieldProviders;

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
            this.fieldProviders = new HashMap<>();
            this.reference      = reference;
            this.name           = name;
        }

        /** Add a provider for a field.
         * @param field field to which the elements belong
         * @param fieldProvider provider for field-based PV coordinates
         * @param <T> type of the field elements
         */
        public <T extends RealFieldElement<T>> void addFieldProvider(final Field<T> field,
                                                                     final FieldPVCoordinatesProvider<T> fieldProvider) {
            fieldProviders.put(field, fieldProvider);
        }

        /** {@inheritDoc} */
        public Transform getTransform(final AbsoluteDate date) throws OrekitException {
            return type.transformFromInertial(date, provider.getPVCoordinates(date, reference));
        }

        /** {@inheritDoc} */
        public <T extends RealFieldElement<T>> FieldTransform<T> getTransform(final FieldAbsoluteDate<T> date)
            throws OrekitException {

            @SuppressWarnings("unchecked")
            final FieldPVCoordinatesProvider<T> fp = (FieldPVCoordinatesProvider<T>) fieldProviders.get(date.getField());
            if (fp == null) {
                throw new OrekitException(OrekitMessages.LOF_FRAME_NO_PROVIDER_FOR_FIELD,
                                          date.getField().getClass().toString(), name);
            }

            return type.transformFromInertial(date, fp.getPVCoordinates(date, reference));

        }

        /** Ensure fieldProviders is properly set.
         * @return fixed instance
         */
        private Object readResolve() {
            // this is really an ugly hack, intended only to prevent findbugs complaining
            // anyway, the class is deprecated and already ill-designed
            if (fieldProviders == null) {
                fieldProviders = new HashMap<>();
            }
            return this;
        }

    }

}
