package org.orekit.estimation.measurements.filtering;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.hipparchus.stat.descriptive.DescriptiveStatistics;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.data.DataFilter;
import org.orekit.data.DataSource;
import org.orekit.files.rinex.HatanakaCompressFilter;
import org.orekit.files.rinex.observation.ObservationData;
import org.orekit.files.rinex.observation.ObservationDataSet;
import org.orekit.files.rinex.observation.RinexObservationParser;
import org.orekit.gnss.MeasurementType;
import org.orekit.gnss.ObservationType;
import org.orekit.gnss.SatInSystem;
import org.orekit.gnss.SatelliteSystem;

public class PseudoRangeFilteringTest {

    private String baseName;
    @BeforeEach
    public void setUp() {
        baseName = "src/test/resources/gnss/filtering/";
        Utils.setDataRoot("regular-data:gnss/filtering");
    }

    @Test
    public void testHatchDoppler() throws IOException {

        // Definition of the ObservationTypes to study
        ObservationType dopplerType = ObservationType.D1C;
        ObservationType rangeType = ObservationType.C1C;

        // Definition of the Satellite to study
        SatelliteSystem system = SatelliteSystem.GPS;
        int prnNumber = 1;


        String fileName = "AGGO00ARG_S_20190250000_15M_01S_MO.crx";
        File file  = new File(baseName + fileName);
        DataFilter filter = new HatanakaCompressFilter();
        DataSource nd = new DataSource(file);
        nd = filter.filter(nd);
        final RinexObservationParser parser = new RinexObservationParser();
        List<ObservationDataSet> listObsDataSet = parser.parse(nd).getObservationDataSets();
        ObservationDataSet lastObsDataSet = listObsDataSet.get(listObsDataSet.size() - 1);

        // Test reset and null condition on doppler
        ObservationData obsDataRange = new ObservationData(rangeType, 10, 0, 7);
        ObservationData obsDataDopplerNull = new ObservationData(dopplerType, Double.NaN, 0, 7);
        List<ObservationData> listObsData = new ArrayList<ObservationData>();
        listObsData.add(obsDataDopplerNull);
        listObsData.add(obsDataRange);
        ObservationDataSet obsDataSetNullDoppler = new ObservationDataSet(new SatInSystem(system, prnNumber),
                                                                          lastObsDataSet.getDate(), 0, prnNumber, listObsData);

        ObservationData obsDataRangeNull = new ObservationData(rangeType, 10, 0, 0);
        ObservationData obsDataDoppler = new ObservationData(dopplerType, Double.NaN, 0, 0);
        List<ObservationData> listObsData2 = new ArrayList<ObservationData>();
        listObsData2.add(obsDataDoppler);
        listObsData2.add(obsDataRangeNull);
        ObservationDataSet obsDataSetNullRange= new ObservationDataSet(new SatInSystem(system, prnNumber),
                                                                       lastObsDataSet.getDate(), 0, prnNumber, listObsData2);

        List<ObservationDataSet> copiedListObsDataSet = new ArrayList<>(listObsDataSet);
        copiedListObsDataSet.add(obsDataSetNullRange);
        copiedListObsDataSet.add(obsDataSetNullDoppler);

        SingleFrequencySmoother prs = new SingleFrequencySmoother(MeasurementType.DOPPLER, 100.0, 1, 50.0);
        prs.filterDataSet(copiedListObsDataSet, system, prnNumber, ObservationType.D1C);

        List<SmoothedObservationDataSet> listObsDataSetUpdate = prs.getFilteredDataMap().get(rangeType);

        double lastUpdatedValue = listObsDataSetUpdate.get(listObsDataSetUpdate.size() - 1).getSmoothedData().getValue();
        Assertions.assertEquals(2.0650729099E7, lastUpdatedValue, 1E-6);

    }

