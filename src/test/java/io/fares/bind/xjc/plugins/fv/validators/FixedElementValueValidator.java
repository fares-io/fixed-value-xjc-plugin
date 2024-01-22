/*
 * Copyright 2024 Niels Bertram
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package io.fares.bind.xjc.plugins.fv.validators;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.FieldAccessExpr;

public class FixedElementValueValidator extends TestValidator {

  @Override
  public void visit(ClassOrInterfaceDeclaration n, Void arg) {
    super.visit(n, arg);

    boolean match = n.getFieldByName("unit")
      .filter(f -> f.getVariables().size() == 1)
      .map(f -> f.getVariable(0))
      .map(VariableDeclarator::getInitializer)
      .flatMap(e -> e) // unpack
      .filter(e -> e instanceof FieldAccessExpr)
      .map(FieldAccessExpr.class::cast)
      .map(Object::toString)
      .filter("UnitOfMeasurement.LBS"::equals)
      .isPresent();

    if (match) foundIt();

  }

}
