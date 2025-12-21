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
 * Container for data contained in a GPS/QZNSS civilian navigation message.
 * @param <T> type of the field elements
 * @param <O> type of the orbital elements (non-field version)
 * @param <P> type of the orbital elements (field version)
 * @author Luc Maisonobe
 * @since 13.0
 */
public abstract class FieldCivilianNavigationMessage<T extends CalculusFieldElement<T>,
                                                     O extends CivilianNavigationMessage<O>,
                                                     P extends FieldAbstractNavigationMessage<T, O, P>>
    extends FieldAbstractNavigationMessage<T, O, P>
    implements FieldGNSSClockElements<T> {

    /** Indicator for CNV 2 messages. */
    private final boolean cnv2;

    /** The user SV accuracy (m). */
    private final T svAccuracy;

    /** Satellite health status. */
    private final int svHealth;

    /** Inter Signal Delay for L1 C/A. */
    private final T iscL1CA;

    /** Inter Signal Delay for L1 CD. */
    private final T iscL1CD;

    /** Inter Signal Delay for L1 CP. */
    private final T iscL1CP;

    /** Inter Signal Delay for L2 C. */
    private final T iscL2C;

    /** Inter Signal Delay for L5I. */
    private final T iscL5I5;

    /** Inter Signal Delay for L5Q. */
    private final T iscL5Q5;

    /** Elevation-Dependent User Range Accuracy. */
    private final int uraiEd;

    /** Term 0 of Non-Elevation-Dependent User Range Accuracy. */
    private final int uraiNed0;

    /** Term 1 of Non-Elevation-Dependent User Range Accuracy. */
    private final int uraiNed1;

    /** Term 2 of Non-Elevation-Dependent User Range Accuracy. */
    private final int uraiNed2;

    /** Flags.
     * @since 14.0
     */
    private final int flags;

    /** Constructor from non-field instance.
     * @param orbit    orbit in the correct field
     * @param original regular non-field instance
     */
    protected FieldCivilianNavigationMessage(final FieldKeplerianOrbit<T> orbit, final O original) {
        super(orbit, original);
        cnv2       = original.isCnv2();
        svAccuracy = orbit.getMu().newInstance(original.getSvAccuracy());
        svHealth   = original.getSvHealth();
        iscL1CA    = orbit.getMu().newInstance(original.getIscL1CA());
        iscL1CD    = orbit.getMu().newInstance(original.getIscL1CD());
        iscL1CP    = orbit.getMu().newInstance(original.getIscL1CP());
        iscL2C     = orbit.getMu().newInstance(original.getIscL2C());
        iscL5I5    = orbit.getMu().newInstance(original.getIscL5I5());
        iscL5Q5    = orbit.getMu().newInstance(original.getIscL5Q5());
        uraiEd     = original.getUraiEd();
        uraiNed0   = original.getUraiNed0();
        uraiNed1   = original.getUraiNed1();
        uraiNed2   = original.getUraiNed2();
        flags      = original.getFlags();
    }

    /** Creates a new instance.
     * @param cnv2             indicator for CNV2 messages
     * @param angularVelocity  mean angular velocity of the Earth for the GNSS model
     * @param weeksInCycle     number of weeks in the GNSS cycle
     * @param timeScales       known time scales
     * @param type             type (null if not a navigation message)
     * @param prn              PRN number of the satellite
     * @param gnssDate         GNSS date (<em>must</em> be consistent with {@code orbit})
     * @param orbit            Keplerian orbit in Earth-frozen frame
     * @param nonKeplerian     15 non-Keplerian parameters (in the order given by {@link NonKeplerianDriversFactory}
     * @param tgd              group delay differential TGD for L1-L2 correction
     * @param toc              time of clock
     * @param epochToc         time of clock epoch
     * @param transmissionTime transmission time
     * @param svAccuracy       user SV accuracy (m)
     * @param svHealth         satellite health status
     * @param iscL1CA          inter signal delay for L1 C/A
     * @param iscL1CD          inter signal delay for L1 CD
     * @param iscL1CP          inter signal delay for L1 CP
     * @param iscL2C           inter signal delay for L2 C
     * @param iscL5I5          inter signal delay for L5I
     * @param iscL5Q5          inter signal delay for L5Q
     * @param uraiEd           elevation-dependent user range accuracy
     * @param uraiNed0         term 0 of non-elevation-dependent user range accuracy
     * @param uraiNed1         term 1 of non-elevation-dependent user range accuracy
     * @param uraiNed2         term 2 of non-elevation-dependent user range accuracy
     * @param flags            flags
     * @since 14.0
     */
    public FieldCivilianNavigationMessage(final boolean cnv2,
                                          final double angularVelocity, final int weeksInCycle,
                                          final TimeScales timeScales, final String type, final int prn,
                                          final GNSSDate gnssDate, final FieldKeplerianOrbit<T> orbit,
                                          final T[] nonKeplerian, final T tgd, final T toc,
                                          final FieldAbsoluteDate<T> epochToc, final T transmissionTime,
                                          final T svAccuracy, final int svHealth,
                                          final T iscL1CA, final T iscL1CD, final T iscL1CP,
                                          final T iscL2C, final T iscL5I5, final T iscL5Q5,
                                          final int uraiEd, final int uraiNed0, final int uraiNed1, final int uraiNed2,
                                          final int flags) {
        super(angularVelocity, weeksInCycle, timeScales, type, prn, gnssDate, orbit, nonKeplerian,
              tgd, toc, epochToc, transmissionTime);
        this.cnv2       = cnv2;
        this.svAccuracy = svAccuracy;
        this.svHealth   = svHealth;
        this.iscL1CA    = iscL1CA;
        this.iscL1CD    = iscL1CD;
        this.iscL1CP    = iscL1CP;
        this.iscL2C     = iscL2C;
        this.iscL5I5    = iscL5I5;
        this.iscL5Q5    = iscL5Q5;
        this.uraiEd     = uraiEd;
        this.uraiNed0   = uraiNed0;
        this.uraiNed1   = uraiNed1;
        this.uraiNed2   = uraiNed2;
        this.flags      = flags;
    }

    /** Constructor from different field instance.
     * @param <V> type of the old field elements
     * @param orbit     orbit in the correct field
     * @param original  regular non-field instance
     * @param converter for field elements
     */
    protected <V extends CalculusFieldElement<V>> FieldCivilianNavigationMessage(final FieldKeplerianOrbit<T> orbit,
                                                                                 final Function<V, T> converter,
                                                                                 final FieldCivilianNavigationMessage<V, O, ?> original) {
        super(orbit, converter, original);
        cnv2 = original.isCnv2();
        svAccuracy = converter.apply(original.getSvAccuracy());
        svHealth   = original.getSvHealth();
        iscL1CA    = converter.apply(original.getIscL1CA());
        iscL1CD    = converter.apply(original.getIscL1CD());
        iscL1CP    = converter.apply(original.getIscL1CP());
        iscL2C     = converter.apply(original.getIscL2C());
        iscL5I5    = converter.apply(original.getIscL5I5());
        iscL5Q5    = converter.apply(original.getIscL5Q5());
        uraiEd     = original.getUraiEd();
        uraiNed0   = original.getUraiNed0();
        uraiNed1   = original.getUraiNed1();
        uraiNed2   = original.getUraiNed2();
        flags      = original.getFlags();
     }

    /** {@inheritDoc} */
    @Override
    public boolean isCivilianMessage() {
        return true;
    }

    /** Check it message is a CNV2 message.
     * @return true if message is a CNV2 message
     */
    public boolean isCnv2() {
        return cnv2;
    }

    /**
     * Getter for the user SV accuray (meters).
     * @return the user SV accuracy
     */
    public T getSvAccuracy() {
        return svAccuracy;
    }

    /**
     * Getter for the satellite health status.
     * @return the satellite health status
     */
    public int getSvHealth() {
        return svHealth;
    }

    /**
     * Getter for inter Signal Delay for L1 C/A.
     * @return inter signal delay
     */
    public T getIscL1CA() {
        return iscL1CA;
    }

    /**
     * Getter for inter Signal Delay for L1 CD.
     * @return inter signal delay
     */
    public T getIscL1CD() {
        return iscL1CD;
    }

    /**
     * Getter for inter Signal Delay for L1 CP.
     * @return inter signal delay
     */
    public T getIscL1CP() {
        return iscL1CP;
    }

    /**
     * Getter for inter Signal Delay for L2 C.
     * @return inter signal delay
     */
    public T getIscL2C() {
        return iscL2C;
    }

    /**
     * Getter for inter Signal Delay for L5I.
     * @return inter signal delay
     */
    public T getIscL5I5() {
        return iscL5I5;
    }

    /**
     * Getter for inter Signal Delay for L5Q.
     * @return inter signal delay
     */
    public T getIscL5Q5() {
        return iscL5Q5;
    }

    /**
     * Getter for Elevation-Dependent User Range Accuracy.
     * @return Elevation-Dependent User Range Accuracy
     */
    public int getUraiEd() {
        return uraiEd;
    }

    /**
     * Getter for term 0 of Non-Elevation-Dependent User Range Accuracy.
     * @return term 0 of Non-Elevation-Dependent User Range Accuracy
     */
    public int getUraiNed0() {
        return uraiNed0;
    }

    /**
     * Getter for term 1 of Non-Elevation-Dependent User Range Accuracy.
     * @return term 1 of Non-Elevation-Dependent User Range Accuracy
     */
    public int getUraiNed1() {
        return uraiNed1;
    }

    /**
     * Getter for term 2 of Non-Elevation-Dependent User Range Accuracy.
     * @return term 2 of Non-Elevation-Dependent User Range Accuracy
     */
    public int getUraiNed2() {
        return uraiNed2;
    }

    /** Get the flags.
     * @return flags
     * @since 14.0
     */
    public int getFlags() {
        return flags;
    }

}
