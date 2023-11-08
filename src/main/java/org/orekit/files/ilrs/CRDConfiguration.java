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
package org.orekit.files.ilrs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Container for Consolidated laser ranging Data Format (CDR) configuration records.
 * @author Bryan Cazabonne
 * @author Rongwang Li
 * @since 10.3
 */
public class CRDConfiguration {

    /** Dict of configuration record. **/
    private Map<String, BaseConfiguration> mapConfigurationRecords;

    /** List of system configuration. **/
    private List<SystemConfiguration> systemConfigurationRecords;

    /**
     * Constructor.
     */
    public CRDConfiguration() {
        systemConfigurationRecords = new ArrayList<>();
        mapConfigurationRecords = new Hashtable<>();
    }

    /**
     * Get the system configuration record.
     * @return the system configuration record
     */
    public SystemConfiguration getSystemRecord() {
        return systemConfigurationRecords.isEmpty() ? null : systemConfigurationRecords.get(0);
    }

    /**
     * Get the system configuration record.
     * @return the system configuration record
     */
    public SystemConfiguration getLastSystemRecord() {
        return systemConfigurationRecords.isEmpty() ? null : systemConfigurationRecords.get(systemConfigurationRecords.size() - 1);
    }

    /**
     * Get the laser configuration record.
     * @return the laser configuration record
     */
    public LaserConfiguration getLaserRecord() {
        return getLaserRecord(getSystemRecord());
    }

    /**
     * Get the detector configuration record.
     * @return the detector configuration record
     */
    public DetectorConfiguration getDetectorRecord() {
        return getDetectorRecord(getSystemRecord());
    }

    /**
     * Get the timing system configuration record.
     * @return the timing system configuration record
     */
    public TimingSystemConfiguration getTimingRecord() {
        return getTimingRecord(getSystemRecord());
    }

    /**
     * Get the transponder configuration record.
     * @return the transponder configuration record
     */
    public TransponderConfiguration getTransponderRecord() {
        return getTransponderRecord(getSystemRecord());
    }


    /**
     * Get the software configuration record.
     * @return the software configuration record
     */
    public SoftwareConfiguration getSoftwareRecord() {
        return getSoftwareRecord(getSystemRecord());
    }

    /**
     * Get the meteorological record.
     * @return the meteorological record
     */
    public MeteorologicalConfiguration getMeteorologicalRecord() {
        return getMeteorologicalRecord(getSystemRecord());
    }

    /**
     * Add a configuration record, such as SystemConfiguation, LaserConfiguration, DetectorConfiguration, etc.
     * @param config the configuration record
     * @since 12.0
     */
    public void addConfigurationRecord(final BaseConfiguration config) {
        if (config == null) {
            return;
        }

        mapConfigurationRecords.put(config.getConfigurationId(), config);

        if (config instanceof SystemConfiguration) {
            // Add to the list systemConfigurationRecords if it is a SystemConfiguration
            systemConfigurationRecords.add((SystemConfiguration) config);
        }
    }

    /**
     * Get the configuration records map.
     * @return the configuration records map
     * @since 12.0
     */
    public Map<String, BaseConfiguration> getConfigurationRecordMap() {
        return Collections.unmodifiableMap(mapConfigurationRecords);
    }

    /**
     * Get configuration record corresponding to the configId.
     * @param configId the id of configuration
     * @return the configuration with configId, or null
     * @since 12.0
     */
    public BaseConfiguration getConfigurationRecord(final String configId) {
        return mapConfigurationRecords.get(configId);
    }

    /**
     * Get a set of configuration ids.
     * @return an unmodifiable set of configuration ids
     * @since 12.0
     */
    public Set<String> getSystemConfigurationIds() {
        return Collections.unmodifiableSet(mapConfigurationRecords.keySet());
    }

    /**
     * Get a list of system configurations.
     * @return an unmodifiable list of system configurations
     * @since 12.0
     */
    public List<SystemConfiguration> getSystemConfigurationRecords() {
        return Collections.unmodifiableList(systemConfigurationRecords);
    }

    /**
     * Get configuration record related to systemRecord and the given class.
     * @param systemRecord system configuration record
     * @param c the class, such as LaserConfiguration, DetectorConfiguration, TimingSystemConfiguration, etc
     * @return the configuration record
     * @since 12.0
     */
    private BaseConfiguration getRecord(final SystemConfiguration systemRecord,
            final Class<? extends BaseConfiguration> c) {
        BaseConfiguration config;
        for (final String configId : systemRecord.getComponents()) {
            config = getConfigurationRecord(configId);
            if (config != null && config.getClass() == c) {
                return config;
            }
        }

        return null;
    }

    /**
     * Get system configuration record. If configId is null, the default(first system configuration record) is returned.
     * @param configId system configuration id, it can be null.
     * @return the system configuration record
     * @since 12.0
     */
    public SystemConfiguration getSystemRecord(final String configId) {
        if (configId == null) {
            // default
            return getSystemRecord();
        }

        final BaseConfiguration config = mapConfigurationRecords.get(configId);
        return config == null ? null : (SystemConfiguration) config;
    }

    /**
     * Get laser configuration record related to the systemRecord.
     * @param systemRecord the system configuration
     * @return the laser configuration record related the the systemRecord
     * @since 12.0
     */
    public LaserConfiguration getLaserRecord(final SystemConfiguration systemRecord) {
        final BaseConfiguration config = getRecord(systemRecord, LaserConfiguration.class);
        return config == null ? null : (LaserConfiguration) config;
    }

    /**
     * Get detector configuration record related to the systemRecord.
     * @param systemRecord the system configuration
     * @return the detector configuration record related the the systemRecord
     * @since 12.0
     */
    public DetectorConfiguration getDetectorRecord(final SystemConfiguration systemRecord) {
        final BaseConfiguration config = getRecord(systemRecord, DetectorConfiguration.class);
        return config == null ? null : (DetectorConfiguration) config;
    }

