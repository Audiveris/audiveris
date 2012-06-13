//----------------------------------------------------------------------------//
//                                                                            //
//                          G l y p h C o n t e n t                           //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2012. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.facets;

import omr.glyph.text.OcrChar;
import omr.glyph.text.OcrLine;
import omr.glyph.text.TextArea;
import omr.glyph.text.TextRole;

import omr.lag.Section;

import omr.score.common.PixelPoint;
import omr.score.entity.Text.CreatorText.CreatorType;

import java.util.List;
import java.util.SortedSet;
import omr.glyph.text.Sentence;

/**
 * Interface {@code GlyphContent} defines a facet that deals with
 * the textual content, if any, of a glyph.
 *
 * @author Hervé Bitteur
 */
public interface GlyphContent
    extends GlyphFacet
{
    //~ Instance fields --------------------------------------------------------

    /**
     * String equivalent of Character used for elision. (undertie)
     */
    String ELISION_STRING = new String(Character.toChars(8255));

    /**
     * String equivalent of Character used for extension. (underscore)
     */
    String EXTENSION_STRING = "_";

    /**
     * String equivalent of Character used for hyphen.
     */
    String HYPHEN_STRING = "-";

    //~ Methods ----------------------------------------------------------------

    /**
     * Report the containing sentence, if any
     * @return the sentence
     */
    Sentence getSentence ();

    /**
     * Assign the containing sentence
     * @param sentence the sentence to set
     */
    void setSentence (Sentence sentence);
    
    /**
     * Report the creator type, if any.
     * @return the creatorType
     */
    CreatorType getCreatorType ();

    /**
     * Report the proper font size for the textual glyph.
     * @return the fontSize
     */
    Float getFontSize ();

    /**
     * Report the manually assigned text, if any.
     * @return manualValue the manual string value for this glyph, or null
     * @see #setManualValue
     */
    String getManualValue ();

    /**
     * Report the current language, if any, defined for this glyph.
     * @return the current glyph language code, or null
     */
    String getOcrLanguage ();

    /**
     * Return the single OCR line for this glyph, assuming that the
     * glyph language is set and the OCR output consists in exactly
     * one line.
     * @return the single OCR line, if all conditions are met, null otherwise.
     */
    OcrLine getOcrLine ();

    /**
     * Report the detailed OCR information for a given language.
     * @return the ocrLine(s), if any, for this language.
     * Nota, if more than 1 line is found, many methods such as {@link
     * #getOcrValue} are not relevant.
     * A null value indicates that no OCR data is available.
     * @see #setOcrLines
     */
    List<OcrLine> getOcrLines (String language);

    /**
     * Report what the OCR has provided for this glyph using its
     * current language.
     * @return the text provided by the OCR engine, if any. Return null
     * if no language is set or if the corresponding OCR output is not
     * a single line.
     */
    String getOcrValue ();

    /**
     * Report a dummy content for this glyph (for lack of known content).
     * @return an artificial text content, based on the enclosing sentence type
     */
    String getPseudoValue ();

    /**
     * Report the text area that contains this glyph.
     * @return the text area for this glyph
     */
    TextArea getTextArea ();

    /**
     * Determine the uniform character height for this glyph.
     * @return the standard character height in pixels
     */
    int getTextHeight ();

    /**
     * Report the text type (role) of the textual glyph within the score.
     * @return the role of this textual glyph
     * @see #setTextRole
     */
    TextRole getTextRole ();

    /**
     * Report the starting point of this text glyph, which is the left
     * side abscissa and the baseline ordinate.
     * @return the starting point of the text glyph, specified in pixels
     */
    PixelPoint getTextStart ();

    /**
     * Report the string value of this text glyph if any.
     * @return the text meaning of this glyph if any, either entered manually
     * or via an OCR function
     */
    String getTextValue ();

    /**
     * Launch the OCR on this glyph, to retrieve the OcrLine instance(s)
     * this glyph represents.
     * @param language the probable language
     * @return a list, not null but perhaps empty, of OcrLine instances with
     * absolute coordinates.
     */
    List<OcrLine> retrieveOcrLines (String language);

    /**
     * Retrieve the glyph sections that correspond to the collection
     * of OCR char descriptors.
     * @param chars the char descriptors
     * @return the set of corresponding sections
     */
    SortedSet<Section> retrieveSections (List<OcrChar> chars);

    /**
     * Decompose the glyph into a sequence of (sub) glyphs, either by
     * word or by syllable.
     * @param bySyllable true for split by syllable, false for split by word
     * @return The sequence of (sub) glyphs created, with exactly one glyph
     * per word (or syllable). Non null but perhaps empty.
     */
    List<Glyph> retrieveSubGlyphs (boolean bySyllable);

    /**
     * Set the creator type.
     * @param creatorType the creatorType to set
     */
    void setCreatorType (CreatorType creatorType);

    /**
     * Manually assign a text meaning to the glyph.
     * @param manualValue the string value for this text glyph
     * @see #getManualValue
     */
    void setManualValue (String manualValue);

    /**
     * Store the information as provided by the OCR engine or by the
     * word-based split of (long) sentence.
     * @param ocrLanguage the language provided to OCR engine for recognition
     * @param ocrLines the sequence of OCR lines for this glyph
     * @see #getOcrLines
     */
    void setOcrLines (String        ocrLanguage,
                      List<OcrLine> ocrLines);

    /**
     * Force the text type (role) of the textual glyph within the score.
     * @param type the role of this textual item
     * @see #getTextRole
     */
    void setTextRole (TextRole type);
}
