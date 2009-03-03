//-----------------------------------------------------------------------//
//                                                                       //
//                        S y m b o l R i p p e r                        //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//
package omr.ui.icon;

import omr.Main;

import omr.glyph.Shape;

import omr.log.Logger;

import omr.ui.field.IntegerListSpinner;
import omr.ui.field.LField;
import omr.ui.field.LIntegerSpinner;
import omr.ui.field.LSpinner;
import omr.ui.field.SpinnerUtilities;
import omr.ui.util.Panel;
import omr.ui.util.UILookAndFeel;

import com.jgoodies.forms.builder.*;
import com.jgoodies.forms.layout.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.*;

import javax.swing.*;
import javax.swing.event.*;

/**
 * Class <code>SymbolRipper</code> is a stand-alone utility to generate the
 * textual description of a symbol. Symbol appearance is "ripped" from a musical
 * font.
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class SymbolRipper
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(SymbolRipper.class);

    static {
        // UI Look and Feel
        UILookAndFeel.setUI(null);
    }

    //~ Instance fields --------------------------------------------------------

    /** Related frame */
    private final JFrame frame;

    /** Drawing transform */
    private AffineTransform scaleXform;

    /** Image being built */
    private BufferedImage image;

    //---------------//
    // paramListener //
    //---------------//
    private ChangeListener     paramListener = new ChangeListener() {
        public void stateChanged (ChangeEvent e)
        {
            JSpinner s = (JSpinner) e.getSource();

            if (s == fontName.getSpinner()) {
                // New font name
                defineFont();
            } else {
                if (s == fontBase) {
                    // New font base
                    changeCode();
                } else if (s == fontSize.getSpinner()) {
                    // New font size
                    defineFont();
                } else if (s == pointCode.getSpinner()) {
                    // New point code
                    changeCode();
                } else if (s == scale.getSpinner()) {
                    // New drawing scale
                    scaleXform = AffineTransform.getScaleInstance(
                        (double) scale.getValue(),
                        (double) scale.getValue());
                    resizeDrawing();
                } else if ((s == xOffset.getSpinner()) ||
                           (s == yOffset.getSpinner())) {
                    // New drawing offset
                } else if ((s == width.getSpinner()) ||
                           (s == height.getSpinner())) {
                    // New drawing dimension
                    resizeDrawing();
                }
            }

            // For all
            image = buildImage();
            frame.repaint();
        }
    };

    // Current music font
    private Font               musicFont;

    // Font base
    private IntegerListSpinner fontBase = new IntegerListSpinner();
    private JButton            shapeButton;
    private JPopupMenu         menu = new JPopupMenu();

    // Hexa representation
    private LField          hexaCode = new LField(
        false,
        "Hexa",
        "Hexa value of the point code");

    // File name for output
    private LField          output = new LField(
        "File",
        "Name of the output file");

    // Font size
    private LIntegerSpinner fontSize = new LIntegerSpinner(
        "Size",
        "Font size in picas");

    // Height
    private LIntegerSpinner height = new LIntegerSpinner(
        "Height",
        "Drawing Height");

    // Point code
    private LIntegerSpinner pointCode = new LIntegerSpinner(
        "Code",
        "Point code");

    // Scale
    private LIntegerSpinner scale = new LIntegerSpinner(
        "Scale",
        "Scaling ratio");

    // Width
    private LIntegerSpinner width = new LIntegerSpinner(
        "Width",
        "Drawing Width");

    // X Offset
    private LIntegerSpinner xOffset = new LIntegerSpinner(
        "xOffset",
        "X offset");

    // Y Offset
    private LIntegerSpinner yOffset = new LIntegerSpinner(
        "yOffset",
        "Y offset");

    // Font name
    private LSpinner       fontName = new LSpinner("Font", "Name of the font");

    // Button for reloading the shape icons
    private ReloadAction   reloadAction = new ReloadAction();
    private JButton        reloadButton = new JButton(reloadAction);

    // Related shape
    private ShapeAction    shapeAction = new ShapeAction();

    // Button for storing the result
    private StoreAction    storeAction = new StoreAction();

    //---------------//
    // shapeListener //
    //---------------//
    private ActionListener shapeListener = new ActionListener() {
        // Called when a shape has been selected
        public void actionPerformed (ActionEvent e)
        {
            JMenuItem source = (JMenuItem) e.getSource();
            Shape     current = Shape.valueOf(source.getText());

            if (current != null) {
                shapeButton.setText(current.toString());
                output.setText(current.toString());
                storeAction.setEnabled(true);
            } else {
                disableStore();
            }
        }
    };

    private JButton storeButton = new JButton(storeAction);

    // Panel where the icon is drawn
    private JPanel drawing;

    // String used to draw the symbol
    private String string;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new SymbolRipper object.
     */
    public SymbolRipper ()
    {
        // Related frame
        frame = new JFrame();
        frame.setTitle("Symbol Ripper");

        shapeButton = new JButton(shapeAction);

        // Actors
        drawing = new Drawing();

        fontBase.setModel(new SpinnerListModel(new Integer[] { 0, 0xf000 }));
        SpinnerUtilities.setRightAlignment(fontBase);
        SpinnerUtilities.fixIntegerList(fontBase);

        fontName.setModel(
            new SpinnerListModel(
                GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));

        // Initial values
        //        fontName.getSpinner()
        //                .setValue("Maestro");
        fontSize.setValue(31);
        pointCode.setModel(new SpinnerNumberModel(38, 0, 255, 1));
        width.setValue(23);
        height.setValue(63);
        xOffset.setValue(0);
        yOffset.setValue(40);
        scale.setValue(5);
        scaleXform = AffineTransform.getScaleInstance(
            (double) scale.getValue(),
            (double) scale.getValue());
        changeCode();
        defineFont();

        // Listeners
        fontName.addChangeListener(paramListener);
        fontBase.addChangeListener(paramListener);
        fontSize.addChangeListener(paramListener);
        pointCode.addChangeListener(paramListener);
        scale.addChangeListener(paramListener);
        xOffset.addChangeListener(paramListener);
        yOffset.addChangeListener(paramListener);
        width.addChangeListener(paramListener);
        height.addChangeListener(paramListener);

        storeAction.setEnabled(false);

        // Populate with defined symbols
        reloadAction.actionPerformed(null);

        // Global layout
        defineLayout();

        // Frame behavior
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setBounds(100, 100, 500, 500);
        frame.setVisible(true);

        // Actions
        image = buildImage();
        frame.repaint();
    }

    //~ Methods ----------------------------------------------------------------

    //------//
    // main //
    //------//
    /**
     * Command line entry point, no arguments are used today.
     */
    public static void main (String... args)
    {
        // Make sure the environment variable is set
        Main.getIconsFolder();

        new SymbolRipper();
    }

    //----------//
    // getFrame //
    //----------//
    /**
     * Report the related User Interface frame of this entity
     *
     * @return the window frame
     */
    public JFrame getFrame ()
    {
        return frame;
    }

    //---------------//
    // getParamPanel //
    //---------------//
    private JPanel getParamPanel ()
    {
        FormLayout   layout = Panel.makeFormLayout(12, 2);

        PanelBuilder builder = new PanelBuilder(layout, new Panel());
        builder.setDefaultDialogBorder();

        CellConstraints cst = new CellConstraints();

        int             r = 1; // --------------------------------
        builder.addSeparator("Font", cst.xyw(1, r, 7));

        r += 2; // --------------------------------
        builder.add(fontName.getLabel(), cst.xy(1, r));
        builder.add(fontName.getSpinner(), cst.xyw(3, r, 5));

        r += 2; // --------------------------------
        builder.addLabel("Base", cst.xy(1, r));
        builder.add(fontBase, cst.xy(3, r));

        builder.add(fontSize.getLabel(), cst.xy(5, r));
        builder.add(fontSize.getSpinner(), cst.xy(7, r));

        r += 2; // --------------------------------
        builder.add(pointCode.getLabel(), cst.xy(1, r));
        builder.add(pointCode.getSpinner(), cst.xy(3, r));

        builder.add(hexaCode.getLabel(), cst.xy(5, r));
        builder.add(hexaCode.getField(), cst.xy(7, r));

        r += 2; // --------------------------------
        builder.addSeparator("Drawing", cst.xyw(1, r, 7));

        r += 2; // --------------------------------
        builder.add(scale.getLabel(), cst.xy(5, r));
        builder.add(scale.getSpinner(), cst.xy(7, r));

        r += 2; // --------------------------------
        builder.add(xOffset.getLabel(), cst.xy(1, r));
        builder.add(xOffset.getSpinner(), cst.xy(3, r));

        builder.add(width.getLabel(), cst.xy(5, r));
        builder.add(width.getSpinner(), cst.xy(7, r));

        r += 2; // --------------------------------
        builder.add(yOffset.getLabel(), cst.xy(1, r));
        builder.add(yOffset.getSpinner(), cst.xy(3, r));

        builder.add(height.getLabel(), cst.xy(5, r));
        builder.add(height.getSpinner(), cst.xy(7, r));

        r += 2; // --------------------------------
        builder.addSeparator("Output", cst.xyw(1, r, 7));

        r += 2; // --------------------------------
        builder.add(shapeButton, cst.xyw(3, r, 5));

        r += 2; // --------------------------------
        builder.add(output.getLabel(), cst.xy(1, r));
        builder.add(output.getField(), cst.xyw(3, r, 5));

        r += 2; // --------------------------------
        builder.add(reloadButton, cst.xy(3, r));
        builder.add(storeButton, cst.xy(7, r));

        return builder.getPanel();
    }

    //------------//
    // buildImage //
    //------------//
    private BufferedImage buildImage ()
    {
        BufferedImage image = (BufferedImage) drawing.createImage(
            width.getValue(),
            height.getValue());

        Graphics2D    g2 = image.createGraphics();
        g2.setBackground(Color.white);
        g2.setColor(Color.white);
        g2.fillRect(0, 0, width.getValue(), height.getValue());
        g2.setRenderingHint(
            RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(Color.black);
        g2.setFont(musicFont);
        g2.drawString(string, xOffset.getValue(), yOffset.getValue());

        return image;
    }

    //------------//
    // changeCode //
    //------------//
    private void changeCode ()
    {
        int base = (Integer) fontBase.getValue();
        int code = (Integer) pointCode.getSpinner()
                                      .getValue();
        hexaCode.setText(Integer.toString(base + code, 16));
        string = new String(Character.toChars(base + code));
        disableStore();
    }

    //------------//
    // defineFont //
    //------------//
    private void defineFont ()
    {
        String name = (String) fontName.getSpinner()
                                       .getValue();
        int    val = fontSize.getValue();
        musicFont = new Font(name, Font.PLAIN, val);
    }

    //--------------//
    // defineLayout //
    //--------------//
    private void defineLayout ()
    {
        Container pane = frame.getContentPane();
        pane.setLayout(new BorderLayout());

        pane.add(getParamPanel(), BorderLayout.WEST);

        resizeDrawing();

        JScrollPane scrollPane = new JScrollPane(drawing);
        pane.add(scrollPane, BorderLayout.CENTER);
    }

    //--------------//
    // disableStore //
    //--------------//
    private void disableStore ()
    {
        output.setText(""); // Safer
        shapeButton.setText("Shape");
        storeAction.setEnabled(false);
    }

    //---------------//
    // resizeDrawing //
    //---------------//
    private void resizeDrawing ()
    {
        drawing.setPreferredSize(
            new Dimension(
                width.getValue() * scale.getValue(),
                height.getValue() * scale.getValue()));
        drawing.revalidate();
    }

    //~ Inner Classes ----------------------------------------------------------

    //---------//
    // Drawing //
    //---------//
    private class Drawing
        extends Panel
    {
        //~ Methods ------------------------------------------------------------

        @Override
        public void paintComponent (Graphics g)
        {
            // For background
            super.paintComponent(g);

            // Meant for visual check
            if (image != null) {
                Graphics2D g2 = (Graphics2D) g;

                g2.drawImage(image, scaleXform, this);
                //g2.drawImage (image, 1, 1, this);
                g.setColor(Color.red);
                g.drawRect(
                    0,
                    0,
                    width.getValue() * scale.getValue(),
                    height.getValue() * scale.getValue());
            }
        }
    }

    //--------------//
    // ReloadAction //
    //--------------//
    private class ReloadAction
        extends AbstractAction
    {
        //~ Constructors -------------------------------------------------------

        public ReloadAction ()
        {
            super("Reload");
        }

        //~ Methods ------------------------------------------------------------

        public void actionPerformed (ActionEvent e)
        {
            // Populate with defined symbols
            for (Shape shape : Shape.values()) {
                shape.setIcon(
                    IconManager.getInstance().loadSymbolIcon(shape.toString()));
            }

            // Update the shape menu accordingly
            menu.removeAll();
            Shape.addShapeItems(menu, shapeListener);
        }
    }

    //-------------//
    // ShapeAction //
    //-------------//
    private class ShapeAction
        extends AbstractAction
    {
        //~ Constructors -------------------------------------------------------

        public ShapeAction ()
        {
            super("Shape");
        }

        //~ Methods ------------------------------------------------------------

        public void actionPerformed (ActionEvent e)
        {
            JButton button = (JButton) e.getSource();
            menu.show(button, 0, 0);
        }
    }

    //-------------//
    // StoreAction //
    //-------------//
    private class StoreAction
        extends AbstractAction
    {
        //~ Constructors -------------------------------------------------------

        public StoreAction ()
        {
            super("Store");
        }

        //~ Methods ------------------------------------------------------------

        public void actionPerformed (ActionEvent e)
        {
            if (!output.getText()
                       .equals("")) {
                // Store the new icon definition
                SymbolIcon icon = new SymbolIcon(image);
                icon.setName(output.getText());
                IconManager.getInstance()
                           .storeSymbolIcon(icon);

                // Try to load this new icon definition as a shape icon
                try {
                    // This may fail
                    Shape shape = Shape.valueOf(output.getText());
                    shape.setIcon(
                        IconManager.getInstance().loadSymbolIcon(
                            shape.toString()));

                    // Update the shape menu accordingly
                    menu.removeAll();
                    Shape.addShapeItems(menu, shapeListener);
                } catch (Exception ex) {
                    logger.info("Symbol just stored is not a known shape");
                }

                // Disable new storing
                disableStore();
            } else {
                logger.warning("No name defined for symbol output");
            }
        }
    }
}
