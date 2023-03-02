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
package org.orekit.frames;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;


public class PredictedEOPHistoryTest {

    @Test
    public void testExtensionDates() {

        // load only annual EOPC04 files
        FramesFactory.clearEOPHistoryLoaders();
        FramesFactory.addDefaultEOP2000HistoryLoaders("none", "none", "^eopc04_08_IAU2000\\.0[345]$", "none", "none");
        final TimeScale  utc = TimeScalesFactory.getUTC();
        final EOPHistory raw = FramesFactory.getEOPHistory(IERSConventions.IERS_2010, true);

        // raw history over 3 years
        Assertions.assertEquals(0.0,
                                new AbsoluteDate(2003, 1, 1, 0, 0, 0.0, utc).durationFrom(raw.getStartDate()),
                                1.0e-10);
        Assertions.assertEquals(0.0,
                                new AbsoluteDate(2005, 12, 31, 0, 0, 0.0, utc).durationFrom(raw.getEndDate()),
                                1.0e-10);

        // predicted history extended one month
        EOPHistory predicted = new PredictedEOPHistory(raw,
                                                       30 * Constants.JULIAN_DAY,
                                                       EOPFitter.createDefaultDut1Fitter(),
                                                       EOPFitter.createDefaultPoleFitter(),
                                                       EOPFitter.createDefaultPoleFitter(),
                                                       EOPFitter.createDefaultNutationFitter(),
                                                       EOPFitter.createDefaultNutationFitter());
        Assertions.assertEquals(0.0,
                                new AbsoluteDate(2003, 1, 1, 0, 0, 0.0, utc).durationFrom(predicted.getStartDate()),
                                1.0e-10);
        Assertions.assertEquals(0.0,
                                new AbsoluteDate(2006, 1, 29, 23, 59, 59.0, utc).durationFrom(predicted.getEndDate()),
                                1.0e-10);
    }

    @Test
    public void testCommonCoverage() {

        // load only annual EOPC04 files
        FramesFactory.clearEOPHistoryLoaders();
        FramesFactory.addDefaultEOP2000HistoryLoaders("none", "none", "^eopc04_08_IAU2000\\.0[345]$", "none", "none");
        final EOPHistory raw = FramesFactory.getEOPHistory(IERSConventions.IERS_2010, true);

        EOPHistory predicted = new PredictedEOPHistory(raw,
                                                       30 * Constants.JULIAN_DAY,
                                                       EOPFitter.createDefaultDut1Fitter(),
                                                       EOPFitter.createDefaultPoleFitter(),
                                                       EOPFitter.createDefaultPoleFitter(),
                                                       EOPFitter.createDefaultNutationFitter(),
                                                       EOPFitter.createDefaultNutationFitter());

        // check we get the same value as raw EOP (dropping the last interpolated day)
        double maxErrorUT1  = 0;
        double maxErrorLOD  = 0;
        double maxErrorXp   = 0;
        double maxErrorYp   = 0;
        double maxErrordPsi = 0;
        double maxErrordEps = 0;
        double maxErrordx   = 0;
        double maxErrordy   = 0;
        for (double dt = 0;
             dt < raw.getEndDate().durationFrom(raw.getStartDate()) - Constants.JULIAN_DAY;
             dt += 20000.0) {
            final AbsoluteDate   date      = raw.getStartDate().shiftedBy(dt);
            final PoleCorrection rawPC     = raw.getPoleCorrection(date);
            final PoleCorrection predPC    = predicted.getPoleCorrection(date);
            final double[]       rawENC    = raw.getEquinoxNutationCorrection(date);
            final double[]       predENC   = predicted.getEquinoxNutationCorrection(date);
            final double[]       rawNroNC  = raw.getNonRotatinOriginNutationCorrection(date);
            final double[]       predNroNC = predicted.getNonRotatinOriginNutationCorrection(date);
            maxErrorUT1  = FastMath.max(maxErrorUT1,  raw.getUT1MinusUTC(date) - predicted.getUT1MinusUTC(date));
            maxErrorLOD  = FastMath.max(maxErrorLOD,  raw.getLOD(date)         - predicted.getLOD(date));
            maxErrorXp   = FastMath.max(maxErrorXp,   rawPC.getXp()            - predPC.getXp());
            maxErrorYp   = FastMath.max(maxErrorYp,   rawPC.getYp()            - predPC.getYp());
            maxErrordPsi = FastMath.max(maxErrordPsi, rawENC[0]                - predENC[0]);
            maxErrordEps = FastMath.max(maxErrordEps, rawENC[1]                - predENC[1]);
            maxErrordx   = FastMath.max(maxErrordx,   rawNroNC[0]              - predNroNC[0]);
            maxErrordy   = FastMath.max(maxErrordy,   rawNroNC[1]              - predNroNC[1]);
        }
        Assertions.assertEquals(0.0, maxErrorUT1,  1.0e-15);
        Assertions.assertEquals(0.0, maxErrorLOD,  1.0e-15);
        Assertions.assertEquals(0.0, maxErrorXp,   1.0e-15);
        Assertions.assertEquals(0.0, maxErrorYp,   1.0e-15);
        Assertions.assertEquals(0.0, maxErrordPsi, 1.0e-15);
        Assertions.assertEquals(0.0, maxErrordEps, 1.0e-15);
        Assertions.assertEquals(0.0, maxErrordx,   1.0e-15);
        Assertions.assertEquals(0.0, maxErrordy,   1.0e-15);
    }

