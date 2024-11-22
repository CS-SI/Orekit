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

import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.data.DataSource;
import org.orekit.models.earth.troposphere.TroposphericModel;
import org.orekit.models.earth.troposphere.TroposphericModelUtils;
import org.orekit.models.earth.weather.GlobalPressureTemperature3;
import org.orekit.models.earth.weather.PressureTemperatureHumidity;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.TrackingCoordinates;
import org.orekit.utils.units.Unit;

import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class SeasonalGridTest extends AbstractGridTest<SeasonalGrid> {

    @Test
    @Override
    public void testMetadata() {
        doTestMetadata(new SeasonalGrid(TroposphericModelUtils.HECTO_PASCAL,
                                        "pres_gd_a1.dat", "pres_gd_a2.dat", "pres_gd_a3.dat"),
                       -90.0, 90.0, 121, -180.0, 180.0, 241);
    }

    @Test
    @Override
    public void testMinMax() {
        doTestMinMax(new SeasonalGrid(TroposphericModelUtils.HECTO_PASCAL,
                                      "pres_gd_a1.dat", "pres_gd_a2.dat", "pres_gd_a3.dat"),
                     62197.016, 669552.249, 1.0e-3);
    }

    @Test
    @Override
    public void testValue() {
        doTestValue(new SeasonalGrid(TroposphericModelUtils.HECTO_PASCAL,
                                     "pres_gd_a1.dat", "pres_gd_a2.dat", "pres_gd_a3.dat"),
                    new GeodeticPoint(FastMath.toRadians(47.71675), FastMath.toRadians(6.12264), 300.0),
                    12.5 * Constants.JULIAN_DAY, 124296.222, 1.0e-3);
    }

    @Test
    @Override
    public void testGradient() {
        doTestGradient(new SeasonalGrid(TroposphericModelUtils.HECTO_PASCAL,
                                        "pres_gd_a1.dat", "pres_gd_a2.dat", "pres_gd_a3.dat"),
                       new GeodeticPoint(FastMath.toRadians(47.71675), FastMath.toRadians(6.12264), 300.0),
                       12.5 * Constants.JULIAN_DAY, 1.0e-12, 1.1e-5);
    }

    @Test
    public void testTmp() throws IOException, URISyntaxException {
        final String par = "tmpm";
       SeasonalGrid grid = new SeasonalGrid(Unit.NONE,
                                            par + "_gd_a1.dat", par + "_gd_a2.dat", par + "_gd_a3.dat");
            final ProcessBuilder pb = new ProcessBuilder("gnuplot").
                            redirectOutput(ProcessBuilder.Redirect.INHERIT).
                            redirectError(ProcessBuilder.Redirect.INHERIT);
            pb.environment().remove("XDG_SESSION_TYPE");
            final Process gnuplot = pb.start();
            try (PrintStream out = new PrintStream(gnuplot.getOutputStream(), false, StandardCharsets.UTF_8.name())) {
                out.format(Locale.US, "set terminal qt size %d, %d title '%s'%n", 1000, 1000, par);
                //out.format(Locale.US, "set terminal pngcairo size %d, %d%n", 1000, 1000);
                //out.format(Locale.US, "set output '/tmp/itu-r.p834.png'%n");
                out.format(Locale.US, "set xrange [-180:180]%n");
                out.format(Locale.US, "set xtics 30%n");
                out.format(Locale.US, "set yrange [-90:90]%n");
                out.format(Locale.US, "set ytics 15%n");
                out.format(Locale.US, "set xlabel 'longitude (°)'%n");
                out.format(Locale.US, "set ylabel 'latitude (m)'%n");
                out.format(Locale.US, "$grid <<EOD%n");
                for (int i = 0; i < grid.getLongitudeAxis().size(); ++i) {
                    final double lon = grid.getLongitudeAxis().node(i);
                    if (i > 0) {
                        out.format(Locale.US, "%n");
                    }
                    for (int j = 0; j < grid.getLatitudeAxis().size(); ++j) {
                        final double lat = grid.getLatitudeAxis().node(j);
                        out.format(Locale.US, "%f %f %f%n",
                                   FastMath.toDegrees(lat),
                                   FastMath.toDegrees(lon),
                                   grid.getCell(new GeodeticPoint(lat, lon, 0), 12.5).evaluate());
                    }
                    out.format(Locale.US, "%n");
                }
                out.format(Locale.US, "EOD%n");
                out.format(Locale.US, "plot $grid using 2:1:3 with image%n");
                out.format(Locale.US, "pause mouse close%n");
            }
    }

}
