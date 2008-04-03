package fr.cs.orekit.models.perturbations;

import java.util.Calendar;
import java.util.GregorianCalendar;
import org.apache.commons.math.geometry.Vector3D;
import fr.cs.orekit.bodies.BodyShape;
import fr.cs.orekit.bodies.GeodeticPoint;
import fr.cs.orekit.bodies.ThirdBody;
import fr.cs.orekit.errors.OrekitException;
import fr.cs.orekit.frames.Frame;
import fr.cs.orekit.frames.Transform;
import fr.cs.orekit.time.AbsoluteDate;
import fr.cs.orekit.time.UTCScale;
import fr.cs.orekit.utils.PVCoordinates;

/** This class is the OREKIT compliant realization of the DTM2000 atmosphere model.
 *
 * It should be instantiated to be used by the {@link
 * fr.cs.orekit.propagation.numerical.forces.perturbations.AtmosphericDrag
 * drag force model} as it implements the {@link Atmosphere} interface.
 *
 *  The input parameters are computed with orbital state information, but solar
 *  activity and magnetic activity data must be provided by the user threw
 *  the interface {@link DTM2000InputParameters}.
 *
 * @author F. Maussion
 * @see DTM2000Atmosphere
 */
public class DTM2000AtmosphereModel extends DTM2000Atmosphere implements Atmosphere {

    /** Sun position */
    private ThirdBody sun;

    /** External data container */
    private DTM2000InputParameters inputParams;

    /** Earth body shape */
    private BodyShape earth;

    /** Earth fixed frame */
    private Frame bodyFrame;

    /** Constructor with space environment information for internal computation.
     * @param parameters the solar and magnetic activity data
     * @param sun the sun position
     * @param earth the earth body shape
     * @param earthFixed the earth fixed frame
     * @exception OrekitException if some specific resource file reading error occurs
     */
    public DTM2000AtmosphereModel(DTM2000InputParameters parameters,
                                  ThirdBody sun, BodyShape earth, Frame earthFixed) throws OrekitException {
        super();
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
     * @exception OrekitException if date is out of range of solar activity model
     * or if some frame conversion cannot be performed
     */
    public double getDensity(AbsoluteDate date, Vector3D position, Frame frame)
        throws OrekitException {

        // check if data are available :
        if(date.compareTo(inputParams.getMaxDate())>0 ||
                date.compareTo(inputParams.getMinDate())<0) {
            throw new OrekitException("Current date is out of range. " +
                                      "Solar activity data are not available",
                                      new Object[0]);
        }

        // compute day number in current year
        final Calendar cal = new GregorianCalendar();
        cal.setTime(date.toDate(UTCScale.getInstance()));
        final int day = cal.get(Calendar.DAY_OF_YEAR);
        // compute geodetic position
        final Vector3D posInBody = frame.getTransformTo(bodyFrame, date).transformPosition(position);
        final GeodeticPoint inBody = earth.transform(posInBody);
        final double alti = inBody.altitude;
        final double lon = inBody.longitude;
        final double lat = inBody.latitude;

        // compute local solar time

        final Vector3D sunPos = sun.getPosition(date, frame);
        final double hl = Math.PI + Math.atan2(sunPos.getX() * position.getY() - sunPos.getY() * position.getX(),
                                               sunPos.getX() * position.getX() + sunPos.getY() * position.getY());

        // get current solar activity data and compute
        return getDensity(day, alti, lon, lat, hl, inputParams.getInstantFlux(date),
                          inputParams.getMeanFlux(date), inputParams.getThreeHourlyKP(date),
                          inputParams.get24HoursKp(date));

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
    public Vector3D getVelocity(AbsoluteDate date, Vector3D position, Frame frame)
        throws OrekitException {
        final Transform bodyToFrame = bodyFrame.getTransformTo(frame, date);
        final Vector3D posInBody = bodyToFrame.getInverse().transformPosition(position);
        final PVCoordinates pvBody = new PVCoordinates(posInBody, new Vector3D(0, 0, 0));
        final PVCoordinates pvFrame = bodyToFrame.transformPVCoordinates(pvBody);
        return pvFrame.getVelocity();
    }

}
