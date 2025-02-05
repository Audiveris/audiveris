//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  X s l t T r a n s f o r m e r                                 //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2023. All rights reserved.
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
package org.audiveris.schema;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.ParserProperties;
import org.kohsuke.args4j.spi.OptionHandler;
import org.kohsuke.args4j.spi.StopOptionHandler;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

/**
 * Class <code>XsltTransformer</code> is a simple XSLT transformer using JAXP,
 * derived from https://en.wikipedia.org/wiki/Java_API_for_XML_Processing.
 *
 * @author Hervé Bitteur
 */
public class XsltTransformer
{
    //~ Static fields/initializers -----------------------------------------------------------------

    //~ Instance fields ----------------------------------------------------------------------------

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new <code>XsltTransformer</code> instance.
     */
    public XsltTransformer ()
    {
    }
    //~ Methods ------------------------------------------------------------------------------------

    //~ Static Methods -----------------------------------------------------------------------------

    //-----------------//
    // convertToString //
    //-----------------//
    /**
     * Converts the input stream content as a single string.
     *
     * @param inputStream the input stream
     * @return the resulting string
     * @throws IOException
     */
    public static String convertToString (InputStream inputStream)
        throws IOException
    {
        final StringBuilder stringBuilder = new StringBuilder();

        try (BufferedReader bufferedReader = new BufferedReader(
                new InputStreamReader(inputStream))) {
            String line;

            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
                stringBuilder.append("\n");
            }
        }

        return stringBuilder.toString();
    }

    //------//
    // main //
    //------//
    /**
     * Command line entry point.
     *
     * @param args styleSheet.xsl source.xsd target.html
     * @throws TransformerFactoryConfigurationError
     * @throws TransformerException
     * @throws java.io.FileNotFoundException
     * @throws org.kohsuke.args4j.CmdLineException
     */
    public static void main (String[] args)
        throws TransformerFactoryConfigurationError, TransformerException, FileNotFoundException,
        IOException, CmdLineException
    {
        //        // BINGO to be removed ASAP
        //        final List<String> opts = new ArrayList<>();
        //        opts.add("-xsl");
        //        opts.add("xs3p.xsl");
        //        opts.add("-s");
        //        opts.add("build/output/Sheet.xsd");
        //        opts.add("-o");
        //        opts.add("build/output/Sheet.html");
        //        opts.add("--");
        //        opts.add("title='Sheet-rooted Schema Documentation'");
        //        opts.add("xmlDocWidth=50");
        //        opts.add("printGlossary=false");
        //        opts.add("printNSPrefixes=false");
        //        opts.add("useTypeShortcut=true");
        //        opts.add("useMarkdown=false");
        //        //opts.add("debug=true");
        //        args = opts.toArray(new String[opts.size()]);

        /** Parameters structure. */
        final Parameters params = parseParameters(args);

        final String xsltResource = convertToString(
                new FileInputStream(params.styleSheetPath.toFile()));
        final String xmlSourceResource = convertToString(
                new FileInputStream(params.sourcePath.toFile()));
        final StringWriter xmlResultResource = new StringWriter();
        final StreamResult result = new StreamResult(xmlResultResource);

        final TransformerFactory factory = TransformerFactory.newInstance();
        final Transformer xmlTransformer = factory.newTransformer(
                new StreamSource(new StringReader(xsltResource)));

        // Set transformer parameters
        params.arguments.forEach(str -> {
            final String[] tokens = str.split("\\s*=\\s*");
            final String name = tokens[0].trim();
            final String value = tokens[1].trim().replaceAll("'", "");
            xmlTransformer.setParameter(name, value);
        });

        xmlTransformer.transform(new StreamSource(new StringReader(xmlSourceResource)), result);

        try {
            final FileWriter writer = new FileWriter(params.outputPath.toFile());
            try (BufferedWriter bufferedWriter = new BufferedWriter(writer)) {
                bufferedWriter.write(xmlResultResource.getBuffer().toString());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        ///System.out.println(xmlResultResource.getBuffer().toString());
    }

    //-----------------//
    // parseParameters //
    //-----------------//
    /**
     * Parse the CLI arguments and return the populated parameters structure.
     *
     * @param args the CLI arguments
     * @return the parsed parameters, or null if failed
     * @throws org.kohsuke.args4j.CmdLineException if error found in arguments
     */
    private static Parameters parseParameters (final String... args)
        throws CmdLineException
    {
        //        final String[] args = new String[]
        //        {
        //                "-help",
        //                "-xsl",
        //                "xs3p.xsl",
        //                "-s",
        //                "build/output/PlayList.xsd",
        //                "-o",
        //                "build/output/PlayList.html",
        //                "--",
        //                "title='PlayList-rooted Schema Documentation'",
        //                "debug=true" };
        System.out.println(String.format("CLI args: %s", Arrays.toString(args)));

        /** Sequence of (trimmed) arguments for this run. */
        // Bug fix if an arg is made of spaces
        String[] trimmedArgs = new String[args.length];

        for (int i = 0; i < args.length; i++) {
            trimmedArgs[i] = args[i].trim();
        }

        /** CLI parser. */
        final Comparator<OptionHandler> noSorter = (o1,
                                                    o2) -> 0;
        final ParserProperties props = ParserProperties.defaults().withAtSyntax(true)
                .withUsageWidth(100).withShowDefaults(false).withOptionSorter(noSorter);

        final Parameters params = new Parameters();
        final CmdLineParser parser = new CmdLineParser(params, props);

        parser.parseArgument(trimmedArgs);

        if (params.helpMode) {
            final StringWriter writer = new StringWriter();
            parser.printUsage(writer, null);
            System.out.println(writer.getBuffer());
        }

        return params;
    }

    //~ Inner classes ------------------------------------------------------------------------------

    //------------//
    // Parameters //
    //------------//
    /**
     * The structure that collects the various parameters parsed out of the command line.
     * def opts = []
     * opts.add("-xsl:${styleSheet}")
     * opts.add("-s:build/output/${cls}.xsd")
     * opts.add("-o:build/output/${cls}.html")
     * opts.add("title=${cls}-rooted Schema Documentation")
     * opts.add("printGlossary=false")
     * opts.add("printNSPrefixes=false")
     * opts.add("xmlDocWidth=60")
     * opts.add("useTypeShortcut=true")
     * opts.add("useMarkdown=false")
     * opts.add("debug=true")
     */
    private static class Parameters
    {
        /** Help mode. */
        @Option(name = "-help", help = true, usage = "Display general help then stop")
        boolean helpMode;

        /** Path to the style sheet. */
        @Option(name = "-xsl", usage = "Path to the style sheet", metaVar = "<file.xsl>")
        Path styleSheetPath;

        /** Path to the source file. */
        @Option(name = "-s", usage = "Path to the source file", metaVar = "<file.xsd>")
        Path sourcePath;

        /** Path to the output file. */
        @Option(name = "-o", usage = "Path to the output file", metaVar = "<file.html>")
        Path outputPath;

        /** Optional "--" separator. */
        @Argument
        @Option(name = "--", handler = StopOptionHandler.class)

        /** Final arguments, as a collection of name=value. */
        List<String> arguments = new ArrayList<>();
    }
}
