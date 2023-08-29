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
import org.orekit.time.AbsoluteDate;

/** Container for data contained in a Earth Orientation Parameter navigation message.
 * @author Luc Maisonobe
 * @since 12.0
 */
public class EarthOrientationParameterMessage extends TypeSvMessage {

    /** Reference epoch. */
    private AbsoluteDate referenceEpoch;

    /** X component of the pole (rad). */
    private double xp;

    /** X component of the pole first derivative (rad/s). */
    private double xpDot;

    /** X component of the pole second derivative (rad/s²). */
    private double xpDotDot;

    /** Y component of the pole (rad). */
    private double yp;

    /** Y component of the pole first derivative (rad/s). */
    private double ypDot;

    /** Y component of the pole second derivative (rad/s²). */
    private double ypDotDot;

    /** ΔUT₁ (s).
     * According to Rinex 4.00 table A31, this may be either UT₁-UTC or UT₁-GPST
     * depending on constellation and applicable Interface Control Document).
     */
    private double dUt1;

    /** ΔUT₁ first derivative (s/s). */
    private double dUt1Dot;

    /** ΔUT₁ second derivative (s/s²). */
    private double dUt1DotDot;

    /** Transmission time. */
    private double transmissionTime;

    /** Simple constructor.
     * @param system satellite system
     * @param prn satellite number
     * @param navigationMessageType navigation message type
     */
    public EarthOrientationParameterMessage(final SatelliteSystem system, final int prn, final String navigationMessageType) {
        super(system, prn, navigationMessageType);
    }

    /** Get the reference epoch.
     * @return the reference epoch
     */
    public AbsoluteDate getReferenceEpoch() {
        return referenceEpoch;
    }

    /** Set the reference epoch.
     * @param referenceEpoch the reference epoch to set
     */
    public void setReferenceEpoch(final AbsoluteDate referenceEpoch) {
        this.referenceEpoch = referenceEpoch;
    }

    /** Get the X component of the pole.
     * @return the X component of the pole (rad)
     */
    public double getXp() {
        return xp;
    }

    /** Set the X component of the pole.
     * @param xp X component of the pole (rad)
     */
    public void setXp(final double xp) {
        this.xp = xp;
    }

    /** Get the X component of the pole first derivative.
     * @return the X component of the pole first derivative (rad/s)
     */
    public double getXpDot() {
        return xpDot;
    }

    /** Set the X component of the pole first derivative.
     * @param xpDot X component of the pole first derivative (rad/s)
     */
    public void setXpDot(final double xpDot) {
        this.xpDot = xpDot;
    }

    /** Get the X component of the pole second derivative.
     * @return the X component of the pole second derivative (rad/s²)
     */
    public double getXpDotDot() {
        return xpDotDot;
    }

    /** Set the X component of the pole second derivative.
     * @param xpDotDot X component of the pole second derivative (rad/s²)
     */
    public void setXpDotDot(final double xpDotDot) {
        this.xpDotDot = xpDotDot;
    }

    /** Get the Y component of the pole.
     * @return the Y component of the pole (rad)
     */
    public double getYp() {
        return yp;
    }

    /** Set the Y component of the pole.
     * @param yp Y component of the pole (rad)
     */
    public void setYp(final double yp) {
        this.yp = yp;
    }

    /** Get the Y component of the pole first derivative.
     * @return the Y component of the pole first derivative (rad/s)
     */
    public double getYpDot() {
        return ypDot;
    }

    /** Set the Y component of the pole first derivative.
     * @param ypDot Y component of the pole first derivative (rad/s)
     */
    public void setYpDot(final double ypDot) {
        this.ypDot = ypDot;
    }

    /** Get the Y component of the pole second derivative.
     * @return the Y component of the pole second derivative (rad/s²)
     */
    public double getYpDotDot() {
        return ypDotDot;
    }

    /** Set the Y component of the pole second derivative.
     * @param ypDotDot Y component of the pole second derivative (rad/s²)
     */
    public void setYpDotDot(final double ypDotDot) {
        this.ypDotDot = ypDotDot;
    }

    /** Get the ΔUT₁.
     * @return the ΔUT₁ (s)
     */
    public double getDut1() {
        return dUt1;
    }

    /** Set the ΔUT₁.
     * @param dUT1 ΔUT₁ (s)
     */
    public void setDut1(final double dUT1) {
        this.dUt1 = dUT1;
    }

    /** Get the ΔUT₁ first derivative.
     * @return the ΔUT₁ first derivative (s/s)
     */
    public double getDut1Dot() {
        return dUt1Dot;
    }

    /** Set the ΔUT₁ first derivative.
     * @param dUT1Dot ΔUT₁ first derivative (s/s)
     */
    public void setDut1Dot(final double dUT1Dot) {
        this.dUt1Dot = dUT1Dot;
    }

    /** Get the ΔUT₁ second derivative.
     * @return the ΔUT₁ second derivative (s/s²)
     */
    public double getDut1DotDot() {
        return dUt1DotDot;
    }

    /** Set the ΔUT₁ second derivative.
     * @param dUT1DotDot ΔUT₁ second derivative (s/s²)
     */
    public void setDut1DotDot(final double dUT1DotDot) {
        this.dUt1DotDot = dUT1DotDot;
    }

    /** Get the message transmission time.
     * @return message transmission time
     */
    public double getTransmissionTime() {
        return transmissionTime;
    }

    /** Set the message transmission time.
     * @param transmissionTime the message transmission time
     */
    public void setTransmissionTime(final double transmissionTime) {
        this.transmissionTime = transmissionTime;
    }

}
