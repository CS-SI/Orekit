/* Copyright 2002-2012 CS Systèmes d'Information
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

import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinatesProvider;

/** Class for frames moving with an orbiting satellite.
 * <p>There are several local orbital frames available. They are specified
 * by the {@link LOFType} enumerate.</p>
 * @author Luc Maisonobe
 */
public class LocalOrbitalFrame extends Frame {

    /** Serializable UID. */
    private static final long serialVersionUID = 3442484777549385439L;

    /** Frame type. */
    private final LOFType type;

    /** Provider used to compute frame motion. */
    private final PVCoordinatesProvider provider;

    /** Build a new instance.
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
        super(parent, Transform.IDENTITY, name, false);
        this.type     = type;
        this.provider = provider;
    }

    /** {@inheritDoc} */
    protected void updateFrame(final AbsoluteDate date) throws OrekitException {

        // update the frame defining transform
        setTransform(type.transformFromInertial(provider.getPVCoordinates(date, getParent())));

    }

}
