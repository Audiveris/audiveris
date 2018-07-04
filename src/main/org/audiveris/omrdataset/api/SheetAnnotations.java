//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                 S h e e t A n n o t a t i o n s                                //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2018. All rights reserved.
//
//  This program is free software: you can redistribute it and/or modify it under the terms of the
//  GNU Affero General Public License as published by the Free Software Foundation, either version
//  3 of the License, or (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
//  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//  See the GNU Affero General Public License for more details.
//
//  You should have received a copy of the GNU Affero General Public License along with this
//  program.  If not, see <http://www.gnu.org/licenses/>.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package org.audiveris.omrdataset.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Dimension;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Class {@code SheetAnnotations} represents the symbols information for a sheet.
 * It's essentially a sequence of: {symbol name + symbol bounding box}
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "Annotations")
public class SheetAnnotations
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(
            SheetAnnotations.class);

    /** Un/marshalling context for use with JAXB. */
    private static volatile JAXBContext jaxbContext;

    //~ Instance fields ----------------------------------------------------------------------------
    @XmlAttribute(name = "version")
    private String version;

    @XmlAttribute(name = "complete")
    private Boolean complete;

    @XmlElement(name = "Source")
    private String source;

    @XmlElement(name = "Page")
    private SheetInfo sheetInfo;

    @XmlElement(name = "Symbol")
    private ArrayList<SymbolInfo> symbols = new ArrayList<SymbolInfo>();

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code SheetAnnotations} object.
     */
    public SheetAnnotations ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    /**
     * Add a symbol to the annotations
     *
     * @param symbol symbol to add
     */
    public void addSymbol (SymbolInfo symbol)
    {
        symbols.add(symbol);
    }

    /**
     * Report information about sheet.
     *
     * @return sheet information
     */
    public SheetInfo getSheetInfo ()
    {
        return sheetInfo;
    }

    /**
     * @return the source
     */
    public String getSource ()
    {
        return source;
    }

    /**
     * Report the (live) list of symbols in sheet.
     *
     * @return symbols list
     */
    public List<SymbolInfo> getSymbols ()
    {
        return symbols;
    }

    /**
     * @return the version
     */
    public String getVersion ()
    {
        return version;
    }

    /**
     * Report whether these annotations are complete.
     *
     * @return the complete
     */
    public boolean isComplete ()
    {
        return (complete != null) && complete;
    }

    //----------//
    // marshall //
    //----------//
    /**
     * Marshall this instance to the provided XML file.
     *
     * @param path to the XML output file
     * @throws IOException   in case of IO problem
     * @throws JAXBException in case of marshalling problem
     */
    public void marshall (Path path)
            throws IOException, JAXBException
    {
        if (!Files.exists(path.getParent())) {
            Files.createDirectories(path.getParent());
        }

        OutputStream os = new BufferedOutputStream(
                Files.newOutputStream(path, StandardOpenOption.CREATE));
        Marshaller m = getJaxbContext().createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        m.marshal(this, os);
        os.flush();
        os.close();
    }

    /**
     * Set completeness information.
     *
     * @param complete the complete to set
     */
    public void setComplete (boolean complete)
    {
        this.complete = complete ? true : null;
    }

    /**
     * @param sheetInfo the sheetInfo to set
     */
    public void setSheetInfo (SheetInfo sheetInfo)
    {
        this.sheetInfo = sheetInfo;
    }

    /**
     * @param source the source to set
     */
    public void setSource (String source)
    {
        this.source = source;
    }

    /**
     * @param version the version to set
     */
    public void setVersion (String version)
    {
        this.version = version;
    }

    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder("Annotations{");
        sb.append("version:").append(version);

        if (source != null) {
            sb.append(" source:").append(source);
        }

        if (sheetInfo != null) {
            sb.append(" sheet:").append(sheetInfo);
        }

        sb.append(" symbols:").append(symbols.size());

        sb.append("}");

        return sb.toString();
    }

    //-----------//
    // unmarshal //
    //-----------//
    /**
     * Load SheetAnnotations from the annotations XML file.
     *
     * @param path to the XML input file.
     * @return the unmarshalled SheetAnnotations object
     * @throws IOException in case of IO problem
     */
    public static SheetAnnotations unmarshal (Path path)
            throws IOException
    {
        logger.debug("SheetAnnotations unmarshalling {}", path);

        try {
            InputStream is = Files.newInputStream(path, StandardOpenOption.READ);
            Unmarshaller um = getJaxbContext().createUnmarshaller();
            SheetAnnotations sheetInfo = (SheetAnnotations) um.unmarshal(is);
            logger.debug("Unmarshalled {}", sheetInfo);
            is.close();

            return sheetInfo;
        } catch (JAXBException ex) {
            logger.warn("Error unmarshalling " + path + " " + ex, ex);

            return null;
        }
    }

    //----------------//
    // getJaxbContext //
    //----------------//
    private static JAXBContext getJaxbContext ()
            throws JAXBException
    {
        // Lazy creation
        if (jaxbContext == null) {
            jaxbContext = JAXBContext.newInstance(SheetAnnotations.class);
        }

        return jaxbContext;
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // SheetInfo //
    //-----------//
    public static class SheetInfo
    {
        //~ Instance fields ------------------------------------------------------------------------

        @XmlElement(name = "Image")
        public final String imageFileName;

        @XmlElement(name = "Size")
        @XmlJavaTypeAdapter(DimensionAdapter.class)
        public final Dimension dim;

        //~ Constructors ---------------------------------------------------------------------------
        public SheetInfo (String imageFileName,
                          Dimension dim)
        {
            this.imageFileName = imageFileName;
            this.dim = dim;
        }

        // No-arg constructor needed by JAXB
        private SheetInfo ()
        {
            this.imageFileName = null;
            this.dim = null;
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public String toString ()
        {
            return "{" + imageFileName + " [width=" + dim.width + ",height=" + dim.height + "]}";
        }

        //~ Inner Classes --------------------------------------------------------------------------
        public static class DimensionAdapter
                extends XmlAdapter<DimensionAdapter.DimensionFacade, Dimension>
        {
            //~ Methods ----------------------------------------------------------------------------

            @Override
            public DimensionFacade marshal (Dimension dim)
                    throws Exception
            {
                return new DimensionFacade(dim);
            }

            @Override
            public Dimension unmarshal (DimensionFacade facade)
                    throws Exception
            {
                return facade.getDimension();
            }

            //~ Inner Classes ----------------------------------------------------------------------
            private static class DimensionFacade
            {
                //~ Instance fields ----------------------------------------------------------------

                @XmlAttribute(name = "w")
                public int width;

                @XmlAttribute(name = "h")
                public int height;

                //~ Constructors -------------------------------------------------------------------
                public DimensionFacade (Dimension dimension)
                {
                    width = dimension.width;
                    height = dimension.height;
                }

                private DimensionFacade ()
                {
                }

                //~ Methods ------------------------------------------------------------------------
                public Dimension getDimension ()
                {
                    return new Dimension(width, height);
                }
            }
        }
    }
}
