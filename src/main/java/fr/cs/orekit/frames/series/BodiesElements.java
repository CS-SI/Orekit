package fr.cs.orekit.frames.series;

/** Elements of the bodies having an effect on nutation.
 * <p>This class is a simple placeholder,
 * it does not provide any processing method.</p>
 * @author Luc Maisonobe
 */
public final class BodiesElements {

    /** Simple constructor.
     * @param l mean anomaly of the Moon
     * @param lPrime mean anomaly of the Sun
     * @param f L - &Omega; where L is the mean longitude of the Moon
     * @param d mean elongation of the Moon from the Sun
     * @param omega mean longitude of the ascending node of the Moon
     * @param lMe mean Mercury longitude
     * @param lVe mean Venus longitude
     * @param lE mean Earth longitude
     * @param lMa mean Mars longitude
     * @param lJu mean Jupiter longitude
     * @param lSa mean Saturn longitude
     * @param lUr mean Uranus longitude
     * @param lNe mean Neptune longitude
     * @param pa general accumulated precession in longitude
     */
    public BodiesElements(double l, double lPrime, double f, double d, double omega,
                          double lMe, double lVe, double lE, double lMa, double lJu,
                          double lSa, double lUr, double lNe, double pa) {
        this.l      = l;
        this.lPrime = lPrime;
        this.f      = f;
        this.d      = d;
        this.omega  = omega;
        this.lMe    = lMe;
        this.lVe    = lVe;
        this.lE     = lE;
        this.lMa    = lMa;
        this.lJu    = lJu;
        this.lSa    = lSa;
        this.lUr    = lUr;
        this.lNe    = lNe;
        this.pa     = pa;
    }

    /** Mean anomaly of the Moon. */
    public final double l;

    /** Mean anomaly of the Sun. */
    public final double lPrime;

    /** L - &Omega; where L is the mean longitude of the Moon. */
    public final double f;

    /** Mean elongation of the Moon from the Sun. */
    public final double d;

    /** Mean longitude of the ascending node of the Moon. */
    public final double omega;

    /** Mean Mercury longitude. */
    public final double lMe;

    /** Mean Venus longitude. */
    public final double lVe;

    /** Mean Earth longitude. */
    public final double lE;

    /** Mean Mars longitude. */
    public final double lMa;

    /** Mean Jupiter longitude. */
    public final double lJu;

    /** Mean Saturn longitude. */
    public final double lSa;

    /** Mean Uranus longitude. */
    public final double lUr;

    /** Mean Neptune longitude. */
    public final double lNe;

    /** General accumulated precession in longitude. */
    public final double pa;

}
