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
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.GNSSDate;
import org.orekit.time.TimeScales;

import java.util.function.Function;

/**
 * Container for data contained in a NavIC navigation message.
 * @param <T> type of the field elements
 * @author Luc Maisonobe
 * @since 13.0
 */
public class FieldNavicL1NvNavigationMessage<T extends CalculusFieldElement<T>>
    extends FieldAbstractNavigationMessage<T, NavICL1NvNavigationMessage, FieldNavicL1NvNavigationMessage<T>> {

    /** Reference signal flag. */
    private final int referenceSignalFlag;

    /** User Range Accuracy Index.
     * @since 14.0
     */
    private final int urai;

    /** L1 SPS health.
     * @since 14.0
     */
    private final int l1SpsHealth;

    /** Estimated group delay differential TGD for S-L5 correction. */
    private final T tgdSL5;

    /** Inter Signal Delay for S L1P. */
    private final T iscSL1P;

    /** Inter Signal Delay for L1D L1P. */
    private final T iscL1DL1P;

    /** Inter Signal Delay for L1P S. */
    private final T iscL1PS;

    /** Inter Signal Delay for L1DS. */
    private final T iscL1DS;

    /** Constructor from non-field instance.
     * @param orbit    orbit in the correct field
     * @param original regular non-field instance
     */
    public FieldNavicL1NvNavigationMessage(final FieldKeplerianOrbit<T> orbit, final NavICL1NvNavigationMessage original) {
        super(orbit, original);
        referenceSignalFlag = original.getReferenceSignalFlag();
        urai                = original.getUrai();
        l1SpsHealth         = original.getL1SpsHealth();
        tgdSL5              = orbit.getMu().newInstance(original.getTGDSL5());
        iscSL1P             = orbit.getMu().newInstance(original.getIscSL1P());
        iscL1DL1P           = orbit.getMu().newInstance(original.getIscL1DL1P());
        iscL1PS             = orbit.getMu().newInstance(original.getIscL1PS());
        iscL1DS             = orbit.getMu().newInstance(original.getIscL1DS());
    }

    /** Creates a new instance.
     * @param angularVelocity     mean angular velocity of the Earth for the GNSS model
     * @param weeksInCycle        number of weeks in the GNSS cycle
     * @param timeScales          known time scales
     * @param type                type (null if not a navigation message)
     * @param prn                 PRN number of the satellite
     * @param gnssDate            GNSS date (<em>must</em> be consistent with {@code orbit})
     * @param orbit               Keplerian orbit in Earth-frozen frame
     * @param nonKeplerian        15 non-Keplerian parameters (in the order given by {@link NonKeplerianDriversFactory}
     * @param tgd                 group delay differential TGD for L1-L2 correction
     * @param toc                 time of clock
     * @param epochToc            time of clock epoch
     * @param transmissionTime    transmission time
     * @param referenceSignalFlag reference signal flag
     * @param urai                User Range Accuracy Index
     * @param l1SpsHealth         L1 SPS health
     * @param tgdSL5              estimated group delay differential TGD for S-L5 correction
     * @param iscSL1P             inter signal delay for S L1P
     * @param iscL1DL1P           inter signal delay for L1D L1P
     * @param iscL1PS             inter signal delay for L1P S
     * @param iscL1DS             inter signal delay for L1D S
     * @since 14.0
     */
    public FieldNavicL1NvNavigationMessage(final double angularVelocity, final int weeksInCycle,
                                           final TimeScales timeScales, final String type, final int prn,
                                           final GNSSDate gnssDate, final FieldKeplerianOrbit<T> orbit,
                                           final T[] nonKeplerian, final T tgd, final T toc,
                                           final FieldAbsoluteDate<T> epochToc, final T transmissionTime,
                                           final int referenceSignalFlag,
                                           final int urai, final int l1SpsHealth,
                                           final T tgdSL5,
                                           final T iscSL1P, final T iscL1DL1P,
                                           final T iscL1PS, final T iscL1DS) {
        super(angularVelocity, weeksInCycle, timeScales, type, prn, gnssDate, orbit, nonKeplerian,
              tgd, toc, epochToc, transmissionTime);
        this.referenceSignalFlag = referenceSignalFlag;
        this.urai                = urai;
        this.l1SpsHealth         = l1SpsHealth;
        this.tgdSL5              = tgdSL5;
        this.iscSL1P             = iscSL1P;
        this.iscL1DL1P           = iscL1DL1P;
        this.iscL1PS             = iscL1PS;
        this.iscL1DS             = iscL1DS;
    }

    /** Constructor from different field instance.
     * @param <V> type of the old field elements
     * @param orbit     orbit in the correct field
     * @param original  regular non-field instance
     * @param converter for field elements
     */
    public <V extends CalculusFieldElement<V>> FieldNavicL1NvNavigationMessage(final FieldKeplerianOrbit<T> orbit,
                                                                               final Function<V, T> converter,
                                                                               final FieldNavicL1NvNavigationMessage<V> original) {
        super(orbit, converter, original);
        referenceSignalFlag = original.getReferenceSignalFlag();
        urai                = original.getUrai();
        l1SpsHealth         = original.getL1SpsHealth();
        tgdSL5              = converter.apply(original.getTGDSL5());
        iscSL1P             = converter.apply(original.getIscSL1P());
        iscL1DL1P           = converter.apply(original.getIscL1DL1P());
        iscL1PS             = converter.apply(original.getIscL1PS());
        iscL1DS             = converter.apply(original.getIscL1DS());
    }

    /** {@inheritDoc} */
    @Override
    public NavICL1NvNavigationMessage toNonField() {
        return new NavICL1NvNavigationMessage(this);
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public <U extends CalculusFieldElement<U>, V extends FieldGnssOrbitalElements<U, NavICL1NvNavigationMessage, V>>
        V toField(final FieldKeplerianOrbit<U> orbit, final Function<T, U> converter) {
        return (V) new FieldNavicL1NvNavigationMessage<>(orbit, converter, this);
    }

    /** Get reference signal flag.
     * @return reference signal flag
     */
    public int getReferenceSignalFlag() {
        return referenceSignalFlag;
    }

    /** Get User Range Accuracy Index.
     * @return User Range Accuracy Index
     * @since 14.0
     */
    public int getUrai() {
        return urai;
    }

    /** Get L1 SPS health.
     * @return L1 SPS health
     * @since 14.0
     */
    public int getL1SpsHealth() {
        return l1SpsHealth;
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
     * Getter for inter Signal Delay for L1D L1P.
     * @return inter signal delay
     */
    public T getIscL1DL1P() {
        return iscL1DL1P;
    }

    /**
     * Getter for inter Signal Delay for L1P S.
     * @return inter signal delay
     */
    public T getIscL1PS() {
        return iscL1PS;
    }

    /**
     * Getter for inter Signal Delay for L1D S.
     * @return inter signal delay
     */
    public T getIscL1DS() {
        return iscL1DS;
    }

}