    /**
     * Get timing system configuration record related to the systemRecord.
     * @param systemRecord the system configuration
     * @return the timing system configuration record related the the systemRecord
     * @since 12.0
     */
    public TimingSystemConfiguration getTimingRecord(final SystemConfiguration systemRecord) {
        final BaseConfiguration config = getRecord(systemRecord, TimingSystemConfiguration.class);
        return config == null ? null : (TimingSystemConfiguration) config;
    }

    /**
     * Get transponder configuration record related to the systemRecord.
     * @param systemRecord the system configuration
     * @return the transponder configuration record related the the systemRecord
     * @since 12.0
     */
    public TransponderConfiguration getTransponderRecord(final SystemConfiguration systemRecord) {
        final BaseConfiguration config = getRecord(systemRecord, TransponderConfiguration.class);
        return config == null ? null : (TransponderConfiguration) config;
    }

    /**
     * Get software configuration record related to the systemRecord.
     * @param systemRecord the system configuration
     * @return the software configuration record related the the systemRecord
     * @since 12.0
     */
    public SoftwareConfiguration getSoftwareRecord(final SystemConfiguration systemRecord) {
        final BaseConfiguration config = getRecord(systemRecord, SoftwareConfiguration.class);
        return config == null ? null : (SoftwareConfiguration) config;
    }

    /**
     * Get meteorological configuration record related to the systemRecord.
     * @param systemRecord the system configuration
     * @return the meteorological configuration record related the the systemRecord
     * @since 12.0
     */
    public MeteorologicalConfiguration getMeteorologicalRecord(final SystemConfiguration systemRecord) {
        final BaseConfiguration config = getRecord(systemRecord, MeteorologicalConfiguration.class);
        return config == null ? null : (MeteorologicalConfiguration) config;
    }

    /**
     * Get calibration target configuration record related to the systemRecord.
     * @param systemRecord the system configuration
     * @return the calibration target configuration record related the the systemRecord
     * @since 12.0
     */
    public CalibrationTargetConfiguration getCalibrationTargetRecord(final SystemConfiguration systemRecord) {
        final BaseConfiguration config = getRecord(systemRecord, CalibrationTargetConfiguration.class);
        return config == null ? null : (CalibrationTargetConfiguration) config;
    }

    /**
     * Get the calibration target configuration record.
     * @return the calibration target configuration record
     * @since 12.0
     */
    public CalibrationTargetConfiguration getCalibrationTargetRecord() {
        return getCalibrationTargetRecord(getSystemRecord());
    }

    /**
     * Base class for configuration record.
     * @since 12.0
     */
    public abstract static class BaseConfiguration {

        /** Configuration ID. */
        private String configurationId;

        /** Empty constructor.
         * <p>
         * This constructor is not strictly necessary, but it prevents spurious
         * javadoc warnings with JDK 18 and later.
         * </p>
         * @since 12.0
         */
        public BaseConfiguration() {
            // nothing to do
        }

        /**
         * Get the configuration ID.
         * @return the configuration ID
         */
        public String getConfigurationId() {
            return configurationId;
        }

        /**
         * Set the configuration ID.
         * @param configurationId the configuration ID to set
         */
        public void setConfigurationId(final String configurationId) {
            this.configurationId = configurationId;
        }

        @Override
        public int hashCode() {
            return toString().hashCode();
        }

        @Override
        public boolean equals(final Object record) {
            if (record == null) {
                return false;
            }

            if (record == this) {
                return true;
            }

            return toString().equals(record.toString());

        }

        /**
         * Get a string representation of the instance in the CRD format.
         * @return a string representation of the instance, in the CRD format.
         * @since 12.0
         */
        public abstract String toCrdString();
    }

    /** Container for system configuration record. */
    public static class SystemConfiguration extends BaseConfiguration {

        /** Transmit Wavelength [m]. */
        private double wavelength;

        /** List of components. **/
        private List<String> components;

        /**
         * Constructor.
         */
        public SystemConfiguration() {
            this.components = new ArrayList<>();
        }

        /**
         * Get the transmit wavelength.
         * @return the transmit wavelength in meters
         */
        public double getWavelength() {
            return wavelength;
        }

        /**
         * Set the transmit wavelength.
         * @param wavelength the wavelength to set
         */
        public void setWavelength(final double wavelength) {
            this.wavelength = wavelength;
        }

        /**
         * Get the system configuration ID.
         * @return the system configuration ID
         */
        public String getSystemId() {
            return getConfigurationId();
        }

        /**
         * Set the system configuration ID.
         * @param systemId the system configuration ID to set
         */
        public void setSystemId(final String systemId) {
            setConfigurationId(systemId);
        }

        /**
         * Get the components (config ids) for system configuration.
         * @return an unmodifiable list of components
         * @since 12.0
         */
        public List<String> getComponents() {
            return Collections.unmodifiableList(components);
        }

        /**
         * Set the components (config ids) for system configuration.
         * @param components the components (config ids)
         * @since 12.0
         */
        public void setComponents(final String[] components) {
            this.components = Arrays.asList(components);
        }

        /** {@inheritDoc} */
        @Override
        public String toCrdString() {
            return String.format("C0 0 %s", toString());
        }

        @Override
        public String toString() {
            // CRD suggested format, excluding the record type
            final StringBuilder sb = new StringBuilder();
            sb.append(String.format("%10.3f %s", wavelength * 1e9, getConfigurationId()));
            for (final String comp : components) {
                sb.append(String.format(" %s", comp));
            }
            return sb.toString().replace(',', '.');
        }
    }

    /** Container for laser configuration record. */
    public static class LaserConfiguration extends BaseConfiguration {

        /** Laser Type. */
        private String laserType;

        /** Primary wavelength [m]. */
        private double primaryWavelength;

        /** Nominal Fire Rate [Hz]. */
        private double nominalFireRate;

        /** Pulse Energy [mJ]. */
        private double pulseEnergy;

        /** Pulse Width [ps]. */
        private double pulseWidth;

        /** Bean divergence [arcsec]. */
        private double beamDivergence;

        /** Number of pulses in outgoing semi-train. */
        private int pulseInOutgoingSemiTrain;