    @Test
    public void testHatchCarrierPhaseValues() throws IOException {
        /* The data used for the validation was generated following the gLAB tutorial available at :
         * https://gssc.esa.int/navipedia/GNSS_Book/ESA_GNSS-Book_TM-23_Vol_II.pdf
         * The truncated gLAB processed file is given for tests.
         * The following commands were used for generation :
         * gLAB_linux -input:cfg meas.cfg -input:obs UPC33510.08O_trunc | gawk '{if ($6==3) print $0}' > upc3.meas
            gawk '{print $4,$11-$14-3.09*($14-$16)}' upc3.meas > upc3.C1
            gawk 'BEGIN{Ts=60}{if (NR>Ts){n=Ts}else{n=NR}; C1s=$11/n+(n-1)/n*(C1s+($14-L1p));L1p=$14; print $4,C1s-$14-3.09*($14-$16)}' upc3.meas > upc3.C1S_60
            gawk 'BEGIN{Ts=60}{if (NR>Ts){n=Ts}else{n=NR};L1f=$14+3.09*($14-$16);C1s=$11/n+(n-1)/n*(C1s+(L1f-L1fp));L1fp=L1f;print $4,C1s-L1f}' upc3.meas > upc3.C1DFreeS_60
            graph.py -f upc3.C1 -s- -l "C1: PRN G03" -f upc3.C1S_60 -s.- -l "C1 smoothed" -f upc3.C1DFreeS_60 -s- -l "C1 DFree smoothed" --xn 35000 --xx 40000 --yn 17 --yx 25
         *
         * The commands were slightly modified, but not for the core formula.
         * The behaviour is graphically consistent with that of gLAB, still the difference
         * in the mean result and RMS error might be due to the prealignment step realized
         * in gLAB but not in Orekit.
         * Therefore the data will be filtered and checked against a constant value.
         *
         * This test uses observations that produces low divergence on the single frequency
         * Hatch filter.
         */

        ObservationType rangeType = ObservationType.C1;
        ObservationType phaseTypeF1 = ObservationType.L1;
        ObservationType phaseTypeF2 = ObservationType.L2;

        SatelliteSystem system = SatelliteSystem.GPS;
        int prnNumber = 3;

        String fileName = "UPC33510.08O_trunc";
        String fileName_gLAB_SF = "upc3.C1S_60";
        String fileName_gLAB_DF = "upc3.C1DFreeS_60";
        File file  = new File(baseName+fileName);

        DataSource nd = new DataSource(file);
        final RinexObservationParser parser = new RinexObservationParser();

        // Test SatelliteSystem / SNR
        List<ObservationDataSet> listObsDataSet = parser.parse(nd).getObservationDataSets();
        ObservationDataSet lastObsDataSet = listObsDataSet.get(listObsDataSet.size() - 1);

        ObservationData obsDataRange = new ObservationData(rangeType, 0, 0, 0);
        ObservationData obsDataRangeSNR = new ObservationData(rangeType, 0, 0, 1);
        ObservationData obsDataF1 = new ObservationData(phaseTypeF1, 0, 0, 0);
        ObservationData obsDataF2 = new ObservationData(phaseTypeF2, 0, 0, 0);

        List<ObservationData> listObsDataSatSystem = new ArrayList<ObservationData>();
        listObsDataSatSystem.add(obsDataF1);
        listObsDataSatSystem.add(obsDataF2);
        listObsDataSatSystem.add(obsDataRange);
        List<ObservationData> listObsDataSNR = new ArrayList<ObservationData>();
        listObsDataSNR.add(obsDataF1);
        listObsDataSNR.add(obsDataF2);
        listObsDataSNR.add(obsDataRangeSNR);
        ObservationDataSet obsDataSetRangeGLONASS = new ObservationDataSet(new SatInSystem(SatelliteSystem.GLONASS, prnNumber),
                                                                           lastObsDataSet.getDate(), 0, prnNumber, listObsDataSatSystem);

        ObservationDataSet obsDataSetRangeSNR = new ObservationDataSet(new SatInSystem(system, prnNumber),
                                                                       lastObsDataSet.getDate(), 0, prnNumber, listObsDataSNR);
        //
        List<ObservationDataSet> copiedListObsDataSet = new ArrayList<>(listObsDataSet);
        copiedListObsDataSet.add(obsDataSetRangeGLONASS);
        copiedListObsDataSet.add(obsDataSetRangeSNR);

        //
        DualFrequencySmoother prs = new DualFrequencySmoother(100.0, 60);
        prs.filterDataSet(copiedListObsDataSet, system, prnNumber, phaseTypeF1, phaseTypeF2);
        SingleFrequencySmoother prsSF = new SingleFrequencySmoother(MeasurementType.CARRIER_PHASE, 100.0, 60, 50.0);
        prsSF.filterDataSet(copiedListObsDataSet, system, prnNumber, phaseTypeF1);

        DualFrequencyHatchFilter filter = prs.getMapFilters().get(rangeType);
        SingleFrequencyHatchFilter filterSF = prsSF.getMapFilters().get(rangeType);

        ArrayList<Double> filteredSF = filterSF.getSmoothedCodeHistory();
        ArrayList<Double> filteredDF = filter.getSmoothedCodeHistory();
        ArrayList<Double> gLAB_SF = readFile(baseName + fileName_gLAB_SF);
        ArrayList<Double> gLAB_DF = readFile(baseName + fileName_gLAB_DF);

        ArrayList<Double> phaseArrayF1 = filter.getFirstFrequencyPhaseHistory();
        ArrayList<Double> phaseArrayF2 = filter.getSecondFrequencyPhaseHistory();

        ArrayList<Double> differencesSF = new ArrayList<Double>();
        ArrayList<Double> differencesDF = new ArrayList<Double>();
        DescriptiveStatistics dSF = new DescriptiveStatistics();
        DescriptiveStatistics dDF = new DescriptiveStatistics();
        for (int i = 0; i < 5000; i++) {
            double diffSF = gLAB_SF.get(i)-(filteredSF.get(i)-2.4931100000e7 - phaseArrayF1.get(i) - 3.09*(phaseArrayF1.get(i)-phaseArrayF2.get(i)));
            double diffDF = gLAB_DF.get(i)-(filteredDF.get(i)-2.4931100000e7 - phaseArrayF1.get(i) - 3.09*(phaseArrayF1.get(i)-phaseArrayF2.get(i)));
            differencesSF.add(diffSF);
            differencesDF.add(diffDF);
            dSF.addValue(diffSF);
            dDF.addValue(diffDF);
        }

        double rmsSF = dSF.getQuadraticMean();
        double rmsDF = dDF.getQuadraticMean();

        // Non-Regression test : The value is above one due to a constant bias between the 2.
        // The reason of the bias is to be explored, but might be related the pre alignement process
        // performed by gLAB.
        Assertions.assertTrue(rmsSF < 1.0063);
        Assertions.assertTrue(rmsDF < 1.0060);
    }

