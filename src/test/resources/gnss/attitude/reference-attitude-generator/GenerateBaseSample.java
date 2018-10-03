/* Copyright 2002-2018 CS Systèmes d'Information
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
package fr.cs.examples.gnss;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.hipparchus.exception.LocalizedCoreFormats;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.bodies.CelestialBody;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.data.DataProvidersManager;
import org.orekit.data.DirectoryCrawler;
import org.orekit.data.NamedData;
import org.orekit.data.UnixCompressFilter;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.sp3.SP3File;
import org.orekit.files.sp3.SP3File.SP3Ephemeris;
import org.orekit.files.sp3.SP3Parser;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.Transform;
import org.orekit.gnss.SatelliteSystem;
import org.orekit.gnss.antenna.AntexLoader;
import org.orekit.gnss.antenna.SatelliteAntenna;
import org.orekit.propagation.BoundedPropagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.AbstractDetector;
import org.orekit.propagation.events.AlignmentDetector;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.events.handlers.StopOnIncreasing;
import org.orekit.time.GPSDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.PVCoordinatesProvider;
import org.orekit.utils.TimeSpanMap;


public class GenerateBaseSample {

    /** Option for orekit data directory. */
    private static final String OPTION_OREKIT_DATA_DIR = "-orekit-data-dir";

    /** Option for SP3 files directory. */
    private static final String OPTION_SP3_DIR = "-sp3-dir";

    /** Option for output directory. */
    private static final String OPTION_OUTPUT_DIR = "-output-dir";

    /** Option for antex file. */
    private static final String OPTION_ANTEX_FILE = "-antex";

    public static void main(String[] args) {
        try {
            File orekitDataDir = null;
            File sp3Dir        = null;
            File outputDir     = null;
            File antexDir      = null;
            String antexName   = null;
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
                    default :
                        throw new OrekitException(LocalizedCoreFormats.SIMPLE_MESSAGE,
                                                  "unknown option " + args[i]);

                }
            }
            if (orekitDataDir == null || sp3Dir == null || outputDir == null || antexDir == null) {
                throw new OrekitException(LocalizedCoreFormats.SIMPLE_MESSAGE,
                                          "usage: java fr.cs.examples.gnss.GenerateBaseSample " +
                                          OPTION_OREKIT_DATA_DIR + " <directory> " +
                                          OPTION_SP3_DIR + " <directory> " +
                                          OPTION_ANTEX_FILE + " <file>");
            }
            for (File directory : Arrays.asList(orekitDataDir, sp3Dir, outputDir, antexDir)) {
                if (!directory.exists() || !directory.isDirectory()) {
                    throw new OrekitException(LocalizedCoreFormats.SIMPLE_MESSAGE,
                                              directory + " does not exist or is not a directory");
                }
            }

            DataProvidersManager.getInstance().addProvider(new DirectoryCrawler(orekitDataDir));
            DataProvidersManager.getInstance().addProvider(new DirectoryCrawler(antexDir));
            final AntexLoader loader = new AntexLoader(antexName);
            final CelestialBody sun = CelestialBodyFactory.getSun();

            // find the available Unix-compressed sp3 files in lexicographic order
            // (which is here chronological order too)
            final List<String> sp3Names = Arrays.asList(sp3Dir.list()).
                                                 stream().
                                                 filter(name -> name.endsWith(".sp3.Z")).
                                                 collect(Collectors.toList());
            Collections.sort(sp3Names);

            try (PrintStream outLargeNeg = new PrintStream(new File(outputDir, "beta-large-negative.txt"));
                 PrintStream outSmallNeg = new PrintStream(new File(outputDir, "beta-small-negative.txt"));
                 PrintStream outCrossing = new PrintStream(new File(outputDir, "beta-crossing.txt"));
                 PrintStream outSmallPos = new PrintStream(new File(outputDir, "beta-small-positive.txt"));
                 PrintStream outLargePos = new PrintStream(new File(outputDir, "beta-large-positive.txt"));) {
                final String header = "# GPS date week   milliseconds" +
                                "   Id    type     satCode" +
                                "    PxSat (m)         PySat (m)         PzSat (m)       VxSat (m/s)       VySat (m/s)       VZsat (m/s)" +
                                "        PxSun (m)         PySun (m)         PzSun (m)         β (deg)        Δ (deg)" +
                                "         xsatX (nominal)      ysatX (nominal)      zsatX (nominal)     ψ nom. (deg)" +
                                "       xsatX (eclips)       ysatX (eclips)       zsatX (eclips)      ψ ecl. (deg)%n";
                outLargeNeg.format(Locale.US, header);            
                outSmallNeg.format(Locale.US, header);             
                outCrossing.format(Locale.US, header);            
                outSmallPos.format(Locale.US, header);            
                outLargePos.format(Locale.US, header);            
                for (String sp3Name : sp3Names) {
                    System.out.println("     " + sp3Name);
                    final File f = new File(sp3Dir, sp3Name);
                    final NamedData compressed = new NamedData(sp3Name, ()-> new FileInputStream(f));

                    try (InputStream is = new UnixCompressFilter().filter(compressed).getStreamOpener().openStream()) {
                        final SP3File sp3 = new SP3Parser().parse(is);
                        for (final Map.Entry<String, SP3Ephemeris> entry : sp3.getSatellites().entrySet()) {
                            final String sat = entry.getKey();
                            try {
                                final SatelliteSystem system = SatelliteSystem.parseSatelliteSystem(sat.substring(0, 1));
                                final TimeSpanMap<SatelliteAntenna> map =
                                                loader.findSatelliteAntenna(system, Integer.parseInt(sat.substring(1)));
                                final SatelliteAntenna antenna = map.get(sp3.getEpoch());
                                final BoundedPropagator propagator = entry.getValue().getPropagator();
                                if ("BEIDOU-2M".equals(antenna.getType()) || "BEIDOU-2I".equals(antenna.getType())) {
                                    // for Beidou MEO and IGSO, we are only interested in large β and (more importantly) in β = ±4°
                                    propagator.addEventDetector(new BetaDetector(900.0, 1.0, sun, -19.0).
                                                                withHandler(new Handler<>(sun, antenna, -19.0, outLargeNeg, sat,
                                                                                          48, 3 * 3600.0)));
                                    propagator.addEventDetector(new BetaDetector(900.0, 1.0, sun, -4.0).
                                                                withHandler(new Handler<>(sun, antenna, -4.0, outSmallNeg, sat,
                                                                                          48, 3 * 3600.0)));
                                    propagator.addEventDetector(new BetaDetector(900.0, 1.0, sun, +4.0).
                                                                withHandler(new Handler<>(sun, antenna, +4.0, outSmallPos, sat,
                                                                                          48, 3 * 3600.0)));
                                    propagator.addEventDetector(new BetaDetector(900.0, 1.0, sun, +30.0).
                                                                withHandler(new Handler<>(sun, antenna, +30.0, outLargePos, sat,
                                                                                          48, 3 * 3600.0)));
                                } else {
                                    // for other satellites, we are interested in noon/midnight turns
                                    propagator.addEventDetector(new AlignmentDetector(900.0, 1.0, sun, 0.0).
                                                                withHandler(new Handler<>(sun, antenna, -19.0, outLargeNeg, sat,
                                                                                          14, 6 * 60.0)));
                                    propagator.addEventDetector(new AlignmentDetector(900.0, 1.0, sun, 0.0).
                                                                withHandler(new Handler<>(sun, antenna,  -1.5, outSmallNeg, sat,
                                                                                          14, 6 * 60.0)));
                                    propagator.addEventDetector(new AlignmentDetector(900.0, 1.0, sun, 0.0).
                                                                withHandler(new Handler<>(sun, antenna,   0.0, outCrossing, sat,
                                                                                          14, 6 * 60.0)));
                                    propagator.addEventDetector(new AlignmentDetector(900.0, 1.0, sun, 0.0).
                                                                withHandler(new Handler<>(sun, antenna,  +1.5, outSmallPos, sat,
                                                                                          14, 6 * 60.0)));
                                    propagator.addEventDetector(new AlignmentDetector(900.0, 1.0, sun, 0.0).
                                                                withHandler(new Handler<>(sun, antenna, +30.0, outLargePos, sat,
                                                                                          14, 6 * 60.0)));
                                }
                                propagator.propagate(propagator.getMinDate().shiftedBy( 10),
                                                     propagator.getMaxDate().shiftedBy(-10));
                            } catch (OrekitException oe) {
                                if (oe.getSpecifier() != OrekitMessages.CANNOT_FIND_SATELLITE_IN_SYSTEM) {
                                    System.err.println("# unable to propagate " + sat);
                                }
                            }
                        }
                    }
                }
            }
        } catch (OrekitException | IOException e) {
            System.err.println(e.getLocalizedMessage());
        }
    }

    private static class BetaDetector extends AbstractDetector<BetaDetector> {

        private static final long serialVersionUID = 20181003L;
        private final PVCoordinatesProvider sun;
        private final double targetAngle;

        BetaDetector(final double maxCheck, final double threshold,
                     final PVCoordinatesProvider sun,
                     final double targetAngleDeg) {
            this(maxCheck, threshold, DEFAULT_MAX_ITER,
                 new StopOnIncreasing<BetaDetector>(),
                 sun, FastMath.toRadians(targetAngleDeg));
        }

        private BetaDetector(final double maxCheck, final double threshold,
                             final int maxIter, final EventHandler<? super BetaDetector> handler,
                             final PVCoordinatesProvider sun,
                             final double targetAngle) {
                super(maxCheck, threshold, maxIter, handler);
            this.sun         = sun;
            this.targetAngle = targetAngle;
        }

        /** {@inheritDoc} */
        @Override
        protected BetaDetector create(final double newMaxCheck, final double newThreshold,
                                      final int newMaxIter, final EventHandler<? super BetaDetector> newHandler) {
            return new BetaDetector(newMaxCheck, newThreshold, newMaxIter, newHandler, sun, targetAngle);
        }

        /** {@inheritDoc} */
        @Override
        public double g(final SpacecraftState s) {
            final Vector3D pSun = sun.getPVCoordinates(s.getDate(), s.getFrame()).getPosition();
            final Vector3D mSat = s.getPVCoordinates().getMomentum();
            final double beta = 0.5 * FastMath.PI - Vector3D.angle(pSun, mSat);
            return beta - targetAngle;
        }

    }

    private static class Handler<T extends EventDetector> implements EventHandler<T> {
        final CelestialBody     sun;
        final double            betaRef;
        final String            sat;
        final SatelliteAntenna  antenna;
        final PrintStream       out;
        final TimeScale         gps;
        final Frame             itrf;
        final int               nbPoints;
        final double            step;

        Handler(final CelestialBody sun, final SatelliteAntenna antenna,
                final double betaRef, final PrintStream out, final String sat,
                final int nbPoints, final double step) {
            this.sun      = sun;
            this.betaRef  = betaRef;
            this.sat      = sat;
            this.antenna  = antenna;
            this.out      = out;
            this.gps      = TimeScalesFactory.getGPS();
            this.itrf     = FramesFactory.getITRF(IERSConventions.IERS_2010, false);
            this.nbPoints = nbPoints;
            this.step     = step;
        }

        @Override
        public Action eventOccurred(SpacecraftState s, T detector, boolean increasing) {
            if (FastMath.abs(beta(s) - betaRef) < 0.5 &&
                (beta(s.shiftedBy(-1800)) - betaRef) * (beta(s.shiftedBy(+1800)) - betaRef) <= 0) {
                for (int i = 0; i < nbPoints; ++i) {
                    display(s.shiftedBy((2 * i + 1 - nbPoints) * step / 2), antenna);
                }
            }
            return Action.CONTINUE;
        }

        private void display(final SpacecraftState s, final SatelliteAntenna antenna) {
            GPSDate gpsDate = new GPSDate(s.getDate());
            PVCoordinates pvSatInert = s.getPVCoordinates();
            Transform t = s.getFrame().getTransformTo(itrf, s.getDate());
            Vector3D pSat = t.transformPosition(pvSatInert.getPosition());
            Vector3D vSat = t.transformVector(pvSatInert.getVelocity());
            Vector3D pSun = t.transformPosition(sun.getPVCoordinates(s.getDate(), s.getFrame()).getPosition());
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
