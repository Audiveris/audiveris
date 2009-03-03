//----------------------------------------------------------------------------//
//                                                                            //
//                              T e x t T y p e                               //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
package omr.glyph;


/**
 * Class <code>TextType</code> describes the role of a piece of text (typically
 * a sentence)
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public enum TextType {
    /** No type known */
    NoType,
    /** (Part of) lyrics */
    Lyrics, 
    /** Title of the opus */
    Title, 
    /** Playing instruction */
    Direction, 
    /** Number for this opus */
    Number, 
    /** Name for the part */
    Name, 
    /** A creator (composer, etc...) */
    Creator, 
    /** Copyright notice */
    Rights;
    /**/

    //-----------------//
    // getStringHolder //
    //-----------------//
    /**
     * A convenient method to forge a string to be used in lieu of text value
     * @param NbOfChars the number of characters desired
     * @return a dummy string of NbOfChars chars
     */
    public String getStringHolder (int NbOfChars)
    {
        StringBuilder sb = new StringBuilder("[");

        while (sb.length() < (NbOfChars - 1)) {
            sb.append(toString())
              .append("-");
        }

        return sb.substring(0, Math.max(NbOfChars - 1, 0)) + "]";
    }
}
