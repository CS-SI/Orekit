package org.orekit.estimation.measurements.filtering;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.hipparchus.stat.descriptive.DescriptiveStatistics;
import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.data.DataFilter;
import org.orekit.data.DataSource;
import org.orekit.gnss.HatanakaCompressFilter;
import org.orekit.gnss.MeasurementType;
import org.orekit.gnss.ObservationData;
import org.orekit.gnss.ObservationDataSet;
import org.orekit.gnss.ObservationType;
import org.orekit.gnss.RinexObservationLoader;
import org.orekit.gnss.SatelliteSystem;

public class PseudoRangeFilteringTest {

    @Before
    public void setUp() {
        Utils.setDataRoot("gnss/filtering");
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
        File file  = new File("src/test/resources/gnss/filtering/" + fileName);
        DataFilter filter = new HatanakaCompressFilter();
        DataSource nd = new DataSource(file);
        nd = filter.filter(nd);
        final RinexObservationLoader loader = new RinexObservationLoader(nd);

        PseudoRangeDopplerSmoother prs = new PseudoRangeDopplerSmoother(1.0,50);
        prs.filterDataSet(loader.getObservationDataSets(), system, prnNumber, ObservationType.D1C);

        List<ObservationDataSetUpdate> listObsDataSetUpdate = prs.getFilteredDataMap().get(rangeType);
        
        double lastUpdatedValue = listObsDataSetUpdate.get(listObsDataSetUpdate.size() - 1).getNewObsData().getValue();
        Assert.assertEquals(2.0650728907598175E7, lastUpdatedValue, 1E-6);
        
        // Tests for ObservationDataSetUpdate
        List<ObservationDataSet> listRinexObsDataSet = loader.getObservationDataSets();
        ObservationDataSet newObsDataSet = listObsDataSetUpdate.get(0).getObsDataSet();
        ObservationDataSet rinexObsDataSet = listRinexObsDataSet.get(0);
        
        Assert.assertEquals(newObsDataSet, rinexObsDataSet);
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
        String baseName = "src/test/resources/gnss/filtering/";
        File file  = new File(baseName+fileName);
        
        DataSource nd = new DataSource(file);
        final RinexObservationLoader loader = new RinexObservationLoader(nd);
        
        PseudoRangeDualFrequencySmoother prs = new PseudoRangeDualFrequencySmoother(60);
        prs.filterDataSet(loader.getObservationDataSets(), system, prnNumber, phaseTypeF1, phaseTypeF2);
        PseudoRangeSingleFrequencySmoother prsSF = new PseudoRangeSingleFrequencySmoother(60);
        prsSF.filterDataSet(loader.getObservationDataSets(), system, prnNumber, phaseTypeF1);
        
        CarrierPhaseHatchFilterDualFrequency filter = prs.getMapFilters().get(rangeType);
        CarrierPhaseHatchFilterSingleFrequency filterSF = prsSF.getMapFilters().get(rangeType);
        
        ArrayList<Double> filteredSF = filterSF.getSmoothedCodeHistory();
        ArrayList<Double> filteredDF = filter.getSmoothedCodeHistory();
        ArrayList<Double> gLAB_SF = readFile(baseName + fileName_gLAB_SF);
        ArrayList<Double> gLAB_DF = readFile(baseName + fileName_gLAB_DF);

        ArrayList<Double> phaseArrayF1 = filter.getPhase1History();
        ArrayList<Double> phaseArrayF2 = filter.getPhase2History();
        
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
        
        // Regression test : The value is above one due to a constant bias between the 2.
        // The reason of the bias is to be explored, but might be related the pre alignement process
        // performed by gLAB.
        Assert.assertTrue(rmsSF < 1.0063);
        Assert.assertTrue(rmsDF < 1.0060);
    }
    