        /** Empty constructor.
         * <p>
         * This constructor is not strictly necessary, but it prevents spurious
         * javadoc warnings with JDK 18 and later.
         * </p>
         * @since 12.0
         */
        public LaserConfiguration() {
            // nothing to do
        }

        /**
         * Get the laser configuration ID.
         * @return the laser configuration ID
         */
        public String getLaserId() {
            return getConfigurationId();
        }

        /**
         * Set the laser configuration ID.
         * @param laserId the laser configuration ID to set
         */
        public void setLaserId(final String laserId) {
            setConfigurationId(laserId);
        }

        /**
         * Get the laser type.
         * @return the laser type
         */
        public String getLaserType() {
            return laserType;
        }

        /**
         * Set the laser type.
         * @param laserType the laser type to set
         */
        public void setLaserType(final String laserType) {
            this.laserType = laserType;
        }

        /**
         * Get the primary wavelength.
         * @return the primary wavelength in meters
         */
        public double getPrimaryWavelength() {
            return primaryWavelength;
        }

        /**
         * Set the primary wavelength.
         * @param primaryWavelength the primary wavelength to set in meters
         */
        public void setPrimaryWavelength(final double primaryWavelength) {
            this.primaryWavelength = primaryWavelength;
        }

        /**
         * Get the nominal fire rate.
         * @return the nominal fire rate in Hz.
         */
        public double getNominalFireRate() {
            return nominalFireRate;
        }

        /**
         * Set the nominal fire rate.
         * @param nominalFireRate the nominal fire rate to set in Hz
         */
        public void setNominalFireRate(final double nominalFireRate) {
            this.nominalFireRate = nominalFireRate;
        }

        /**
         * Get the pulse energy.
         * @return the pulse energy in mJ
         */
        public double getPulseEnergy() {
            return pulseEnergy;
        }

        /**
         * Set the pulse energy.
         * @param pulseEnergy the pulse energy to set in mJ
         */
        public void setPulseEnergy(final double pulseEnergy) {
            this.pulseEnergy = pulseEnergy;
        }

        /**
         * Get the pulse width (FWHM in ps).
         * @return the pulse width
         */
        public double getPulseWidth() {
            return pulseWidth;
        }

        /**
         * Set the pulse width.
         * @param pulseWidth the pulse width to set, ps
         */
        public void setPulseWidth(final double pulseWidth) {
            this.pulseWidth = pulseWidth;
        }

        /**
         * Get the beam divergence.
         * @return the beam divergence in arcsec
         */
        public double getBeamDivergence() {
            return beamDivergence;
        }

        /**
         * Set the beam divergence.
         * @param beamDivergence the beam divergence to set in arcsec
         */
        public void setBeamDivergence(final double beamDivergence) {
            this.beamDivergence = beamDivergence;
        }

        /**
         * Get the number of pulses in outgoing semi-train.
         * @return the number of pulses in outgoing semi-train
         */
        public int getPulseInOutgoingSemiTrain() {
            return pulseInOutgoingSemiTrain;
        }

        /**
         * Set the number of pulses in outgoing semi-train.
         * @param pulseInOutgoingSemiTrain the number of pulses in outgoing semi-train to set
         */
        public void setPulseInOutgoingSemiTrain(final int pulseInOutgoingSemiTrain) {
            this.pulseInOutgoingSemiTrain = pulseInOutgoingSemiTrain;
        }

        /** {@inheritDoc} */
        @Override
        public String toCrdString() {
            return String.format("C1 0 %s", toString());
        }

        @Override
        public String toString() {
            // CRD suggested format, excluding the record type
            // primaryWavelength: m --> nm
            final String str = String.format(
                    "%s %s %.2f %.2f %.2f %.1f %.2f %d", getConfigurationId(),
                    laserType, primaryWavelength * 1e9, nominalFireRate,
                    pulseEnergy, pulseWidth, beamDivergence,
                    pulseInOutgoingSemiTrain);
            return CRD.handleNaN(str).replace(',', '.');
        }

    }

    /** Container for detector configuration record. */
    public static class DetectorConfiguration extends BaseConfiguration {

        /** Detector Type. */
        private String detectorType;

        /** Applicable wavelength. */
        private double applicableWavelength;

        /** Quantum efficiency at applicable wavelength [%]. */
        private double quantumEfficiency;

        /** Applied voltage [V]. */
        private double appliedVoltage;

        /** Dark Count [Hz]. */
        private double darkCount;

        /** Output pulse type. */
        private String outputPulseType;

        /** Output pulse width [ps]. */
        private double outputPulseWidth;

        /** Spectral Filter [m]. */
        private double spectralFilter;

        /** % Transmission of Spectral Filter. */
        private double transmissionOfSpectralFilter;

        /** Spatial Filter [arcsec]. */
        private double spatialFilter;

        /** External Signal processing. */
        private String externalSignalProcessing;

        /** Amplifier Gain. */
        private double amplifierGain;

        /** Amplifier Bandwidth [Hz]. */
        private double amplifierBandwidth;

        /** Amplifier In Use. */
        private String amplifierInUse;

        /** Empty constructor.
         * <p>
         * This constructor is not strictly necessary, but it prevents spurious
         * javadoc warnings with JDK 18 and later.
         * </p>
         * @since 12.0
         */
        public DetectorConfiguration() {
            // nothing to do
        }

        /**
         * Get the detector configuration ID.
         * @return the detector configuration ID
         */
        public String getDetectorId() {
            return getConfigurationId();
        }

        /**
         * Set the detector configuration ID.
         * @param detectorId the detector configuration ID to set
         */
        public void setDetectorId(final String detectorId) {
            setConfigurationId(detectorId);
        }

        /**
         * Get the detector type.
         * @return the detector type
         */
        public String getDetectorType() {
            return detectorType;
        }

        /**
         * Set the detector type.
         * @param detectorType the detector type to set
         */
        public void setDetectorType(final String detectorType) {
            this.detectorType = detectorType;
        }

        /**
         * Get the applicable wavelength.
         * @return pplicable wavelength in meters
         */
        public double getApplicableWavelength() {
            return applicableWavelength;
        }

