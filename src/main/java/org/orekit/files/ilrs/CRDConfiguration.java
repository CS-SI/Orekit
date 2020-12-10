/* Copyright 2002-2020 CS GROUP
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

/**
 * Container for Consolidated laser ranging Data Format (CDR) configuration records.
 * @author Bryan Cazabonne
 * @since 10.3
 */
public class CRDConfiguration {

    /** System configuration record. */
    private SystemConfiguration systemRecord;

    /** Laser configuration record. */
    private LaserConfiguration laserRecord;

    /** Detector configuration record. */
    private DetectorConfiguration detectorRecord;

    /** Timing system configuration record. */
    private TimingSystemConfiguration timingRecord;

    /** Transponder configuration record. */
    private TransponderConfiguration transponderRecord;

    /** Software configuration record. */
    private SoftwareConfiguration softwareRecord;

    /** Meteorological configuration record. */
    private MeteorologicalConfiguration meteorologicalRecord;

    /**
     * Get the system configuration record.
     * @return the system configuration record
     */
    public SystemConfiguration getSystemRecord() {
        return systemRecord;
    }

    /**
     * Set the system configuration record.
     * @param systemRecord the record to set
     */
    public void setSystemRecord(final SystemConfiguration systemRecord) {
        this.systemRecord = systemRecord;
    }

    /**
     * Get the laser configuration record.
     * @return the laser configuration record
     */
    public LaserConfiguration getLaserRecord() {
        return laserRecord;
    }

    /**
     * Set the laser configuration record.
     * @param laserRecord the record to set
     */
    public void setLaserRecord(final LaserConfiguration laserRecord) {
        this.laserRecord = laserRecord;
    }

    /**
     * Get the detector configuration record.
     * @return the detector configuration record
     */
    public DetectorConfiguration getDetectorRecord() {
        return detectorRecord;
    }

    /**
     * Set the detector configuration record.
     * @param detectorRecord the record to set
     */
    public void setDetectorRecord(final DetectorConfiguration detectorRecord) {
        this.detectorRecord = detectorRecord;
    }

    /**
     * Get the timing system configuration record.
     * @return the timing system configuration record
     */
    public TimingSystemConfiguration getTimingRecord() {
        return timingRecord;
    }

    /**
     * Set the timing system configuration record.
     * @param timingRecord the record to set
     */
    public void setTimingRecord(final TimingSystemConfiguration timingRecord) {
        this.timingRecord = timingRecord;
    }

    /**
     * Get the transponder configuration record.
     * @return the transponder configuration record
     */
    public TransponderConfiguration getTransponderRecord() {
        return transponderRecord;
    }

    /**
     * Set the transponder configuration record.
     * @param transponderRecord the record to set
     */
    public void setTransponderRecord(final TransponderConfiguration transponderRecord) {
        this.transponderRecord = transponderRecord;
    }

    /**
     * Get the software configuration record.
     * @return the software configuration record
     */
    public SoftwareConfiguration getSoftwareRecord() {
        return softwareRecord;
    }

    /**
     * Set the software configuration record.
     * @param softwareRecord the record to set
     */
    public void setSoftwareRecord(final SoftwareConfiguration softwareRecord) {
        this.softwareRecord = softwareRecord;
    }

    /**
     * Get the meteorological record.
     * @return the meteorological record
     */
    public MeteorologicalConfiguration getMeteorologicalRecord() {
        return meteorologicalRecord;
    }

    /**
     * Set the meteorological record.
     * @param meteorologicalRecord the meteorological record to set
     */
    public void setMeteorologicalRecord(final MeteorologicalConfiguration meteorologicalRecord) {
        this.meteorologicalRecord = meteorologicalRecord;
    }

    /** Container for system configuration record. */
    public static class SystemConfiguration {

        /** Transmit Wavelength [m]. */
        private double wavelength;

        /** System configuration ID. */
        private String systemId;

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
            return systemId;
        }

