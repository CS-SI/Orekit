/* Copyright 2002-2025 CS GROUP
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
package org.orekit.models.earth.atmosphere;

import java.util.stream.IntStream;
import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathArrays;
import org.hipparchus.util.MathUtils;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.FieldGeodeticPoint;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateTimeComponents;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.utils.Constants;
import org.orekit.utils.ExtendedPositionProvider;

/**
 * Base class for Jacchia-Bowman atmospheric models.
 * @see JB2006
 * @see JB2008
 * @author Bruce R Bowman (HQ AFSPC, Space Analysis Division), Feb 2006: FORTRAN routine
 * @author Fabien Maussion (java translation)
 * @author Bryan Cazabonne (field elements translation)
 * @since 13.1
 */
public abstract class AbstractJacchiaBowmanModel extends AbstractSunInfluencedAtmosphere {

    /** Minimum altitude (m) for JB models use. */
    private static final double ALT_MIN = 90000.;

    /** The alpha are the thermal diffusion coefficients in equation (6). */
    private static final double[] ALPHA = {0, 0, 0, 0, -0.38};

    /** Natural logarithm of 10.0. */
    private static final double LOG10  = FastMath.log(10.);

    /** Molecular weights in order: N2, O2, O, Ar, He and H. */
    private static final double[] AMW = {28.0134, 31.9988, 15.9994, 39.9480, 4.0026, 1.00797};

    /** Avogadro's number in mks units (molecules/kmol). */
    private static final double AVOGAD = 6.02257e26;

    /** The FRAC are the assumed sea-level volume fractions in order: N2, O2, Ar, and He. */
    private static final double[] FRAC = {0.78110, 0.20955, 9.3400e-3, 1.2890e-5};

    /** Universal gas-constant in mks units (joules/K/kmol). */
    private static final double RSTAR  = 8.31432;

    /** Value used to establish height step sizes in the regime 90km to 105km. */
    private static final double R1 = 0.010;

    /** Value used to establish height step sizes in the regime 105km to 500km. */
    private static final double R2 = 0.025;

    /** Value used to establish height step sizes in the regime above 500km. */
    private static final double R3 = 0.075;

    /** Weights for the Newton-Cotes five-points quadrature formula. */
    private static final double[] WT = {0.311111111111111, 1.422222222222222, 0.533333333333333, 1.422222222222222, 0.311111111111111};

    /** Earth radius (km). */
    private static final double EARTH_RADIUS = 6356.766;

    /** DTC relative data. */
    private static final double[] BDT_SUB = {-0.457512297e+01, -0.512114909e+01, -0.693003609e+02,
                                             0.203716701e+03, 0.703316291e+03, -0.194349234e+04,
                                             0.110651308e+04, -0.174378996e+03, 0.188594601e+04,
                                             -0.709371517e+04, 0.922454523e+04, -0.384508073e+04,
                                             -0.645841789e+01, 0.409703319e+02, -0.482006560e+03,
                                             0.181870931e+04, -0.237389204e+04, 0.996703815e+03,
                                             0.361416936e+02};

    /** DTC relative data.  */
    private static final double[] CDT_SUB = {-0.155986211e+02, -0.512114909e+01, -0.693003609e+02,
                                             0.203716701e+03, 0.703316291e+03, -0.194349234e+04,
                                             0.110651308e+04, -0.220835117e+03, 0.143256989e+04,
                                             -0.318481844e+04, 0.328981513e+04, -0.135332119e+04,
                                             0.199956489e+02, -0.127093998e+02, 0.212825156e+02,
                                             -0.275555432e+01, 0.110234982e+02, 0.148881951e+03,
                                             -0.751640284e+03, 0.637876542e+03, 0.127093998e+02,
                                             -0.212825156e+02, 0.275555432e+01};

    /** Mbar polynomial coeffcients. */
    private static final double[] CXAMB = {28.15204, -8.5586e-2, +1.2840e-4, -1.0056e-5, -1.0210e-5, +1.5044e-6, +9.9826e-8};

    /** Coefficients for high altitude density correction. */
    private static final double[] CHT = {0.22, -0.20e-02, 0.115e-02, -0.211e-05};

    /** UTC time scale. */
    private final TimeScale utc;

    /** Earth body shape. */
    private final BodyShape earth;

    /** Earliest epoch of solar activity data. */
    private final AbsoluteDate minDataEpoch;

    /** Latest epoch of solar activity data. */
    private final AbsoluteDate maxDataEpoch;

    /**
     * Constructor.
     *
     * @param sun          position provider.
     * @param utc          UTC time scale. Used to computed the day fraction.
     * @param earth        the earth body shape
     * @param minDataEpoch earliest epoch of solar activity data
     * @param maxDataEpoch latest epoch of solar activity data
     */
    protected AbstractJacchiaBowmanModel(final ExtendedPositionProvider sun, final TimeScale utc, final BodyShape earth,
                                         final AbsoluteDate minDataEpoch, final AbsoluteDate maxDataEpoch) {
        super(sun);
        this.utc          = utc;
        this.earth        = earth;
        this.minDataEpoch = minDataEpoch;
        this.maxDataEpoch = maxDataEpoch;
    }

    /** Get the UTC time scale.
     * @return UTC time scale
     */
    public TimeScale getUtc() {
        return utc;
    }

    /** Get the Earth body shape.
     * @return the Earth body shape
     */
    public BodyShape getEarth() {
        return earth;
    }

    /** {@inheritDoc}*/
    @Override
    public double getDensity(final AbsoluteDate date, final Vector3D position, final Frame frame) {

        // Verify availability of data
        if (date.compareTo(maxDataEpoch) > 0 || date.compareTo(minDataEpoch) < 0) {
            throw new OrekitException(OrekitMessages.NO_SOLAR_ACTIVITY_AT_DATE, date, minDataEpoch, maxDataEpoch);
        }

        // compute geodetic position
        final GeodeticPoint inBody = earth.transform(position, frame, date);

        // compute sun position
        final Frame ecef = getFrame();
        final Vector3D sunPos = getSunPosition(date, ecef);
        final GeodeticPoint sunInBody = earth.transform(sunPos, ecef, date);
        return computeDensity(date, sunInBody.getLongitude(), sunInBody.getLatitude(), inBody.getLongitude(), inBody.getLatitude(), inBody.getAltitude());
    }