    @Test
    public void testAccuracy() {

        // load files from 2003 to 2004
        FramesFactory.clearEOPHistoryLoaders();
        FramesFactory.addDefaultEOP2000HistoryLoaders("none", "none", "^eopc04_08_IAU2000\\.0[34]$", "none", "none");
        final EOPHistory raw2003To2004 = FramesFactory.getEOPHistory(IERSConventions.IERS_2010, true);
        EOPEntry last2004 = raw2003To2004.getEntries().get(raw2003To2004.getEntries().size() - 1);
        Assertions.assertEquals(53370, last2004.getMjd());

        // load files from 2003, 2004 and 2005
        FramesFactory.clearEOPHistoryLoaders();
        FramesFactory.addDefaultEOP2000HistoryLoaders("none", "none", "^eopc04_08_IAU2000\\.0[345]$", "none", "none");
        final EOPHistory trueEOP = FramesFactory.getEOPHistory(IERSConventions.IERS_2010, true);
        EOPEntry last2005 = trueEOP.getEntries().get(trueEOP.getEntries().size() - 1);
        Assertions.assertEquals(53735, last2005.getMjd());

        // set up a prediction for January 2005, based on a fitting over 2004
        PredictedEOPHistory predictedEOP = new PredictedEOPHistory(raw2003To2004,
                                                                   180 * Constants.JULIAN_DAY,
                                                                   EOPFitter.createDefaultDut1Fitter(),
                                                                   EOPFitter.createDefaultPoleFitter(),
                                                                   EOPFitter.createDefaultPoleFitter(),
                                                                   EOPFitter.createDefaultNutationFitter(),
                                                                   EOPFitter.createDefaultNutationFitter());

        // set up two itrf frames, one using true, one using predicted EOP
        final Frame itrfTrue = FramesFactory.buildUncachedITRF(trueEOP, TimeScalesFactory.getUTC());
        final Frame itrfPred = FramesFactory.buildUncachedITRF(predictedEOP, TimeScalesFactory.getUTC());

        double maxError007Days = 0;
        double maxError030Days = 0;
        double maxError060Days = 0;
        double maxError090Days = 0;
        double maxError120Days = 0;
        double maxError150Days = 0;
        double maxError180Days = 0;
        for (double dt = 0; dt < 180 * Constants.JULIAN_DAY; dt += 7200) {
            final AbsoluteDate date = last2004.getDate().shiftedBy(dt);
            final Transform t = itrfTrue.getTransformTo(itrfPred, date);
            final double deltaP = t.getRotation().getAngle() * Constants.WGS84_EARTH_EQUATORIAL_RADIUS;
            if (dt <= 7 * Constants.JULIAN_DAY) {
                maxError007Days = FastMath.max(maxError007Days, deltaP);
            }
            if (dt <= 30 * Constants.JULIAN_DAY) {
                maxError030Days = FastMath.max(maxError030Days, deltaP);
            }
            if (dt <= 60 * Constants.JULIAN_DAY) {
                maxError060Days = FastMath.max(maxError060Days, deltaP);
            }
            if (dt <= 90 * Constants.JULIAN_DAY) {
                maxError090Days = FastMath.max(maxError090Days, deltaP);
            }
            if (dt <= 120 * Constants.JULIAN_DAY) {
                maxError120Days = FastMath.max(maxError120Days, deltaP);
            }
            if (dt <= 150 * Constants.JULIAN_DAY) {
                maxError150Days = FastMath.max(maxError150Days, deltaP);
            }
            if (dt <= 180 * Constants.JULIAN_DAY) {
                maxError180Days = FastMath.max(maxError180Days, deltaP);
            }
        }

        Assertions.assertEquals(0.776, maxError007Days, 0.001);
        Assertions.assertEquals(1.737, maxError030Days, 0.001);
        Assertions.assertEquals(4.080, maxError060Days, 0.001);
        Assertions.assertEquals(6.832, maxError090Days, 0.001);
        Assertions.assertEquals(6.832, maxError120Days, 0.001);
        Assertions.assertEquals(7.187, maxError150Days, 0.001);
        Assertions.assertEquals(9.000, maxError180Days, 0.001);
    }

