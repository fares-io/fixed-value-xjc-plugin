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
package io.fares.bind.xjc.plugins.fv;

import io.fares.bind.xjc.plugins.fv.validators.FixedElementValueValidator;
import io.fares.bind.xjc.plugins.fv.validators.TestValidator;

import java.io.File;

public class FixedValuePluginTest extends AbstractPluginTest {

  @Override
  public File getSchemaDirectory() {
    return new File(getBaseDir(), "src/test/resources/schemas/FixedElementValue");
  }

  @Override
  protected File getClassFile(File genDir) {
    return new File(genDir, "testfixedelement/Product.java");
  }

  @Override
  protected TestValidator getValidator() {
    return new FixedElementValueValidator();
  }

}
