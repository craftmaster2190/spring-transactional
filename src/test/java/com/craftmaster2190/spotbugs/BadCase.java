package com.craftmaster2190.spotbugs;

class BadCase {
    private final MyEntity myEntity = new MyEntity();

    void nonTransactionalMethod_lazy() {
        myEntity.getLazy();
        getLazy_nestedMethod();
    }

    void getLazy_nestedMethod() {
        myEntity.getLazy();
    }
}