        /**
         * Set the applicable wavelength.
         * @param applicableWavelength the applicable wavelength to set in meters
         */
        public void setApplicableWavelength(final double applicableWavelength) {
            this.applicableWavelength = applicableWavelength;
        }

        /**
         * Get the quantum efficiency at applicable wavelength.
         * @return the quantum efficiency at applicable wavelength in percents
         */
        public double getQuantumEfficiency() {
            return quantumEfficiency;
        }

        /**
         * Set the quantum efficiency at applicable wavelength.
         * @param quantumEfficiency the efficiency to set in percents
         */
        public void setQuantumEfficiency(final double quantumEfficiency) {
            // NOTE: The quantumEfficiency may be -1.0, which means 'not available'
            if (quantumEfficiency == -1.0) {
                this.quantumEfficiency = Double.NaN;
            } else {
                this.quantumEfficiency = quantumEfficiency;
            }
        }

        /**
         * Get the applied voltage.
         * @return the applied voltage in Volts
         */
        public double getAppliedVoltage() {
            return appliedVoltage;
        }

        /**
         * Set the applied voltage.
         * @param appliedVoltage the applied voltage to set in Volts
         */
        public void setAppliedVoltage(final double appliedVoltage) {
            // NOTE: The quantumEfficiency may be -1.0, which means 'not available' or 'not applicable'
            if (appliedVoltage == -1.0) {
                this.appliedVoltage = Double.NaN;
            } else {
                this.appliedVoltage = appliedVoltage;
            }
        }

        /**
         * Get the dark count.
         * @return the dark count in Hz
         */
        public double getDarkCount() {
            return darkCount;
        }

        /**
         * Set the dark count.
         * @param darkCount the dark count to set in Hz
         */
        public void setDarkCount(final double darkCount) {
         // NOTE: The quantumEfficiency may be -1.0, which means 'not available'
            if (darkCount == -1.0e3) { // -1=na, kHz --> Hz
                this.darkCount = Double.NaN;
            } else {
                this.darkCount = darkCount;
            }
        }

        /**
         * Get the output pulse type.
         * @return the output pulse type
         */
        public String getOutputPulseType() {
            return outputPulseType;
        }

        /**
         * Set the output pulse type.
         * @param outputPulseType the output pulse type to set
         */
        public void setOutputPulseType(final String outputPulseType) {
            this.outputPulseType = outputPulseType;
        }

        /**
         * Get the output pulse width.
         * @return the output pulse width in ps
         */
        public double getOutputPulseWidth() {
            return outputPulseWidth;
        }

        /**
         * Set the output pulse width.
         * @param outputPulseWidth the output pulse width to set in ps
         */
        public void setOutputPulseWidth(final double outputPulseWidth) {
            // NOTE: The quantumEfficiency may be -1.0, which means 'not available' or 'not applicable'
            if (outputPulseWidth == -1.0) {
                this.outputPulseWidth = Double.NaN;
            } else {
                this.outputPulseWidth = outputPulseWidth;
            }
        }

        /**
         * Get the spectral filter.
         * @return the spectral filter in meters
         */
        public double getSpectralFilter() {
            return spectralFilter;
        }

        /**
         * Set the spectral filter.
         * @param spectralFilter  the spectral filter to set in meters
         */
        public void setSpectralFilter(final double spectralFilter) {
            this.spectralFilter = spectralFilter;
        }

        /**
         * Get the percentage of transmission of spectral filter.
         * @return the percentage of transmission of spectral filter
         */
        public double getTransmissionOfSpectralFilter() {
            return transmissionOfSpectralFilter;
        }

        /**
         * Set the percentage of transmission of spectral filter.
         * @param transmissionOfSpectralFilter the percentage to set
         */
        public void setTransmissionOfSpectralFilter(final double transmissionOfSpectralFilter) {
            this.transmissionOfSpectralFilter = transmissionOfSpectralFilter;
        }

        /**
         * Get the spatial filter.
         * @return the spatial filter in arcsec
         */
        public double getSpatialFilter() {
            return spatialFilter;
        }

        /**
         * Set the spatial filter.
         * @param spatialFilter the spatial filter to set in arcsec
         */
        public void setSpatialFilter(final double spatialFilter) {
            this.spatialFilter = spatialFilter;
        }

        /**
         * Get the external signal processing.
         * @return the external signal processing
         */
        public String getExternalSignalProcessing() {
            return externalSignalProcessing;
        }

        /**
         * Set the external signal processing.
         * @param externalSignalProcessing the external signal processing to set
         */
        public void setExternalSignalProcessing(final String externalSignalProcessing) {
            this.externalSignalProcessing = externalSignalProcessing;
        }

        /**
         * Get the amplifier gain.
         * @return the amplifier gain
         */
        public double getAmplifierGain() {
            return amplifierGain;
        }

        /**
         * Set the amplifier gain.
         * @param amplifierGain the amplifier gain to set
         */
        public void setAmplifierGain(final double amplifierGain) {
            this.amplifierGain = amplifierGain;
        }

        /**
         * Get the amplifier bandwidth.
         * @return the amplifier bandwidth in Hz
         */
        public double getAmplifierBandwidth() {
            return amplifierBandwidth;
        }

        /**
         * Set the amplifier bandwidth.
         * @param amplifierBandwidth the amplifier bandwidth to set in Hz
         */
        public void setAmplifierBandwidth(final double amplifierBandwidth) {
            this.amplifierBandwidth = amplifierBandwidth;
        }

        /**
         * Get the amplifier in use.
         * @return the amplifier in use
         */
        public String getAmplifierInUse() {
            return amplifierInUse;
        }

        /**
         * Set the amplifier in use.
         * @param amplifierInUse the amplifier in use to set
         */
        public void setAmplifierInUse(final String amplifierInUse) {
            this.amplifierInUse = amplifierInUse;
        }

        /** {@inheritDoc} */
        @Override
        public String toCrdString() {
            return String.format("C2 0 %s", toString());
        }

