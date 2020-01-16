package com.craftmaster2190.spotbugs;

class GoodCase {
    private final MyEntity myEntity = new MyEntity();

    @org.springframework.transaction.annotation.Transactional
    @javax.transaction.Transactional
    void transactionalMethod_lazy() {
        myEntity.getLazy();
        getLazy_nestedMethod();
    }

    @org.springframework.transaction.annotation.Transactional
    @javax.transaction.Transactional
    void transactionalMethod_eager() {
        myEntity.getEager();
        getLazy_nestedMethod();
    }

    void nonTransactionalMethod_eager() {
        myEntity.getEager();
    }

    void getLazy_nestedMethod() {
        myEntity.getLazy();
    }
}
