package fr.cs.orekit.bodies;

import java.io.Serializable;

import fr.cs.orekit.errors.OrekitException;
import fr.cs.orekit.frames.Frame;
import fr.cs.orekit.time.AbsoluteDate;
import org.apache.commons.math.geometry.Vector3D;

/** This class represents an attracting body different from the central one.
 * @version $Id$
 * @author E. Delente
 */

public abstract class ThirdBody implements CelestialBody, Serializable {

    /** Reference radius. */
    private double radius;

    /** Attraction coefficient. */
    private double mu;

    /** Simple constructor.
     * @param radius reference radius
     * @param mu attraction coefficient
     */
    protected ThirdBody(final double radius, final double mu) {
        this.radius = radius;
        this.mu = mu;
    }

    /** {@inheritDoc} */
    public abstract Vector3D getPosition(AbsoluteDate date, Frame frame)
        throws OrekitException;

    /** Get the reference radius of the body.
     * @return reference radius of the body (m)
     */
    public double getRadius() {
        return radius;
    }

    /** Get the attraction coefficient of the body.
     * @return attraction coefficient of the body (m<sup>3</sup>/s<sup>2</sup>)
     */
    public double getMu() {
        return mu;
    }

}
