package fr.cs.aerospace.orekit.errors;

import java.util.ListResourceBundle;

/** This class gather the french translations of the message resources
 * for the orekit library.
 * @author Luc Maisonobe
 * @version $Id$
 */
public class MessagesResources_fr
  extends ListResourceBundle {

  /** Simple constructor.
   */
  public MessagesResources_fr() {
  }

  public Object[][] getContents() {
    return contents;
  }

  static final Object[][] contents = {
    { "unable to converge after {0} iterations",
      "impossible de converger apr\u00e8s {0} it\u00e9rations" },
    { "C and S should have the same size :" +
        " (C = [{0}][{1}] ; S = [{2}][{3}])",
        "C et S doivent avoir la même taille :" +
        " (C = [{0}][{1}] ; S = [{2}][{3}])" },
    { "trajectory inside the Brillouin sphere (r = {0})",
      "trajectoire int\u00e9rieure \u00e0 la sph\u00e8re de Brillouin (r = {0})" },
    { "polar trajectory (r = {0})",
      "trajectoire polaire (r = {0})" },
    { "too excentric orbit (e = {0})",
      "orbite trop excentrique (e = {0})" },
    { "almost equatorial orbit (i = {0} degrees)",
      "orbite quasiment \u00e9quatoriale (i = {0} degr\u00e9s)" },
    { "almost critically inclined orbit (i = {0} degrees)",
      "orbite quasiment \u00e0 inclinaison critique (i = {0} degr\u00e9s)" },
    { "unable to compute Eckstein-Hechler mean parameters"
    + " after {0} iterations",
      "impossible de calculer les param\u00e8tres moyens au sens de"
    + " Eckstein-Hechler apr\u00e8s {0} it\u00e9rations"},
    { "non-increasing dates in file {0}, line {1}",
      "dates non croissantes dans le fichier {0} \u00e0 la ligne {1}" },
    { "null parent frame",
      "rep\u00e8re parent nul" },
    { "IERS root directory {0} does not exist",
      "r\u00e9pertoire IERS racine {0} inexistant" },
    { "{0} is not a directory",
      "{0} n''est pas un r\u00e9pertoire" },
    { "duplicated Earth Orientation Parameters for month {0}-{1}",
      "Param\u00e8tres d''Orientation de la Terre dupliqu\u00e9s pour le mois {0}-{1}" },
    { "missing Earth Orientation Parameters between {0}-{1} and {2}-{3}",
      "Param\u00e8tres d''Orientation de la Terre manquants entre {0}-{1} et {2}-{3}" },
    { "unable to parse line {0} in IERS data file {1}",
      "impossible d''analyser la ligne {0} du fichier de donn\u00e9es IERS {1}" },
    { "file {0} is not an IERS data file",
      "le fichier {0} n''est pas un fichier de donn\u00e9es IERS" },
    { "several IERS UTC-TAI history files found: {0} and {1}",
      "plusieurs fichiers IERS d''historique UTC-TAI trouv\u00e9s : {0} et {1}" },
    { "unexpected data line {0} in file {1} (line {2} should not be followed by data)",
      "ligne de donn\u00e9es {0} inattendue dans le fichier {1} (la ligne {2} ne devrait pas \u00eatre suivie d''autres donn\u00e9es)" },
    { "file {0} is not an IERS UTC-TAI history file",
      "le fichier {0} n''est pas un fichier d''historique UTC-TAI de l''IERS" },
    { "unable to parse line {0} in IERS UTC-TAI history file {1}",
      "impossible d''analyser la ligne {0} du fichier d''historique UTC-TAI de l''IERS {1}" },
    { "missing serie j = {0} in nutation model file {1} (line {2})",
      "s\u00e9rie j = {0} manquante dans le fichier de mod\u00e8le de nutation {1} (ligne {2})" },
    { "unexpected end of nutation model file {0} (after line {1})",
      "fin inattendue du fichier de mod\u00e8le de nutation {0} (apr\u00e8s la ligne {1})" },
    { "unable to parse line {0} of nutation model file {1}:\n{2}",
      "impossible d''analyser la ligne {0} du fichier de mod\u00e8le de nutation {1} :\n{2}" },
    { "unable to find nutation model file {0}",
      "impossible de trouver le fichier de mod\u00e8le de nutation {0}" }
  };

}
