package org.brewchain.cvm.exec.zksnark;

interface Field<T> {

    T add(T o);
    T mul(T o);
    T sub(T o);
    T squared();
    T dbl();
    T inverse();
    T negate();
    boolean isZero();
    boolean isValid();
}