    /** {@inheritDoc}*/
    @Override
    public <T extends CalculusFieldElement<T>> T getDensity(final FieldAbsoluteDate<T> date, final FieldVector3D<T> position, final Frame frame) {

        // Verify availability of data
        final AbsoluteDate dateD = date.toAbsoluteDate();
        if (dateD.compareTo(maxDataEpoch) > 0 || dateD.compareTo(minDataEpoch) < 0) {
            throw new OrekitException(OrekitMessages.NO_SOLAR_ACTIVITY_AT_DATE, dateD, minDataEpoch, maxDataEpoch);
        }

        // compute geodetic position (km and °)
        final FieldGeodeticPoint<T> inBody = earth.transform(position, frame, date);

        // compute sun position
        final Frame ecef = getFrame();
        final FieldVector3D<T> sunPos = getSunPosition(date, ecef);
        final FieldGeodeticPoint<T> sunInBody = earth.transform(sunPos, ecef, date);
        return computeDensity(date,
                              sunInBody.getLongitude(), sunInBody.getLatitude(),
                              inBody.getLongitude(), inBody.getLatitude(), inBody.getAltitude());
    }

    /** {@inheritDoc} */
    @Override
    public Frame getFrame() {
        return earth.getBodyFrame();
    }

    /** Computes the local density with initial entries.
     * @param date computation epoch
     * @param sunRA Right Ascension of Sun (radians)
     * @param sunDecli Declination of Sun (radians)
     * @param satLon Right Ascension of position (radians)
     * @param satLat Geocentric latitude of position (radians)
     * @param satAlt Height of position (m)
     * @return total mass-Density at input position (kg/m³)
     */
    protected double computeDensity(final AbsoluteDate date,
                                    final double sunRA, final double sunDecli,
                                    final double satLon, final double satLat, final double satAlt) {

        if (satAlt < ALT_MIN) {
            throw new OrekitException(OrekitMessages.ALTITUDE_BELOW_ALLOWED_THRESHOLD, satAlt, ALT_MIN);
        }
        final double altKm = satAlt / 1000.0;

        final DateTimeComponents dt = date.getComponents(utc);
        final double dateMJD = dt.getDate().getMJD() +
                               dt.getTime().getSecondsInLocalDay() / Constants.JULIAN_DAY;

        // Equation (14)
        // Temperature equation obtained using numerous satellites for the years from 1996 through 2004 when all new solar indices were available
        final double tsubc = computeTc(date);

        // Equation (15)
        final double eta = 0.5 * FastMath.abs(satLat - sunDecli);
        final double theta = 0.5 * FastMath.abs(satLat + sunDecli);

        // Equation (16)
        final double h   = satLon - sunRA;
        final double tau = h - 0.64577182 + 0.10471976 * FastMath.sin(h + 0.75049158);
        final double solarTime = solarTimeHour(h);

        // Equation (17)
        final double tsubl = tSubL(eta, theta, tau, tsubc);

        // Compute correction to dTc for local solar time and lat correction
        final double dtclst = dTc(getF10(date), solarTime, satLat, altKm);

        // Compute the local exospheric temperature.
        final double tInf = computeTInf(date, tsubl, dtclst);

        // Equation (9)
        final double tsubx = tSubX(tInf);

        // Equation (11)
        final double gsubx = gSubX(tsubx);

        // The TC array will be an argument in the call to "localTemp"
        final double[] tc = tSubCArray(tsubx, gsubx, tInf);

        // Equation (5)
        final double z1    = 90.;
        final double z2    = FastMath.min(altKm, 105.0);
        double al          = FastMath.log(z2 / z1);
        int n              = (int) FastMath.floor(al / R1) + 1;
        double zr          = FastMath.exp(al / n);
        final double mb1   = mBar(z1);
        final double tloc1 = localTemp(z1, tc);
        double zend        = z1;
        double sub2        = 0.;
        double ain         = mb1 * gravity(z1) / tloc1;
        double mb2         = 0;
        double tloc2       = 0;
        double z           = 0;
        double gravl       = 0;

        for (int i = 0; i < n; ++i) {
            z = zend;
            zend = zr * z;
            final double dz = 0.25 * (zend - z);
            double sum1 = WT[0] * ain;
            for (int j = 1; j < 5; ++j) {
                z += dz;
                mb2   = mBar(z);
                tloc2 = localTemp(z, tc);
                gravl = gravity(z);
                ain   = mb2 * gravl / tloc2;
                sum1 += WT[j] * ain;
            }
            sub2 += dz * sum1;
        }
        double rho = 3.46e-6 * mb2 * tloc1 / FastMath.exp(sub2 / RSTAR) / (mb1 * tloc2);

        // Equation (2)
        final double anm = AVOGAD * rho;
        final double an = anm / mb2;

        // Equation (3)
        double fact2 = anm / 28.960;
        final double[] aln = new double[6];
        aln[0] = FastMath.log(FRAC[0] * fact2);
        aln[3] = FastMath.log(FRAC[2] * fact2);
        aln[4] = FastMath.log(FRAC[3] * fact2);
        // Equation (4)
        aln[1] = FastMath.log(fact2 * (1. + FRAC[1]) - an);
        aln[2] = FastMath.log(2. * (an - fact2));

        if (altKm <= 105.0) {
            // Put in negligible hydrogen for use in DO-LOOP 13
            aln[5] = aln[4] - 25.0;
        }
        else {
            // Equation (6)
            al = FastMath.log(FastMath.min(altKm, 500.0) / z);
            n = 1 + (int) FastMath.floor(al / R2);
            zr = FastMath.exp(al / n);
            sub2 = 0.;
            ain = gravl / tloc2;

            double tloc3 = 0;
            for (int i = 0; i < n; ++i) {
                z = zend;
                zend = zr * z;
                final double dz = 0.25 * (zend - z);
                double SUM1 = WT[0] * ain;
                for (int j = 1; j < 5; ++j) {
                    z += dz;
                    tloc3 = localTemp(z, tc);
                    gravl = gravity(z);
                    ain = gravl / tloc3;
                    SUM1 += WT[j] * ain;
                }
                sub2 += dz * SUM1;
            }

            al = FastMath.log(FastMath.max(altKm, 500.0) / z);
            final double r = (altKm > 500.0) ? R3 : R2;
            n = 1 + (int) FastMath.floor(al / r);
            zr = FastMath.exp(al / n);
            double sum3 = 0.;
            double tloc4 = 0;
            for (int i = 0; i < n; ++i) {
                z = zend;
                zend = zr * z;
                final double dz = 0.25 * (zend - z);
                double sum1 = WT[0] * ain;
                for (int j = 1; j < 5; ++j) {
                    z += dz;
                    tloc4 = localTemp(z, tc);
                    gravl = gravity(z);
                    ain = gravl / tloc4;
                    sum1 = sum1 + WT[j] * ain;
                }
                sum3 = sum3 + dz * sum1;
            }
            final double altr;
            final double hSign;
            if (altKm <= 500.) {
                altr = FastMath.log(tloc3 / tloc2);
                fact2 = sub2 / RSTAR;
                hSign = 1.0;
            }
            else {
                altr = FastMath.log(tloc4 / tloc2);
                fact2 = (sub2 + sum3) / RSTAR;
                hSign = -1.0;
            }
            for (int i = 0; i < 5; ++i) {
                aln[i] = aln[i] - (1.0 + ALPHA[i]) * altr - fact2 * AMW[i];
            }

            // Equation (7) - Note that in CIRA72, AL10T5 = DLOG10(T500)
            final double al10t5 = FastMath.log10(tInf);
            final double alnh5 = (5.5 * al10t5 - 39.40) * al10t5 + 73.13;
            aln[5] = LOG10 * (alnh5 + 6.) + hSign * (FastMath.log(tloc4 / tloc3) + sum3 * AMW[5] / RSTAR);
        }

        // Equation (24)  - J70 Seasonal-Latitudinal Variation
        final double dlrsl = dlrsl(altKm, dateMJD, satLat);

        // Equation (23) - Computes the semiannual variation
        double dlrsa = 0;
        if (z < 2000.0) {
            // Use new semiannual model DELTA LOG RHO
            dlrsa = semian(date, dayOfYear(dateMJD), altKm);
        }

        // Sum the delta-log-rhos and apply to the number densities.
        // In CIRA72 the following equation contains an actual sum,
        // namely DLR = AL10 * (DLRGM + DLRSA + DLRSL)
        // However, for Jacchia 70, there is no DLRGM or DLRSA.
        final double dlr = LOG10 * (dlrsl + dlrsa);
        for (int i = 0; i < 6; ++i) {
            aln[i] += dlr;
        }

        // Compute mass-density and mean-molecular-weight and
        // convert number density logs from natural to common.
        final double sumnm = IntStream.range(0, 6).mapToDouble(i -> FastMath.exp(aln[i]) * AMW[i]).sum();
        rho = sumnm / AVOGAD;

        // Compute the high altitude exospheric density correction factor
        final double fex = densityCorrectionFactor(altKm, getF10B(date));

        // Apply the exospheric density correction factor.
        rho *= fex;

        return rho;
    }