        @Override
        public String toString() {
            // CRD suggested format, excluding the record type
            // applicableWavelength, spectralFilter: m --> nm
            // darkCount, amplifierBandwidth: Hz --> kHz
            final String str = String.format(
                    "%s %s %.3f %.2f %.1f %.1f %s %.1f %.2f %.1f %.1f %s %.1f %.1f %s",
                    getConfigurationId(), detectorType,
                    applicableWavelength * 1e9, quantumEfficiency,
                    appliedVoltage, darkCount * 1e-3, outputPulseType,
                    outputPulseWidth, spectralFilter * 1e9,
                    transmissionOfSpectralFilter, spatialFilter,
                    externalSignalProcessing, amplifierGain,
                    amplifierBandwidth * 1e-3, amplifierInUse);
            return CRD.handleNaN(str).replace(',', '.');
        }
    }

    /** Container for timing system configuration record. */
    public static class TimingSystemConfiguration extends BaseConfiguration {

        /** Time Source. */
        private String timeSource;

        /** Frequency Source. */
        private String frequencySource;

        /** Timer. */
        private String timer;

        /** Timer Serial Number. */
        private String timerSerialNumber;

        /** Epoch delay correction [s]. */
        private double epochDelayCorrection;

        /** Empty constructor.
         * <p>
         * This constructor is not strictly necessary, but it prevents spurious
         * javadoc warnings with JDK 18 and later.
         * </p>
         * @since 12.0
         */
        public TimingSystemConfiguration() {
            // nothing to do
        }

        /**
         * Get the time source.
         * @return the time source
         */
        public String getTimeSource() {
            return timeSource;
        }

        /**
         * Get the local timing system configuration ID.
         * @return the local timing system configuration ID
         */
        public String getLocalTimingId() {
            return getConfigurationId();
        }

        /**
         * Set the local timing system configuration ID.
         * @param localTimingId the local timing system configuration ID to set
         */
        public void setLocalTimingId(final String localTimingId) {
            setConfigurationId(localTimingId);
        }

        /**
         * Set the time source.
         * @param timeSource the time source to set
         */
        public void setTimeSource(final String timeSource) {
            this.timeSource = timeSource;
        }

        /**
         * Get the frequency source.
         * @return the frequency source
         */
        public String getFrequencySource() {
            return frequencySource;
        }

        /**
         * Set the frequency source.
         * @param frequencySource the frequency source to set
         */
        public void setFrequencySource(final String frequencySource) {
            this.frequencySource = frequencySource;
        }

        /**
         * Get the timer name.
         * @return the timer name
         */
        public String getTimer() {
            return timer;
        }

        /**
         * Set the timer name.
         * @param timer the timer name to set
         */
        public void setTimer(final String timer) {
            this.timer = timer;
        }

        /**
         * Get the timer serial number.
         * @return the timer serial number
         */
        public String getTimerSerialNumber() {
            return timerSerialNumber;
        }

        /**
         * Set the timer serial number.
         * @param timerSerialNumber the timer serial number to set
         */
        public void setTimerSerialNumber(final String timerSerialNumber) {
            this.timerSerialNumber = timerSerialNumber;
        }

        /**
         * Get the epoch delay correction.
         * @return the epoch delay correction in seconds
         */
        public double getEpochDelayCorrection() {
            return epochDelayCorrection;
        }

        /**
         * Set the epoch delay correction.
         * @param epochDelayCorrection the epoch delay correction to set in seconds
         */
        public void setEpochDelayCorrection(final double epochDelayCorrection) {
            this.epochDelayCorrection = epochDelayCorrection;
        }

        /** {@inheritDoc} */
        @Override
        public String toCrdString() {
            return String.format("C3 0 %s", toString());
        }

        @Override
        public String toString() {
            // CRD suggested format, excluding the record type
            // epochDelayCorrection: s --> us
            final String str = String.format("%s %s %s %s %s %.1f",
                    getConfigurationId(), timeSource, frequencySource, timer,
                    timerSerialNumber, epochDelayCorrection * 1e6);
            return CRD.handleNaN(str).replace(',', '.');
        }
    }

    /** Container for transponder configuration record. */
    public static class TransponderConfiguration extends BaseConfiguration {

        /** Estimated Station UTC offset [s]. */
        private double stationUTCOffset;

        /** Estimated Station Oscillator Drift in parts in 10^15. */
        private double stationOscDrift;

        /** Estimated Transponder UTC offset [s]. */
        private double transpUTCOffset;

        /** Estimated Transponder Oscillator Drift in parts in 10^15. */
        private double transpOscDrift;

        /** Transponder Clock Reference Time. */
        private double transpClkRefTime;

        /** Station clock offset and drift applied indicator. */
        private int stationClockAndDriftApplied;

        /** Spacecraft clock offset and drift applied indicator . */
        private int spacecraftClockAndDriftApplied;

        /** Spacecraft time simplified flag. */
        private boolean isSpacecraftTimeSimplified;

        /** Empty constructor.
         * <p>
         * This constructor is not strictly necessary, but it prevents spurious
         * javadoc warnings with JDK 18 and later.
         * </p>
         * @since 12.0
         */
        public TransponderConfiguration() {
            // nothing to do
        }

        /**
         * Get the transponder configuration ID.
         * @return the transponder configuration ID
         */
        public String getTransponderId() {
            return getConfigurationId();
        }

        /**
         * Set the transponder configuration ID.
         * @param transponderId the transponder configuration ID to set
         */
        public void setTransponderId(final String transponderId) {
            setConfigurationId(transponderId);
        }

        /**
         * Get the estimated station UTC offset.
         * @return the estimated station UTC offset in seconds
         */
        public double getStationUTCOffset() {
            return stationUTCOffset;
        }

        /**
         * Set the estimated station UTC offset.
         * @param stationUTCOffset the estimated station UTC offset to set in seconds
         */
        public void setStationUTCOffset(final double stationUTCOffset) {
            this.stationUTCOffset = stationUTCOffset;
        }

        /**
         * Get the estimated station oscillator drift in parts in 10¹⁵.
         * @return the station oscillator drift
         */
        public double getStationOscDrift() {
            return stationOscDrift;
        }

