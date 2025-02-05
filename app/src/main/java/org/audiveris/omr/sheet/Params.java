//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                           P a r a m s                                          //
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
package org.audiveris.omr.sheet;

import org.audiveris.omr.image.FilterDescriptor;
import org.audiveris.omr.image.FilterParam;
import org.audiveris.omr.text.Language;
import org.audiveris.omr.ui.symbol.MusicFamily;
import org.audiveris.omr.ui.symbol.MusicFont;
import org.audiveris.omr.ui.symbol.TextFamily;
import org.audiveris.omr.ui.symbol.TextFont;
import org.audiveris.omr.util.param.IntegerParam;
import org.audiveris.omr.util.param.StringParam;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Class <code>Params</code> is the base structure of editable parameters for the Book and
 * SheetStub classes.
 *
 * @param <P> parent type: either Object (for a Book) or Book (for a SheetStub)
 * @author Hervé Bitteur
 */
public abstract class Params<P>
        implements Cloneable
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(Params.class);

    //~ Instance fields ----------------------------------------------------------------------------

    /** Specification of the MusicFont family to use. */
    @XmlElement(name = "music-font")
    public MusicFamily.MyParam musicFamily;

    /** Specification of the TextFont family to use. */
    @XmlElement(name = "text-font")
    public TextFamily.MyParam textFamily;

    /** Specification of the input quality to use. */
    @XmlElement(name = "input-quality")
    public InputQualityParam inputQuality;

    /** Specification of binarization filter. */
    @XmlElement(name = "binarization")
    @XmlJavaTypeAdapter(FilterParam.JaxbAdapter.class)
    public FilterParam binarizationFilter;

    /** Specification of interline in pixels. */
    @XmlElement(name = "interline")
    public IntegerParam interlineSpecification;

    /** Specification of barline height, in interlines, for 1-line staves. */
    @XmlElement(name = "barline-height")
    public BarlineHeight.MyParam barlineSpecification;

    /** Specification of beam thickness, in pixels. */
    @XmlElement(name = "beam-thickness")
    public IntegerParam beamSpecification;

    /**
     * This string specifies the dominant language(s).
     * <p>
     * For example, <code>eng+ita</code> specification will ask OCR to use English and Italian
     * dictionaries and only those.
     */
    @XmlElement(name = "ocr-languages")
    public StringParam ocrLanguages;

    /** The set of specific processing switches. */
    @XmlElement(name = "processing")
    public ProcessingSwitches switches;

    //~ Constructors -------------------------------------------------------------------------------

    protected Params ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------

    /**
     * For any missing param in this structure, allocate the proper param.
     */
    public final void completeParams ()
    {
        if (musicFamily == null)
            musicFamily = new MusicFamily.MyParam(null);

        if (textFamily == null)
            textFamily = new TextFamily.MyParam(null);

        if (inputQuality == null)
            inputQuality = new InputQualityParam(null);

        if (binarizationFilter == null)
            binarizationFilter = new FilterParam(null);

        if (interlineSpecification == null)
            interlineSpecification = new IntegerParam(null);

        if (barlineSpecification == null)
            barlineSpecification = new BarlineHeight.MyParam(null);

        if (beamSpecification == null)
            beamSpecification = new IntegerParam(null);

        if (ocrLanguages == null)
            ocrLanguages = new StringParam(null);

        if (switches == null)
            switches = new ProcessingSwitches(null, null);
    }

    /**
     * Clone the structure.
     *
     * @return a cloned structure
     */
    public abstract Params duplicate ();

    /** Report whether all structure params are null. */
    private boolean isEmpty ()
    {
        return musicFamily == null //
                && textFamily == null //
                && inputQuality == null //
                && binarizationFilter == null //
                && interlineSpecification == null //
                && barlineSpecification == null //
                && beamSpecification == null //
                && ocrLanguages == null //
                && switches == null;
    }

    /**
     * Nullify all params without specific value.
     * NOTA: An integer zero value is considered as non-specific.
     *
     * @return true if the pruned structure is now empty
     */
    public boolean prune ()
    {
        if ((musicFamily != null) && !musicFamily.isSpecific()) {
            musicFamily = null;
        }

        if ((textFamily != null) && !textFamily.isSpecific()) {
            textFamily = null;
        }

        if ((inputQuality != null) && !inputQuality.isSpecific()) {
            inputQuality = null;
        }

        if ((binarizationFilter != null) && !binarizationFilter.isSpecific()) {
            binarizationFilter = null;
        }

        if ((ocrLanguages != null) && !ocrLanguages.isSpecific()) {
            ocrLanguages = null;
        }

        // A O value means no specific value
        if ((interlineSpecification != null) //
                && ((interlineSpecification.getSpecific() == null) //
                        || (interlineSpecification.getSpecific() == 0))) {
            interlineSpecification = null;
        }

        if ((barlineSpecification != null) && !barlineSpecification.isSpecific()) {
            barlineSpecification = null;
        }

        // A O value means no specific value
        if ((beamSpecification != null) //
                && ((beamSpecification.getSpecific() == null) //
                        || (beamSpecification.getSpecific() == 0))) {
            beamSpecification = null;
        }

        if ((switches != null) && switches.isEmpty()) {
            switches = null;
        }

        return isEmpty();
    }

    /**
     * Connect each structure param to the corresponding param in the parent scope.
     *
     * @param parent the parent scope (null for a Book, book for a SheetStub)
     */
    public abstract void setParents (P parent);

    /**
     * Set the scope of each structure param.
     *
     * @param scope the provided scope (Book or SheetStub)
     */
    public final void setScope (Object scope)
    {
        musicFamily.setScope(scope);
        textFamily.setScope(scope);
        inputQuality.setScope(scope);
        binarizationFilter.setScope(scope);
        interlineSpecification.setScope(scope);
        barlineSpecification.setScope(scope);
        beamSpecification.setScope(scope);
        ocrLanguages.setScope(scope);
        switches.setScope(scope);
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //------------//
    // BookParams //
    //------------//
    /**
     * Parameters structure for a Book.
     */
    public static class BookParams
            extends Params<Object>
    {
        public BookParams ()
        {
        }

        @Override
        public BookParams duplicate ()
        {
            try {
                return (BookParams) super.clone();
            } catch (CloneNotSupportedException ex) {
                return null; // Should never happen
            }
        }

        @Override
        public final void setParents (Object ignored)
        {
            // These (default) parents are taken from diverse sources
            musicFamily.setParent(MusicFont.defaultMusicParam);
            textFamily.setParent(TextFont.defaultTextParam);
            inputQuality.setParent(Profiles.defaultQualityParam);
            binarizationFilter.setParent(FilterDescriptor.defaultFilter);
            interlineSpecification.setParent(Scale.defaultInterlineSpecification);
            barlineSpecification.setParent(BarlineHeight.defaultParam);
            beamSpecification.setParent(Scale.defaultBeamSpecification);
            ocrLanguages.setParent(Language.ocrDefaultLanguages);
            switches.setParent(ProcessingSwitches.getDefaultSwitches());
        }
    }

    //-------------//
    // SheetParams //
    //-------------//
    /**
     * Parameters structure for a SheetStub.
     */
    public static class SheetParams
            extends Params<Book>
    {
        public SheetParams ()
        {
        }

        @Override
        public SheetParams duplicate ()
        {
            try {
                return (SheetParams) super.clone();
            } catch (CloneNotSupportedException ex) {
                return null; // Should never happen
            }
        }

        @Override
        public final void setParents (Book book)
        {
            musicFamily.setParent(book.getMusicFamilyParam());
            textFamily.setParent(book.getTextFamilyParam());
            inputQuality.setParent(book.getInputQualityParam());
            binarizationFilter.setParent(book.getBinarizationParam());
            interlineSpecification.setParent(book.getInterlineSpecificationParam());
            barlineSpecification.setParent(book.getBarlineHeightParam());
            beamSpecification.setParent(book.getBeamSpecificationParam());
            ocrLanguages.setParent(book.getOcrLanguagesParam());
            switches.setParent(book.getProcessingSwitches());
        }
    }
}