    @Test
    public void testHatchCarrierPhase2() throws IOException {
        ObservationType rangeType = ObservationType.C1;
        ObservationType phaseTypeF1 = ObservationType.L1;
        ObservationType phaseTypeF2 = ObservationType.L2;

        SatelliteSystem system = SatelliteSystem.GPS;
        int prnNumber = 7;

        String fileName = "irkm0440.16o";

        File file  = new File(baseName+fileName);

        DataSource nd = new DataSource(file);
        final RinexObservationParser parser = new RinexObservationParser();

        DualFrequencySmoother prs = new DualFrequencySmoother(100.0, 60);
        prs.filterDataSet(parser.parse(nd).getObservationDataSets(), system, prnNumber, phaseTypeF1, phaseTypeF2);

        DualFrequencyHatchFilter filterDF = prs.getMapFilters().get(rangeType);
        ArrayList<Double> codeDFArray = filterDF.getCodeHistory();
        ArrayList<Double> smoothedDFArray = filterDF.getSmoothedCodeHistory();

        double lastValueSmoothed = smoothedDFArray.get(smoothedDFArray.size()-1);
        double lastValueCode = codeDFArray.get(codeDFArray.size()-1);

        // Regression test
        Assertions.assertEquals(2.4715822416833777E7, lastValueSmoothed, 1e-6);
        Assertions.assertEquals(2.4715823158E7, lastValueCode, 1e-4);

        ///// Test CarrierHatchFilterSingleFrequency

        SingleFrequencySmoother prsSF = new SingleFrequencySmoother(MeasurementType.CARRIER_PHASE, 100.0, 60, 50.0);
        prsSF.filterDataSet(parser.parse(nd).getObservationDataSets(), system, prnNumber, phaseTypeF1);

        SingleFrequencyHatchFilter filterSF = prsSF.getMapFilters().get(rangeType);

        ArrayList<Double> codeSFArray = filterSF.getCodeHistory();
        ArrayList<Double> smoothedSFArray = filterSF.getSmoothedCodeHistory();

        lastValueSmoothed = smoothedSFArray.get(smoothedSFArray.size()-1);
        lastValueCode = codeSFArray.get(codeSFArray.size()-1);

        // Regression test
        Assertions.assertEquals(2.4715820677129257E7, lastValueSmoothed, 1e-6);
        Assertions.assertEquals(2.4715823158E7, lastValueCode, 1e-4);

        // Threshold test
        Assertions.assertEquals(100.0, filterDF.getThreshold(), 1e-12);

        // Test getFilteredDataMap
        List<SmoothedObservationDataSet> listObsDataSetUpdateDF = prs.getFilteredDataMap().get(rangeType);
        List<SmoothedObservationDataSet> listObsDataSetUpdateSF = prsSF.getFilteredDataMap().get(rangeType);

        double lastUpdatedValueDF = listObsDataSetUpdateDF.get(listObsDataSetUpdateDF.size() - 1).getSmoothedData().getValue();
        Assertions.assertEquals(2.4715822416833777E7, lastUpdatedValueDF, 1E-6);

        double lastUpdatedValueSF = listObsDataSetUpdateSF.get(listObsDataSetUpdateSF.size() - 1).getSmoothedData().getValue();
        Assertions.assertEquals(2.4715820677129257E7, lastUpdatedValueSF, 1E-6);

    }

    public ArrayList<Double> readFile(final String fileName) throws IOException {

        final ArrayList<Double> valueArray = new ArrayList<Double>();
        final ArrayList<Double> timeArray = new ArrayList<Double>();
        int cpt = 0;
        Path pathToFile = Paths.get(fileName);

        try (BufferedReader br = Files.newBufferedReader(pathToFile, StandardCharsets.UTF_8)) {
            String line = br.readLine();
            while (line != null) {
                String[] splitLine = line.split("\\s+");
                timeArray.add(Double.parseDouble(splitLine[0]));
                valueArray.add(Double.parseDouble(splitLine[1]));
                cpt = cpt+1;
                line = br.readLine();
            }
        }
        return valueArray;
    }

}