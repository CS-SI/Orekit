package fr.cs.orekit.frames;

import org.apache.commons.math.geometry.Rotation;
import org.apache.commons.math.geometry.Vector3D;
import fr.cs.orekit.errors.OrekitException;
import fr.cs.orekit.frames.series.BodiesElements;
import fr.cs.orekit.frames.series.Development;
import fr.cs.orekit.time.AbsoluteDate;

/** Intermediate Reference Frame 2000 : true equinox and equator of date.
 * <p> It considers precession and nutation effects and not the earth rotation. Its parent
 * frame is the J2000 frame. <p>
 */
class IRF2000Frame extends Frame {

    /** Serializable UID. */
    private static final long serialVersionUID = 2781008917378714616L;

    /** 2&pi;. */
    private static final double twoPi = 2.0 * Math.PI;

    /** Radians per arcsecond. */
    private static final double radiansPerArcsecond = twoPi / 1296000;

    /** Julian century per second. */
    private static final double julianCenturyPerSecond = 1.0 / (36525.0 * 86400.0);

    // lunisolar nutation elements
    private static final double f10 = Math.toRadians(134.96340251);
    private static final double f11 = 1717915923.217800  * radiansPerArcsecond;
    private static final double f12 =         31.879200  * radiansPerArcsecond;
    private static final double f13 =          0.051635  * radiansPerArcsecond;
    private static final double f14 =         -0.0002447 * radiansPerArcsecond;

    private static final double f20 = Math.toRadians(357.52910918);
    private static final double f21 = 129596581.048100   * radiansPerArcsecond;
    private static final double f22 =        -0.553200   * radiansPerArcsecond;
    private static final double f23 =         0.000136   * radiansPerArcsecond;
    private static final double f24 =        -0.00001149 * radiansPerArcsecond;

    private static final double f30 = Math.toRadians(93.27209062);
    private static final double f31 = 1739527262.847800   * radiansPerArcsecond;
    private static final double f32 =        -12.751200   * radiansPerArcsecond;
    private static final double f33 =         -0.001037   * radiansPerArcsecond;
    private static final double f34 =          0.00000417 * radiansPerArcsecond;

    private static final double f40 = Math.toRadians(297.85019547);
    private static final double f41 = 1602961601.209000   * radiansPerArcsecond;
    private static final double f42 =         -6.370600   * radiansPerArcsecond;
    private static final double f43 =          0.006593   * radiansPerArcsecond;
    private static final double f44 =         -0.00003169 * radiansPerArcsecond;

    private static final double f50 = Math.toRadians(125.04455501);
    private static final double f51 = -6962890.543100   * radiansPerArcsecond;
    private static final double f52 =        7.472200   * radiansPerArcsecond;
    private static final double f53 =        0.007702   * radiansPerArcsecond;
    private static final double f54 =       -0.00005939 * radiansPerArcsecond;

    // planetary nutation elements
    private static final double f60 = 4.402608842;
    private static final double f61 = 2608.7903141574;

    private static final double f70 = 3.176146697;
    private static final double f71 = 1021.3285546211;

    private static final double f80 = 1.753470314;
    private static final double f81 = 628.3075849991;

    private static final double f90 = 6.203480913;
    private static final double f91 = 334.0612426700;

    private static final double f100 = 0.599546497;
    private static final double f101 = 52.9690962641;

    private static final double f110 = 0.874016757;
    private static final double f111 = 21.3299104960;

    private static final double f120 = 5.481293872;
    private static final double f121 = 7.4781598567;

    private static final double f130 = 5.311886287;
    private static final double f131 = 3.8133035638;

    private static final double f141 = 0.024381750;
    private static final double f142 = 0.00000538691;

    /** IERS conventions (2003) resources base directory. */
    private static final String iers2003Base = "/META-INF/IERS-conventions-2003/";

    /** Resources for IERS table 5.2a from IERS conventions (2003). */
    private static final String xModel2000A    = iers2003Base + "tab5.2a.txt";
    private static final String xModel2000B    = iers2003Base + "tab5.2a.reduced.txt";

    /** Resources for IERS table 5.2b from IERS conventions (2003). */
    private static final String yModel2000A    = iers2003Base + "tab5.2b.txt";
    private static final String yModel2000B    = iers2003Base + "tab5.2b.reduced.txt";

    /** Resources for IERS table 5.2c from IERS conventions (2003). */
    private static final String sxy2Model2000A = iers2003Base + "tab5.2c.txt";
    private static final String sxy2Model2000B = iers2003Base + "tab5.2c.reduced.txt";

    /** Indicator for complete or reduced precession-nutation model. */
    private boolean useIAU2000B;

    /** Pole position (X). */
    private Development xDevelopment = null;

    /** Pole position (Y). */
    private Development yDevelopment = null;

    /** Pole position (S + XY/2). */
    private Development sxy2Development = null;

    /** Cached date to avoid useless calculus */
    private AbsoluteDate cachedDate;

