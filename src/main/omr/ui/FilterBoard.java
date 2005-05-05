//-----------------------------------------------------------------------//
//                                                                       //
//                         F i l t e r B o a r d                         //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2005. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//
//      $Id$
package omr.ui;

import omr.stick.FilterMonitor;

import com.jgoodies.forms.builder.*;
import com.jgoodies.forms.layout.*;

// import javax.swing.BorderFactory;
import javax.swing.JEditorPane;

/**
 * Class <code>FilterBoard</code> defines a board dedicated to the display of
 * HTML-coded filter information.
 */
public class FilterBoard
    extends Board
    implements FilterMonitor
{
    //~ Instance variables ------------------------------------------------

    // For display of filter suite results
    private JEditorPane filterPane;

    //~ Constructors ------------------------------------------------------

    //------------//
    // FilterBoard //
    //------------//
    /**
     * Create a Filter Board
     */
    public FilterBoard ()
    {
        super(Board.Tag.FILTER);
        filterPane = new JEditorPane("text/html", "");
        filterPane.setEditable(false);

        defineLayout();
    }

    //~ Methods -----------------------------------------------------------

    //--------------//
    // defineLayout //
    //--------------//
    private void defineLayout()
    {
        FormLayout layout = new FormLayout("pref", "pref,pref,pref");
        PanelBuilder builder = new PanelBuilder(layout, this);
        builder.setDefaultDialogBorder();
        CellConstraints cst = new CellConstraints();

        int r = 1;                      // --------------------------------
        builder.addSeparator("Filter",   cst.xy(1,  r));

        r += 2;                         // --------------------------------
        builder.add(filterPane,         cst.xy (1,  r));
    }

//     //----------//
//     // setTitle //
//     //----------//
//     public void setTitle (String title)
//     {
//         setBorder(BorderFactory.createTitledBorder(title));
//     }

    //----------//
    // tellHtml //
    //----------//
    /**
     * Render the Html stream
     *
     * @param html the html source to be displayed
     */
    public void tellHtml (String html)
    {
        if (html != null) {
            setVisible(true);
            filterPane.setText(html);
        } else {
            setVisible(false);
        }
    }
}
