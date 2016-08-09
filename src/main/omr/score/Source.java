//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                           S o u r c e                                          //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
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
package omr.score;

import omr.sheet.SystemInfo;

import com.audiveris.proxymusic.Identification;
import com.audiveris.proxymusic.Miscellaneous;
import com.audiveris.proxymusic.MiscellaneousField;
import com.audiveris.proxymusic.ObjectFactory;
import com.audiveris.proxymusic.ScorePartwise;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Class {@code Source} precisely describes the source of a Score.
 * <p>
 * Within a given input file (or URI), perhaps composed of several sheet images, the relevant sheets
 * are listed, each with the systems processed for the ScorePartwise instance at hand.
 * <p>
 * A sheet with no system processed (for example because it is made of text only), could explicitly
 * appear but with an empty sequence of system numbers (this is recommended), or the sheet could not
 * appear at all in this source structure.
 * <p>
 * In the following example, the Score spans the first 3 sheets of the input file, with
 * sheet #2 being "empty", and only the first system in sheet #3.
 * Another Score instance, typically representing a following movement, could start with the
 * same sheet #3, but from system 2.
 * <p>
 * In MusicXML, such Source data is encoded using the miscellaneous element.
 * <br/>
 * Using file:
 * <pre>
 * &lt;miscellaneous&gt;
 *     &lt;miscellaneous-field name="source-file"&gt;D:\soft\scores\morphology\recordare\MozartTrio.png&lt;/miscellaneous-field&gt;
 *     &lt;miscellaneous-field name="source-offset"&gt;4&lt;/miscellaneous-field&gt;
 *     &lt;miscellaneous-field name="source-sheet-1"&gt;1 2&lt;/miscellaneous-field&gt;
 *     &lt;miscellaneous-field name="source-sheet-2"&gt;&lt;/miscellaneous-field&gt;
 *     &lt;miscellaneous-field name="source-sheet-3"&gt;1&lt;/miscellaneous-field&gt;
 * &lt;/miscellaneous&gt;
 * </pre>
 * <p>
 * Using uri:
 * <pre>
 * &lt;miscellaneous&gt;
 *     &lt;miscellaneous-field name="source-uri"&gt;file:///MozartTrio.png&lt;/miscellaneous-field&gt;
 *     &lt;miscellaneous-field name="source-offset"&gt;4&lt;/miscellaneous-field&gt;
 *     &lt;miscellaneous-field name="source-sheet-1"&gt;1 2&lt;/miscellaneous-field&gt;
 *     &lt;miscellaneous-field name="source-sheet-2"&gt;&lt;/miscellaneous-field&gt;
 *     &lt;miscellaneous-field name="source-sheet-3"&gt;1&lt;/miscellaneous-field&gt;
 * &lt;/miscellaneous&gt;
 * </pre>
 *
 * @author Hervé Bitteur
 */
