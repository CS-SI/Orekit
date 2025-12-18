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

    /** Constructor from non-field instance.
     * @param orbit    orbit in the correct field
     * @param original regular non-field instance
     */
    public FieldBeidouCivilianNavigationMessage(final FieldKeplerianOrbit<T> orbit, final BeidouCivilianNavigationMessage original) {
        super(orbit, original);
        this.beidouType     = original.getBeidouType();
        this.iode           = original.getIODE();
        this.iodc           = original.getIODC();
        this.iscB1CD        = orbit.getMu().newInstance(original.getIscB1CD());
        this.iscB1CP        = orbit.getMu().newInstance(original.getIscB1CP());
        this.iscB2AD        = orbit.getMu().newInstance(original.getIscB2AD());
        this.sisaiOe        = original.getSisaiOe();
        this.sisaiOcb       = original.getSisaiOcb();
        this.sisaiOc1       = original.getSisaiOc1();
        this.sisaiOc2       = original.getSisaiOc2();
        this.sismai         = original.getSismai();
        this.health         = original.getHealth();
        this.integrityFlags = original.getIntegrityFlags();
        this.tgdB1Cp        = orbit.getMu().newInstance(original.getTgdB1Cp());
        this.tgdB2ap        = orbit.getMu().newInstance(original.getTgdB2ap());
        this.tgdB2bI        = orbit.getMu().newInstance(original.getTgdB2bI());
        this.satelliteType  = original.getSatelliteType();
    }

    /** Constructor from different field instance.
     * @param <V> type of the old field elements
     * @param orbit     orbit in the correct field
     * @param original  regular non-field instance
     * @param converter for field elements
     */
    public <V extends CalculusFieldElement<V>> FieldBeidouCivilianNavigationMessage(final FieldKeplerianOrbit<T> orbit,
                                                                                    final Function<V, T> converter,
                                                                                    final FieldBeidouCivilianNavigationMessage<V> original) {
        super(orbit, converter, original);
        this.beidouType     = original.getBeidouType();
        this.iode           = original.getIODE();
        this.iodc           = original.getIODC();
        this.iscB1CD        = converter.apply(original.getIscB1CD());
        this.iscB1CP        = converter.apply(original.getIscB1CP());
        this.iscB2AD        = converter.apply(original.getIscB2AD());
        this.sisaiOe        = original.getSisaiOe();
        this.sisaiOcb       = original.getSisaiOcb();
        this.sisaiOc1       = original.getSisaiOc1();
        this.sisaiOc2       = original.getSisaiOc2();
        this.sismai         = original.getSismai();
        this.health         = original.getHealth();
        this.integrityFlags = original.getIntegrityFlags();
        this.tgdB1Cp        = converter.apply(original.getTgdB1Cp());
        this.tgdB2ap        = converter.apply(original.getTgdB2ap());
        this.tgdB2bI        = converter.apply(original.getTgdB2bI());
        this.satelliteType  = original.getSatelliteType();
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
        V toField(final FieldKeplerianOrbit<U> orbit, final Function<T, U> converter) {
        return (V) new FieldBeidouCivilianNavigationMessage<>(orbit, converter, this);
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
