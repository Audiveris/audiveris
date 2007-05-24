//----------------------------------------------------------------------------//
//                                                                            //
//                          E r r o r s E d i t o r                           //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.ui;

import omr.score.ScoreNode;

import omr.sheet.Sheet;

import omr.util.Logger;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 * Class <code>ErrorsEditor</code> handles the set of error messages
 * recorded during the translation from sheet to score, allowing the user
 * to interactively browse the errors and go to the related locations in
 * the sheet and score views.
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class ErrorsEditor
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(ErrorsEditor.class);

    //~ Instance fields --------------------------------------------------------

    /** Related score */
    //////////////////private final Score score;

    /** Related sheet */
    private final Sheet sheet;

    /** The list of displayed errors */
    private JList list;

    /** The scrolling area */
    private JScrollPane scrollPane;

    /** Selection listener */
    ListSelectionListener listener = new ListSelectionListener() {
        public void valueChanged (ListSelectionEvent e)
        {
            if (!e.getValueIsAdjusting()) {
                logger.info("value=" + list.getSelectedValue());
            }
        }
    };

    //~ Constructors -----------------------------------------------------------

    //-------------//
    // ErrorEditor //
    //-------------//
    /**
     * Create an instance of ErrorEditor (one per sheet / score)
     *
     * @param sheet the related sheet
     */
    public ErrorsEditor (Sheet sheet)
    {
        this.sheet = sheet;
        list = new JList(
            new Object[] {
                sheet.getRadix(), "One", "Two", "Three", "Four", "Five", "Six",
                "Seven", "Eight"
            });
        scrollPane = new JScrollPane(list);
        list.setPrototypeCellValue("--------------------------------------");

        list.addListSelectionListener(listener);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    }

    //~ Methods ----------------------------------------------------------------

    //----------//
    // addError //
    //----------//
    public void addError (ScoreNode container,
                          Object    entity,
                          String    text)
    {
    }

    //--------------//
    // getComponent //
    //--------------//
    /**
     * Give access to the real component
     *
     * @return the concrete component
     */
    public JComponent getComponent ()
    {
        return scrollPane;
    }
}
