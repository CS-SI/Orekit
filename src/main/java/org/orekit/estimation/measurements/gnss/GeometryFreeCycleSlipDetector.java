/* Copyright 2002-2019 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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
package org.orekit.estimation.measurements.gnss;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hipparchus.fitting.PolynomialCurveFitter;
import org.hipparchus.fitting.WeightedObservedPoint;
import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.gnss.Frequency;
import org.orekit.gnss.MeasurementType;
import org.orekit.gnss.ObservationData;
import org.orekit.gnss.ObservationDataSet;
import org.orekit.gnss.SatelliteSystem;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;


/** Double frequency cycle slip detectors.
 * The detector is based the algorithm given in <a
 * href="https://gssc.esa.int/navipedia/index.php/Detector_based_in_carrier_phase_data:_The_geometry-free_combination">
 * Detector based in carrier phase data: The geometry-free combination</a> by Zornoza and M. Hernández-Pajares. Within this class
 * a second order polynomial is used to smooth the data. We consider a cycle-slip occurring if the current measurement is  too
 * far from the one predicted with the polynomial (algorithm 1 on Navipedia).
 * <p>
 * For building the detector, one should give a RINEX file a threshold, a gap time limit, and an integer.
 * After construction of the detectors, one can have access to a List of CycleData. Each CycleDate represents
 * a link between the station (define by the RINEX file) and a satellite at a specific frequency. For each cycle data,
 * one has access to the begin and end of availability, and a sorted set which contains all the date at which
 * cycle-slip have been detected
 * </p>
 * <p>
 * @author David Soulard
 *
 */

public class GeometryFreeCycleSlipDetector extends AbstractCycleSlipDetector {

    /** Constant to transform a value in MHz to a value in Hz. */
    private static final double MHZ_2_HZ = 1e6;

    /** Time constant for the threshold. */
    private final double t0;

    /** Multiplicative factor for customizing the threshold. */
    private final double fact;

    /** Geometry free cycle slip detector Constructor.
     * @param obserDataSets observationDataSet list from a RINEX file
     * @param dt time gap threshold between two consecutive measurement (if time between two consecutive measurement is greater than dt, a cycle slip is declared)
     * @param threshold as for Geometry free a threshold is computed for each frequency, this is never used.
     * @param n number of measurement before starting
     * @param t0 time constant for the threshold
     * @param fact multiplicative factor for customizing the threshold
     */
    public GeometryFreeCycleSlipDetector(final List<ObservationDataSet> obserDataSets, final double dt, final double threshold, final int n, final double t0, final double fact) {
        super(obserDataSets, dt, threshold, n);
        this.t0     = t0;
        this.fact   = fact;
        for (final ObservationDataSet obser : obserDataSets) {
            setStationName(obser.getHeader().getMarkerName());
            manageData(obser, dt);
        }
    }

    @Override
    protected void manageData(final ObservationDataSet obser, final double dt) {
        final int numSat = obser.getPrnNumber();
        String nameSat = "";
        final AbsoluteDate date = obser.getDate();
        final List<DataMeasure> dataPhase = new ArrayList<>();
        for (ObservationData od: obser.getObservationData()) {
            if (!Double.isNaN(od.getValue())) {
                //check if there is a cycle_slip at the current time
                if (od.getObservationType().getMeasurementType() == MeasurementType.CARRIER_PHASE) {
                    final Frequency f = od.getObservationType().getFrequency(obser.getSatelliteSystem());
                    final double value = od.getValue();
                    nameSat = setName(numSat, f.getSatelliteSystem());
                    dataPhase.add(new DataMeasure(value, f));
                }
            }
        }
        //If there are than two measurements we can use the data at this date
        if (dataPhase.size() >= 2) {
            if (containsL1L2(dataPhase)) {
                final DataMeasure[] meas = getL1L2(dataPhase);
                final double value = meas[0].getValue() - meas[1].getValue();
                nameSat = setName(numSat, obser.getSatelliteSystem());
                final Frequency freq = getFrequency(12, meas[0].getFrequency().getSatelliteSystem());
                final int slip = cycleSlipDetection(nameSat, date, value, freq, 12.0);
                if (slip == 1) { cycleSlipDataSet(nameSat, date, value, freq); };

            } else if (containsL1L5(dataPhase)) {
                final DataMeasure[] meas = getL1L5(dataPhase);
                final double value = meas[0].getValue() - meas[1].getValue();
                nameSat = setName(numSat, obser.getSatelliteSystem());
                final Frequency freq = getFrequency(15, meas[0].getFrequency().getSatelliteSystem());
                final int slip = cycleSlipDetection(nameSat, date, value, freq, 15.0);
                if (slip == 1) { cycleSlipDataSet(nameSat, date, value, freq); };

            } else if (containsL2L5(dataPhase)) {
                final DataMeasure[] meas = getL2L5(dataPhase);
                final double value = meas[0].getValue() - meas[1].getValue();
                nameSat = setName(numSat, obser.getSatelliteSystem());
                final Frequency freq = getFrequency(25, meas[0].getFrequency().getSatelliteSystem());
                final int slip = cycleSlipDetection(nameSat, date, value, freq, 25.0);
                if (slip == 1) { cycleSlipDataSet(nameSat, date, value, freq); };
            }
        }
    }

