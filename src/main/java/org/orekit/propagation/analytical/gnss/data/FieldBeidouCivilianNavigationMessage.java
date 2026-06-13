/* Copyright 2022-2026 Luc Maisonobe
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
 * Container for data contained in a Beidou civilian navigation message.
 * @param <T> type of the field elements
 * @author Luc Maisonobe
 * @since 13.0
 */
public class FieldBeidouCivilianNavigationMessage<T extends CalculusFieldElement<T>>
    extends FieldAbstractNavigationMessage<T, BeidouCivilianNavigationMessage, FieldBeidouCivilianNavigationMessage<T>> {

    /** Beidou civilian message type.
     * @since 14.0
     */
    private final BeidouCivilianType beidouType;

    /** Issue of Data, Ephemeris. */
    private final int iode;

    /** Issue of Data, Clock. */
    private final int iodc;

    /** Inter Signal Delay for B1 CD. */
    private final T iscB1CD;

    /** Inter Signal Delay for B1 CP. */
    private final T iscB1CP;

    /** Inter Signal Delay for B2 AD. */
    private final T iscB2AD;

    /** Signal In Space Accuracy Index (along track and across track). */
    private final int sisaiOe;

    /** Signal In Space Accuracy Index (radial and clock). */
    private final int sisaiOcb;

    /** Signal In Space Accuracy Index (clock drift accuracy). */
    private final int sisaiOc1;

    /** Signal In Space Accuracy Index (clock drift rate accuracy). */
    private final int sisaiOc2;

    /** Signal In Space Monitoring Accuracy Index. */
    private final int sismai;

    /** Health. */
    private final int health;

    /** Integrity flags. */
    private final int integrityFlags;

    /** B1/B3 Group Delay Differential (s). */
    private final T tgdB1Cp;

    /** B2 AP Group Delay Differential (s). */
    private final T tgdB2ap;

    /** B2B_i / B3I Group Delay Differential (s). */
    private final T tgdB2bI;

    /** Satellite type. */
    private final BeidouSatelliteType satelliteType;

    /** Creates a new instance.
     * @param beidouType       Beidou civilian message type
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
     * @param transmissionTime transmission time
     * @param iode             issue of data, ephemeris
     * @param iodc             issue of data, clock
     * @param iscB1CD          inter signal delay for B1 CD
     * @param iscB1CP          inter signal delay for B1 CP
     * @param iscB2AD          inter signal delay for B2 AD
     * @param sisaiOe          signal in space accuracy index (along track and across track)
     * @param sisaiOcb         signal in space accuracy index (radial and clock)
     * @param sisaiOc1         signal in space accuracy index (clock drift accuracy)
     * @param sisaiOc2         signal in space accuracy index (clock drift rate accuracy)
     * @param sismai           signal in space monitoring accuracy index
     * @param health           health
     * @param integrityFlags   integrity flags
     * @param tgdB1Cp          B1/B3 Group Delay Differential (s)
     * @param tgdB2ap          B2 AP Group Delay Differential (s)
     * @param tgdB2bI          B2B_i / B3I Group Delay Differential (s)
     * @param satelliteType    satellite type
     * @since 14.0
     */
    public FieldBeidouCivilianNavigationMessage(final BeidouCivilianType beidouType,
                                                final double angularVelocity, final int weeksInCycle,
                                                final TimeScales timeScales, final String type, final int prn,
                                                final GNSSDate gnssDate, final FieldKeplerianOrbit<T> orbit,
                                                final T[] nonKeplerian, final T tgd,
                                                final FieldAbsoluteDate<T> toc, final T transmissionTime,
                                                final int iode, final int iodc,
                                                final T iscB1CD, final T iscB1CP, final T iscB2AD,
                                                final int sisaiOe, final int sisaiOcb,
                                                final int sisaiOc1, final int sisaiOc2,
                                                final int sismai, final int health, final int integrityFlags,
                                                final T tgdB1Cp, final T tgdB2ap, final T tgdB2bI,
                                                final BeidouSatelliteType satelliteType) {
        super(angularVelocity, weeksInCycle, timeScales, type, prn, gnssDate, orbit, nonKeplerian,
              tgd, toc, transmissionTime);
        this.beidouType     = beidouType;
        this.iode           = iode;
        this.iodc           = iodc;
        this.iscB1CD        = iscB1CD;
        this.iscB1CP        = iscB1CP;
        this.iscB2AD        = iscB2AD;
        this.sisaiOe        = sisaiOe;
        this.sisaiOcb       = sisaiOcb;
        this.sisaiOc1       = sisaiOc1;
        this.sisaiOc2       = sisaiOc2;
        this.sismai         = sismai;
        this.health         = health;
        this.integrityFlags = integrityFlags;
        this.tgdB1Cp        = tgdB1Cp;
        this.tgdB2ap        = tgdB2ap;
        this.tgdB2bI        = tgdB2bI;
        this.satelliteType  = satelliteType;
    }

    /** Get the Beidou civilian message type.
     * @return Beidou civilian message type
     * @since 14.0
     */
    public BeidouCivilianType getBeidouType() {
        return beidouType;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isCivilianMessage() {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public BeidouCivilianNavigationMessage toNonField() {
        return new BeidouCivilianNavigationMessage(this);
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public <U extends CalculusFieldElement<U>, V extends FieldGnssOrbitalElements<U, BeidouCivilianNavigationMessage, V>>
        V toField(final FieldKeplerianOrbit<U> orbit, final U[] nonKeplerian, final Function<T, U> converter) {
        return (V) new FieldBeidouCivilianNavigationMessage<>(getBeidouType(),
                                                              getAngularVelocity(), getWeeksInCycle(), getTimeScales(),
                                                              getType(), getPrn(), getGnssDate().getGnssDate(),
                                                              orbit, nonKeplerian,
                                                              converter.apply(getTgd()),
                                                              new FieldAbsoluteDate<>(orbit.getMu().getField(),
                                                                                      getToc().toAbsoluteDate()),
                                                              converter.apply(getTransmissionTime()),
                                                              getIODE(), getIODC(),
                                                              converter.apply(getIscB1CD()),
                                                              converter.apply(getIscB1CP()),
                                                              converter.apply(getIscB2AD()),
                                                              getSisaiOe(), getSisaiOcb(),
                                                              getSisaiOc1(), getSisaiOc2(),
                                                              getSismai(), getHealth(), getIntegrityFlags(),
                                                              converter.apply(getTgdB1Cp()),
                                                              converter.apply(getTgdB2ap()),
                                                              converter.apply(getTgdB2bI()),
                                                              getSatelliteType());
    }

    /**
     * Getter for the Issue Of Data Ephemeris (IODE).
     * @return the Issue Of Data Ephemeris (IODE)
     */
    public int getIODE() {
        return iode;
    }

    /**
     * Getter for the Issue Of Data Clock (IODC).
     * @return the Issue Of Data Clock (IODC)
     */
    public int getIODC() {
        return iodc;
    }

    /**
     * Getter for inter Signal Delay for B1 CD.
     * @return inter signal delay
     */
    public T getIscB1CD() {
        return iscB1CD;
    }

    /**
     * Getter for inter Signal Delay for B2 AD.
     * @return inter signal delay
     */
    public T getIscB2AD() {
        return iscB2AD;
    }

    /**
     * Getter for inter Signal Delay for B1 CP.
     * @return inter signal delay
     */
    public T getIscB1CP() {
        return iscB1CP;
    }

    /**
     * Getter for Signal In Space Accuracy Index (along track and across track).
     * @return Signal In Space Accuracy Index (along track and across track)
     */
    public int getSisaiOe() {
        return sisaiOe;
    }

    /**
     * Getter for Signal In Space Accuracy Index (radial and clock).
     * @return Signal In Space Accuracy Index (radial and clock)
     */
    public int getSisaiOcb() {
        return sisaiOcb;
    }

    /**
     * Getter for Signal In Space Accuracy Index (clock drift accuracy).
     * @return Signal In Space Accuracy Index (clock drift accuracy)
     */
    public int getSisaiOc1() {
        return sisaiOc1;
    }

    /**
     * Getter for Signal In Space Accuracy Index (clock drift rate accuracy).
     * @return Signal In Space Accuracy Index (clock drift rate accuracy)
     */
    public int getSisaiOc2() {
        return sisaiOc2;
    }

    /**
     * Getter for Signal In Space Monitoring Accuracy Index.
     * @return Signal In Space Monitoring Accuracy Index
     */
    public int getSismai() {
        return sismai;
    }

    /**
     * Getter for health.
     * @return health
     */
    public int getHealth() {
        return health;
    }

    /**
     * Getter for B1C integrity flags.
     * @return B1C integrity flags
     */
    public int getIntegrityFlags() {
        return integrityFlags;
    }

    /**
     * Getter for B1/B3 Group Delay Differential (s).
     * @return B1/B3 Group Delay Differential (s)
     */
    public T getTgdB1Cp() {
        return tgdB1Cp;
    }

    /**
     * Getter for B2 AP Group Delay Differential (s).
     * @return B2 AP Group Delay Differential (s)
     */
    public T getTgdB2ap() {
        return tgdB2ap;
    }

    /**
     * Getter for B2B_i / B3I Group Delay Differential (s).
     * @return B2B_i / B3I Group Delay Differential (s)
     */
    public T getTgdB2bI() {
        return tgdB2bI;
    }

    /**
     * Getter for satellite type.
     * @return satellite type
     */
    public BeidouSatelliteType getSatelliteType() {
        return satelliteType;
    }

}
