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
package org.orekit.bodies;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.hipparchus.util.Precision;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.bodies.JPLEphemeridesLoader.EphemerisType;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;

public class IAUPoleFactoryTest {

    @Test
    public void testGCRFAligned() throws OrekitException, UnsupportedEncodingException, IOException {
        IAUPole iauPole = IAUPoleFactory.getIAUPole(EphemerisType.SOLAR_SYSTEM_BARYCENTER);
        Vector3D pole = iauPole.getPole(AbsoluteDate.J2000_EPOCH);
        double w = iauPole.getPrimeMeridianAngle(AbsoluteDate.J2000_EPOCH.shiftedBy(3600.0));
        Assert.assertEquals(0,   Vector3D.distance(pole, Vector3D.PLUS_K), 1.0e-15);
        Assert.assertEquals(0.0, w, 1.0e-15);
    }

    @Test
    public void testSun() throws OrekitException, UnsupportedEncodingException, IOException {
        IAUPole iauPole = IAUPoleFactory.getIAUPole(EphemerisType.SUN);
        Vector3D pole = iauPole.getPole(AbsoluteDate.J2000_EPOCH);
        final double alphaRef    = FastMath.toRadians(286.13);
        final double deltaRef    = FastMath.toRadians(63.87);
        final double wRef        = FastMath.toRadians(84.176);
        final double rateRef     = FastMath.toRadians(14.1844000);
        double w = iauPole.getPrimeMeridianAngle(new AbsoluteDate(AbsoluteDate.J2000_EPOCH, 3600.0,
                                                                  TimeScalesFactory.getTDB()));
        Assert.assertEquals(alphaRef, MathUtils.normalizeAngle(pole.getAlpha(), alphaRef), 1.0e-15);
        Assert.assertEquals(deltaRef, pole.getDelta(), 1.0e-15);
        Assert.assertEquals(wRef + rateRef / 24.0, w, 1.0e-15);
    }

    @Test
    public void testNaif() throws OrekitException, UnsupportedEncodingException, IOException {
        final TimeScale tdb = TimeScalesFactory.getTDB();
        final InputStream inEntry = getClass().getResourceAsStream("/naif/IAU-pole-NAIF.txt");
        BufferedReader reader = new BufferedReader(new InputStreamReader(inEntry, "UTF-8"));
        for (String line = reader.readLine(); line != null; line = reader.readLine()) {
            line = line.trim();
            if (!line.isEmpty() && !line.startsWith("#")) {

                // extract reference data from Naif
                String[] fields = line.split("\\s+");
                final AbsoluteDate date1 = new AbsoluteDate(fields[0], tdb);
                final AbsoluteDate date2 = new AbsoluteDate(AbsoluteDate.J2000_EPOCH,
                                                            Double.parseDouble(fields[1]),
                                                            tdb);
                final EphemerisType type = EphemerisType.valueOf(fields[2]);
                final double alphaRef    = Double.parseDouble(fields[3]);
                final double deltaRef    = Double.parseDouble(fields[4]);
                final double wRef        = Double.parseDouble(fields[5]);
                final double[][] m       = new double[3][3];
                int index = 6;
                for (int i = 0; i < 3; ++i) {
                    for (int j = 0; j < 3; ++j) {
                        // we transpose the matrix to get the transform
                        // from ICRF to body frame
                        m[j][i] = Double.parseDouble(fields[index++]);
                    }
                }
                Rotation rRef = new Rotation(m, 1.0e-10);

                // check pole
                IAUPole iauPole = IAUPoleFactory.getIAUPole(type);
                Vector3D pole = iauPole.getPole(date2);
                double w = iauPole.getPrimeMeridianAngle(date2);
                Assert.assertEquals(0.0, date2.durationFrom(date1), 8.0e-5);
                Assert.assertEquals(alphaRef, MathUtils.normalizeAngle(pole.getAlpha(), alphaRef), 1.8e-15);
                Assert.assertEquals(deltaRef, pole.getDelta(), 2.4e-13);
                Assert.assertEquals(wRef, MathUtils.normalizeAngle(w, wRef), 2.5e-12);

                // check matrix
                Vector3D qNode = Vector3D.crossProduct(Vector3D.PLUS_K, pole);
                if (qNode.getNormSq() < Precision.SAFE_MIN) {
                    qNode = Vector3D.PLUS_I;
                }
                final Rotation rotation = new Rotation(Vector3D.PLUS_K, wRef, RotationConvention.FRAME_TRANSFORM).
                                          applyTo(new Rotation(pole, qNode, Vector3D.PLUS_K, Vector3D.PLUS_I));
                Assert.assertEquals(0.0, Rotation.distance(rRef, rotation), 1.9e-15);

            }
        }
    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

}

