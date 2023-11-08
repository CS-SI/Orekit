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

import org.orekit.time.TimeScale;

/** Specific version of International Terrestrial Reference Frame.
 * <p>
 * This class represents an ITRF with a specific version, regardless of
 * the version of the underlying {@link EOPEntry Earth Orientation Parameters}.
 * </p>
 * @author Luc Maisonobe
 * @since 9.2
 */
public class VersionedITRF extends Frame {

    /** Serializable UID. */
    private static final long serialVersionUID = 20180403L;

    /** Simple constructor.
     * @param parent parent frame (must be non-null)
     * @param version ITRF version this provider should generate
     * @param rawProvider raw ITRF provider
     * @param name name of the frame
     * @param tt TT time scale.
     * @exception IllegalArgumentException if the parent frame is null
     */
    VersionedITRF(final Frame parent, final ITRFVersion version,
                  final ITRFProvider rawProvider, final String name, final TimeScale tt)
        throws IllegalArgumentException {
        super(parent, new VersionedITRFProvider(version, rawProvider, tt), name);
    }

    /** Get the ITRF version.
     * @return ITRF version
     */
    public ITRFVersion getITRFVersion() {
        return ((VersionedITRFProvider) getTransformProvider()).getITRFVersion();
    }

}
