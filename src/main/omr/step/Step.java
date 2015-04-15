//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                            S t e p                                             //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.step;

import omr.sheet.Sheet;
import omr.sheet.SystemInfo;
import omr.sheet.beam.BeamsStep;
import omr.sheet.beam.CueBeamsStep;
import omr.sheet.curve.CurvesStep;
import omr.sheet.grid.GridStep;
import omr.sheet.header.HeadersStep;
import omr.sheet.ledger.LedgersStep;
import omr.sheet.note.ChordsStep;
import omr.sheet.note.HeadsStep;
import omr.sheet.rhythm.MeasuresStep;
import omr.sheet.rhythm.RhythmsStep;
import omr.sheet.stem.StemSeedsStep;
import omr.sheet.stem.StemsStep;
import omr.sheet.symbol.SymbolReductionStep;
import omr.sheet.symbol.SymbolsStep;
import omr.sheet.ui.SheetTab;
import static omr.sheet.ui.SheetTab.*;

import omr.text.TextsStep;

import java.util.Collection;

/**
 * Enum {@code Step} describes a sheet processing step.
 * <p>
 * <img src="doc-files/Step.png" alt="Step image" />
 *
 * @author Hervé Bitteur
 */
public enum Step
{

    LOAD("Load the sheet (gray) picture", PICTURE_TAB, new LoadStep()),
    BINARY("Binarize the sheet picture", BINARY_TAB, new BinaryStep()),
    SCALE("Compute sheet line thickness, interline, beam thickness", BINARY_TAB, new ScaleStep()),
    GRID("Retrieve Staff lines, bar-lines, systems & parts", DATA_TAB, new GridStep()),
    HEADERS("Retrieve Clef-Key-Time systems headers", DATA_TAB, new HeadersStep()),
    STEM_SEEDS("Retrieve Stem thickness & seeds for stems", DATA_TAB, new StemSeedsStep()),
    BEAMS("Retrieve beams", DATA_TAB, new BeamsStep()),
    LEDGERS("Retrieve ledgers", DATA_TAB, new LedgersStep()),
    HEADS("Retrieve note heads & whole notes", DATA_TAB, new HeadsStep()),
    STEMS("Build stems connected to heads & beams", DATA_TAB, new StemsStep()),
    REDUCTION("Reduce structures of heads, stems & beams", DATA_TAB, new ReductionStep()),
    CUE_BEAMS("Retrieve cue beams", DATA_TAB, new CueBeamsStep()),
    TEXTS("Call OCR on textual items", DATA_TAB, new TextsStep()),
    CHORDS("Gather notes heads into chords", DATA_TAB, new ChordsStep()),
    CURVES("Retrieve slurs, wedges & endings", DATA_TAB, new CurvesStep()),
    SYMBOLS("Retrieve fixed-shape symbols", DATA_TAB, new SymbolsStep()),
    MEASURES("Retrieve raw measures from groups of bar lines", DATA_TAB, new MeasuresStep()),
    RHYTHMS("Handle rhythms within measures", DATA_TAB, new RhythmsStep()),
    SYMBOL_REDUCTION("Reduce symbols", DATA_TAB, new SymbolReductionStep()),
    PAGE("Connect systems within page", DATA_TAB, new PageStep());

    /** Related short label. */
    private final SheetTab tab;

    /** Description of the step. */
    private final String description;

    /** Helper for step implementation. */
    private final AbstractStep helper;

    /**
     * Create an instance of {@code Step}.
     *
     * @param description step description
     * @param tab         tab for related sheet assembly
     * @param helper      step implementation
     */
    private Step (String description,
                  SheetTab tab,
                  AbstractStep helper)
    {
        this.tab = tab;
        this.description = description;
        this.helper = helper;
    }

    //-------//
    // first //
    //-------//
    /**
     * Report the first step in enumeration
     *
     * @return the first step
     */
    public static Step first ()
    {
        return values()[0];
    }

    //------//
    // last //
    //------//
    /**
     * Report the last step in enumeration
     *
     * @return the last step
     */
    public static Step last ()
    {
        return values()[values().length - 1];
    }

    //------//
    // next //
    //------//
    /**
     * Report the step immediately after this one.
     *
     * @return the next step if any, otherwise null
     */
    public Step next ()
    {
        if (ordinal() < (values().length - 1)) {
            return values()[ordinal() + 1];
        }

        return null;
    }

    //-------------//
    // clearErrors //
    //-------------//
    /**
     * Clear the errors that relate to this step on the provided sheet.
     *
     * @param sheet the sheet to work upon
     */
    public void clearErrors (Sheet sheet)
    {
        helper.clearErrors(this, sheet);
    }

    //-----------//
    // displayUI //
    //-----------//
    /**
     * Make the related user interface visible for this step.
     *
     * @param sheet the sheet to work upon
     */
    public void displayUI (Sheet sheet)
    {
        helper.displayUI(this, sheet);
    }

    //------//
    // doit //
    //------//
    /**
     * Run the step.
     *
     * @param systems systems to process (null means all systems)
     * @param sheet   the sheet to work upon
     * @throws StepException if processing had to stop at this step
     */
    public void doit (Collection<SystemInfo> systems,
                      Sheet sheet)
            throws StepException
    {
        helper.doit(systems, sheet);
    }

    //----------------//
    // getDescription //
    //----------------//
    /**
     * Report a description of the step.
     *
     * @return a short description
     */
    public String getDescription ()
    {
        return description;
    }

    //--------//
    // getTab //
    //--------//
    /**
     * Related assembly view tab, selected when steps completes
     *
     * @return the related view tab
     */
    public SheetTab getTab ()
    {
        return tab;
    }

    //----------//
    // Constant //
    //----------//
    /**
     * Class {@code Constant} is a {@link omr.constant.Constant} meant to store a {@link
     * Step} value.
     */
    public static class Constant
            extends omr.constant.Constant
    {

        public Constant (Step defaultValue,
                         java.lang.String description)
        {
            super(null, defaultValue.toString(), description);
        }

        public Step getValue ()
        {
            return (Step) getCachedValue();
        }

        public void setValue (Step val)
        {
            setTuple(val.toString(), val);
        }

        @Override
        public void setValue (java.lang.String string)
        {
            setValue(decode(string));
        }

        @Override
        protected Step decode (java.lang.String str)
        {
            return Step.valueOf(str);
        }
    }
}
