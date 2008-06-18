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


/** This class is the OREKIT compliant realization of the JB2006 atmosphere model.
 *
 * It should be instantiated to be used by the {@link
 * org.orekit.forces.perturbations.AtmosphericDrag
 * drag force model} as it implements the {@link Atmosphere} interface.
 *
 *  The input parameters are computed with orbital state information, but solar
 *  activity and magnetic activity data must be provided by the user thanks to the
 *  the interface {@link JB2006InputParameters}.
 *
 * @author Fabien Maussion
 * @see JB2006Atmosphere
 * @version $Revision:1665 $ $Date:2008-06-11 12:12:59 +0200 (mer., 11 juin 2008) $
 */
public class JB2006AtmosphereModel extends JB2006Atmosphere implements Atmosphere {

    /** Serializable UID.*/
    private static final long serialVersionUID = -4566140204081960905L;

    /** Sun position. */
    private CelestialBody sun;

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
                                 final CelestialBody sun, final BodyShape earth,
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
        final double dateMJD = date.minus(AbsoluteDate.MODIFIED_JULIAN_EPOCH) / 86400.;

        // compute geodetic position
        final GeodeticPoint inBody = earth.transform(position, frame, date);

        // compute sun position
        final GeodeticPoint sunInBody =
            earth.transform(sun.getPosition(date, frame), frame, date);
        return getDensity(dateMJD,
                          sunInBody.getLongitude(), sunInBody.getLatitude(),
                          inBody.getLongitude(), inBody.getLatitude(),
                          inBody.getAltitude(), inputParams.getF10(date),
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
