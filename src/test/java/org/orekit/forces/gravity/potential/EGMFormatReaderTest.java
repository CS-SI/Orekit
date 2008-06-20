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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.orekit.errors.OrekitException;
import org.orekit.forces.gravity.potential.PotentialCoefficientsReader;
import org.orekit.forces.gravity.potential.PotentialReaderFactory;
import org.orekit.forces.gravity.potential.SHMFormatReader;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class EGMFormatReaderTest extends TestCase {

    public void testRead() throws OrekitException, IOException {

        InputStream in =
            EGMFormatReaderTest.class.getResourceAsStream("/potential/egm-format/egm96_to5.ascii.gz");

        PotentialReaderFactory factory = new PotentialReaderFactory();
        PotentialCoefficientsReader reader = factory.getPotentialReader(in);

        reader.read();
        double[][] C = reader.getC(5, 5, true);
        double[][] S = reader.getS(5, 5, true);
        assertEquals(0.957254173792E-06 ,C[3][0],  0);
        assertEquals(0.174971983203E-06,C[5][5],  0);
        assertEquals(0, S[4][0],  0);
        assertEquals(0.308853169333E-06,S[4][4],  0);

        double[][] UC = reader.getC(5, 5, false);
        double a = (-0.295301647654E-06);
        double b = 9*8*7*6*5*4*3*2;
        double c = 2*11/b;
        double result = a*Math.sqrt(c);

        assertEquals(result,UC[5][4],  0);

        a = -0.188560802735E-06;
        b = 8*7*6*5*4*3*2;
        c=2*9/b;
        result = a*Math.sqrt(c);
        assertEquals(result,UC[4][4],  0);

        assertEquals(1.0826266835531513e-3, reader.getJ(false, 2)[2],0);

    }

    public void testException() throws FileNotFoundException, IOException {

        PotentialReaderFactory factory = new PotentialReaderFactory();
        int c = 0;
        try {
            InputStream in =
                EGMFormatReaderTest.class.getResourceAsStream("/potential/egm-format-corrupted/fakegm1");
            factory.getPotentialReader(in);
        } catch (OrekitException e) {
            c++;
            // expected behaviour
        }
        try {
            InputStream in =
                EGMFormatReaderTest.class.getResourceAsStream("/potential/egm-format-corrupted/fakegm2");
            factory.getPotentialReader(in);
        } catch (OrekitException e) {
            c++;
            // expected behaviour
        }

        try {
            PotentialCoefficientsReader reader = new SHMFormatReader();
            reader.read();
        } catch (OrekitException e) {
            c++;
            // expected behaviour
        }

        assertEquals(3 , c);

    }

    public static Test suite() {
        return new TestSuite(EGMFormatReaderTest.class);
    }
}