    @Test
    public void testSerialization() throws IOException, ClassNotFoundException {
        // load only annual EOPC04 files
        FramesFactory.clearEOPHistoryLoaders();
        FramesFactory.addDefaultEOP2000HistoryLoaders("none", "none", "^eopc04_08_IAU2000\\.0[345]$", "none", "none");
        final EOPHistory raw = FramesFactory.getEOPHistory(IERSConventions.IERS_2010, true);

        PredictedEOPHistory predicted = new PredictedEOPHistory(raw,
        		                                                30 * Constants.JULIAN_DAY,
        		                                                EOPFitter.createDefaultDut1Fitter(),
        		                                                EOPFitter.createDefaultPoleFitter(),
        		                                                EOPFitter.createDefaultPoleFitter(),
        		                                                EOPFitter.createDefaultNutationFitter(),
        		                                                EOPFitter.createDefaultNutationFitter());

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream    oos = new ObjectOutputStream(bos);
        oos.writeObject(predicted);

        Assertions.assertTrue(bos.size() > 110000);
        Assertions.assertTrue(bos.size() < 120000);

        ByteArrayInputStream  bis = new ByteArrayInputStream(bos.toByteArray());
        ObjectInputStream     ois = new ObjectInputStream(bis);
        PredictedEOPHistory deserialized  = (PredictedEOPHistory) ois.readObject();
        Assertions.assertEquals(predicted.getStartDate(), deserialized.getStartDate());
        Assertions.assertEquals(predicted.getEndDate(), deserialized.getEndDate());
        Assertions.assertEquals(predicted.getEntries().size(), deserialized.getEntries().size());
        for (int i = 0; i < predicted.getEntries().size(); ++i) {
            EOPEntry e1 = predicted.getEntries().get(i);
            EOPEntry e2 = deserialized.getEntries().get(i);
            Assertions.assertEquals(e1.getMjd(),         e2.getMjd());
            Assertions.assertEquals(e1.getDate(),        e2.getDate());
            Assertions.assertEquals(e1.getUT1MinusUTC(), e2.getUT1MinusUTC(), 1.0e-10);
            Assertions.assertEquals(e1.getLOD(),         e2.getLOD(),         1.0e-10);
            Assertions.assertEquals(e1.getDdEps(),       e2.getDdEps(),       1.0e-10);
            Assertions.assertEquals(e1.getDdPsi(),       e2.getDdPsi(),       1.0e-10);
            Assertions.assertEquals(e1.getDx(),          e2.getDx(),          1.0e-10);
            Assertions.assertEquals(e1.getDy(),          e2.getDy(),          1.0e-10);
            Assertions.assertEquals(e1.getX(),           e2.getX(),           1.0e-10);
            Assertions.assertEquals(e1.getY(),           e2.getY(),           1.0e-10);
        }

    }

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

}
