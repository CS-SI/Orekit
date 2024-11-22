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
import org.orekit.Utils;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.data.DataSource;
import org.orekit.models.earth.troposphere.AbstractPathDelayTest;
import org.orekit.models.earth.troposphere.CanonicalSaastamoinenModel;
import org.orekit.models.earth.troposphere.ConstantAzimuthalGradientProvider;
import org.orekit.models.earth.troposphere.ConstantViennaAProvider;
import org.orekit.models.earth.troposphere.FixedTroposphericDelay;
import org.orekit.models.earth.troposphere.MariniMurray;
import org.orekit.models.earth.troposphere.MendesPavlisModel;
import org.orekit.models.earth.troposphere.ModifiedHopfieldModel;
import org.orekit.models.earth.troposphere.ModifiedSaastamoinenModel;
import org.orekit.models.earth.troposphere.TroposphericModel;
import org.orekit.models.earth.troposphere.TroposphericModelUtils;
import org.orekit.models.earth.troposphere.ViennaACoefficients;
import org.orekit.models.earth.troposphere.ViennaOne;
import org.orekit.models.earth.troposphere.ViennaThree;
import org.orekit.models.earth.weather.GlobalPressureTemperature3;
import org.orekit.models.earth.weather.PressureTemperatureHumidity;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.TrackingCoordinates;

