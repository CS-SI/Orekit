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
package org.orekit.frames;

import java.io.Serializable;

import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitInternalError;


/** Base class for the predefined frames that are managed by {@link FramesFactory}.
 * @author Luc Maisonobe
 */
public class FactoryManagedFrame extends Frame {

    /** Serializable UID. */
    private static final long serialVersionUID = -8176399341069422724L;

    /** Key of the frame within the factory. */
    private final Predefined factoryKey;

    /** Simple constructor.
     * @param parent parent frame (must be non-null)
     * @param transformProvider provider for transform from parent frame to instance
     * @param pseudoInertial true if frame is considered pseudo-inertial
     * (i.e. suitable for propagating orbit)
     * @param factoryKey key of the frame within the factory
     */
    public FactoryManagedFrame(final Frame parent, final TransformProvider transformProvider,
                               final boolean pseudoInertial, final Predefined factoryKey) {
        super(parent, transformProvider, factoryKey.getName(), pseudoInertial);
        this.factoryKey = factoryKey;
    }

    /** Get the key of the frame within the factory.
     * @return key of the frame within the factory
     */
    public Predefined getFactoryKey() {
        return factoryKey;
    }

    /** Replace the instance with a data transfer object for serialization.
     * <p>
     * This intermediate class serializes only the frame key.
     * </p>
     * @return data transfer object that will be serialized
     */
    private Object writeReplace() {
        return new DataTransferObject(factoryKey);
    }

    /** Internal class used only for serialization. */
    private static class DataTransferObject implements Serializable {

        /** Serializable UID. */
        private static final long serialVersionUID = 2970971575793609756L;

        /** Name of the frame within the factory. */
        private final String name;

        /** Simple constructor.
         * @param factoryKey key of the frame within the factory
         */
        private DataTransferObject(final Predefined factoryKey) {
            this.name = factoryKey.name();
        }

        /** Replace the deserialized data transfer object with a {@link FactoryManagedFrame}.
         * @return replacement {@link FactoryManagedFrame}
         */
        private Object readResolve() {
            try {
                // retrieve a managed frame
                return FramesFactory.getFrame(Predefined.valueOf(name));
            } catch (OrekitException oe) {
                throw new OrekitInternalError(oe);
            }
        }

    }

}