    /** Compute if there is a cycle slip at an specific date.
     * @param nameSat name of the satellite, on the pre-defined format (e.g.: GPS - 07 for satellite 7 of GPS constellation)
     * @param currentDate the date at which we check if a cycle-slip occurs
     * @param value phase measurement minus code measurement
     * @param freq frequency used (expressed in MHz)
     * @param type of measurement combination use (e.g.: 12 for L1-L2)
     * @return 0 if a cycle slip has been detected, 1  otherwise.
     */
    protected int cycleSlipDetection(final String nameSat, final AbsoluteDate currentDate, final double value, final Frequency freq, final double type) {
        final List<CycleSlipDetectorResults> data = getResults();
        final List<Map<Frequency, DataForDetection>> stuff = getStuffReference();
        if (data != null) {
            for (CycleSlipDetectorResults d: data) {
                //found the right cycle data
                if (d.getSatelliteName().compareTo(nameSat) == 0 && d.getCycleSlipMap().containsKey(freq)) {
                    final Map<Frequency, DataForDetection> values = stuff.get(data.indexOf(d));
                    final DataForDetection v = values.get(freq);
                    //Check the time gap condition
                    final double deltaT = FastMath.abs(currentDate.durationFrom(v.getFiguresReference()[v.getWrite()].getDate()));
                    if (deltaT > getMaxTimeBeetween2Measurement()) {
                        d.addCycleSlipDate(freq, currentDate);
                        v.resetFigures(new SlipComputationData[getMinMeasurementNumber()], value, currentDate);
                        d.setDate(freq, currentDate);
                        return 0;
                    }
                    //Compute the fitting polynamil if there are enough measurement since last cycle-slip
                    if (v.getCanBeComputed() >= getMinMeasurementNumber()) {
                        final double threshold = fact * getThreshold(deltaT, freq.getSatelliteSystem(), (int) type);
                        final List<WeightedObservedPoint> xy = new ArrayList<>();
                        for (int i = 0; i < getMinMeasurementNumber(); i++) {
                            final SlipComputationData current = v.getFiguresReference()[i];

                            xy.add(new WeightedObservedPoint(1.0, current.getDate().durationFrom(currentDate),
                                                             current.getValue()));
                        }
                        final PolynomialCurveFitter fitting = PolynomialCurveFitter.create(2);
                        //Check if there is a cycle_slip
                        if (FastMath.abs(fitting.fit(xy)[0] - value) > threshold) {
                            d.addCycleSlipDate(freq, currentDate);
                            v.resetFigures(new SlipComputationData[getMinMeasurementNumber()], value, currentDate);
                            d.setDate(freq, currentDate);
                            return 0;
                        }
                    } else {
                        break;
                    }
                }
            }
        }
        return 1;
    }

