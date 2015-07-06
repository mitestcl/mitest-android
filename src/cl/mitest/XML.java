package cl.mitest;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class XML {
	
	private Document dom = null;
	
	public Document getDom () {
		return dom;
	}
	
	public XML (InputStream in) {
		this.loadXML(in);
	}

	public void loadXML (InputStream in) {
		DocumentBuilderFactory factory = null;
		DocumentBuilder builder = null;
		try {
			factory = DocumentBuilderFactory.newInstance();
			builder = factory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}
		try {
			this.dom = builder.parse(new InputSource(in));
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public String getFirstNodeValue (String tag) {
		return this.getNodeValue(this.dom.getElementsByTagName(tag).item(0));
	}
	
	public String getNodeValue (Node nodo) {
		StringBuilder texto = new StringBuilder();
		NodeList fragmentos = nodo.getChildNodes();
        for (int k=0;k<fragmentos.getLength();k++) {
        	texto.append(fragmentos.item(k).getNodeValue());
        }
        return texto.toString();
    }

}
