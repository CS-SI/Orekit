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

import org.apache.commons.math3.util.FastMath;
import org.junit.Assert;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;

public class EGMFormatReaderTest {

    @Test
    public void testRead() throws IOException, ParseException, OrekitException {
        Utils.setDataRoot("potential");
        GravityFieldFactory.addPotentialCoefficientsReader(new EGMFormatReader("egm96_to5.ascii", false));
        PotentialCoefficientsProvider provider = GravityFieldFactory.getPotentialProvider();
        double[][] C = provider.getC(5, 5, true);
        double[][] S = provider.getS(5, 5, true);
        Assert.assertEquals(0.957254173792E-06 ,C[3][0],  0);
        Assert.assertEquals(0.174971983203E-06,C[5][5],  0);
        Assert.assertEquals(0, S[4][0],  0);
        Assert.assertEquals(0.308853169333E-06,S[4][4],  0);

        double[][] UC = provider.getC(5, 5, false);
        double a = (-0.295301647654E-06);
        double b = 9*8*7*6*5*4*3*2;
        double c = 2*11/b;
        double result = a*FastMath.sqrt(c);

        Assert.assertEquals(result,UC[5][4],  0);

        a = -0.188560802735E-06;
        b = 8*7*6*5*4*3*2;
        c=2*9/b;
        result = a*FastMath.sqrt(c);
        Assert.assertEquals(result,UC[4][4],  0);

        Assert.assertEquals(1.0826266835531513e-3, provider.getJ(false, 2)[2],0);

    }

    @Test(expected=OrekitException.class)
    public void testCorruptedFile1() throws IOException, ParseException, OrekitException {
        Utils.setDataRoot("potential");
        GravityFieldFactory.addPotentialCoefficientsReader(new EGMFormatReader("egm96_to5.corrupted-1", false));
        GravityFieldFactory.getPotentialProvider();
    }

    @Test(expected=OrekitException.class)
    public void testCorruptedFile2() throws IOException, ParseException, OrekitException {
        Utils.setDataRoot("potential");
        GravityFieldFactory.addPotentialCoefficientsReader(new EGMFormatReader("egm96_to5.corrupted-2", false));
        GravityFieldFactory.getPotentialProvider();
    }

}
