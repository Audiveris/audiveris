//----------------------------------------------------------------------------//
//                                                                            //
//                            B a s i c G l y p h                             //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Hervé Bitteur 2000-2011. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.facets;

import omr.check.Result;

import omr.glyph.Evaluation;
import omr.glyph.GlyphSignature;
import omr.glyph.Nest;
import omr.glyph.Shape;
import omr.glyph.text.TextInfo;

import omr.lag.Lag;
import omr.lag.Section;

import omr.log.Logger;

import omr.math.Circle;
import omr.math.Line;
import omr.math.Moments;

import omr.run.Orientation;

import omr.score.common.PixelPoint;
import omr.score.common.PixelRectangle;
import omr.score.entity.TimeRational;

import omr.sheet.SystemInfo;

import omr.util.HorizontalSide;
import omr.util.Predicate;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

/**
 * Class {@code BasicGlyph} is the basic Glyph implementation for any glyph
 * found, such as stem, ledger, accidental, note head, etc...
 *
 * <p>A Glyph is basically a collection of sections. It can be split into
 * smaller glyphs, which may later be re-assembled into another instance of
 * glyph. There is a means, based on a simple signature (weight and bounding
 * box), to detect if the glyph at hand is identical to a previous one, which is
 * then reused.
 *
 * <p>A Glyph can be stored on disk and reloaded
 *
 * <p>From an implementation point of view, this {@code BasicGlyph} is just a
 * shell around specialized Glyph facets, and most of the methods are simply
 * forwarding to the proper facet.
 *
 * @author Hervé Bitteur
 */
