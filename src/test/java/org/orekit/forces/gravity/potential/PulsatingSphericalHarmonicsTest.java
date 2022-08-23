/* Copyright 2002-2022 CS GROUP
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
package org.orekit.forces.gravity.potential;

import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.data.DataContext;
import org.orekit.forces.gravity.potential.RawSphericalHarmonicsProvider.RawSphericalHarmonics;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider.UnnormalizedSphericalHarmonics;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.utils.TimeSpanMap;

import java.lang.reflect.Field;

@Deprecated
public class PulsatingSphericalHarmonicsTest {

    @Deprecated
    @Test
    public void testDeprecated() {
        Utils.setDataRoot("potential");
        TimeScale tt = DataContext.getDefault().getTimeScales().getTT();
        GravityFieldFactory.addPotentialCoefficientsReader(new ICGEMFormatReader("EIGEN-6S4-v2-truncated", true, tt));
        UnnormalizedSphericalHarmonicsProvider ush      = GravityFieldFactory.getUnnormalizedProvider(1, 1);
        UnnormalizedSphericalHarmonics         parsed   = ush.onDate(AbsoluteDate.J2000_EPOCH);
        PulsatingSphericalHarmonics            psh      = toPulsatingSphericalHarmonics(ush, AbsoluteDate.J2000_EPOCH);
        RawSphericalHarmonics                  rebuilt  = psh.onDate(AbsoluteDate.J2000_EPOCH);
        Assertions.assertEquals(AbsoluteDate.J2000_EPOCH, rebuilt.getDate());
        Assertions.assertEquals(new AbsoluteDate(1950, 1, 1, 0, 0, 0.0, tt), psh.getReferenceDate());
        Assertions.assertEquals(ush.getMu(), psh.getMu(), 1.0e-20);
        Assertions.assertEquals(ush.getAe(), psh.getAe(), 1.0e-20);
        Assertions.assertEquals(ush.getMaxDegree(),   psh.getMaxDegree());
        Assertions.assertEquals(ush.getMaxOrder(),    psh.getMaxOrder());
        Assertions.assertEquals(ush.getTideSystem(),  psh.getTideSystem());
        for (int n = 0; n <= ush.getMaxDegree(); ++n) {
            for (int m = 0; m <= FastMath.min(n, ush.getMaxOrder()); ++m) {
                Assertions.assertEquals(parsed.getUnnormalizedCnm(n, m), rebuilt.getRawCnm(n, m), 1.0e-15);
                Assertions.assertEquals(parsed.getUnnormalizedSnm(n, m), rebuilt.getRawSnm(n, m), 1.0e-15);
            }
        }
    }

    private PulsatingSphericalHarmonics toPulsatingSphericalHarmonics(final UnnormalizedSphericalHarmonicsProvider ush,
                                                                      final AbsoluteDate date) {
        PiecewiseSphericalHarmonics     psh        = extractField(ush,            "rawProvider");
        ConstantSphericalHarmonics      constant   = extractField(psh,            "constant");
        AbsoluteDate[]                  references = extractField(psh,            "references");
        double[]                        pulsations = extractField(psh,            "pulsations");
        TimeSpanMap<PiecewisePart>      pieces     = extractField(psh,            "pieces");
        TimeSpanMap.Span<PiecewisePart> span       = pieces.getSpan(date);
        Flattener                       flattener  = extractField(span.getData(), "flattener");
        TimeDependentHarmonic[]         components = extractField(span.getData(), "components");

        // extract constant part
        double[] rawC = ((double[]) extractField(constant, "rawC")).clone();
        double[] rawS = ((double[]) extractField(constant, "rawS")).clone();

        // extract secular part (patching constant part as required)
        int trendReferenceIndex = -1;
        double[] cTrend = new double[flattener.arraySize()];
        double[] sTrend = new double[flattener.arraySize()];
        for (int n = 0; n <= flattener.getDegree(); ++n) {
            for (int m = 0; m <= FastMath.min(n, flattener.getOrder()); ++m) {
                final int index = flattener.index(n, m);
                if (components[index] != null) {
                    trendReferenceIndex = ((Integer) extractField(components[index], "trendReferenceIndex")).intValue();
                    rawC[index]  += ((Double) extractField(components[index], "cBase")).doubleValue();
                    rawS[index]  += ((Double) extractField(components[index], "sBase")).doubleValue();
                    cTrend[index] = ((Double) extractField(components[index], "cTrend")).doubleValue();
                    sTrend[index] = ((Double) extractField(components[index], "sTrend")).doubleValue();
                }
            }
        }
        RawSphericalHarmonicsProvider raw =
                        new ConstantSphericalHarmonics(constant.getAe(), constant.getMu(), constant.getTideSystem(),
                                                       flattener, rawC, rawS);
        raw = new SecularTrendSphericalHarmonics(raw, references[trendReferenceIndex],
                                                 flattener, cTrend, sTrend);

        // extract harmonic parts
        for (int i = 0; i < pulsations.length; ++i) {
            double[][] cosC = new double[flattener.getDegree() + 1][];
            double[][] sinC = new double[flattener.getDegree() + 1][];
            double[][] cosS = new double[flattener.getDegree() + 1][];
            double[][] sinS = new double[flattener.getDegree() + 1][];
            for (int n = 0; n <= flattener.getDegree(); ++n) {
                cosC[n] = new double[FastMath.min(n, flattener.getOrder()) + 1];
                sinC[n] = new double[FastMath.min(n, flattener.getOrder()) + 1];
                cosS[n] = new double[FastMath.min(n, flattener.getOrder()) + 1];
                sinS[n] = new double[FastMath.min(n, flattener.getOrder()) + 1];
                for (int m = 0; m <= FastMath.min(n, flattener.getOrder()); ++m) {
                    final int index = flattener.index(n, m);
                    if (components[index] != null) {
                        cosC[n][m] = ((double[]) extractField(components[index], "cosC"))[i];
                        sinC[n][m] = ((double[]) extractField(components[index], "sinC"))[i];
                        cosS[n][m] = ((double[]) extractField(components[index], "cosS"))[i];
                        sinS[n][m] = ((double[]) extractField(components[index], "sinS"))[i];
                    }
                }
            }
            raw = new PulsatingSphericalHarmonics(raw, MathUtils.TWO_PI / pulsations[i],
                                                  cosC, sinC, cosS, sinS);
        }

        return (PulsatingSphericalHarmonics) raw;

    }

    @SuppressWarnings("unchecked")
    private <T> T extractField(final Object o, final String fieldName) {
        try {
            Field field = o.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return (T) field.get(o);
        } catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException e) {
            Assertions.fail(e.getLocalizedMessage());
            return null;
        }
    }

}
