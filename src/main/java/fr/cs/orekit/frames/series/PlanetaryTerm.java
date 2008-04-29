package fr.cs.orekit.frames.series;

/** Class for planetary only terms.
 * @author Luc Maisonobe
 */
class PlanetaryTerm extends SeriesTerm {

    /** Serializable UID. */
    private static final long serialVersionUID = -666953066880447449L;

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

    /** Coefficient for mean Uranus longitude. */
    private final int cUr;

    /** Coefficient for mean Neptune longitude. */
    private final int cNe;

    /** Coefficient for general accumulated precession in longitude. */
    private final int cPa;

    /** Build a planetary term for nutation series.
     * @param sinCoeff coefficient for the sine of the argument
     * @param cosCoeff coefficient for the cosine of the argument
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
    public PlanetaryTerm(final double sinCoeff, final double cosCoeff,
                         final int cMe, final int cVe, final int cE, final int cMa, final int cJu,
                         final int cSa, final int cUr, final int cNe, final int cPa) {
        super(sinCoeff, cosCoeff);
        this.cMe = cMe;
        this.cVe = cVe;
        this.cE  = cE;
        this.cMa = cMa;
        this.cJu = cJu;
        this.cSa = cSa;
        this.cUr = cUr;
        this.cNe = cNe;
        this.cPa = cPa;
    }

    protected double argument(final BodiesElements elements) {
        return cMe * elements.lMe + cVe * elements.lVe + cE  * elements.lE +
               cMa * elements.lMa + cJu * elements.lJu +
               cSa * elements.lSa + cUr * elements.lUr +
               cNe * elements.lNe + cPa * elements.pa;
    }

}
