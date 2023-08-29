/* Copyright 2002-2023 CS GROUP
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.orekit.files.rinex.RinexFile;
import org.orekit.gnss.SatelliteSystem;
import org.orekit.models.earth.ionosphere.KlobucharIonoModel;
import org.orekit.models.earth.ionosphere.NeQuickModel;
import org.orekit.propagation.analytical.gnss.data.BeidouCivilianNavigationMessage;
import org.orekit.propagation.analytical.gnss.data.BeidouLegacyNavigationMessage;
import org.orekit.propagation.analytical.gnss.data.GLONASSNavigationMessage;
import org.orekit.propagation.analytical.gnss.data.GPSCivilianNavigationMessage;
import org.orekit.propagation.analytical.gnss.data.GPSLegacyNavigationMessage;
import org.orekit.propagation.analytical.gnss.data.GalileoNavigationMessage;
import org.orekit.propagation.analytical.gnss.data.IRNSSNavigationMessage;
import org.orekit.propagation.analytical.gnss.data.QZSSCivilianNavigationMessage;
import org.orekit.propagation.analytical.gnss.data.QZSSLegacyNavigationMessage;
import org.orekit.propagation.analytical.gnss.data.SBASNavigationMessage;

/**
 * Represents a parsed RINEX navigation messages files.
 * @author Bryan Cazabonne
 * @author Luc Maisonobe
 * @since 11.0
 */
public class RinexNavigation extends RinexFile<RinexNavigationHeader> {

    /** The 4 Klobuchar coefficients of a cubic equation representing the amplitude of the vertical delay. */
    private double[] klobucharAlpha;

    /** The 4 coefficients of a cubic equation representing the period of the model. */
    private double[] klobucharBeta;

    /** The three ionospheric coefficients broadcast in the Galileo navigation message. */
    private double[] neQuickAlpha;

    /** A map containing the GPS navigation messages. */
    private final Map<String, List<GPSLegacyNavigationMessage>> gpsLegacyData;

    /** A map containing the GPS navigation messages. */
    private final Map<String, List<GPSCivilianNavigationMessage>> gpsCivilianData;

    /** A map containing the Galileo navigation messages. */
    private final Map<String, List<GalileoNavigationMessage>> galileoData;

    /** A map containing the Beidou navigation messages. */
    private final Map<String, List<BeidouLegacyNavigationMessage>> beidouLegacyData;

    /** A map containing the Beidou navigation messages. */
    private final Map<String, List<BeidouCivilianNavigationMessage>> beidouCivilianData;

    /** A map containing the QZSS navigation messages. */
    private final Map<String, List<QZSSLegacyNavigationMessage>> qzssLegacyData;

    /** A map containing the QZSS navigation messages. */
    private final Map<String, List<QZSSCivilianNavigationMessage>> qzssCivilianData;

    /** A map containing the IRNSS navigation messages. */
    private final Map<String, List<IRNSSNavigationMessage>> irnssData;

    /** A map containing the GLONASS navigation messages. */
    private final Map<String, List<GLONASSNavigationMessage>> glonassData;

    /** A map containing the SBAS navigation messages. */
    private final Map<String, List<SBASNavigationMessage>> sbasData;

    /** System time offsets.
     * @since 12.0
     */
    private final List<SystemTimeOffsetMessage> systemTimeOffsets;

    /** Earth orientation parameters.
     * @since 12.0
     */
    private final List<EarthOrientationParameterMessage> eops;

    /** Ionosphere Klobuchar messages.
     * @since 12.0
     */
    private final List<IonosphereKlobucharMessage> klobucharMessages;

    /** Ionosphere Nequick G messages.
     * @since 12.0
     */
    private final List<IonosphereNequickGMessage> nequickGMessages;

    /** Ionosphere BDGIM messages.
     * @since 12.0
     */
    private final List<IonosphereBDGIMMessage> bdgimMessages;

    /** Constructor. */
    public RinexNavigation() {
        super(new RinexNavigationHeader());
        this.gpsLegacyData      = new HashMap<>();
        this.gpsCivilianData    = new HashMap<>();
        this.galileoData        = new HashMap<>();
        this.beidouLegacyData   = new HashMap<>();
        this.beidouCivilianData = new HashMap<>();
        this.qzssLegacyData     = new HashMap<>();
        this.qzssCivilianData   = new HashMap<>();
        this.irnssData          = new HashMap<>();
        this.glonassData        = new HashMap<>();
        this.sbasData           = new HashMap<>();
        this.systemTimeOffsets  = new ArrayList<>();
        this.eops               = new ArrayList<>();
        this.klobucharMessages  = new ArrayList<>();
        this.nequickGMessages   = new ArrayList<>();
        this.bdgimMessages      = new ArrayList<>();
    }

