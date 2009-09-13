#ifndef INTROSPECTION_H
#define INTROSPECTION_H

#include <QtCore/QVariant>

class Introspectable;

class Introspection {
public:
    const QString name;
    const int numberOfMembers;
    const QString* const names;
    int (* const * const numberOfInstances)(const Introspectable*);
    QVariant (* const * const value)(const Introspectable*, int position);
    const Introspectable* (* const * const introspectable)(const Introspectable*, int position);

    Introspection(const QString& n,
            const int nOM,
            const QString* const ns,
            int (* const * const mC)(const Introspectable*),
            QVariant (* const * const vG)(const Introspectable*, int),
            const Introspectable* (* const * const i)(const Introspectable*, int))
        :name(n), numberOfMembers(nOM), names(ns),
         numberOfInstances(mC), value(vG), introspectable(i) {}

    // convenience
    static int zero(const Introspectable*) { return 0; }
    static int one(const Introspectable*) { return 1; }
    static const Introspectable* null(const Introspectable*, int) { return NULL; } 
    static QVariant nullValue(const Introspectable*, int) { return QVariant(); }
};

class Introspectable {
public:
    virtual ~Introspectable() {}
    virtual const Introspection* getIntrospection() const = 0;
};


#endif
