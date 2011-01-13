/* Copyright 2002-2011 CS Communication & Systèmes
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

import org.orekit.time.AbsoluteDate;


/** International Terrestrial Reference System realization derived from another
 * (more recent) realization.
 * <p>The various realizations of ITRS are linked together by {@link HelmertTransformation
 * Helmert transformations}.</p>
 * @author Luc Maisonobe
 * @version $Revision$ $Date$
 */
class DerivedITRSRealizationFrame extends FactoryManagedFrame {

    /** Serializable UID. */
    private static final long serialVersionUID = -8190590969971222110L;

    /** Helmert transformation from reference frame. */
    private final HelmertTransformation helmert;

    /** Cached date to avoid useless computation. */
    private AbsoluteDate cachedDate;

    /** Simple constructor, ignoring tidal effects.
     * @param parent parent frame (should be another International Terrestrial Reference Frame)
     * @param helmertTransformation Helmert transformation from parent frame
     * @param factoryKey key of the frame within the factory
     */
    protected DerivedITRSRealizationFrame(final Frame parent,
                                          final HelmertTransformation helmertTransformation,
                                          final Predefined factoryKey) {
        super(parent, null, false, factoryKey);
        this.helmert = helmertTransformation;

        // everything is in place, we can now synchronize the frame
        updateFrame(helmert.getEpoch());

    }

    /** Update the frame to the given date.
     * @param date new value of the date
     */
    protected void updateFrame(final AbsoluteDate date) {
        if ((cachedDate == null) || !cachedDate.equals(date)) {
            setTransform(helmert.getTransform(date));
            cachedDate = date;
        }
    }

}
