package fr.cs.orekit.frames.series;

/** Class for luni-solar only terms.
 * @author Luc Maisonobe
 */
class LuniSolarTerm extends SeriesTerm {

    /** Serializable UID. */
    private static final long serialVersionUID = -461066685792014379L;

    /** Coefficient for mean anomaly of the Moon. */
    private final int cL;

    /** Coefficient for mean anomaly of the Sun. */
    private final int cLPrime;

    /** Coefficient for L - &Omega; where L is the mean longitude of the Moon. */
    private final int cF;

    /** Coefficient for mean elongation of the Moon from the Sun. */
    private final int cD;

    /** Coefficient for mean longitude of the ascending node of the Moon. */
    private final int cOmega;

    /** Build a luni-solar term for nutation series.
     * @param sinCoeff coefficient for the sine of the argument
     * @param cosCoeff coefficient for the cosine of the argument
     * @param cL coefficient for mean anomaly of the Moon
     * @param cLPrime coefficient for mean anomaly of the Sun
     * @param cF coefficient for L - &Omega; where L is the mean longitude of the Moon
     * @param cD coefficient for mean elongation of the Moon from the Sun
     * @param cOmega coefficient for mean longitude of the ascending node of the Moon
     */
    public LuniSolarTerm(final double sinCoeff, final double cosCoeff,
                         final int cL, final int cLPrime, final int cF, final int cD, final int cOmega) {
        super(sinCoeff, cosCoeff);
        this.cL      = cL;
        this.cLPrime = cLPrime;
        this.cF      = cF;
        this.cD      = cD;
        this.cOmega  = cOmega;
    }

    /** {@inheritDoc} */
    protected double argument(final BodiesElements elements) {
        return cL * elements.getL() + cLPrime * elements.getLPrime() + cF * elements.getF() +
               cD * elements.getD() + cOmega * elements.getOmega();
    }

}
