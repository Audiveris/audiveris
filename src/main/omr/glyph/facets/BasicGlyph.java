//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      B a s i c G l y p h                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.facets;

import omr.check.Failure;

import omr.glyph.GlyphLayer;
import omr.glyph.GlyphNest;
import omr.glyph.GlyphSignature;
import omr.glyph.Shape;

import omr.lag.Section;

import omr.math.Circle;
import omr.math.Line;
import omr.math.PointsCollector;

import omr.moments.ARTMoments;
import omr.moments.GeometricMoments;

import omr.run.Orientation;

import omr.sheet.Scale;

import ij.process.ByteProcessor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.lang.reflect.Constructor;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

/**
 * Class {@code BasicGlyph} is the basic Glyph implementation.
 * <p>
 * From an implementation point of view, this {@code BasicGlyph} is just a shell around specialized
 * Glyph facets, and most of the methods are simply using delegation to the proper facet.
 *
 * @author Hervé Bitteur
 */
public class BasicGlyph
        implements Glyph
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(BasicGlyph.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** All needed facets */
    final GlyphAdministration administration;

    final GlyphComposition composition;

    final GlyphDisplay display;

    final GlyphEnvironment environment;

    final GlyphGeometry geometry;

    final GlyphRecognition recognition;

    final GlyphAlignment alignment;

    // The content facet is not final to allow lazy allocation
    protected GlyphContent content;

    final GlyphInterpret interpretation;

    // Set all facets
    final Set<GlyphFacet> facets = new LinkedHashSet<GlyphFacet>();

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Create a new BasicGlyph object.
     *
     * @param scale the scaling
     * @param layer the assigned layer
     */
    public BasicGlyph (Scale scale,
                       GlyphLayer layer)
    {
        this(scale.getInterline(), layer);
    }

    /**
     * Create a new BasicGlyph object.
     *
     * @param interline the scaling interline value
     * @param layer     the assigned layer
     */
    public BasicGlyph (int interline,
                       GlyphLayer layer)
    {
        addFacet(administration = new BasicAdministration(this, layer));
        addFacet(composition = new BasicComposition(this));
        addFacet(display = new BasicDisplay(this));
        addFacet(environment = new BasicEnvironment(this));
        addFacet(geometry = new BasicGeometry(this, interline));
        addFacet(recognition = new BasicRecognition(this));
        addFacet(interpretation = new BasicInterpret(this));
        addFacet(alignment = new BasicAlignment(this));
    }

    /**
     * Create a new BasicGlyph object from a GlyphValue instance
     * (typically un-marshalled from XML).
     *
     * @param value the GlyphValue "builder" object
     */
    public BasicGlyph (GlyphValue value)
    {
        this(value.interline, GlyphLayer.XML);

        setId(value.id);
        setShape(value.shape);
        setPitchPosition(value.pitchPosition);

        for (Section section : value.members) {
            addSection(section, Linking.NO_LINK);
        }
    }

    /**
     * Create a glyph with a specific alignment class.
     *
     * @param interline      the scaling information
     * @param layer          the assigned layer
     * @param alignmentClass the specific alignment class
     */
    protected BasicGlyph (int interline,
                          GlyphLayer layer,
                          Class<? extends GlyphAlignment> alignmentClass)
    {
        addFacet(administration = new BasicAdministration(this, layer));
        addFacet(composition = new BasicComposition(this));
        addFacet(display = new BasicDisplay(this));
        addFacet(environment = new BasicEnvironment(this));
        addFacet(geometry = new BasicGeometry(this, interline));
        addFacet(recognition = new BasicRecognition(this));
        addFacet(interpretation = new BasicInterpret(this));

        GlyphAlignment theAlignment = null;

        try {
            Constructor<?> constructor = alignmentClass.getConstructor(
                    new Class<?>[]{Glyph.class});
            theAlignment = (GlyphAlignment) constructor.newInstance(new Object[]{this});
        } catch (Exception ex) {
            logger.error("Cannot instantiate BasicGlyph with {} ex:{}", alignmentClass, ex);
        }

        addFacet(alignment = theAlignment);
    }

    //~ Methods ------------------------------------------------------------------------------------
    @Override
    public void addAttachment (String id,
                               java.awt.Shape attachment)
    {
        display.addAttachment(id, attachment);
    }

    @Override
    public void addFailure (Failure failure)
    {
        composition.addFailure(failure);
    }

    @Override
    public void addSection (Section section,
                            Linking link)
    {
        composition.addSection(section, link);
    }

    @Override
    public String asciiDrawing ()
    {
        return display.asciiDrawing();
    }

    @Override
    public boolean containsSection (int id)
    {
        return composition.containsSection(id);
    }

    @Override
    public void cutSections ()
    {
        composition.cutSections();
    }

    //--------//
    // dumpOf //
    //--------//
    @Override
    public String dumpOf ()
    {
        StringBuilder sb = new StringBuilder();

        for (GlyphFacet facet : facets) {
            sb.append(facet.dumpOf());
        }

        return sb.toString();
    }

    @Override
    public ARTMoments getARTMoments ()
    {
        return geometry.getARTMoments();
    }

    @Override
    public Glyph getAncestor ()
    {
        return composition.getAncestor();
    }

    @Override
    public Point getAreaCenter ()
    {
        return geometry.getAreaCenter();
    }

    @Override
    public double getAspect (Orientation orientation)
    {
        return alignment.getAspect(orientation);
    }

    @Override
    public Map<String, java.awt.Shape> getAttachments ()
    {
        return display.getAttachments();
    }

    @Override
    public Rectangle getBounds ()
    {
        return geometry.getBounds();
    }

    @Override
    public Point getCentroid ()
    {
        return geometry.getCentroid();
    }

    @Override
    public Circle getCircle ()
    {
        return geometry.getCircle();
    }

    @Override
    public Color getColor ()
    {
        return display.getColor();
    }

    @Override
    public double getDensity ()
    {
        return geometry.getDensity();
    }

    @Override
    public Set<Failure> getFailures ()
    {
        return composition.getFailures();
    }

    @Override
    public Section getFirstSection ()
    {
        return composition.getFirstSection();
    }

    @Override
    public GeometricMoments getGeometricMoments ()
    {
        return geometry.getGeometricMoments();
    }

    @Override
    public int getId ()
    {
        return administration.getId();
    }

    @Override
    public ByteProcessor getImage ()
    {
        return display.getImage();
    }

    @Override
    public int getInterline ()
    {
        return geometry.getInterline();
    }

    @Override
    public double getInvertedSlope ()
    {
        return alignment.getInvertedSlope();
    }

    @Override
    public GlyphLayer getLayer ()
    {
        return administration.getLayer();
    }

    @Override
    public int getLength (Orientation orientation)
    {
        return alignment.getLength(orientation);
    }

    @Override
    public Line getLine ()
    {
        return alignment.getLine();
    }

    @Override
    public Point getLocation ()
    {
        return geometry.getLocation();
    }

    @Override
    public Point getLocation (Shape shape)
    {
        return geometry.getLocation(shape);
    }

    @Override
    public double getMeanDistance ()
    {
        return alignment.getMeanDistance();
    }

    @Override
    public double getMeanThickness (Orientation orientation)
    {
        return alignment.getMeanThickness(orientation);
    }

    @Override
    public SortedSet<Section> getMembers ()
    {
        return composition.getMembers();
    }

    @Override
    public int getMidPos (Orientation orientation)
    {
        return alignment.getMidPos(orientation);
    }

    @Override
    public GlyphNest getNest ()
    {
        return administration.getNest();
    }

    @Override
    public double getNormalizedHeight ()
    {
        return geometry.getNormalizedHeight();
    }

    @Override
    public double getNormalizedWeight ()
    {
        return geometry.getNormalizedWeight();
    }

    @Override
    public double getNormalizedWidth ()
    {
        return geometry.getNormalizedWidth();
    }

    @Override
    public Glyph getPartOf ()
    {
        return composition.getPartOf();
    }

    @Override
    public double getPitchPosition ()
    {
        return environment.getPitchPosition();
    }

    @Override
    public PointsCollector getPointsCollector ()
    {
        return geometry.getPointsCollector();
    }

    @Override
    public double getPositionAt (double coord,
                                 Orientation orientation)
    {
        return alignment.getPositionAt(coord, orientation);
    }

    @Override
    public Point2D getRectangleCentroid (Rectangle absRoi)
    {
        return alignment.getRectangleCentroid(absRoi);
    }

    @Override
    public GlyphSignature getRegisteredSignature ()
    {
        return geometry.getRegisteredSignature();
    }

    @Override
    public Shape getShape ()
    {
        return recognition.getShape();
    }

    @Override
    public GlyphSignature getSignature ()
    {
        return geometry.getSignature();
    }

    @Override
    public double getSlope ()
    {
        return alignment.getSlope();
    }

    @Override
    public Point2D getStartPoint (Orientation orientation)
    {
        return alignment.getStartPoint(orientation);
    }

    @Override
    public Point2D getStopPoint (Orientation orientation)
    {
        return alignment.getStopPoint(orientation);
    }

    @Override
    public int getThickness (Orientation orientation)
    {
        return alignment.getThickness(orientation);
    }

    @Override
    public double getThicknessAt (double coord,
                                  Orientation orientation)
    {
        return alignment.getThicknessAt(coord, orientation);
    }

    @Override
    public int getWeight ()
    {
        return geometry.getWeight();
    }

    @Override
    public String idString ()
    {
        return administration.idString();
    }

    @Override
    public boolean intersects (java.awt.Shape shape)
    {
        return geometry.intersects(shape);
    }

    @Override
    public boolean intersects (Glyph that)
    {
        return environment.intersects(that);
    }

    //-----------------//
    // invalidateCache //
    //-----------------//
    @Override
    public void invalidateCache ()
    {
        // Invalidate all allocated facets
        for (GlyphFacet facet : facets) {
            facet.invalidateCache();
        }
    }

    @Override
    public boolean isActive ()
    {
        return composition.isActive();
    }

    @Override
    public boolean isLineDefined ()
    {
        return alignment.isLineDefined();
    }

    @Override
    public boolean isTransient ()
    {
        return administration.isTransient();
    }

    @Override
    public boolean isVip ()
    {
        return administration.isVip();
    }

    @Override
    public boolean isVirtual ()
    {
        return administration.isVirtual();
    }

    @Override
    public void linkAllSections ()
    {
        composition.linkAllSections();
    }

    @Override
    public int removeAttachments (String prefix)
    {
        return display.removeAttachments(prefix);
    }

    @Override
    public boolean removeSection (Section section,
                                  Linking link)
    {
        return composition.removeSection(section, link);
    }

    @Override
    public void renderAttachments (Graphics2D g)
    {
        display.renderAttachments(g);
    }

    @Override
    public void renderLine (Graphics2D g)
    {
        alignment.renderLine(g);
    }

    @Override
    public void setCircle (Circle circle)
    {
        geometry.setCircle(circle);
    }

    @Override
    public void setContourBox (Rectangle contourBox)
    {
        geometry.setContourBox(contourBox);
    }

    @Override
    public void setEndingPoints (Point2D pStart,
                                 Point2D pStop)
    {
        alignment.setEndingPoints(pStart, pStop);
    }

    @Override
    public void setId (int id)
    {
        administration.setId(id);
    }

    @Override
    public void setNest (GlyphNest nest)
    {
        administration.setNest(nest);
    }

    @Override
    public void setPartOf (Glyph compound)
    {
        composition.setPartOf(compound);
    }

    @Override
    public void setPitchPosition (double pitchPosition)
    {
        environment.setPitchPosition(pitchPosition);
    }

    @Override
    public void setRegisteredSignature (GlyphSignature sig)
    {
        geometry.setRegisteredSignature(sig);
    }

    @Override
    public void setShape (Shape shape)
    {
        recognition.setShape(shape);
    }

    @Override
    public void setVip ()
    {
        administration.setVip();
    }

    @Override
    public void stealSections (Glyph that)
    {
        composition.stealSections(that);
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder();

        sb.append("{").append(getClass().getSimpleName()).append("#").append(getId());

        sb.append(internalsString());

        sb.append("}");

        return sb.toString();
    }

    @Override
    public boolean touches (Glyph that)
    {
        return environment.touches(that);
    }

    @Override
    public boolean touches (Section section)
    {
        return composition.touches(section);
    }

    @Override
    public void translate (Point vector)
    {
        geometry.translate(vector);
    }

    //--------------//
    // getAlignment //
    //--------------//
    protected GlyphAlignment getAlignment ()
    {
        return alignment;
    }

    //----------------//
    // getComposition //
    //----------------//
    protected GlyphComposition getComposition ()
    {
        return composition;
    }

    //-----------------//
    // internalsString //
    //-----------------//
    /**
     * Return the string of the internals of this class, typically for
     * inclusion in a toString.
     * The overriding methods, if any, should return a string that begins with
     * a " " followed by some content.
     *
     * @return the string of internals
     */
    protected String internalsString ()
    {
        StringBuilder sb = new StringBuilder();

        if (getShape() != null) {
            sb.append(" ").append(getShape());

            if (getShape().getPhysicalShape() != getShape()) {
                sb.append(" physical=").append(getShape().getPhysicalShape());
            }
        }

        if (getPartOf() != null) {
            sb.append(" partOf#").append(getPartOf().getId());
        }

        Rectangle box = getBounds();

        if (box != null) {
            sb.append(" bounds=[").append(box.x).append(",").append(box.y).append(",")
                    .append(box.width).append(",").append(box.height).append("]");
        }

        if (!getFailures().isEmpty()) {
            sb.append(" ").append(getFailures());
        }

        return sb.toString();
    }

    //----------//
    // addFacet //
    //----------//
    /**
     * Register a facet
     *
     * @param facet the facet to register
     */
    final void addFacet (GlyphFacet facet)
    {
        facets.add(facet);
    }
}