    /** Computes the local density with initial entries.
     * @param date computation epoch
     * @param sunRA Right Ascension of Sun (radians)
     * @param sunDecli Declination of Sun (radians)
     * @param satLon Right Ascension of position (radians)
     * @param satLat Geocentric latitude of position (radians)
     * @param satAlt Height of position (m)
     * @param <T> type of the elements
     * @return total mass-Density at input position (kg/m³)
     */
    protected <T extends CalculusFieldElement<T>> T computeDensity(final FieldAbsoluteDate<T> date,
                                                                   final T sunRA, final T sunDecli,
                                                                   final T satLon, final T satLat, final T satAlt) {

        if (satAlt.getReal() < ALT_MIN) {
            throw new OrekitException(OrekitMessages.ALTITUDE_BELOW_ALLOWED_THRESHOLD, satAlt, ALT_MIN);
        }

        final AbsoluteDate dateR = date.toAbsoluteDate();
        final DateTimeComponents components = date.getComponents(utc);
        final T dateMJD = date.durationFrom(new FieldAbsoluteDate<>(date.getField(), components, utc))
                              .add(components.getTime().getSecondsInLocalDay())
                              .divide(Constants.JULIAN_DAY)
                              .add(components.getDate().getMJD());

        final Field<T> field  = satAlt.getField();
        final T altKm = satAlt.divide(1000.0);

        // Equation (14) (Temperature equation)
        final double tsubc = computeTc(dateR);

        // Equation (15)
        final T eta   = FastMath.abs(satLat.subtract(sunDecli)).multiply(0.5);
        final T theta = FastMath.abs(satLat.add(sunDecli)).multiply(0.5);

        // Equation (16)
        final T h = satLon.subtract(sunRA);
        final T tau = h.subtract(0.64577182).add(h.add(0.75049158).sin().multiply(0.10471976));
        final T solarTime = solarTimeHour(h);

        // Equation (17)
        final T tsubl = tSubL(eta, theta, tau, tsubc);

        // Compute correction to dTc for local solar time and lat correction
        final T dtclst = dTc(getF10(dateR), solarTime, satLat, altKm);

        // Compute the local exospheric temperature.
        final T tinf = computeTInf(dateR, tsubl, dtclst);

        // Equation (9)
        final T tsubx = tSubX(tinf);

        // Equation (11)
        final T gsubx = gSubX(tsubx);

        // The TC array will be an argument in the call to "localTemp"
        final T[] tc = tSubCArray(tsubx, gsubx, tinf, field);

        // Equation (5)
        final T z1    = field.getZero().newInstance(90.);
        final T z2    = FastMath.min(altKm, 105.0);
        T al          = z2.divide(z1).log();
        int n         = 1 + (int) FastMath.floor(al.getReal() / R1);
        T zr          = al.divide(n).exp();
        final T mb1   = mBar(z1);
        final T tloc1 = localTemp(z1, tc);
        T zend        = z1;
        T sub2        = field.getZero();
        T ain         = mb1.multiply(gravity(z1)).divide(tloc1);
        T mb2         = field.getZero();
        T tloc2       = field.getZero();
        T z           = field.getZero();
        T gravl       = field.getZero();

        for (int i = 0; i < n; ++i) {
            z = zend;
            zend = zr.multiply(z);
            final T dz = zend.subtract(z).multiply(0.25);
            T sum1 = ain.multiply(WT[0]);
            for (int j = 1; j < 5; ++j) {
                z = z.add(dz);
                mb2   = mBar(z);
                tloc2 = localTemp(z, tc);
                gravl = gravity(z);
                ain   = mb2.multiply(gravl).divide(tloc2);
                sum1  = sum1.add(ain.multiply(WT[j]));
            }
            sub2 = sub2.add(dz.multiply(sum1));
        }
        T rho = mb2.multiply(3.46e-6).multiply(tloc1).divide(sub2.divide(RSTAR).exp().multiply(mb1.multiply(tloc2)));

        // Equation (2)
        final T anm = rho.multiply(AVOGAD);
        final T an  = anm.divide(mb2);

        // Equation (3)
        T fact2  = anm.divide(28.960);
        final T[] aln = MathArrays.buildArray(field, 6);
        aln[0] = fact2.multiply(FRAC[0]).log();
        aln[3] = fact2.multiply(FRAC[2]).log();
        aln[4] = fact2.multiply(FRAC[3]).log();

        // Equation (4)
        aln[1] = fact2.multiply(1. + FRAC[1]).subtract(an).log();
        aln[2] = an.subtract(fact2).multiply(2).log();

        if (altKm.getReal() <= 105.0) {
            // Put in negligible hydrogen for use in DO-LOOP 13
            aln[5] = aln[4].subtract(25.0);
        }
        else {
            // Equation (6)
            al   = FastMath.min(altKm, 500.0).divide(z).log();
            n    = 1 + (int) FastMath.floor(al.getReal() / R2);
            zr   = al.divide(n).exp();
            sub2 = field.getZero();
            ain  = gravl.divide(tloc2);

            T tloc3 = field.getZero();
            for (int i = 0; i < n; ++i) {
                z = zend;
                zend = zr.multiply(z);
                final T dz = zend.subtract(z).multiply(0.25);
                T sum1 = ain.multiply(WT[0]);
                for (int j = 1; j < 5; ++j) {
                    z     = z.add(dz);
                    tloc3 = localTemp(z, tc);
                    gravl = gravity(z);
                    ain   = gravl.divide(tloc3);
                    sum1  = sum1.add(ain.multiply(WT[j]));
                }
                sub2 = sub2.add(dz.multiply(sum1));
            }

            al = FastMath.max(altKm, 500.0).divide(z).log();
            final double r = (altKm.getReal() > 500.0) ? R3 : R2;
            n = 1 + (int) FastMath.floor(al.getReal() / r);
            zr = al.divide(n).exp();
            T sum3 = field.getZero();
            T tloc4 = field.getZero();
            for (int i = 0; i < n; ++i) {
                z = zend;
                zend = zr.multiply(z);
                final T dz = zend.subtract(z).multiply(0.25);
                T sum1 = ain.multiply(WT[0]);
                for (int j = 1; j < 5; ++j) {
                    z = z.add(dz);
                    tloc4 = localTemp(z, tc);
                    gravl = gravity(z);
                    ain   = gravl.divide(tloc4);
                    sum1  = sum1.add(ain.multiply(WT[j]));
                }
                sum3 = sum3.add(dz.multiply(sum1));
            }
            final T altr;
            final double hSign;
            if (altKm.getReal() <= 500.) {
                altr = tloc3.divide(tloc2).log();
                fact2 = sub2.divide(RSTAR);
                hSign = 1.0;
            }
            else {
                altr = tloc4.divide(tloc2).log();
                fact2 = sub2.add(sum3).divide(RSTAR);
                hSign = -1.0;
            }
            for (int i = 0; i < 5; ++i) {
                aln[i] = aln[i].subtract(altr.multiply(1.0 + ALPHA[i])).subtract(fact2.multiply(AMW[i]));
            }

            // Equation (7) - Note that in CIRA72, AL10T5 = DLOG10(T500)
            final T al10t5 = tinf.log10();
            final T alnh5 = al10t5.multiply(5.5).subtract(39.40).multiply(al10t5).add(73.13);
            aln[5] = alnh5.add(6.).multiply(LOG10).add(tloc4.divide(tloc3).log().add(sum3.multiply(AMW[5] / RSTAR)).multiply(hSign));
        }

        // Equation (24)  - J70 Seasonal-Latitudinal Variation
        final T dlrsl = dlrsl(altKm, dateMJD, satLat);

        // Equation (23) - Computes the semiannual variation
        T dlrsa = field.getZero();
        if (z.getReal() < 2000.0) {
            // Use new semiannual model DELTA LOG RHO
            dlrsa = semian(dateR, dayOfYear(dateMJD), altKm);
        }

        // Sum the delta-log-rhos and apply to the number densities.
        // In CIRA72 the following equation contains an actual sum,
        // namely DLR = AL10 * (DLRGM + DLRSA + DLRSL)
        // However, for Jacchia 70, there is no DLRGM or DLRSA.
        final T dlr = dlrsl.add(dlrsa).multiply(LOG10);
        for (int i = 0; i < 6; ++i) {
            aln[i] = aln[i].add(dlr);
        }

        // Compute mass-density and mean-molecular-weight and
        // convert number density logs from natural to common.
        T sumnm = field.getZero();
        for (int i = 0; i < 6; ++i) {
            sumnm = sumnm.add(aln[i].exp().multiply(AMW[i]));
        }
        rho = sumnm.divide(AVOGAD);

        // Compute the high altitude exospheric density correction factor
        final T fex = densityCorrectionFactor(altKm, getF10B(dateR), field);

        // Apply the exospheric density correction factor.
        rho = rho.multiply(fex);

        return rho;
    }

