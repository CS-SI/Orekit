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
package fr.cs.examples.gnss;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.commons.math3.util.FastMath;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.frames.FramesFactory;
import org.orekit.gnss.DOP;
import org.orekit.gnss.DOPComputer;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.propagation.analytical.tle.TLEPropagator;
import org.orekit.propagation.analytical.tle.TLESeries;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;

import fr.cs.examples.Autoconfiguration;

/**
 * Orekit tutorial for DOP computation.
 * <p>This tutorial shows a basic usage of the computation of the DOP for a geographic zone and a period.<p>
 * @author Pascal Parraud
 */
public class DOPComputation {

    /**
     * Program entry point.
     * @param args program arguments (unused here)
     */
    public static void main(String[] args) throws OrekitException, IOException {

        // Configuration d'Orekit
        Autoconfiguration.configureOrekit();

        // Periode de calcul
        final AbsoluteDate dateDeb = new AbsoluteDate(2015, 4, 8, 0, 0, 00.000,
                                                      TimeScalesFactory.getUTC());
        final AbsoluteDate dateFin = dateDeb.shiftedBy(Constants.JULIAN_DAY);
        final double tStep = 600.;

        // Build the Earth body shape
        final OneAxisEllipsoid shape = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                            Constants.WGS84_EARTH_FLATTENING,
                                                            FramesFactory.getITRF(IERSConventions.IERS_2010, true));

        // Zone de calcul

        // Toulouse
        // Nominal ordering (counterclockwise)
//        final double[][] area = new double[][] { {43.652515, 1.425460},
//                                                 {43.613503, 1.387351},
//                                                 {43.568246, 1.417906},
//                                                 {43.566007, 1.488974},
//                                                 {43.643820, 1.470092} };
        // Inverse ordering (clockwise)
        final double[][] area = new double[][] { {43.643820, 1.470092},
                                                 {43.566007, 1.488974},
                                                 {43.568246, 1.417906},
                                                 {43.613503, 1.387351},
                                                 {43.652515, 1.425460} };
        
        final double cellSize  = 1000.;

        // La Corse
//        final double[][] points = new double[][] { { 42.15249,  9.56001 },
//                                                 { 43.00998,  9.39000 },
//                                                 { 42.62812,  8.74600 },
//                                                 { 42.25651,  8.54421 },
//                                                 { 41.58361,  8.77572 },
//                                                 { 41.38000,  9.22975 } };
//        final double cellSize  = 10000.;
        final List<GeodeticPoint> zone = new ArrayList<GeodeticPoint>(area.length);
        for (double[] point: area) {
            zone.add(new GeodeticPoint(FastMath.toRadians(point[0]),
                                       FastMath.toRadians(point[1]),
                                       0.));
        }

        // Récuperation des TLE des GPS opérationnels (fichier gps-ops.txt)
        final TLESeries tleFile = new TLESeries("^gps-ops\\.txt$", true);
        final Set<Integer> scn = tleFile.getAvailableSatelliteNumbers();
        final TLE[] tles = new TLE[scn.size()];
        int i = 0;
        for (int nb: scn) {
            tleFile.loadTLEData(nb);
            tles[i++] = tleFile.getLast();
        }
        final List<Propagator> gnss = new ArrayList<Propagator>(tles.length);
        for (TLE tle : tles) {
            gnss.add(TLEPropagator.selectExtrapolator(tle));
        }

        // Creation du calculateur
        final DOPComputer dop = DOPComputer.create(shape, zone, cellSize);

        // Calcul des DOP sur la zone et sur la periode pour les GPS opérationnels
        final List<List<DOP>> allDop = dop.compute(dateDeb, dateFin, tStep, gnss);

        // Post-traitement
        for (List<DOP> dopAtTime : allDop) {
            final SummaryStatistics pDoP = new SummaryStatistics();
            for (DOP dopAtLoc : dopAtTime) {
                pDoP.addValue(dopAtLoc.getPdop());
            }
            final AbsoluteDate date = dopAtTime.get(0).getDate();
            System.out.println(date.toString() + " - PDOP min = " + pDoP.getMin()
                                               + " - PDOP max = " + pDoP.getMax());
        }
    }

}
