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

public class SHMFormatReaderTest extends TestCase {

    public void testRead() throws OrekitException, IOException {

        InputStream in =
            SHMFormatReaderTest.class.getResourceAsStream("/potential/shm-format/g003_eigen-cg01c_coef");
        PotentialReaderFactory factory = new PotentialReaderFactory();
        PotentialCoefficientsReader reader = factory.getPotentialReader(in);
        reader.read();
        double[][] C = reader.getC(5, 5, true);
        double[][] S = reader.getS(5, 5, true);

        assertEquals(0.957187536534E-06,C[3][0],  0);
        assertEquals(0.174787189024E-06,C[5][5],  0);
        assertEquals(0, S[4][0],  0);
        assertEquals(0.308834848269E-06 ,S[4][4],  0);
        assertEquals(0.3986004415E+15 ,reader.getMu(),  0);
        assertEquals(0.6378136460E+07 ,reader.getAe(),  0);

        in =
            SHMFormatReaderTest.class.getResourceAsStream("/potential/shm-format/eigen_cg03c_coef");
        reader = factory.getPotentialReader(in);
        reader.read();
        C = reader.getC(5, 5, true);;
        S = reader.getS(5, 5, true);

        assertEquals(0.957201462136E-06,C[3][0],  0);
        assertEquals(0.174786174485E-06,C[5][5],  0);
        assertEquals(0, S[4][0],  0);
        assertEquals(0.308834784975E-06 ,S[4][4],  0);
        assertEquals(0.3986004415E+15 ,reader.getMu(),  0);
        assertEquals(0.6378136460E+07 ,reader.getAe(),  0);

    }

    public void testReadCompressed() throws OrekitException, IOException {
        InputStream in =
            SHMFormatReaderTest.class.getResourceAsStream("/potential/shm-format-compressed/eigen-cg01c_coef.gz");
        PotentialReaderFactory factory = new PotentialReaderFactory();
        PotentialCoefficientsReader reader = factory.getPotentialReader(in);
        reader.read();
        double[][] C = reader.getC(5, 5, true);;
        double[][] S = reader.getS(5, 5, true);;

        assertEquals(0.957187536534E-06,C[3][0],  0);
        assertEquals(0.174787189024E-06,C[5][5],  0);
        assertEquals(0, S[4][0],  0);
        assertEquals(0.308834848269E-06 ,S[4][4],  0);
        assertEquals(0.3986004415E+15 ,reader.getMu(),  0);
        assertEquals(0.6378136460E+07 ,reader.getAe(),  0);

        in =
            SHMFormatReaderTest.class.getResourceAsStream("/potential/shm-format-compressed/eigen_cg03c_coef.gz");
        reader = factory.getPotentialReader(in);
        reader.read();
        C = reader.getC(5, 5, true);;
        S = reader.getS(5, 5, true);;

        assertEquals(0.957201462136E-06,C[3][0],  0);
        assertEquals(0.174786174485E-06,C[5][5],  0);
        assertEquals(0, S[4][0],  0);
        assertEquals(0.308834784975E-06 ,S[4][4],  0);
        assertEquals(0.3986004415E+15 ,reader.getMu(),  0);
        assertEquals(0.6378136460E+07 ,reader.getAe(),  0);

    }

    public void testException() throws FileNotFoundException, IOException {

        PotentialCoefficientsReader reader;
        int c = 0;
        PotentialReaderFactory factory = new PotentialReaderFactory();
        try {
            InputStream in =
                SHMFormatReaderTest.class.getResourceAsStream("/potential/shm-format-corrupted/fakeeigen1");
            reader = factory.getPotentialReader(in);
        } catch (OrekitException e) {
            c++;
            // expected behaviour
        }
        try {
            InputStream in =
                SHMFormatReaderTest.class.getResourceAsStream("/potential/shm-format-corrupted/fakeeigen2");
            reader = factory.getPotentialReader(in);
        } catch (OrekitException e) {
            c++;
            // expected behaviour
        }
        try {
            InputStream in =
                SHMFormatReaderTest.class.getResourceAsStream("/potential/shm-format-corrupted/fakeeigen3");
            reader = factory.getPotentialReader(in);
        } catch (OrekitException e) {
            c++;
            // expected behaviour
        }

        try {
            reader = new SHMFormatReader();
            reader.read();
        } catch (OrekitException e) {
            c++;
            // expected behaviour
        }

        assertEquals(4 , c);

    }

    public static Test suite() {
        return new TestSuite(SHMFormatReaderTest.class);
    }
}