    /** Computes the semi-annual variation (delta log(rho)).
     * @param date computation epoch
     * @param day day of year
     * @param altKm height (km)
     * @return semi-annual variation
     */
    protected abstract double semian(AbsoluteDate date, double day, double altKm);

    /**
     * Computes the local exospheric temperature.
     * @param date computation epoch
     * @param tsubl exospheric temperature ("tsubl" term), given by Equation (17)
     * @param dtclst correction to dTc for local solar time and lat correction
     * @return the local exospheric temperature
     */
    protected abstract double computeTInf(AbsoluteDate date, double tsubl, double dtclst);

    /** Computes the temperature equation.
     * @param date computation epoch
     * @return the temperature equation
     */
    protected abstract double computeTc(AbsoluteDate date);

    /** Get the 10.7-cm Solar flux (1e<sup>-22</sup>*Watt/(m²*Hertz))<br> (Tabular time 1.0 day earlier).
     * @param date computation epoch
     * @return the 10.7-cm Solar flux from model input parameters
     */
    protected abstract double getF10(AbsoluteDate date);

    /** Get the 10.7-cm Solar Flux, averaged 81-day centered on the input time<br> (Tabular time 1.0 day earlier).
     * @param date computation epoch
     * @return the 10.7-cm Solar Flux, averaged 81-day centered on the input time
     */
    protected abstract double getF10B(AbsoluteDate date);

    /** Computes the semi-annual variation (delta log(rho)).
     * @param date computation epoch
     * @param day day of year
     * @param altKm height (km)
     * @param <T> type of the elements
     * @return semi-annual variation
     */
    protected abstract <T extends CalculusFieldElement<T>> T semian(AbsoluteDate date, T day, T altKm);

    /**
     * Computes the local exospheric temperature.
     * @param date computation epoch
     * @param tsubl exospheric temperature ("tsubl" term), given by Equation (17)
     * @param dtclst correction to dTc for local solar time and lat correction
     * @param <T> type of the elements
     * @return the local exospheric temperature
     */
    protected abstract <T extends CalculusFieldElement<T>> T computeTInf(AbsoluteDate date, T tsubl, T dtclst);

    /** Evaluates the solar time in hours.
     * @param h difference between the Right Ascension of position and the Right Ascension of Sun (radians)
     * @return the solar time in hours
     */
    private static double solarTimeHour(final double h) {
        final double solTimeHour = FastMath.toDegrees(h + FastMath.PI) / 15.0;
        if (solTimeHour >= 24) {
            return solTimeHour - 24.;
        }
        if (solTimeHour < 0) {
            return solTimeHour + 24.;
        }
        return solTimeHour;
    }

