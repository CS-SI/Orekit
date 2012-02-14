/* Copyright 2002-2011 CS Communication & Systèmes
 * Licensed to CS Communication & Systèmes (CS) under one or more
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
package org.orekit.propagation.events;

import java.util.Arrays;
import java.util.Comparator;

import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.util.MathUtils;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.TopocentricFrame;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;

/** Finder for satellite azimuth-elevation events with respect to a mask.
 * <p>This class finds elevation events (i.e. satellite raising and setting) with
 * respect to an azimuth-elevation mask.</p>
 * <p>An azimuth-elevation mask defines the physical horizon for a local point,
 * origin of some topocentric frame.</p>
 * <p>Azimuth is defined according to {@link TopocentricFrame#getAzimuth(org.apache.commons.math3.geometry.euclidean.threed.Vector3D, org.orekit.frames.Frame, org.orekit.time.AbsoluteDate) getAzimuth}.
 *  Elevation is defined according to {@link TopocentricFrame#getElevation(org.apache.commons.math3.geometry.euclidean.threed.Vector3D, org.orekit.frames.Frame, org.orekit.time.AbsoluteDate) getElevation}.</p>
 * <p>The azimuth elevation mask must be supplied as a twodimensional array with
 *  multiples lines of pairs of azimuth-elevation angles. First row will be filled with
 *  azimuth values, second row with elevation values, as in the following snippet:
 *  <pre>
 *    double [][] mask = {
 *                        {FastMathFastMath.toRadians(0),   FastMath.toRadians(10)},
 *                        {FastMathFastMath.toRadians(45),  FastMath.toRadians(8)},
 *                        {FastMathFastMath.toRadians(90),  FastMath.toRadians(6)},
 *                        {FastMathFastMath.toRadians(135), FastMath.toRadians(4)},
 *                        {FastMathFastMath.toRadians(180), FastMath.toRadians(5)},
 *                        {FastMathFastMath.toRadians(225), FastMath.toRadians(6)},
 *                        {FastMathFastMath.toRadians(270), FastMath.toRadians(8)},
 *                        {FastMathFastMath.toRadians(315), FastMath.toRadians(9)}
 *                       };
 *  </pre>
 * </p>
 * <p>No assumption is made on azimuth values and ordering. The only restraint is
 * that only one elevation value can be associated to identical azimuths modulo 2PI.</p>
 * <p>The default implementation behavior is to {@link
 * EventDetector.Action#CONTINUE continue} propagation at raising and to
 * {@link EventDetector.Action#STOP stop} propagation at setting. This can be changed
 * by overriding the {@link #eventOccurred(SpacecraftState, boolean) eventOccurred}
 * method in a derived class.</p>
 * @see org.orekit.propagation.Propagator#addEventDetector(EventDetector)
 * @author Pascal Parraud
 */
public class GroundMaskElevationDetector extends AbstractDetector {

    /** Serializable UID. */
    private static final long serialVersionUID = -8124322408349693773L;

    /** Azimuth-elevation mask. */
    private final double[][] azelmask;

    /** Topocentric frame in which azimuth and elevation should be evaluated. */
    private final TopocentricFrame topo;

    /** Build a new azimuth-elevation detector.
     *  <p>This simple constructor takes default values for maximal checking
     *   interval ({@link #DEFAULT_MAXCHECK}) and convergence threshold
     *   ({@link #DEFAULT_THRESHOLD}).</p>
     * @param azimelev azimuth-elevation mask (rad)
     * @param topo topocentric frame in which elevation should be evaluated
     * @exception IllegalArgumentException if azimuth-elevation mask is not supported
     */
    public GroundMaskElevationDetector(final double[][] azimelev, final TopocentricFrame topo) {
        super(DEFAULT_MAXCHECK, DEFAULT_THRESHOLD);
        this.azelmask = checkMask(azimelev);
        this.topo = topo;
    }

    /** Build a new azimuth-elevation detector.
     * <p>This constructor takes default value for convergence threshold
     * ({@link #DEFAULT_THRESHOLD}).</p>
     * <p>The maximal interval between elevation checks should
     * be smaller than the half duration of the minimal pass to handle,
     * otherwise some short passes could be missed.</p>
     * @param maxCheck maximal checking interval (s)
     * @param azimelev azimuth-elevation mask (rad)
     * @param topo topocentric frame in which elevation should be evaluated
     * @exception IllegalArgumentException if azimuth-elevation mask is not supported
     */
    public GroundMaskElevationDetector(final double maxCheck,
                                    final double[][] azimelev,
                                    final TopocentricFrame topo) {
        super(maxCheck, DEFAULT_THRESHOLD);
        this.azelmask = checkMask(azimelev);
        this.topo = topo;
    }

