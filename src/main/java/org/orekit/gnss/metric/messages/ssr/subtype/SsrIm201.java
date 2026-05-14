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
package org.orekit.gnss.metric.messages.ssr.subtype;

import java.util.List;

import org.orekit.annotation.DefaultDataContext;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.data.DataContext;
import org.orekit.frames.Frame;
import org.orekit.gnss.metric.messages.ssr.SsrMessage;
import org.orekit.models.earth.ionosphere.SsrVtecIonosphericModel;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;

/**
 * SSR Ionosphere VTEC Spherical Harmonics Message.
 * @author Bryan Cazabonne
 * @since 11.0
 */
public class SsrIm201 extends SsrMessage<SsrIm201Header, SsrIm201Data> {

    /**
     * Constructor.
     * @param typeCode message number
     * @param header message header
     * @param data message data
     */
    public SsrIm201(final int typeCode, final SsrIm201Header header,
                    final List<SsrIm201Data> data) {
        super(typeCode, header, data);
    }

    /**
     * Get the ionospheric model adapted to the current IM201 message.
     * <p>
     * This method uses the {@link DefaultDataContext default data context} for
     * ITRF frame.
     * </p>
     * @see #getIonosphericModel(Frame)
     * @return the ionospheric model
     */
    @DefaultDataContext
    public SsrVtecIonosphericModel getIonosphericModel() {
        return getIonosphericModel(DataContext.getDefault().getFrames().getITRF(IERSConventions.IERS_2010, true));
    }

    /**
     * Get the ionospheric model adapted to the current IM201 message.
     * @return the ionospheric model
     * @param itrf ITRF frame to use for Earth shape
     */
    @DefaultDataContext
    public SsrVtecIonosphericModel getIonosphericModel(final Frame itrf) {
        final OneAxisEllipsoid earthBodyShape  = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                Constants.WGS84_EARTH_FLATTENING, itrf);
        return new SsrVtecIonosphericModel(earthBodyShape, this);
    }
}
