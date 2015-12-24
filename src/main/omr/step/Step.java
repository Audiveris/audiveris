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
import omr.sheet.symbol.LinksStep;
import omr.sheet.symbol.SymbolsStep;
import omr.sheet.ui.SheetTab;

import omr.text.TextsStep;

import java.util.Collection;

/**
 * Enum {@code Step} describes the steps of sheet processing pipeline.
 * <p>
 * <img src="doc-files/Step.png" alt="Step image">
 *
 * @author Hervé Bitteur
 */
public enum Step
{

    LOAD("Load the sheet (gray) picture", new LoadStep()),
    BINARY("Binarize the sheet picture", new BinaryStep()),
    SCALE("Compute sheet line thickness, interline, beam thickness", new ScaleStep()),
    GRID("Retrieve staff lines, barlines, systems & parts", new GridStep()),
    HEADERS("Retrieve Clef-Key-Time systems headers", new HeadersStep()),
    STEM_SEEDS("Retrieve Stem thickness & seeds for stems", new StemSeedsStep()),
    BEAMS("Retrieve beams", new BeamsStep()),
    LEDGERS("Retrieve ledgers", new LedgersStep()),
    HEADS("Retrieve note heads & whole notes", new HeadsStep()),
    STEMS("Build stems connected to heads & beams", new StemsStep()),
    REDUCTION("Reduce structures of heads, stems & beams", new ReductionStep()),
    CUE_BEAMS("Retrieve cue beams", new CueBeamsStep()),
    TEXTS("Call OCR on textual items", new TextsStep()),
    MEASURES("Retrieve raw measures from groups of bar lines", new MeasuresStep()),
    CHORDS("Gather notes heads into chords", new ChordsStep()),
    CURVES("Retrieve slurs, wedges & endings", new CurvesStep()),
    SYMBOLS("Retrieve fixed-shape symbols", new SymbolsStep()),
    RHYTHMS("Handle rhythms within measures", new RhythmsStep()),
    LINKS("Link symbols", new LinksStep()),
    PAGE("Connect systems within page", new PageStep());

    /** Description of the step. */
    private final String description;

    /** Helper for step implementation. */
    private final AbstractStep helper;

    /**
     * Create an instance of {@code Step}.
     *
     * @param description step description
     * @param helper      step implementation
     */
    private Step (String description,
                  AbstractStep helper)
    {
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

    //-------------//
    // clearErrors //
    //-------------//
    /**
     * Clear the errors that relate to this step on the provided sheet. (Not used)
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

    //-------------//
    // getSheetTab //
    //-------------//
    /**
     * Related assembly view tab, selected when step completes
     *
     * @return the related view tab
     */
    public SheetTab getSheetTab ()
    {
        return helper.getSheetTab();
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

        public void setValue (Step step)
        {
            setTuple(step.toString(), step);
        }

        @Override
        public void setValue (java.lang.String str)
        {
            setValue(decode(str));
        }

        @Override
        protected Step decode (java.lang.String str)
        {
            return Step.valueOf(str);
        }
    }
}
