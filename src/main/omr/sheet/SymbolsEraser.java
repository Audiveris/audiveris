//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    S y m b o l s E r a s e r                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.GlyphLayer;
import omr.glyph.Shape;
import omr.glyph.facets.Glyph;

import omr.image.ShapeDescriptor;
import omr.image.Template;

import omr.lag.JunctionRatioPolicy;
import omr.lag.Lag;
import omr.lag.Lags;
import omr.lag.Section;
import omr.lag.SectionsBuilder;

import omr.run.RunsTable;
import omr.run.RunsTableFactory;
import static omr.sheet.SymbolsFilter.SYMBOL_ORIENTATION;

import omr.sig.AbstractNoteInter;
import omr.sig.Inter;
import omr.sig.SIGraph;
import omr.sig.StemInter;

import ij.process.ByteProcessor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Class {@code SymbolsEraser} erases shapes and glyphs to prepare symbols retrieval.
 * <ul>
 * <li>All the "strong" instances of relevant shapes are always erased.</li>
 * <li>The "weak" ones area also erased but saved apart as optional glyphs.
 * Doing so, the symbols builder will be able to try all combinations with, as well as without,
 * these optional weak glyphs.</li>
 * </ul>
 *
 * @author Hervé Bitteur
 */
public class SymbolsEraser
        extends PageEraser
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(
            SymbolsEraser.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Lag used by optional sections. */
    private final Lag symLag;

    /** Map system -> list of weak glyphs. */
    private final Map<SystemInfo, List<Glyph>> weaksMap = new TreeMap<SystemInfo, List<Glyph>>();

    /**
     * Current system list of weak glyphs.
     * Null value when processing strong inters, non-null value when processing weak ones.
     */
    private List<Glyph> systemWeaks;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new SymbolsEraser object.
     *
     * @param buffer page buffer
     * @param g      graphics context on buffer
     * @param sheet  related sheet
     */
    public SymbolsEraser (ByteProcessor buffer,
                          Graphics2D g,
                          Sheet sheet)
    {
        super(buffer, g, sheet);

        symLag = sheet.getLag(Lags.SYMBOL_LAG);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-------------//
    // eraseShapes //
    //-------------//
    /**
     * Erase from image graphics all instances of provided shapes.
     *
     * @param shapes the shapes to process
     * @return weaksMap: the erased weak inter instances per system
     */
    public Map<SystemInfo, List<Glyph>> eraseShapes (Collection<Shape> shapes)
    {
        for (SystemInfo system : sheet.getSystems()) {
            final SIGraph sig = system.getSig();
            final List<Inter> strongs = new ArrayList<Inter>();
            final List<Inter> weaks = new ArrayList<Inter>();
            systemWeaks = null;

            for (Inter inter : sig.vertexSet()) {
                if (!inter.isDeleted() && shapes.contains(inter.getShape())) {
                    if (canHide(inter)) {
                        strongs.add(inter);
                    } else {
                        weaks.add(inter);
                    }
                }
            }

            // Simply erase the strongs
            for (Inter inter : strongs) {
                inter.accept(this);
            }

            // Erase and save the weaks
            systemWeaks = new ArrayList<Glyph>();
            weaksMap.put(system, systemWeaks);

            for (Inter inter : weaks) {
                inter.accept(this);
            }

            systemWeaks = null;
        }

        return weaksMap;
    }

    //-------//
    // visit //
    //-------//
    @Override
    public void visit (AbstractNoteInter inter)
    {
        final ShapeDescriptor desc = inter.getDescriptor();
        final int pitch = inter.getPitch();
        final boolean hasLine = (pitch % 2) == 0;
        final Template tpl = desc.getTemplate(new Template.Key(inter.getShape(), hasLine));
        final Rectangle box = desc.getBounds(inter.getBounds());

        // Use underlying glyph (enlarged only for strong inters)
        final List<Point> fores = tpl.getForegroundPixels(
                box,
                buffer,
                (systemWeaks == null) ? constants.margin.getValue() : 0);

        // Erase foreground pixels
        for (final Point p : fores) {
            g.fillRect(box.x + p.x, box.y + p.y, 1, 1);
        }

        // Save foreground pixels for optional (weak) glyphs
        if (systemWeaks != null) {
            savePixels(box, fores);
        }
    }

    //---------//
    // canHide //
    //---------//
    /**
     * Check if we can safely hide the inter (which cannot be mistaken for a symbol).
     * TODO: Quick hack, to be better implemented.
     *
     * @param inter the inter to check
     * @return true if we can safely hide the inter
     */
    @Override
    protected boolean canHide (Inter inter)
    {
        Double ctxGrade = inter.getContextualGrade();

        if (ctxGrade == null) {
            ctxGrade = inter.getGrade();
        }

        if (inter instanceof StemInter) {
            return ctxGrade >= 0.8;
        }

        if (inter instanceof AbstractNoteInter) {
            return ctxGrade >= 0.7;
        }

        return super.canHide(inter);
    }

    //--------------//
    // processGlyph //
    //--------------//
    @Override
    protected void processGlyph (Glyph glyph)
    {
        // Erase
        super.processGlyph(glyph);

        // Save the glyph?
        if (systemWeaks != null) {
            // The glyph may be made of several parts, so it's safer to restart from sections
            List<Glyph> glyphs = sheet.getNest().retrieveGlyphs(
                    glyph.getMembers(),
                    GlyphLayer.SYMBOL,
                    true,
                    Glyph.Linking.NO_LINK);

            systemWeaks.addAll(glyphs);
        }
    }

    //------------//
    // savePixels //
    //------------//
    /**
     * Save the provided pixels as optional glyphs.
     *
     * @param box   the absolute bounding box of inter descriptor (perhaps larger than the symbol)
     * @param fores foreground pixels with coordinates relative to descriptor bounding box
     */
    private void savePixels (Rectangle box,
                             List<Point> fores)
    {
        ByteProcessor buf = new ByteProcessor(box.width, box.height);
        buf.invert();

        for (Point p : fores) {
            buf.set(p.x, p.y, 0);
        }

        // Runs
        RunsTable runTable = new RunsTableFactory(SYMBOL_ORIENTATION, buf, 0).createTable(
                "optionals");

        // Sections
        SectionsBuilder sectionsBuilder = new SectionsBuilder(symLag, new JunctionRatioPolicy());
        List<Section> sections = sectionsBuilder.createSections(runTable, false);

        // Translate sections to absolute coordinates
        for (Section section : sections) {
            section.translate(box.getLocation());
        }

        // Glyphs
        List<Glyph> glyphs = sheet.getNest().retrieveGlyphs(
                sections,
                GlyphLayer.SYMBOL,
                true,
                Glyph.Linking.NO_LINK);

        for (Glyph glyph : glyphs) {
            systemWeaks.add(glyph);
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        final Constant.Integer margin = new Constant.Integer(
                "pixels",
                2,
                "Number of pixels added around notes");
    }
}
