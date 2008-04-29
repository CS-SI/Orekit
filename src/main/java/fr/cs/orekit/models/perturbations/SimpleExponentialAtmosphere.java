package fr.cs.orekit.models.perturbations;

import org.apache.commons.math.geometry.Vector3D;

import fr.cs.orekit.bodies.BodyShape;
import fr.cs.orekit.errors.OrekitException;
import fr.cs.orekit.frames.Frame;
import fr.cs.orekit.frames.Transform;
import fr.cs.orekit.time.AbsoluteDate;
import fr.cs.orekit.utils.PVCoordinates;

/** Simple exponential atmospheric model.
 * <p>This model represents a simple atmosphere with an exponential
 * density and rigidly bound to the underlying rotating body.</p>
 * @version $Id: Atmosphere.java 840 2006-06-07 09:41:38Z internMS $
 * @author F.Maussion
 * @author L.Maisonobe
 */
public class SimpleExponentialAtmosphere implements Atmosphere {

    /** Serializable UID.*/
    private static final long serialVersionUID = 2772347498196369601L;

    /** Earth shape model. */
    private BodyShape    shape;

    /** Earth reference frame. */
    private Frame        bodyFrame;

    /** Reference density. */
    private double       rho0;

    /** Reference altitude. */
    private double       h0;

    /** Reference altitude scale. */
    private double       hscale;

    /** Create an exponential atmosphere.
     * @param shape body shape model
     * @param bodyFrame body rotation frame
     * @param rho0 Density at the altitude h0
     * @param h0 Altitude of reference (m)
     * @param hscale Scale factor
     */
    public SimpleExponentialAtmosphere(final BodyShape shape, final Frame bodyFrame,
                                       final double rho0, final double h0, final double hscale) {
        this.shape  = shape;
        this.bodyFrame = bodyFrame;
        this.rho0   = rho0;
        this.h0     = h0;
        this.hscale = hscale;
    }

    /** {@inheritDoc} */
    public double getDensity(final AbsoluteDate date, final Vector3D position, final Frame frame)
        throws OrekitException {
        return rho0 * Math.exp((h0 - shape.transform(position, frame, date).altitude) / hscale);
    }

    /** {@inheritDoc} */
    public Vector3D getVelocity(final AbsoluteDate date,
                                final Vector3D position, final Frame frame)
        throws OrekitException {
        final Transform bodyToFrame = bodyFrame.getTransformTo(frame, date);
        final Vector3D posInBody = bodyToFrame.getInverse().transformPosition(position);
        final PVCoordinates pvBody = new PVCoordinates(posInBody, new Vector3D(0, 0, 0));
        final PVCoordinates pvFrame = bodyToFrame.transformPVCoordinates(pvBody);
        return pvFrame.getVelocity();
    }

}