    /**
     * Get the "alpha" ionospheric parameters.
     * <p>
     * They are used to initialize the {@link KlobucharIonoModel}.
     * </p>
     * @return the "alpha" ionospheric parameters
     */
    public double[] getKlobucharAlpha() {
        return klobucharAlpha.clone();
    }

    /**
     * Set the "alpha" ionspheric parameters.
     * @param klobucharAlpha the "alpha" ionspheric parameters to set
     */
    public void setKlobucharAlpha(final double[] klobucharAlpha) {
        this.klobucharAlpha = klobucharAlpha.clone();
    }

    /**
     * Get the "beta" ionospheric parameters.
     * <p>
     * They are used to initialize the {@link KlobucharIonoModel}.
     * </p>
     * @return the "beta" ionospheric parameters
     */
    public double[] getKlobucharBeta() {
        return klobucharBeta.clone();
    }

    /**
     * Set the "beta" ionospheric parameters.
     * @param klobucharBeta the "beta" ionospheric parameters to set
     */
    public void setKlobucharBeta(final double[] klobucharBeta) {
        this.klobucharBeta = klobucharBeta.clone();
    }

    /**
     * Get the "alpha" ionospheric parameters.
     * <p>
     * They are used to initialize the {@link NeQuickModel}.
     * </p>
     * @return the "alpha" ionospheric parameters
     */
    public double[] getNeQuickAlpha() {
        return neQuickAlpha.clone();
    }

    /**
     * Set the "alpha" ionospheric parameters.
     * @param neQuickAlpha the "alpha" ionospheric parameters to set
     */
    public void setNeQuickAlpha(final double[] neQuickAlpha) {
        this.neQuickAlpha = neQuickAlpha.clone();
    }

    /**
     * Get all the GPS legacy navigation messages contained in the file.
     * @return an unmodifiable list of GPS legacy navigation messages
     * @since 12.0
     */
    public Map<String, List<GPSLegacyNavigationMessage>> getGPSLegacyNavigationMessages() {
        return Collections.unmodifiableMap(gpsLegacyData);
    }

    /**
     * Get the GPS legacy navigation messages for the given satellite Id.
     * @param satId satellite Id (i.e. Satellite System (e.g. G) + satellite number)
     * @return an unmodifiable list of GPS legacy navigation messages
     * @since 12.0
     */
    public List<GPSLegacyNavigationMessage> getGPSLegacyNavigationMessages(final String satId) {
        return Collections.unmodifiableList(gpsLegacyData.get(satId));
    }

    /**
     * Add a GPS legacy navigation message to the list.
     * @param message message to add
     * @since 12.0
     */
    public void addGPSLegacyNavigationMessage(final GPSLegacyNavigationMessage message) {
        final int    gpsPRN = message.getPRN();
        final String prnString = gpsPRN < 10 ? "0" + String.valueOf(gpsPRN) : String.valueOf(gpsPRN);
        final String satId = SatelliteSystem.GPS.getKey() + prnString;
        gpsLegacyData.putIfAbsent(satId, new ArrayList<>());
        gpsLegacyData.get(satId).add(message);
    }

    /**
     * Get all the GPS civilian navigation messages contained in the file.
     * @return an unmodifiable list of GPS civilian navigation messages
     * @since 12.0
     */
    public Map<String, List<GPSCivilianNavigationMessage>> getGPSCivilianNavigationMessages() {
        return Collections.unmodifiableMap(gpsCivilianData);
    }

    /**
     * Get the GPS civilian navigation messages for the given satellite Id.
     * @param satId satellite Id (i.e. Satellite System (e.g. G) + satellite number)
     * @return an unmodifiable list of GPS civilian navigation messages
     * @since 12.0
     */
    public List<GPSCivilianNavigationMessage> getGPSCivilianNavigationMessages(final String satId) {
        return Collections.unmodifiableList(gpsCivilianData.get(satId));
    }

