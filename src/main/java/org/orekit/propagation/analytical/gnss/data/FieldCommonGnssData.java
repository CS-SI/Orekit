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

/** Container for common GNSS data contained in almanac and navigation messages.
 * @param <T> type of the field elements
 * @param <O> type of the orbital elements (non-field version)
 * @author Luc Maisonobe
 * @since 13.0
 */
public abstract class FieldCommonGnssData<T extends CalculusFieldElement<T>,
                                          O extends CommonGnssData<O>>
    extends FieldGnssOrbitalElements<T, O>
    implements FieldGNSSClockElements<T> {

    /** SV zero-th order clock correction (s). */
    private T af0;

    /** SV first order clock correction (s/s). */
    private T af1;

    /** SV second order clock correction (s/s²). */
    private T af2;

    /** Group delay differential TGD for L1-L2 correction. */
    private T tgd;

    /** Time Of Clock. */
    private T toc;

    /** Constructor from non-field instance.
     * @param field    field to which elements belong
     * @param original regular non-field instance
     */
    protected FieldCommonGnssData(final Field<T> field, final O original) {
        super(field, original);
        setAf0(field.getZero().newInstance(original.getAf0()));
        setAf1(field.getZero().newInstance(original.getAf1()));
        setAf2(field.getZero().newInstance(original.getAf2()));
        setTGD(field.getZero().newInstance(original.getTGD()));
        setToc(field.getZero().newInstance(original.getToc()));
    }

    /** Constructor from different field instance.
     * @param <V> type of the old field elements
     * @param original regular non-field instance
     * @param converter for field elements
     */
    protected <V extends CalculusFieldElement<V>> FieldCommonGnssData(final Function<V, T> converter,
                                                                      final FieldCommonGnssData<V, O> original) {
        super(converter, original);
        setAf0(converter.apply(original.getAf0()));
        setAf1(converter.apply(original.getAf1()));
        setAf2(converter.apply(original.getAf2()));
        setTGD(converter.apply(original.getTGD()));
        setToc(converter.apply(original.getToc()));
    }

    /** {@inheritDoc} */
    @Override
    public T getAf0() {
        return af0;
    }

    /**
     * Setter for the SV Clock Bias Correction Coefficient (s).
     * @param af0 the SV Clock Bias Correction Coefficient to set
     */
    public void setAf0(final T af0) {
        this.af0 = af0;
    }

    /** {@inheritDoc} */
    @Override
    public T getAf1() {
        return af1;
    }

    /**
     * Setter for the SV Clock Drift Correction Coefficient (s/s).
     * @param af1 the SV Clock Drift Correction Coefficient to set
     */
    public void setAf1(final T af1) {
        this.af1 = af1;
    }

    /** {@inheritDoc} */
    @Override
    public T getAf2() {
        return af2;
    }

    /**
     * Setter for the Drift Rate Correction Coefficient (s/s²).
     * @param af2 the Drift Rate Correction Coefficient to set
     */
    public void setAf2(final T af2) {
        this.af2 = af2;
    }

    /**
     * Set the estimated group delay differential TGD for L1-L2 correction.
     * @param groupDelayDifferential the estimated group delay differential TGD for L1-L2 correction (s)
     */
    public void setTGD(final T groupDelayDifferential) {
        this.tgd = groupDelayDifferential;
    }

    /** {@inheritDoc} */
    @Override
    public T getTGD() {
        return tgd;
    }

    /**
     * Set the time of clock.
     * @param toc the time of clock (s)
     * @see #getAf0()
     * @see #getAf1()
     * @see #getAf2()
     */
    public void setToc(final T toc) {
        this.toc = toc;
    }

    /** {@inheritDoc} */
    @Override
    public T getToc() {
        return toc;
    }

}
