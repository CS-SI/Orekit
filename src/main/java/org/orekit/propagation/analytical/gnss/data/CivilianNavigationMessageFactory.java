/* Copyright 2022-2025 Thales Alenia Space
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
package org.orekit.propagation.analytical.gnss.data;

import org.orekit.frames.Frame;
import org.orekit.gnss.SatelliteSystem;
import org.orekit.time.TimeScales;

/**
 * Factory for {@link CivilianNavigationMessage}.
 * @param <O> type of the orbital elements
 * @author Luc Maisonobe
 * @since 14.0
 */
public abstract class CivilianNavigationMessageFactory<O extends CivilianNavigationMessage<O>>
    extends AbstractNavigationMessageFactory<O> {

    /** Indicator for CNV 2 messages. */
    private final boolean cnv2;

    /** The user SV accuracy (m). */
    private double svAccuracy;

    /** Satellite health status. */
    private int svHealth;

    /** Inter Signal Delay for L1 C/A. */
    private double iscL1CA;

    /** Inter Signal Delay for L1 CD. */
    private double iscL1CD;

    /** Inter Signal Delay for L1 CP. */
    private double iscL1CP;

    /** Inter Signal Delay for L2 C. */
    private double iscL2C;

    /** Inter Signal Delay for L5I. */
    private double iscL5I5;

    /** Inter Signal Delay for L5Q. */
    private double iscL5Q5;

    /** Elevation-Dependent User Range Accuracy. */
    private int uraiEd;

    /** Term 0 of Non-Elevation-Dependent User Range Accuracy. */
    private int uraiNed0;

    /** Term 1 of Non-Elevation-Dependent User Range Accuracy. */
    private int uraiNed1;

    /** Term 2 of Non-Elevation-Dependent User Range Accuracy. */
    private int uraiNed2;

    /** Flags. */
    private int flags;

    /** Simple constructor.
     * @param angularVelocity mean angular velocity of the Earth for the GNSS model
     * @param timeScales      known time scales
     * @param system          satellite system to use for interpreting week number
     * @param type            message type (null if not a navigation message)
     * @param inertial        reference inertial frame
     * @param bodyFixed       body fixed frame (will be frozen at {@code date} to build the orbital elements
     * @param mu              central attraction coefficient (m³/s²)
     * @param cnv2            indicator for CNV 2 messages
     */
    public CivilianNavigationMessageFactory(final double angularVelocity,
                                            final TimeScales timeScales, final SatelliteSystem system,
                                            final String type, final Frame inertial, final Frame bodyFixed,
                                            final double mu, final boolean cnv2) {
        super(angularVelocity, timeScales, system, type, inertial, bodyFixed, mu);
        this.cnv2 = cnv2;
    }

    /** Check it message is a CNV2 message.
     * @return true if message is a CNV2 message
     */
    public boolean isCnv2() {
        return cnv2;
    }

    /** Get the user SV accuray (meters).
     * @return the user SV accuracy
     */
    public double getSvAccuracy() {
        return svAccuracy;
    }

    /** Set the user SV accuray (meters).
     * @param svAccuracy the user SV accuracy
     */
    public void setSvAccuracy(final double svAccuracy) {
        this.svAccuracy = svAccuracy;
    }

    /** Get the satellite health status.
     * @return the satellite health status
     */
    public int getSvHealth() {
        return svHealth;
    }

    /** Set the satellite health status.
     * @param svHealth the satellite health status
     */
    public void setSvHealth(final int svHealth) {
        this.svHealth = svHealth;
    }

    /** Get inter Signal Delay for L1 C/A.
     * @return inter signal delay
     */
    public double getIscL1CA() {
        return iscL1CA;
    }

    /** Set inter Signal Delay for L1 C/A.
     * @param iscL1CA inter signal delay
     */
    public void setIscL1CA(final double iscL1CA) {
        this.iscL1CA = iscL1CA;
    }

    /** Get inter Signal Delay for L1 CD.
     * @return inter signal delay
     */
    public double getIscL1CD() {
        return iscL1CD;
    }

    /** Set inter Signal Delay for L1 CD.
     * @param iscL1CD inter signal delay
     */
    public void setIscL1CD(final double iscL1CD) {
        this.iscL1CD = iscL1CD;
    }

    /** Get inter Signal Delay for L1 CP.
     * @return inter signal delay
     */
    public double getIscL1CP() {
        return iscL1CP;
    }

    /** Set inter Signal Delay for L1 CP.
     * @param iscL1CP inter signal delay
     */
    public void setIscL1CP(final double iscL1CP) {
        this.iscL1CP = iscL1CP;
    }

    /** Get inter Signal Delay for L2 C.
     * @return inter signal delay
     */
    public double getIscL2C() {
        return iscL2C;
    }

    /** Set inter Signal Delay for L2 C.
     * @param iscL2C inter signal delay
     */
    public void setIscL2C(final double iscL2C) {
        this.iscL2C = iscL2C;
    }

    /** Get inter Signal Delay for L5I.
     * @return inter signal delay
     */
    public double getIscL5I5() {
        return iscL5I5;
    }

    /** Set inter Signal Delay for L5I.
     * @param iscL5I5 inter signal delay
     */
    public void setIscL5I5(final double iscL5I5) {
        this.iscL5I5 = iscL5I5;
    }

    /** Get inter Signal Delay for L5Q.
     * @return inter signal delay
     */
    public double getIscL5Q5() {
        return iscL5Q5;
    }

    /** Set inter Signal Delay for L5Q.
     * @param iscL5Q5 inter signal delay
     */
    public void setIscL5Q5(final double iscL5Q5) {
        this.iscL5Q5 = iscL5Q5;
    }

    /** Get Elevation-Dependent User Range Accuracy.
     * @return Elevation-Dependent User Range Accuracy
     */
    public int getUraiEd() {
        return uraiEd;
    }

    /** Set Elevation-Dependent User Range Accuracy.
     * @param uraiEd Elevation-Dependent User Range Accuracy
     */
    public void setUraiEd(final int uraiEd) {
        this.uraiEd = uraiEd;
    }

    /** Get term 0 of Non-Elevation-Dependent User Range Accuracy.
     * @return term 0 of Non-Elevation-Dependent User Range Accuracy
     */
    public int getUraiNed0() {
        return uraiNed0;
    }

    /** Set term 0 of Non-Elevation-Dependent User Range Accuracy.
     * @param uraiNed0 term 0 of Non-Elevation-Dependent User Range Accuracy
     */
    public void setUraiNed0(final int uraiNed0) {
        this.uraiNed0 = uraiNed0;
    }

    /** Get term 1 of Non-Elevation-Dependent User Range Accuracy.
     * @return term 1 of Non-Elevation-Dependent User Range Accuracy
     */
    public int getUraiNed1() {
        return uraiNed1;
    }

    /** Set term 1 of Non-Elevation-Dependent User Range Accuracy.
     * @param uraiNed1 term 1 of Non-Elevation-Dependent User Range Accuracy
     */
    public void setUraiNed1(final int uraiNed1) {
        this.uraiNed1 = uraiNed1;
    }

    /** Get term 2 of Non-Elevation-Dependent User Range Accuracy.
     * @return term 2 of Non-Elevation-Dependent User Range Accuracy
     */
    public int getUraiNed2() {
        return uraiNed2;
    }

    /** Set term 2 of Non-Elevation-Dependent User Range Accuracy.
     * @param uraiNed2 term 2 of Non-Elevation-Dependent User Range Accuracy
     */
    public void setUraiNed2(final int uraiNed2) {
        this.uraiNed2 = uraiNed2;
    }

    /** Get the flags.
     * @return flags
     */
    public int getFlags() {
        return flags;
    }

    /** Set the flags.
     * @param flags flags
     */
    public void setFlags(final int flags) {
        this.flags = flags;
    }

}
