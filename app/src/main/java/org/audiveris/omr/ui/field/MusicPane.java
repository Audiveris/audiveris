//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        M u s i c P a n e                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2025. All rights reserved.
//
//  This program is free software: you can redistribute it and/or modify it under the terms of the
//  GNU Affero General Public License as published by the Free Software Foundation, either version
//  3 of the License, or (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
//  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//  See the GNU Affero General Public License for more details.
//
//  You should have received a copy of the GNU Affero General Public License along with this
//  program.  If not, see <http://www.gnu.org/licenses/>.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package org.audiveris.omr.ui.field;

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.ShapeSet;
import org.audiveris.omr.ui.Colors;
import org.audiveris.omr.ui.symbol.MusicFamily;
import org.audiveris.omr.ui.symbol.Symbols;
import org.audiveris.omr.ui.symbol.TextFamily;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Font;
import java.util.HashSet;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JTextPane;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

/**
 * Class <code>MusicPane</code> is a JTextPane which can mix text and music code points.
 *
 * @author Hervé Bitteur
 */
public class MusicPane
        extends JTextPane
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(MusicPane.class);

    /** Minimum code point value to detect a potential music character. */
    private static final int HIGH_CODE = 0xE000;

    /** Codes of supported music characters. */
    public static final Set<Integer> musicCodes = buildMusicCodes();

    //~ Instance fields ----------------------------------------------------------------------------

    /** Attributes referring the music font family. */
    private final SimpleAttributeSet musicSet = new SimpleAttributeSet();

    /** Default text attributes. */
    private final SimpleAttributeSet textSet = new SimpleAttributeSet();

    /** The specific pane document. */
    private final StyledDocument doc = new MyStyledDocument();

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new <code>MusicPane</code> object.
     *
     * @param editable    Specifies whether this pane will be editable
     * @param tip         the related tool tip text
     * @param musicFamily the music family to be used initially
     * @param textFamily  the text family to be used
     * @see #setFamilies(MusicFamily)
     */

    public MusicPane (boolean editable,
                      String tip,
                      MusicFamily musicFamily,
                      TextFamily textFamily)
    {
        setEditable(editable);

        if (tip != null) {
            setToolTipText(tip);
        }

        if (!editable) {
            setFocusable(false);
            setBorder(null);
        } else {
            setBorder(BorderFactory.createEtchedBorder());
        }

        // Specific attributes for music portions
        StyleConstants.setFontFamily(musicSet, musicFamily.name());
        StyleConstants.setForeground(musicSet, Color.RED);
        StyleConstants.setFontSize(musicSet, constants.fontSize.getValue());

        // Use a large enough font size to make content more readable
        setFont(new Font(textFamily.name(), Font.PLAIN, constants.fontSize.getValue()));

        // Use a specific background color, to avoid confusion with plain text fields
        setBackground(Colors.MUSIC_PANE_BACKGROUND);

        setDocument(doc);
    }

    //~ Methods ------------------------------------------------------------------------------------

    //-----------------------//
    // adjustMusicCharacters //
    //-----------------------//
    /**
     * Process every character to set music attributes where needed.
     */
    public void adjustMusicCharacters ()
    {
        final String str = getText();
        final int length = str.length();

        // Set default attributes to all characters
        doc.setCharacterAttributes(0, length, textSet, true);

        for (int i = 0; i < length; i++) {
            final int code = str.codePointAt(i);

            if (musicCodes.contains(code)) {
                // Set music attributes to the current character
                doc.setCharacterAttributes(i, 1, musicSet, true);
            }
        }
    }

    //-------------//
    // insertMusic //
    //-------------//
    /**
     * Use the provided music string to replace the selection if any,
     * otherwise insert it at the caret location.
     *
     * @param str the music string to insert
     */
    public void insertMusic (String str)
    {
        try {
            if (str == null || str.isEmpty()) {
                return;
            }

            // Selection?
            final int start = getSelectionStart();
            final int end = getSelectionEnd();
            if (end > start) {
                doc.remove(start, end - start);
            }

            // Insert raw text
            doc.insertString(start, str, null);
        } catch (BadLocationException ex) {
            logger.warn("MusicPane.insertMusic error {}", ex.getMessage(), ex);
        }
    }

    //-------------//
    // setFamilies //
    //-------------//
    /**
     * Switch to the provided music and text families
     *
     * @param musicFamily the new music family
     * @param textFamily  the new text family
     */
    public void setFamilies (MusicFamily musicFamily,
                             TextFamily textFamily)
    {
        StyleConstants.setFontFamily(musicSet, musicFamily.name());
        StyleConstants.setFontFamily(textSet, textFamily.name());

        adjustMusicCharacters();
    }

    //~ Static Methods -----------------------------------------------------------------------------

    //-----------------//
    // buildMusicCodes //
    //-----------------//
    private static Set<Integer> buildMusicCodes ()
    {
        final Set<Integer> set = new HashSet<>();
        final Symbols symbols = MusicFamily.Bravura.getSymbols();

        ShapeSet.BeatUnits.forEach(bu -> {
            for (int c : symbols.getCode(bu)) {
                if (c >= HIGH_CODE) {
                    set.add(c);
                }
            }
        });

        return set;
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {
        private final Constant.Integer fontSize = new Constant.Integer(
                "PointSize",
                20,
                "Font size for text and music");
    }

    //------------------//
    // MyStyledDocument //
    //------------------//
    /**
     * A StyledDocument which adjusts its content (music vs text) after each insertion.
     */
    public class MyStyledDocument
            extends DefaultStyledDocument
    {
        @Override
        public void insertString (int offs,
                                  String str,
                                  AttributeSet a)
            throws BadLocationException
        {
            super.insertString(offs, str, a);
            adjustMusicCharacters();
        }
    }
}
