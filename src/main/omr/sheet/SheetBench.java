//----------------------------------------------------------------------------//
//                                                                            //
//                            S h e e t B e n c h                             //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet;

import omr.Main;

import omr.script.Script;

import omr.step.Step;

import java.io.*;
import java.util.*;

import javax.xml.bind.*;
import javax.xml.bind.annotation.*;

/**
 * Class {@code SheetBench} is in charge of recording all important information
 * related to the processing of a music sheet
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "bench")
public class SheetBench
{
    //~ Static fields/initializers ---------------------------------------------

    /** JAXB context for marshalling score benches */
    private static JAXBContext jaxbContext;

    //~ Instance fields --------------------------------------------------------

    /** The related sheet */
    private final Sheet sheet;

    /** Time stamp when this instance was created */
    private final long startTime = System.currentTimeMillis();

    /** Starting date */
    @XmlElement
    Date date = new Date(startTime);

    /** Application name */
    @XmlElement
    private final String program = Main.getToolName();

    /** Functional specification */
    @XmlElement
    private final String version = Main.getToolVersion();

    /** Hg tip revision */
    @XmlElement
    private final String revision = "No yet implemented"; // Place-holder

    /** Build info */
    @XmlElement
    private final String build = Main.getToolBuild();

    /** Score image */
    @XmlElement(name = "image")
    private String imagePath;

    /** The processing script */
    @XmlElement
    private Script script;

    /** Time when we started current step */
    private long stepStartTime = startTime;

    /** Size of the initial (non-rotated) image */
    @XmlElement(name = "initial-dimension")
    private Dim initialDimension;

    /** Skew angle */
    @XmlElement
    private double skew;

    /** Size of the rotated image */
    @XmlElement(name = "rotated-dimension")
    private Dim rotatedDimension;

    /** Scale of the sheet */
    @XmlElement(name = "scale")
    private Scale scale;

    /** Total number of staves */
    @XmlElement
    private int staves;

    /** Number of parts */
    @XmlElement
    private int parts;

    /** Number of systems */
    @XmlElement
    private int systems;

    /** Step sequence */
    @XmlElement(name = "step")
    private final List<StepBench> steps = new ArrayList<StepBench>();

    /** Whole processing duration */
    @XmlElement(name = "whole-duration")
    private long wholeDuration;

    //~ Constructors -----------------------------------------------------------

    //------------//
    // SheetBench //
    //------------//
    /**
     * Creates a new SheetBench object.
     * @param sheet the related sheet
     */
    public SheetBench (Sheet sheet)
    {
        this.sheet = sheet;
    }

    //------------//
    // SheetBench //
    //------------//
    /** Needed by JAXB */
    private SheetBench ()
    {
        sheet = null;
    }

    //~ Methods ----------------------------------------------------------------

    //------------------------//
    // recordInitialDimension //
    //------------------------//
    public void recordInitialDimension (int width,
                                        int height)
    {
        this.initialDimension = new Dim(width, height);
    }

    //-----------------//
    // recordPartCount //
    //-----------------//
    public void recordPartCount (int partCount)
    {
        this.parts = partCount;
    }

    //------------------------//
    // recordRotatedDimension //
    //------------------------//
    public void recordRotatedDimension (int width,
                                        int height)
    {
        this.rotatedDimension = new Dim(width, height);
    }

    //-------------//
    // recordScale //
    //-------------//
    public void recordScale (Scale scale)
    {
        this.scale = scale;
    }

    //------------//
    // recordSkew //
    //------------//
    public void recordSkew (double skew)
    {
        this.skew = skew;
    }

    //------------------//
    // recordStaveCount //
    //------------------//
    public void recordStaveCount (int staveCount)
    {
        this.staves = staveCount;
    }

    //------------//
    // recordStep //
    //------------//
    public void recordStep (Step step)
    {
        long now = System.currentTimeMillis();
        steps.add(new StepBench(step.name(), now - stepStartTime));
        stepStartTime = now;
    }

    //-------------------//
    // recordSystemCount //
    //-------------------//
    public void recordSystemCount (int systemCount)
    {
        this.systems = systemCount;
    }

    //-------//
    // store //
    //-------//
    /**
     * Store this bench into an output stream
     *
     * @param output the output stream to be written
     * @throws JAXBException
     */
    public void store (OutputStream output)
        throws JAXBException
    {
        wholeDuration = System.currentTimeMillis() - startTime;

        // Finalize this bench
        script = sheet.getScript();
        imagePath = sheet.getPath();

        // Store to file
        Marshaller m = getJaxbContext()
                           .createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        m.marshal(this, output);
    }

    //----------------//
    // getJaxbContext //
    //----------------//
    private static JAXBContext getJaxbContext ()
        throws JAXBException
    {
        // Lazy creation
        if (jaxbContext == null) {
            jaxbContext = JAXBContext.newInstance(SheetBench.class);
        }

        return jaxbContext;
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----//
    // Dim //
    //-----//
    private static class Dim
    {
        //~ Instance fields ----------------------------------------------------

        @XmlElement
        final int            width;
        @XmlElement
        final int            height;

        //~ Constructors -------------------------------------------------------

        public Dim (int width,
                    int height)
        {
            this.width = width;
            this.height = height;
        }

        // For JAXB
        private Dim ()
        {
            width = height = 0;
        }
    }

    //-----------//
    // StepBench //
    //-----------//
    private static class StepBench
    {
        //~ Instance fields ----------------------------------------------------

        @XmlElement
        final String            name;
        @XmlElement
        final long              duration;

        //~ Constructors -------------------------------------------------------

        public StepBench (String step,
                          long   duration)
        {
            this.name = step;
            this.duration = duration;
        }

        // For JAXB
        private StepBench ()
        {
            name = null;
            duration = 0;
        }
    }
}
