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
package org.orekit.bodies;

import org.hipparchus.analysis.UnivariateFunction;
import org.hipparchus.analysis.UnivariateVectorFunction;
import org.hipparchus.analysis.differentiation.DSFactory;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.analysis.differentiation.FiniteDifferencesDifferentiator;
import org.hipparchus.analysis.differentiation.UnivariateDifferentiableFunction;
import org.hipparchus.analysis.differentiation.UnivariateDifferentiableVectorFunction;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.hipparchus.util.Precision;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.bodies.IAUPoleFactory.OldIAUPole;
import org.orekit.bodies.JPLEphemeridesLoader.EphemerisType;
import org.orekit.data.DataContext;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScales;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

public class PredefinedIAUPolesTest {

    @Test
    public void testGCRFAligned() throws UnsupportedEncodingException, IOException {
        IAUPole iauPole = PredefinedIAUPoles.getIAUPole(EphemerisType.SOLAR_SYSTEM_BARYCENTER, timeScales);
        Vector3D pole = iauPole.getPole(AbsoluteDate.J2000_EPOCH);
        double w = iauPole.getPrimeMeridianAngle(AbsoluteDate.J2000_EPOCH.shiftedBy(3600.0));
        Assertions.assertEquals(0,   Vector3D.distance(pole, Vector3D.PLUS_K), 1.0e-15);
        Assertions.assertEquals(0.0, w, 1.0e-15);
    }

    @Test
    public void testSun() throws UnsupportedEncodingException, IOException {
        IAUPole iauPole = PredefinedIAUPoles.getIAUPole(EphemerisType.SUN, timeScales);
        Vector3D pole = iauPole.getPole(AbsoluteDate.J2000_EPOCH);
        final double alphaRef    = FastMath.toRadians(286.13);
        final double deltaRef    = FastMath.toRadians(63.87);
        final double wRef        = FastMath.toRadians(84.176);
        final double rateRef     = FastMath.toRadians(14.1844000);
        double w = iauPole.getPrimeMeridianAngle(new AbsoluteDate(AbsoluteDate.J2000_EPOCH, 3600.0,
                                                                  TimeScalesFactory.getTDB()));
        Assertions.assertEquals(alphaRef, MathUtils.normalizeAngle(pole.getAlpha(), alphaRef), 1.0e-15);
        Assertions.assertEquals(deltaRef, pole.getDelta(), 1.0e-15);
        Assertions.assertEquals(wRef + rateRef / 24.0, w, 1.0e-15);
    }

