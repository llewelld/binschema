#include "api.h"
#include "pole.h"
#include <QtCore/QDebug>

void
testFile(const char* path) {
    POLE::Storage storage(path);
    if (!storage.open()) return;
    std::string prefix;
    if (storage.isDirectory("PP97_DUALSTORAGE")) {
        prefix = "PP97_DUALSTORAGE/";
    } else {
        prefix = "/";
    }
    POLE::Stream stream(&storage, prefix + "PowerPoint Document");

    QByteArray array;
    array.resize(stream.size());
    unsigned long read = stream.read((unsigned char*)array.data(), stream.size());
    if (read != stream.size()) {
        qDebug() << "Error reading stream ";
        return;
    }
    MSO::PowerPointStructs s(array.data(), array.size());
    qDebug() << "Parsed " << s._size << " of " << array.size() << ": "
        << ((s._size == array.size())?"OK":"FAIL");
    if (s._size != array.size()) {
        qDebug() << path;
    }
}
int
main(int argc, char** argv) {
    for (int i=1; i<argc; ++i) {
        testFile(argv[i]);
    }
    return 0;
}
