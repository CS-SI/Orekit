/* Copyright 2002-2008 CS Communication & Systèmes
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
package org.orekit.orbits;

import org.apache.commons.math.geometry.Vector3D;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.Transform;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeStamped;
import org.orekit.utils.PVCoordinates;

import java.io.Serializable;

/**
 * This class handles orbital parameters without date.

 * <p>
 * The aim of this class is to separate the orbital parameters from the date
 * for cases where dates are managed elsewhere. This occurs for example during
 * numerical integration and interpolation because date is the free parameter
 * whereas the orbital parameters are bound to either differential or
 * interpolation equations.</p>

 * <p>
 * For user convenience, both the cartesian and the equinoctial elements
 * are provided by this class, regardless of the canonical representation
 * implemented in the derived class (which may be classical keplerian
 * elements for example).
 * </p>
 * <p>
 * The parameters are defined in a frame specified by the user. It is important
 * to make sure this frame is consistent : it probably is inertial and centered
 * on the central body. This information is used for example by some
 * force models.
 * </p>
 * <p>
 * The object <code>OrbitalParameters</code> is guaranteed to be immutable.
 * </p>
 * @author Luc Maisonobe
 * @author Guylaine Prat
 * @author Fabien Maussion
 * @author V&eacute;ronique Pommier-Maurussane
 * @version $Revision:1665 $ $Date:2008-06-11 12:12:59 +0200 (mer., 11 juin 2008) $
 */
public abstract class Orbit implements TimeStamped, Serializable {

    /** Frame in which are defined the orbital parameters. */
    private final Frame frame;

    /** Date of the orbital parameters. */
    private final AbsoluteDate date;

    /** Value of mu used to compute position and velocity (m<sup>3</sup>/s<sup>2</sup>). */
    private final double mu;

    /** Computed PVCoordinates. */
    private PVCoordinates pvCoordinates;

    /** Default constructor.
     * Build a new instance with arbitrary default elements.
     * @param frame the frame in which the parameters are defined
     * @param date date of the orbital parameters
     * @param mu central attraction coefficient (m^3/s^2)
     */
    protected Orbit(final Frame frame, final AbsoluteDate date, final double mu) {
        this.date = date;
        this.mu = mu;
        this.pvCoordinates = null;
        this.frame =  frame;
    }

    /** Set the orbit from cartesian parameters.
     * @param pvCoordinates the position and velocity in the inertial frame
     * @param frame the frame in which the {@link PVCoordinates} are defined
     * @param date date of the orbital parameters
     * @param mu central attraction coefficient (m^3/s^2)
     */
    protected Orbit(final PVCoordinates pvCoordinates, final Frame frame,
                                final AbsoluteDate date, final double mu) {
        this.date = date;
        this.mu = mu;
        this.pvCoordinates = pvCoordinates;
        this.frame = frame;
    }

    /** Get the frame in which the orbital parameters are defined.
     * @return frame in which the orbital parameters are defined
     */
    public Frame getFrame() {
        return frame;
    }

    /** Get the semi-major axis.
     * @return semi-major axis (m)
     */
    public abstract double getA();

    /** Get the first component of the equinoctial eccentricity vector.
     * @return first component of the equinoctial eccentricity vector
     */
    public abstract double getEquinoctialEx();

    /** Get the second component of the equinoctial eccentricity vector.
     * @return second component of the equinoctial eccentricity vector
     */
    public abstract double getEquinoctialEy();

    /** Get the first component of the inclination vector.
     * @return first component of the inclination vector
     */
    public abstract double getHx();

    /** Get the second component of the inclination vector.
     * @return second component of the inclination vector
     */
    public abstract double getHy();

    /** Get the eccentric latitude argument.
     * @return eccentric latitude argument (rad)
     */
    public abstract double getLE();

    /** Get the true latitude argument.
     * @return true latitude argument (rad)
     */
    public abstract double getLv();

    /** Get the mean latitude argument.
     * @return mean latitude argument (rad)
     */
    public abstract double getLM();

    // Additional orbital elements

    /** Get the eccentricity.
     * @return eccentricity
     */
    public abstract double getE();

