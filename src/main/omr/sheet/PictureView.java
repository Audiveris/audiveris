//-----------------------------------------------------------------------//
//                                                                       //
//                         P i c t u r e V i e w                         //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2005. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.sheet;

import omr.score.PagePoint;
import omr.ui.ScrollView;
import omr.ui.Rubber;
import omr.ui.RubberZoomedPanel;
import omr.ui.Zoom;
import omr.util.*;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * Class <code>PictureView</code> defines the view dedicated to the display
 * of the picture bitmap of a music sheet.
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class PictureView
    extends ScrollView
{
    //~ Static variables/initializers -------------------------------------

    private static final Logger logger = Logger.getLogger(PictureView.class);

    //~ Instance variables ------------------------------------------------

    // The displayed view
    private final MyView view;

    // Link with sheet
    private final Sheet sheet;

    //~ Constructors ------------------------------------------------------

    //-------------//
    // PictureView //
    //-------------//
    /**
     * Create a new <code>PictureView</code> instance, dedicated to a
     * sheet.
     *
     * @param sheet the related sheet
     */
    public PictureView (Sheet sheet)
    {
        view = new MyView();

        if (logger.isDebugEnabled()) {
            logger.debug("creating PictureView on " + sheet);
        }

        this.sheet = sheet;

        // Insert view
        setView(view);

        if (logger.isDebugEnabled()) {
            logger.debug("PictureView ready");
        }
    }

    //~ Methods -----------------------------------------------------------

    //---------//
    // getView //
    //---------//
    public MyView getView()
    {
        return view;
    }

    //----------//
    // getSheet //
    //----------//
    /**
     * Report the sheet this view is related to
     *
     * @return the related sheet
     */
    public Sheet getSheet ()
    {
        return sheet;
    }

    //-------//
    // close //
    //-------//
    /**
     * Close the view, by removing it from the containing sheet tabbed
     * pane.
     */
    public void close ()
    {
        ///Main.getJui().sheetPane.close(this);
    }

    //------------------//
    // showRelatedScore //
    //------------------//
    /**
     * If the sheet-related score exists, then display the score view in
     * the score display. Focus is determined as the corresponding point in
     * score/score display as the provided image point (or of the current
     * sheet image point if a null image point is provided)
     */
    public void showRelatedScore ()
    {
//         // Do we have the corresponding score ?
//         final Score score = sheet.getScore();

//         if (logger.isDebugEnabled()) {
//             logger.debug("showRelatedScore: " + score);
//         }

//         if (score != null) {
//             Main.getJui().scorePane.showScoreView(score.getView());
//         } else {
//             Main.getJui().scorePane.showScoreView(null);
//         }
    }

    //----------//
    // toString //
    //----------//
    /**
     * A readable description of this view
     *
     * @return a name based on the sheet this view is dedicated to
     */
    @Override
    public String toString ()
    {
        return "{PictureView " + sheet.getPath() + "}";
    }

    //----------//
    // getPixel //
    //----------//
    /**
     * Report the pixel level at the designated point.
     *
     * @param pt the designated point
     * @return the pixel level (0->255) or -1 if info is not available, for
     * example because the designated point is outside the image boundaries
     */
    @Override
    public int getPixel (Point pt)
    {
        // Make sure the picture is available
        Picture picture = sheet.getPicture();

        // Check that we are not pointing outside the image
        if ((pt.x < picture.getWidth()) && (pt.y < picture.getHeight())) {
            return picture.getPixel(pt.x, pt.y);
        } else {
            return -1;
        }
    }

    //~ Classes -----------------------------------------------------------

    //--------//
    // MyView //
    //--------//
    private class MyView
        extends RubberZoomedPanel
    {
        //~ Methods -------------------------------------------------------

        //--------//
        // render //
        //--------//
        @Override
        public void render (Graphics g)
        {
            // Render the picture image
            sheet.getPicture().render(g, getZoom().getRatio());
        }

        //--------------//
        // pointUpdated //
        //--------------//
        @Override
        public void pointUpdated (MouseEvent e,
                                  Point pt)
        {
            // We use a specific version which displays the pixel level
            notifyObservers(pt, getPixel(pt));
        }
     }
}
