package com.controls.train.app;

import android.content.Context;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class SvgParser {

    // Represents a parsed path element from the SVG
    public static class TrackPath {
        public String id;
        public String name;      // from inkscape:label or a custom "name" attribute
        public String fillColor;

        @Override
        public String toString() {
            return "TrackPath{id='" + id + "', name='" + name + "'}";
        }
    }

    /**
     * Parses an SVG file from assets and returns a list of TrackPath objects.
     * Call once at startup — DocumentBuilder loads the full file into memory.
     */
    public static List<TrackPath> parse(Context context, String svgFileName) {
        List<TrackPath> paths = new ArrayList<>();

        try {
            // Open the SVG from assets
            InputStream is = context.getAssets().open(svgFileName);

            // Build the DOM document
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true); // needed for inkscape: / xlink: attributes
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(is);
            doc.getDocumentElement().normalize();

            // Query all <path> elements
            NodeList pathNodes = doc.getElementsByTagName("path");

            for (int i = 0; i < pathNodes.getLength(); i++) {
                Node node = pathNodes.item(i);
                if (node.getNodeType() != Node.ELEMENT_NODE) continue;

                Element el = (Element) node;
                TrackPath track = new TrackPath();

                // Standard SVG attributes
                track.id          = el.getAttribute("id");
                track.fillColor   = el.getAttribute("fill");

                // Inkscape uses inkscape:label for human-readable names
                // Try that first, fall back to id
                String inkscapeLabel = el.getAttributeNS(
                        "http://www.inkscape.org/namespaces/inkscape", "label"
                                                        );
                track.name = (inkscapeLabel != null && !inkscapeLabel.isEmpty())
                        ? inkscapeLabel
                        : track.id;

                // Only add paths that have an id
                if (track.id != null && !track.id.isEmpty()) {
                    paths.add(track);
                }
            }

            is.close();

        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }

        return paths;
    }
}
