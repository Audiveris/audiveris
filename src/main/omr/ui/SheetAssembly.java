//-----------------------------------------------------------------------//
//                                                                       //
//                       S h e e t A s s e m b l y                       //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2005. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.ui;

import omr.Main;
import omr.glyph.Glyph;
import omr.glyph.GlyphFocus;
import omr.glyph.GlyphSection;
import omr.lag.LagView;
import omr.lag.Run;
import omr.lag.Section;
import omr.sheet.PictureView;
import omr.sheet.Sheet;
import omr.util.Logger;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Class <code>SheetAssembly</code> is a UI assembly dedicated to the
 * display of one sheet, gathering a {@link Zoom}, a related {@link
 * LogSlider}, a mouse {@link Rubber}, and a tabbed collection of {@link
 * ScrollView}'s for all views of this sheet.
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class SheetAssembly
    extends JPanel
{
    //~ Static variables/initializers -------------------------------------

    private static final Logger logger = Logger.getLogger(SheetAssembly.class);

    //~ Instance variables ------------------------------------------------

    // Link with sheet
    private final Sheet sheet;

    private final MyTabbedPane tabbedPane;
    private final LogSlider    slider;
    private final Zoom         zoom;
    private final Rubber       rubber;

    // Map to retrieve the boardspane related to a tab
    private final Map<Integer, BoardsPane> boardsPaneMap;

    //~ Constructors ------------------------------------------------------

    //---------------//
    // SheetAssembly //
    //---------------//
    /**
     * Create a new <code>SheetAssembly</code> instance, dedicated to one
     * sheet.
     *
     * @param sheet the related sheet
     */
    public SheetAssembly (Sheet sheet)
    {
        if (logger.isDebugEnabled()) {
            logger.debug("creating SheetAssembly on " + sheet);
        }

        // Cross links between sheet and its assembly
        this.sheet = sheet;
        sheet.setAssembly(this);

        // Allocate all surrounding entities
        slider = new LogSlider(2, 5, LogSlider.VERTICAL, -3, 4, 0);
        slider.setToolTipText("Adjust Zoom Ratio");

        zoom = new Zoom(slider, 1);     // Default ratio set to 1

        rubber = new Rubber(zoom);

        // Tabbed container for all views of the sheet
        tabbedPane = new MyTabbedPane();

        // General layout
        setLayout(new BorderLayout());
        add(slider, BorderLayout.WEST);
        add(tabbedPane, BorderLayout.CENTER);

        // Map of boardspanes
        boardsPaneMap = new HashMap<Integer, BoardsPane>();
    }

    //~ Methods -----------------------------------------------------------

    //------------------//
    // assemblySelected //
    //------------------//
    /**
     * Method called when this sheet assembly is selected (since we can
     * have several sheets displayed, each one with its own sheet
     * assembly). This is called from {@link SheetPane} when the tab of
     * another sheet is selected.
     */
    public void assemblySelected()
    {
        logger.debug("assemblySelected");
        // Update boards according to current rubber state
        updateBoards();
    }

    //------------//
    // addViewTab //
    //------------//
    /**
     * Add a new tab, that contains a new view on the sheet
     *
     * @param title label to be used for the tab
     * @param sv the view on the sheet
     */
    public void addViewTab (String     title,
                            ScrollView sv,
                            BoardsPane boardsPane)
    {
        if (logger.isDebugEnabled()) {
            logger.debug("addViewTab title=" + title + " sv=" + sv
                         + " boardsPane=" + boardsPane);
        }

        sv.setZoom(zoom);
        sv.getView().setRubber(rubber);

        if (sheet.getWidth() != -1) {
            sv.getView().setModelSize(new Dimension(sheet.getWidth(),
                                                    sheet.getHeight()));
        } else {
            sv.getView().setModelSize(sheet.getPicture().getDimension());
        }

        // Register the related boardspane
        boardsPaneMap.put(tabbedPane.getTabCount(), boardsPane);

        // Force scroll bar computations
        zoom.forceRatio(zoom.getRatio());

        // Forward mouse actions to this entity
        rubber.setMouseMonitor(sv.getView());

        // Insert the scrollView as a new tab
        tabbedPane.addTab(title, sv);
        tabbedPane.setSelectedComponent(sv);
    }

    //-----------------//
    // getSelectedView //
    //-----------------//
    /**
     * Report the tabbed view currently selected
     *
     * @return the current tabbed view
     */
    public ScrollView getSelectedView()
    {
        return (ScrollView) tabbedPane.getSelectedComponent();
    }

    //----------//
    // getSheet //
    //----------//
    /**
     * Report the sheet this assembly is related to
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
     * Close the assembly, by removing it from the containing sheet tabbed
     * pane.
     */
    public void close ()
    {
        Main.getJui().sheetPane.close(this);

        // Disconnect from boards
        Main.getJui().setBoardsPane(null);
    }

    //--------------//
    // setZoomRatio //
    //--------------//
    /**
     * Modify the ratio of the global zoom for all views of the sheet
     *
     * @param ratio the new display ratio
     */
    public void setZoomRatio(double ratio)
    {
        zoom.setRatio(ratio);
    }

    //--------------//
    // updateBoards //
    //--------------//
    private void updateBoards()
    {
        logger.debug("updateBoards");
        Jui jui = Main.getJui();

        BoardsPane boardsPane = boardsPaneMap.get(tabbedPane.getSelectedIndex());
        jui.setBoardsPane(boardsPane);

        Rectangle rect = rubber.getRectangle();
        if (rect != null) {
            ScrollView scrollView = getSelectedView();
            RubberZoomedPanel view = scrollView.getView();

            if (rect.width != 0 || rect.height != 0) {
                view.rectangleSelected(null, rect);
            } else {
                view.pointSelected(null, rect.getLocation());
            }
        }
    }

    //~ Classes -----------------------------------------------------------

    //--------------//
    // MyTabbedPane //
    //--------------//
    private class MyTabbedPane
        extends JTabbedPane
        implements ChangeListener
    {
        //~ Instance variables --------------------------------------------

        protected ScrollView previousScrollView;

        //~ Constructors --------------------------------------------------

        public MyTabbedPane ()
        {
            addChangeListener(this);
        }

        //~ Methods -------------------------------------------------------

        //--------------//
        // stateChanged //
        //--------------//
        /**
         * This method is called whenever a view tab is selected in the
         * Sheet Assembly.
         *
         * @param e the originating change event
         */
        public void stateChanged (ChangeEvent e)
        {
            logger.debug("stateChanged");
            ScrollView scrollView = getSelectedView();
            RubberZoomedPanel view = scrollView.getView();

            // Link rubber with right view
            rubber.setComponent(view);
            rubber.setMouseMonitor(view);

            // Keep previous scroll bar positions
            if (previousScrollView != null) {
                scrollView.getVerticalScrollBar().setValue
                    (previousScrollView.getVerticalScrollBar().getValue());
                scrollView.getHorizontalScrollBar().setValue
                    (previousScrollView.getHorizontalScrollBar().getValue());
            }

            // Restore proper boardspane
            updateBoards();

            previousScrollView = scrollView;
        }
    }
}
