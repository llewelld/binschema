package mso.generator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

class MSO {
	List<Struct> structs = new ArrayList<Struct>();
	List<Stream> streams = new ArrayList<Stream>();

	MSO(Document dom) throws IOException {
		List<Element> orderedElements = ParserGeneratorUtils
				.getOrderedStructureList(dom);
		for (Element e : orderedElements) {
			structs.add(new Struct(e));
		}

		NodeList l = dom.getElementsByTagName("stream");
		for (int i = 0; i < l.getLength(); ++i) {
			streams.add(new Stream((Element) l.item(i)));
		}
	}
}

class Limitation {
	final String name;
	final String expression;
	final String value;

	Limitation(Element e) {
		name = e.getAttribute("name");
		if (e.hasAttribute("expression")) {
			expression = e.getAttribute("expression");
			value = null;
		} else {
			expression = null;
			value = e.getAttribute("value");
		}
	}
}

class Member {
	final String name;
	final String type;
	final String count;
	final String size;
	final String condition;
	final boolean isArray;
	final boolean isOptional;
	final boolean isComplex;
	final boolean isInteger;
	final String choices[];
	final Limitation limitations[];

	Member(Element e) {
		name = e.getAttribute("name");
		condition = (e.hasAttribute("condition")) ? e.getAttribute("condition")
				: null;
		count = (e.hasAttribute("count")) ? e.getAttribute("count") : null;
		size = (e.hasAttribute("size")) ? e.getAttribute("size") : null;

		List<String> _choices = null;
		isOptional = e.hasAttribute("optional");
		isArray = count != null || e.hasAttribute("array");
		isComplex = e.hasAttribute("type");
		if (isComplex) {
			type = e.getAttribute("type");
			isInteger = false;
		} else {
			type = e.getNodeName();
			isInteger = type.startsWith("int") || type.startsWith("uint");
			if (type.equals("choice")) {
				_choices = new ArrayList<String>();
				NodeList l = e.getElementsByTagName("type");
				for (int i = 0; i < l.getLength(); ++i) {
					_choices.add(((Element) l.item(i)).getAttribute("type"));
				}
			}
		}

		List<Limitation> _limitations = new ArrayList<Limitation>();
		NodeList l = e.getChildNodes();
		for (int i = 0; i < l.getLength(); ++i) {
			if (l.item(i) instanceof Element) {
				_limitations.add(new Limitation((Element) l.item(i)));
			}
		}
		choices = (_choices == null) ? null : _choices.toArray(new String[0]);
		limitations = _limitations.toArray(new Limitation[0]);
	}
}

class Struct {

	String name;
	final List<Member> members = new ArrayList<Member>();
	final boolean containsArrayMember;
	final boolean containsOptionalMember;
	final boolean containsUnknownLengthArrayMember;
	final boolean containsKnownLengthArrayMember;

	Struct(Element e) {
		name = e.getAttribute("name");

		boolean _containsArrayMember = false;
		boolean _containsOptionalMember = false;
		boolean _containsUnknownLengthArrayMember = false;
		boolean _containsKnownLengthArrayMember = false;
		NodeList l = e.getChildNodes();
		for (int i = 0; i < l.getLength(); ++i) {
			Node n = l.item(i);
			if (n instanceof Element) {
				Member m = new Member((Element) n);
				members.add(m);
				_containsArrayMember = _containsArrayMember || m.isArray;
				_containsOptionalMember = _containsOptionalMember
						|| m.isOptional;
				_containsUnknownLengthArrayMember = _containsUnknownLengthArrayMember
						|| (m.isArray && m.count == null);
				_containsKnownLengthArrayMember = _containsKnownLengthArrayMember
						|| m.count != null;
			}
		}
		containsArrayMember = _containsArrayMember;
		containsOptionalMember = _containsOptionalMember;
		containsUnknownLengthArrayMember = _containsUnknownLengthArrayMember;
		containsKnownLengthArrayMember = _containsKnownLengthArrayMember;
	}
}

class Stream {

	final String key;
	final String type;

	Stream(Element e) {
		key = e.getAttribute("key");
		type = e.getAttribute("type");
	}
}

public class ParserGeneratorUtils {

	static Collection<String> getDependencies(Element e) {
		Set<String> deps = new TreeSet<String>();
		NodeList l = e.getElementsByTagName("type");
		for (int i = 0; i < l.getLength(); ++i) {
			Element se = (Element) l.item(i);
			if (!se.hasAttribute("count") && !se.hasAttribute("array")) {
				deps.add(se.getAttribute("type"));
			}
		}
		return deps;
	}

	static List<Element> getOrderedStructureList(Document dom)
			throws IOException {

		List<Element> unorderedList = new ArrayList<Element>();
		NodeList l = dom.getElementsByTagName("struct");
		for (int i = 0; i < l.getLength(); ++i) {
			unorderedList.add((Element) l.item(i));
		}

		List<String> done = new ArrayList<String>();
		List<Element> orderedList = new ArrayList<Element>();
		while (unorderedList.size() > 0) {
			int count = unorderedList.size();
			for (int i = 0; i < unorderedList.size();) {
				Element e = unorderedList.get(i);
				Collection<String> deps = getDependencies(e);
				deps.removeAll(done);
				if (deps.size() == 0) {
					orderedList.add(e);
					done.add(e.getAttribute("name"));
					unorderedList.remove(e);
				} else {
					i++;
				}
			}
			if (count == unorderedList.size()) {
				String msg = "";
				for (Element e : unorderedList) {
					Collection<String> deps = getDependencies(e);
					deps.removeAll(done);
					msg = msg + e.getAttribute("name") + ": " + deps + "\n";
				}
				throw new IOException(msg + count);
			}
		}
		return orderedList;
	}

	static boolean hasElementWithAttribute(Element e, String attribute) {
		NodeList l = e.getChildNodes();
		for (int i = 0; i < l.getLength(); ++i) {
			Node n = l.item(i);
			if (n instanceof Element && ((Element) n).hasAttribute(attribute)) {
				return true;
			}
		}
		return false;
	}

	static Map<Integer, String> getRecordTypeNames(Document dom)
			throws XPathExpressionException {
		Map<Integer, String> map = new HashMap<Integer, String>();

		XPath xpath = XPathFactory.newInstance().newXPath();
		String expression = "/mso/struct/type[@name='rh']/limitation[@name='recType']";
		NodeList list = (NodeList) xpath.evaluate(expression, dom,
				XPathConstants.NODESET);

		for (int i = 0; i < list.getLength(); ++i) {
			Element e = (Element) list.item(i);
			String type = e.getAttribute("value").replace("0x", "");
			for (String s : type.split("\\|")) {
				int typeNumber = Integer.parseInt(s, 16);
				String name = ((Element) e.getParentNode().getParentNode())
						.getAttribute("name");
				if (map.containsKey(typeNumber)) {
					name = map.get(typeNumber) + "/" + name;
				}
				map.put(typeNumber, name);
			}
		}
		return map;
	}

}
