/* Copyright 2022-2025 Luc Maisonobe
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

import java.util.function.Function;

/**
 * Container for data contained in a NavIC navigation message.
 * @param <T> type of the field elements
 * @author Luc Maisonobe
 * @since 13.0
 */
public class FieldNavicL1NVNavigationMessage<T extends CalculusFieldElement<T>>
    extends FieldCivilianNavigationMessage<T, NavICL1NVNavigationMessage> {

    /** Reference signal flag. */
    private int referenceSignalFlag;

    /** Estimated group delay differential TGD for S-L5 correction. */
    private T tgdSL5;

    /** Inter Signal Delay for S L1P. */
    private T iscSL1P;

    /** Inter Signal Delay for L1D L1P. */
    private T iscL1DL1P;

    /** Inter Signal Delay for L1P S. */
    private T iscL1PS;

    /** Inter Signal Delay for L1DS. */
    private T iscL1DS;

    /** Constructor from non-field instance.
     * @param field    field to which elements belong
     * @param original regular non-field instance
     */
    public FieldNavicL1NVNavigationMessage(final Field<T> field, final NavICL1NVNavigationMessage original) {
        super(field, original);
        setReferenceSignalFlag(original.getReferenceSignalFlag());
        setTGDSL5(field.getZero().newInstance(original.getTGDSL5()));
        setIscSL1P(field.getZero().newInstance(original.getIscSL1P()));
        setIscL1DL1P(field.getZero().newInstance(original.getIscL1DL1P()));
        setIscL1PS(field.getZero().newInstance(original.getIscL1PS()));
        setIscL1DS(field.getZero().newInstance(original.getIscL1DS()));
    }

    /** Constructor from different field instance.
     * @param <V> type of the old field elements
     * @param original regular non-field instance
     * @param converter for field elements
     */
    public <V extends CalculusFieldElement<V>> FieldNavicL1NVNavigationMessage(final Function<V, T> converter,
                                                                               final FieldNavicL1NVNavigationMessage<V> original) {
        super(converter, original);
        setReferenceSignalFlag(original.getReferenceSignalFlag());
        setTGDSL5(converter.apply(original.getTGDSL5()));
        setIscSL1P(converter.apply(original.getIscSL1P()));
        setIscL1DL1P(converter.apply(original.getIscL1DL1P()));
        setIscL1PS(converter.apply(original.getIscL1PS()));
        setIscL1DS(converter.apply(original.getIscL1DS()));
    }

    /** {@inheritDoc} */
    @Override
    public NavICL1NVNavigationMessage toNonField() {
        return new NavICL1NVNavigationMessage(this);
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public <U extends CalculusFieldElement<U>, G extends FieldGnssOrbitalElements<U, NavICL1NVNavigationMessage>>
        G changeField(final Function<T, U> converter) {
        return (G) new FieldNavicL1NVNavigationMessage<>(converter, this);
    }

    /** Set reference signal flag.
     * @param referenceSignalFlag reference signal flag
     */
    public void setReferenceSignalFlag(final int referenceSignalFlag) {
        this.referenceSignalFlag = referenceSignalFlag;
    }

    /** Get reference signal flag.
     * @return reference signal flag
     */
    public int getReferenceSignalFlag() {
        return referenceSignalFlag;
    }

    /**
     * Set the estimated group delay differential TGD for S-L5 correction.
     * @param groupDelayDifferential the estimated group delay differential TGD for S-L3 correction (s)
     */
    public void setTGDSL5(final T groupDelayDifferential) {
        this.tgdSL5 = groupDelayDifferential;
    }

    /**
     * Set the estimated group delay differential TGD for S-L5 correction.
     * @return estimated group delay differential TGD for S-L3 correction (s)
     */
    public T getTGDSL5() {
        return tgdSL5;
    }

    /**
     * Getter for inter Signal Delay for S L1P.
     * @return inter signal delay
     */
    public T getIscSL1P() {
        return iscSL1P;
    }

    /**
     * Setter for inter Signal Delay for S L1P.
     * @param delay delay to set
     */
    public void setIscSL1P(final T delay) {
        this.iscSL1P = delay;
    }

    /**
     * Getter for inter Signal Delay for L1D L1P.
     * @return inter signal delay
     */
    public T getIscL1DL1P() {
        return iscL1DL1P;
    }

    /**
     * Setter for inter Signal Delay for L1D L1P.
     * @param delay delay to set
     */
    public void setIscL1DL1P(final T delay) {
        this.iscL1DL1P = delay;
    }

    /**
     * Getter for inter Signal Delay for L1P S.
     * @return inter signal delay
     */
    public T getIscL1PS() {
        return iscL1PS;
    }

    /**
     * Setter for inter Signal Delay for L1P S.
     * @param delay delay to set
     */
    public void setIscL1PS(final T delay) {
        this.iscL1PS = delay;
    }

    /**
     * Getter for inter Signal Delay for L1D S.
     * @return inter signal delay
     */
    public T getIscL1DS() {
        return iscL1DS;
    }

    /**
     * Setter for inter Signal Delay for L1D S.
     * @param delay delay to set
     */
    public void setIscL1DS(final T delay) {
        this.iscL1DS = delay;
    }

}
