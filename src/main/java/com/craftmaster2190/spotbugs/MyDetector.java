package com.craftmaster2190.spotbugs;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;
import org.apache.bcel.Const;
import org.apache.bcel.Repository;
import org.apache.bcel.classfile.AnnotationEntry;
import org.apache.bcel.classfile.ElementValue;
import org.apache.bcel.classfile.ElementValuePair;
import org.apache.bcel.classfile.EnumElementValue;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.FieldOrMethod;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.Type;

/**
 * Finds lazy accessors in not transactional places
 */
public class MyDetector extends OpcodeStackDetector {

  private final BugReporter bugReporter;

  public MyDetector(BugReporter bugReporter) {
    this.bugReporter = bugReporter;
  }

  @Override
  public void sawOpcode(int seen) {
    if (seen == Const.INVOKEVIRTUAL) {
      final String nameConstantOperand = getNameConstantOperand();
      final String classConstantOperand = getClassConstantOperand();
      final String dottedClassConstantOperand = getDottedClassConstantOperand();

      final JavaClass entityClass = ReflectionUtils.getClass(dottedClassConstantOperand);
      ReflectionUtils.findAnnotation(entityClass,
          ReflectionUtils.toByteCodeFieldDescriptor("javax.persistence.Entity"))
          .or(() -> ReflectionUtils.findAnnotation(entityClass, "org.hibernate.annotations.Entity"))
          .flatMap(entityAnnotation -> ReflectionUtils.findMethod(entityClass, nameConstantOperand))
          .flatMap(entityMethod ->
              Stream.of("javax.persistence.Basic")
                  .map(ReflectionUtils::toByteCodeFieldDescriptor)
                  .flatMap(annotationName ->
                      ReflectionUtils
                          .findAnnotation(entityMethod, annotationName)
                          .or(() -> ReflectionUtils.findFieldForGetter(entityClass, entityMethod)
                              .flatMap(entityField -> ReflectionUtils
                                  .findAnnotation(entityField, annotationName)))
                          .stream())
                  .findFirst())
          .flatMap(entityFieldOrMethodAnnotation -> ReflectionUtils
              .findAnnotationValue(entityFieldOrMethodAnnotation, "fetch"))
          .filter(elementValue -> elementValue instanceof EnumElementValue
              && ((EnumElementValue) elementValue).getEnumTypeString()
              .equals(ReflectionUtils.toByteCodeFieldDescriptor("javax.persistence.FetchType"))
              && ((EnumElementValue) elementValue).getEnumValueString()
              .equals("LAZY"))
          .ifPresent(elementValue -> {
            final boolean hasTransactionalAnnotation = Stream.of("javax.transaction.Transactional",
                "org.springframework.transaction.annotation.Transactional")
                .map(ReflectionUtils::toByteCodeFieldDescriptor)
                .flatMap(annotationName -> ReflectionUtils
                    .findAnnotation(getThisClass(), annotationName).stream())
                .findFirst()
                .isPresent();

            if (!hasTransactionalAnnotation) {
              final String bugMessage = "calls " + dottedClassConstantOperand + "." + nameConstantOperand
                  + "(), an Entity, which is marked as fetch = LAZY";
              bugReporter.reportBug(new BugInstance(this, "MY_BUG", NORMAL_PRIORITY)
//                  .addCalledMethod(dottedClassConstantOperand, nameConstantOperand)
                  .addString(bugMessage)
                  .addClassAndMethod(this)
                  .addSourceLine(this, getPC()));
            }
          });
    }
  }
}


class ReflectionUtils {

  public static String toByteCodeFieldDescriptor(String dottedClassName) {
    return 'L' + dottedClassName.replaceAll("\\.", "/") + ';';
  }

  public static JavaClass getClass(String className) {
    try {
      return Repository.lookupClass(className);
    } catch (ClassNotFoundException e) {
      throw new IllegalStateException("Unable to load Class: " + className, e);
    }
  }

  public static Optional<Method> findMethod(JavaClass class_,
      String methodName, Type... argumentTypes) {
    return Arrays.stream(class_.getMethods())
        .filter(method -> method.getName().equals(methodName))
        .filter(method -> Arrays.equals(method.getArgumentTypes(), argumentTypes))
        .findFirst();
  }

  public static Optional<AnnotationEntry> findAnnotation(JavaClass class_, String annotation) {
    return findAnnotation(class_.getAnnotationEntries(), annotation);
  }

  public static Optional<AnnotationEntry> findAnnotation(FieldOrMethod fieldOrMethod,
      String annotation) {
    return findAnnotation(fieldOrMethod.getAnnotationEntries(), annotation);
  }

  public static Optional<AnnotationEntry> findAnnotation(AnnotationEntry[] annotationEntries,
      String annotation) {
    return Arrays.stream(annotationEntries)
        .filter(annotationEntry -> annotationEntry.getAnnotationType().equals(annotation))
        .findFirst();
  }

  public static Optional<ElementValue> findAnnotationValue(AnnotationEntry annotationEntry,
      String annotationParameterName) {
    return Arrays.stream(annotationEntry.getElementValuePairs())
        .filter(
            elementValuePair -> elementValuePair.getNameString().equals(annotationParameterName))
        .findFirst()
        .map(ElementValuePair::getValue);
  }

  public static Optional<Field> findFieldForGetter(JavaClass entityClass, Method entityMethod) {
    return Arrays.stream(entityClass.getFields())
        .filter(field -> getBeanMethodNamesForField(field.getName())
            .anyMatch(beanMethodName -> beanMethodName.equals(entityMethod.getName())))
        .findFirst();
  }

  public static Stream<String> getBeanMethodNamesForField(String fieldName) {
    final String titleizedFieldName = titleize(fieldName);

    return Stream.of("get", "is", "has")
        .map(prefix -> prefix + titleizedFieldName);
  }

  public static String titleize(String string) {
    return string.substring(0, 1).toUpperCase() + string.substring(1);
  }
}