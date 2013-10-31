package org.orekit.forces.gravity.potential;

import org.junit.Assert;
import org.apache.commons.math3.exception.util.LocalizedFormats;
import org.apache.commons.math3.util.Precision;
import org.junit.Before;
import org.junit.Test;
import org.orekit.errors.OrekitException;
import org.orekit.forces.gravity.potential.NormalizedSphericalHarmonicsProvider.NormalizedSphericalHarmonics;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;

/** @author Evan Ward */
public class CachedNormalizedSphericalHarmonicsProviderTest {


    private static final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
    private NormalizedSphericalHarmonicsProvider raw;
    private static final double step = 60 * 60, slotSpan = Constants.JULIAN_DAY, newSlotInterval = Constants.JULIAN_DAY;
    private static final int interpolationPoints = 3, maxSlots = 100;
    private CachedNormalizedSphericalHarmonicsProvider cache;

    @Before
    public void setUp() {
        raw = new QuadraticProvider(date);
        cache = new CachedNormalizedSphericalHarmonicsProvider(raw, step, interpolationPoints, maxSlots, slotSpan, newSlotInterval);

    }

    @Test
    public void testInterpolation() throws OrekitException {
        //setup
        //generate points on grid with date as the origin
        cache.onDate(date);
        //sample at step/2
        AbsoluteDate sampleDate = date.shiftedBy(step / 2.0);
        //expected values
        NormalizedSphericalHarmonics expected = raw.onDate(sampleDate);

        //action
        NormalizedSphericalHarmonics actual = cache.onDate(sampleDate);

        //verify
        double tol = Precision.EPSILON;
        for (int n = 0; n < raw.getMaxDegree(); n++) {
            for (int m = 0; m < n; m++) {
                Assert.assertEquals(expected.getNormalizedCnm(n,m), actual.getNormalizedCnm(n,m), tol);
                Assert.assertEquals(expected.getNormalizedSnm(n,m), actual.getNormalizedSnm(n,m), tol);
            }
        }
    }

    private static class QuadraticProvider implements NormalizedSphericalHarmonicsProvider {

        private final AbsoluteDate date;

        private QuadraticProvider(AbsoluteDate date) {
            this.date = date;
        }

        @Override
        public NormalizedSphericalHarmonics onDate(final AbsoluteDate date) throws OrekitException {
            final double t = date.durationFrom(this.date);
            return new NormalizedSphericalHarmonics() {
                @Override
                public double getNormalizedCnm(int n, int m) throws OrekitException {
                    return n + m + t * t;
                }

                @Override
                public double getNormalizedSnm(int n, int m) throws OrekitException {
                    return n + m + t * t + 1;
                }

                @Override
                public AbsoluteDate getDate() {
                    return date;
                }
            };
        }

        @Override
        public int getMaxDegree() {
            return 2;
        }

        @Override
        public int getMaxOrder() {
            return 2;
        }

        @Override
        public double getMu() {
            return 1;
        }

        @Override
        public double getAe() {
            return 1;
        }

        @Override
        public AbsoluteDate getReferenceDate() {
            return date;
        }

        @Override
        public double getOffset(AbsoluteDate date) {
            return date.durationFrom(this.date);
        }

        @Override
        public TideSystem getTideSystem() {
            return TideSystem.UNKNOWN;
        }

        @Override
        public double getNormalizedCnm(final double dateOffset, final int n, final int m) {
            throw OrekitException.createIllegalStateException(LocalizedFormats.SIMPLE_MESSAGE,
                                                              "this deprecated metho should never be called");
        }

        @Override
        public double getNormalizedSnm(final double dateOffset, final int n, final int m) {
            throw OrekitException.createIllegalStateException(LocalizedFormats.SIMPLE_MESSAGE,
                                                              "this deprecated metho should never be called");
        }

    }
}
