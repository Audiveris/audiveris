//----------------------------------------------------------------------------//
//                                                                            //
//                        S h e e t D e p e n d e n t                         //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.ui;

import omr.selection.Selection;
import omr.selection.SelectionHint;
import omr.selection.SelectionObserver;
import omr.selection.SelectionTag;

import omr.sheet.SheetManager;

import omr.util.Implement;
import omr.util.Logger;

import org.jdesktop.application.AbstractBean;

/**
 * Class <code>SheetDependent</code> handles the dependency on sheet
 * availability
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public abstract class SheetDependent
    extends AbstractBean
    implements SelectionObserver
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(SheetDependent.class);

    //~ Instance fields --------------------------------------------------------

    /** Indicates whether there is a current sheet */
    protected boolean sheetAvailable = false;

    //~ Constructors -----------------------------------------------------------

    //----------------//
    // SheetDependent //
    //----------------//
    /**
     * Creates a new SheetDependent object.
     */
    protected SheetDependent ()
    {
        // Stay informed on sheet status, in order to enable sheet-dependent
        // actions accordingly
        SheetManager.getSelection()
                    .addObserver(this);
    }

    //~ Methods ----------------------------------------------------------------

    //---------//
    // getName //
    //---------//
    /**
     * {@inheritDoc}
     */
    @Implement(SelectionObserver.class)
    public String getName ()
    {
        return "SheetDependent";
    }

    //-------------------//
    // setSheetAvailable //
    //-------------------//
    /**
     * Setter for sheetAvailable property
     * @param sheetAvailable the new property value
     */
    public void setSheetAvailable (boolean sheetAvailable)
    {
        boolean oldValue = this.sheetAvailable;
        this.sheetAvailable = sheetAvailable;
        firePropertyChange("sheetAvailable", oldValue, this.sheetAvailable);
    }

    //------------------//
    // isSheetAvailable //
    //------------------//
    /**
     * Getter for sheetAvailable property
     * @return the current property value
     */
    public boolean isSheetAvailable ()
    {
        return sheetAvailable;
    }

    //--------//
    // update //
    //--------//
    /**
     * Notification of sheet selection
     *
     * @param selection the selection object (SHEET)
     * @param hint processing hint (not used)
     */
    @Implement(SelectionObserver.class)
    public void update (Selection     selection,
                        SelectionHint hint)
    {
        if (selection.getTag() == SelectionTag.SHEET) {
            setSheetAvailable(selection.getEntity() != null);
        }
    }
}
