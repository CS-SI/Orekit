/* Copyright 2002-2010 CS Communication & Systèmes
 * Licensed to CS Communication & Systèmes (CS) under one or more
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

import java.io.ObjectStreamException;

import org.orekit.errors.OrekitException;


/** Base class for the predefined frames that are managed by {@link FramesFactory}.
 * @version $Revision$ $Date$
 * @author Luc Maisonobe
 */
public class FactoryManagedFrame extends Frame {

    /** Serializable UID. */
    private static final long serialVersionUID = 1566019035725009300L;

    /** Key of the frame within the factory. */
    private final Predefined factoryKey;

    /** Simple constructor.
     * @param parent parent frame (must be non-null)
     * @param transform transform from parent frame to instance
     * @param pseudoInertial true if frame is considered pseudo-inertial
     * (i.e. suitable for propagating orbit)
     * @param factoryKey key of the frame within the factory
     */
    protected FactoryManagedFrame(final Frame parent, final Transform transform,
                                  final boolean pseudoInertial, final Predefined factoryKey) {
        super(parent, transform, factoryKey.getName(), pseudoInertial);
        this.factoryKey = factoryKey;
    }

    /** Get the key of the frame within the factory.
     * @return key of the frame within the factory
     */
    public Predefined getFactoryKey() {
        return factoryKey;
    }

    /** Replace deserialized objects by singleton instance.
     * @return singleton instance
     * @exception ObjectStreamException if object cannot be deserialized
     */
    private Object readResolve() throws ObjectStreamException {
        try {
            return FramesFactory.getFrame(factoryKey);
        } catch (OrekitException oe) {
            throw new OrekitDeserializationException(oe.getLocalizedMessage(), oe.getCause());
        }
    }

    /** Extended ObjectStreamException with some more information. */
    private static class OrekitDeserializationException extends ObjectStreamException {

        /** Serializable UID. */
        private static final long serialVersionUID = -4647126795776569854L;

        /** Simple constructor.
         * Build an exception from a cause and with a specified message
         * @param message descriptive message
         * @param cause underlying cause
         */
        public OrekitDeserializationException(final String message, final Throwable cause) {
            super(message);
        }

    }

}
