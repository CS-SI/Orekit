package org.orekit.time;

import org.hipparchus.util.MathArrays;
import org.orekit.frames.EOPHistory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;

/**
 * Abstract base class for {@link TimeScales} that implements some common functionality.
 *
 * @author Evan Ward
 * @author Luc Maisonobe
 * @since 10.1
 */
public abstract class AbstractTimeScales implements TimeScales {

    /**
     * Get the Universal Time 1 scale.
     * <p>
     * As this method allow associating any history with the time scale, it may involve
     * large data sets. So this method does <em>not</em> cache the resulting {@link
     * UT1Scale UT1Scale} instance, a new instance will be returned each time. In order to
     * avoid wasting memory, calling {@link #getUT1(IERSConventions, boolean)} with the
     * single enumerate corresponding to the conventions may be a better solution. This
     * method is made available only for expert use.
     * </p>
     *
     * @param history EOP parameters providing dUT1 (may be null if no correction is
     *                desired)
     * @return Universal Time 1 scale
     * @see #getUT1(IERSConventions, boolean)
     */
    protected UT1Scale getUT1(final EOPHistory history) {
        return new UT1Scale(history, getUTC());
    }

    @Override
    public AbsoluteDate getJulianEpoch() {
        return new AbsoluteDate(DateComponents.JULIAN_EPOCH, TimeComponents.H12, this.getTT());
    }

    @Override
    public AbsoluteDate getModifiedJulianEpoch() {
        return new AbsoluteDate(DateComponents.MODIFIED_JULIAN_EPOCH, TimeComponents.H00, this.getTT());
    }

    @Override
    public AbsoluteDate getFiftiesEpoch() {
        return new AbsoluteDate(DateComponents.FIFTIES_EPOCH, TimeComponents.H00, this.getTT());
    }

    @Override
    public AbsoluteDate getCcsdsEpoch() {
        return new AbsoluteDate(DateComponents.CCSDS_EPOCH, TimeComponents.H00, this.getTAI());
    }

    @Override
    public AbsoluteDate getGalileoEpoch() {
        return new AbsoluteDate(DateComponents.GALILEO_EPOCH, TimeComponents.H00, this.getGST());
    }

    @Override
    public AbsoluteDate getGpsEpoch() {
        return new AbsoluteDate(DateComponents.GPS_EPOCH, TimeComponents.H00, this.getGPS());
    }

    @Override
    public AbsoluteDate getQzssEpoch() {
        return new AbsoluteDate(DateComponents.QZSS_EPOCH, TimeComponents.H00, this.getQZSS());
    }

    @Override
    public AbsoluteDate getIrnssEpoch() {
        return new AbsoluteDate(DateComponents.IRNSS_EPOCH, TimeComponents.H00, this.getIRNSS());
    }

    @Override
    public AbsoluteDate getBeidouEpoch() {
        return new AbsoluteDate(DateComponents.BEIDOU_EPOCH, TimeComponents.H00, this.getBDT());
    }

    @Override
    public AbsoluteDate getGlonassEpoch() {
        return new AbsoluteDate(DateComponents.GLONASS_EPOCH,
                new TimeComponents(29.0), this.getTAI()).shiftedBy(-10800.0);
    }

    @Override
    public AbsoluteDate getJ2000Epoch() {
        return new AbsoluteDate(DateComponents.J2000_EPOCH, TimeComponents.H12, this.getTT());
    }

    @Override
    public AbsoluteDate getJavaEpoch() {
        return new AbsoluteDate(DateComponents.JAVA_EPOCH, this.getTAI()).shiftedBy(8.000082);
    }

    @Override
    public AbsoluteDate getPastInfinity() {
        return getJavaEpoch().shiftedBy(Double.NEGATIVE_INFINITY);
    }

    @Override
    public AbsoluteDate getFutureInfinity() {
        return getJavaEpoch().shiftedBy(Double.POSITIVE_INFINITY);
    }

    @Override
    public AbsoluteDate createJulianEpoch(final double julianEpoch) {
        return new AbsoluteDate(getJ2000Epoch(),
                Constants.JULIAN_YEAR * (julianEpoch - 2000.0));
    }

    @Override
    public AbsoluteDate createBesselianEpoch(final double besselianEpoch) {
        return new AbsoluteDate(getJ2000Epoch(),
                MathArrays.linearCombination(
                        Constants.BESSELIAN_YEAR, besselianEpoch - 1900,
                        Constants.JULIAN_DAY, -36525,
                        Constants.JULIAN_DAY, 0.31352));
    }

}
