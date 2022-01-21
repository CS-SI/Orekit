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
package eu.csgroup.examples.gnss;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

import org.hipparchus.exception.LocalizedCoreFormats;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.bodies.CelestialBody;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.data.DataContext;
import org.orekit.data.DataProvidersManager;
import org.orekit.data.DataSource;
import org.orekit.data.DirectoryCrawler;
import org.orekit.data.UnixCompressFilter;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.sp3.SP3File;
import org.orekit.files.sp3.SP3Parser;
import org.orekit.files.sp3.SP3File.SP3Ephemeris;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.Transform;
import org.orekit.gnss.SatelliteSystem;
import org.orekit.gnss.antenna.AntexLoader;
import org.orekit.gnss.antenna.SatelliteAntenna;
import org.orekit.propagation.BoundedPropagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.AggregateBoundedPropagator;
import org.orekit.propagation.sampling.OrekitFixedStepHandler;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.GNSSDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.TimeSpanMap;


public class GenerateBaseSamples {

    /** Option for orekit data directory. */
    private static final String OPTION_OREKIT_DATA_DIR = "-orekit-data-dir";

    /** Option for SP3 files directory. */
    private static final String OPTION_SP3_DIR = "-sp3-dir";

    /** Option for output directory. */
    private static final String OPTION_OUTPUT_DIR = "-output-dir";

    /** Option for antex file. */
    private static final String OPTION_ANTEX_FILE = "-antex";

    /** Option for meta-data file. */
    private static final String OPTION_METADATA_FILE = "-meta-data";

    public static void main(String[] args) {
        try {
            File orekitDataDir = null;
            File sp3Dir        = null;
            File outputDir     = null;
            File antexDir      = null;
            String antexName   = null;
            File metadataFile  = null;
            for (int i = 0; i < args.length - 1; ++i) {
                switch (args[i]) {
                    case OPTION_OREKIT_DATA_DIR :
                        orekitDataDir = new File(args[++i]);
                        break;
                    case OPTION_SP3_DIR :
                        sp3Dir   = new File(args[++i]);
                        break;
                    case OPTION_OUTPUT_DIR :
                        outputDir   = new File(args[++i]);
                        break;
                    case OPTION_ANTEX_FILE : {
                        final File fullPath = new File(args[++i]);
                        antexDir  = fullPath.getParentFile();
                        antexName = fullPath.getName();
                        break;
                    }
                    case OPTION_METADATA_FILE : {
                        metadataFile = new File(args[++i]);
                        break;
                    }
                    default :
                        throw new OrekitException(LocalizedCoreFormats.SIMPLE_MESSAGE,
                                                  "unknown option " + args[i]);

                }
            }
            if (orekitDataDir == null || sp3Dir == null || outputDir == null || antexDir == null) {
                throw new OrekitException(LocalizedCoreFormats.SIMPLE_MESSAGE,
                                          "usage: java fr.cs.examples.gnss.GenerateBaseSamples " +
                                          OPTION_OREKIT_DATA_DIR + " <directory> " +
                                          OPTION_SP3_DIR + " <directory> " +
                                          OPTION_ANTEX_FILE + " <file>" +
                                          OPTION_METADATA_FILE + " <file>");
            }
            for (File directory : Arrays.asList(orekitDataDir, sp3Dir, outputDir, antexDir)) {
                if (!directory.exists() || !directory.isDirectory()) {
                    throw new OrekitException(LocalizedCoreFormats.SIMPLE_MESSAGE,
                                              directory + " does not exist or is not a directory");
                }
            }
            if (!metadataFile.exists() || metadataFile.isDirectory()) {
                throw new OrekitException(LocalizedCoreFormats.SIMPLE_MESSAGE,
                                          metadataFile + " does not exist or is a directory");
            }

            final DataProvidersManager manager = DataContext.getDefault().getDataProvidersManager();
            manager.addProvider(new DirectoryCrawler(orekitDataDir));
            manager.addProvider(new DirectoryCrawler(antexDir));
            final AntexLoader loader = new AntexLoader(antexName);
            final CelestialBody sun = CelestialBodyFactory.getSun();
            final TimeScale gps = TimeScalesFactory.getGPS();

            final Set<String> overridden = new TreeSet<>();
            final String header = "# GPS date week   milliseconds" +
                            "   Id    type     satCode" +
                            "    PxSat (m)         PySat (m)         PzSat (m)       VxSat (m/s)       VySat (m/s)       VZsat (m/s)" +
                            "        PxSun (m)         PySun (m)         PzSun (m)         β (deg)        Δ (deg)" +
                            "           xsatX (nominal)        ysatX (nominal)        zsatX (nominal)     ψ nom. (deg)" +
                            "         xsatX (eclips)         ysatX (eclips)         zsatX (eclips)      ψ ecl. (deg)";

            final String csn = StandardCharsets.UTF_8.name();
            try (final InputStream    is = new FileInputStream(metadataFile);
                 final Reader         r  = new InputStreamReader(is, csn);
                 final BufferedReader br = new BufferedReader(r)) {
                for (String line = br.readLine(); line != null; line = br.readLine()) {
                    if (line.startsWith("#")) {
                        continue;
                    }
                    String[] fields = line.trim().split("\\s+");

                    // extract metadata for the current event
                    final String       id      = fields[0];
                    final String       type    = fields[1];
                    final String       satCode = fields[2];
                    final String       useCase = fields[3];
                    final AbsoluteDate start   = new AbsoluteDate(fields[4], gps);
                    final AbsoluteDate end     = new AbsoluteDate(fields[5], gps);

                    // aggregate the SP3 files for the current event
                    final List<BoundedPropagator> propagators = new ArrayList<>(fields.length - 6);
                    for (int i = 6; i < fields.length; ++i) {
                        final File f = new File(sp3Dir, fields[i]);
                        final DataSource   compressed   = new DataSource(f.getName(), () -> new FileInputStream(f));
                        final DataSource   uncompressed = new UnixCompressFilter().filter(compressed);
                        final SP3File      sp3          = new SP3Parser().parse(uncompressed);
                        final SP3Ephemeris ephemeris    = sp3.getSatellites().get(id);
                        propagators.add(ephemeris.getPropagator());
                    }
                    BoundedPropagator propagator = new AggregateBoundedPropagator(propagators);

                    System.out.format(Locale.US, "  %3s %-11s  %-4s    %s   %s   %s%n",
                                      id, type, satCode, useCase, start, end);

                    final String  outName = mapUseCase(useCase) + "-" + mapType(type) + ".txt";
                    final File    outFile = new File(outputDir, outName);
                    final boolean append  = overridden.contains(outName);
                    overridden.add(outName);
                    try (final FileWriter  fw  = new FileWriter(outFile, append);
                         final PrintWriter out = new PrintWriter(fw)) {
                        if (!append) {
                            out.format(Locale.US, "%s%n", header);
                        }
                        final SatelliteSystem system = SatelliteSystem.parseSatelliteSystem(id.substring(0, 1));
                        final TimeSpanMap<SatelliteAntenna> map =
                                        loader.findSatelliteAntenna(system, Integer.parseInt(id.substring(1)));
                        final SatelliteAntenna antenna = map.get(propagator.getMinDate());
                        final double step = end.durationFrom(start) > Constants.JULIAN_DAY ? 3600.0 : 6 * 60.0;
                        propagator.setMasterMode(step, new Handler(sun, antenna, out, id));
                        propagator.propagate(start, end);
                    } catch (OrekitException oe) {
                        if (oe.getSpecifier() != OrekitMessages.CANNOT_FIND_SATELLITE_IN_SYSTEM) {
                            oe.printStackTrace();
                            System.err.println("# unable to propagate " + id);
                        }
                    }
                }
            }
        } catch (OrekitException | IOException e) {
            System.err.println(e.getLocalizedMessage());
        }
    }

