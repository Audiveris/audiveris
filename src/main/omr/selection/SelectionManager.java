//----------------------------------------------------------------------------//
//                                                                            //
//                      S e l e c t i o n M a n a g e r                       //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.               //
//  This software is released under the terms of the GNU General Public       //
//  License. Please contact the author at herve.bitteur@laposte.net           //
//  to report bugs & suggestions.                                             //
//----------------------------------------------------------------------------//
//
package omr.selection;

import omr.sheet.Sheet;
import omr.sheet.SheetManager;

import java.util.HashMap;
import java.util.Map;

/**
 * Class <code>SelectionManager</code> handles all selections for a single sheet
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class SelectionManager
{
    //~ Instance fields --------------------------------------------------------

    /** All current selections */
    private final Map<SelectionTag, Selection> selections = new HashMap<SelectionTag, Selection>();

    /** Related sheet */
    private Sheet sheet;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new instance of SelectionManager
     * @param sheet the sheet for which this manager works
     */
    public SelectionManager (Sheet sheet)
    {
        this.sheet = sheet;
    }

    //~ Methods ----------------------------------------------------------------

    //--------------//
    // getSelection //
    //--------------//
    /**
     * Report, within this sheet, the Selection related to the provided Tag
     *
     * @param tag specific selection (such as PIXEL, GLYPH, etc)
     * @return the selection object, that can be observed
     */
    public Selection getSelection (SelectionTag tag)
    {
        Selection selection = selections.get(tag);

        // Lazy construction
        if (selection == null) {
            selection = new Selection(tag);
            selections.put(tag, selection);
        }

        return selection;
    }

    //-------------------//
    // dumpAllSelections //
    //-------------------//
    /**
     * Dump current state of all selections of all sheets
     */
    public static void dumpAllSelections ()
    {
        for (Sheet sheet : SheetManager.getInstance()
                                       .getSheets()) {
            sheet.getSelectionManager()
                 .dumpSelections();
        }
    }

    //-------------//
    // addObserver //
    //-------------//
    /**
     * Convenient method to register an observer to multiple selections
     *
     * @param observer the observer to be notified
     * @param tags the varying number of selections concerned
     */
    public void addObserver (SelectionObserver observer,
                             SelectionTag... tags)
    {
        for (SelectionTag tag : tags) {
            getSelection(tag)
                .addObserver(observer);
        }
    }

    //----------------//
    // dumpSelections //
    //----------------//
    /**
     * Dump the current state of all selections that depend on this sheet
     */
    public void dumpSelections ()
    {
        System.out.println("\nSelections for sheet : " + sheet.getRadix());

        for (Selection selection : selections.values()) {
            selection.dump();
        }
    }
}
