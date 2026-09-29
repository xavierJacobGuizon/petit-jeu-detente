package org.jeuroute.configuration;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

public final class XmlResourceCache<T> {

	private final Function<Element, T> parser;
	private final Map<String, T> cache = new HashMap<>();

	public XmlResourceCache(Function<Element, T> parser) {
		this.parser = Objects.requireNonNull(parser, "parser cannot be null");
	}

	public T getOrLoad(String resourcePath) {
		Objects.requireNonNull(resourcePath, "resourcePath cannot be null");
		return cache.computeIfAbsent(resourcePath, this::loadResource);
	}

	private T loadResource(String resourcePath) {
		try (InputStream stream = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
			if (stream == null) {
				throw new IllegalArgumentException("Resource not found: " + resourcePath);
			}
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
			factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
			factory.setXIncludeAware(false);
			factory.setExpandEntityReferences(false);
			Document document = factory.newDocumentBuilder().parse(stream);
			return parser.apply(document.getDocumentElement());
		} catch (IOException | ParserConfigurationException | SAXException exception) {
			throw new IllegalStateException(
				"Unable to read XML resource: " + resourcePath,
				exception
			);
		}
	}
}
