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
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Class <code>SheetAssembly</code> is a UI assembly dedicated to the
 * display of one sheet, gathering a {@link Zoom} with its dedicated
 * graphical {@link LogSlider}, a mouse adapter {@link Rubber}, and a
 * tabbed collection of {@link ScrollView}'s for all views of this sheet.
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class SheetAssembly
    implements ChangeListener
{
    //~ Static variables/initializers -------------------------------------

    private static final Logger logger = Logger.getLogger(SheetAssembly.class);

    //~ Instance variables ------------------------------------------------

    // The concrete UI component
    private JPanel component;

    // Link with sheet
    private final Sheet sheet;

    // To manually control the zoom ratio
    private final LogSlider slider
        = new LogSlider(2, 5, LogSlider.VERTICAL, -3, 4, 0);

    // Zoom , with default ratio set to 1
    private final Zoom zoom = new Zoom(slider, 1);

    // Mouse adapter
    private final Rubber rubber = new Rubber(zoom);

    // Tabbed container for all views of the sheet
    private final JTabbedPane tabbedPane = new JTabbedPane();

    // Map tab title -> BoardsPane
    private final HashMap<String,BoardsPane> map
        = new HashMap<String,BoardsPane>();

    // Map tab component -> ScrollView
    private final HashMap<JScrollPane,ScrollView> viewMap
        = new HashMap<JScrollPane,ScrollView>();

    // Previously selected view
    protected JScrollPane previousScrollView;

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

        component = new JPanel();

        // Cross links between sheet and its assembly
        this.sheet = sheet;
        sheet.setAssembly(this);

        // GUI stuff
        slider.setToolTipText("Adjust Zoom Ratio");
        tabbedPane.addChangeListener(this);

        // General layout
        component.setLayout(new BorderLayout());
        component.add(slider, BorderLayout.WEST);
        component.add(tabbedPane, BorderLayout.CENTER);

        if (logger.isDebugEnabled()) {
            logger.debug("SheetAssembly created.");
        }
    }

    //~ Methods -----------------------------------------------------------

    //--------------//
    // getComponent //
    //--------------//
    /**
     * Report the UI component
     *
     * @return the concrete component
     */
    public JComponent getComponent()
    {
        return component;
    }

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

        // Display current context
        displayContext();
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
        boardsPane.setName(sheet.getName() + ":" + title);

        if (logger.isDebugEnabled()) {
            logger.debug("addViewTab title=" + title +
                         " boardsPane=" + boardsPane);
        }

        // Remember the association between tab and boardsPane
        map.put(title, boardsPane);

        sv.setZoom(zoom);
        sv.getView().setRubber(rubber);

        if (sheet.getWidth() != -1) {
            sv.getView().setModelSize(new Dimension(sheet.getWidth(),
                                                    sheet.getHeight()));
        } else {
            sv.getView().setModelSize(sheet.getPicture().getDimension());
        }

        // Force scroll bar computations
        zoom.forceRatio(zoom.getRatio());

        // Forward mouse actions to this entity
        rubber.setMouseMonitor(sv.getView());

        // Add the boardsPane to Jui
        Main.getJui().addBoardsPane(boardsPane);

        // Insert the scrollView as a new tab
        viewMap.put(sv.getComponent(), sv);
        tabbedPane.addTab(title, sv.getComponent());
        tabbedPane.setSelectedComponent(sv.getComponent());
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
        JScrollPane pane = (JScrollPane) tabbedPane.getSelectedComponent();
        return viewMap.get(pane);
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
        Jui jui = Main.getJui();

        jui.sheetPane.close(this);

        // Disconnect all boards panes for this assembly
        for (int i = tabbedPane.getTabCount() -1; i >= 0; i--) {
            String title = tabbedPane.getTitleAt(i);
            jui.removeBoardsPane(map.get(title));
            JScrollPane pane = (JScrollPane) tabbedPane.getComponentAt(i);
            viewMap.remove(pane);
            map.remove(title);
        }
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

    //----------------//
    // displayContext //
    //----------------//
    private void displayContext()
    {
        logger.debug("displayContext");

        // Make sure the tab is ready
        int index = tabbedPane.getSelectedIndex();
        if (index == -1) {
            return;
        }

        // Display the proper boards pane
        BoardsPane boardsPane = map.get
            (tabbedPane.getTitleAt(index));
        if (logger.isDebugEnabled()) {
            logger.debug("displaying " + boardsPane);
        }
        Main.getJui().showBoardsPane(boardsPane);

        // Display proper view selection if any
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

    //--------------//
    // stateChanged //
    //--------------//
    /**
     * This method is called whenever a view tab is selected in the Sheet
     * Assembly.
     *
     * @param e the originating change event
     */
    public void stateChanged (ChangeEvent e)
    {
        logger.debug("stateChanged");
        ScrollView scrollView = getSelectedView();
        RubberZoomedPanel view = scrollView.getView();

        // Link rubber with proper view
        rubber.setComponent(view);
        rubber.setMouseMonitor(view);

        // Keep previous scroll bar positions
        if (previousScrollView != null) {
            scrollView.getComponent().getVerticalScrollBar().setValue
                (previousScrollView.getVerticalScrollBar().getValue());
            scrollView.getComponent().getHorizontalScrollBar().setValue
                (previousScrollView.getHorizontalScrollBar().getValue());
        }

        // Restore display of proper context
        displayContext();

        previousScrollView = scrollView.getComponent();
    }
}
