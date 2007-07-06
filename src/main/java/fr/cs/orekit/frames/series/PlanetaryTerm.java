package fr.cs.orekit.frames.series;

/** Class for planetary only terms.
 * @author Luc Maisonobe
 */
class PlanetaryTerm extends SeriesTerm {

  public PlanetaryTerm(double sinCoeff, double cosCoeff,
                       int cMe, int cVe, int cE, int cMa, int cJu,
                       int cSa, int cUr, int cNe, int cPa) {
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

  protected double argument(BodiesElements elements) {
    return cMe * elements.lMe + cVe * elements.lVe + cE  * elements.lE
         + cMa * elements.lMa + cJu * elements.lJu
         + cSa * elements.lSa + cUr * elements.lUr
         + cNe * elements.lNe + cPa * elements.pa;
  }

  private final int cMe; 
  private final int cVe; 
  private final int cE; 
  private final int cMa; 
  private final int cJu; 
  private final int cSa; 
  private final int cUr; 
  private final int cNe; 
  private final int cPa; 

  private static final long serialVersionUID = -4604270587132683569L;

}
