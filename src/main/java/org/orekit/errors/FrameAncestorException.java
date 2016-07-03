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
package org.orekit.errors;

import org.hipparchus.exception.Localizable;

/** This class is the base class for exception thrown by
 * the {@link org.orekit.frames.UpdatableFrame#updateTransform(org.orekit.frames.Frame,
 * org.orekit.frames.Frame, org.orekit.frames.Transform, org.orekit.time.AbsoluteDate)
 * UpdatableFrame.updateTransform} method.
 */
public class FrameAncestorException extends OrekitException {

    /** Serializable UID. */
    private static final long serialVersionUID = -8279818119798166504L;

    /** Simple constructor.
     * Build an exception with a translated and formatted message
     * @param specifier format specifier (to be translated)
     * @param parts parts to insert in the format (no translation)
     */
    public FrameAncestorException(final Localizable specifier, final Object ... parts) {
        super(specifier, parts);
    }

}
