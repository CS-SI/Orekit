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
import org.orekit.propagation.analytical.gnss.data.GNSSConstants;
import org.orekit.utils.units.Unit;

/** Container for data contained in a ionosphere Klobuchar message.
 * @author Luc Maisonobe
 * @since 12.0
 */
public class IonosphereKlobucharMessage extends IonosphereBaseMessage {

    /** Converters for Klobuchar parameters. */
    public static final Unit[] S_PER_SC_N;
    static {
        final Unit sc = Unit.RADIAN.scale("sc", GNSSConstants.GNSS_PI);
        S_PER_SC_N = new Unit[4];
        S_PER_SC_N[0] = Unit.SECOND;
        S_PER_SC_N[1] = S_PER_SC_N[0].divide("s/sc",  sc);
        S_PER_SC_N[2] = S_PER_SC_N[1].divide("s/sc²", sc);
        S_PER_SC_N[3] = S_PER_SC_N[2].divide("s/sc³", sc);
    }

    /** α (s/radⁿ). */
    private final double[] alpha;

    /** β (s/radⁿ). */
    private final double[] beta;

    /** Region code. */
    private RegionCode regionCode;

    /** Simple constructor.
     * @param system satellite system
     * @param prn satellite number
     * @param navigationMessageType navigation message type
     */
    public IonosphereKlobucharMessage(final SatelliteSystem system, final int prn, final String navigationMessageType) {
        super(system, prn, navigationMessageType);
        alpha = new double[4];
        beta  = new double[4];
    }

    /** Get the α coefficients.
     * <p>
     * Beware Orekit uses SI units here.
     * In order to retrieve the more traditional s/semi-circleⁿ, use
     * {@code IonosphereKlobucharMessage.S_PER_SC_N[i].fromSI(alpha[i])}
     * </p>
     * @return α coefficients (s/radⁿ)
     * @see #S_PER_SC_N
     */
    public double[] getAlpha() {
        return alpha.clone();
    }

    /** Set one α coefficient.
     * <p>
     * Beware Orekit uses SI units here.
     * In order to use the more traditional s/semi-circleⁿ, use
     * {@code setAlphaI(i, IonosphereKlobucharMessage.S_PER_SC_N[i].toSi(alpha[i]))}
     * </p>
     * @param i index of the coefficient
     * @param alphaI α coefficient to set (s/radⁿ)
     * @see #S_PER_SC_N
     */
    public void setAlphaI(final int i, final double alphaI) {
        alpha[i] = alphaI;
    }

    /** Get the β coefficients.
     * <p>
     * Beware Orekit uses SI units here.
     * In order to retrieve the more traditional s/semi-circleⁿ, use
     * {@code IonosphereKlobucharMessage.S_PER_SC_N[i].fromSI(beta[i])}
     * </p>
     * @return β coefficients (s/radⁿ)
     * @see #S_PER_SC_N
     */
    public double[] getBeta() {
        return beta.clone();
    }

    /** Set one β coefficient.
     * <p>
     * Beware Orekit uses SI units here.
     * In order to use the more traditional s/semi-circleⁿ, use
     * {@code setBetaI(i, IonosphereKlobucharMessage.S_PER_SC_N[i].toSi(beta[i]))}
     * </p>
     * @param i index of the coefficient
     * @param betaI β coefficient to set (s/radⁿ)
     * @see #S_PER_SC_N
     */
    public void setBetaI(final int i, final double betaI) {
        beta[i] = betaI;
    }

    /** Get the region code.
     * @return region code
     */
    public RegionCode getRegionCode() {
        return regionCode;
    }

    /** Set the region code.
     * @param regionCode region code
     */
    public void setRegionCode(final RegionCode regionCode) {
        this.regionCode = regionCode;
    }

}
