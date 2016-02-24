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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.geometry.spherical.twod.S2Point;
import org.apache.commons.math3.geometry.spherical.twod.SphericalPolygonsSet;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.util.MathUtils;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.frames.TopocentricFrame;
import org.orekit.models.earth.tessellation.ConstantAzimuthAiming;
import org.orekit.models.earth.tessellation.EllipsoidTessellator;
import org.orekit.models.earth.tessellation.TileAiming;
import org.orekit.propagation.Propagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.ElevationMask;

/**
 * This class aims at computing the dilution of precision.
 * 
 * @see <a href="http://en.wikipedia.org/wiki/Dilution_of_precision_%28GPS%29">Dilution of precision</a>
 *
 * @author pparraud
 */
public class DOPComputer {

    /** Minimum elevation : 0° */
    public final static double DOP_MIN_ELEVATION = 0.;

    /** The list of considered locations as topocentric frames. */
    private final List<TopocentricFrame> frames = new ArrayList<TopocentricFrame>();

    /** Elevation mask used for computation, if defined. */
    private final ElevationMask elevationMask;

    /** Minimum elevation value used if no mask is defined. */
    private final double minElevation;

    /**
     * Constructor for DOP computation.
     *
     * @param frames the topocentric frames linked to the locations where DOP will be computed
     * @param minElev the minimum elevation to consider
     * @param elevMask the elevation mask to consider
     */
    private DOPComputer(final List<TopocentricFrame> frames, final double minElev, final ElevationMask elevMask) {
        // Set the topocentric frames
        this.frames.addAll(frames);
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
        return new DOPComputer(getFrame(shape, location), DOP_MIN_ELEVATION, null);
    }

    /**
     * Creates a DOP computer for a geographic zone.
     *
     * <p>A minimum elevation of 0° is taken into account to compute
     * visibility between the locations and the GNSS spacecrafts.</p>
     *
     * @param shape the body shape on which locations are defined
     * @param locations the points defining the geographic zone of interest
     * @param meshSize the size of the square meshes as a distance on the Earth surface (in meters)
     * @return a configured DOP computer
     * @throws OrekitException if something wrong occurs when sampling the zone
     */
    public static DOPComputer create(final OneAxisEllipsoid shape,
                                     final List<GeodeticPoint> locations,
                                     final double meshSize) throws OrekitException {
        return new DOPComputer(sample(shape, locations, meshSize), DOP_MIN_ELEVATION, null);
    }

    /**
     * Gets the topocentric frame attached to a point defined with respect to a shape.
     *
     * @param shape the shape
     * @param location the location of the point
     * @return the topocentric frame at the location
     */
    private static List<TopocentricFrame> getFrame(final OneAxisEllipsoid shape, final GeodeticPoint location) {
        final List<TopocentricFrame> frames = new ArrayList<TopocentricFrame>();
        frames.add(new TopocentricFrame(shape, location, "Location #1"));
        return frames;
    }

    /**
     * Mesh an area of interest into a grid of topocentric frames based on geodetic points.
     * 
     * @param zone the area to mesh
     * @param meshSize the size of the square meshes as a distance on the Earth surface (in meters)
     * @return a list of topocentric frames based on points sampling the zone of interest
     * @throws OrekitException if the area cannot be meshed
     */
    private static List<TopocentricFrame> sample(final OneAxisEllipsoid shape,
                                                 final List<GeodeticPoint> zone,
                                                 final double meshSize) throws OrekitException {
        // Convert the area into a SphericalPolygonsSet
        final SphericalPolygonsSet sps = computeSphericalPolygonsSet(zone);
    
        // Build the tesselator
        final TileAiming aiming = new ConstantAzimuthAiming(shape, 0.);
        final EllipsoidTessellator tessellator = new EllipsoidTessellator(shape, aiming, 4);

        // Sample the area into a grid of geodetic points
        final List<List<GeodeticPoint>> gridPoints = tessellator.sample(sps, meshSize, meshSize);

        // Convert the double list of geodetic points into a single list of topocentric frames
        final List<TopocentricFrame> frames = new ArrayList<TopocentricFrame>();
        int i = 0;
        for (List<GeodeticPoint> points : gridPoints) {
            for (GeodeticPoint point : points) {
                frames.add(new TopocentricFrame(shape, point, "Location #" + ++i));
            }
        }
    
        // Return the list of topocentric frames
        return frames;
    }

    /**
     * Computes a spherical polygons set from a geographic zone.
     *
     * @param zone the geographic zone
     * @return the spherical polygons set
     */
    private static SphericalPolygonsSet computeSphericalPolygonsSet(final List<GeodeticPoint> zone) {
        // Convert the area into a SphericalPolygonsSet
        final SphericalPolygonsSet sps = computeSPS(zone);
        // If the zone is not defined counterclockwise
        if (sps.getSize() > MathUtils.TWO_PI) {
            // Inverts the order of the points
            final List<GeodeticPoint> zone2 = new ArrayList<GeodeticPoint>(zone.size());
            for (int j = zone.size() - 1; j > -1; j--) {
                zone2.add(zone.get(j));
            }
            return computeSPS(zone2);
        } else {
            return sps;
        }
    }

    /**
     * Computes a spherical polygons set from a geographic zone.
     *
     * @param zone the geographic zone
     * @return the spherical polygons set
     */
    private static SphericalPolygonsSet computeSPS(final List<GeodeticPoint> zone) {
        // Convert the area into a SphericalPolygonsSet
        final S2Point[] vertices = new S2Point[zone.size()];
        int i = 0;
        for (GeodeticPoint point : zone) {
            final double theta = point.getLongitude();
            final double phi   = 0.5 * FastMath.PI - point.getLatitude();
            vertices[i++] = new S2Point(theta, phi);
        }
        return new SphericalPolygonsSet(1.0e-10, vertices);
    }