public class BasicGlyph
    implements Glyph
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(BasicGlyph.class);

    //~ Instance fields --------------------------------------------------------

    /** All needed facets */
    final GlyphAdministration administration;
    final GlyphComposition composition;
    final GlyphDisplay     display;
    final GlyphEnvironment environment;
    final GlyphGeometry    geometry;
    final GlyphRecognition recognition;
    final GlyphTranslation translation;
    final GlyphAlignment   alignment;

    // Sequence of all facets
    final Set<GlyphFacet> facets = new LinkedHashSet<GlyphFacet>();

    //~ Constructors -----------------------------------------------------------

    //------------//
    // BasicGlyph //
    //------------//
    /**
     * Create a new BasicGlyph object
     * @param interline the scaling interline value
     */
    public BasicGlyph (int interline)
    {
        addFacet(administration = new BasicAdministration(this));
        addFacet(composition = new BasicComposition(this));
        addFacet(display = new BasicDisplay(this));
        addFacet(environment = new BasicEnvironment(this));
        addFacet(geometry = new BasicGeometry(this, interline));
        addFacet(recognition = new BasicRecognition(this));
        addFacet(translation = new BasicTranslation(this));
        addFacet(alignment = new BasicAlignment(this));
    }

    //------------//
    // BasicGlyph //
    //------------//
    /**
     * Create a new BasicGlyph object from a GlyphValue instance (typically
     * unmarshalled from XML)
     * @param value the GlyphValue "builder" object
     */
    public BasicGlyph (GlyphValue value)
    {
        this(value.interline);

        this.setId(value.id);
        this.setShape(value.shape);
        this.setStemNumber(value.stemNumber);
        this.setWithLedger(value.withLedger);
        this.setPitchPosition(value.pitchPosition);
        this.getMembers()
            .addAll(value.members);
    }

    //------------//
    // BasicGlyph //
    //------------//
    /**
     * Create a glyph with a specific alignment class
     * @param interline the scaling information
     * @param alignmentClass the specific alignment class
     */
    protected BasicGlyph (int                            interline,
                          Class<?extends GlyphAlignment> alignmentClass)
    {
        addFacet(administration = new BasicAdministration(this));
        addFacet(composition = new BasicComposition(this));
        addFacet(display = new BasicDisplay(this));
        addFacet(environment = new BasicEnvironment(this));
        addFacet(geometry = new BasicGeometry(this, interline));
        addFacet(recognition = new BasicRecognition(this));
        addFacet(translation = new BasicTranslation(this));

        GlyphAlignment theAlignment = null;

        try {
            Constructor constructor = alignmentClass.getConstructor(
                new Class[] { Glyph.class });
            theAlignment = (GlyphAlignment) constructor.newInstance(
                new Object[] { this });
        } catch (Exception ex) {
            logger.severe(
                "Cannot instantiate BasicGlyph with " + alignmentClass +
                " ex:" + ex);
        }

        addFacet(alignment = theAlignment);
    }

    //~ Methods ----------------------------------------------------------------

    public void addAttachment (String         id,
                               java.awt.Shape attachment)
    {
        display.addAttachment(id, attachment);
    }

    public void addSection (Section section,
                            Linking link)
    {
        composition.addSection(section, link);
    }

    public void addTranslation (Object entity)
    {
        translation.addTranslation(entity);
    }

    public void allowShape (Shape shape)
    {
        recognition.allowShape(shape);
    }

    public void clearTranslations ()
    {
        translation.clearTranslations();
    }

    public void colorize (Collection<Section> sections,
                          Color               color)
    {
        display.colorize(sections, color);
    }

    public void colorize (Color color)
    {
        display.colorize(color);
    }

    public void computeMoments ()
    {
        geometry.computeMoments();
    }

    public boolean containsSection (int id)
    {
        return composition.containsSection(id);
    }

    public void copyStemInformation (Glyph glyph)
    {
        environment.copyStemInformation(glyph);
    }

    public void cutSections ()
    {
        composition.cutSections();
    }

    public void drawAscii ()
    {
        display.drawAscii();
    }

    //------//
    // dump //
    //------//
    public void dump ()
    {
        for (GlyphFacet facet : facets) {
            facet.dump();
        }
    }

    public void forbidShape (Shape shape)
    {
        recognition.forbidShape(shape);
    }

    public int getAlienPixelsFrom (Lag                lag,
                                   PixelRectangle     absRoi,
                                   Predicate<Section> predicate)
    {
        return environment.getAlienPixelsFrom(lag, absRoi, predicate);
    }

    public SystemInfo getAlienSystem (SystemInfo system)
    {
        return composition.getAlienSystem(system);
    }

    public Glyph getAncestor ()
    {
        return composition.getAncestor();
    }

    public PixelPoint getAreaCenter ()
    {
        return geometry.getAreaCenter();
    }

    public double getAspect (Orientation orientation)
    {
        return alignment.getAspect(orientation);
    }

    public Map<String, java.awt.Shape> getAttachments ()
    {
        return display.getAttachments();
    }

    public PixelPoint getCentroid ()
    {
        return geometry.getCentroid();
    }

    public Circle getCircle ()
    {
        return geometry.getCircle();
    }

    public Color getColor ()
    {
        return display.getColor();
    }

    @Override
    public Set<Glyph> getConnectedNeighbors ()
    {
        return environment.getConnectedNeighbors();
    }

    public PixelRectangle getContourBox ()
    {
        return geometry.getContourBox();
    }

    public double getDensity ()
    {
        return geometry.getDensity();
    }

    public Evaluation getEvaluation ()
    {
        return recognition.getEvaluation();
    }

    public Section getFirstSection ()
    {
        return composition.getFirstSection();
    }

    public Glyph getFirstStem ()
    {
        return environment.getFirstStem();
    }

    public int getFirstStuck ()
    {
        return alignment.getFirstStuck();
    }

    public double getGrade ()
    {
        return recognition.getGrade();
    }

    public int getId ()
    {
        return administration.getId();
    }

    public BufferedImage getImage ()
    {
        return display.getImage();
    }

    public int getInterline ()
    {
        return geometry.getInterline();
    }

    public double getInvertedSlope ()
    {
        return alignment.getInvertedSlope();
    }

    public int getLastStuck ()
    {
        return alignment.getLastStuck();
    }

    public int getLength (Orientation orientation)
    {
        return alignment.getLength(orientation);
    }

    public Line getLine ()
    {
        return alignment.getLine();
    }

    public PixelPoint getLocation ()
    {
        return geometry.getLocation();
    }

    public double getMeanDistance ()
    {
        return alignment.getMeanDistance();
    }

    public double getMeanThickness (Orientation orientation)
    {
        return alignment.getMeanThickness(orientation);
    }

    public SortedSet<Section> getMembers ()
    {
        return composition.getMembers();
    }

    public int getMidPos (Orientation orientation)
    {
        return alignment.getMidPos(orientation);
    }

    public Moments getMoments ()
    {
        return geometry.getMoments();
    }

    public Nest getNest ()
    {
        return administration.getNest();
    }

    public double getNormalizedHeight ()
    {
        return geometry.getNormalizedHeight();
    }

    public double getNormalizedWeight ()
    {
        return geometry.getNormalizedWeight();
    }

    public double getNormalizedWidth ()
    {
        return geometry.getNormalizedWidth();
    }

    public Glyph getPartOf ()
    {
        return composition.getPartOf();
    }

    public double getPitchPosition ()
    {
        return environment.getPitchPosition();
    }

    public double getPositionAt (double      coord,
                                 Orientation orientation)
    {
        return alignment.getPositionAt(coord, orientation);
    }

    public Point2D getRectangleCentroid (PixelRectangle absRoi)
    {
        return alignment.getRectangleCentroid(absRoi);
    }

    public Result getResult ()
    {
        return composition.getResult();
    }

    public Shape getShape ()
    {
        return recognition.getShape();
    }

    public GlyphSignature getSignature ()
    {
        return geometry.getSignature();
    }

    public double getSlope ()
    {
        return alignment.getSlope();
    }

    public Point2D getStartPoint (Orientation orientation)
    {
        return alignment.getStartPoint(orientation);
    }

    public Glyph getStem (HorizontalSide side)
    {
        return environment.getStem(side);
    }

    public int getStemNumber ()
    {
        return environment.getStemNumber();
    }

    public Point2D getStopPoint (Orientation orientation)
    {
        return alignment.getStopPoint(orientation);
    }

    public void getSymbolsAfter (Predicate<Glyph> predicate,
                                 Set<Glyph>       goods,
                                 Set<Glyph>       bads)
    {
        environment.getSymbolsAfter(predicate, goods, bads);
    }

    public void getSymbolsBefore (Predicate<Glyph> predicate,
                                  Set<Glyph>       goods,
                                  Set<Glyph>       bads)
    {
        environment.getSymbolsBefore(predicate, goods, bads);
    }

    public TextInfo getTextInfo ()
    {
        return recognition.getTextInfo();
    }

    public int getThickness (Orientation orientation)
    {
        return alignment.getThickness(orientation);
    }

    public double getThicknessAt (double      coord,
                                  Orientation orientation)
    {
        return alignment.getThicknessAt(coord, orientation);
    }

    public TimeRational getTimeRational ()
    {
        return recognition.getTimeRational();
    }

    public Collection<Object> getTranslations ()
    {
        return translation.getTranslations();
    }

    public int getWeight ()
    {
        return geometry.getWeight();
    }

    public void include (Glyph that)
    {
        composition.include(that);
    }

    public boolean intersects (PixelRectangle rectangle)
    {
        return geometry.intersects(rectangle);
    }

    //-----------------//
    // invalidateCache //
    //-----------------//
    public void invalidateCache ()
    {
        for (GlyphFacet facet : facets) {
            facet.invalidateCache();
        }
    }

    public boolean isActive ()
    {
        return composition.isActive();
    }

    public boolean isBar ()
    {
        return recognition.isBar();
    }

    public boolean isClef ()
    {
        return recognition.isClef();
    }

    public boolean isKnown ()
    {
        return recognition.isKnown();
    }

    public boolean isManualShape ()
    {
        return recognition.isManualShape();
    }

    public boolean isShapeForbidden (Shape shape)
    {
        return recognition.isShapeForbidden(shape);
    }

    public boolean isStem ()
    {
        return recognition.isStem();
    }

    public boolean isSuccessful ()
    {
        return composition.isSuccessful();
    }

    public boolean isText ()
    {
        return recognition.isText();
    }

    public boolean isTransient ()
    {
        return administration.isTransient();
    }

    public boolean isTranslated ()
    {
        return translation.isTranslated();
    }

    public boolean isVip ()
    {
        return administration.isVip();
    }

    public boolean isVirtual ()
    {
        return administration.isVirtual();
    }

    public boolean isWellKnown ()
    {
        return recognition.isWellKnown();
    }

    public boolean isWithLedger ()
    {
        return environment.isWithLedger();
    }

    public void linkAllSections ()
    {
        composition.linkAllSections();
    }

    public void recolorize ()
    {
        display.recolorize();
    }

    public void renderAttachments (Graphics2D g)
    {
        display.renderAttachments(g);
    }

    public void renderLine (Graphics2D g)
    {
        alignment.renderLine(g);
    }

    public void resetEvaluation ()
    {
        recognition.resetEvaluation();
    }

    public void setCircle (Circle circle)
    {
        geometry.setCircle(circle);
    }

    public void setContourBox (PixelRectangle contourBox)
    {
        geometry.setContourBox(contourBox);
    }

    public void setEndingPoints (Point2D pStart,
                                 Point2D pStop)
    {
        alignment.setEndingPoints(pStart, pStop);
    }

    public void setEvaluation (Evaluation evaluation)
    {
        recognition.setEvaluation(evaluation);
    }

    public void setId (int id)
    {
        administration.setId(id);
    }

    public void setNest (Nest nest)
    {
        administration.setNest(nest);
    }

    public void setPartOf (Glyph compound)
    {
        composition.setPartOf(compound);
    }

    public void setPitchPosition (double pitchPosition)
    {
        environment.setPitchPosition(pitchPosition);
    }

    public void setResult (Result result)
    {
        composition.setResult(result);
    }

    public void setShape (Shape  shape,
                          double grade)
    {
        recognition.setShape(shape, grade);
    }

    public void setShape (Shape shape)
    {
        recognition.setShape(shape);
    }

    public void setStem (Glyph          stem,
                         HorizontalSide side)
    {
        environment.setStem(stem, side);
    }

    public void setStemNumber (int stemNumber)
    {
        environment.setStemNumber(stemNumber);
    }

    public void setTimeRational (TimeRational timeRational)
    {
        recognition.setTimeRational(timeRational);
    }

    public void setTranslation (Object entity)
    {
        translation.setTranslation(entity);
    }

    public void setVip ()
    {
        administration.setVip();
    }

    public void setWithLedger (boolean withLedger)
    {
        environment.setWithLedger(withLedger);
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder();

        sb.append("{")
          .append(getClass().getSimpleName())
          .append("#")
          .append(this.getId());

        sb.append(internalsString());

        sb.append("}");

        return sb.toString();
    }

    //-----------//
    // translate //
    //-----------//
    @Override
    public void translate (PixelPoint vector)
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
     * Return the string of the internals of this class, typically for inclusion
     * in a toString. The overriding methods, if any, should return a string
     * that begins with a " " followed by some content.
     *
     * @return the string of internals
     */
    protected String internalsString ()
    {
        StringBuilder sb = new StringBuilder(25);

        if (getShape() != null) {
            sb.append(" ")
              .append(recognition.getEvaluation());

            if (getShape()
                    .getPhysicalShape() != getShape()) {
                sb.append(" physical=")
                  .append(getShape().getPhysicalShape());
            }

            if (getShape()
                    .isText()) {
                String textContent = getTextInfo()
                                         .getContent();

                if (textContent != null) {
                    sb.append(" \"")
                      .append(textContent)
                      .append("\"");
                }
            }
        }

        if (getPartOf() != null) {
            sb.append(" partOf#")
              .append(getPartOf().getId());
        }

        if (getCentroid() != null) {
            sb.append(" centroid=[")
              .append(getCentroid().x)
              .append(",")
              .append(getCentroid().y)
              .append("]");
        }

        if (isTranslated()) {
            sb.append(" trans=[")
              .append(getTranslations())
              .append("]");
        }

        if (getResult() != null) {
            sb.append(" ")
              .append(getResult());
        }

        return sb.toString();
    }

    //----------//
    // addFacet //
    //----------//
    /**
     * Register a facet
     * @param facet the facet to register
     */
    final void addFacet (GlyphFacet facet)
    {
        facets.add(facet);
    }
}
