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
import org.hipparchus.analysis.differentiation.UnivariateDerivative1;
import org.hipparchus.analysis.differentiation.UnivariateDerivative1Field;
import org.hipparchus.analysis.differentiation.UnivariateDerivative2;
import org.hipparchus.analysis.differentiation.UnivariateDerivative2Field;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBody;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.data.DataContext;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.AngularCoordinates;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;


/** Unit tests for {@link EclipticProvider}. */
public class EclipticProviderTest {

    /** Set the orekit data to include ephemerides. */
    @BeforeAll
    public static void setUpBefore() {
        Utils.setDataRoot("regular-data");
    }

    @ParameterizedTest
    @EnumSource(IERSConventions.class)
    void testKinematicConsistency(final IERSConventions conventions) {
        checkKinematicConsistency(conventions);
    }

    @Test
    void testGcrfAndModToEclipticPVAConsistency() {
        final AbsoluteDate date = new AbsoluteDate(2025, 4, 3, 12, 0, 0.0, TimeScalesFactory.getUTC());
        final Frame ecliptic = FramesFactory.getEcliptic(IERSConventions.IERS_2010);
        checkPVAConsistency(FramesFactory.getGCRF(), ecliptic, date);
        checkPVAConsistency(ecliptic.getParent(), ecliptic, date);
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

    private void checkKinematicConsistency(final IERSConventions conventions) {
        final EclipticProvider provider = new EclipticProvider(conventions,
                                                              DataContext.getDefault().getTimeScales());
        final AbsoluteDate date = new AbsoluteDate(2025, 4, 3, 12, 0, 0.0, TimeScalesFactory.getUTC());
        final Transform scalarTransform = provider.getTransform(date);

        final UnivariateDerivative2Field field = UnivariateDerivative2Field.getInstance();
        final FieldAbsoluteDate<UnivariateDerivative2> derivativeDate =
                        new FieldAbsoluteDate<>(field, date).shiftedBy(new UnivariateDerivative2(0, 1, 0));
        final FieldTransform<UnivariateDerivative2> derivativeTransform = provider.getTransform(derivativeDate);
        final AngularCoordinates embeddedDerivatives = new AngularCoordinates(derivativeTransform.getRotation());

        final FieldAbsoluteDate<Binary64> binaryDate = new FieldAbsoluteDate<>(Binary64Field.getInstance(), date);
        final FieldTransform<Binary64> binaryTransform = provider.getTransform(binaryDate);
        final KinematicTransform kinematicTransform = provider.getKinematicTransform(date);
        final FieldKinematicTransform<Binary64> binaryKinematicTransform = provider.getKinematicTransform(binaryDate);

        final UnivariateDerivative1Field ud1Field = UnivariateDerivative1Field.getInstance();
        final FieldAbsoluteDate<UnivariateDerivative1> ud1Date =
                        new FieldAbsoluteDate<>(ud1Field, date).shiftedBy(new UnivariateDerivative1(0, 1));
        final FieldKinematicTransform<UnivariateDerivative1> derivativeKinematicTransform =
                        provider.getKinematicTransform(ud1Date);
        final AngularCoordinates embeddedKinematicDerivatives =
                        new AngularCoordinates(derivativeKinematicTransform.getRotation());

        for (final double dt : new double[] { -Constants.JULIAN_CENTURY, 0.0, Constants.JULIAN_CENTURY }) {
            final AbsoluteDate rotationDate = AbsoluteDate.J2000_EPOCH.shiftedBy(dt);
            final FieldAbsoluteDate<UnivariateDerivative2> fieldDate = new FieldAbsoluteDate<>(field, rotationDate);
            Assertions.assertEquals(0.0,
                                    Rotation.distance(provider.getTransform(rotationDate).getRotation(),
                                                      provider.getTransform(fieldDate).getRotation().toRotation()),
                                    2.0e-15);
        }
        Assertions.assertEquals(0.0,
                                Rotation.distance(binaryTransform.getRotation().toRotation(),
                                                  scalarTransform.getRotation()),
                                2.0e-15);
        Assertions.assertEquals(0.0,
                                Vector3D.distance(binaryTransform.getRotationRate().toVector3D(),
                                                  scalarTransform.getRotationRate()),
                                2.0e-22);
        Assertions.assertEquals(0.0,
                                Vector3D.distance(binaryTransform.getRotationAcceleration().toVector3D(),
                                                  scalarTransform.getRotationAcceleration()),
                                2.0e-29);
        Assertions.assertEquals(0.0,
                                Vector3D.distance(embeddedDerivatives.getRotationRate(),
                                                  derivativeTransform.getRotationRate().toVector3D()),
                                2.0e-22);
        Assertions.assertEquals(0.0,
                                Vector3D.distance(embeddedDerivatives.getRotationAcceleration(),
                                                  derivativeTransform.getRotationAcceleration().toVector3D()),
                                2.0e-29);
        Assertions.assertEquals(0.0,
                                Rotation.distance(kinematicTransform.getRotation(), scalarTransform.getRotation()),
                                2.0e-15);
        Assertions.assertEquals(0.0,
                                Vector3D.distance(kinematicTransform.getRotationRate(), scalarTransform.getRotationRate()),
                                2.0e-22);
        Assertions.assertEquals(0.0,
                                Rotation.distance(binaryKinematicTransform.getRotation().toRotation(),
                                                  binaryTransform.getRotation().toRotation()),
                                2.0e-15);
        Assertions.assertEquals(0.0,
                                Vector3D.distance(binaryKinematicTransform.getRotationRate().toVector3D(),
                                                  binaryTransform.getRotationRate().toVector3D()),
                                2.0e-22);
        Assertions.assertEquals(0.0,
                                Vector3D.distance(embeddedKinematicDerivatives.getRotationRate(),
                                                  derivativeKinematicTransform.getRotationRate().toVector3D()),
                                2.0e-22);
        final Vector3D position = new Vector3D(42000000.0, -13000000.0, 5000000.0);
        final double h = 10.0;
        final Vector3D pM = provider.getTransform(date.shiftedBy(-h)).transformPosition(position);
        final Vector3D pP = provider.getTransform(date.shiftedBy(+h)).transformPosition(position);
        final Vector3D finiteDifference = new Vector3D(-1.0 / (2.0 * h), pM, 1.0 / (2.0 * h), pP);
        final Vector3D transformedVelocity =
                        scalarTransform.transformPVCoordinates(new PVCoordinates(position)).getVelocity();
        Assertions.assertEquals(0.0, Vector3D.distance(finiteDifference, transformedVelocity), 2.0e-9);

        final PVCoordinates pv = new PVCoordinates(position, new Vector3D(1200.0, 2900.0, -400.0));
        final PVCoordinates transformedPV = scalarTransform.transformPVCoordinates(pv);
        final PVCoordinates transformedOnlyPV = kinematicTransform.transformOnlyPV(pv);
        Assertions.assertEquals(0.0,
                                Vector3D.distance(transformedPV.getPosition(), transformedOnlyPV.getPosition()),
                                2.0e-9);
        Assertions.assertEquals(0.0,
                                Vector3D.distance(transformedPV.getVelocity(), transformedOnlyPV.getVelocity()),
                                2.0e-12);
    }

    private void checkPVAConsistency(final Frame source, final Frame destination, final AbsoluteDate date) {
        final Transform scalarTransform = source.getTransformTo(destination, date);
        final UnivariateDerivative2Field field = UnivariateDerivative2Field.getInstance();
        final FieldAbsoluteDate<UnivariateDerivative2> derivativeDate =
                        new FieldAbsoluteDate<>(field, date).shiftedBy(new UnivariateDerivative2(0, 1, 0));
        final FieldTransform<UnivariateDerivative2> fieldTransform = source.getTransformTo(destination, derivativeDate);
        final Transform referenceTransform =
                        new Transform(date,
                                      new PVCoordinates(fieldTransform.getTranslation().toVector3D(),
                                                        fieldTransform.getVelocity().toVector3D(),
                                                        fieldTransform.getAcceleration().toVector3D()),
                                      new AngularCoordinates(fieldTransform.getRotation().toRotation(),
                                                             fieldTransform.getRotationRate().toVector3D(),
                                                             fieldTransform.getRotationAcceleration().toVector3D()));

        final PVCoordinates pv = new PVCoordinates(new Vector3D(42000000.0, -12000000.0, 5000000.0),
                                                   new Vector3D(1200.0, 2900.0, -400.0),
                                                   new Vector3D(-0.2, 0.1, -0.05));
        final PVCoordinates actual = scalarTransform.transformPVCoordinates(pv);
        final PVCoordinates expected = referenceTransform.transformPVCoordinates(pv);
        Assertions.assertEquals(0.0, Vector3D.distance(expected.getPosition(), actual.getPosition()), 2.0e-8);
        Assertions.assertEquals(0.0, Vector3D.distance(expected.getVelocity(), actual.getVelocity()), 2.0e-12);
        Assertions.assertEquals(0.0, Vector3D.distance(expected.getAcceleration(), actual.getAcceleration()), 5.0e-15);
    }

}