    /** Build the IRF2000 frame singleton.
     * <p>If the <code>useIAU2000B</code> boolean parameter is true (which is the
     * recommended value) the reduced IAU2000B precession-nutation model will be
     * used, otherwise the complete IAU2000A precession-nutation model will be used.
     * The IAU2000B is recommended for most applications since it is <strong>far
     * less</strong> computation intensive than the IAU2000A model and its accuracy
     * is only slightly degraded (1 milliarcsecond instead of 0.2 milliarcsecond).</p>
     * @param date the date.
     * @param useIAU2000B if true (recommended value), the IAU2000B model will be used
     * @exception OrekitException if the nutation model data embedded in the
     * library cannot be read.
     * @see Frame
     */
    protected IRF2000Frame(AbsoluteDate date, boolean useIAU2000B, String name)
        throws OrekitException {

        super(getJ2000(), null , name);

        this.useIAU2000B = useIAU2000B;

        // nutation models are in micro arcseconds
        final Class c = getClass();
        final String xModel = useIAU2000B ? xModel2000B : xModel2000A;
        xDevelopment =
            new Development(c.getResourceAsStream(xModel), radiansPerArcsecond * 1.0e-6, xModel);
        final String yModel = useIAU2000B ? yModel2000B : yModel2000A;
        yDevelopment =
            new Development(c.getResourceAsStream(yModel), radiansPerArcsecond * 1.0e-6, yModel);
        final String sxy2Model = useIAU2000B ? sxy2Model2000B : sxy2Model2000A;
        sxy2Development =
            new Development(c.getResourceAsStream(sxy2Model), radiansPerArcsecond * 1.0e-6, sxy2Model);

        // everything is in place, we can now synchronize the frame
        updateFrame(date);
    }

    /** Update the frame to the given date.
     * <p>The update considers the nutation and precession effects from IERS data.</p>
     * @param date new value of the date
     * @exception OrekitException if the nutation model data embedded in the
     * library cannot be read
     */
    protected void updateFrame(AbsoluteDate date) throws OrekitException {

        if (cachedDate == null || cachedDate != date) {
            //    offset from J2000 epoch in julian centuries
            final double tts = date.minus(AbsoluteDate.J2000Epoch);
            final double ttc =  tts * julianCenturyPerSecond;

            // luni-solar and planetary elements
            final BodiesElements elements = computeBodiesElements(ttc);


            // precession and nutation effect (pole motion in celestial frame)
            final Rotation qRot = precessionNutationEffect(ttc, elements);

            // combined effects
            final Rotation combined = qRot.revert();

            // set up the transform from parent GCRS (J2000) to ITRF
            updateTransform(new Transform(combined , Vector3D.zero));
            cachedDate = date;
        }
    }

    /** Compute the nutation elements.
     * @param tt offset from J2000.0 epoch in julian centuries
     * @return luni-solar and planetary elements
     */
    private BodiesElements computeBodiesElements(double tt) {
        if (useIAU2000B) {
            return new BodiesElements(f11 * tt + f10, // mean anomaly of the Moon
                                      f21 * tt + f20, // mean anomaly of the Sun
                                      f31 * tt + f30, // L - &Omega; where L is the mean longitude of the Moon
                                      f41 * tt + f40, // mean elongation of the Moon from the Sun
                                      f51 * tt + f50, // mean longitude of the ascending node of the Moon
                                      Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN,
                                      Double.NaN, Double.NaN, Double.NaN, Double.NaN);
        }
        return new BodiesElements((((f14 * tt + f13) * tt + f12) * tt + f11) * tt + f10, // mean anomaly of the Moon
                                  (((f24 * tt + f23) * tt + f22) * tt + f21) * tt + f20, // mean anomaly of the Sun
                                  (((f34 * tt + f33) * tt + f32) * tt + f31) * tt + f30, // L - &Omega; where L is the mean longitude of the Moon
                                  (((f44 * tt + f43) * tt + f42) * tt + f41) * tt + f40, // mean elongation of the Moon from the Sun
                                  (((f54 * tt + f53) * tt + f52) * tt + f51) * tt + f50, // mean longitude of the ascending node of the Moon
                                  f61  * tt +  f60, // mean Mercury longitude
                                  f71  * tt +  f70, // mean Venus longitude
                                  f81  * tt +  f80, // mean Earth longitude
                                  f91  * tt +  f90, // mean Mars longitude
                                  f101 * tt + f100, // mean Jupiter longitude
                                  f111 * tt + f110, // mean Saturn longitude
                                  f121 * tt + f120, // mean Uranus longitude
                                  f131 * tt + f130, // mean Neptune longitude
                                  (f142 * tt + f141) * tt); // general accumulated precession in longitude
    }

    /** Compute precession and nutation effects.
     * @param t offset from J2000.0 epoch in julian centuries
     * @param elements luni-solar and planetary elements for the current date
     * @return precession and nutation rotation
     */
    public Rotation precessionNutationEffect(double t, BodiesElements elements) {

        // pole position
        final double x =    xDevelopment.value(t, elements);
        final double y =    yDevelopment.value(t, elements);
        final double s = sxy2Development.value(t, elements) - x * y / 2;

        final double x2 = x * x;
        final double y2 = y * y;
        final double r2 = x2 + y2;
        final double e = Math.atan2(y, x);
        final double d = Math.acos(Math.sqrt(1 - r2));
        final Rotation rpS = new Rotation(Vector3D.plusK, -s);
        final Rotation rpE = new Rotation(Vector3D.plusK, -e);
        final Rotation rmD = new Rotation(Vector3D.plusJ, +d);

        // combine the 4 rotations (rpE is used twice)
        // IERS conventions (2003), section 5.3, equation 6
        return rpE.applyInverseTo(rmD.applyTo(rpE.applyTo(rpS)));

    }

}
