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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.data.DataProvidersManager;
import org.orekit.errors.OrekitException;
import org.orekit.forces.gravity.potential.AstronomicalAmplitudeReader;
import org.orekit.forces.gravity.potential.FESCHatEpsilonReader;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.OceanLoadDeformationCoefficients;
import org.orekit.forces.gravity.potential.NormalizedSphericalHarmonicsProvider.NormalizedSphericalHarmonics;
import org.orekit.forces.gravity.potential.OceanTidesWave;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.time.UT1Scale;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;

public class OceanTidesFieldTest {

    @Test
    public void testDeltaCnmSnm() throws OrekitException {

        // this is an arbitrarily truncated model, limited to 4x4 and with only a few waves
        List<OceanTidesWave> waves = getWaves(4, 4, 55565, 56554, 85455, 135655, 273555);

        UT1Scale ut1 = TimeScalesFactory.getUT1(IERSConventions.IERS_2010, true);
        TimeScale utc = TimeScalesFactory.getUTC();

        AbsoluteDate date = new AbsoluteDate(2003, 5, 6, 13, 43, 32.125, utc);
        OceanTidesField tidesField =
                new OceanTidesField(Constants.EIGEN5C_EARTH_EQUATORIAL_RADIUS, Constants.EIGEN5C_EARTH_MU,
                                    waves,
                                    IERSConventions.IERS_2010.getNutationArguments(ut1),
                                    null);
        NormalizedSphericalHarmonics harmonics = tidesField.onDate(date);
        double[][] refDeltaCnm = new double[][] {
            {           0.0,                    0.0,                   0.0,                      0.0,                     0.0           },
            {           0.0,                    0.0,                   0.0,                      0.0,                     0.0           },
            { -4.812565797928061E-11,  -4.1748378190052583E-11, 7.013273986245356E-11,           0.0,                     0.0           },
            { -2.5341227608443308E-11,  9.76515813742254E-11,  -1.21931214469994E-10,    1.3179722429471184E-10,          0.0           },
            { -2.7496974839179478E-11,  8.419627031293907E-11,  6.56546217101275E-11,   -3.375298928713117E-11,  -7.588006744166988E-11 }
        };
        double[][] refDeltaSnm = new double[][] {
            {           0.0,                    0.0,                   0.0,                      0.0,                     0.0           },
            {           0.0,                    0.0,                   0.0,                      0.0,                     0.0           },
            { -1.168129177701461E-10,   5.646187590518608E-12,  1.742233297668071E-10,           0.0,                     0.0           },
            { -6.586546350227345E-11,  -8.032186864783105E-11, -3.118910148495339E-11,   1.0566857199592183E-10,          0.0           },
            {  7.665313525684617E-11,   7.37884528812169E-11,  -1.3085142873419844E-10, -1.5813709543115768E-10,  1.770903634801541E-10 }
        };
        for (int n = 0; n < refDeltaCnm.length; ++n) {
            double threshold = 4.0e-17;
            for (int m = 0; m <= n; ++m) {
                Assert.assertEquals(refDeltaCnm[n][m], harmonics.getNormalizedCnm(n, m), threshold);
                Assert.assertEquals(refDeltaSnm[n][m], harmonics.getNormalizedSnm(n, m), threshold);
            }
        }
    }

    private List<OceanTidesWave> getWaves(int degree, int order, int ... doodson)
        throws OrekitException {

        // load a complete model
        AstronomicalAmplitudeReader aaReader =
                new AstronomicalAmplitudeReader("hf-fes2004.dat", 5, 2, 3, 1.0);
        DataProvidersManager.getInstance().feed(aaReader.getSupportedNames(), aaReader);
        Map<Integer, Double> map = aaReader.getAstronomicalAmplitudesMap();
        GravityFieldFactory.addOceanTidesReader(new FESCHatEpsilonReader("fes2004-7x7.dat",
                                                                         0.01, FastMath.toRadians(1.0),
                                                                         OceanLoadDeformationCoefficients.IERS_2010,
                                                                         map));
        List<OceanTidesWave> complete =  GravityFieldFactory.getOceanTidesWaves(degree, order);
        double[][][] triangular = new double[degree + 1][][];
        for (int i = 0; i <= degree; ++i) {
            triangular[i] = new double[FastMath.min(i, order) + 1][4];
        };

        // filter waves
        List<OceanTidesWave> filtered = new ArrayList<OceanTidesWave>(doodson.length);
        for (final int d : doodson) {
            for (final OceanTidesWave wave : complete) {
                if (wave.getDoodson() == d) {
                    filtered.add(wave);
                }
            }
        }

        return filtered;

    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data:tides");
    }

}