public class Source
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(Source.class);

    private static final String SOURCE_PREFIX = "source-";

    private static final String SHEET_PREFIX = "sheet-";

    //~ Instance fields ----------------------------------------------------------------------------
    /** Path to source image file, if any. */
    private String file;

    /** Sheet offset with respect to full work. */
    private int offset;

    /** Source image URI, if any. */
    private URI uri;

    /** Systems processed in each image sheet. */
    private final List<SheetSystems> sheets = new ArrayList<SheetSystems>();

    //~ Methods ------------------------------------------------------------------------------------
    //--------//
    // decode //
    //--------//
    public static Source decode (ScorePartwise scorePartwise)
    {
        Identification identification = scorePartwise.getIdentification();

        if (identification == null) {
            return null;
        }

        Miscellaneous misc = identification.getMiscellaneous();

        if (misc == null) {
            return null;
        }

        Source source = new Source();

        for (MiscellaneousField field : misc.getMiscellaneousField()) {
            String name = field.getName();
            String value = field.getValue().trim();

            ///logger.info("miscellaneous-field name:{} value:'{}'", name, value);
            if (name.startsWith(SOURCE_PREFIX)) {
                String tail = name.substring(SOURCE_PREFIX.length());

                if (tail.equals("file")) {
                    source.file = value;
                } else if (tail.equals("uri")) {
                    source.uri = URI.create(value);
                } else if (tail.equals("offset")) {
                    source.offset = Integer.decode(value);
                } else if (tail.startsWith(SHEET_PREFIX)) {
                    String numStr = tail.substring(SHEET_PREFIX.length());
                    int num = Integer.decode(numStr);
                    SheetSystems sheet = new SheetSystems(num);
                    source.sheets.add(sheet);
                    sheet.getSystems().addAll(parseInts(value));
                }
            }
        }

        return source;
    }

    //------------//
    // encodePage //
    //------------//
    public void encodePage (Page page,
                            ScorePartwise scorePartwise)
    {
        final ObjectFactory factory = new ObjectFactory();
        Identification identification = scorePartwise.getIdentification();

        if (identification == null) {
            identification = factory.createIdentification();
            scorePartwise.setIdentification(identification);
        }

        Miscellaneous misc = identification.getMiscellaneous();

        if (misc == null) {
            misc = factory.createMiscellaneous();
            identification.setMiscellaneous(misc);
        }

        Source.SheetSystems sheetSystems = new Source.SheetSystems(
                page.getSheet().getStub().getNumber());
        sheets.add(sheetSystems);

        for (SystemInfo system : page.getSystems()) {
            sheetSystems.getSystems().add(system.getId());
        }

        MiscellaneousField field = factory.createMiscellaneousField();
        misc.getMiscellaneousField().add(field);
        field.setName(SOURCE_PREFIX + SHEET_PREFIX + sheetSystems.sheetNumber);
        field.setValue(packInts(sheetSystems.getSystems()));
    }

    //-------------//
    // encodeScore //
    //-------------//
    public void encodeScore (ScorePartwise scorePartwise)
    {
        final ObjectFactory factory = new ObjectFactory();
        Identification identification = scorePartwise.getIdentification();

        if (identification == null) {
            identification = factory.createIdentification();
            scorePartwise.setIdentification(identification);
        }

        Miscellaneous misc = identification.getMiscellaneous();

        if (misc == null) {
            misc = factory.createMiscellaneous();
            identification.setMiscellaneous(misc);
        }

        MiscellaneousField field;

        if (file != null) {
            misc.getMiscellaneousField().add(field = factory.createMiscellaneousField());
            field.setName(SOURCE_PREFIX + "file");
            field.setValue(file);
        } else if (uri != null) {
            misc.getMiscellaneousField().add(field = factory.createMiscellaneousField());
            field.setName(SOURCE_PREFIX + "uri");
            field.setValue(uri.toString());
        }

        if (offset != 0) {
            misc.getMiscellaneousField().add(field = factory.createMiscellaneousField());
            field.setName(SOURCE_PREFIX + "offset");
            field.setValue("" + offset);
        }
    }

    //
    //    //--------//
    //    // encode //
    //    //--------//
    //    public void encode (ScorePartwise scorePartwise)
    //    {
    //        final ObjectFactory factory = new ObjectFactory();
    //        Identification identification = scorePartwise.getIdentification();
    //
    //        if (identification == null) {
    //            identification = factory.createIdentification();
    //            scorePartwise.setIdentification(identification);
    //        }
    //
    //        Miscellaneous misc = identification.getMiscellaneous();
    //
    //        if (misc == null) {
    //            misc = factory.createMiscellaneous();
    //            identification.setMiscellaneous(misc);
    //        }
    //
    //        MiscellaneousField field;
    //
    //        if (file != null) {
    //            misc.getMiscellaneousField().add(field = factory.createMiscellaneousField());
    //            field.setName(SOURCE_PREFIX + "file");
    //            field.setValue(file);
    //        } else if (uri != null) {
    //            misc.getMiscellaneousField().add(field = factory.createMiscellaneousField());
    //            field.setName(SOURCE_PREFIX + "uri");
    //            field.setValue(uri.toString());
    //        }
    //
    //        if (offset != 0) {
    //            misc.getMiscellaneousField().add(field = factory.createMiscellaneousField());
    //            field.setName(SOURCE_PREFIX + "offset");
    //            field.setValue("" + offset);
    //        }
    //
    //        for (SheetSystems sheet : sheets) {
    //            misc.getMiscellaneousField().add(field = factory.createMiscellaneousField());
    //            field.setName(SOURCE_PREFIX + SHEET_PREFIX + sheet.sheetNumber);
    //            field.setValue(packInts(sheet.getSystems()));
    //        }
    //    }
    //
    /**
     * @return the file
     */
    public String getFile ()
    {
        return file;
    }

    /**
     * @return the offset
     */
    public int getOffset ()
    {
        return offset;
    }

    /**
     * @return the sheetSystems
     */
    public List<SheetSystems> getSheets ()
    {
        return sheets;
    }

    /**
     * @return the uri
     */
    public URI getUri ()
    {
        return uri;
    }

    /**
     * @param file the file to set
     */
    public void setFile (String file)
    {
        this.file = file;
    }

    /**
     * @param offset the offset to set
     */
    public void setOffset (int offset)
    {
        this.offset = offset;
    }

    /**
     * @param uri the uri to set
     */
    public void setUri (URI uri)
    {
        this.uri = uri;
    }

    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder(getClass().getSimpleName());
        sb.append("{");

        if (file != null) {
            sb.append("file='").append(file).append("'");
        } else if (uri != null) {
            sb.append("uri=").append(uri);
        }

        if (offset != 0) {
            sb.append(" offset=").append(offset);
        }

        for (SheetSystems sheet : sheets) {
            sb.append(" ").append(sheet);
        }

        sb.append("}");

        return sb.toString();
    }

    //----------//
    // packInts //
    //----------//
    /**
     * Return a single string made of provided ints separated by space.
     *
     * @param ints the provided integer values
     * @return the resulting string
     */
    private static String packInts (List<Integer> ints)
    {
        StringBuilder sb = new StringBuilder();

        for (int val : ints) {
            if (sb.length() > 0) {
                sb.append(" ");
            }

            sb.append(val);
        }

        return sb.toString();
    }

    //-----------//
    // parseInts //
    //-----------//
    /**
     * Parse a string of integers, separated by space.
     *
     * @param str the string to parse
     * @return the sequence of integers decoded
     */
    private static List<Integer> parseInts (String str)
    {
        final List<Integer> intList = new ArrayList<Integer>();
        final String[] tokens = str.split("\\s+");

        for (String token : tokens) {
            try {
                String trimmedToken = token.trim();

                if (!trimmedToken.isEmpty()) {
                    intList.add(Integer.decode(trimmedToken));
                }
            } catch (NumberFormatException ex) {
                logger.warn("Illegal integer", ex);
            }
        }

        return intList;
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //--------------//
    // SheetSystems //
    //--------------//
    /**
     * Describes which systems were processed in this sheet.
     */
    public static class SheetSystems
    {
        //~ Instance fields ------------------------------------------------------------------------

        /** Sheet number within source file, starting from 1. */
        final int sheetNumber;

        /** Sequence of systems processed, starting from 1. */
        private final List<Integer> systems = new ArrayList<Integer>();

        //~ Constructors ---------------------------------------------------------------------------
        public SheetSystems (int sheetNumber)
        {
            this.sheetNumber = sheetNumber;
        }

        //~ Methods --------------------------------------------------------------------------------
        /**
         * @return the systems
         */
        public List<Integer> getSystems ()
        {
            return systems;
        }

        @Override
        public String toString ()
        {
            StringBuilder sb = new StringBuilder("sheet");
            sb.append("#").append(sheetNumber);
            sb.append("[");

            boolean first = true;

            for (Integer system : systems) {
                if (!first) {
                    sb.append(" ");
                }

                sb.append(system);
                first = false;
            }

            sb.append("]");

            return sb.toString();
        }
    }
}
