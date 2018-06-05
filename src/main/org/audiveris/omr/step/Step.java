//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                            S t e p                                             //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2017. All rights reserved.
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
package org.audiveris.omr.step;

import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.beam.BeamsStep;
import org.audiveris.omr.sheet.beam.CueBeamsStep;
import org.audiveris.omr.sheet.curve.CurvesStep;
import org.audiveris.omr.sheet.grid.GridStep;
import org.audiveris.omr.sheet.header.HeadersStep;
import org.audiveris.omr.sheet.ledger.LedgersStep;
import org.audiveris.omr.sheet.note.ChordsStep;
import org.audiveris.omr.sheet.note.HeadsStep;
import org.audiveris.omr.sheet.rhythm.MeasuresStep;
import org.audiveris.omr.sheet.rhythm.RhythmsStep;
import org.audiveris.omr.sheet.stem.StemSeedsStep;
import org.audiveris.omr.sheet.stem.StemsStep;
import org.audiveris.omr.sheet.symbol.LinksStep;
import org.audiveris.omr.sheet.symbol.SymbolsStep;
import org.audiveris.omr.sheet.ui.SheetTab;
import org.audiveris.omr.sig.ui.UITask.OpKind;
import org.audiveris.omr.sig.ui.UITaskList;
import org.audiveris.omr.text.TextsStep;

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
    ANNOTATIONS("Detect and classify most symbols", new AnnotationsStep()),
    GRID("Retrieve staff lines, barlines, systems & parts", new GridStep()),
    HEADERS("Retrieve Clef-Key-Time systems headers", new HeadersStep()),
    STEM_SEEDS("Retrieve stem thickness & seeds for stems", new StemSeedsStep()),
    BEAMS("Retrieve beams", new BeamsStep()),
    LEDGERS("Retrieve ledgers", new LedgersStep()),
    HEADS("Retrieve note heads & whole notes", new HeadsStep()),
    STEMS("Build stems connected to heads & beams", new StemsStep()),
    REDUCTION("Reduce conflicts in heads, stems & beams", new ReductionStep()),
    CUE_BEAMS("Retrieve cue beams", new CueBeamsStep()),
    TEXTS("Call OCR on textual items", new TextsStep()),
    MEASURES("Retrieve raw measures from groups of bar lines", new MeasuresStep()),
    CHORDS("Gather notes heads into chords", new ChordsStep()),
    CURVES("Retrieve slurs, wedges & endings", new CurvesStep()),
    SYMBOLS("Retrieve fixed-shape symbols", new SymbolsStep()),
    LINKS("Link and reduce symbols", new LinksStep()),
    RHYTHMS("Handle rhythms within measures", new RhythmsStep()),
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

    //--------------//
    // isImpactedBy //
    //--------------//
    /**
     * Report whether this step is impact by an action on the provided class
     *
     * @param classe the class to examine
     * @return true if impacted
     */
    public boolean isImpactedBy (Class classe)
    {
        return helper.isImpactedBy(classe);
    }

    //------------//
    // isParallel //
    //------------//
    /**
     * Report whether the step can run systems in parallel.
     *
     * @return true if potentially parallel
     */
    public boolean isParallel ()
    {
        return helper instanceof AbstractSystemStep;
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
     * @param sheet the sheet to work upon
     * @throws StepException if processing had to stop at this step
     */
    public void doit (Sheet sheet)
            throws StepException
    {
        helper.doit(sheet);
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

    //--------//
    // impact //
    //--------//
    /**
     * Process the impact of the UI task sequence on this step.
     *
     * @param seq    the provided UI task sequence
     * @param opKind which operation is done on seq
     */
    public void impact (UITaskList seq,
                        OpKind opKind)
    {
        helper.impact(seq, opKind);
    }
}
