package mso.generator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class QtApiGenerator {

	public class QtApiConfiguration {
		public String namespace;
		public String outputdir;
		public String basename;
		public boolean createHeader;
	}

	final public QtApiConfiguration config = new QtApiConfiguration();

	final private String generatedWarning = "/* This code was generated by msoscheme (http://gitorious.org/msoscheme) */";

	private String type(Member m) {
		return type(m.type());
	}

	private String type(TypeRegistry.Type t) {
		TypeRegistry r = t.registry;
		if (t == r.bit) {
			return "bool";
		} else if (t == r.uint2 || t == r.uint3 || t == r.uint4 || t == r.uint5
				|| t == r.uint6 || t == r.uint7 || t == r.uint8) {
			return "quint8";
		} else if (t == r.uint9 || t == r.uint12 || t == r.uint14
				|| t == r.uint15 || t == r.uint16) {
			return "quint16";
		} else if (t == r.uint20 || t == r.uint30 || t == r.uint32) {
			return "quint32";
		} else if (t == r.int16) {
			return "qint16";
		} else if (t == r.int32) {
			return "qint32";
		}
		return t.name;
	}

	void generate(MSO mso) throws IOException {
		FileWriter fout;
		if (config.createHeader) {
			fout = new FileWriter(config.outputdir + File.separator
					+ config.basename + ".h");
		} else {
			fout = new FileWriter(config.outputdir + File.separator
					+ config.basename + ".cpp");
		}
		PrintWriter out = new PrintWriter(fout);
		out.println(generatedWarning);
		if (config.createHeader) {
			out.println("#ifndef " + config.basename.toUpperCase() + "_H");
			out.println("#define " + config.basename.toUpperCase() + "_H");
		}
		// out.println("#include <QtCore/QString>");
		// out.println("#include <QtCore/QByteArray>");
		// out.println("#include <QtCore/QVector>");
		out.println("#include \"leinput.h\"");
		out.println("namespace " + config.namespace + "{");

		for (Struct s : mso.structs) {
			out.println("class " + s.name + ";");
		}
		for (Choice s : mso.choices) {
			out.println("class " + s.name + ";");
		}
		for (Struct s : mso.structs) {
			printStructureClassDeclaration(out, s);
		}
		for (Choice s : mso.choices) {
			printChoiceClassDeclaration(out, s);
		}

		if (config.createHeader) {
			out.println("} // close namespace");
			out.println("#endif");
			out.close();
			fout = new FileWriter(config.outputdir + File.separator
					+ config.basename + ".cpp");
			out = new PrintWriter(fout);
			out.println(generatedWarning);
			out.println("#include \"" + config.basename + ".h\"");
			// out.println("using namespace " + config.namespace + ";");
		}

		for (Struct s : mso.structs) {
			printStructureDefinition(out, s);
		}

		if (!config.createHeader) {
			out.println("}");// close namespace
		}
		out.close();
		fout.close();
	}

	private void printStructureClassDeclaration(PrintWriter out, Struct s) {
		out.println("class " + s.name + " {");
		out.println("public:");
		out.println("    const char* _data;");
		if (s.size == -1) {
			out.println("    quint32 _size;");
			out.println("    " + s.name + "() :_data(0), _size(0) {}");
			out.println("    " + s.name
					+ "(const char* data, const quint32 size);");
		} else {
			out.println("    static const quint32 _size;");
			out.println("    " + s.name + "() :_data(0) {}");
			out.println("    " + s.name + "(const char* data);// "
					+ (s.size / 8) + " bytes");
		}
		for (Member m : s.members) {
			printMemberDeclaration(out, m);
		}
		for (Member m : s.members) {
			if (m.isSimple && m.condition != null) {
				out.println("    bool _has_" + m.name + ";");
			}
		}
		out.println("};");
	}

	private void printChoiceClassDeclaration(PrintWriter out, Choice s) {
		// TODO
		out.println("class " + s.name + " {");
		out.println("public:");
		out.println("    const char* _data;");
		if (s.size == -1) {
			out.println("    quint32 _size;");
			out.println("    " + s.name + "() :_data(0), _size(0) {}");
			out.println("    " + s.name
					+ "(const char* data, const quint32 size);");
		} else {
			out.println("    static const quint32 _size;");
			out.println("    " + s.name + "() :_data(0) {}");
			out.println("    " + s.name + "(const char* data);// "
					+ (s.size / 8) + " bytes");
		}
		out.println("};");
	}

	private String definitionPrefix() {
		return (config.namespace == null || config.namespace.length() == 0) ? ""
				: config.namespace + "::";
	}

	private void printStructureDefinition(PrintWriter out, Struct s) {
		// constructor
		String c = definitionPrefix() + s.name + "::";
		if (s.size == -1) {
			out.println(c + s.name
					+ "(const char* _d, quint32 _maxsize) :_data(0), _size(0)");
			out.println("{");
		} else {
			out.println("const quint32 " + c + "_size = " + (s.size / 8) + ";");
			out.println(c + s.name + "(const char* _d) :_data(0)");
			out.println("{");
		}
		out.println("    quint32 _position = 0;");
		out.println("    quint32 _msize;");
		int bytepos = 0;
		String sp;
		for (Member m : s.members) {
			sp = "    ";
			int msize = getSize(m);
			String condition = m.condition;
			if (m.isSimple && condition != null) {
				out.println(sp + "_has_" + m.name + " = " + condition + ";");
				condition = "_has_" + m.name;
			}
			if (condition != null) {
				out.println(sp + "if (" + condition + ") {");
				sp = "        ";
			}
			if (msize != -1 && s.size == -1) {
				out.println(sp + "if (_position + " + msize
						+ " > _maxsize) return;");
			}
			bytepos = printMemberParser(out, sp, s, m, bytepos);
			printLimitationCheck(out, sp, m.name, m);
			out.println(sp + "_position += _msize;");
			if (condition != null) {
				out.println("    }");
			}
		}
		if (s.size == -1) {
			out.println("    _size = _position;");
		}
		out.println("    _data = _d;");
		out.println("}");
		// access functions
		for (Member m : s.members) {
			if (m.isArray && m.isStruct) {
				printStructArrayAccessor(out, s, m);
			}
		}
	}

	private int printMemberParser(PrintWriter out, String sp, Struct s,
			Member m, int bytepos) {
		if (m.isArray) {
			if (m.isSimple) {
				printSimpleArrayMemberParser(out, sp, s, m);
			} else {
				printStructArrayMemberParser(out, sp, s, m);
			}
			if (m.isChoice) {
				throw new Error("There should be no choice in an array.");
			}
		} else {
			if (m.isSimple) {
				return printSimpleMemberParser(out, sp, s, m, bytepos);
			} else if (m.isStruct) {
				printStructMemberParser(out, sp, s, m);
			} else if (m.isChoice) {
				printChoiceMemberParser(out, sp, s, m);
			}
		}
		return 0;
	}

	private int printSimpleMemberParser(PrintWriter out, String sp, Struct s,
			Member m, int bytepos) {
		out.println(sp + m.name + " = read" + m.type().name
				+ ((bytepos > 0) ? "_" + String.valueOf(bytepos) : "")
				+ "(_d + _position);");
		int size = m.type().size / 8;
		if (bytepos != 0 && ((bytepos + m.type().size) % 8) == 0) {
			size += 1;
		}
		bytepos = (bytepos + m.type().size) % 8;
		out.println(sp + "_msize = " + size + ";");
		return bytepos;
	}

	/** return size in bytes **/
	private int getSize(Member m) {
		int size = -1;
		if (m.count != null && m.isArray && m.type().size != -1) {
			try {
				size = m.type().size * Integer.parseInt(m.count) / 8;
			} catch (NumberFormatException e) {
				size = -1;
			}
		} else if (m.size != null) {
			try {
				size = Integer.parseInt(m.size);
			} catch (NumberFormatException e) {
				size = -1;
			}
		} else if (!m.isArray) {
			return m.type().size / 8;
		}
		return size;
	}

	private void printSimpleArrayMemberParser(PrintWriter out, String sp,
			Struct s, Member m) {
		if (m.type().size % 8 != 0) {
			throw new Error(
					"only arrays of types with a size of a multiple of 8 bits are allowed.");
		}
		// get the expression for the size
		int size = getSize(m);
		String count = String.valueOf(size / m.type().size);
		if (size == -1) {
			if (m.count != null) {
				count = m.count;
			} else {
				count = "(" + m.size + ")/" + (m.type().size / 8);
			}
		} else {
			if (size % (m.type().size / 8) != 0) {
				throw new Error(
						"array size must be a multiple of the size of the type for "
								+ m.type().name + " and size " + size);
			}
		}
		out.println(sp + m.name + "Count = " + count + ";");
		out.println(sp + "_msize = " + m.name + "Count*" + (m.type().size / 8)
				+ ";");
		out.println(sp + m.name + " = (const " + type(m)
				+ "*)(_d + _position);");
	}

	private void printStructArrayMemberParser(PrintWriter out, String sp,
			Struct s, Member m) {
		if (m.count != null || m.size != null) {
			printStructFixedArrayMemberParser(out, sp, s, m);
			return;
		}
		// parser for when size is not know: parse till the first fail
		out.println(sp + "_msize = 0;");
		out.println(sp + m.name + "Count = 0;");
		out.println(sp + "while (_position < _maxsize) {");
		if (m.type().size == -1) {
			out
					.println(sp
							+ "    "
							+ m.type().name
							+ " _v(_d + _position + _msize, _maxsize - _position - _msize);");
		} else {
			if (s.size == -1) {
				out.println(sp + "    if (_maxsize - _position - _msize < "
						+ (m.type().size / 8) + ") return;");
			}
			out.println(sp + "    " + m.type().name
					+ " _v(_d + _position + _msize);");
		}
		out.println(sp + "    if (_v._data == 0) break;");
		out.println(sp + "    _msize += _v._size;");
		out.println(sp + "    " + m.name + "Count++;");
		out.println(sp + "}");
	}

	private void printStructFixedArrayMemberParser(PrintWriter out, String sp,
			Struct s, Member m) {
		out.println(sp + "_msize = 0;");
		if (m.count != null) {
			out.println(sp + m.name + "Count = " + m.count + ";");
			out.println(sp + "for (quint32 i=0; i<" + m.name + "Count; ++i) {");
		} else {
			out.println(sp + m.name + "Count = 0;");
			out.println(sp + "quint32 _lsize = " + m.size + ";");
			out.println(sp + "while (_msize < _lsize) {");
		}
		if (m.type().size == -1) {
			out
					.println(sp
							+ "    "
							+ m.type().name
							+ " _v(_d + _position + _msize, _maxsize - _position - _msize);");
		} else {
			if (s.size == -1) {
				out.println(sp + "    if (_maxsize - _position - _msize < "
						+ (m.type().size / 8) + ") return;");
			}
			out.println(sp + "    " + m.type().name
					+ " _v(_d + _position + _msize);");
		}
		out.println(sp + "    if (_v._data == 0) return;");
		out.println(sp + "    _msize += _v._size;");
		if (m.count != null) {
			out.println(sp + "}");
		} else {
			out.println(sp + "    " + m.name + "Count++;");
			out.println(sp + "}");
			out.println(sp + "if (_msize != _lsize) return;");
		}
	}

	private void printStructMemberParser(PrintWriter out, String sp, Struct s,
			Member m) {
		if (m.type().size == -1) {
			out.println(sp + m.name + " = " + m.type().name
					+ "(_d + _position, _maxsize - _position);");
		} else {
			out.println(sp + m.name + " = " + m.type().name
					+ "(_d + _position);");
		}
		if (m.isOptional) {
			out.println(sp + "_msize = (" + m.name + "._data) ?" + m.name
					+ "._size :0;");
		} else {
			out.println(sp + "if (" + m.name + "._data == 0) return;");
			out.println(sp + "_msize = " + m.name + "._size;");
		}
	}

	private void printChoiceMemberParser(PrintWriter out, String sp, Struct s,
			Member m) {
		Choice c = (Choice) m.type();
		// try all options until one has non-zero size
		// String s = "";
		boolean first = true;
		for (Option o : c.options) {
			TypeRegistry.Type t = o.type;
			if (!first) {
				out.println(sp + "if (_msize == 0) {");
				out.print(sp);
			}
			if (t.size == -1) {
				out.println(sp + "_msize = " + t.name
						+ "(_d + _position, _maxsize - _position)._size;");
			} else {
				out.println(sp + "_msize = (" + t.name
						+ "(_d + _position)._data) ?" + t.name
						+ "::_size :0;");
			}
			if (!first) {
				out.println(sp + "}");
			}
			first = false;
		}
		if (!m.isOptional) {
			out.println(sp + "if (_msize == 0) return;");
		}
	}

	private String getTypeName(TypeRegistry.Type t) {
		TypeRegistry r = t.registry;
		if (t instanceof Choice) {
			return t.name;
		} else if (t == r.bit) {
			return "bool";
		} else if (t == r.uint2 || t == r.uint3 || t == r.uint4 || t == r.uint5
				|| t == r.uint6 || t == r.uint7 || t == r.uint8) {
			return "quint8";
		} else if (t == r.uint9 || t == r.uint12 || t == r.uint14
				|| t == r.uint15 || t == r.uint16) {
			return "quint16";
		} else if (t == r.uint20 || t == r.uint30 || t == r.uint32) {
			return "quint32";
		} else if (t == r.int16) {
			return "qint16";
		} else if (t == r.int32) {
			return "qint32";
		}
		return t.name;
	}

	private void printStructArrayAccessor(PrintWriter out, TypeRegistry.Type s,
			Member m) {
		String c = definitionPrefix();
		String t = m.type().name;
		out.println(c + t + " " + c + s.name + "::" + m.name
				+ "(quint32 _i) const {");
		if (m.type().size == -1) {
			out.println("    " + c + t + " _v(_data + " + m.name
					+ "Offset, _size-" + m.name + "Offset);");
			out.println("    quint32 _offset = " + m.name
					+ "Offset + _v._size;");
			out.println("    while (_i-- && _v._size) {");
			out.println("        _v = " + c + t
					+ "(_data + _offset, _size - _offset);");
			out.println("        _offset += _v._size;");
			out.println("    }");
			out.println("    return _v;");
		} else {
			out.println("    if (_i >= " + m.name + "Count) return " + c + t
					+ "();");
			out.print("    return " + c + t + "(_data + " + m.name
					+ "Offset + _i*" + (m.type().size / 8) + ");");
			out.println("    ");
		}
		out.println("}");
	}

	private void printMemberDeclaration(PrintWriter out, Member m) {
		String t = getTypeName(m.type());
		if (m.isArray) {
			out.println("    quint32 " + m.name + "Count;");
			if (m.isSimple) {
				out.println("    const " + t + "* " + m.name + ";");
			}
			if (m.isStruct || m.isChoice) {
				out.println("    quint32 " + m.name + "Offset;");
				out.println("    " + t + " " + m.name + "(quint32 i) const;");
			}
		} else if (m.isChoice) {
			out.println("    " + t + " " + m.name + "() const;");
		} else {
			out.println("    " + t + " " + m.name + ";");
		}
	}

	private void printLimitationCheck(PrintWriter out, String s, String name,
			Member m) {
		if (m.type() instanceof Choice)
			return;
		for (Limitation l : m.limitations) {
			String mname = l.name;
			if (!"".equals(mname)) {
				mname = name + "." + mname;
			} else {
				mname = name;
			}
			if (!m.isStruct) {
				mname = "((" + getTypeName(m.type()) + ")" + mname + ")";
			}
			String condition = l.expression;
			if (condition == null) {
				condition = QtParserGenerator.getCondition(mname, l);
			} else {
				condition = QtParserGenerator.getExpression(mname, condition);
			}

			out.println(s + "if (!(" + condition + ")) {");
			out.println(s + "     return;");
			out.println(s + "}");
		}
	}
}
