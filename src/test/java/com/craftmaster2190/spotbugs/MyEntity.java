package com.craftmaster2190.spotbugs;

import static javax.persistence.FetchType.EAGER;
import static javax.persistence.FetchType.LAZY;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.ManyToMany;
import lombok.Data;
import org.hibernate.annotations.Any;
import org.hibernate.annotations.CollectionOfElements;
import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;
import org.hibernate.annotations.ManyToAny;

@Data
@javax.persistence.Entity
@org.hibernate.annotations.Entity
public class MyEntity {

  @ManyToMany(fetch = LAZY)
  @ManyToAny(fetch = LAZY, metaColumn = @Column)
  @LazyToOne(LazyToOneOption.PROXY)
  @javax.persistence.OneToMany(fetch = LAZY)
  @javax.persistence.ManyToOne(fetch = LAZY)
  @javax.persistence.OneToOne(fetch = LAZY)
  @javax.persistence.Basic(fetch = LAZY)
  @Any(fetch = LAZY, metaColumn = @Column)
  @CollectionOfElements(fetch = LAZY)
  @ElementCollection(fetch = LAZY)

  private Object lazy;

  @javax.persistence.OneToMany(fetch = EAGER)
  @javax.persistence.ManyToOne(fetch = EAGER)
  @javax.persistence.OneToOne(fetch = EAGER)
  @javax.persistence.Basic(fetch = EAGER)
  private Object eager;


  @LazyToOne(LazyToOneOption.NO_PROXY)
  public Object getLazyMethod() {
    return lazy;
  }

}