    /** Evaluates the solar time in hours.
     * @param h difference between the Right Ascension of position and the Right Ascension of Sun (radians)
     * @param <T> type of the field elements
     * @return the solar time in hours
     */
    private static <T extends CalculusFieldElement<T>> T solarTimeHour(final T h) {
        final T solarTime = FastMath.toDegrees(h.add(FastMath.PI)).divide(15.0);
        if (solarTime.getReal() >= 24) {
            return solarTime.subtract(24);
        }
        if (solarTime.getReal() < 0) {
            return solarTime.add(24);
        }
        return solarTime;
    }

    /** Evaluates the exospheric temperature ("tsubl" term), Equation (17).
     * <p>
     * This temperature corresponds to the exospheric temperature in low
     * geomagnetic conditions.
     * </p>
     * @param eta "eta" term, Equation (15)
     * @param theta "theta" term, Equation (15)
     * @param tau "tau" term, Equation (16)
     * @param tsubc exospheric temperature Tc, Equation (14)
     * @return Tl temperature computed by Equation (17)
     */
    private static double tSubL(final double eta, final double theta,
                               final double tau, final double tsubc) {
        final double cosEta   = FastMath.pow(FastMath.cos(eta), 2.5);
        final double sinTheta = FastMath.pow(FastMath.sin(theta), 2.5);
        final double cosTau   = FastMath.abs(FastMath.cos(0.5 * tau));
        final double df       = sinTheta + (cosEta - sinTheta) * cosTau * cosTau * cosTau;
        return tsubc * (1. + 0.31 * df);
    }

    /** Evaluates exospheric temperature ("tsubl" term), Equation (17).
     * <p>
     * This temperature corresponds to the exospheric temperature in low
     * geomagnetic conditions.
     * </p>
     * @param eta "eta" term, Equation (15)
     * @param theta "theta" term, Equation (15)
     * @param tau "tau" term, Equation (16)
     * @param tsubc exospheric temperature Tc, Equation (14)
     * @param <T> type of the field elements
     * @return Tl temperature computed by Equation (17)
     */
    private static <T extends CalculusFieldElement<T>>  T tSubL(final T eta, final T theta,
                                                               final T tau, final double tsubc) {
        final T cos      = eta.cos();
        final T cosEta   = cos.square().multiply(cos.sqrt());
        final T sin      = theta.sin();
        final T sinTheta = sin.square().multiply(sin.sqrt());
        final T cosTau   = tau.multiply(0.5).cos().abs();
        final T df       = sinTheta.add(cosEta.subtract(sinTheta).multiply(cosTau).multiply(cosTau).multiply(cosTau));
        return df.multiply(0.31).add(1).multiply(tsubc);
    }

    /** Evaluates the inflection temperature ("tsubx" term), Equation (9).
     * <p>
     * This temperature corresponds to the temperature at the inflexion altitude.
     * At this altitude, the temperature profile has an inflection point.
     * </p>
     * @param tInf local exospheric temperature
     * @return Tx temperature at inflection point, computed by Equation (9)
     */
    private static double tSubX(final double tInf) {
        return 444.3807 + 0.02385 * tInf - 392.8292 * FastMath.exp(-0.0021357 * tInf);
    }

    /** Evaluates the inflection temperature ("tsubx" term), Equation (9).
     * <p>
     * This temperature corresponds to the temperature at the inflexion altitude.
     * At this altitude, the temperature profile has an inflection point.
     * </p>
     * @param tInf local exospheric temperature
     * @param <T> type of the field elements
     * @return Tx temperature at inflection point, computed by Equation (9)
     */
    private static <T extends CalculusFieldElement<T>>  T tSubX(final T tInf) {
        return tInf.multiply(0.02385).add(444.3807).subtract(tInf.multiply(-0.0021357).exp().multiply(392.8292));
    }

    /** Evaluates the temperature gradient at the inflection point ("gsubx" term), Equation (11).
     * @param tSubX temperature at inflection point
     * @return the temperature gradient at the inflection point computed by Equation (11)
     */
    private static double gSubX(final double tSubX) {
        return 0.054285714 * (tSubX - 183.);
    }

    /** Evaluates the temperature gradient at the inflection point ("gsubx" term), Equation (11).
     * @param tSubX temperature at inflection point
     * @param <T> type of the field elements
     * @return the temperature gradient at the inflection point computed by Equation (11)
     */
    private static <T extends CalculusFieldElement<T>> T gSubX(final T tSubX) {
        return tSubX.subtract(183.).multiply(0.054285714);
    }

    /** Evaluates the Tc array.
     * <p>
     * The Tc array will be used to evaluates {@link #localTemp(double, double[])}.
     * </p>
     * @param tSubX temperature at inflection point
     * @param gSubX the temperature gradient at the inflection point
     * @param tInf local exospheric temperature
     * @return the Tc array
     */
    private static double[] tSubCArray(final double tSubX, final double gSubX,
                                      final double tInf) {
        final double[] tc = new double[4];
        tc[0] = tSubX;
        tc[1] = gSubX;
        tc[2] = (tInf - tSubX) / MathUtils.SEMI_PI;
        tc[3] = gSubX / tc[2];
        return tc;
    }

    /** Evaluates the Tc array.
     * <p>
     * The Tc array will be used to evaluates {@link #localTemp(CalculusFieldElement, CalculusFieldElement[])}
     * </p>
     * @param tSubX temperature at inflection point
     * @param gSubX the temperature gradient at the inflection point
     * @param tInf local exospheric temperature
     * @param field field for the elements
     * @param <T> type of the field elements
     * @return the Tc array
     */
    private static <T extends CalculusFieldElement<T>> T[] tSubCArray(final T tSubX, final T gSubX,
                                                                     final T tInf, final Field<T> field) {
        final T[] tc = MathArrays.buildArray(field, 4);
        tc[0] = tSubX;
        tc[1] = gSubX;
        tc[2] = tInf.subtract(tSubX).divide(MathUtils.SEMI_PI);
        tc[3] = gSubX.divide(tc[2]);
        return tc;
    }

    /** Evaluates the local temperature, Equation (10) or (13) depending on altitude.
     * @param z  altitude
     * @param tc tc array
     * @return temperature profile
     */
    private static double localTemp(final double z, final double[] tc) {
        final double dz = z - 125;
        if (dz <= 0) {
            return ((-9.8204695e-6 * dz - 7.3039742e-4) * dz * dz + 1.0) * dz * tc[1] + tc[0];
        }
        else {
            return tc[0] + tc[2] * FastMath.atan(tc[3] * dz * (1 + 4.5e-6 * FastMath.pow(dz, 2.5)));
        }
    }

