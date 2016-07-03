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
package org.orekit.gnss;

import java.util.List;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.util.FastMath;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.TopocentricFrame;
import org.orekit.propagation.Propagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.ElevationMask;

/**
 * This class aims at computing the dilution of precision.
 *
 * @author Pascal Parraud
 * @since 8.0
 * @see <a href="http://en.wikipedia.org/wiki/Dilution_of_precision_%28GPS%29">Dilution of precision</a>
 */
public class DOPComputer {

    // Constants
    /** Minimum elevation : 0°. */
    public static final double DOP_MIN_ELEVATION = 0.;

    /** Minimum number of propagators for DOP computation. */
    private static final int DOP_MIN_PROPAGATORS = 4;

    // Fields
    /** The location as a topocentric frame. */
    private final TopocentricFrame frame;

    /** Elevation mask used for computation, if defined. */
    private final ElevationMask elevationMask;

    /** Minimum elevation value used if no mask is defined. */
    private final double minElevation;

    /**
     * Constructor for DOP computation.
     *
     * @param frame the topocentric frame linked to the locations where DOP will be computed
     * @param minElev the minimum elevation to consider (rad)
     * @param elevMask the elevation mask to consider
     */
    private DOPComputer(final TopocentricFrame frame, final double minElev, final ElevationMask elevMask) {
        // Set the topocentric frame
        this.frame = frame;
        // Set the min elevation
        this.minElevation = minElev;
        // Set the elevation mask
        this.elevationMask = elevMask;
    }

    /**
     * Creates a DOP computer for one location.
     *
     * <p>A minimum elevation of 0° is taken into account to compute
     * visibility between the location and the GNSS spacecrafts.</p>
     *
     * @param shape the body shape on which the location is defined
     * @param location the point of interest
     * @return a configured DOP computer
     */
    public static DOPComputer create(final OneAxisEllipsoid shape, final GeodeticPoint location) {
        return new DOPComputer(new TopocentricFrame(shape, location, "Location"), DOP_MIN_ELEVATION, null);
    }

    /**
     * Set the minimum elevation.
     *
     * <p>This will override an elevation mask if it has been configured as such previously.</p>
     *
     * @param newMinElevation minimum elevation for visibility (rad)
     * @return a new DOP computer with updated configuration (the instance is not changed)
     *
     * @see #getMinElevation()
     */
    public DOPComputer withMinElevation(final double newMinElevation) {
        return new DOPComputer(frame, newMinElevation, null);
    }

    /**
     * Set the elevation mask.
     *
     * <p>This will override the min elevation if it has been configured as such previously.</p>
     *
     * @param newElevationMask elevation mask to use for the computation
     * @return a new detector with updated configuration (the instance is not changed)
     *
     * @see #getElevationMask()
     */
    public DOPComputer withElevationMask(final ElevationMask newElevationMask) {
        return new DOPComputer(frame, DOP_MIN_ELEVATION, newElevationMask);
    }

    /**
     * Compute the {@link DOP} at a given date for a set of GNSS spacecrafts.
     * <p>Four GNSS spacecraft at least are needed to compute the DOP.
     * If less than 4 propagators are provided, an exception will be thrown.
     * If less than 4 spacecrafts are visible at the date, all DOP values will be
     * set to {@link java.lang.Double#NaN NaN}.</p>
     *
     * @param date the computation date
     * @param gnss the propagators for GNSS spacecraft involved in the DOP computation
     * @return the {@link DOP} at the location
     * @throws OrekitException if something wrong occurs
     */
    public DOP compute(final AbsoluteDate date, final List<Propagator> gnss) throws OrekitException {

        // Checks the number of provided propagators
        if (gnss.size() < DOP_MIN_PROPAGATORS) {
            throw new OrekitException(OrekitMessages.NOT_ENOUGH_GNSS_FOR_DOP, gnss.size(), DOP_MIN_PROPAGATORS);
        }

        // Initializes DOP values
        double gdop = Double.NaN;
        double pdop = Double.NaN;
        double hdop = Double.NaN;
        double vdop = Double.NaN;
        double tdop = Double.NaN;

        // Loop over the propagators of GNSS orbits
        final double[][] satDir = new double[gnss.size()][4];
        int satNb = 0;
        for (Propagator prop : gnss) {
            final Vector3D pos = prop.getPVCoordinates(date, frame).getPosition();
            final double elev  = frame.getElevation(pos, frame, date);
            final double elMin = (elevationMask != null) ?
                                 elevationMask.getElevation(frame.getAzimuth(pos, frame, date)) :
                                 minElevation;
            // Only visible satellites are considered
            if (elev > elMin) {
                // Create the rows of the H matrix
                final Vector3D r = pos.normalize();
                satDir[satNb][0] = r.getX();
                satDir[satNb][1] = r.getY();
                satDir[satNb][2] = r.getZ();
                satDir[satNb][3] = -1.;
                satNb++;
            }
        }

        // DOP values are computed only if at least 4 SV are visible from the location
        if (satNb > 3) {
            // Construct matrix H
            final RealMatrix h = MatrixUtils.createRealMatrix(satNb, 4);
            for (int k = 0; k < satNb; k++) {
                h.setRow(k, satDir[k]);
            }

            // Compute the pseudo-inverse of H
            final RealMatrix hInv = MatrixUtils.inverse(h.transpose().multiply(h));
            final double sx2 = hInv.getEntry(0, 0);
            final double sy2 = hInv.getEntry(1, 1);
            final double sz2 = hInv.getEntry(2, 2);
            final double st2 = hInv.getEntry(3, 3);

            // Extract various DOP : GDOP, PDOP, HDOP, VDOP, TDOP
            gdop = FastMath.sqrt(hInv.getTrace());
            pdop = FastMath.sqrt(sx2 + sy2 + sz2);
            hdop = FastMath.sqrt(sx2 + sy2);
            vdop = FastMath.sqrt(sz2);
            tdop = FastMath.sqrt(st2);
        }

        // Return all the DOP values
        return new DOP(frame.getPoint(), date, satNb, gdop, pdop, hdop, vdop, tdop);
    }

    /**
     * Get the minimum elevation.
     *
     * @return the minimum elevation (rad)
     */
    public double getMinElevation() {
        return minElevation;
    }

    /**
     * Get the elevation mask.
     *
     * @return the elevation mask
     */
    public ElevationMask getElevationMask() {
        return elevationMask;
    }
}
