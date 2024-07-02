/* Contributed in the public domain.
 * Licensed to CS GROUP (CS) under one or more
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
package org.orekit.frames;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsInstanceOf;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBody;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/** Unit tests for {@link EclipticProvider}. */
public class EclipticProviderTest {

    /** Set the orekit data to include ephemerides. */
    @BeforeAll
    public static void setUpBefore() {
        Utils.setDataRoot("regular-data");
    }

    /**
     * Check the Ecliptic frame defined from IERS mean obliquity equations against the
     * position of Sun and Earth from the JPL 406 ephemerides.
     *
     * @throws Exception on error
     */
    @Test
    public void testAgreementWith406Ephemerides() throws Exception {
        TimeScale utc = TimeScalesFactory.getUTC();

        //time spans we have test data sets for.
        checkAlignment(new AbsoluteDate(1969, 5, 27, utc), new AbsoluteDate(1969, 9, 20, utc));
        checkAlignment(new AbsoluteDate(1969, 12, 5, utc), new AbsoluteDate(1970, 4, 1, utc));
        checkAlignment(new AbsoluteDate(1970, 6, 15, utc), new AbsoluteDate(1970, 8, 1, utc));
        checkAlignment(new AbsoluteDate(2002, 12, 16, utc), new AbsoluteDate(2004, 2, 3, utc));

        checkAlignment(new AbsoluteDate(1999, 11, 22, utc), new AbsoluteDate(2000, 5, 21, utc));
    }

    /**
     * Check alignment of ecliptic +z with Earth-Moon barycenter angular momentum. Angular
     * difference will be checked every month.
     *
     * @param start start date of check.
     * @param end   en date of check.
     */
    private void checkAlignment(AbsoluteDate start, AbsoluteDate end) {
        //setup
        CelestialBody sun = CelestialBodyFactory.getSun();
        CelestialBody emb = CelestialBodyFactory.getEarthMoonBarycenter();
        Frame heliocentric = sun.getInertiallyOrientedFrame();
        //subject under test
        Frame ecliptic = FramesFactory.getEcliptic(IERSConventions.IERS_2010);

        //verify
        //precise definition is +z is parallel to Earth-Moon barycenter's angular momentum
        //over date range of ephemeris, a season at a time
        double preciseTol = 0.50 * Constants.ARC_SECONDS_TO_RADIANS;
        for (AbsoluteDate date = start;
             date.compareTo(end) < 0;
             date = date.shiftedBy(Constants.JULIAN_YEAR / 12.0)) {

            Transform heliocentricToEcliptic = heliocentric.getTransformTo(ecliptic, date);
            Vector3D momentum = emb.getPVCoordinates(date, heliocentric).getMomentum();
            Vector3D actual = heliocentricToEcliptic.transformVector(momentum);
            double angle = Vector3D.angle(
                    Vector3D.PLUS_K,
                    actual
            );
            Assertions.assertEquals(0, angle, preciseTol,"Agrees with ephemerides to within " + preciseTol);

        }

    }

    /**
     * Check frame has the right name.
     */
    @Test
    public void testGetName() {
        Assertions.assertEquals("Ecliptic/1996",
                            FramesFactory.getEcliptic(IERSConventions.IERS_1996).getName());
        Assertions.assertEquals("Ecliptic/2003",
                            FramesFactory.getEcliptic(IERSConventions.IERS_2003).getName());
        Assertions.assertEquals("Ecliptic/2010",
                            FramesFactory.getEcliptic(IERSConventions.IERS_2010).getName());
    }

    /**
     * Check the parent frame is MOD.
     */
    @Test
    public void testGetParent() {
        //setup
        Frame frame = FramesFactory.getEcliptic(IERSConventions.IERS_2003);

        //action + verify
        MatcherAssert.assertThat(frame.getParent().getTransformProvider(),
                          IsInstanceOf.instanceOf(MODProvider.class));
    }

    @Test
    public void testSerialization() throws IOException, ClassNotFoundException {
        TransformProvider provider = new EclipticProvider(IERSConventions.IERS_2010);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream    oos = new ObjectOutputStream(bos);
        oos.writeObject(provider);

        Assertions.assertTrue(bos.size() > 200);
        Assertions.assertTrue(bos.size() < 250);

        ByteArrayInputStream  bis = new ByteArrayInputStream(bos.toByteArray());
        ObjectInputStream     ois = new ObjectInputStream(bis);
        TransformProvider deserialized  = (TransformProvider) ois.readObject();
        for (double dt = 0; dt < Constants.JULIAN_DAY; dt += 3600) {
            AbsoluteDate date = AbsoluteDate.J2000_EPOCH.shiftedBy(dt);
            Transform expectedIdentity = new Transform(date,
                                                       provider.getTransform(date).getInverse(),
                                                       deserialized.getTransform(date));
            Assertions.assertEquals(0.0, expectedIdentity.getTranslation().getNorm(), 1.0e-15);
            Assertions.assertEquals(0.0, expectedIdentity.getRotation().getAngle(),   1.0e-15);
        }

    }

}