    /** Evaluates the local temperature, Equation (10) or (13) depending on altitude.
     * @param z altitude
     * @param tc tc array
     * @return temperature profile
     * @param <T> type of the field elements
     */
    private static <T extends CalculusFieldElement<T>> T localTemp(final T z, final T[] tc) {
        final T dz = z.subtract(125.);
        if (dz.getReal() <= 0.) {
            return dz.multiply(-9.8204695e-6).subtract(7.3039742e-4).multiply(dz).multiply(dz).add(1.0).multiply(dz).multiply(tc[1]).add(tc[0]);
        } else {
            return dz.multiply(dz).multiply(dz.sqrt()).multiply(4.5e-6).add(1).multiply(dz).multiply(tc[3]).atan().multiply(tc[2]).add(tc[0]);
        }
    }

    /** Evaluates mean molecualr mass, Equation (1).
     * @param z altitude (km)
     * @return mean molecular mass
     */
    private static double mBar(final double z) {
        final double dz = z - 100.;
        double amb = CXAMB[6];
        for (int i = 5; i >= 0; --i) {
            amb = dz * amb + CXAMB[i];
        }
        return amb;
    }

    /** Evaluates mean molecualr mass, Equation (1).
     * @param z altitude (km)
     * @return mean molecular mass
     * @param <T> type of the field elements
     */
    private static <T extends CalculusFieldElement<T>>  T mBar(final T z) {
        final T dz = z.subtract(100.);
        T amb = z.getField().getZero().newInstance(CXAMB[6]);
        for (int i = 5; i >= 0; --i) {
            amb = dz.multiply(amb).add(CXAMB[i]);
        }
        return amb;
    }

    /** Evaluates the gravity at the altitude, Equation (8).
     * @param z altitude (km)
     * @return the gravity field (m/s2)
     */
    private static double gravity(final double z) {
        final double temp = 1.0 + z / EARTH_RADIUS;
        return Constants.G0_STANDARD_GRAVITY / (temp * temp);
    }

    /** Evaluates the gravity at the altitude, Equation (8).
     * @param z altitude (km)
     * @return the gravity (m/s2)
     * @param <T> type of the field elements
     */
    private static <T extends CalculusFieldElement<T>> T gravity(final T z) {
        final T tmp = z.divide(EARTH_RADIUS).add(1);
        return tmp.multiply(tmp).reciprocal().multiply(Constants.G0_STANDARD_GRAVITY);
    }

    /** Compute day of year.
     * @param dateMJD Modified Julian date
     * @return the number days in year
     */
    private static double dayOfYear(final double dateMJD) {
        final double d1950 = dateMJD - 33281;

        int iyday = (int) d1950;
        final double frac = d1950 - iyday;
        iyday = iyday + 364;

        int itemp = iyday / 1461;

        iyday = iyday - itemp * 1461;
        itemp = iyday / 365;
        if (itemp >= 3) {
            itemp = 3;
        }
        iyday = iyday - 365 * itemp + 1;
        return iyday + frac;
    }

    /** Compute day of year.
     * @param dateMJD Modified Julian date
     * @param <T> type of the field elements
     * @return the number days in year
     */
    private static <T extends CalculusFieldElement<T>> T dayOfYear(final T dateMJD) {
        final T d1950 = dateMJD.subtract(33281);

        int iyday = (int) d1950.getReal();
        final T frac = d1950.subtract(iyday);
        iyday = iyday + 364;

        int itemp = iyday / 1461;

        iyday = iyday - itemp * 1461;
        itemp = iyday / 365;
        if (itemp >= 3) {
            itemp = 3;
        }
        iyday = iyday - 365 * itemp + 1;
        return frac.add(iyday);
    }

    /** Evaluates the J70 Seasonal-Latitudinal Variation, Equation (24).
     * @param altKm satellite altitude (km)
     * @param dateMJD Modified Julian date
     * @param satLat Geocentric latitude of position (radians)
     * @return the J70 Seasonal-Latitudinal Variation
     */
    private static double dlrsl(final double altKm, final double dateMJD, final double satLat) {
        final double capPhi = ((dateMJD - 36204.0) / 365.2422) % 1;
        final int signum = (satLat >= 0) ? 1 : -1;
        final double sinLat = FastMath.sin(satLat);
        final double hm90  = altKm - 90.;
        return 0.02 * hm90 * FastMath.exp(-0.045 * hm90) * signum * FastMath.sin(MathUtils.TWO_PI * capPhi + 1.72) * sinLat * sinLat;
    }

    /** Evaluates the J70 Seasonal-Latitudinal Variation, Equation (24).
     * @param altKm satellite altitude (km)
     * @param dateMJD Modified Julian date
     * @param satLat Geocentric latitude of position (radians)
     * @param <T> type of the elements
     * @return the J70 Seasonal-Latitudinal Variation
     */
    private static <T extends CalculusFieldElement<T>> T dlrsl(final T altKm, final T dateMJD, final T satLat) {
        T capPhi = dateMJD.subtract(36204.0).divide(365.2422);
        capPhi = capPhi.subtract(FastMath.floor(capPhi.getReal()));
        final int signum = (satLat.getReal() >= 0.) ? 1 : -1;
        final T sinLat = satLat.sin();
        final T hm90  = altKm.subtract(90.);
        return hm90.multiply(0.02).multiply(hm90.multiply(-0.045).exp()).multiply(capPhi.multiply(MathUtils.TWO_PI).add(1.72).sin()).multiply(signum).multiply(sinLat).multiply(sinLat);
    }

    /** Evaluates the high altitude exospheric density correction factor.
     * @param altKm satellite altitude in km
     * @param f10B 10.7-cm Solar Flux, averaged 81-day centered on the input time
     * @return the high altitude exospheric density correction factor
     */
    private static double densityCorrectionFactor(final double altKm, final double f10B) {
        if (altKm >= 1000.0 && altKm < 1500.0) {
            final double zeta = (altKm - 1000.) * 0.002;
            final double f15c = CHT[0] + CHT[1] * f10B + CHT[2] * 1500.0 + CHT[3] * f10B * 1500.0;
            final double f15cZeta = (CHT[2] + CHT[3] * f10B) * 500.0;
            final double fex2 = 3.0 * f15c - f15cZeta - 3.0;
            final double fex3 = f15cZeta - 2.0 * f15c + 2.0;
            return 1.0 + zeta * zeta * (fex2 + fex3 * zeta);
        }
        if (altKm >= 1500.0) {
            return CHT[0] + CHT[1] * f10B + CHT[2] * altKm + CHT[3] * f10B * altKm;
        }
        return 1.0;
    }