    /**
     * Return the correct frequency associated with the current combination.
     * @param type type of combination (e.g.: 12 for L1-L2).
     * @param sys Satellite system considering
     * @return Frequency
     */
    private Frequency getFrequency(final int type, final SatelliteSystem sys) {
        switch (type) {
            case 12:
                switch (sys) {
                    case GPS:
                        return Frequency.G0102;

                    case GLONASS:
                        return Frequency.R102;

                    case GALILEO:
                        return Frequency.E105b;

                    case BEIDOU:
                        return Frequency.B102;

                    default:
                        throw new OrekitException(OrekitMessages.INVALID_SATELLITE_SYSTEM, sys);
                }

            case 15:
                switch (sys) {
                    case GPS:
                        return Frequency.G0105;

                    case GLONASS:
                        return Frequency.R103;

                    case GALILEO:
                        return Frequency.E105a;

                    case BEIDOU:
                        return Frequency.B103;

                    default:
                        throw new OrekitException(OrekitMessages.INVALID_SATELLITE_SYSTEM, sys);
                }

            case 25:
                switch (sys) {
                    case GPS:
                        return Frequency.G0205;

                    case GLONASS:
                        return Frequency.R203;

                    case GALILEO:
                        return Frequency.E5b05a;

                    case BEIDOU:
                        return Frequency.B203;

                    default:
                        throw new OrekitException(OrekitMessages.INVALID_SATELLITE_SYSTEM, sys);
                }

            default:
                //Cannot appears but avoid the need to add a exception.
                return Frequency.B01;
        }
    }