    /**
     * Add a GPS civilian navigation message to the list.
     * @param message message to add
     * @since 12.0
     */
    public void addGPSLegacyNavigationMessage(final GPSCivilianNavigationMessage message) {
        final int    gpsPRN = message.getPRN();
        final String prnString = gpsPRN < 10 ? "0" + String.valueOf(gpsPRN) : String.valueOf(gpsPRN);
        final String satId = SatelliteSystem.GPS.getKey() + prnString;
        gpsCivilianData.putIfAbsent(satId, new ArrayList<>());
        gpsCivilianData.get(satId).add(message);
    }

    /**
     * Get all the Galileo navigation messages contained in the file.
     * @return an unmodifiable list of Galileo navigation messages
     */
    public Map<String, List<GalileoNavigationMessage>> getGalileoNavigationMessages() {
        return Collections.unmodifiableMap(galileoData);
    }

    /**
     * Get the Galileo navigation messages for the given satellite Id.
     * @param satId satellite Id (i.e. Satellite System (e.g. E) + satellite number)
     * @return an unmodifiable list of Galileo navigation messages
     */
    public List<GalileoNavigationMessage> getGalileoNavigationMessages(final String satId) {
        return Collections.unmodifiableList(galileoData.get(satId));
    }

    /**
     * Add a Galileo navigation message to the list.
     * @param message message to add
     */
    public void addGalileoNavigationMessage(final GalileoNavigationMessage message) {
        final int    galPRN = message.getPRN();
        final String prnString = galPRN < 10 ? "0" + String.valueOf(galPRN) : String.valueOf(galPRN);
        final String satId = SatelliteSystem.GALILEO.getKey() + prnString;
        galileoData.putIfAbsent(satId, new ArrayList<>());
        galileoData.get(satId).add(message);
    }

    /**
     * Get all the Beidou navigation messages contained in the file.
     * @return an unmodifiable list of Beidou navigation messages
     * @since 12.0
     */
    public Map<String, List<BeidouLegacyNavigationMessage>> getBeidouLegacyNavigationMessages() {
        return Collections.unmodifiableMap(beidouLegacyData);
    }

    /**
     * Get the Beidou navigation messages for the given satellite Id.
     * @param satId satellite Id (i.e. Satellite System (e.g. C) + satellite number)
     * @return an unmodifiable list of Beidou navigation messages
     * @since 12.0
     */
    public List<BeidouLegacyNavigationMessage> getBeidouLegacyNavigationMessages(final String satId) {
        return Collections.unmodifiableList(beidouLegacyData.get(satId));
    }

    /**
     * Add a Beidou navigation message to the list.
     * @param message message to add
     * @since 12.0
     */
    public void addBeidouLegacyNavigationMessage(final BeidouLegacyNavigationMessage message) {
        final int    bdtPRN = message.getPRN();
        final String prnString = bdtPRN < 10 ? "0" + String.valueOf(bdtPRN) : String.valueOf(bdtPRN);
        final String satId = SatelliteSystem.BEIDOU.getKey() + prnString;
        beidouLegacyData.putIfAbsent(satId, new ArrayList<>());
        beidouLegacyData.get(satId).add(message);
    }

    /**
     * Get all the Beidou navigation messages contained in the file.
     * @return an unmodifiable list of Beidou navigation messages
     * @since 12.0
     */
    public Map<String, List<BeidouCivilianNavigationMessage>> getBeidouCivilianNavigationMessages() {
        return Collections.unmodifiableMap(beidouCivilianData);
    }

    /**
     * Get the Beidou navigation messages for the given satellite Id.
     * @param satId satellite Id (i.e. Satellite System (e.g. C) + satellite number)
     * @return an unmodifiable list of Beidou navigation messages
     * @since 12.0
     */
    public List<BeidouCivilianNavigationMessage> getBeidouCivilianNavigationMessages(final String satId) {
        return Collections.unmodifiableList(beidouCivilianData.get(satId));
    }

    /**
     * Add a Beidou navigation message to the list.
     * @param message message to add
     * @since 12.0
     */
    public void addBeidouCivilianNavigationMessage(final BeidouCivilianNavigationMessage message) {
        final int    bdtPRN = message.getPRN();
        final String prnString = bdtPRN < 10 ? "0" + String.valueOf(bdtPRN) : String.valueOf(bdtPRN);
        final String satId = SatelliteSystem.BEIDOU.getKey() + prnString;
        beidouCivilianData.putIfAbsent(satId, new ArrayList<>());
        beidouCivilianData.get(satId).add(message);
    }

    /**
     * Get all the QZSS navigation messages contained in the file.
     * @return an unmodifiable list of QZSS navigation messages
     * @since 12.0
     */
    public Map<String, List<QZSSLegacyNavigationMessage>> getQZSSLegacyNavigationMessages() {
        return Collections.unmodifiableMap(qzssLegacyData);
    }

