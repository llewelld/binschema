# MSOScheme

MSOScheme generates code to read Microsoft Office files

The code is generated from a computer readable description of the Microsoft Office file format. This description is in the file `src/mso.xml` and follows the XML Schema in `src/mso.xsd`.

MSOScheme can generate code for any project. To write a custom code generator for your project, add a custom generator to the class `mso.generator.ParserGeneratorRunner`.

MSOScheme comes with three code generators:

JavaParserGenerator
: creates `mso.javaparser.GeneratedMsoParser`
  This parser parses from a `byte[]` that is passed to `LEInputStream`.

QtParserGenerator
: creates `simpleParser.h`, `simpleParser.cpp` and `generatedclasses.cpp`
  `simpleParser.h` and `simpleParser.cpp` are used by Calligra.
  The generated code can parse by reading from a `QIODevice` that is passed
  into a `LEInputStream`.

QtApiGenerator
: creates `api.h` and `api.cpp`
  The generated code parses files passed as a `const char*`.

## Building

### `mso.jar`

The code generator is compiled into a jar file `mso.jar`, with
```bash
ant mso.jar
```
This step also runs the code generators that are shipped with MSOScheme.

### C++

Builing the C++ code requires cmake, zlib, xml2, libxslt, make and Qt 4.8.

```bash
ant generateParsers
mkdir build
cd build
cmake ../cpp
make
```

This creates executables for testing the generated code on binary Microsoft Office documents.

## License

MSOScheme is licensed as GNU Library General Public License v2.
MSOScheme uses Apache POI which is licensed under Apache License Version 2.0.
The files generated by MSOScheme are not under any license.
