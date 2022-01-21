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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.hipparchus.exception.LocalizedCoreFormats;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.events.Action;
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
import org.orekit.files.sp3.SP3File.SP3Ephemeris;
import org.orekit.files.sp3.SP3Parser;
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
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinatesProvider;
import org.orekit.utils.TimeSpanMap;


public class FindBaseSamples {

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
                                          "usage: java fr.cs.examples.gnss.FindBaseSamples " +
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

            final DataProvidersManager manager = DataContext.getDefault().getDataProvidersManager();
            manager.addProvider(new DirectoryCrawler(orekitDataDir));
            manager.addProvider(new DirectoryCrawler(antexDir));
            final AntexLoader loader = new AntexLoader(antexName);
            final CelestialBody sun = CelestialBodyFactory.getSun();

            // find the available Unix-compressed sp3 files in lexicographic order
            // (which is here chronological order too)
            final List<String> sp3Names = Arrays.asList(sp3Dir.list()).
                                                 stream().
                                                 filter(name -> name.endsWith(".sp3.Z")).
                                                 collect(Collectors.toList());
            Collections.sort(sp3Names);
            final TimeSpanMap<File> sp3ByDate = new TimeSpanMap<File>(null);
            final List<SampleMetaData> samples = new ArrayList<>();

            for (String sp3Name : sp3Names) {
                System.out.println("     " + sp3Name);
                final File f = new File(sp3Dir, sp3Name);
                final DataSource compressed   = new DataSource(sp3Name, () -> new FileInputStream(f));
                final DataSource uncompressed = new UnixCompressFilter().filter(compressed);

                final SP3File sp3 = new SP3Parser().parse(uncompressed);
                sp3ByDate.addValidAfter(f, sp3.getEpoch());
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
                                                        withHandler(new Handler<>(sun, antenna, -19.0, samples,
                                                                        sat, "β≪0", 6 * Constants.JULIAN_DAY)));
                            propagator.addEventDetector(new BetaDetector(900.0, 1.0, sun, -4.0).
                                                        withHandler(new Handler<>(sun, antenna, -4.0, samples,
                                                                        sat, "β<0", 6 * Constants.JULIAN_DAY)));
                            propagator.addEventDetector(new BetaDetector(900.0, 1.0, sun, +4.0).
                                                        withHandler(new Handler<>(sun, antenna, +4.0, samples,
                                                                        sat, "β>0", 6 * Constants.JULIAN_DAY)));
                            propagator.addEventDetector(new BetaDetector(900.0, 1.0, sun, +30.0).
                                                        withHandler(new Handler<>(sun, antenna, +30.0, samples,
                                                                        sat, "β≫0", 6 * Constants.JULIAN_DAY)));
                        } else {
                            // for other satellites, we are interested in noon/midnight turns
                            propagator.addEventDetector(new AlignmentDetector(900.0, 1.0, sun, 0.0).
                                                        withHandler(new Handler<>(sun, antenna, -19.0, samples,
                                                                        sat, "β≪0", 90 * 60.0)));
                            propagator.addEventDetector(new AlignmentDetector(900.0, 1.0, sun, 0.0).
                                                        withHandler(new Handler<>(sun, antenna, -1.5,  samples,
                                                                        sat, "β<0", 90 * 60.0)));
                            propagator.addEventDetector(new AlignmentDetector(900.0, 1.0, sun, 0.0).
                                                        withHandler(new Handler<>(sun, antenna, 0.0,   samples,
                                                                        sat, "β≈0", 90 * 60.0)));
                            propagator.addEventDetector(new AlignmentDetector(900.0, 1.0, sun, 0.0).
                                                        withHandler(new Handler<>(sun, antenna, +1.5,  samples,
                                                                        sat, "β>0", 90 * 60.0)));
                            propagator.addEventDetector(new AlignmentDetector(900.0, 1.0, sun, 0.0).
                                                        withHandler(new Handler<>(sun, antenna, +30.0, samples,
                                                                        sat, "β≫0", 90 * 60.0)));
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

            final String csn = StandardCharsets.UTF_8.name();
            try (PrintStream out = new PrintStream(new File(outputDir, "samples-meta-data.txt"), csn)) {
                final TimeScale gps = TimeScalesFactory.getGPS();
                out.format(Locale.US,
                           "#  Id    type     satCode  case" +
                           "         start (GPS)               end (GPS)              SP3 files%n");
                for (final SampleMetaData sample : samples) {
                    out.format(Locale.US, "  %3s %-11s  %-4s    %s   %s   %s  " ,
                               sample.id, sample.type, sample.satCode, sample.useCase,
                               sample.start.toString(gps), sample.end.toString(gps));
                    File previous = null;
                    for (AbsoluteDate date = sample.start;
                         date.compareTo(sample.end) <= 0;
                         date = date.shiftedBy(60.0)) {
                        File current = sp3ByDate.get(date);
                        if (current != null && !current.equals(previous)) {
                            out.format(Locale.US, " %s", current.getName());
                        }
                        previous = current;
                    }
                    out.format(Locale.US, "%n");
                }
            }

        } catch (OrekitException | IOException e) {
            System.err.println(e.getLocalizedMessage());
        }
    }

    private static class BetaDetector extends AbstractDetector<BetaDetector> {

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
        final CelestialBody        sun;
        final SatelliteAntenna     antenna;
        final double               betaRef;
        final List<SampleMetaData> samples;
        final String               sat;
        final String               useCase;
        final double               span;

        Handler(final CelestialBody sun, final SatelliteAntenna antenna, final double betaRef,
                final List<SampleMetaData> samples, final String sat, final String useCase,
                final double span) {
            this.sun      = sun;
            this.antenna  = antenna;
            this.betaRef  = betaRef;
            this.samples  = samples;
            this.sat      = sat;
            this.useCase  = useCase;
            this.span     = span;
        }

        @Override
        public Action eventOccurred(SpacecraftState s, T detector, boolean increasing) {
            if (FastMath.abs(beta(s) - betaRef) < 0.5 &&
                (beta(s.shiftedBy(-1800)) - betaRef) * (beta(s.shiftedBy(+1800)) - betaRef) <= 0) {
                samples.add(new SampleMetaData(sat,
                                               antenna.getType().replaceAll(" ", "-"),
                                               sat.substring(0, 1) + antenna.getSatelliteCode(),
                                               useCase,
                                               s.getDate().shiftedBy(-0.5 * span),
                                               s.getDate().shiftedBy(+0.5 * span)));
            }
            return Action.CONTINUE;
        }

        private double beta(final SpacecraftState s) {
            final Vector3D pSun = sun.getPVCoordinates(s.getDate(), s.getFrame()).getPosition();
            final Vector3D mSat = s.getPVCoordinates().getMomentum();
            return FastMath.toDegrees(0.5 * FastMath.PI - Vector3D.angle(pSun, mSat));
        }

    }

    private static class SampleMetaData {

        private final String id;
        private final String type;
        private final String satCode;
        private final String useCase;
        private final AbsoluteDate start;
        private final AbsoluteDate end;

        SampleMetaData(final String id, final String type, final String satCode, final String useCase,
                       final AbsoluteDate start, final AbsoluteDate end) {
            this.id      = id;
            this.type    = type;
            this.satCode = satCode;
            this.useCase = useCase;
            this.start   = start;
            this.end     = end;
        }

    }

}
