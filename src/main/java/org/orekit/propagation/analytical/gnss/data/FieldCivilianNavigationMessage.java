/* Copyright 2002-2024 Luc Maisonobe
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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;

/**
 * Container for data contained in a GPS/QZNSS civilian navigation message.
 * @param <T> type of the field elements
 * @param <O> type of the orbital elements (non-field version)
 * @author Luc Maisonobe
 * @since 13.0
 */
public abstract class FieldCivilianNavigationMessage<T extends CalculusFieldElement<T>,
                                                     O extends CivilianNavigationMessage<O>>
    extends FieldAbstractNavigationMessage<T, O>
    implements FieldGNSSClockElements<T> {

    /** Indicator for CNV 2 messages. */
    private final boolean cnv2;

    /** Change rate in semi-major axis (m/s). */
    private T aDot;

    /** Change rate in Δn₀. */
    private T deltaN0Dot;

    /** The user SV accuracy (m). */
    private T svAccuracy;

    /** Satellite health status. */
    private int svHealth;

    /** Inter Signal Delay for L1 C/A. */
    private T iscL1CA;

    /** Inter Signal Delay for L1 CD. */
    private T iscL1CD;

    /** Inter Signal Delay for L1 CP. */
    private T iscL1CP;

    /** Inter Signal Delay for L2 C. */
    private T iscL2C;

    /** Inter Signal Delay for L5I. */
    private T iscL5I5;

    /** Inter Signal Delay for L5Q. */
    private T iscL5Q5;

    /** Elevation-Dependent User Range Accuracy. */
    private int uraiEd;

    /** Term 0 of Non-Elevation-Dependent User Range Accuracy. */
    private int uraiNed0;

    /** Term 1 of Non-Elevation-Dependent User Range Accuracy. */
    private int uraiNed1;

    /** Term 2 of Non-Elevation-Dependent User Range Accuracy. */
    private int uraiNed2;

    /** Constructor from non-field instance.
     * @param field    field to which elements belong
     * @param original regular non-field instance
     */
    protected FieldCivilianNavigationMessage(final Field<T> field, final O original) {
        super(field, original);
        this.cnv2 = original.isCnv2();
        setADot(field.getZero().newInstance(original.getADot()));
        setDeltaN0Dot(field.getZero().newInstance(original.getDeltaN0Dot()));
        setSvAccuracy(field.getZero().newInstance(original.getSvAccuracy()));
        setSvHealth(original.getSvHealth());
        setIscL1CA(field.getZero().newInstance(original.getIscL1CA()));
        setIscL1CD(field.getZero().newInstance(original.getIscL1CD()));
        setIscL1CP(field.getZero().newInstance(original.getIscL1CP()));
        setIscL2C(field.getZero().newInstance(original.getIscL2C()));
        setIscL5I5(field.getZero().newInstance(original.getIscL5I5()));
        setIscL5Q5(field.getZero().newInstance(original.getIscL5Q5()));
        setUraiEd(original.getUraiEd());
        setUraiNed0(original.getUraiNed0());
        setUraiNed1(original.getUraiNed1());
        setUraiNed2(original.getUraiNed2());
    }

    /** Check it message is a CNV2 message.
     * @return true if message is a CNV2 message
     */
    public boolean isCnv2() {
        return cnv2;
    }

    /**
     * Getter for the change rate in semi-major axis.
     * @return the change rate in semi-major axis
     */
    public T getADot() {
        return aDot;
    }

    /**
     * Setter for the change rate in semi-major axis.
     * @param value the change rate in semi-major axis
     */
    public void setADot(final T value) {
        this.aDot = value;
    }

    /**
     * Getter for change rate in Δn₀.
     * @return change rate in Δn₀
     */
    public T getDeltaN0Dot() {
        return deltaN0Dot;
    }

    /**
     * Setter for change rate in Δn₀.
     * @param deltaN0Dot change rate in Δn₀
     */
    public void setDeltaN0Dot(final T deltaN0Dot) {
        this.deltaN0Dot = deltaN0Dot;
    }

    /**
     * Getter for the user SV accuray (meters).
     * @return the user SV accuracy
     */
    public T getSvAccuracy() {
        return svAccuracy;
    }

    /**
     * Setter for the user SV accuracy.
     * @param svAccuracy the value to set
     */
    public void setSvAccuracy(final T svAccuracy) {
        this.svAccuracy = svAccuracy;
    }

    /**
     * Getter for the satellite health status.
     * @return the satellite health status
     */
    public int getSvHealth() {
        return svHealth;
    }

    /**
     * Setter for the satellite health status.
     * @param svHealth the value to set
     */
    public void setSvHealth(final int svHealth) {
        this.svHealth = svHealth;
    }

    /**
     * Getter for inter Signal Delay for L1 C/A.
     * @return inter signal delay
     */
    public T getIscL1CA() {
        return iscL1CA;
    }

    /**
     * Setter for inter Signal Delay for L1 C/A.
     * @param delay delay to set
     */
    public void setIscL1CA(final T delay) {
        this.iscL1CA = delay;
    }

    /**
     * Getter for inter Signal Delay for L1 CD.
     * @return inter signal delay
     */
    public T getIscL1CD() {
        return iscL1CD;
    }

    /**
     * Setter for inter Signal Delay for L1 CD.
     * @param delay delay to set
     */
    public void setIscL1CD(final T delay) {
        this.iscL1CD = delay;
    }

    /**
     * Getter for inter Signal Delay for L1 CP.
     * @return inter signal delay
     */
    public T getIscL1CP() {
        return iscL1CP;
    }

    /**
     * Setter for inter Signal Delay for L1 CP.
     * @param delay delay to set
     */
    public void setIscL1CP(final T delay) {
        this.iscL1CP = delay;
    }

    /**
     * Getter for inter Signal Delay for L2 C.
     * @return inter signal delay
     */
    public T getIscL2C() {
        return iscL2C;
    }

    /**
     * Setter for inter Signal Delay for L2 C.
     * @param delay delay to set
     */
    public void setIscL2C(final T delay) {
        this.iscL2C = delay;
    }

    /**
     * Getter for inter Signal Delay for L5I.
     * @return inter signal delay
     */
    public T getIscL5I5() {
        return iscL5I5;
    }

    /**
     * Setter for inter Signal Delay for L5I.
     * @param delay delay to set
     */
    public void setIscL5I5(final T delay) {
        this.iscL5I5 = delay;
    }

    /**
     * Getter for inter Signal Delay for L5Q.
     * @return inter signal delay
     */
    public T getIscL5Q5() {
        return iscL5Q5;
    }

    /**
     * Setter for inter Signal Delay for L5Q.
     * @param delay delay to set
     */
    public void setIscL5Q5(final T delay) {
        this.iscL5Q5 = delay;
    }

    /**
     * Getter for Elevation-Dependent User Range Accuracy.
     * @return Elevation-Dependent User Range Accuracy
     */
    public int getUraiEd() {
        return uraiEd;
    }

    /**
     * Setter for Elevation-Dependent User Range Accuracy.
     * @param uraiEd Elevation-Dependent User Range Accuracy
     */
    public void setUraiEd(final int uraiEd) {
        this.uraiEd = uraiEd;
    }

    /**
     * Getter for term 0 of Non-Elevation-Dependent User Range Accuracy.
     * @return term 0 of Non-Elevation-Dependent User Range Accuracy
     */
    public int getUraiNed0() {
        return uraiNed0;
    }

    /**
     * Setter for term 0 of Non-Elevation-Dependent User Range Accuracy.
     * @param uraiNed0 term 0 of Non-Elevation-Dependent User Range Accuracy
     */
    public void setUraiNed0(final int uraiNed0) {
        this.uraiNed0 = uraiNed0;
    }

    /**
     * Getter for term 1 of Non-Elevation-Dependent User Range Accuracy.
     * @return term 1 of Non-Elevation-Dependent User Range Accuracy
     */
    public int getUraiNed1() {
        return uraiNed1;
    }

    /**
     * Setter for term 1 of Non-Elevation-Dependent User Range Accuracy.
     * @param uraiNed1 term 1 of Non-Elevation-Dependent User Range Accuracy
     */
    public void setUraiNed1(final int uraiNed1) {
        this.uraiNed1 = uraiNed1;
    }

    /**
     * Getter for term 2 of Non-Elevation-Dependent User Range Accuracy.
     * @return term 2 of Non-Elevation-Dependent User Range Accuracy
     */
    public int getUraiNed2() {
        return uraiNed2;
    }

    /**
     * Setter for term 2 of Non-Elevation-Dependent User Range Accuracy.
     * @param uraiNed2 term 2 of Non-Elevation-Dependent User Range Accuracy
     */
    public void setUraiNed2(final int uraiNed2) {
        this.uraiNed2 = uraiNed2;
    }

}