    /** Get the inclination.
     * @return inclination (rad)
     */
    public abstract double getI();

    /** Get the central acceleration constant.
     * @return central acceleration constant
     */
    public double getMu() {
        return mu;
    }

    /** Get the keplerian period.
     * <p>The keplerian period is computed directly from semi major axis
     * and central acceleration constant.</p>
     * @return keplerian period in seconds
     */
    public double getKeplerianPeriod() {
        final double a = getA();
        return 2.0 * Math.PI * a * Math.sqrt(a / mu);
    }

    /** Get the keplerian mean motion.
     * <p>The keplerian mean motion is computed directly from semi major axis
     * and central acceleration constant.</p>
     * @return keplerian mean motion in radians per second
     */
    public double getKeplerianMeanMotion() {
        final double a = getA();
        return Math.sqrt(mu / a) / a;
    }

    /** Get the date of orbital parameters.
     * @return date of the orbital parameters
     */
    public AbsoluteDate getDate() {
        return date;
    }

    /** Get the {@link PVCoordinates}.
     * @param outputFrame frame in which the position/velocity coordinates shall be computed
     * @return pvCoordinates in the specified output frame
     * @exception OrekitException if transformation between frames cannot be computed
     * @see #getPVCoordinates()
     */
    public PVCoordinates getPVCoordinates(final Frame outputFrame)
        throws OrekitException {
        if (pvCoordinates == null) {
            initPVCoordinates();
        }

        // If output frame requested is the same as definition frame,
        // PV coordinates are returned directly
        if (outputFrame == frame) {
            return pvCoordinates;
        }

        // Else, PV coordinates are transformed to output frame
        final Transform t = frame.getTransformTo(outputFrame, date);
        return t.transformPVCoordinates(pvCoordinates);
    }

    /** Get the {@link PVCoordinates} in definition frame.
     * @return pvCoordinates in the definition frame
     * @see #getPVCoordinates(Frame)
     */
    public PVCoordinates getPVCoordinates() {
        if (pvCoordinates == null) {
            initPVCoordinates();
        }
        return pvCoordinates;
    }

    /** Initialize the position/velocity coordinates.
     */
    private void initPVCoordinates() {

        // get equinoctial parameters
        final double a  = getA();
        final double ex = getEquinoctialEx();
        final double ey = getEquinoctialEy();
        final double hx = getHx();
        final double hy = getHy();
        final double lE = getLE();

        // inclination-related intermediate parameters
        final double hx2   = hx * hx;
        final double hy2   = hy * hy;
        final double factH = 1. / (1 + hx2 + hy2);

        // reference axes defining the orbital plane
        final double ux = (1 + hx2 - hy2) * factH;
        final double uy =  2 * hx * hy * factH;
        final double uz = -2 * hy * factH;

        final double vx = uy;
        final double vy = (1 - hx2 + hy2) * factH;
        final double vz =  2 * hx * factH;

        // eccentricity-related intermediate parameters
        final double exey = ex * ey;
        final double ex2  = ex * ex;
        final double ey2  = ey * ey;
        final double e2   = ex2 + ey2;
        final double eta  = 1 + Math.sqrt(1 - e2);
        final double beta = 1. / eta;

        // eccentric latitude argument
        final double cLe    = Math.cos(lE);
        final double sLe    = Math.sin(lE);
        final double exCeyS = ex * cLe + ey * sLe;

        // coordinates of position and velocity in the orbital plane
        final double x      = a * ((1 - beta * ey2) * cLe + beta * exey * sLe - ex);
        final double y      = a * ((1 - beta * ex2) * sLe + beta * exey * cLe - ey);

        final double factor = Math.sqrt(mu / a) / (1 - exCeyS);
        final double xdot   = factor * (-sLe + beta * ey * exCeyS);
        final double ydot   = factor * ( cLe - beta * ex * exCeyS);

        final Vector3D position =
            new Vector3D(x * ux + y * vx, x * uy + y * vy, x * uz + y * vz);
        final Vector3D velocity =
            new Vector3D(xdot * ux + ydot * vx, xdot * uy + ydot * vy, xdot * uz + ydot * vz);
        pvCoordinates = new PVCoordinates(position, velocity);

    }

}