    /**
     * Compute the Threshold = 3/2(lambda1 -lambda2)*(1-exp(-deltaT/t0)).
     * @param deltaT time between two consecutive epochs
     * @param sys satellite system use.
     * @param type depends on which measurement is it (e.g.  12 for phase(L1) -phase(L2) or 25 for phase(L2)-phase(L5)).
     * @return the threshold computed
     */
    private double getThreshold(final double deltaT, final SatelliteSystem sys, final int type) {
        final double lambda1;
        final double lambda2;
        switch (type) {
            //case L1L2
            case 12:
                switch (sys) {
                    case GPS:
                        lambda1 = Constants.SPEED_OF_LIGHT / (Frequency.G01.getMHzFrequency() * MHZ_2_HZ);
                        lambda2 = Constants.SPEED_OF_LIGHT / (Frequency.G02.getMHzFrequency() * MHZ_2_HZ);
                        return 1.5 * (lambda2 - lambda1) * (1 - FastMath.exp(-deltaT / t0) * 0.5);

                    case GALILEO:
                        lambda1 = Constants.SPEED_OF_LIGHT / (Frequency.E01.getMHzFrequency() * MHZ_2_HZ);
                        lambda2 = Constants.SPEED_OF_LIGHT / (Frequency.E07.getMHzFrequency() * MHZ_2_HZ);
                        return 1.5 * (lambda2 - lambda1) * (1 - FastMath.exp(-deltaT / t0) * 0.5);

                    case GLONASS:
                        lambda1 = Constants.SPEED_OF_LIGHT / (Frequency.R01.getMHzFrequency() * MHZ_2_HZ);
                        lambda2 = Constants.SPEED_OF_LIGHT / (Frequency.R02.getMHzFrequency() * MHZ_2_HZ);
                        return 1.5 * (lambda2 - lambda1) * (1 - FastMath.exp(-deltaT / t0) * 0.5);

                    case BEIDOU:
                        lambda1 = Constants.SPEED_OF_LIGHT / (Frequency.B01.getMHzFrequency() * MHZ_2_HZ);
                        lambda2 = Constants.SPEED_OF_LIGHT / (Frequency.B02.getMHzFrequency() * MHZ_2_HZ);
                        return 1.5 * (lambda2 - lambda1) * (1 - FastMath.exp(-deltaT / t0) * 0.5);
                    default:
                        return 0.0;
                }
            case 15:
                switch (sys) {
                    case GPS:
                        lambda1 = Constants.SPEED_OF_LIGHT / (Frequency.G01.getMHzFrequency() * MHZ_2_HZ);
                        lambda2 = Constants.SPEED_OF_LIGHT / (Frequency.G05.getMHzFrequency() * MHZ_2_HZ);
                        return 1.5 * (lambda2 - lambda1) * (1 - FastMath.exp(-deltaT / t0) * 0.5);

                    case GALILEO:
                        lambda1 = Constants.SPEED_OF_LIGHT / (Frequency.E01.getMHzFrequency() * MHZ_2_HZ);
                        lambda2 = Constants.SPEED_OF_LIGHT / (Frequency.E05.getMHzFrequency() * MHZ_2_HZ);
                        return 1.5 * (lambda2 - lambda1) * (1 - FastMath.exp(-deltaT / t0) * 0.5);

                    case GLONASS:
                        lambda1 = Constants.SPEED_OF_LIGHT / (Frequency.R01.getMHzFrequency() * MHZ_2_HZ);
                        lambda2 = Constants.SPEED_OF_LIGHT / (Frequency.R03.getMHzFrequency() * MHZ_2_HZ);
                        return 1.5 * (lambda2 - lambda1) * (1 - FastMath.exp(-deltaT / t0) * 0.5);

                    case BEIDOU:
                        lambda1 = Constants.SPEED_OF_LIGHT / (Frequency.B01.getMHzFrequency() * MHZ_2_HZ);
                        lambda2 = Constants.SPEED_OF_LIGHT / (Frequency.B03.getMHzFrequency() * MHZ_2_HZ);
                        return 1.5 * (lambda2 - lambda1) * (1 - FastMath.exp(-deltaT / t0) * 0.5);
                    default:
                        return 0.0;

                }
            case 25:
                switch (sys) {
                    case GPS:
                        lambda1 = Constants.SPEED_OF_LIGHT / (Frequency.G02.getMHzFrequency() * MHZ_2_HZ);
                        lambda2 = Constants.SPEED_OF_LIGHT / (Frequency.G05.getMHzFrequency() * MHZ_2_HZ);
                        return 1.5 * (lambda2 - lambda1) * (1 - FastMath.exp(-deltaT / t0) * 0.5);

                    case GALILEO:
                        lambda1 = Constants.SPEED_OF_LIGHT / (Frequency.E07.getMHzFrequency() * MHZ_2_HZ);
                        lambda2 = Constants.SPEED_OF_LIGHT / (Frequency.E05.getMHzFrequency() * MHZ_2_HZ);
                        return 1.5 * (lambda2 - lambda1) * (1 - FastMath.exp(-deltaT / t0) * 0.5);

                    case GLONASS:
                        lambda1 = Constants.SPEED_OF_LIGHT / (Frequency.R02.getMHzFrequency() * MHZ_2_HZ);
                        lambda2 = Constants.SPEED_OF_LIGHT / (Frequency.R03.getMHzFrequency() * MHZ_2_HZ);
                        return 1.5 * (lambda2 - lambda1)  * (1 - FastMath.exp(-deltaT / t0) * 0.5);

                    case BEIDOU:
                        lambda1 = Constants.SPEED_OF_LIGHT / (Frequency.B02.getMHzFrequency() * MHZ_2_HZ);
                        lambda2 = Constants.SPEED_OF_LIGHT / (Frequency.B03.getMHzFrequency() * MHZ_2_HZ);
                        return 1.5 * (lambda2 - lambda1) * (1 - FastMath.exp(-deltaT / t0) * 0.5);
                    default:
                        return 0.0;
                }
            default:
                return 0.0;
        }

    }