    /**
     * Get the QZSS navigation messages for the given satellite Id.
     * @param satId satellite Id (i.e. Satellite System (e.g. J) + satellite number)
     * @return an unmodifiable list of QZSS navigation messages
     * @since 12.0
     */
    public List<QZSSLegacyNavigationMessage> getQZSSLegacyNavigationMessages(final String satId) {
        return Collections.unmodifiableList(qzssLegacyData.get(satId));
    }

    /**
     * Add a QZSS navigation message to the list.
     * @param message message to add
     * @since 12.0
     */
    public void addQZSSLegacyNavigationMessage(final QZSSLegacyNavigationMessage message) {
        final int    qzsPRN = message.getPRN();
        final String prnString = qzsPRN < 10 ? "0" + String.valueOf(qzsPRN) : String.valueOf(qzsPRN);
        final String satId = SatelliteSystem.QZSS.getKey() + prnString;
        qzssLegacyData.putIfAbsent(satId, new ArrayList<>());
        qzssLegacyData.get(satId).add(message);
    }

    /**
     * Get all the QZSS navigation messages contained in the file.
     * @return an unmodifiable list of QZSS navigation messages
     * @since 12.0
     */
    public Map<String, List<QZSSCivilianNavigationMessage>> getQZSSCivilianNavigationMessages() {
        return Collections.unmodifiableMap(qzssCivilianData);
    }

    /**
     * Get the QZSS navigation messages for the given satellite Id.
     * @param satId satellite Id (i.e. Satellite System (e.g. J) + satellite number)
     * @return an unmodifiable list of QZSS navigation messages
     * @since 12.0
     */
    public List<QZSSCivilianNavigationMessage> getQZSSCivilianNavigationMessages(final String satId) {
        return Collections.unmodifiableList(qzssCivilianData.get(satId));
    }

    /**
     * Add a QZSS navigation message to the list.
     * @param message message to add
     * @since 12.0
     */
    public void addQZSSCivilianNavigationMessage(final QZSSCivilianNavigationMessage message) {
        final int    qzsPRN = message.getPRN();
        final String prnString = qzsPRN < 10 ? "0" + String.valueOf(qzsPRN) : String.valueOf(qzsPRN);
        final String satId = SatelliteSystem.QZSS.getKey() + prnString;
        qzssCivilianData.putIfAbsent(satId, new ArrayList<>());
        qzssCivilianData.get(satId).add(message);
    }

    /**
     * Get all the IRNSS navigation messages contained in the file.
     * @return an unmodifiable list of IRNSS navigation messages
     */
    public Map<String, List<IRNSSNavigationMessage>> getIRNSSNavigationMessages() {
        return Collections.unmodifiableMap(irnssData);
    }

    /**
     * Get the IRNSS navigation messages for the given satellite Id.
     * @param satId satellite Id (i.e. Satellite System (e.g. I) + satellite number)
     * @return an unmodifiable list of IRNSS navigation messages
     */
    public List<IRNSSNavigationMessage> getIRNSSNavigationMessages(final String satId) {
        return Collections.unmodifiableList(irnssData.get(satId));
    }

    /**
     * Add a IRNSS navigation message to the list.
     * @param message message to add
     */
    public void addIRNSSNavigationMessage(final IRNSSNavigationMessage message) {
        final int    irsPRN = message.getPRN();
        final String prnString = irsPRN < 10 ? "0" + String.valueOf(irsPRN) : String.valueOf(irsPRN);
        final String satId = SatelliteSystem.IRNSS.getKey() + prnString;
        irnssData.putIfAbsent(satId, new ArrayList<>());
        irnssData.get(satId).add(message);
    }

    /**
     * Get all the Glonass navigation messages contained in the file.
     * @return an unmodifiable list of Glonass navigation messages
     */
    public Map<String, List<GLONASSNavigationMessage>> getGlonassNavigationMessages() {
        return Collections.unmodifiableMap(glonassData);
    }

    /**
     * Get the Glonass navigation messages for the given satellite Id.
     * @param satId satellite Id (i.e. Satellite System (e.g. R) + satellite number)
     * @return an unmodifiable list of Glonass navigation messages
     */
    public List<GLONASSNavigationMessage> getGlonassNavigationMessages(final String satId) {
        return Collections.unmodifiableList(glonassData.get(satId));
    }

