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
    { "underground trajectory (r = {0})",
      "trajectoire souterraine (r = {0})" },
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
    { "unexpected element \"{0}\" in UTC/TAI time steps file",
      "\u00e9l\u00e9 element \u00ab {0} \u00bb inattendu"
    + " dans le fichier des sauts du TUC" },
    { "missing attribute \"{0}\" in UTC/TAI time steps file",
      "attribut \"{0}\" manquant dans le fichier des sauts du TUC" },
    { "unparsable date in UTC/TAI time steps file ({0})",
      "date de saut illisible dans le fichier des sauts du TUC ({0})" },
    { "unparsable step value in UTC/TAI time steps file ({0})",
      "valeur de saut illisible dans le fichier des sauts du TUC ({0})" },
    { "non-increasing dates in UTC/TAI time steps file ({0})",
      "dates non croissantes dans le fichier des sauts du TUC ({0})" },
    { "null parent frame",
      "rep\u00e8re parent nul" },
    { "IERS local cache top directory not defined",
      "r\u00e9pertoire racine de cache local IERS non d\u00e9fini" },
    { "IERS local cache top directory {0}",
      "r\u00e9pertoire racine de cache local IERS {0} inexistant" },
    { "{0} is not a directory",
      "{0} n''est pas un r\u00e9pertoire" },
    { "no IERS data file found in local cache {0}",
      "aucun fichier de donn\u00e9es IERS trouv\u00e9 dans le cache local {0}" },
    { "missing IERS data between {0} and {1}",
      "donn\u00e9es IERS manquantes entre {0} et {1}" }
  };

}