import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class ITURP834WeatherParameterProviderTest {

    @Test
    public void testTmp() throws IOException, URISyntaxException {
        Utils.setDataRoot("regular-data");
        final TimeScale utc = TimeScalesFactory.getUTC();
        final AbsoluteDate date = new AbsoluteDate(2018, 11, 25, 12, 0, 0, utc);
        final GeodeticPoint point = new GeodeticPoint(FastMath.toRadians(47.71675), FastMath.toRadians(6.12264), 300.0);
        final URL url = ITURP834WeatherParameterProviderTest.class.getClassLoader().getResource("gpt-grid/gpt3_5.grd");
        final GlobalPressureTemperature3 gpt3 = new GlobalPressureTemperature3(new DataSource(url.toURI()),
                                                                               TimeScalesFactory.getUTC());
        final ITURP834WeatherParametersProvider itu = new ITURP834WeatherParametersProvider(utc);

        final PressureTemperatureHumidity pth    = gpt3.getWeatherParameters(point, date);
        final PressureTemperatureHumidity pthITU = itu.getWeatherParameters(point, date);
        System.out.format(Locale.US, "altitude              : %.6f   %.6f%n", pth.getAltitude(), pthITU.getAltitude());
        System.out.format(Locale.US, "temperature           : %.6f   %.6f%n", pth.getTemperature(), pthITU.getTemperature());
        System.out.format(Locale.US, "pressure              : %.6f   %.6f%n", pth.getPressure(), pthITU.getPressure());
        System.out.format(Locale.US, "water vapour pressure : %.6f   %.6f%n", pth.getWaterVaporPressure(), pthITU.getWaterVaporPressure());
        System.out.format(Locale.US, "Tₘ                    : %.6f   %.6f%n", pth.getTm(), pthITU.getTm());
        System.out.format(Locale.US, "λ                     : %.6f   %.6f%n", pth.getLambda(), pthITU.getLambda());

//            final ProcessBuilder pb = new ProcessBuilder("gnuplot").
//                            redirectOutput(ProcessBuilder.Redirect.INHERIT).
//                            redirectError(ProcessBuilder.Redirect.INHERIT);
//            pb.environment().remove("XDG_SESSION_TYPE");
//            final Process gnuplot = pb.start();
//            try (PrintStream out = new PrintStream(gnuplot.getOutputStream(), false, StandardCharsets.UTF_8.name())) {
//                out.format(Locale.US, "set terminal qt size %d, %d title 'path delay'%n", 1000, 1000);
//                //out.format(Locale.US, "set terminal pngcairo size %d, %d%n", 1000, 1000);
//                //out.format(Locale.US, "set output '/tmp/itu-r.p834.png'%n");
//                out.format(Locale.US, "set xlabel 'elevation (°)'%n");
//                out.format(Locale.US, "set ylabel 'path delay (m)'%n");
//                print(buildTroposphericModel(), out, "$itu", point, pthITU, date);
//                print(new CanonicalSaastamoinenModel(), out, "$canonical_saastamoinen", point,
//                      pth, date);
//                print(new ModifiedSaastamoinenModel(TroposphericModelUtils.STANDARD_ATMOSPHERE_PROVIDER), out, "$modified_saastamoinen",
//                      point, pth, date);
//                print(new ViennaOne(new ConstantViennaAProvider(new ViennaACoefficients(0.00127683, 0.00060955)),
//                                              new ConstantAzimuthalGradientProvider(null),
//                                              new CanonicalSaastamoinenModel(),
//                                              TimeScalesFactory.getUTC()), out, "$vienna_1", point, pth, date);
//                print(new ViennaThree(new ConstantViennaAProvider(new ViennaACoefficients(0.00127683, 0.00060955)),
//                                              new ConstantAzimuthalGradientProvider(null),
//                                              new ModifiedSaastamoinenModel(TroposphericModelUtils.STANDARD_ATMOSPHERE_PROVIDER),
//                                              TimeScalesFactory.getUTC()), out, "$vienna_3", point, pth, date);
//                print(FixedTroposphericDelay.getDefaultModel(), out, "$tabulated", point, pth, date);
//                print(new ModifiedHopfieldModel(), out, "$modified_hopfield", point, pth, date);
//                print(new MariniMurray(694.3, TroposphericModelUtils.NANO_M), out, "$marini_murray", point, pth, date);
//                print(new MendesPavlisModel(TroposphericModelUtils.STANDARD_ATMOSPHERE_PROVIDER,
//                                            0.532, TroposphericModelUtils.MICRO_M), out, "$mendes_pavlis", point, pth, date);
//                out.format(Locale.US, "set multiplot layout 2,1%n");
//                out.format(Locale.US, "set key right top%n");
//                out.format(Locale.US, "set label 1 'slanted dry component' at graph 0.2, 0.8 font 'Helvetica,14' tc rgb 'sea-green'%n");
//                out.format(Locale.US, "plot $itu_d with linespoints pt 9 dt 3 title 'ITU-R P.834', \\%n");
//                out.format(Locale.US, "     $canonical_saastamoinen_d with lines dt 3 title 'canonical Saastamoinen', \\%n");
//                out.format(Locale.US, "     $modified_saastamoinen_d with lines dt 3 title 'modified Saastamoinen', \\%n");
//                out.format(Locale.US, "     $vienna_1_d with linespoints pt 8 dt 3 title 'Vienna 1/canonical Saastamoinen', \\%n");
//                out.format(Locale.US, "     $vienna_3_d with linespoints pt 10 dt 3 title 'Vienna 3/modified Saastamoinen', \\%n");
//                out.format(Locale.US, "     $tabulated_d with lines dt 3 title 'tabulated', \\%n");
//                out.format(Locale.US, "     $modified_hopfield_d with linespoints pt 12 dt 3 title 'modified Hopfield', \\%n");
//                out.format(Locale.US, "     $marini_murray_d with lines dt 2 title 'Marini-Murray (optical at 694.3nm)', \\%n");
//                out.format(Locale.US, "     $mendes_pavlis_d with lines dt 2 title 'Mendes-Pavlis (optical at 532nm)'%n");
//                out.format(Locale.US, "unset label 1%n");
//                out.format(Locale.US, "set label 2 'slanted wet component' at graph 0.2, 0.4 font 'Helvetica,14' tc rgb 'sea-green'%n");
//                out.format(Locale.US, "set key right bottom%n");
//                out.format(Locale.US, "plot $itu_w with linespoints pt 9 dt 3 title 'ITU-R P.834', \\%n");
//                out.format(Locale.US, "     $canonical_saastamoinen_w with lines dt 3 title 'canonical Saastamoinen', \\%n");
//                out.format(Locale.US, "     $modified_saastamoinen_w with lines dt 3 title 'modified Saastamoinen', \\%n");
//                out.format(Locale.US, "     $vienna_1_w with linespoints pt 8 dt 3 title 'Vienna 1/canonical Saastamoinen', \\%n");
//                out.format(Locale.US, "     $vienna_3_w with linespoints pt 10 dt 3 title 'Vienna 3/modified Saastamoinen', \\%n");
//                out.format(Locale.US, "     $tabulated_w with lines dt 3 title 'tabulated', \\%n");
//                out.format(Locale.US, "     $modified_hopfield_w with linespoints pt 12 dt 3 title 'modified Hopfield', \\%n");
//                out.format(Locale.US, "     $marini_murray_w with lines dt 2 title 'Marini-Murray (optical at 694.3nm)', \\%n");
//                out.format(Locale.US, "     $mendes_pavlis_w with lines dt 2 title 'Mendes-Pavlis (optical at 532nm)'%n");
//                out.format(Locale.US, "pause mouse close%n");
//            }
    }
    private void print(final TroposphericModel tm, final PrintStream out, final String name, final GeodeticPoint point, final PressureTemperatureHumidity weather, final AbsoluteDate date) {
        out.format(Locale.US, "%s_d <<EOD%n", name);
        for (double e = 5; e <= 20; e += 0.25) {
            out.format(Locale.US, "%.6f %.6f%n",
                       e,
                       tm.pathDelay(new TrackingCoordinates(0, FastMath.toRadians(e), 1.4e6), point, weather,
                                    tm.getParameters(date), date).getSh());
        }
        out.format(Locale.US, "EOD%n");
        out.format(Locale.US, "%s_w <<EOD%n", name);
        for (double e = 5; e <= 20; e += 0.25) {
            out.format(Locale.US, "%.6f %.6f%n",
                       e,
                       tm.pathDelay(new TrackingCoordinates(0, FastMath.toRadians(e), 1.4e6), point, weather,
                                    tm.getParameters(date), date).getSw());
        }
        out.format(Locale.US, "EOD%n");
    }

}
