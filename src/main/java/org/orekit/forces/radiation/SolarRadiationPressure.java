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
package org.orekit.forces.radiation;

import org.apache.commons.math.geometry.Vector3D;
import org.orekit.bodies.ThirdBody;
import org.orekit.errors.OrekitException;
import org.orekit.forces.ForceModel;
import org.orekit.frames.Frame;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.AbstractDetector;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.numerical.TimeDerivativesEquations;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;

/** Solar radiation pressure force model.
 *
 * @author Fabien Maussion
 * @author &Eacute;douard Delente
 * @author V&eacute;ronique Pommier-Maurussane
 * @version $Revision:1665 $ $Date:2008-06-11 12:12:59 +0200 (mer., 11 juin 2008) $
 */
public class SolarRadiationPressure implements ForceModel {

    /** Serializable UID. */
    private static final long serialVersionUID = 8874297900604482921L;

    /** Error message for too low trajectory. */
    private static final String LOW_TRAJECTORY_MESSAGE =
        "trajectory inside the Brillouin sphere (r = {0})";

    /** Reference distance (m). */
    private final double dRef;

    /** Reference radiation pressure at dRef (N/m<sup>2</sup>).*/
    private final double pRef;

    /** Sun model. */
    private final ThirdBody sun;

    /** Earth model. */
    private final double equatorialRadius;

    /** Spacecraft. */
    private final RadiationSensitive spacecraft;

    /** Simple constructor with default reference values.
     * <p>When this constructor is used, the reference values are:</p>
     * <ul>
     *   <li>d<sub>ref</sub> = 149597870000.0 m</li>
     *   <li>p<sub>ref</sub> = 4.56 10<sup>-6</sup> N/m<sup>2</sup></li>
     * </ul>
     * @param sun Sun model
     * @param equatorialRadius spherical shape model (for umbra/penumbra computation)
     * @param spacecraft the object physical and geometrical information
     */
    public SolarRadiationPressure(final ThirdBody sun, final double equatorialRadius,
                                  final RadiationSensitive spacecraft) {
        this(149597870000.0, 4.56e-6, sun, equatorialRadius, spacecraft);
    }

    /** Complete constructor.
     * @param dRef reference distance for the radiation pressure (m)
     * @param pRef reference radiation pressure at dRef (N/m<sup>2</sup>)
     * @param sun Sun model
     * @param equatorialRadius spherical shape model (for umbra/penumbra computation)
     * @param spacecraft the object physical and geometrical information
     */
    public SolarRadiationPressure(final double dRef, final double pRef, final ThirdBody sun,
                                  final double equatorialRadius,
                                  final RadiationSensitive spacecraft) {
        this.dRef  = dRef;
        this.pRef  = pRef;
        this.sun   = sun;
        this.equatorialRadius = equatorialRadius;
        this.spacecraft = spacecraft;
    }

    /** {@inheritDoc} */
    public void addContribution(final SpacecraftState s, final TimeDerivativesEquations adder)
        throws OrekitException {
        // raw radiation pressure
        final Vector3D satSunVector =
            sun.getPosition(s.getDate() ,
                            s.getFrame()).subtract(s.getPVCoordinates().getPosition());

        final double dRatio = dRef / satSunVector.getNorm();
        final double rawP   =
            pRef * dRatio * dRatio * getLightningRatio(s.getPVCoordinates().getPosition(),
                                                       s.getFrame(), s.getDate());

        // spacecraft characteristics effects
        final Vector3D u = satSunVector.normalize();
        final Vector3D inSpacecraft = s.getAttitude().getRotation().applyTo(u);
        final double kd = (1.0 - spacecraft.getAbsorptionCoef(inSpacecraft).getNorm()) *
            (1.0 - spacecraft.getReflectionCoef(inSpacecraft).getNorm());

        final double acceleration =
            rawP * (1 + kd * 4.0 / 9.0 ) * spacecraft.getRadiationCrossSection(inSpacecraft) / s.getMass();

        // provide the perturbing acceleration to the derivatives adder
        adder.addXYZAcceleration(acceleration * u.getX(),
                                 acceleration * u.getY(),
                                 acceleration * u.getZ());

    }

