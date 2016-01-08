/* Copyright 2002-2016 CS Systèmes d'Information
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
package org.orekit.forces.gravity;

import java.lang.reflect.Field;

import org.junit.Assert;
import org.junit.Test;
import org.orekit.errors.OrekitException;
import org.orekit.forces.gravity.potential.OceanTidesWave;

public class OceanTidesWaveTest {

    @Test
    public void testDelaunayParameters()
        throws OrekitException, SecurityException, NoSuchFieldException,
               IllegalArgumentException, IllegalAccessException {

        Field cGammaField = OceanTidesWave.class.getDeclaredField("cGamma");
        cGammaField.setAccessible(true);
        Field cLField = OceanTidesWave.class.getDeclaredField("cL");
        cLField.setAccessible(true);
        Field cLPrimeField = OceanTidesWave.class.getDeclaredField("cLPrime");
        cLPrimeField.setAccessible(true);
        Field cFField = OceanTidesWave.class.getDeclaredField("cF");
        cFField.setAccessible(true);
        Field cDField = OceanTidesWave.class.getDeclaredField("cD");
        cDField.setAccessible(true);
        Field cOmegaField = OceanTidesWave.class.getDeclaredField("cOmega");
        cOmegaField.setAccessible(true);

        // these reference values have been extract from table 6.5b, 6.5a and 6.5c
        // in IERS conventions 2010
        int[][] tab65 = new int[][] {

            // long period waves from table 6.5b
            {  55565, 0,  0,  0,  0,  0,  1 },
            {  55575, 0,  0,  0,  0,  0,  2 },
            {  56554, 0,  0, -1,  0,  0,  0 },
            {  57555, 0,  0,  0, -2,  2, -2 },
            {  57565, 0,  0,  0, -2,  2, -1 },
            {  58554, 0,  0, -1, -2,  2, -2 },
            {  63655, 0,  1,  0,  0, -2,  0 },
            {  65445, 0, -1,  0,  0,  0, -1 },
            {  65455, 0, -1,  0,  0,  0,  0 },
            {  65465, 0, -1,  0,  0,  0,  1 },
            {  65655, 0,  1,  0, -2,  0, -2 },
            {  73555, 0,  0,  0,  0, -2,  0 },
            {  75355, 0, -2,  0,  0,  0,  0 },
            {  75555, 0,  0,  0, -2,  0, -2 },
            {  75565, 0,  0,  0, -2,  0, -1 },
            {  75575, 0,  0,  0, -2,  0,  0 },
            {  83655, 0,  1,  0, -2, -2, -2 },
            {  85455, 0, -1,  0, -2,  0, -2 },
            {  85465, 0, -1,  0, -2,  0, -1 },
            {  93555, 0,  0,  0, -2, -2, -2 },
            {  95355, 0, -2,  0, -2,  0, -2 },

            // diurnal waves from table 6.5a
            { 125755, 1,  2,  0,  2,  0,  2 },
            { 127555, 1,  0,  0,  2,  2,  2 },
            { 135645, 1,  1,  0,  2,  0,  1 },
            { 135655, 1,  1,  0,  2,  0,  2 },
            { 137455, 1, -1,  0,  2,  2,  2 },
            { 145545, 1,  0,  0,  2,  0,  1 },
            { 145555, 1,  0,  0,  2,  0,  2 },
            { 147555, 1,  0,  0,  0,  2,  0 },
            { 153655, 1,  1,  0,  2, -2,  2 },
            { 155445, 1, -1,  0,  2,  0,  1 },
            { 155455, 1, -1,  0,  2,  0,  2 },
            { 155655, 1,  1,  0,  0,  0,  0 },
            { 155665, 1,  1,  0,  0,  0,  1 },
            { 157455, 1, -1,  0,  0,  2,  0 },
            { 157465, 1, -1,  0,  0,  2,  1 },
            { 162556, 1,  0,  1,  2, -2,  2 },
            { 163545, 1,  0,  0,  2, -2,  1 },
            { 163555, 1,  0,  0,  2, -2,  2 },
            { 164554, 1,  0, -1,  2, -2,  2 },
            { 164556, 1,  0,  1,  0,  0,  0 },
            { 165345, 1, -2,  0,  2,  0,  1 },
            { 165535, 1,  0,  0,  0,  0, -2 },
            { 165545, 1,  0,  0,  0,  0, -1 },
            { 165555, 1,  0,  0,  0,  0,  0 },
            { 165565, 1,  0,  0,  0,  0,  1 },
            { 165575, 1,  0,  0,  0,  0,  2 },
            { 166455, 1, -1,  0,  0,  1,  0 },
            { 166544, 1,  0, -1,  0,  0, -1 },
            { 166554, 1,  0, -1,  0,  0,  0 },
            { 166556, 1,  0,  1, -2,  2, -2 },
            { 166564, 1,  0, -1,  0,  0,  1 },
            { 167355, 1, -2,  0,  0,  2,  0 },
            { 167365, 1, -2,  0,  0,  2,  1 },
            { 167555, 1,  0,  0, -2,  2, -2 },
            { 167565, 1,  0,  0, -2,  2, -1 },
            { 168554, 1,  0, -1, -2,  2, -2 },
            { 173655, 1,  1,  0,  0, -2,  0 },
            { 173665, 1,  1,  0,  0, -2,  1 },
            { 175445, 1, -1,  0,  0,  0, -1 },
            { 175455, 1, -1,  0,  0,  0,  0 },
            { 175465, 1, -1,  0,  0,  0,  1 },
            { 183555, 1,  0,  0,  0, -2,  0 },
            { 185355, 1, -2,  0,  0,  0,  0 },
            { 185555, 1,  0,  0, -2,  0, -2 },
            { 185565, 1,  0,  0, -2,  0, -1 },
            { 185575, 1,  0,  0, -2,  0,  0 },
            { 195455, 1, -1,  0, -2,  0, -2 },
            { 195465, 1, -1,  0, -2,  0, -1 },

            // semi-diurnal waves from table 6.5.c
            { 245655, 2,  1,  0,  2,  0,  2 },
            { 255555, 2,  0,  0,  2,  0,  2 }

        };

        for (int[] row : tab65) {
            OceanTidesWave wave = new OceanTidesWave(row[0], 0, 0, new double[1][1][4]);
            Assert.assertEquals( row[0], wave.getDoodson());
            Assert.assertEquals( row[1], ((Integer) cGammaField.get(wave)).intValue());
            Assert.assertEquals(-row[2], ((Integer) cLField.get(wave)).intValue());
            Assert.assertEquals(-row[3], ((Integer) cLPrimeField.get(wave)).intValue());
            Assert.assertEquals(-row[4], ((Integer) cFField.get(wave)).intValue());
            Assert.assertEquals(-row[5], ((Integer) cDField.get(wave)).intValue());
            Assert.assertEquals(-row[6], ((Integer) cOmegaField.get(wave)).intValue());
        }

    }

}
