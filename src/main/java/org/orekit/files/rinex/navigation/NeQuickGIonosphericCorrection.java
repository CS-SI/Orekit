/* Copyright 2022-2026 Luc Maisonobe
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
package org.orekit.files.rinex.navigation;

/** Container for NeQuick G ionospheric corrections.
 * @author Luc Maisonobe
 * @since 14.0
 */
public class NeQuickGIonosphericCorrection extends IonosphericCorrection {

    /** The three ionospheric coefficients broadcast in the Galileo navigation message. */
    private final double[] neQuickAlpha;

    /**
     * Constructor.
     * @param type     ionospheric correction type
     * @param timeMark time mark (A: 00h-01h, B: 01h-02h…, X: 23h-24h)
     * @param neQuickAlpha the α ionospheric parameters to set
     */
    public NeQuickGIonosphericCorrection(final IonosphericCorrectionType type, final char timeMark,
                                         final double[] neQuickAlpha) {
        super(type, timeMark);
        this.neQuickAlpha = neQuickAlpha.clone();
    }

    /**
     * Get the α ionospheric parameters.
     * <p>
     * They are used to initialize the {@link org.orekit.models.earth.ionosphere.nequick.NeQuickModel}.
     * </p>
     * @return the α ionospheric parameters or null if not initialized
     */
    public double[] getNeQuickAlpha() {
        return neQuickAlpha.clone();
    }

}
