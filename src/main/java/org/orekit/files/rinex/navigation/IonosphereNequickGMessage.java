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
import org.orekit.utils.units.Unit;

/** Container for data contained in a ionosphere Nequick G message.
 * @author Luc Maisonobe
 * @since 12.0
 */
public class IonosphereNequickGMessage extends IonosphereBaseMessage {

    /** Converter for Nequick-G aᵢ₀ parameter. */
    public static final Unit SFU = Unit.SOLAR_FLUX_UNIT;

    /** Converter for Nequick-G aᵢ₁ parameter. */
    public static final Unit SFU_PER_DEG = SFU.divide("sfu/deg", Unit.DEGREE);

    /** Converter for Nequick-G aᵢ₂ parameter. */
    public static final Unit SFU_PER_DEG2 = SFU_PER_DEG.divide("sfu/deg²", Unit.DEGREE);

    /** aᵢ₀ (sfu). */
    private double ai0;

    /** aᵢ₁ (sfu/rad). */
    private double ai1;

    /** aᵢ₂ (sfu/rad²). */
    private double ai2;

    /** Disturbance flags. */
    private int flags;

    /** Simple constructor.
     * @param system satellite system
     * @param prn satellite number
     * @param navigationMessageType navigation message type
     */
    public IonosphereNequickGMessage(final SatelliteSystem system, final int prn, final String navigationMessageType) {
        super(system, prn, navigationMessageType);
    }

    /** Get aᵢ₀.
     * <p>
     * Beware Orekit uses SI units here.
     * In order to retrieve the more traditional SFU, use
     * {@code IonosphereNequickGMessage.SFU.fromSI(msg.getAi0())}
     * </p>
     * @return aᵢ₀ (W/m²/Hz)
     * @see #SFU
     */
    public double getAi0() {
        return ai0;
    }

    /** Set aᵢ₀.
     * <p>
     * Beware Orekit uses SI units here.
     * In order to use the more traditional SFU, use
     * {@code msg.setAi0(IonosphereNequickGMessage.SFU.toSI(ai0))}
     * </p>
     * @param ai0 aᵢ₀ (W/m²/Hz)
     * @see #SFU
     */
    public void setAi0(final double ai0) {
        this.ai0 = ai0;
    }

    /** Get aᵢ₁.
     * <p>
     * Beware Orekit uses SI units here.
     * In order to retrieve the more traditional SFU/deg, use
     * {@code IonosphereNequickGMessage.SFU_PAR_DEG.fromSI(msg.getAi1())}
     * </p>
     * @return aᵢ₁ (W/m²/Hz/rad)
     * @see #SFU_PER_DEG
     */
    public double getAi1() {
        return ai1;
    }

    /** Set aᵢ₁.
     * <p>
     * Beware Orekit uses SI units here.
     * In order to use the more traditional SFU/deg, use
     * {@code msg.setAi1(IonosphereNequickGMessage.SFU_PER_DEG.toSI(ai1))}
     * </p>
     * @param ai1 aᵢ₁ (W/m²/Hz/rad)
     * @see #SFU_PER_DEG
     */
    public void setAi1(final double ai1) {
        this.ai1 = ai1;
    }

    /** Get aᵢ₂.
     * <p>
     * Beware Orekit uses SI units here.
     * In order to retrieve the more traditional SFU/deg², use
     * {@code IonosphereNequickGMessage.SFU_PER_DEG_2.fromSI(msg.getAi2())}
     * </p>
     * @return aᵢ₂ (W/m²/Hz/rad²)
     * @see #SFU_PER_DEG2
     */
    public double getAi2() {
        return ai2;
    }

    /** Set aᵢ₂.
     * <p>
     * Beware Orekit uses SI units here.
     * In order to use the more traditional SFU/deg², use
     * {@code msg.setAi2(IonosphereNequickGMessage.SFU_PER_DEG2.toSI(ai2))}
     * </p>
     * @param ai2 aᵢ₂ (W/m²/Hz/rad²)
     * @see #SFU_PER_DEG2
     */
    public void setAi2(final double ai2) {
        this.ai2 = ai2;
    }

    /** Get the disturbance flags.
     * @return disturbance flags
     */
    public int getFlags() {
        return flags;
    }

    /** Set the disturbance flags.
     * @param flags disturbance flags
     */
    public void setFlags(final int flags) {
        this.flags = flags;
    }

}
