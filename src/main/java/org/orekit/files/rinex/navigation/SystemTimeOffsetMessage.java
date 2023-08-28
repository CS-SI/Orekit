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
import org.orekit.gnss.TimeSystem;
import org.orekit.time.AbsoluteDate;

/**
 * Container for data contained in a System Time Offset navigation message.
 * @author Luc Maisonobe
 * @since 12.0
 */
public class SystemTimeOffsetMessage extends TypeSvMessage {

    /** Reference epoch. */
    private AbsoluteDate referenceEpoch;

    /** Time system defined by this message. */
    private TimeSystem definedTimeSystem;

    /** Time system used as a reference to define a time system. */
    private TimeSystem referenceTimeSystem;

    /** SBAS ID. */
    private SbasId sbasId;

    /** UTC ID. */
    private UtcId utcId;

    /** Constant term of the offset. */
    private double a0;

    /** Linear term of the offset. */
    private double a1;

    /** Quadratic term of the offset. */
    private double a2;

    /** Transmission time. */
    private double transmissionTime;

    /** Simple constructor.
     * @param system satellite system
     * @param prn satellite number
     * @param navigationMessageType navigation message type
     */
    public SystemTimeOffsetMessage(final SatelliteSystem system, final int prn, final String navigationMessageType) {
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

    /** Get the time system defined by this message.
     * @return the time system defined by this message
     */
    public TimeSystem getDefinedTimeSystem() {
        return definedTimeSystem;
    }

    /** Set the time system defined by this message.
     * @param definedTimeSystem the time system defined by this message
     */
    public void setDefinedTimeSystem(final TimeSystem definedTimeSystem) {
        this.definedTimeSystem = definedTimeSystem;
    }

    /** Get the time system used as a reference to define a time system.
     * @return the time system used as a reference to define a time system
     */
    public TimeSystem getReferenceTimeSystem() {
        return referenceTimeSystem;
    }

    /** Set the time system used as a reference to define a time system.
     * @param referenceTimeSystem the time system used as a reference to define a time system
     */
    public void setReferenceTimeSystem(final TimeSystem referenceTimeSystem) {
        this.referenceTimeSystem = referenceTimeSystem;
    }

    /** Get the SBAS Id.
     * @return the SBAS Id
     */
    public SbasId getSbasId() {
        return sbasId;
    }

    /** Set the SBAS Id.
     * @param sbasId the SBAS Id to set
     */
    public void setSbasId(final SbasId sbasId) {
        this.sbasId = sbasId;
    }

    /** Get the UTC Id.
     * @return the URTC Id
     */
    public UtcId getUtcId() {
        return utcId;
    }

    /** Set the UTC Id.
     * @param utcId the URC Id to set
     */
    public void setUtcId(final UtcId utcId) {
        this.utcId = utcId;
    }

    /** Get the constant term of the offset.
     * @return the constant term of the offset
     */
    public double getA0() {
        return a0;
    }

    /** Set the constant term of the offset.
     * @param a0 constant term of the offset
     */
    public void setA0(final double a0) {
        this.a0 = a0;
    }

    /** Get the linear term of the offset.
     * @return the linear term of the offset
     */
    public double getA1() {
        return a1;
    }

    /** set the linear term of the offset.
     * @param a1 the linear term of the offset
     */
    public void setA1(final double a1) {
        this.a1 = a1;
    }

    /** Get the quadratic term of the offset.
     * @return the quadratic term of the offset
     */
    public double getA2() {
        return a2;
    }

    /** Set the quadratic term of the offset.
     * @param a2 quadratic term of the offset
     */
    public void setA2(final double a2) {
        this.a2 = a2;
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
