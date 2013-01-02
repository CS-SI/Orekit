/* Copyright 2002-2012 CS Systèmes d'Information
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
package org.orekit.forces.gravity.potential;


import java.io.IOException;
import java.text.ParseException;

import org.junit.Assert;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;

public class GRGSFormatReaderTest {

    @Test
    public void testAdditionalColumn() throws IOException, ParseException, OrekitException {
        Utils.setDataRoot("potential");
        GravityFieldFactory.addPotentialCoefficientsReader(new GRGSFormatReader("grim5-c1.txt", true));
        PotentialCoefficientsProvider provider = GravityFieldFactory.getPotentialProvider(5, 5);
        double[][] C = provider.getC(5, 5, true);
        double[][] S = provider.getS(5, 5, true);

        Assert.assertEquals(0.95857491635129E-06,C[3][0],  0);
        Assert.assertEquals(0.17481512311600E-06,C[5][5],  0);
        Assert.assertEquals(0, S[4][0],  0);
        Assert.assertEquals(0.30882755318300E-06 ,S[4][4],  0);
        Assert.assertEquals(0.3986004415E+15 ,provider.getMu(),  0);
        Assert.assertEquals(0.6378136460E+07 ,provider.getAe(),  0);

    }

    @Test
    public void testRegular05c() throws IOException, ParseException, OrekitException {
        Utils.setDataRoot("potential");
        GravityFieldFactory.addPotentialCoefficientsReader(new GRGSFormatReader("grim5_C1.dat", true));
        PotentialCoefficientsProvider provider = GravityFieldFactory.getPotentialProvider(5, 5);
        double[][] C = provider.getC(5, 5, true);
        double[][] S = provider.getS(5, 5, true);

        Assert.assertEquals(0.95857491635129E-06,C[3][0],  0);
        Assert.assertEquals(0.17481512311600E-06,C[5][5],  0);
        Assert.assertEquals(0, S[4][0],  0);
        Assert.assertEquals(0.30882755318300E-06 ,S[4][4],  0);
        Assert.assertEquals(0.3986004415E+15 ,provider.getMu(),  0);
        Assert.assertEquals(0.6378136460E+07 ,provider.getAe(),  0);

    }

    @Test
    public void testReadLimits() throws IOException, ParseException, OrekitException {
        Utils.setDataRoot("potential");
        GravityFieldFactory.addPotentialCoefficientsReader(new GRGSFormatReader("grim5_C1.dat", true));
        PotentialCoefficientsProvider provider = GravityFieldFactory.getPotentialProvider(3, 2);
        try {
            provider.getC(3, 3, true);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            // expected
        } catch (Exception e) {
            Assert.fail("wrong exception caught: " + e.getLocalizedMessage());
        }
        try {
            provider.getC(4, 2, true);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            // expected
        } catch (Exception e) {
            Assert.fail("wrong exception caught: " + e.getLocalizedMessage());
        }
        double[][] c = provider.getC(3,  2, true);
        Assert.assertEquals(4, c.length);
        Assert.assertEquals(2, c[1].length);
        Assert.assertEquals(3, c[2].length);
        Assert.assertEquals(3, c[3].length);
    }

    @Test(expected=OrekitException.class)
    public void testCorruptedFile1() throws IOException, ParseException, OrekitException {
        Utils.setDataRoot("potential");
        GravityFieldFactory.addPotentialCoefficientsReader(new GRGSFormatReader("grim5_corrupted1.dat", false));
        GravityFieldFactory.getPotentialProvider(5, 5);
    }

    @Test(expected=OrekitException.class)
    public void testCorruptedFile2() throws IOException, ParseException, OrekitException {
        Utils.setDataRoot("potential");
        GravityFieldFactory.addPotentialCoefficientsReader(new GRGSFormatReader("grim5_corrupted2.dat", false));
        GravityFieldFactory.getPotentialProvider(5, 5);
    }

    @Test(expected=OrekitException.class)
    public void testCorruptedFile3() throws IOException, ParseException, OrekitException {
        Utils.setDataRoot("potential");
        GravityFieldFactory.addPotentialCoefficientsReader(new GRGSFormatReader("grim5_corrupted3.dat", false));
        GravityFieldFactory.getPotentialProvider(5, 5);
    }

}