        /**
         * Set the estimated station oscillator drift in parts in 10¹⁵.
         * @param stationOscDrift the station oscillator drift to set
         */
        public void setStationOscDrift(final double stationOscDrift) {
            this.stationOscDrift = stationOscDrift;
        }

        /**
         * Get the estimated transponder UTC offset.
         * @return the estimated transponder UTC offset in seconds
         */
        public double getTranspUTCOffset() {
            return transpUTCOffset;
        }

        /**
         * Set the estimated transponder UTC offset.
         * @param transpUTCOffset the estimated transponder UTC offset to set in seconds
         */
        public void setTranspUTCOffset(final double transpUTCOffset) {
            this.transpUTCOffset = transpUTCOffset;
        }

        /**
         * Get the estimated transponder oscillator drift in parts in 10¹⁵.
         * @return the estimated transponder oscillator drift
         */
        public double getTranspOscDrift() {
            return transpOscDrift;
        }

        /**
         * Set the estimated transponder oscillator drift in parts in 10¹⁵.
         * @param transpOscDrift the estimated transponder oscillator drift to set
         */
        public void setTranspOscDrift(final double transpOscDrift) {
            this.transpOscDrift = transpOscDrift;
        }

        /**
         * Get the transponder clock reference time.
         * @return the transponder clock reference time
         */
        public double getTranspClkRefTime() {
            return transpClkRefTime;
        }

        /**
         * Set the transponder clock reference time.
         * @param transpClkRefTime the transponder clock reference time to set
         */
        public void setTranspClkRefTime(final double transpClkRefTime) {
            this.transpClkRefTime = transpClkRefTime;
        }

        /**
         * Get the station clock offset and drift applied indicator.
         * @return the station clock offset and drift applied indicator
         */
        public int getStationClockAndDriftApplied() {
            return stationClockAndDriftApplied;
        }

        /**
         * Set the station clock offset and drift applied indicator.
         * @param stationClockAndDriftApplied the indicator to set
         */
        public void setStationClockAndDriftApplied(final int stationClockAndDriftApplied) {
            this.stationClockAndDriftApplied = stationClockAndDriftApplied;
        }

        /**
         * Get the spacecraft clock offset and drift applied indicator.
         * @return the spacecraft clock offset and drift applied indicator
         */
        public int getSpacecraftClockAndDriftApplied() {
            return spacecraftClockAndDriftApplied;
        }

        /**
         * Set the spacecraft clock offset and drift applied indicator.
         * @param spacecraftClockAndDriftApplied the indicator to set
         */
        public void setSpacecraftClockAndDriftApplied(final int spacecraftClockAndDriftApplied) {
            this.spacecraftClockAndDriftApplied = spacecraftClockAndDriftApplied;
        }

        /**
         * Get the spacecraft time simplified flag.
         * @return true if spacecraft time is simplified
         */
        public boolean isSpacecraftTimeSimplified() {
            return isSpacecraftTimeSimplified;
        }

        /**
         * Set the spacecraft time simplified flag.
         * @param isSpacecraftTimeSimplified true if spacecraft time is simplified
         */
        public void setIsSpacecraftTimeSimplified(final boolean isSpacecraftTimeSimplified) {
            this.isSpacecraftTimeSimplified = isSpacecraftTimeSimplified;
        }

        /** {@inheritDoc} */
        @Override
        public String toCrdString() {
            return String.format("C4 0 %s", toString());
        }

        @Override
        public String toString() {
            // CRD suggested format, excluding the record type
            // stationUTCOffset, transpUTCOffset: s --> ns
            final String str = String.format(
                    "%s %.3f %.2f %.3f %.2f %.12f %d %d %d",
                    getConfigurationId(),
                    stationUTCOffset * 1e9, stationOscDrift,
                    transpUTCOffset * 1e9, transpOscDrift,
                    transpClkRefTime,
                    stationClockAndDriftApplied, spacecraftClockAndDriftApplied,
                    isSpacecraftTimeSimplified ? 1 : 0);
            return CRD.handleNaN(str).replace(',', '.');
        }

    }

    /** Container for software configuration record. */
    public static class SoftwareConfiguration extends BaseConfiguration {

        /** Pattern of "[\\s+\\[\\]]". */
        private static final Pattern PATTERN_WHITESPACE_OR_SQUAREBRACKET = Pattern.compile("[\\s+\\[\\]]");

        /** Tracking software in measurement path. */
        private String[] trackingSoftwares;

        /** Tracking software version(s). */
        private String[] trackingSoftwareVersions;

        /** Processing software in measurement path. */
        private String[] processingSoftwares;

        /** Processing software version(s). */
        private String[] processingSoftwareVersions;

        /** Empty constructor.
         * <p>
         * This constructor is not strictly necessary, but it prevents spurious
         * javadoc warnings with JDK 18 and later.
         * </p>
         * @since 12.0
         */
        public SoftwareConfiguration() {
            // nothing to do
        }

        /**
         * Get the software configuration ID.
         * @return the software configuration ID.
         */
        public String getSoftwareId() {
            return getConfigurationId();
        }

        /**
         * Set the software configuration ID.
         * @param softwareId the software configuration ID
         */
        public void setSoftwareId(final String softwareId) {
            setConfigurationId(softwareId);
        }

        /**
         * Get the tracking softwares.
         * @return the tracking softwares
         */
        public String[] getTrackingSoftwares() {
            return trackingSoftwares.clone();
        }

        /**
         * Set the tracking softwares.
         * @param trackingSoftwares the tracking softwares to set
         */
        public void setTrackingSoftwares(final String[] trackingSoftwares) {
            this.trackingSoftwares = trackingSoftwares.clone();
        }

        /**
         * Get the tracking software versions.
         * @return the tracking software versions
         */
        public String[] getTrackingSoftwareVersions() {
            return trackingSoftwareVersions.clone();
        }

        /**
         * Set the tracking software versions.
         * @param trackingSoftwareVersions the tracking software versions to set
         */
        public void setTrackingSoftwareVersions(final String[] trackingSoftwareVersions) {
            this.trackingSoftwareVersions = trackingSoftwareVersions.clone();
        }

