/* Copyright 2002-2008 CS Communication & Systèmes
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.orekit.models.perturbations;

import java.util.Calendar;
import java.util.GregorianCalendar;
import org.apache.commons.math.geometry.Vector3D;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.CelestialBody;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.Transform;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.UTCScale;
import org.orekit.utils.PVCoordinates;


/** This class is the OREKIT compliant realization of the DTM2000 atmosphere model.
 *
 * It should be instantiated to be used by the {@link
 * org.orekit.propagation.numerical.forces.perturbations.AtmosphericDrag
 * drag force model} as it implements the {@link Atmosphere} interface.
 *
 *  The input parameters are computed with orbital state information, but solar
 *  activity and magnetic activity data must be provided by the user threw
 *  the interface {@link DTM2000InputParameters}.
 *
 * @author Fabien Maussion
 * @see DTM2000Atmosphere
 * @version $Revision$ $Date$
 */
public class DTM2000AtmosphereModel extends DTM2000Atmosphere implements Atmosphere {

    /** Serializable UID.*/
    private static final long serialVersionUID = 7601194421088896381L;

    /** Sun position. */
    private CelestialBody sun;

    /** External data container. */
    private DTM2000InputParameters inputParams;

    /** Earth body shape. */
    private BodyShape earth;

    /** Earth fixed frame. */
    private Frame bodyFrame;

    /** Constructor with space environment information for internal computation.
     * @param parameters the solar and magnetic activity data
     * @param sun the sun position
     * @param earth the earth body shape
     * @param earthFixed the earth fixed frame
     * @exception OrekitException if some specific resource file reading error occurs
     */
    public DTM2000AtmosphereModel(final DTM2000InputParameters parameters,
                                  final CelestialBody sun, final BodyShape earth,
                                  final Frame earthFixed)
        throws OrekitException {
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
    public double getDensity(final AbsoluteDate date, final Vector3D position,
                             final Frame frame)
        throws OrekitException {

        // check if data are available :
        if ((date.compareTo(inputParams.getMaxDate()) > 0) ||
            (date.compareTo(inputParams.getMinDate()) < 0)) {
            final TimeScale utcScale = UTCScale.getInstance();
            throw new OrekitException("no solar activity available at {0}, " +
                                      "data available only in range [{1}, {2}]",
                                      new Object[] {
                                          date.toString(utcScale),
                                          inputParams.getMinDate().toString(utcScale),
                                          inputParams.getMaxDate().toString(utcScale)
                                      });
        }

        // compute day number in current year
        final Calendar cal = new GregorianCalendar();
        cal.setTime(date.toDate(UTCScale.getInstance()));
        final int day = cal.get(Calendar.DAY_OF_YEAR);
        // compute geodetic position
        final GeodeticPoint inBody = earth.transform(position, frame, date);
        final double alti = inBody.getAltitude();
        final double lon = inBody.getLongitude();
        final double lat = inBody.getLatitude();

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