        /**
         * Set the system configuration ID.
         * @param systemId the system configuration ID to set
         */
        public void setSystemId(final String systemId) {
            this.systemId = systemId;
        }

    }

    /** Container for laser configuration record. */
    public static class LaserConfiguration {

        /** Laser configuration ID. */
        private String laserId;

        /** Laser Type. */
        private String laserType;

        /** Primary wavelength [m]. */
        private double primaryWavelength;

        /** Nominal Fire Rate [Hz]. */
        private double nominalFireRate;

        /** Pulse Energy [mJ]. */
        private double pulseEnergy;

        /** Pulse Width. */
        private double pulseWidth;

        /** Bean divergence [arcsec]. */
        private double beamDivergence;

        /** Number of pulses in outgoing semi-train. */
        private int pulseInOutgoingSemiTrain;


        /**
         * Get the laser configuration ID.
         * @return the laser configuration ID
         */
        public String getLaserId() {
            return laserId;
        }

        /**
         * Set the laser configuration ID.
         * @param laserId the laser configuration ID to set
         */
        public void setLaserId(final String laserId) {
            this.laserId = laserId;
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
         * @param pulseWidth the pulse width to set
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

    }

    /** Container for detector configuration record. */
    public static class DetectorConfiguration {

        /** Detector configuration ID. */
        private String detectorId;

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

        /**
         * Get the detector configuration ID.
         * @return the detector configuration ID
         */
        public String getDetectorId() {
            return detectorId;
        }

        /**
         * Set the detector configuration ID.
         * @param detectorId the detector configuration ID to set
         */
        public void setDetectorId(final String detectorId) {
            this.detectorId = detectorId;
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
            this.quantumEfficiency = quantumEfficiency;
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
            this.appliedVoltage = appliedVoltage;
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
            this.darkCount = darkCount;
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
            this.outputPulseWidth = outputPulseWidth;
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

    }

    /** Container for timing system configuration record. */
    public static class TimingSystemConfiguration {

        /** Local timing system configuration ID. */
        private String localTimingId;

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
            return localTimingId;
        }

        /**
         * Set the local timing system configuration ID.
         * @param localTimingId the local timing system configuration ID to set
         */
        public void setLocalTimingId(final String localTimingId) {
            this.localTimingId = localTimingId;
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

    }

    /** Container for transponder configuration record. */
    public static class TransponderConfiguration {

        /** Transponder configuration ID. */
        private String transponderId;

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

        /**
         * Get the transponder configuration ID.
         * @return the transponder configuration ID
         */
        public String getTransponderId() {
            return transponderId;
        }

        /**
         * Set the transponder configuration ID.
         * @param transponderId the transponder configuration ID to set
         */
        public void setTransponderId(final String transponderId) {
            this.transponderId = transponderId;
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

    }

    /** Container for software configuration record. */
    public static class SoftwareConfiguration {

        /** Software configuration ID. */
        private String softwareId;

        /** Tracking software in measurement path. */
        private String[] trackingSoftwares;

        /** Tracking software version(s). */
        private String[] trackingSoftwareVersions;

        /** Processing software in measurement path. */
        private String[] processingSoftwares;

        /** Processing software version(s). */
        private String[] processingSoftwareVersions;

        /**
         * Get the software configuration ID.
         * @return the software configuration ID.
         */
        public String getSoftwareId() {
            return softwareId;
        }

        /**
         * Set the software configuration ID.
         * @param softwareId the software configuration ID
         */
        public void setSoftwareId(final String softwareId) {
            this.softwareId = softwareId;
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

    }

    /** Container for meteorological configuration record. */
    public static class MeteorologicalConfiguration {

        /** Meteorological configuration ID. */
        private String meteorologicalId;

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

        /**
         * Get the meteorological configuration ID.
         * @return the meteorological configuration ID
         */
        public String getMeteorologicalId() {
            return meteorologicalId;
        }

        /**
         * Set the meteorological configuration ID.
         * @param meteorologicalId the meteorological configuration ID to set
         */
        public void setMeteorologicalId(final String meteorologicalId) {
            this.meteorologicalId = meteorologicalId;
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

    }

}