    /**
     * Constructor for the L1L2 (phase(L1)-phase(L2)) measurement.
     * @param measurements list of data measure
     * @return a double corresponding to phase measurement on L1 minus the phase measurement on L2
     */
    private DataMeasure[] getL1L2(final List<DataMeasure> measurements) {
        final DataMeasure[] result = new DataMeasure[2];
        final SatelliteSystem satSys = measurements.get(0).getFrequency().getSatelliteSystem();
        switch (satSys) {
            case GPS:
                for (DataMeasure d: measurements) {
                    if (d.getFrequency().compareTo(Frequency.G01) == 0) {
                        result[0] = d;
                    }
                    if (d.getFrequency().compareTo(Frequency.G02) == 0) {
                        result[1] = d;
                    }
                }
                break;

            case GLONASS:
                for (DataMeasure d: measurements) {
                    if (d.getFrequency().compareTo(Frequency.R01) == 0) {
                        result[0] = d;
                    }
                    if (d.getFrequency().compareTo(Frequency.R02) == 0) {
                        result[1] = d;
                    }
                }
                break;

            case GALILEO:
                for (DataMeasure d: measurements) {
                    if (d.getFrequency().compareTo(Frequency.E01) == 0) {
                        result[0] = d;
                    }
                    if (d.getFrequency().compareTo(Frequency.E07) == 0) {
                        result[1] = d;
                    }
                }
                break;
            case BEIDOU:
                for (DataMeasure d: measurements) {
                    if  (d.getFrequency().compareTo(Frequency.B01) == 0) {
                        result[0] = d;
                    }
                    if (d.getFrequency().compareTo(Frequency.B02) == 0) {
                        result[1] = d;
                    }
                }
                break;
            case QZSS:
                for (DataMeasure d: measurements) {
                    if  (d.getFrequency().compareTo(Frequency.J01) == 0) {
                        result[0] = d;
                    }
                    if (d.getFrequency().compareTo(Frequency.J02) == 0) {
                        result[1] = d;
                    }
                }
                break;
            default:
                throw new OrekitException(OrekitMessages.INVALID_SATELLITE_SYSTEM);
        }
        return result;
    }
    /**
     * Method to know if the list of measurement contains L1 and L2 measurement.
     * @param dataMeasures list of the previous measurement
     * @return boolean
     */
    private boolean containsL1L2(final List<DataMeasure> dataMeasures) {
        int l1 = 0;
        int l2 = 0;
        final SatelliteSystem satSys = dataMeasures.get(0).getFrequency().getSatelliteSystem();
        switch (satSys) {
            case GPS:
                for (DataMeasure d: dataMeasures) {
                    if (d.getFrequency().compareTo(Frequency.G01) == 0) { l1 = 1; };
                    if (d.getFrequency().compareTo(Frequency.G02) == 0) { l2 = 1; };
                }
                break;

            case GLONASS:
                for (DataMeasure d: dataMeasures) {
                    if (d.getFrequency().compareTo(Frequency.R01) == 0) { l1 = 1; };
                    if (d.getFrequency().compareTo(Frequency.R02) == 0) { l2 = 1; };
                }
                break;

            case GALILEO:
                for (DataMeasure d: dataMeasures) {
                    if (d.getFrequency().compareTo(Frequency.E01) == 0) { l1 = 1; };
                    if (d.getFrequency().compareTo(Frequency.E07) == 0) { l2 = 1; };
                }
                break;
            case BEIDOU:
                for (DataMeasure d: dataMeasures) {
                    if (d.getFrequency().compareTo(Frequency.B01) == 0) { l1 = 1; };
                    if (d.getFrequency().compareTo(Frequency.B02) == 0) { l2 = 1; };
                }
                break;
            case QZSS:
                for (DataMeasure d: dataMeasures) {
                    if (d.getFrequency().compareTo(Frequency.J01) == 0) { l1 = 1; };
                    if (d.getFrequency().compareTo(Frequency.J02) == 0) { l2 = 1; };
                }
                break;
            default:
                break;
        }
        if (l1 == 1 && l2 == 1) {
            return true;
        } else {
            return false;
        }

    }