        /**
         * Get the processing softwares.
         * @return the processing softwares
         */
        public String[] getProcessingSoftwares() {
            return processingSoftwares.clone();
        }

        /**
         * Set the processing softwares.
         * @param processingSoftwares the processing softwares to set
         */
        public void setProcessingSoftwares(final String[] processingSoftwares) {
            this.processingSoftwares = processingSoftwares.clone();
        }

        /**
         * Get the processing software versions.
         * @return the processing software versions
         */
        public String[] getProcessingSoftwareVersions() {
            return processingSoftwareVersions.clone();
        }

        /**
         * Set the processing software versions.
         * @param processingSoftwareVersions the processing software versions to set
         */
        public void setProcessingSoftwareVersions(final String[] processingSoftwareVersions) {
            this.processingSoftwareVersions = processingSoftwareVersions.clone();
        }

        private static String formatArray(final String[] arr) {
            // comma delimited
            // "[Monitor, Sattrk]" ==> "Monitor,Sattrk"
            // "[conpro, crd_cal, PoissonCRD, gnp]" ==> "conpro,crd_cal,PoissonCRD,gnp"
            final String s = Arrays.toString(arr);
            return PATTERN_WHITESPACE_OR_SQUAREBRACKET.matcher(s).replaceAll("");
        }

        /** {@inheritDoc} */
        @Override
        public String toCrdString() {
            return String.format("C5 0 %s", toString());
        }

        @Override
        public String toString() {
            // CRD suggested format, excluding the record type
            return String.format("%s %s %s %s %s", getConfigurationId(),
                    formatArray(trackingSoftwares),
                    formatArray(trackingSoftwareVersions),
                    formatArray(processingSoftwares),
                    formatArray(processingSoftwareVersions));
        }

    }

    /** Container for meteorological configuration record. */
    public static class MeteorologicalConfiguration extends BaseConfiguration {

        /** Pressure Sensor Manufacturer. */
        private String pressSensorManufacturer;

        /** Pressure Sensor Model. */
        private String pressSensorModel;

        /** Pressure Sensor Serial Number. */
        private String pressSensorSerialNumber;

        /** Temperature Sensor Manufacturer. */
        private String tempSensorManufacturer;

        /** Temperature Sensor Model. */
        private String tempSensorModel;

        /** Temperature Sensor Serial Number. */
        private String tempSensorSerialNumber;

        /** Humidity Sensor Manufacturer. */
        private String humiSensorManufacturer;

        /** Humidity Sensor Model. */
        private String humiSensorModel;

        /** Humidity Sensor Serial Number. */
        private String humiSensorSerialNumber;

        /** Empty constructor.
         * <p>
         * This constructor is not strictly necessary, but it prevents spurious
         * javadoc warnings with JDK 18 and later.
         * </p>
         * @since 12.0
         */
        public MeteorologicalConfiguration() {
            // nothing to do
        }

        /**
         * Get the meteorological configuration ID.
         * @return the meteorological configuration ID
         */
        public String getMeteorologicalId() {
            return getConfigurationId();
        }

        /**
         * Set the meteorological configuration ID.
         * @param meteorologicalId the meteorological configuration ID to set
         */
        public void setMeteorologicalId(final String meteorologicalId) {
            setConfigurationId(meteorologicalId);
        }

        /**
         * Get the pressure sensor manufacturer.
         * @return the pressure sensor manufacturer
         */
        public String getPressSensorManufacturer() {
            return pressSensorManufacturer;
        }

        /**
         * Set the pressure sensor manufacturer.
         * @param pressSensorManufacturer the manufacturer to set
         */
        public void setPressSensorManufacturer(final String pressSensorManufacturer) {
            this.pressSensorManufacturer = pressSensorManufacturer;
        }

        /**
         * Get the pressure sensor model.
         * @return the pressure sensor model
         */
        public String getPressSensorModel() {
            return pressSensorModel;
        }

        /**
         * Set the pressure sensor model.
         * @param pressSensorModel the model to set
         */
        public void setPressSensorModel(final String pressSensorModel) {
            this.pressSensorModel = pressSensorModel;
        }

        /**
         * Get the pressure sensor serial number.
         * @return the pressure sensor serial number
         */
        public String getPressSensorSerialNumber() {
            return pressSensorSerialNumber;
        }

        /**
         * Set the pressure sensor serial number.
         * @param pressSensorSerialNumber the serial number to set
         */
        public void setPressSensorSerialNumber(final String pressSensorSerialNumber) {
            this.pressSensorSerialNumber = pressSensorSerialNumber;
        }

        /**
         * Get the temperature sensor manufacturer.
         * @return the temperature sensor manufacturer
         */
        public String getTempSensorManufacturer() {
            return tempSensorManufacturer;
        }

        /**
         * Set the temperature sensor manufacturer.
         * @param tempSensorManufacturer the temperature sensor manufacturer
         */
        public void setTempSensorManufacturer(final String tempSensorManufacturer) {
            this.tempSensorManufacturer = tempSensorManufacturer;
        }

        /**
         * Get the temperature sensor model.
         * @return the temperature sensor model
         */
        public String getTempSensorModel() {
            return tempSensorModel;
        }

        /**
         * Set the temperature sensor model.
         * @param tempSensorModel the model to set
         */
        public void setTempSensorModel(final String tempSensorModel) {
            this.tempSensorModel = tempSensorModel;
        }

        /**
         * Get the temperature sensor serial number.
         * @return the temperature sensor serial number
         */
        public String getTempSensorSerialNumber() {
            return tempSensorSerialNumber;
        }

        /**
         * Set the temperature sensor serial number.
         * @param tempSensorSerialNumber the serial number to set
         */
        public void setTempSensorSerialNumber(final String tempSensorSerialNumber) {
            this.tempSensorSerialNumber = tempSensorSerialNumber;
        }

        /**
         * Get the humidity sensor manufacturer.
         * @return the humidity sensor manufacturer
         */
        public String getHumiSensorManufacturer() {
            return humiSensorManufacturer;
        }

