package fr.cs.orekit.models.perturbations;

import org.apache.commons.math.geometry.Vector3D;

import fr.cs.orekit.bodies.BodyShape;
import fr.cs.orekit.bodies.GeodeticPoint;
import fr.cs.orekit.bodies.ThirdBody;
import fr.cs.orekit.errors.OrekitException;
import fr.cs.orekit.frames.Frame;
import fr.cs.orekit.frames.Transform;
import fr.cs.orekit.time.AbsoluteDate;
import fr.cs.orekit.time.TimeScale;
import fr.cs.orekit.time.UTCScale;
import fr.cs.orekit.utils.PVCoordinates;

/** This class is the OREKIT compliant realization of the JB2006 atmosphere model.
 *
 * It should be instantiated to be used by the {@link
 * fr.cs.orekit.propagation.numerical.forces.perturbations.AtmosphericDrag
 * drag force model} as it implements the {@link Atmosphere} interface.
 *
 *  The input parameters are computed with orbital state information, but solar
 *  activity and magnetic activity data must be provided by the user thanks to the
 *  the interface {@link JB2006InputParameters}.
 *
 * @author F. Maussion
 * @see JB2006Atmosphere
 */
public class JB2006AtmosphereModel extends JB2006Atmosphere implements Atmosphere {

    /** Serializable UID.*/
    private static final long serialVersionUID = -4566140204081960905L;

    /** Sun position. */
    private ThirdBody sun;

    /** External data container. */
    private JB2006InputParameters inputParams;

    /** Earth body shape. */
    private BodyShape earth;

    /** Earth fixed frame. */
    private Frame bodyFrame;

    /** Constructor with space environment information for internal computation.
     * @param parameters the solar and magnetic activity data
     * @param sun the sun position
     * @param earth the earth body shape
     * @param earthFixed the earth fixed frame
     */
    public JB2006AtmosphereModel(final JB2006InputParameters parameters,
                                 final ThirdBody sun, final BodyShape earth,
                                 final Frame earthFixed) {
        this.earth = earth;
        this.sun = sun;
        this.inputParams = parameters;
        this.bodyFrame = earthFixed;
    }

    /** Get the local density.
     * @param date current date
     * @param position current position in frame
     * @param frame the frame in which is defined the position
     * @return local density (kg/m<sup>3</sup>)
     * @exception OrekitException if date is out of range of solar activity
     */
    public double getDensity(final AbsoluteDate date, final Vector3D position,
                             final Frame frame)
        throws OrekitException {
        // check if data are available :
        if (date.compareTo(inputParams.getMaxDate()) > 0 ||
            date.compareTo(inputParams.getMinDate()) < 0) {
            final TimeScale utcScale = UTCScale.getInstance();
            throw new OrekitException("no solar activity available at {0}, " +
                                      "data available only in range [{1}, {2}]",
                                      new Object[] {
                                          date.toString(utcScale),
                                          inputParams.getMinDate().toString(utcScale),
                                          inputParams.getMaxDate().toString(utcScale)
                                      });
        }

        // compute modified julian days date
        final double dateMJD = date.minus(AbsoluteDate.ModifiedJulianEpoch) / 86400.;

        // compute geodetic position
        final GeodeticPoint inBody = earth.transform(position, frame, date);

        // compute sun position
        final GeodeticPoint sunInBody =
            earth.transform(sun.getPosition(date, frame), frame, date);
        return getDensity(dateMJD, sunInBody.longitude, sunInBody.latitude,
                          inBody.longitude, inBody.latitude,
                          inBody.altitude, inputParams.getF10(date),
                          inputParams.getF10B(date),
                          inputParams.getAp(date), inputParams.getS10(date),
                          inputParams.getS10B(date), inputParams.getXM10(date),
                          inputParams.getXM10B(date));
    }

    /** Get the inertial velocity of atmosphere molecules.
     * Here the case is simplified : atmosphere is supposed to have a null velocity
     * in earth frame.
     * @param date current date
     * @param position current position in frame
     * @param frame the frame in which is defined the position
     * @return velocity (m/s) (defined in the same frame as the position)
     * @exception OrekitException if some frame conversion cannot be performed
     */
    public Vector3D getVelocity(final AbsoluteDate date, final Vector3D position,
                                final Frame frame)
        throws OrekitException {
        final Transform bodyToFrame = bodyFrame.getTransformTo(frame, date);
        final Vector3D posInBody = bodyToFrame.getInverse().transformPosition(position);
        final PVCoordinates pvBody = new PVCoordinates(posInBody, new Vector3D(0, 0, 0));
        final PVCoordinates pvFrame = bodyToFrame.transformPVCoordinates(pvBody);
        return pvFrame.getVelocity();
    }

}
