/* Copyright 2002-2024 Thales Alenia Space
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
package org.orekit.models.earth.troposphere.iturp834;

import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Test;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.models.earth.troposphere.AbstractPathDelayTest;
import org.orekit.models.earth.troposphere.ChaoMappingFunction;
import org.orekit.models.earth.troposphere.ConstantAzimuthalGradientProvider;
import org.orekit.models.earth.troposphere.ConstantTroposphericModel;
import org.orekit.models.earth.troposphere.ConstantViennaAProvider;
import org.orekit.models.earth.troposphere.GlobalMappingFunctionModel;
import org.orekit.models.earth.troposphere.MendesPavlisModel;
import org.orekit.models.earth.troposphere.NiellMappingFunctionModel;
import org.orekit.models.earth.troposphere.RevisedChaoMappingFunction;
import org.orekit.models.earth.troposphere.TroposphereMappingFunction;
import org.orekit.models.earth.troposphere.TroposphericDelay;
import org.orekit.models.earth.troposphere.TroposphericModelUtils;
import org.orekit.models.earth.troposphere.ViennaACoefficients;
import org.orekit.models.earth.troposphere.ViennaOne;
import org.orekit.models.earth.troposphere.ViennaThree;
import org.orekit.models.earth.weather.ConstantPressureTemperatureHumidityProvider;
import org.orekit.models.earth.weather.PressureTemperatureHumidity;
import org.orekit.models.earth.weather.water.CIPM2007;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.TrackingCoordinates;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class ITURP834PathDelayTest extends AbstractPathDelayTest<ITURP834PathDelay> {

    protected ITURP834PathDelay buildTroposphericModel() {
        return new ITURP834PathDelay(VerticalExcessPath.NON_COASTAL_NON_EQUATORIAL, new CIPM2007());
    }

    @Test
    @Override
    public void testDelay() {
        doTestDelay(defaultDate, defaultPoint, defaultTrackingCoordinates,
                    10.15, 0, 0, 0, 0);
    }

    @Test
    @Override
    public void testFieldDelay() {
        doTestDelay(Binary64Field.getInstance(),
                    defaultDate, defaultPoint, defaultTrackingCoordinates,
                    10.15, 0, 0, 0, 0);
    }

    @Test
    public void testTmp() throws
                          IOException {
        final AbsoluteDate date = new AbsoluteDate(1994, 1, 1, TimeScalesFactory.getUTC());

        final double latitude    = FastMath.toRadians(48.0);
        final double longitude   = FastMath.toRadians(0.20);
        final double height      = 68.0;
        final GeodeticPoint point = new GeodeticPoint(latitude, longitude, height);

        final ITURP834PathDelay model = buildTroposphericModel();

        final TimeScale utc = TimeScalesFactory.getUTC();

            final ProcessBuilder pb = new ProcessBuilder("gnuplot").
                            redirectOutput(ProcessBuilder.Redirect.INHERIT).
                            redirectError(ProcessBuilder.Redirect.INHERIT);
            pb.environment().remove("XDG_SESSION_TYPE");
            final Process gnuplot = pb.start();
            try (PrintStream out = new PrintStream(gnuplot.getOutputStream(), false, StandardCharsets.UTF_8.name())) {
                out.format(Locale.US, "set terminal qt size %d, %d title 'path delay'%n", 1000, 1000);
                out.format(Locale.US, "set xlabel 'elevation'%n");
                out.format(Locale.US, "set ylabel 'path delay'%n");
                out.format(Locale.US, "set title '%s'%n", "Path delays");
                print(new ITURP834MappingFunction(utc), out, "$itu", 0, point,
                      TroposphericModelUtils.STANDARD_ATMOSPHERE, date);
                print(new ChaoMappingFunction(), out, "$chao", 0, point,
                      TroposphericModelUtils.STANDARD_ATMOSPHERE, date);
                print(new RevisedChaoMappingFunction(), out, "$revised_chao", 0, point,
                      TroposphericModelUtils.STANDARD_ATMOSPHERE, date);
                print(new ViennaOne(new ConstantViennaAProvider(new ViennaACoefficients(0.00127683, 0.00060955)),
                                              new ConstantAzimuthalGradientProvider(null),
                                              new ConstantTroposphericModel(new TroposphericDelay(2.0966, 0.2140, 0, 0)),
                                              TimeScalesFactory.getUTC()), out, "$vienna_1", 0, point,
                      TroposphericModelUtils.STANDARD_ATMOSPHERE, date);
                print(new ViennaThree(new ConstantViennaAProvider(new ViennaACoefficients(0.00127683, 0.00060955)),
                                              new ConstantAzimuthalGradientProvider(null),
                                              new ConstantTroposphericModel(new TroposphericDelay(2.0966, 0.2140, 0, 0)),
                                              TimeScalesFactory.getUTC()), out, "$vienna_3", 0, point,
                      TroposphericModelUtils.STANDARD_ATMOSPHERE, date);
                print(new GlobalMappingFunctionModel(), out, "$global", 0, point,
                      TroposphericModelUtils.STANDARD_ATMOSPHERE, date);
                print(new MendesPavlisModel(new ConstantPressureTemperatureHumidityProvider(TroposphericModelUtils.STANDARD_ATMOSPHERE),
                                            0.532, TroposphericModelUtils.MICRO_M), out, "$mendes_pavlis", 0, point,
                      TroposphericModelUtils.STANDARD_ATMOSPHERE, date);
                print(new NiellMappingFunctionModel(), out, "$niell", 0, point,
                      TroposphericModelUtils.STANDARD_ATMOSPHERE, date);
                out.format(Locale.US, "plot $itu with lines title 'ITU', \\%n");
                out.format(Locale.US, "     $chao with lines title 'Chao', \\%n");
                out.format(Locale.US, "     $revised_chao with lines title 'revised Chao', \\%n");
                out.format(Locale.US, "     $vienna_1 with lines title 'Vienna 1', \\%n");
                out.format(Locale.US, "     $vienna_3 with lines title 'Vienna 3', \\%n");
                out.format(Locale.US, "     $global with lines title 'global', \\%n");
                out.format(Locale.US, "     $mendes_pavlis with lines title 'Mendes-Pavlis', \\%n");
                out.format(Locale.US, "     $niell with lines title 'Niell', \\%n");
                out.format(Locale.US, "pause mouse close%n");
            }
    }
    private void print(final TroposphereMappingFunction tmf, final PrintStream out, final String name, final int index,
                       final GeodeticPoint point, final PressureTemperatureHumidity weather, final AbsoluteDate date) {
        out.format(Locale.US, "%s <<EOD%n", name);
        for (double e = 0; e < FastMath.toRadians(20); e += 0.001) {
            out.format(Locale.US, "%.6f %.6f%n",
                       FastMath.toDegrees(e),
                       tmf.mappingFactors(new TrackingCoordinates(0, e, 1.4e6), point, weather, date)[index]);
        }
        out.format(Locale.US, "EOD%n");
    }

}