    private static String mapUseCase(final String useCase) {
        switch (useCase) {
            case "β≪0" :
                return "beta-large-negative";
            case "β<0" :
                return "beta-small-negative";
            case "β>0" :
                return "beta-small-positive";
            case "β≫0" :
                return "beta-large-positive";
            default :
                return "beta-crossing";
        }
    }

    private static String mapType(final String type) {
        if (type.matches("^BLOCK-IIR-\\w+$") || // map types like BLOCK-IIR-A into BLOCK-IIR
            type.matches("^GLONASS-\\w+$")   || // map types like GLONASS-M into GLONASS
            type.matches("^GALILEO-\\w+$")) {   // map types like GALILEO-2 into GALILEO
            return type.substring(0, type.lastIndexOf('-'));
        } else {
            // preserve types like BLOCK-IIF
            return type;
        }
    }

    private static class Handler implements OrekitFixedStepHandler {

        final CelestialBody     sun;
        final String            sat;
        final SatelliteAntenna  antenna;
        final PrintWriter       out;
        final TimeScale         gps;
        final Frame             itrf;

        Handler(final CelestialBody sun, final SatelliteAntenna antenna,
                final PrintWriter out, final String sat) {
            this.sun     = sun;
            this.sat     = sat;
            this.antenna = antenna;
            this.out     = out;
            this.gps     = TimeScalesFactory.getGPS();
            this.itrf    = FramesFactory.getITRF(IERSConventions.IERS_2010, false);
        }

        @Override
        public void handleStep(final SpacecraftState s, final boolean isLast) {
            GNSSDate      gpsDate    = new GNSSDate(s.getDate(), SatelliteSystem.GPS);
            PVCoordinates pvSatInert = s.getPVCoordinates();
            Transform     t          = s.getFrame().getTransformTo(itrf, s.getDate());
            Vector3D      pSat       = t.transformPosition(pvSatInert.getPosition());
            Vector3D      vSat       = t.transformVector(pvSatInert.getVelocity());
            Vector3D      pSun       = t.transformPosition(sun.getPVCoordinates(s.getDate(), s.getFrame()).getPosition());
            out.format(Locale.US,
                       "%s %4d %16.6f %3s %-11s  %-4s" +
                       " %16.6f  %16.6f  %16.6f %16.9f  %16.9f  %16.9f" +
                       "  %16.2f  %16.2f  %16.2f" +
                       " %15.11f %15.11f%n",
                       s.getDate().getComponents(gps).getDate(), gpsDate.getWeekNumber(), gpsDate.getMilliInWeek(),
                       sat, antenna.getType().replaceAll(" ", "-"), sat.substring(0, 1) + antenna.getSatelliteCode(),
                       pSat.getX(), pSat.getY(), pSat.getZ(), vSat.getX(), vSat.getY(), vSat.getZ(),
                       pSun.getX(), pSun.getY(), pSun.getZ(),
                       beta(s), delta(s));
        }

        private double beta(final SpacecraftState s) {
            final Vector3D pSun = sun.getPVCoordinates(s.getDate(), s.getFrame()).getPosition();
            final Vector3D mSat = s.getPVCoordinates().getMomentum();
            return FastMath.toDegrees(0.5 * FastMath.PI - Vector3D.angle(pSun, mSat));
        }

        private double delta(final SpacecraftState s) {
            final Vector3D pSun = sun.getPVCoordinates(s.getDate(), s.getFrame()).getPosition();
            return FastMath.toDegrees(Vector3D.angle(pSun, s.getPVCoordinates().getPosition()));
        }

    }

}
