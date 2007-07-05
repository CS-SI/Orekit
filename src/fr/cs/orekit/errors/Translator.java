package fr.cs.orekit.errors;

import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.MissingResourceException;

/** This utility class translates strings and formats.
 * @author L. Maisonobe
 * 
 */
public class Translator {

  /** Private constructor.
   * <p>This class is a singleton, hence the constructor is private.</p>
   */
  private Translator() {
    resources =
      ResourceBundle.getBundle("fr.cs.orekit.errors.MessagesResources");
  }

  /** Get the unique instance.
   * @return the unique instance
   */
  public static Translator getInstance() {
    if (instance == null) {
      instance = new Translator();
    }
    return instance;
  }

  /** Translate a string.
   * @param s string to translate
   * @return translated string, or original string if no translation
   * can be found)
   */
  public String translate(String s) {
    return translate(s, new String[0]);
  }

  /** Translate and format a message.
   * @param specifier format specifier (to be translated)
   * @param parts parts to insert in the format (no translation)
   */
  public String translate(String specifier, String[] parts) {
    String translated;
    try {
      translated = resources.getString(specifier);
    } catch (MissingResourceException mre) {
      translated = specifier;
    }
    return new MessageFormat(translated).format(parts);
  }

  /** Unique instance. */
  private static Translator instance = null;

  /** Resources bundle. */
  private ResourceBundle resources;

}