    /** Evaluates the high altitude exospheric density correction factor.
     * @param altKm satellite altitude in km
     * @param f10B 10.7-cm Solar Flux, averaged 81-day centered on the input time
     * @param field field of the elements
     * @param <T> type of the field elements
     * @return the high altitude exospheric density correction factor
     */
    private static <T extends CalculusFieldElement<T>> T densityCorrectionFactor(final T altKm, final double f10B,
                                                                                final Field<T> field) {
        if (altKm.getReal() >= 1000.0 && altKm.getReal() < 1500.0) {
            final T zeta = altKm.subtract(1000.).multiply(0.002);
            final double f15c = CHT[0] + CHT[1] * f10B + CHT[2] * 1500.0 + CHT[3] * f10B * 1500.0;
            final double f15cZeta = (CHT[2] + CHT[3] * f10B) * 500.0;
            final double fex2 = 3.0 * f15c - f15cZeta - 3.0;
            final double fex3 = f15cZeta - 2.0 * f15c + 2.0;
            return field.getOne().add(zeta.multiply(zeta).multiply(zeta.multiply(fex3).add(fex2)));
        }
        if (altKm.getReal() >= 1500.0) {
            return altKm.multiply(CHT[3] * f10B).add(altKm.multiply(CHT[2])).add(CHT[0] + CHT[1] * f10B);
        }
        return field.getOne();
    }

    /** Compute daily temperature correction for Jacchia-Bowman model.
     * @param f10 solar flux index
     * @param solarTime local solar time (hours in [0, 24[)
     * @param satLat sat lat (radians)
     * @param satAlt height (km)
     * @return dTc correction
     */
    private static double dTc(final double f10, final double solarTime,
                              final double satLat, final double satAlt) {
        final double st = solarTime / 24.0;
        final double cs = FastMath.cos(satLat);
        final double fs = (f10 - 100.0) / 100.0;

        // Calculates dTc according to height
        if (satAlt >= 120 && satAlt <= 200) {
            final double dtc200   = poly2CDTC(fs, st, cs);
            final double dtc200dz = poly1CDTC(fs, st, cs);
            final double cc       = 3.0 * dtc200 - dtc200dz;
            final double dd       = dtc200 - cc;
            final double zp       = (satAlt - 120.0) / 80.0;
            return zp * zp * (cc + dd * zp);

        } else if (satAlt > 200.0 && satAlt <= 240.0) {
            final double h = (satAlt - 200.0) / 50.0;
            return poly1CDTC(fs, st, cs) * h + poly2CDTC(fs, st, cs);

        } else if (satAlt > 240.0 && satAlt <= 300.0) {
            final double h        = 0.8;
            final double bb       = poly1CDTC(fs, st, cs);
            final double aa       = bb * h + poly2CDTC(fs, st, cs);
            final double p2BDT    = poly2BDTC(st);
            final double dtc300   = poly1BDTC(fs, st, cs, 3 * p2BDT);
            final double dtc300dz = cs * p2BDT;
            final double cc       = 3.0 * dtc300 - dtc300dz - 3.0 * aa - 2.0 * bb;
            final double dd       = dtc300 - aa - bb - cc;
            final double zp       = (satAlt - 240.0) / 60.0;
            return aa + zp * (bb + zp * (cc + zp * dd));

        } else if (satAlt > 300.0 && satAlt <= 600.0) {
            final double h = satAlt / 100.0;
            return poly1BDTC(fs, st, cs, h * poly2BDTC(st));

        } else if (satAlt > 600.0 && satAlt <= 800.0) {
            final double poly2 = poly2BDTC(st);
            final double aa    = poly1BDTC(fs, st, cs, 6 * poly2);
            final double bb    = cs * poly2;
            final double cc    = -(3.0 * aa + 4.0 * bb) / 4.0;
            final double dd    = (aa + bb) / 4.0;
            final double zp    = (satAlt - 600.0) / 100.0;
            return aa + zp * (bb + zp * (cc + zp * dd));

        }
        return 0.;
    }

    /** Compute daily temperature correction for Jacchia-Bowman model.
     * @param f10 solar flux index
     * @param solarTime local solar time (hours in [0, 24[)
     * @param satLat sat lat (radians)
     * @param satAlt height (km)
     * @param <T> type of the filed elements
     * @return dTc correction
     */
    private static <T extends CalculusFieldElement<T>> T dTc(final double f10, final T solarTime,
                                                             final T satLat, final T satAlt) {
        final T      st = solarTime.divide(24.0);
        final T      cs = satLat.cos();
        final double fs = (f10 - 100.0) / 100.0;

        // Calculates dTc according to height
        if (satAlt.getReal() >= 120 && satAlt.getReal() <= 200) {
            final T dtc200   = poly2CDTC(fs, st, cs);
            final T dtc200dz = poly1CDTC(fs, st, cs);
            final T cc       = dtc200.multiply(3).subtract(dtc200dz);
            final T dd       = dtc200.subtract(cc);
            final T zp       = satAlt.subtract(120.0).divide(80.0);
            return zp.multiply(zp).multiply(cc.add(dd.multiply(zp)));

        } else if (satAlt.getReal() > 200.0 && satAlt.getReal() <= 240.0) {
            final T h = satAlt.subtract(200.0).divide(50.0);
            return poly1CDTC(fs, st, cs).multiply(h).add(poly2CDTC(fs, st, cs));

        } else if (satAlt.getReal() > 240.0 && satAlt.getReal() <= 300.0) {
            final T h        = solarTime.getField().getZero().newInstance(0.8);
            final T bb       = poly1CDTC(fs, st, cs);
            final T aa       = bb.multiply(h).add(poly2CDTC(fs, st, cs));
            final T p2BDT    = poly2BDTC(st);
            final T dtc300   = poly1BDTC(fs, st, cs, p2BDT.multiply(3));
            final T dtc300dz = cs.multiply(p2BDT);
            final T cc       = dtc300.multiply(3).subtract(dtc300dz).subtract(aa.multiply(3)).subtract(bb.multiply(2));
            final T dd       = dtc300.subtract(aa).subtract(bb).subtract(cc);
            final T zp       = satAlt.subtract(240.0).divide(60.0);
            return aa.add(zp.multiply(bb.add(zp.multiply(cc.add(zp.multiply(dd))))));

        } else if (satAlt.getReal() > 300.0 && satAlt.getReal() <= 600.0) {
            final T h = satAlt.divide(100.0);
            return poly1BDTC(fs, st, cs, h.multiply(poly2BDTC(st)));

        } else if (satAlt.getReal() > 600.0 && satAlt.getReal() <= 800.0) {
            final T poly2 = poly2BDTC(st);
            final T aa    = poly1BDTC(fs, st, cs, poly2.multiply(6));
            final T bb    = cs.multiply(poly2);
            final T cc    = aa.multiply(3).add(bb.multiply(4)).divide(-4.0);
            final T dd    = aa.add(bb).divide(4.0);
            final T zp    = satAlt.subtract(600.0).divide(100.0);
            return aa.add(zp.multiply(bb.add(zp.multiply(cc.add(zp.multiply(dd))))));

        }
        return satLat.getField().getZero();
    }

