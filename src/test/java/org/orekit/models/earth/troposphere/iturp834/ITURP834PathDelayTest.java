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
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.frames.FramesFactory;
import org.orekit.models.earth.troposphere.AbstractPathDelayTest;
import org.orekit.models.earth.troposphere.AskneNordiusModel;
import org.orekit.models.earth.troposphere.CanonicalSaastamoinenModel;
import org.orekit.models.earth.troposphere.ChaoMappingFunction;
import org.orekit.models.earth.troposphere.ConstantAzimuthalGradientProvider;
import org.orekit.models.earth.troposphere.ConstantTroposphericModel;
import org.orekit.models.earth.troposphere.ConstantViennaAProvider;
import org.orekit.models.earth.troposphere.GlobalMappingFunctionModel;
import org.orekit.models.earth.troposphere.MendesPavlisModel;
import org.orekit.models.earth.troposphere.ModifiedSaastamoinenModel;
import org.orekit.models.earth.troposphere.NiellMappingFunctionModel;
import org.orekit.models.earth.troposphere.RevisedChaoMappingFunction;
import org.orekit.models.earth.troposphere.TroposphereMappingFunction;
import org.orekit.models.earth.troposphere.TroposphericDelay;
import org.orekit.models.earth.troposphere.TroposphericModel;
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
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.TrackingCoordinates;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class ITURP834PathDelayTest extends AbstractPathDelayTest<ITURP834PathDelay> {

    protected ITURP834PathDelay buildTroposphericModel() {
        return new ITURP834PathDelay(VerticalExcessPath.NON_COASTAL_NON_EQUATORIAL, new CIPM2007(),
                                     new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                          Constants.WGS84_EARTH_FLATTENING,
                                                          FramesFactory.getITRF(IERSConventions.IERS_2010, true)),
                                     TimeScalesFactory.getUTC());
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
    public void testTmp() throws IOException {
        final AbsoluteDate date = new AbsoluteDate(1994, 1, 1, TimeScalesFactory.getUTC());

        final double latitude    = FastMath.toRadians(48.0);
        final double longitude   = FastMath.toRadians(0.20);
        final double height      = 68.0;
        final GeodeticPoint point = new GeodeticPoint(latitude, longitude, height);

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
                print(buildTroposphericModel(), out, "$itu", 0, point,
                      TroposphericModelUtils.STANDARD_ATMOSPHERE, date);
                print(new CanonicalSaastamoinenModel(), out, "$canonical_saastamoinen", 0, point,
                      TroposphericModelUtils.STANDARD_ATMOSPHERE, date);
                print(new ModifiedSaastamoinenModel(TroposphericModelUtils.STANDARD_ATMOSPHERE_PROVIDER), out, "$modified_saastamoinen", 0, point,
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
                print(new AskneNordiusModel(new ChaoMappingFunction()), out, "$askne_nordius", 0, point,
                      TroposphericModelUtils.STANDARD_ATMOSPHERE, date);
                out.format(Locale.US, "plot $itu with lines title 'ITU', \\%n");
                out.format(Locale.US, "     $canonical_saastamoinen with lines title 'canonical Saastamoinen', \\%n");
                out.format(Locale.US, "     $modified_saastamoinen with lines title 'modified Saastamoinen', \\%n");
                out.format(Locale.US, "     $vienna_1 with lines title 'Vienna 1', \\%n");
                out.format(Locale.US, "     $vienna_3 with lines title 'Vienna 3', \\%n");
                out.format(Locale.US, "     $$askne_nordius with lines title 'Askne-Nordius'%n");
                out.format(Locale.US, "pause mouse close%n");
            }
    }
    private void print(final TroposphericModel tm, final PrintStream out, final String name, final int index,
                       final GeodeticPoint point, final PressureTemperatureHumidity weather, final AbsoluteDate date) {
        out.format(Locale.US, "%s <<EOD%n", name);
        for (double e = 0; e < FastMath.toRadians(20); e += 0.001) {
            out.format(Locale.US, "%.6f %.6f%n",
                       FastMath.toDegrees(e),
                       tm.pathDelay(new TrackingCoordinates(0, e, 1.4e6), point, weather,
                                    tm.getParameters(date), date).getDelay());
        }
        out.format(Locale.US, "EOD%n");
    }

}
