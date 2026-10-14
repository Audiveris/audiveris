//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                            S t e p                                             //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2026. All rights reserved.
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
import org.audiveris.omr.util.ResourceUtil;

import java.util.ResourceBundle;

/**
 * Enum <code>OmrStep</code> describes the sequence of sheet processing steps in the OMR
 * engine pipeline.
 * <p>
 * <img src="doc-files/Step.png" alt="Image of all OMR steps">
 *
 * @author Hervé Bitteur
 */
public enum OmrStep
{
    LOAD(new LoadStep()),
    BINARY(new BinaryStep()),
    SCALE(new ScaleStep()),
    GRID(new GridStep()),
    HEADERS(new HeadersStep()),
    STEM_SEEDS(new StemSeedsStep()),
    BEAMS(new BeamsStep()),
    LEDGERS(new LedgersStep()),
    HEADS(new HeadsStep()),
    STEMS(new StemsStep()),
    REDUCTION(new ReductionStep()),
    CUE_BEAMS(new CueBeamsStep()),
    TEXTS(new TextsStep()),
    MEASURES(new MeasuresStep()),
    CHORDS(new ChordsStep()),
    CURVES(new CurvesStep()),
    SYMBOLS(new SymbolsStep()),
    LINKS(new LinksStep()),
    RHYTHMS(new RhythmsStep()),
    PAGE(new PageStep());

    //~ Static fields/initializers -----------------------------------------------------------------

    private static final ResourceBundle resources = ResourceUtil.getBundle(OmrStep.class);

    //~ Instance fields ----------------------------------------------------------------------------

    /** Description of the step. */
    private String description;

    /** User-friendly explanation of the step. */
    private String tip;

    /** Helper for step implementation. */
    private final AbstractStep helper;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Create an instance of <code>Step</code>.
     *
     * @param helper step implementation
     */
    private OmrStep (AbstractStep helper)
    {
        this.helper = helper;
    }

    //~ Methods ------------------------------------------------------------------------------------

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
     * Report a user-friendly description of this OmrStep.
     *
     * @return the step description
     */
    public String getDescription ()
    {
        if (description == null) {
            description = resources.getString(name() + ".text");

            if (description == null) {
                description = name(); // Better than nothing!
            }
        }

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
    // getTip //
    //--------//
    /**
     * Report a user-friendly explanation of this OmrStep.
     *
     * @return the step explanation
     */
    public String getTip ()
    {
        if (tip == null) {
            tip = resources.getString(name() + ".toolTipText");

            if (tip == null) {
                tip = name(); // Better than nothing!
            }
        }

        return tip;
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

    //--------------//
    // isImpactedBy //
    //--------------//
    /**
     * Report whether this step is impact by an action on the provided class
     *
     * @param classe the class to examine
     * @return true if impacted
     */
    public boolean isImpactedBy (Class<?> classe)
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

    //-------//
    // first //
    //-------//
    /**
     * Report the first step in enumeration
     *
     * @return the first step
     */
    public static OmrStep first ()
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
    public static OmrStep last ()
    {
        return values()[values().length - 1];
    }

    //--------------//
    // lastParallel //
    //--------------//
    /**
     * Report the last step for which sheets can be processed in parallel.
     *
     * @return the last parallel step
     */
    public static OmrStep lastParallel ()
    {
        return LINKS;
    }
}