    /**
     * Method to know if the list of measurement contains L1 and L5 measurement.
     * @param dataMeasures list of the previous measurement
     * @return boolean
     */
    private boolean containsL1L5(final  List<DataMeasure> dataMeasures) {
        int l1 = 0;
        int l5 = 0;
        final SatelliteSystem satSys = dataMeasures.get(0).getFrequency().getSatelliteSystem();
        switch (satSys) {
            case GPS:
                for (DataMeasure d: dataMeasures) {
                    if (d.getFrequency().compareTo(Frequency.G01) == 0) { l1 = 1; };
                    if (d.getFrequency().compareTo(Frequency.G05) == 0) { l5 = 1; };
                }
                break;

            case GLONASS:
                for (DataMeasure d: dataMeasures) {
                    if (d.getFrequency().compareTo(Frequency.R01) == 0) { l1 = 1; };
                    if (d.getFrequency().compareTo(Frequency.R03) == 0) { l5 = 1; };
                }
                break;

            case GALILEO:
                for (DataMeasure d: dataMeasures) {
                    if (d.getFrequency().compareTo(Frequency.E01) == 0) { l1 = 1; };
                    if (d.getFrequency().compareTo(Frequency.E05) == 0) { l5 = 1; };
                }
                break;
            case BEIDOU:
                for (DataMeasure d: dataMeasures) {
                    if (d.getFrequency().compareTo(Frequency.B01) == 0) { l1 = 1; };
                    if (d.getFrequency().compareTo(Frequency.B03) == 0) { l5 = 1; };
                }
                break;
            case QZSS:
                for (DataMeasure d: dataMeasures) {
                    if (d.getFrequency().compareTo(Frequency.J01) == 0) { l1 = 1; };
                    if (d.getFrequency().compareTo(Frequency.J05) == 0) { l5 = 1; };
                }
                break;
            case SBAS:
                for (DataMeasure d: dataMeasures) {
                    if (d.getFrequency().compareTo(Frequency.S01) == 0) { l1 = 1; };
                    if (d.getFrequency().compareTo(Frequency.S05) == 0) { l5 = 1; };
                }
                break;
            default:
                break;
        }
        if (l1 == 1 && l5 == 1) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Constructor for the L2L5 (L2-L5) measurement.
     * @param L list of data measure
     * @return a double corresponding to phase measurement on L2 minus the phase measurement on L5
     */
    private DataMeasure[] getL1L5(final List<DataMeasure> L) {
        final DataMeasure[] result = new DataMeasure[2];
        final SatelliteSystem satSys = L.get(0).getFrequency().getSatelliteSystem();
        switch (satSys) {
            case GPS:
                for (DataMeasure d: L) {
                    if (d.getFrequency().compareTo(Frequency.G01) == 0) { result[0] = d; };
                    if (d.getFrequency().compareTo(Frequency.G05) == 0) { result[1] = d; };
                }
                break;
            case GLONASS:
                for (DataMeasure d: L) {
                    if (d.getFrequency().compareTo(Frequency.R01) == 0) { result[0] = d; };
                    if (d.getFrequency().compareTo(Frequency.R03) == 0) { result[1] = d; };
                }
                break;
            case GALILEO:
                for (DataMeasure d: L) {
                    if (d.getFrequency().compareTo(Frequency.E01) == 0) { result[0] = d; };
                    if (d.getFrequency().compareTo(Frequency.E05) == 0) { result[1] = d; };
                }
                break;
            case BEIDOU:
                for (DataMeasure d: L) {
                    if (d.getFrequency().compareTo(Frequency.B01) == 0) { result[0] = d; };
                    if (d.getFrequency().compareTo(Frequency.B03) == 0) { result[1] = d; };
                }
                break;
            case QZSS:
                for (DataMeasure d: L) {
                    if (d.getFrequency().compareTo(Frequency.J01) == 0) { result[0] = d; };
                    if (d.getFrequency().compareTo(Frequency.J05) == 0) { result[1] = d; };
                }
                break;
            case SBAS:
                for (DataMeasure d: L) {
                    if (d.getFrequency().compareTo(Frequency.S01) == 0) { result[0] = d; };
                    if (d.getFrequency().compareTo(Frequency.S05) == 0) { result[1] = d; };
                }
                break;
            default:
                break;
        }
        return result;
    }

    /**
     * Constructor for the L2L5 (phase(L2)-phase(L5)) measurement.
     * @param dataMeasurements list of data measure
     * @return a double corresponding to phase measurement on L2 minus the phase measurement on L5
     */
    private boolean containsL2L5(final List<DataMeasure> dataMeasurements) {
        int l2 = 0;
        int l5 = 0;
        final SatelliteSystem satSys = dataMeasurements.get(0).getFrequency().getSatelliteSystem();
        switch (satSys) {
            case GPS:
                for (DataMeasure d: dataMeasurements) {
                    if (d.getFrequency().compareTo(Frequency.G02) == 0) { l2 = 1; };
                    if (d.getFrequency().compareTo(Frequency.G05) == 0) { l5 = 1; };
                }
                break;

            case GLONASS:
                for (DataMeasure d: dataMeasurements) {
                    if (d.getFrequency().compareTo(Frequency.R02) == 0) { l2 = 1; };
                    if (d.getFrequency().compareTo(Frequency.R03) == 0) { l5 = 1; };
                }
                break;

            case GALILEO:
                for (DataMeasure d: dataMeasurements) {
                    if (d.getFrequency().compareTo(Frequency.E07) == 0) { l2 = 1; };
                    if (d.getFrequency().compareTo(Frequency.E05) == 0) { l5 = 1; };
                }
                break;
            case BEIDOU:
                for (DataMeasure d: dataMeasurements)  {
                    if (d.getFrequency().compareTo(Frequency.B02) == 0) { l2 = 1; };
                    if (d.getFrequency().compareTo(Frequency.B03) == 0) { l5 = 1; };
                }
                break;
            case QZSS:
                for (DataMeasure d: dataMeasurements)  {
                    if (d.getFrequency().compareTo(Frequency.J02) == 0) { l2 = 1; };
                    if (d.getFrequency().compareTo(Frequency.J05) == 0) { l5 = 1; };
                }
                break;
            case IRNSS:
                for (DataMeasure d: dataMeasurements)  {
                    if (d.getFrequency().compareTo(Frequency.I09) == 0) { l2 = 1; };
                    if (d.getFrequency().compareTo(Frequency.I05) == 0) { l5 = 1; };
                }
                break;
            default:
                break;
        }
        if (l2 == 1 && l5 == 1) {
            return true;
        } else {
            return false;
        }

    }

    /**
     * Constructor for the L2L5 (phase(L2) - phase(L5)) measurement.
     * @param dataMeasurements list of data measure
     * @return a double corresponding to phase measurement on L2 minus the phase measurement on L5
     */
    private DataMeasure[] getL2L5(final List<DataMeasure> dataMeasurements) {
        final DataMeasure[] result = new DataMeasure[2];
        final SatelliteSystem satSys = dataMeasurements.get(0).getFrequency().getSatelliteSystem();
        switch (satSys) {
            case GPS:
                for (DataMeasure d: dataMeasurements) {
                    if (d.getFrequency().compareTo(Frequency.G02) == 0) { result[0] = d; };
                    if (d.getFrequency().compareTo(Frequency.G05) == 0) { result[1] = d; };
                }
                break;
            case GLONASS:
                for (DataMeasure d: dataMeasurements) {
                    if (d.getFrequency().compareTo(Frequency.R02) == 0) { result[0] = d; };
                    if (d.getFrequency().compareTo(Frequency.R03) == 0) { result[1] = d; };
                }
                break;
            case GALILEO:
                for (DataMeasure d: dataMeasurements)  {
                    if (d.getFrequency().compareTo(Frequency.E07) == 0) { result[0] = d; };
                    if (d.getFrequency().compareTo(Frequency.E05) == 0) { result[1] = d; };
                }
                break;
            case BEIDOU:
                for (DataMeasure d : dataMeasurements) {
                    if (d.getFrequency().compareTo(Frequency.B02) == 0) { result[0] = d; };
                    if (d.getFrequency().compareTo(Frequency.B03) == 0) { result[1] = d; };
                }
                break;
            case QZSS:
                for (DataMeasure d : dataMeasurements) {
                    if (d.getFrequency().compareTo(Frequency.J02) == 0) { result[0] = d; };
                    if (d.getFrequency().compareTo(Frequency.J05) == 0) { result[1] = d; };
                }
                break;
            case IRNSS:
                for (DataMeasure d : dataMeasurements) {
                    if (d.getFrequency().compareTo(Frequency.I09) == 0) { result[0] = d; };
                    if (d.getFrequency().compareTo(Frequency.I05) == 0) { result[1] = d; };
                }
                break;
            default:
                break;
        }
        return result;
    }

}