    /** Calculates first polynomial with CDTC array.
     * @param fs scaled flux f10
     * @param st local solar time in [0, 1[
     * @param cs cosine of satLat
     * @return the value of the polynomial
     */
    private static double poly1CDTC(final double fs, final double st, final double cs) {
        return CDT_SUB[0] +
               fs * (CDT_SUB[1] + st * (CDT_SUB[2] + st * (CDT_SUB[3] + st * (CDT_SUB[4] + st * (CDT_SUB[5] + st * CDT_SUB[6]))))) +
               cs * st * (CDT_SUB[7] + st * (CDT_SUB[8] + st * (CDT_SUB[9] + st * (CDT_SUB[10] + st * CDT_SUB[11])))) +
               cs * (CDT_SUB[12] + fs * (CDT_SUB[13] + st * (CDT_SUB[14] + st * CDT_SUB[15])));
    }

    /** Calculates first polynomial with CDTC array.
     * @param fs scaled flux f10
     * @param st local solar time in [0, 1[
     * @param cs cosine of satLat
     * @param <T> type of the field elements
     * @return the value of the polynomial
     */
    private static <T extends CalculusFieldElement<T>>  T poly1CDTC(final double fs, final T st, final T cs) {
        return st.multiply(CDT_SUB[6]).
               add(CDT_SUB[5]).multiply(st).
               add(CDT_SUB[4]).multiply(st).
               add(CDT_SUB[3]).multiply(st).
               add(CDT_SUB[2]).multiply(st).
               add(CDT_SUB[1]).multiply(fs).
               add(st.multiply(CDT_SUB[11]).
               add(CDT_SUB[10]).multiply(st).
               add(CDT_SUB[ 9]).multiply(st).
               add(CDT_SUB[ 8]).multiply(st).
               add(CDT_SUB[7]).multiply(st).multiply(cs)).
               add(st.multiply(CDT_SUB[15]).
               add(CDT_SUB[14]).multiply(st).
               add(CDT_SUB[13]).multiply(fs).
               add(CDT_SUB[12]).multiply(cs)).
               add(CDT_SUB[0]);
    }

    /** Calculates second polynomial with CDTC array.
     * @param fs scaled flux f10
     * @param st local solar time in [0, 1[
     * @param cs cosine of satLat
     * @return the value of the polynomial
     */
    private static double poly2CDTC(final double fs, final double st, final double cs) {
        return CDT_SUB[16] + st * cs * (CDT_SUB[17] + st * (CDT_SUB[18] + st * CDT_SUB[19])) +
               fs * cs * (CDT_SUB[20] + st * (CDT_SUB[21] + st * CDT_SUB[22]));
    }

    /** Calculates second polynomial with CDTC array.
     * @param fs scaled flux f10
     * @param st local solar time in [0, 1[
     * @param cs cosine of satLat
     * @param <T> type of the field elements
     * @return the value of the polynomial
     */
    private static <T extends CalculusFieldElement<T>>  T poly2CDTC(final double fs, final T st, final T cs) {
        return st.multiply(CDT_SUB[19]).
               add(CDT_SUB[18]).multiply(st).
               add(CDT_SUB[17]).multiply(cs).multiply(st).
               add(st.multiply(CDT_SUB[22]).
               add(CDT_SUB[21]).multiply(st).
               add(CDT_SUB[20]).multiply(cs).multiply(fs)).
               add(CDT_SUB[16]);
    }

    /** Calculates first polynomial with BDTC array.
     * @param fs scaled flux f10
     * @param st local solar time in [0, 1[
     * @param cs cosine of satLat
     * @param hp scaled height * poly2BDTC
     * @return the value of the polynomial
     */
    private static double poly1BDTC(final double fs, final double st, final double cs, final double hp) {
        return BDT_SUB[0] +
               fs * (BDT_SUB[1] + st * (BDT_SUB[2] + st * (BDT_SUB[3] + st * (BDT_SUB[4] + st * (BDT_SUB[5] + st * BDT_SUB[6]))))) +
               cs * (st * (BDT_SUB[7] + st * (BDT_SUB[8] + st * (BDT_SUB[9] + st * (BDT_SUB[10] + st * BDT_SUB[11])))) + hp + BDT_SUB[18]);
    }

    /** Calculates first polynomial with BDTC array.
     * @param fs scaled flux f10
     * @param st local solar time in [0, 1[
     * @param cs cosine of satLat
     * @param hp scaled height * poly2BDTC
     * @param <T> type of the field elements
     * @return the value of the polynomial
     */
    private static <T extends CalculusFieldElement<T>>  T poly1BDTC(final double fs, final T st, final T cs, final T hp) {
        return st.multiply(BDT_SUB[6]).
               add(BDT_SUB[5]).multiply(st).
               add(BDT_SUB[4]).multiply(st).
               add(BDT_SUB[3]).multiply(st).
               add(BDT_SUB[2]).multiply(st).
               add(BDT_SUB[1]).multiply(fs).
               add(st.multiply(BDT_SUB[11]).
               add(BDT_SUB[10]).multiply(st).
               add(BDT_SUB[ 9]).multiply(st).
               add(BDT_SUB[ 8]).multiply(st).
               add(BDT_SUB[ 7]).multiply(st).
               add(hp).add(BDT_SUB[18]).multiply(cs)).
               add(BDT_SUB[0]);
    }

    /** Calculates second polynomial with BDTC array.
     * @param st local solar time in [0, 1[
     * @return the value of the polynomial
     */
    private static double poly2BDTC(final double st) {
        return BDT_SUB[12] + st * (BDT_SUB[13] + st * (BDT_SUB[14] + st * (BDT_SUB[15] + st * (BDT_SUB[16] + st * BDT_SUB[17]))));
    }

        /** Calculates second polynomial with BDTC array.
     * @param st local solar time in [0, 1[
     * @param <T> type of the field elements
     * @return the value of the polynomial
     */
    private static <T extends CalculusFieldElement<T>>  T poly2BDTC(final T st) {
        return st.multiply(BDT_SUB[17]).
               add(BDT_SUB[16]).multiply(st).
               add(BDT_SUB[15]).multiply(st).
               add(BDT_SUB[14]).multiply(st).
               add(BDT_SUB[13]).multiply(st).
               add(BDT_SUB[12]);
    }

}
