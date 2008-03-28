package fr.cs.orekit.frames.series;

/** Class for terms that do not depend on far planets and some other elements.
 * @author Luc Maisonobe
 */
class NoFarPlanetsTerm extends SeriesTerm {

    /** Serializable UID. */
    private static final long serialVersionUID = -6466886528892169861L;

    /** Coefficient for mean anomaly of the Moon. */
    private final int cL;

    /** Coefficient for L - &Omega; where L is the mean longitude of the Moon. */
    private final int cF;

    /** Coefficient for mean elongation of the Moon from the Sun. */
    private final int cD;

    /** Coefficient for mean longitude of the ascending node of the Moon. */
    private final int cOmega;

    /** Coefficient for mean Mercury longitude. */
    private final int cMe;

    /** Coefficient for mean Venus longitude. */
    private final int cVe;

    /** Coefficient for mean Earth longitude. */
    private final int cE;

    /** Coefficient for mean Mars longitude. */
    private final int cMa;

    /** Coefficient for mean Jupiter longitude. */
    private final int cJu;

    /** Coefficient for mean Saturn longitude. */
    private final int cSa;

    /** Build a planetary term for nutation series.
     * @param cMe coefficient for mean Mercury longitude
     * @param cVe coefficient for mean Venus longitude
     * @param cE coefficient for mean Earth longitude
     * @param cMa coefficient for mean Mars longitude
     * @param cJu coefficient for mean Jupiter longitude
     * @param cSa coefficient for mean Saturn longitude
     * @param cUr coefficient for mean Uranus longitude
     * @param cNe coefficient for mean Neptune longitude
     * @param cPa coefficient for general accumulated precession in longitude
      */
    public NoFarPlanetsTerm(double sinCoeff, double cosCoeff,
                            int cL, int cF, int cD, int cOmega,
                            int cMe, int cVe, int cE, int cMa, int cJu, int cSa) {
        super(sinCoeff, cosCoeff);
        this.cL     = cL;
        this.cF     = cF;
        this.cD     = cD;
        this.cOmega = cOmega;
        this.cMe    = cMe;
        this.cVe    = cVe;
        this.cE     = cE;
        this.cMa    = cMa;
        this.cJu    = cJu;
        this.cSa    = cSa;
    }

    /** {@inheritDoc} */
    protected double argument(BodiesElements elements) {
        return cL * elements.l + cF * elements.f +
               cD * elements.d + cOmega * elements.omega +
               cMe * elements.lMe + cVe * elements.lVe + cE  * elements.lE +
               cMa * elements.lMa + cJu * elements.lJu + cSa * elements.lSa;

    }

}
