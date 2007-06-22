package fr.cs.aerospace.orekit.errors;

import java.util.ListResourceBundle;

/** This class gather the french translations of the message resources
 * for the orekit library.
 * @author Luc Maisonobe
 */
public class MessagesResources_fr
  extends ListResourceBundle {

  /** Simple constructor.
   */
  public MessagesResources_fr() {
  }

  public Object[][] getContents() {
    return (Object[][]) contents.clone();
  }

  static final Object[][] contents = {
    { "unable to converge after {0} iterations",
      "impossible de converger apr\u00e8s {0} it\u00e9rations" },
    { "C and S should have the same size :" +
        " (C = [{0}][{1}] ; S = [{2}][{3}])",
        "C et S doivent avoir la m\u00eame taille :" +
        " (C = [{0}][{1}] ; S = [{2}][{3}])" },
    { "trajectory inside the Brillouin sphere (r = {0})",
      "trajectoire int\u00e9rieure \u00e0 la sph\u00e8re de Brillouin (r = {0})" },
    { "polar trajectory (r = {0})",
      "trajectoire polaire (r = {0})" },
    { "too excentric orbit (e = {0})",
      "orbite trop excentrique (e = {0})" },
    { "Mass is becoming negative",
      "La masse devient n\u00e9gative" },
    { "Mass is null or negative",
      "La masse est nulle ou n\u00e9gative" },
    { "Unknown file format. ",
      "Format de fichier inconnu. " },
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
    { "missing Earth Orientation Parameters between {0} and {1}",
      "Param\u00e8tres d''Orientation de la Terre manquants entre {0} et {1}" },
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
      "impossible de trouver le fichier de mod\u00e8le de nutation {0}" },
    { "Eccentricity is becoming greater than 1. Unable to continue." , 
      "L''eccentricit\u00e9 devient plus grande que 1. Impossible de continuer" },
    { "Choosen frame type is not correct",
      "Le type de rep\u00e8re choisi n''est pas valide" },
    { "Choosen attitude type is not correct",
      "Le type d''attitude choisi n''est pas valide" },
    { "Mass is null or negative",
      "La masse est nulle ou n\u00e9gative" },
    { "Flow rate (dm/dt) is positive : ",
      "Le d\u00e9bit de masse (dm/dt) est positif : " },
    { "null norm",
      "La norme est nulle" },
    { "the reader has not been tested ",
      "le lecteur n''a pas \u00e9t\u00e9 test\u00e9 "},
    { "the reader is not adapted to the format ",
      "le lecteur n''est pas adapt\u00e9 au format "},
    { "too large degree (n = {0}), potential maximal degree is {1})",
      "degr\u00e9 trop important (n = {0}), le degr\u00e9 de potentiel maximal est {1})"},
    { "Unknown file format ",
      "Format de fichier inconnu "},
    { "Cheksum of line 1 is not correct. Should be: {0} but is: {1}",
      "Le cheksum de la ligne 1 n''est pas correct. il devrait valoir: {0} mais vaut: {1}"},
    { "Cheksum of line 2 is not correct. Should be: {0} but is: {1}",
      "Le cheksum de la ligne 2 n''est pas correct. il devrait valoir: {0} mais vaut: {1}"},
    { "Eccentricity is becoming greater than 1. Unable to continue TLE propagation." +
      "Satellite number : {0}. Element number : {1}.",
      "L''eccentricit\u00e9 d\u00e9passe 1. Impossible de continuer l''extrapolation de TLE." +
      "Num\u00e9ro de satellite : {0}. Numero de l''\u00e9l\u00e9ment: {1}."},
    { "Perige within earth." + "Satellite number : {0}. Element number : {1}.",
      "P\u00e9rig\u00e9e souterrain."  +
      "Num\u00e9ro de satellite : {0}. Num\u00e9ro de l''\u00e9l\u00e9ment: {1}."},
    { "Current date is out of range. " + 
      "Solar activity datas are not available" ,
      "La date est hors de l''intervalle permis. " + 
      "Les param\u00e8tres d''activit\u00e9 du soleil ne sont pas disponibles. " }
   
  };

}
