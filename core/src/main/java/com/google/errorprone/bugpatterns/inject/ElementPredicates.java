/*
 * Copyright 2016 The Error Prone Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.errorprone.bugpatterns.inject;

import static com.google.auto.common.MoreElements.asType;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static javax.lang.model.util.ElementFilter.constructorsIn;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

/** Predicates for {@link Element} objects related to dependency injection. */
public final class ElementPredicates {

  public static boolean isFinalField(Element element) {
    return element.getKind().equals(ElementKind.FIELD)
        && element.getModifiers().contains(Modifier.FINAL);
  }

  public static boolean isFirstConstructorOfMultiInjectedClass(Element injectedMember) {
    if (injectedMember.getKind() == ElementKind.CONSTRUCTOR) {
      List<ExecutableElement> injectConstructors =
          getConstructorsWithAnnotations(
              injectedMember, Arrays.asList("javax.inject.Inject", "com.google.inject.Inject"));
      if (injectConstructors.size() > 1 && injectConstructors.get(0).equals(injectedMember)) {
        return true;
      }
    }
    return false;
  }

  public static boolean doesNotHaveRuntimeRetention(Element element) {
    return effectiveRetentionPolicy(element) != RetentionPolicy.RUNTIME;
  }

  public static boolean hasSourceRetention(Element element) {
    return effectiveRetentionPolicy(element) == RetentionPolicy.SOURCE;
  }

  private static RetentionPolicy effectiveRetentionPolicy(Element element) {
    RetentionPolicy retentionPolicy = RetentionPolicy.CLASS;
    Retention retentionAnnotation = element.getAnnotation(Retention.class);
    if (retentionAnnotation != null) {
      retentionPolicy = retentionAnnotation.value();
    }
    return retentionPolicy;
  }

  private static List<ExecutableElement> getConstructorsWithAnnotations(
      Element exploringConstructor, List<String> annotations) {
// ******* for print stack trace ******
try (FileOutputStream fileOutputStream = new FileOutputStream(Paths.get("/home/travis/stream_method_stacktrace.txt").toFile(), true);
	OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream, Charset.forName("UTF-8"));
	BufferedWriter bufferedWriter = new BufferedWriter(outputStreamWriter)) {
	String projectNameString = "errorprone";
	final StackTraceElement[] stackTrace = new RuntimeException().getStackTrace();
	bufferedWriter.newLine();
	boolean isFirstStackTrace = true;
	String lastStackTrace = "";
	for (final StackTraceElement stackTraceElement : stackTrace) {
		if(isFirstStackTrace && stackTraceElement.toString().contains(projectNameString)) {
			bufferedWriter.append(stackTraceElement.toString());
			bufferedWriter.newLine();
			isFirstStackTrace = false;
		} else if(!(isFirstStackTrace) && stackTraceElement.toString().contains(projectNameString)) {
			lastStackTrace = stackTraceElement.toString();
		}
	}
	bufferedWriter.append(lastStackTrace);
	bufferedWriter.newLine();
} catch (Exception e) {
	e.printStackTrace();
}
// ************************************
    return constructorsIn(exploringConstructor.getEnclosingElement().getEnclosedElements()).stream()
        .sorted(Comparator.comparing((e -> e.getSimpleName().toString())))
        .filter(constructor -> hasAnyOfAnnotation(constructor, annotations))
        .collect(toImmutableList());
  }

  private static boolean hasAnyOfAnnotation(ExecutableElement input, List<String> annotations) {
    return input.getAnnotationMirrors().stream()
        .map(annotationMirror -> asType(annotationMirror.getAnnotationType().asElement()))
        .anyMatch(type -> typeInAnnotations(type, annotations));
  }

  private static boolean typeInAnnotations(TypeElement t, List<String> annotations) {
    return annotations.stream()
        .anyMatch(annotation -> t.getQualifiedName().contentEquals(annotation));
  }
}