    /**
     * Add a Glonass navigation message to the list.
     * @param message message to add
     */
    public void addGlonassNavigationMessage(final GLONASSNavigationMessage message) {
        final int    gloPRN = message.getPRN();
        final String prnString = gloPRN < 10 ? "0" + String.valueOf(gloPRN) : String.valueOf(gloPRN);
        final String satId = SatelliteSystem.GLONASS.getKey() + prnString;
        glonassData.putIfAbsent(satId, new ArrayList<>());
        glonassData.get(satId).add(message);
    }

    /**
     * Get all the SBAS navigation messages contained in the file.
     * @return an unmodifiable list of SBAS navigation messages
     */
    public Map<String, List<SBASNavigationMessage>> getSBASNavigationMessages() {
        return Collections.unmodifiableMap(sbasData);
    }

    /**
     * Get the SBAS navigation messages for the given satellite Id.
     * @param satId satellite Id (i.e. Satellite System (e.g. S) + satellite number)
     * @return an unmodifiable list of SBAS navigation messages
     */
    public List<SBASNavigationMessage> getSBASNavigationMessages(final String satId) {
        return Collections.unmodifiableList(sbasData.get(satId));
    }

    /**
     * Add a SBAS navigation message to the list.
     * @param message message to add
     */
    public void addSBASNavigationMessage(final SBASNavigationMessage message) {
        final int    sbsPRN = message.getPRN();
        final String prnString = sbsPRN < 10 ? "0" + String.valueOf(sbsPRN) : String.valueOf(sbsPRN);
        final String satId = SatelliteSystem.SBAS.getKey() + prnString;
        sbasData.putIfAbsent(satId, new ArrayList<>());
        sbasData.get(satId).add(message);
    }

    /**
     * Get the system time offsets.
     * @return an unmodifiable list of system time offsets
     * @since 12.0
     */
    public List<SystemTimeOffsetMessage> getSystemTimeOffsets() {
        return Collections.unmodifiableList(systemTimeOffsets);
    }

    /**
     * Add a system time offset.
     * @param systemTimeOffset system time offset message
     * @since 12.0
     */
    public void addSystemTimeOffset(final SystemTimeOffsetMessage systemTimeOffset) {
        systemTimeOffsets.add(systemTimeOffset);
    }

    /**
     * Get the Earth orientation parameters.
     * @return an unmodifiable list of Earth orientation parameters
     * @since 12.0
     */
    public List<EarthOrientationParameterMessage> getEarthOrientationParameters() {
        return Collections.unmodifiableList(eops);
    }

    /**
     * Add an Earth orientation parameter.
     * @param eop Earth orientation oarameter message
     * @since 12.0
     */
    public void addEarthOrientationParameter(final EarthOrientationParameterMessage eop) {
        eops.add(eop);
    }

    /**
     * Get the ionosphere Klobuchar messages.
     * @return an unmodifiable list of ionosphere Klobuchar messages
     * @since 12.0
     */
    public List<IonosphereKlobucharMessage> getKlobucharMessages() {
        return Collections.unmodifiableList(klobucharMessages);
    }

    /**
     * Add an ionosphere Klobuchar message.
     * @param klobuchar ionosphere Klobuchar message
     * @since 12.0
     */
    public void addKlobucharMessage(final IonosphereKlobucharMessage klobuchar) {
        klobucharMessages.add(klobuchar);
    }

    /**
     * Get the ionosphere Nequick-G messages.
     * @return an unmodifiable list of ionosphere Nequick-G messages
     * @since 12.0
     */
    public List<IonosphereNequickGMessage> getNequickGMessages() {
        return Collections.unmodifiableList(nequickGMessages);
    }

    /**
     * Add an ionosphere Nequick-G message.
     * @param nequickG ionosphere Nequick-G message
     * @since 12.0
     */
    public void addNequickGMessage(final IonosphereNequickGMessage nequickG) {
        nequickGMessages.add(nequickG);
    }

    /**
     * Get the ionosphere BDGIM messages.
     * @return an unmodifiable list of ionosphere BDGIM messages
     * @since 12.0
     */
    public List<IonosphereBDGIMMessage> getBDGIMMessages() {
        return Collections.unmodifiableList(bdgimMessages);
    }

    /**
     * Add an ionosphere BDGIM message.
     * @param bdgim ionosphere BDGIM message
     * @since 12.0
     */
    public void addBDGIMMessage(final IonosphereBDGIMMessage bdgim) {
        bdgimMessages.add(bdgim);
    }

}