    /** Build a new azimuth-elevation detector.
     * <p>The maximal interval between elevation checks should
     * be smaller than the half duration of the minimal pass to handle,
     * otherwise some short passes could be missed.</p>
     * @param maxCheck maximal checking interval (s)
     * @param threshold convergence threshold (s)
     * @param azimelev azimuth-elevation mask (rad)
     * @param topo topocentric frame in which elevation should be evaluated
     * @exception IllegalArgumentException if azimuth-elevation mask is not supported
     */
    public GroundMaskElevationDetector(final double maxCheck,
                                    final double threshold,
                                    final double[][] azimelev,
                                    final TopocentricFrame topo) {
        super(maxCheck, threshold);
        this.azelmask = checkMask(azimelev);
        this.topo = topo;
    }

    /** Get the topocentric frame.
     * @return the topocentric frame
     */
    public TopocentricFrame getTopocentricFrame() {
        return topo;
    }

    /** {@inheritDoc} */
    public void init(final SpacecraftState s0, final AbsoluteDate t) {
    }

    /** Handle an azimuth-elevation event and choose what to do next.
     * <p>The default implementation behavior is to {@link
     * EventDetector.Action#CONTINUE continue} propagation at raising and to
     * {@link EventDetector.Action#STOP stop} propagation at setting.</p>
     * @param s the current state information : date, kinematics, attitude
     * @param increasing if true, the value of the switching function increases
     * when times increases around event.
     * @return {@link EventDetector.Action#STOP} or {@link EventDetector.Action#CONTINUE}
     * @exception OrekitException if some specific error occurs
     */
    public Action eventOccurred(final SpacecraftState s, final boolean increasing)
        throws OrekitException {
        return increasing ? Action.CONTINUE : Action.STOP;
    }

    /** Compute the value of the switching function.
     * This function measures the difference between the current elevation and the
     * elevation for current azimuth interpolated from azimuth-elevation mask.
     * @param s the current state information: date, kinematics, attitude
     * @return value of the switching function
     * @exception OrekitException if some specific error occurs
     */
    public double g(final SpacecraftState s) throws OrekitException {
        final double azimuth = topo.getAzimuth(s.getPVCoordinates().getPosition(), s.getFrame(), s.getDate());
        return topo.getElevation(s.getPVCoordinates().getPosition(), s.getFrame(), s.getDate()) - getElevation(azimuth);
    }

    /** Get the interpolated elevation for a given azimuth according to the mask.
     * @param azimuth azimuth (rad)
     * @return elevation angle (rad)
     */
    public double getElevation(final double azimuth) {
        double elevation = 0.0;
        boolean fin = false;
        for (int i = 1; i < azelmask.length & !fin; i++) {
            if (azimuth <= azelmask[i][0]) {
                fin = true;
                final double azd = azelmask[i - 1][0];
                final double azf = azelmask[i][0];
                final double eld = azelmask[i - 1][1];
                final double elf = azelmask[i][1];
                elevation = eld + (azimuth - azd) * (elf - eld) / (azf - azd);
            }
        }
        return elevation;
    }

    /** Checking and ordering the azimuth-elevation tabulation.
     * @param azimelev azimuth-elevation tabulation to be checked and ordered
     * @return ordered azimuth-elevation tabulation ordered
     */
    private static double[][] checkMask(final double[][] azimelev) {

        /* Copy of the given mask */
        final double[][] mask = new double[azimelev.length + 2][azimelev[0].length];
        for (int i = 0; i < azimelev.length; i++) {
            System.arraycopy(azimelev[i], 0, mask[i + 1], 0, azimelev[i].length);
            /* Reducing azimuth between 0 and 2*Pi */
            mask[i + 1][0] = MathUtils.normalizeAngle(mask[i + 1][0], FastMath.PI);
        }

        /* Sorting the mask with respect to azimuth */
        Arrays.sort(mask, 1, mask.length - 1, new Comparator<double[]>() {
            public int compare(final double[] d1, final double[] d2) {
                return Double.compare(d1[0], d2[0]);
            }
        });

        /* Extending the mask in order to cover [0, 2PI] in azimuth */
        mask[0][0] = mask[mask.length - 2][0] - MathUtils.TWO_PI;
        mask[0][1] = mask[mask.length - 2][1];
        mask[mask.length - 1][0] = mask[1][0] + MathUtils.TWO_PI;
        mask[mask.length - 1][1] = mask[1][1];

        /* Checking the sorted mask: same azimuth modulo 2PI must have same elevation */
        for (int i = 1; i < mask.length; i++) {
            if (Double.compare(mask[i - 1][0], mask[i][0]) == 0) {
                if (Double.compare(mask[i - 1][1], mask[i][1]) != 0) {
                    throw OrekitException.createIllegalArgumentException(OrekitMessages.UNEXPECTED_TWO_ELEVATION_VALUES_FOR_ONE_AZIMUTH, mask[i - 1][1], mask[i][1], mask[i][0]);
                }
            }
        }

        return mask;
    }

}
