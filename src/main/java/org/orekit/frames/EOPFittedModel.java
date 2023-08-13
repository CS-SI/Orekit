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
package org.orekit.frames;

import org.orekit.utils.SecularAndHarmonic;

/** Container for fitted model for Earth Orientation Parameters.
 * @see PredictedEOPHistory
 * @see EOPFitter
 * @since 12.0
 * @author Luc Maisonobe
 */
public class EOPFittedModel {

    /** Fitted model for dut1 and LOD. */
    private final SecularAndHarmonic dut1;

    /** Fitted model for pole x component. */
    private final SecularAndHarmonic xP;

    /** Fitted model for pole y component. */
    private final SecularAndHarmonic yP;

    /** Fitted model for nutation x component. */
    private final SecularAndHarmonic dx;

    /** Fitted model for nutation y component. */
    private final SecularAndHarmonic dy;

    /** Simple constructor.
     * @param dut1 fitted model for dut1 and LOD
     * @param xP fitted model for pole x component
     * @param yP fitted model for pole y component
     * @param dx fitted model for nutation x component
     * @param dy fitted model for nutation y component
     */
    public EOPFittedModel(final SecularAndHarmonic dut1,
                     final SecularAndHarmonic xP, final SecularAndHarmonic yP,
                     final SecularAndHarmonic dx, final SecularAndHarmonic dy) {
        this.dut1 = dut1;
        this.xP   = xP;
        this.yP   = yP;
        this.dx   = dx;
        this.dy   = dy;
    }

    /** Get the fitted secular and harmonics model for DUT1/LOD.
     * <p>
     * LOD can be computed from DUT1 as {@code -Constants.JULIAN_DAY * getDUT1().osculatingDerivative(date)}
     * </p>
     * @return fitted secular and harmonics model for DUT1/LOD
     */
    public SecularAndHarmonic getDUT1() {
        return dut1;
    }

    /** Get the fitted secular and harmonics model for pole x component.
     * @return fitted secular and harmonics model for pole x component
     */
    public SecularAndHarmonic getXp() {
        return xP;
    }

    /** Get the fitted secular and harmonics model for pole y component.
     * @return fitted secular and harmonics model for pole y component
     */
    public SecularAndHarmonic getYp() {
        return yP;
    }

    /** Get the fitted secular and harmonics model for nutation x component.
     * @return fitted secular and harmonics model for nutation x component
     */
    public SecularAndHarmonic getDx() {
        return dx;
    }

    /** Get the fitted secular and harmonics model for nutation y component.
     * @return fitted secular and harmonics model for nutation y component
     */
    public SecularAndHarmonic getDy() {
        return dy;
    }

}
