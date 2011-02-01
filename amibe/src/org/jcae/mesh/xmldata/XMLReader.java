/*
 * Project Info:  http://jcae.sourceforge.net
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 *
 * (C) Copyright 2009, by EADS France
 */


package org.jcae.mesh.xmldata;

import java.io.File;
import java.io.IOException;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *
 * @author Jerome Robert
 */
public abstract class XMLReader {
	private final static Logger LOGGER = Logger.getLogger(XMLReader.class.getName());
	/** 
	 * Wrap a w3c NodeList to make iterable.
	 * This should realy be in the standard.
	 */
	public static class NodeListI<T extends Node> extends AbstractList<T>
	{
		private final NodeList n;
		public NodeListI(XPathExpression xp, Object item) throws XPathExpressionException
		{
			n = (NodeList) xp.evaluate(item, XPathConstants.NODESET);
		}

		public NodeListI(Object n)
		{
			this.n = (NodeList)n;
		}

		@Override
		@SuppressWarnings("unchecked")
		public T get(int index)
		{
			return (T) n.item(index);
		}

		@Override
		public int size()
		{
			return n.getLength();
		}
	}

	protected static Element getElement(Element root, String ... tags)
	{
		if(root == null)
			return null;
		List<Element> r = getElements(root, tags);
		if(r.isEmpty())
			return null;
		else
			return r.get(0);
	}
	
	protected static List<Element> getElements(Element e, String ... tag)
	{
		if(e == null)
			return Collections.emptyList();
		List<Element> currentLevel = new ArrayList<Element>();
		List<Element> nextLevel = new ArrayList<Element>();
		List<Element> tmp;
		currentLevel.add(e);
		for(String s:tag)
		{
			for(Element ee:currentLevel)
				nextLevel.addAll(getElementsOnLevel(ee, s));
			currentLevel.clear();
			tmp = currentLevel;
			currentLevel = nextLevel;
			nextLevel = tmp;
		}
		return currentLevel;
	}
	
	private static List<Element> getElementsOnLevel(Element e, String tag)
	{
		List<Element> toReturn = new ArrayList<Element>();
		NodeList l = e.getChildNodes();
		for(int i = 0; i<l.getLength(); i++)
		{
			Node n = l.item(i);
			if( n instanceof Element)
			{
				Element el = (Element) n;
				if(tag.equals(el.getTagName()))
					toReturn.add(el);
			}
		}
		return toReturn;
	}

	protected abstract void read(Document dom) throws SAXException, XPathExpressionException, IOException;

	public void read(File in) throws SAXException, IOException
	{
		Document dom = createDocument(in);
		if(getXSD() != null)
			getValidator().validate(new DOMSource(dom));
		try
		{
			read(dom);
		}
		catch (XPathExpressionException ex)
		{
			LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
		}
	}

	private static Document createDocument(File f) throws IOException, SAXException
	{
		try
		{
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(true);
			factory.setValidating(false);
			DocumentBuilder builder = factory.newDocumentBuilder();
			// Needed to be able to read files generated by jCAE < 0.17
			builder.setEntityResolver(new ClassPathEntityResolver());
			Document toReturn = builder.parse(f);
			toReturn.normalize();
			return toReturn;
		}
		catch (ParserConfigurationException ex)
		{
			LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
			return null;
		}
	}

	private Validator getValidator() throws SAXException
	{
		Schema xsd = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).
			newSchema(getClass().getResource(getXSD()));
		return xsd.newValidator();
	}

	protected abstract String getXSD();

}
