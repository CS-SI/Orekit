/* Copyright 2002-2008 CS Communication & Systèmes
 * Licensed to CS Communication & Systèmes (CS) under one or more
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
        PotentialReaderFactory factory = new PotentialReaderFactory(null, null, null, "grim5-c1.txt");
        PotentialCoefficientsProvider provider = factory.getPotentialProvider();
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
        PotentialReaderFactory factory = new PotentialReaderFactory(null, null, null, "grim5_C1.dat");
        PotentialCoefficientsProvider provider = factory.getPotentialProvider();
        double[][] C = provider.getC(5, 5, true);
        double[][] S = provider.getS(5, 5, true);

        Assert.assertEquals(0.95857491635129E-06,C[3][0],  0);
        Assert.assertEquals(0.17481512311600E-06,C[5][5],  0);
        Assert.assertEquals(0, S[4][0],  0);
        Assert.assertEquals(0.30882755318300E-06 ,S[4][4],  0);
        Assert.assertEquals(0.3986004415E+15 ,provider.getMu(),  0);
        Assert.assertEquals(0.6378136460E+07 ,provider.getAe(),  0);

    }

    @Test(expected=OrekitException.class)
    public void testCorruptedFile1() throws IOException, ParseException, OrekitException {
        Utils.setDataRoot("potential");
        PotentialReaderFactory factory = new PotentialReaderFactory(null, null, null, "grim5_corrupted1.dat");
        factory.getPotentialProvider();
    }

    @Test(expected=OrekitException.class)
    public void testCorruptedFile2() throws IOException, ParseException, OrekitException {
        Utils.setDataRoot("potential");
        PotentialReaderFactory factory = new PotentialReaderFactory(null, null, null, "grim5_corrupted2.dat");
        factory.getPotentialProvider();
    }

    @Test(expected=OrekitException.class)
    public void testCorruptedFile3() throws IOException, ParseException, OrekitException {
        Utils.setDataRoot("potential");
        PotentialReaderFactory factory = new PotentialReaderFactory(null, null, null, "grim5_corrupted3.dat");
        factory.getPotentialProvider();
    }

}
