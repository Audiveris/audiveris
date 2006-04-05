//-----------------------------------------------------------------------//
//                                                                       //
//                       S h e e t A s s e m b l y                       //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.          //
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
import omr.score.PagePoint;
import omr.score.ScoreView;
import omr.sheet.PixelPoint;
import omr.sheet.Scale;
import omr.sheet.Sheet;
import omr.ui.util.Panel;
import omr.ui.view.LogSlider;
import omr.ui.view.PixelObserver;
import omr.ui.view.Rubber;
import omr.ui.view.RubberZoomedPanel;
import omr.ui.view.ScrollView;
import omr.ui.view.Zoom;
import omr.util.Logger;

import java.awt.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import java.awt.event.MouseEvent;

/**
 * Class <code>SheetAssembly</code> is a UI assembly dedicated to the
 * display of one sheet, gathering : <ul>
 *
 * <li>a single {@link omr.score.ScoreView}</li>
 *
 * <li>a {@link Zoom} with its dedicated graphical {@link LogSlider}</li>
 *
 * <li>a mouse adapter {@link Rubber}</li>
 *
 * <li>a tabbed collection of {@link ScrollView}'s for all views of this
 * sheet.
 *
 * </ul><p>Although not part of the same Swing container, the SheetAssembly
 * also refers to a collection of {@link BoardsPane} which is parallel to
 * the collection of ScrollView's.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class SheetAssembly
    implements ChangeListener,
               PixelObserver
{
    //~ Static variables/initializers -------------------------------------

    private static final Logger logger = Logger.getLogger(SheetAssembly.class);

    //~ Instance variables ------------------------------------------------

    // The concrete UI component
    private Panel component;

    // Link with sheet
    private final Sheet sheet;

    // To manually control the zoom ratio
    private final LogSlider slider
        = new LogSlider(2, 5, LogSlider.VERTICAL, -3, 4, 0);

    // Zoom , with default ratio set to 1
    private final Zoom zoom = new Zoom(slider, 1);

    // Mouse adapter
    private final Rubber rubber = new Rubber(zoom);

    // Split pane for score and sheet views
    private final JSplitPane splitPane
        = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

    // Related Score view
    private ScoreView scoreView;

    // Tabbed container for all views of the sheet
    private final JTabbedPane tabbedPane = new JTabbedPane();

    // Map tab title -> BoardsPane
    private final HashMap<String,BoardsPane> paneMap
        = new HashMap<String,BoardsPane>();

    // Map tab component -> ScrollView
    private final HashMap<JScrollPane,ScrollView> viewMap
        = new HashMap<JScrollPane,ScrollView>();

    // Previously selected view
    private JScrollPane previousScrollView;

    // To avoid cycles in forwarding between score and sheet views
    private transient volatile boolean sheetBarred = false;
    private transient volatile boolean scoreBarred = false;

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
        if (logger.isFineEnabled()) {
            logger.fine("creating SheetAssembly on " + sheet);
        }

        component = new Panel();

        // Cross links between sheet and its assembly
        this.sheet = sheet;
        sheet.setAssembly(this);

        // GUI stuff
        slider.setToolTipText("Adjust Zoom Ratio");
        tabbedPane.addChangeListener(this);

        // General layout
        component.setLayout(new BorderLayout());
        component.setNoInsets();
        Panel views = new Panel();
        views.setNoInsets();
        views.setLayout(new BorderLayout());
        views.add(slider, BorderLayout.WEST);
        views.add(tabbedPane, BorderLayout.CENTER);
        splitPane.setBottomComponent(views);
        component.add(splitPane);

        splitPane.setBorder(null);
        splitPane.setDividerSize(2);

        if (logger.isFineEnabled()) {
            logger.fine("SheetAssembly created.");
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

    //--------------//
    // setScoreView //
    //--------------//
    /**
     * Assign the ScoreView part to the assembly
     * @param scoreView the Score View
     */
    public void setScoreView (ScoreView scoreView)
    {
        if (this.scoreView != null) {
            closeScoreView();
        }

        splitPane.setTopComponent(scoreView.getPane().getComponent());
        splitPane.setDividerLocation(150);

        scoreView.getPane().getView().getZoom().fireStateChanged();

        //splitPane.resetToPreferredSizes();
        component.validate();
        component.repaint();
        this.scoreView = scoreView;
    }

    //----------------//
    // closeScoreView //
    //----------------//
    /**
     * Close the score view part
     */
    public void closeScoreView()
    {
        if (scoreView != null) {
            logger.fine("Closing scoreView for " + scoreView.getScore());
            splitPane.setTopComponent(null);
            scoreView = null;
        }
    }

    //------------------//
    // assemblySelected //
    //------------------//
    /**
     * Method called when this sheet assembly is selected (since we can
     * have several sheets displayed, each one with its own sheet
     * assembly). This is called from {@link omr.sheet.SheetController}
     * when the tab of another sheet is selected.
     */
    public void assemblySelected()
    {
        logger.fine("assemblySelected");

        // Display current context
        displayContext();
    }

    //------------//
    // addViewTab //
    //------------//
    /**
     * Add a new tab, that contains a new view on the sheet
     * @param title label to be used for the tab
     * @param sv the view on the sheet
     * @param boardsPane the board pane associated to the tab
     */
    public void addViewTab (String     title,
                            ScrollView sv,
                            BoardsPane boardsPane)
    {
        boardsPane.setName(sheet.getName() + ":" + title);
        if (logger.isFineEnabled()) {
            logger.fine("addViewTab title=" + title +
                         " boardsPane=" + boardsPane);
        }

        // Remember the association between tab and boardsPane
        paneMap.put(title, boardsPane);

        // Make the new view reuse the common zoom and rubber instances
        sv.getView().setZoom(zoom);
        sv.getView().setRubber(rubber);

        // Set the model size
        if (sheet.getWidth() != -1) {
            sv.getView().setModelSize(new Dimension(sheet.getWidth(),
                                                    sheet.getHeight()));
        } else {
            sv.getView().setModelSize(sheet.getPicture().getDimension());
        }

        // Force scroll bar computations
        zoom.fireStateChanged();

        // Add the boardsPane to Jui
        Main.getJui().addBoardsPane(boardsPane);

        // Connect the assembly as a pixel observer of the view, in order to
        // forward such events to the scoreView if relevant.
        sv.getView().addObserver(this);

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

        // Disconnect all keyboard bindings from PixelBoard's (as a
        // workaround for a Swing memory leak)
        for (BoardsPane pane : paneMap.values()) {
            for (Component topComp : pane.getComponent().getComponents()) {
                for (Component comp : ((Container) topComp).getComponents()) {
                    if (comp instanceof JComponent) {
                        ((JComponent) comp).resetKeyboardActions();
                    }
                }
            }
        }

        // Disconnect all boards panes for this assembly
        for (int i = tabbedPane.getTabCount() -1; i >= 0; i--) {
            String title = tabbedPane.getTitleAt(i);
            jui.removeBoardsPane(paneMap.get(title));
            JScrollPane pane = (JScrollPane) tabbedPane.getComponentAt(i);
            viewMap.remove(pane);
            paneMap.remove(title);
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

    //--------------------//
    // setSheetFocusPoint //
    //--------------------//
    /**
     * Called from score view, to forward pixel info to the sheet part
     * @param point The point to focus upon
     */
    public void setSheetFocusPoint (PagePoint point)
    {
        if (!sheetBarred) {
            ScrollView sv = getSelectedView();
            if (sv != null) {
                Point pt = sheet.getScale().unitsToPixels(point, null);
                scoreBarred = true;
                sv.getView().setFocusPoint(pt);
                scoreBarred = false;
            }
        }
    }

    //------------------------//
    // setSheetFocusRectangle //
    //------------------------//
    /**
     * Called from the score view, to forward rectangle info to the sheet part
     * @param rect the rectangle to focus upon
     */
    public void setSheetFocusRectangle (Rectangle rect)
    {
        ScrollView sv = getSelectedView();
        if (sv != null) {
            Rectangle r = sheet.getScale().unitsToPixels(rect, null);
            scoreBarred = true;
            sv.getView().setFocusRectangle(r);
            scoreBarred = false;
        }
    }

    //----------------//
    // displayContext //
    //----------------//
    private void displayContext()
    {
        // Make sure the tab is ready
        int index = tabbedPane.getSelectedIndex();
        if (index == -1) {
            return;
        }

        // Display the proper boards pane
        BoardsPane boardsPane = paneMap.get
            (tabbedPane.getTitleAt(index));
        if (logger.isFineEnabled()) {
            logger.fine("displaying " + boardsPane);
        }
        Main.getJui().showBoardsPane(boardsPane);

        // Display proper view selection if any
        Rectangle rect = rubber.getRectangle();
        if (rect != null) {
            ScrollView scrollView = getSelectedView();
            RubberZoomedPanel view = scrollView.getView();
            view.setFocusRectangle(rect);
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

    //~ Methods for PixelObserver interface --------------------------------

    //--------//
    // update //
    //--------//
    /**
     * Triggered from sheet pixel subject, to notify the new upper left point
     * @param ul The upper left point
     */
    public void update (Point ul)
    {
        // Forward to the score view
        if (scoreView != null && !scoreBarred) {
            sheetBarred = true;
            scoreView.setFocus
                (sheet.getScale().pixelsToUnits(new PixelPoint(ul), null));
            sheetBarred = false;
        }
    }

    //--------//
    // update //
    //--------//
    /**
     * Triggered from sheet pixel subject, to notify new values for point
     * position an pixel level
     * @param ul new value for point
     * @param level new value for pixel level
     */
    public void update (Point ul,
                        int   level)
    {
        // Forward to the score view
        if (scoreView != null && !scoreBarred) {
            sheetBarred = true;
            scoreView.setFocus
                (sheet.getScale().pixelsToUnits(new PixelPoint(ul), null));
            sheetBarred = false;
        }
    }

    //--------//
    // update //
    //--------//
    /**
     * Triggered from sheet pixel subject, to notify new rectangle focus
     * @param rect the new rectangle focus
     */
    public void update (Rectangle rect)
    {
        // Forward to the score view
        if (scoreView != null && !scoreBarred) {
            sheetBarred = true;
            if (rect != null) {
                Point pt = new Point(rect.x + rect.width/2,
                                     rect.y + rect.height/2);
                scoreView.setFocus
                    (sheet.getScale().pixelsToUnits(new PixelPoint(pt), null));
            } else {
                scoreView.setFocus(null);
            }
            sheetBarred = false;
        }
    }
}