    /** Get the lightning ratio ([0-1]).
     * @param position the satellite's position in the selected frame.
     * @param frame in which is defined the position
     * @param date the date
     * @return lightning ratio
     * @exception OrekitException if the trajectory is inside the Earth
     */
    public double getLightningRatio(final Vector3D position, final Frame frame,
                                    final AbsoluteDate date)
        throws OrekitException {
        final Vector3D satSunVector = sun.getPosition(date, frame).subtract(position);
        // Earth apparent radius
        final double r = position.getNorm();
        if (r <= equatorialRadius) {
            throw new OrekitException(LOW_TRAJECTORY_MESSAGE,
                                      new Object[] {
                                          new Double(r)
                                      });
        }

        final double alphaEarth = Math.atan(equatorialRadius / r);

        // Definition of the Sun's apparent radius
        final double alphaSun = sun.getRadius() / satSunVector.getNorm();

        // Retrieve the Sat-Sun / Sat-Central body angle
        final double sunEarthAngle = Vector3D.angle(satSunVector, position.negate());

        double result = 1.0;

        // Is the satellite in complete penumbra ?
        if (sunEarthAngle - alphaEarth + alphaSun < 0.0) {
            result = 0.0;
        }
        // Compute a lightning ratio in penumbra
        if ((sunEarthAngle - alphaEarth + alphaSun >= 0.0) &&
            (sunEarthAngle - alphaEarth - alphaSun <= 0.0)) {

            //result = (alphaSun + sunEarthAngle - alphaEarth) / (2*alphaSun);

            final double alpha1 =
                (sunEarthAngle * sunEarthAngle -
                        (alphaEarth - alphaSun) * (alphaSun + alphaEarth)) / (2 * sunEarthAngle);

            final double alpha2 =
                (sunEarthAngle * sunEarthAngle +
                        (alphaEarth - alphaSun) * (alphaSun + alphaEarth)) / (2 * sunEarthAngle);

            final double P1 = Math.PI * alphaSun * alphaSun -
                alphaSun * alphaSun * Math.acos(alpha1 / alphaSun) +
                alpha1 * Math.sqrt(alphaSun * alphaSun - alpha1 * alpha1);

            final double P2 = alphaEarth * alphaEarth * Math.acos(alpha2 / alphaEarth) -
                alpha2 * Math.sqrt(alphaEarth * alphaEarth - alpha2 * alpha2);

            result = (P1 - P2) / (Math.PI * alphaSun * alphaSun);


        }

        return result;

    }

    /** {@inheritDoc} */
    public EventDetector[] getEventsDetectors() {
        return new EventDetector[] {
            new UmbraDetector(), new PenumbraDetector()
        };
    }

    /** This class defines the umbra entry/exit detector. */
    private class UmbraDetector extends AbstractDetector {

        /** Serializable UID. */
        private static final long serialVersionUID = -165934451905681928L;

        /** Build a new instance. */
        public UmbraDetector() {
            super(60.0, 1.0e-3);
        }

        /** {@inheritDoc} */
        public int eventOccurred(final SpacecraftState s) {
            return RESET_DERIVATIVES;
        }

        /** The G-function is the difference between the Sat-Sun-Sat-Earth angle and
         * the Earth's apparent radius.
         * @param s the current state information : date, kinematics, attitude
         * @return value of the g function
         * @exception OrekitException if sun or spacecraft position cannot be computed
         */
        public double g(final SpacecraftState s)
            throws OrekitException {
            final PVCoordinates pv = s.getPVCoordinates();
            final Vector3D satSunVector =
                sun.getPosition(s.getDate(), s.getFrame()).subtract(pv.getPosition());
            final double sunEarthAngle = Math.PI - Vector3D.angle(satSunVector, pv.getPosition());
            final double r = pv.getPosition().getNorm();
            if (r <= equatorialRadius) {
                throw new OrekitException(LOW_TRAJECTORY_MESSAGE,
                                          new Object[] {
                                              new Double(r)
                                          });
            }
            final double alphaEarth = equatorialRadius / r;
            return sunEarthAngle - alphaEarth;
        }

    }

    /** This class defines the penumbra entry/exit detector. */
    private class PenumbraDetector extends AbstractDetector {

        /** Serializable UID. */
        private static final long serialVersionUID = -6128481192702533563L;

        /** Build a new instance. */
        public PenumbraDetector() {
            super(60.0, 1.0e-3);
        }

        /** {@inheritDoc} */
        public int eventOccurred(final SpacecraftState s) {
            return RESET_DERIVATIVES;
        }

        /** The G-function is the difference between the Sat-Sun-Sat-Earth angle and
         * the sum of the Earth's and Sun's apparent radius.
         * @param s the current state information : date, kinematics, attitude
         * @return value of the g function
         * @exception OrekitException if sun or spacecraft position cannot be computed
         */
        public double g(final SpacecraftState s)
            throws OrekitException {
            final PVCoordinates pv = s.getPVCoordinates();
            final Vector3D satSunVector =
                sun.getPosition(s.getDate() , s.getFrame()).subtract(pv.getPosition());
            final double sunEarthAngle = Math.PI - Vector3D.angle(satSunVector, pv.getPosition());
            final double r = pv.getPosition().getNorm();
            if (r <= equatorialRadius) {
                throw new OrekitException(LOW_TRAJECTORY_MESSAGE,
                                          new Object[] {
                                              new Double(r)
                                          });
            }
            final double alphaEarth = equatorialRadius / r;
            final double alphaSun   = sun.getRadius() / satSunVector.getNorm();
            return sunEarthAngle - alphaEarth - alphaSun;
        }

    }

}