        /**
         * Set the humidity sensor manufacturer.
         * @param humiSensorManufacturer the manufacturer to set
         */
        public void setHumiSensorManufacturer(final String humiSensorManufacturer) {
            this.humiSensorManufacturer = humiSensorManufacturer;
        }

        /**
         * Get the humidity sensor model.
         * @return the humidity sensor model
         */
        public String getHumiSensorModel() {
            return humiSensorModel;
        }

        /**
         * Set the humidity sensor model.
         * @param humiSensorModel the model to set
         */
        public void setHumiSensorModel(final String humiSensorModel) {
            this.humiSensorModel = humiSensorModel;
        }

        /**
         * Get the humidity sensor serial number.
         * @return the humidity sensor serial number
         */
        public String getHumiSensorSerialNumber() {
            return humiSensorSerialNumber;
        }

        /**
         * Set the humidity sensor serial number.
         * @param humiSensorSerialNumber the serial number to set
         */
        public void setHumiSensorSerialNumber(final String humiSensorSerialNumber) {
            this.humiSensorSerialNumber = humiSensorSerialNumber;
        }

        /** {@inheritDoc} */
        @Override
        public String toCrdString() {
            return String.format("C6 0 %s", toString());
        }

        @Override
        public String toString() {
            // CRD suggested format, excluding the record type
            return String.format("%s %s %s %s %s %s %s %s %s %s",
                    getConfigurationId(), pressSensorManufacturer,
                    pressSensorModel, pressSensorSerialNumber,
                    tempSensorManufacturer, tempSensorModel,
                    tempSensorSerialNumber, humiSensorManufacturer,
                    humiSensorModel, humiSensorSerialNumber);
        }
    }

    /**
     * Container for calibration target configuration record.
     * @since 12.0
     */
    public static class CalibrationTargetConfiguration extends BaseConfiguration {

        /** Target name or ID. */
        private String targetName;

        /** Surveyed target distance. */
        private double surveyedTargetDistance;

        /** Survey error. */
        private double surveyError;

        /** Sum of all constant delays (m, one way). */
        private double sumOfAllConstantDelays;

        /** Pulse Energy [mJ]. */
        private double pulseEnergy;

        /** Processing software name. */
        private String processingSoftwareName;

        /** Processing software version. */
        private String processingSoftwareVersion;

        /** Empty constructor.
         * <p>
         * This constructor is not strictly necessary, but it prevents spurious
         * javadoc warnings with JDK 18 and later.
         * </p>
         * @since 12.0
         */
        public CalibrationTargetConfiguration() {
            // nothing to do
        }

        /**
         * Get the target name or ID.
         * @return the target name or ID
         */
        public String getTargetName() {
            return targetName;
        }

        /**
         * Set the target name or ID.
         * @param targetName target name or ID to set
         */
        public void setTargetName(final String targetName) {
            this.targetName = targetName;
        }

        /**
         * Get the surveyed target distance.
         * @return the surveyed target distance in meters
         */
        public double getSurveyedTargetDistance() {
            return surveyedTargetDistance;
        }

        /**
         * Set the surveyed target distance.
         * @param surveyedTargetDistance the surveyed target distance to set, in meters
         */
        public void setSurveyedTargetDistance(final double surveyedTargetDistance) {
            this.surveyedTargetDistance = surveyedTargetDistance;
        }

        /**
         * Get the survey error.
         * @return the survey error in meters
         */
        public double getSurveyError() {
            return surveyError;
        }

        /**
         * Set the survey error.
         * @param surveyError the survey error to set, in meters
         */
        public void setSurveyError(final double surveyError) {
            this.surveyError = surveyError;
        }

        /**
         * Get the sum of all constant delays (electronic, geometric, optical) that
         * are not included in the time of flight measurements or time- variant
         * or point angle-variant delays in the “42” record below (m, one way).
         * @return the sum of all constant delays
         */
        public double getSumOfAllConstantDelays() {
            return sumOfAllConstantDelays;
        }

        /**
         * Set the sum of all constant delays (electronic, geometric, optical) that
         * are not included in the time of flight measurements or time- variant
         * or point angle-variant delays in the “42” record below (m, one way).
         * @param sumOfAllConstantDelays the sum of all constant delays
         */
        public void setSumOfAllConstantDelays(final double sumOfAllConstantDelays) {
            this.sumOfAllConstantDelays = sumOfAllConstantDelays;
        }

        /**
         * Get the pulse energy.
         * @return the pulse energy in mJ
         */
        public double getPulseEnergy() {
            return pulseEnergy;
        }

        /**
         * Set the pulse energy.
         * @param pulseEnergy the pulse energy to set, in mJ
         */
        public void setPulseEnergy(final double pulseEnergy) {
            this.pulseEnergy = pulseEnergy;
        }

        /**
         * Get the processing software name.
         * @return the processing software name
         */
        public String getProcessingSoftwareName() {
            return processingSoftwareName;
        }

        /**
         * Set the processing software name.
         * @param processingSoftwareName the processing software name to set
         */
        public void setProcessingSoftwareName(final String processingSoftwareName) {
            this.processingSoftwareName = processingSoftwareName;
        }

        /**
         * Get the processing software version.
         * @return the processing software version
         */
        public String getProcessingSoftwareVersion() {
            return processingSoftwareVersion;
        }

        /**
         * Set the processing software version.
         * @param processingSoftwareVersion the processing software version to set
         */
        public void setProcessingSoftwareVersion(final String processingSoftwareVersion) {
            this.processingSoftwareVersion = processingSoftwareVersion;
        }

        /** {@inheritDoc} */
        @Override
        public String toCrdString() {
            return String.format("C7 0 %s", toString());
        }

        @Override
        public String toString() {
            // CRD suggested format, excluding the record type
            // surveyError: m --> mm
            final String str = String.format("%s %s %.5f %.2f %.4f %.2f %s %s",
                    getConfigurationId(), targetName, surveyedTargetDistance,
                    surveyError * 1e3, sumOfAllConstantDelays, pulseEnergy,
                    processingSoftwareName, processingSoftwareVersion);
            return CRD.handleNaN(str).replace(',', '.');
        }

    }
}
