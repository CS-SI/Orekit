/* Copyright 2023 Luc Maisonobe
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

import org.orekit.gnss.SatelliteSystem;

/** Container for data contained in a ionosphere BDGIM message.
 * @author Luc Maisonobe
 * @since 12.0
 */
public class IonosphereBDGIMMessage extends IonosphereBaseMessage {

    /** α (TECu). */
    private final double[] alpha;

    /** Simple constructor.
     * @param system satellite system
     * @param prn satellite number
     * @param navigationMessageType navigation message type
     */
    public IonosphereBDGIMMessage(final SatelliteSystem system, final int prn, final String navigationMessageType) {
        super(system, prn, navigationMessageType);
        alpha = new double[9];
    }

    /** Get the α coefficients.
     * <p>
     * Beware Orekit uses SI units here.
     * In order to retrieve the more traditional TECu, use
     * {@code Unit.TOTAL_ELECTRON_CONTENT_UNIT.fromSI(msg.getAlpha()[i])}
     * </p>
     * @return α coefficients (m⁻²)
     * @see org.orekit.utils.units.Unit#TOTAL_ELECTRON_CONTENT_UNIT
     */
    public double[] getAlpha() {
        return alpha.clone();
    }

    /** Set one α coefficient.
     * <p>
     * Beware Orekit uses SI units here.
     * In order to use the more traditional TECu, use
     * {@code msg.setAlpha(i, Unit.TOTAL_ELECTRON_CONTENT_UNIT.toSI(ai))}
     * </p>
     * @param i index of the coefficient
     * @param alphaI α coefficient to set (m⁻²)
     * @see org.orekit.utils.units.Unit#TOTAL_ELECTRON_CONTENT_UNIT
     */
    public void setAlphaI(final int i, final double alphaI) {
        alpha[i] = alphaI;
    }

}