    /**
     * Setup the minimum elevation for detection.
     *
     * <p>This will override an elevation mask if it has been configured as such previously.</p>
     *
     * @param newMinElevation minimum elevation for visibility in radians (rad)
     * @return a new DOP computer with updated configuration (the instance is not changed)
     *
     * @see #getMinElevation()
     */
    public DOPComputer withConstantElevation(final double newMinElevation) {
        return new DOPComputer(getFrames(), newMinElevation, null);
    }

    /**
     * Setup the elevation mask for detection using the passed in mask object.
     *
     * <p>This will override the min elevation if it has been configured as such previously.</p>
     *
     * @param elevationMask elevation mask to use for the computation
     * @return a new detector with updated configuration (the instance is not changed)
     *
     * @see #getElevationMask()
     */
    public DOPComputer withElevationMask(final ElevationMask elevationMask) {
        return new DOPComputer(getFrames(), DOP_MIN_ELEVATION, elevationMask);
    }

    private List<TopocentricFrame> getFrames() {
        return frames;
    }

    /**
     * Compute the DOP for a period over an area sampled into grid points.
     *
     * @param tdeb start date
     * @param tfin end date
     * @param tstep time step (in seconds)
     * @param gnss the propagators for GNSS spacecraft involved in the DOP computation
     * @return all the DOP at each date and for each grid points
     * @throws OrekitException if something wrong occurs
     */
    public List<List<DOP>> compute(final AbsoluteDate tdeb, final AbsoluteDate tfin, final double tstep,
                                   final List<Propagator> gnss) throws OrekitException {
        // Loop over the dates
        final List<List<DOP>> allDop = new ArrayList<List<DOP>>();
        AbsoluteDate tc = tdeb;
        while (tc.compareTo(tfin) != 1) {
            // Loop over the grid points
            final List<DOP> dopAtDate = compute(frames, tc, gnss);
            allDop.add(dopAtDate);
            tc = tc.shiftedBy(tstep);
        }

        // Return all computed DOP
        return allDop;
    }

    /**
     * Compute the DOP at a given date for a set of GNSS spacecrafts.
     * 
     * @param date the computation date
     * @param gnss the propagators for GNSS spacecraft involved in the DOP computation
     * @return the list of DOP (one for each location)
     * @throws OrekitException if something wrong occurs
     */
    public List<DOP> compute(final AbsoluteDate date, final List<Propagator> gnss) throws OrekitException {
        // Return the DOP
        return compute(getFrames(), date, gnss);
    }

    /**
     * Compute the DOP at a given date for a set of GNSS spacecrafts.
     * 
     * @param date the computation date
     * @param gnss the propagators for GNSS spacecraft involved in the DOP computation
     * @return the list of DOP (one for each location)
     * @throws OrekitException if something wrong occurs
     */
    private List<DOP> compute(final List<TopocentricFrame> frames, final AbsoluteDate date, final List<Propagator> gnss) throws OrekitException {
        final List<DOP> dopZone = new ArrayList<DOP>();
        for (TopocentricFrame frame: frames) {
            final DOP dop = compute(frame, date, gnss);
            dopZone.add(dop);
        }
        return dopZone;
    }

    /**
     * Compute the DOP for a single location on Earth at a given date for a set of GNSS spacecrafts.
     * 
     * @param topoFrame the topocentric frame built on the location
     * @param date the computation date
     * @param gnss the propagators for GNSS spacecraft involved in the DOP computation
     * @return the DOP
     * @throws OrekitException if something wrong occurs
     */
    private DOP compute(final TopocentricFrame topoFrame, final AbsoluteDate date, final List<Propagator> gnss) throws OrekitException {
        // Loop over the propagators of GNSS spacecrafts
        final double[][] satDir = new double[gnss.size()][4];
        int satNb = 0;
        for (Propagator prop : gnss) {
            final Vector3D pos = prop.getPVCoordinates(date, topoFrame).getPosition();
            final double elev  = topoFrame.getElevation(pos, topoFrame, date);
            final double elMin = (elevationMask != null)
                               ? elevationMask.getElevation(topoFrame.getAzimuth(pos, topoFrame, date))
                               : minElevation;
            // Only visible spacecrafts are considered
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

        // Construct matrix H
        final RealMatrix h = MatrixUtils.createRealMatrix(satNb, 4);
        for (int k = 0; k < satNb; k++) {
            h.setRow(k, satDir[k]);
        }
        
        // Compute pseudoinverse of H 
        final RealMatrix hInv = MatrixUtils.inverse(h.transpose().multiply(h));
        final double sx2 = hInv.getEntry(0, 0);
        final double sy2 = hInv.getEntry(1, 1);
        final double sz2 = hInv.getEntry(2, 2);
        final double st2 = hInv.getEntry(3, 3);

        // Extract various DOP : GDOP, PDOP, HDOP, VDOP, TDOP
        final double gdop = FastMath.sqrt(hInv.getTrace()); 
        final double pdop = FastMath.sqrt(sx2 + sy2 + sz2);
        final double hdop = FastMath.sqrt(sx2 + sy2);
        final double vdop = FastMath.sqrt(sz2);
        final double tdop = FastMath.sqrt(st2);

        // Return all the DOP values
        return new DOP(topoFrame.getPoint(), date, satNb, gdop, pdop, hdop, vdop, tdop);
      
    }

}
