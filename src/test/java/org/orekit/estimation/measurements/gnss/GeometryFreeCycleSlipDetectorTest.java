package org.orekit.estimation.measurements.gnss;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.data.DataFilter;
import org.orekit.data.GzipFilter;
import org.orekit.data.NamedData;
import org.orekit.data.UnixCompressFilter;
import org.orekit.gnss.Frequency;
import org.orekit.gnss.HatanakaCompressFilter;
import org.orekit.gnss.ObservationDataSet;
import org.orekit.gnss.RinexLoader;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;

public class GeometryFreeCycleSlipDetectorTest {
    
    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }
    
    @Test
    public void testTheBasicData() throws URISyntaxException, IOException {
        
        final String inputPath = GeometryFreeCycleSlipDetectorTest.class.getClassLoader().getResource("gnss/cycleSlip/shld0440.16d.Z").toURI().getPath();
        final File input  = new File(inputPath);
        String fileName = "shld0440.16d.Z";
        NamedData nd = new NamedData(fileName,
                                     () -> new FileInputStream(new File(input.getParentFile(), fileName)));
        for (final DataFilter filter : Arrays.asList(new GzipFilter(),
                                                     new UnixCompressFilter(),
                                                     new HatanakaCompressFilter())) {
            nd = filter.filter(nd);
        }
        final RinexLoader loader = new RinexLoader(nd.getStreamOpener().openStream(), nd.getName());
        //RinexLoader  loader = loadCompressed("cycleSlip/shld0440.16d.Z");
        final List<ObservationDataSet> obserDataSets = loader.getObservationDataSets();
        GeometryFreeCycleSlipDetector slipDetectors =
            new GeometryFreeCycleSlipDetector(obserDataSets, 31, 0.0, 10, 5, 1);
        for(CycleSlipDetectorResults d: slipDetectors.getResults()) {
            switch(getPrn(d)) {
    
                case 1: 
                    Assert.assertEquals(19.0, d.getEndDate(Frequency.G01).durationFrom(new AbsoluteDate(2016,  2, 13,  5,  0,  0.0000000, TimeScalesFactory.getTAI())),1e-9);
                    Assert.assertEquals(19.0, d.getBeginDate(Frequency.G01).durationFrom(new AbsoluteDate(2016,2, 13  ,2  ,8 ,30.0000000, TimeScalesFactory.getTAI())),1e-9);
                    break;
                case 5: 
                    Assert.assertEquals(19.0, d.getEndDate(Frequency.G01).durationFrom(new AbsoluteDate(2016,  2, 13,  2, 48, 30.0000000, TimeScalesFactory.getTAI())),1e-9);
                    Assert.assertEquals(19.0, d.getBeginDate(Frequency.G01).durationFrom(new AbsoluteDate(2016,2, 13  ,2  ,8 ,30.0000000, TimeScalesFactory.getTAI())),1e-9);
                    break;
                        
                case 6: 
                    Assert.assertEquals(19.0, d.getEndDate(Frequency.G01).durationFrom(new AbsoluteDate(2016, 2, 13,  5,  0,  0.0000000, TimeScalesFactory.getTAI())),1e-9);
                    Assert.assertEquals(19.0, d.getBeginDate(Frequency.G01).durationFrom(new AbsoluteDate(2016,  2, 13,  4, 15,  0.0000000, TimeScalesFactory.getTAI())),1e-9); 
                    break;
               
                case 7: 
                    Assert.assertEquals(19.0, d.getEndDate(Frequency.G01).durationFrom(new AbsoluteDate(2016,  2, 13,  4, 27,  30.0000000, TimeScalesFactory.getTAI())),1e-9);
                    Assert.assertEquals(19.0, d.getBeginDate(Frequency.G01).durationFrom(new AbsoluteDate(2016,2, 13  ,2  ,8 ,30.0000000, TimeScalesFactory.getTAI())),1e-9);
                    break;
                        
                case 9: 
                    Assert.assertEquals(19.0, d.getEndDate(Frequency.G01).durationFrom(new AbsoluteDate(2016, 2, 13,  2, 45,  30.0000000, TimeScalesFactory.getTAI())),1e-9);
                    Assert.assertEquals(19.0, d.getBeginDate(Frequency.G01).durationFrom(new AbsoluteDate(2016,2, 13  ,2  ,8 ,30.0000000, TimeScalesFactory.getTAI())),1e-9);
                    break;
                            
                case 11:
                    Assert.assertEquals(19.0, d.getEndDate(Frequency.G01).durationFrom(new AbsoluteDate(2016,  2, 13,  5,  0,  0.0000000, TimeScalesFactory.getTAI())),1e-9);
                    Assert.assertEquals(19.0, d.getBeginDate(Frequency.G01).durationFrom(new AbsoluteDate(2016,2, 13  ,2  ,8 ,30.0000000, TimeScalesFactory.getTAI())),1e-9);
                    break; 

                default:   break;
            }   
        }
    }
    
    @Test
    public void testTimeCycleSlip() throws URISyntaxException, IOException {
        final String inputPath = GeometryFreeCycleSlipDetectorTest.class.getClassLoader().getResource("gnss/cycleSlip/WithCycleSlip.16o").toURI().getPath();
        final File input  = new File(inputPath);
        String fileName = "WithCycleSlip.16o";
        NamedData nd = new NamedData(fileName,
                                     () -> new FileInputStream(new File(input.getParentFile(), fileName)));
        for (final DataFilter filter : Arrays.asList(new GzipFilter(),
                                                     new UnixCompressFilter(),
                                                     new HatanakaCompressFilter())) {
            nd = filter.filter(nd);
        }
        final RinexLoader loader = new RinexLoader(nd.getStreamOpener().openStream(), nd.getName());
        final List<ObservationDataSet>  obserDataSets = loader.getObservationDataSets();
        //With dt = 31 s, cycle slip should not exist, a very huge threshold is used to not detect cycle-slip
        GeometryFreeCycleSlipDetector slipDetectors =
            new GeometryFreeCycleSlipDetector(obserDataSets, 31, 0.0,10, 60, 1e9);    
        for(CycleSlipDetectorResults d: slipDetectors.getResults()) {
            Assert.assertFalse(d.getCycleSlipMap().get(Frequency.G01).isEmpty());
        }
        //With dt = 29 s, a cycle-slip should occur at each new measurement (97 times)
        GeometryFreeCycleSlipDetector slipDetectors2 =
                        new GeometryFreeCycleSlipDetector(obserDataSets, 29, 0.0,10, 60, 1e9);
        for(CycleSlipDetectorResults d: slipDetectors2.getResults()) {
            Assert.assertTrue(d.getCycleSlipMap().get(Frequency.G01).size() == 97);
        }
    }
    
    @Test
    public void testCycleSlip() throws URISyntaxException, IOException {
        final String inputPath = GeometryFreeCycleSlipDetectorTest.class.getClassLoader().getResource("gnss/cycleSlip/WithCycleSlip.16o").toURI().getPath();
        final File input  = new File(inputPath);
        String fileName = "WithCycleSlip.16o";
        NamedData nd = new NamedData(fileName,
                                     () -> new FileInputStream(new File(input.getParentFile(), fileName)));
        for (final DataFilter filter : Arrays.asList(new GzipFilter(),
                                                     new UnixCompressFilter(),
                                                     new HatanakaCompressFilter())) {
            nd = filter.filter(nd);
        }
        final RinexLoader loader = new RinexLoader(nd.getStreamOpener().openStream(), nd.getName());
        final List<ObservationDataSet> obserDataSets = loader.getObservationDataSets();
        //With dt = 31 s, cycle slip for time gap cannot be detected (see previous test).
        //We use T0 = 60s for threshold time constant as advice from Navipedia page.
        GeometryFreeCycleSlipDetector slipDetectors =
            new GeometryFreeCycleSlipDetector(obserDataSets,31, 0.0, 9, 60, 1000); 
        //According to excel graph, cycle-slip occur at 1 h 59m 43s
        AbsoluteDate trueDate = new AbsoluteDate(2016, 02, 13, 1, 59, 43, TimeScalesFactory.getUTC());
        final int size = slipDetectors.getResults().get(0).getCycleSlipMap().get(Frequency.G01).size();
        Assert.assertEquals(1, size);
        final AbsoluteDate computedDate = slipDetectors.getResults().get(0).getCycleSlipMap().get(Frequency.G01).get(0);
        Assert.assertEquals(0.0, trueDate.durationFrom(computedDate),  1e-9);
   }
    
    private int getPrn(final CycleSlipDetectorResults d) {
        
        if(d.getSatelliteName().substring(6).compareTo("1")==0) {return 1;};
        if(d.getSatelliteName().substring(6).compareTo("2")==0) {return 2;};
        if(d.getSatelliteName().substring(6).compareTo("3")==0) {return 3;};
        if(d.getSatelliteName().substring(6).compareTo("4")==0) {return 4;};
        if(d.getSatelliteName().substring(6).compareTo("5")==0) {return 5;};
        if(d.getSatelliteName().substring(6).compareTo("6")==0) {return 6;};
        if(d.getSatelliteName().substring(6).compareTo("7")==0) {return 7;};
        if(d.getSatelliteName().substring(6).compareTo("8")==0) {return 8;};
        if(d.getSatelliteName().substring(6).compareTo("9")==0) {return 9;};
        if(d.getSatelliteName().substring(6).compareTo("10")==0) {return 10;};
        if(d.getSatelliteName().substring(6).compareTo("11")==0) {return 11;};
        if(d.getSatelliteName().substring(6).compareTo("12")==0) {return 12;};
        if(d.getSatelliteName().substring(6).compareTo("13")==0) {return 13;};
        if(d.getSatelliteName().substring(6).compareTo("14")==0) {return 14;};
        if(d.getSatelliteName().substring(6).compareTo("15")==0) {return 15;};
        if(d.getSatelliteName().substring(6).compareTo("16")==0) {return 16;};
        if(d.getSatelliteName().substring(6).compareTo("17")==0) {return 17;};
        if(d.getSatelliteName().substring(6).compareTo("18")==0) {return 18;};
        if(d.getSatelliteName().substring(6).compareTo("19")==0) {return 19;};
        if(d.getSatelliteName().substring(6).compareTo("20")==0) {return 20;};
        if(d.getSatelliteName().substring(6).compareTo("21")==0) {return 21;};
        if(d.getSatelliteName().substring(6).compareTo("22")==0) {return 22;};
        if(d.getSatelliteName().substring(6).compareTo("23")==0) {return 23;};
        if(d.getSatelliteName().substring(6).compareTo("24")==0) {return 24;};
        if(d.getSatelliteName().substring(6).compareTo("25")==0) {return 25;};
        if(d.getSatelliteName().substring(6).compareTo("26")==0) {return 26;};
        if(d.getSatelliteName().substring(6).compareTo("27")==0) {return 27;};
        if(d.getSatelliteName().substring(6).compareTo("28")==0) {return 28;};
        if(d.getSatelliteName().substring(6).compareTo("29")==0) {return 29;};
        if(d.getSatelliteName().substring(6).compareTo("30")==0) {return 30;};
        if(d.getSatelliteName().substring(6).compareTo("31")==0) {return 31;} else {return 32;}
              
    }

}