    @Test
    public void testHatchCarrierPhase2() throws IOException {
        ObservationType rangeType = ObservationType.C1;
        ObservationType phaseTypeF1 = ObservationType.L1;
        ObservationType phaseTypeF2 = ObservationType.L2;
        
        SatelliteSystem system = SatelliteSystem.GPS;
        int prnNumber = 7;
        
        String fileName = "irkm0440.16o";

        String baseName = "src/test/resources/gnss/filtering/";
        File file  = new File(baseName+fileName);
        
        DataSource nd = new DataSource(file);
        final RinexObservationLoader loader = new RinexObservationLoader(nd);
        
        PseudoRangeDualFrequencySmoother prs = new PseudoRangeDualFrequencySmoother(60);
        prs.filterDataSet(loader.getObservationDataSets(), system, prnNumber, phaseTypeF1, phaseTypeF2);
        
        CarrierPhaseHatchFilterDualFrequency filterDF = prs.getMapFilters().get(rangeType);
        ArrayList<Double> codeDFArray = filterDF.getCodeHistory();
        ArrayList<Double> smoothedDFArray = filterDF.getSmoothedCodeHistory();
        
        double lastValueSmoothed = smoothedDFArray.get(smoothedDFArray.size()-1);
        double lastValueCode = codeDFArray.get(codeDFArray.size()-1);
        
        // Regression test
        Assert.assertEquals(2.4715822416833777E7, lastValueSmoothed, 1e-6);
        Assert.assertEquals(2.4715823158E7, lastValueCode, 1e-4);
        
        // Test getObservationType
        ObservationType obsType1 = filterDF.getObsTypePhaseF1();
        ObservationType obsType2 = filterDF.getObsTypePhaseF2();
        Assert.assertEquals(phaseTypeF1, obsType1);
        Assert.assertEquals(phaseTypeF2, obsType2);
        
        ///// Test CarrierHatchFilterSingleFrequency
        
        PseudoRangeSingleFrequencySmoother prsSF = new PseudoRangeSingleFrequencySmoother(60);
        prsSF.filterDataSet(loader.getObservationDataSets(), system, prnNumber, phaseTypeF1);
        
        CarrierPhaseHatchFilterSingleFrequency filterSF = prsSF.getMapFilters().get(rangeType);

        ArrayList<Double> codeSFArray = filterSF.getCodeHistory();
        ArrayList<Double> smoothedSFArray = filterSF.getSmoothedCodeHistory();
        
        lastValueSmoothed = smoothedSFArray.get(smoothedSFArray.size()-1);
        lastValueCode = codeSFArray.get(codeSFArray.size()-1);
        
        // Regression test
        Assert.assertEquals(2.4715820677129257E7, lastValueSmoothed, 1e-6);
        Assert.assertEquals(2.4715823158E7, lastValueCode, 1e-4);
        
        // Threshold test
        filterDF.setThreshold(1);
        Assert.assertEquals(1, filterDF.getThreshold(), 1e-12);
     
        // Test getFilteredDataMap
        List<ObservationDataSetUpdate> listObsDataSetUpdateDF = prs.getFilteredDataMap().get(rangeType);
        List<ObservationDataSetUpdate> listObsDataSetUpdateSF = prsSF.getFilteredDataMap().get(rangeType);
        
        double lastUpdatedValueDF = listObsDataSetUpdateDF.get(listObsDataSetUpdateDF.size() - 1).getNewObsData().getValue();
        Assert.assertEquals(2.4715822416833777E7, lastUpdatedValueDF, 1E-6);
        
        double lastUpdatedValueSF = listObsDataSetUpdateSF.get(listObsDataSetUpdateSF.size() - 1).getNewObsData().getValue();
        Assert.assertEquals(2.4715820677129257E7, lastUpdatedValueSF, 1E-6);
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
    
    
    // Garbage 
    
//  for (final ObservationDataSet observationDataSet : loader.getObservationDataSets()) {
//  if (observationDataSet.getSatelliteSystem() == system) {
//      gpsSet.add(observationDataSet.getPrnNumber());
//  }
//  for (final ObservationData obsData : observationDataSet.getObservationData()) {
//      if (obsData.getObservationType() == dopplerType 
//              && observationDataSet.getSatelliteSystem() == system
//              && observationDataSet.getPrnNumber() == prnNumber) {
//          listDoppler.add(obsData.getValue());
//      }
//      if (obsData.getObservationType() == rangeType 
//              && observationDataSet.getSatelliteSystem() == system
//              && observationDataSet.getPrnNumber() == prnNumber) {
//          listRange.add(obsData.getValue());
//      }
//      if (obsData.getObservationType() == phaseTypeF1 
//              && observationDataSet.getSatelliteSystem() == system
//              && observationDataSet.getPrnNumber() == prnNumber) {
//          listPhaseF1.add(obsData.getValue());
//      }
//      if (obsData.getObservationType() == phaseTypeF2 
//              && observationDataSet.getSatelliteSystem() == system
//              && observationDataSet.getPrnNumber() == prnNumber) {
//          listPhaseF2.add(obsData.getValue());
//      }
//      if (obsData.getObservationType().getMeasurementType() == MeasurementType.CARRIER_PHASE
//              && observationDataSet.getSatelliteSystem() == system
//              && observationDataSet.getPrnNumber() == prnNumber
//              && !(Double.isNaN(obsData.getValue()))) {
//          phaseSet.add(obsData.getObservationType().toString());
//      }
//  }
//}
//String baseName = "src/test/resources/gnss/filtering/hatch_test/";
//writeListToFile(listDoppler, baseName+"Doppler.txt");
//writeListToFile(listRange, baseName+"Range.txt");
//writeListToFile(listPhaseF1, baseName+"phaseF1.txt");
//writeListToFile(listPhaseF2, baseName+"phaseF2.txt");
//  System.out.println(gpsSet);
//  System.out.println(phaseSet);
//  System.out.println(phaseTypeF1.getFrequency(system).getWavelength());
//  System.out.println(phaseTypeF2.getFrequency(system).getWavelength());
}