    @Test
    public void testNaif() throws UnsupportedEncodingException, IOException {
        final TimeScale tdb = TimeScalesFactory.getTDB();
        final InputStream inEntry = getClass().getResourceAsStream("/naif/IAU-pole-NAIF.txt");
        BufferedReader reader = new BufferedReader(new InputStreamReader(inEntry, StandardCharsets.UTF_8));
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
                IAUPole iauPole = PredefinedIAUPoles.getIAUPole(type, timeScales);
                Vector3D pole = iauPole.getPole(date2);
                double w = iauPole.getPrimeMeridianAngle(date2);
                Assertions.assertEquals(0.0, date2.durationFrom(date1), 8.0e-5);
                Assertions.assertEquals(alphaRef, MathUtils.normalizeAngle(pole.getAlpha(), alphaRef), 1.8e-15);
                Assertions.assertEquals(deltaRef, pole.getDelta(), 2.4e-13);
                Assertions.assertEquals(wRef, MathUtils.normalizeAngle(w, wRef), 2.5e-12);

                // check matrix
                Vector3D qNode = Vector3D.crossProduct(Vector3D.PLUS_K, pole);
                if (qNode.getNormSq() < Precision.SAFE_MIN) {
                    qNode = Vector3D.PLUS_I;
                }
                final Rotation rotation = new Rotation(Vector3D.PLUS_K, wRef, RotationConvention.FRAME_TRANSFORM).
                                          applyTo(new Rotation(pole, qNode, Vector3D.PLUS_K, Vector3D.PLUS_I));
                Assertions.assertEquals(0.0, Rotation.distance(rRef, rotation), 1.9e-15);

            }
        }
    }

    @Test
    public void testVersus80Implementation() {
        for (EphemerisType body : EphemerisType.values()) {
            IAUPole    newPole = PredefinedIAUPoles.getIAUPole(body, timeScales);
            OldIAUPole oldPole = IAUPoleFactory.getIAUPole(body);
            for (double dt = 0; dt < Constants.JULIAN_YEAR; dt += 3600) {
                final AbsoluteDate date = AbsoluteDate.J2000_EPOCH.shiftedBy(dt);
                Assertions.assertEquals(0, Vector3D.angle(newPole.getPole(date), oldPole.getPole(date)), 1.0e-20);
                Assertions.assertEquals(oldPole.getPrimeMeridianAngle(date), newPole.getPrimeMeridianAngle(date), 5.0e-13);
            }
        }

    }

    @Test
    public void testFieldConsistency() {
        for (IAUPole iaupole : PredefinedIAUPoles.values(timeScales)) {
            for (double dt = 0; dt < Constants.JULIAN_YEAR; dt += 3600) {
                final AbsoluteDate date = AbsoluteDate.J2000_EPOCH.shiftedBy(dt);
                final FieldAbsoluteDate<Binary64> date64 = new FieldAbsoluteDate<>(Binary64Field.getInstance(), date);
                Assertions.assertEquals(0, Vector3D.angle(iaupole.getPole(date), iaupole.getPole(date64).toVector3D()), 2.0e-15);
                Assertions.assertEquals(iaupole.getPrimeMeridianAngle(date), iaupole.getPrimeMeridianAngle(date64).getReal(), 1.0e-12);
            }
        }

    }

    @Test
    public void testDerivatives() {
        final DSFactory factory = new DSFactory(1, 1);
        final AbsoluteDate ref = AbsoluteDate.J2000_EPOCH;
        final FieldAbsoluteDate<DerivativeStructure> refDS = new FieldAbsoluteDate<>(factory.getDerivativeField(), ref);
        FiniteDifferencesDifferentiator differentiator = new FiniteDifferencesDifferentiator(8, 60.0);
        for (final IAUPole iaupole : PredefinedIAUPoles.values(timeScales)) {

            UnivariateDifferentiableVectorFunction dPole = differentiator.differentiate(new UnivariateVectorFunction() {
                @Override
                public double[] value(double t) {
                    return iaupole.getPole(ref.shiftedBy(t)).toArray();
                }
            });
            UnivariateDifferentiableFunction dMeridian = differentiator.differentiate(new UnivariateFunction() {
                @Override
                public double value(double t) {
                    return iaupole.getPrimeMeridianAngle(ref.shiftedBy(t));
                }
            });

            for (double dt = 0; dt < Constants.JULIAN_YEAR; dt += 3600) {

                final DerivativeStructure dtDS = factory.variable(0, dt);

                final DerivativeStructure[] refPole = dPole.value(dtDS);
                final DerivativeStructure[] fieldPole = iaupole.getPole(refDS.shiftedBy(dtDS)).toArray();
                for (int i = 0; i < 3; ++i) {
                    Assertions.assertEquals(refPole[i].getValue(),              fieldPole[i].getValue(),              2.0e-15);
                    Assertions.assertEquals(refPole[i].getPartialDerivative(1), fieldPole[i].getPartialDerivative(1), 4.0e-17);
                }

                final DerivativeStructure refMeridian = dMeridian.value(dtDS);
                final DerivativeStructure fieldMeridian = iaupole.getPrimeMeridianAngle(refDS.shiftedBy(dtDS));
                Assertions.assertEquals(refMeridian.getValue(),              fieldMeridian.getValue(),              4.0e-12);
                Assertions.assertEquals(refMeridian.getPartialDerivative(1), fieldMeridian.getPartialDerivative(1), 9.0e-14);

            }
        }

    }

    private TimeScales timeScales;

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
        timeScales = DataContext.getDefault().getTimeScales();
    }

}

