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
package io.fares.bind.xjc.plugins.jackson;

import com.sun.codemodel.*;
import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.model.CPropertyInfo;
import com.sun.tools.xjc.outline.FieldOutline;
import com.sun.tools.xjc.outline.Outline;
import com.sun.xml.xsom.XSElementDecl;
import com.sun.xml.xsom.XSParticle;
import com.sun.xml.xsom.XSTerm;
import org.jvnet.jaxb.plugin.AbstractParameterizablePlugin;
import org.xml.sax.ErrorHandler;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Arrays.stream;

public class FixedValuePlugin extends AbstractParameterizablePlugin {

  public static final String NS = "urn:jaxb.fares.io:fixed-value";


  @Override
  public String getOptionName() {
    return "Xfixed-value";
  }

  @Override
  public String getUsage() {
    return "  -Xfixed-value      : enable fixed value generation for schema elements";
  }

  @Override
  public List<String> getCustomizationURIs() {
    return Collections.singletonList(NS);
  }

  @Override
  public boolean run(Outline outline, Options opt, ErrorHandler errorHandler) {

    outline.getClasses()
      .stream()
      .flatMap(co -> stream(co.getDeclaredFields()))
      .filter(this::isEligible)
      .forEach(this::process);

    return true;

  }

  private void process(FieldOutline fieldOutline) {

    final Outline outline = fieldOutline.parent().parent();
    final CPropertyInfo fieldInfo = fieldOutline.getPropertyInfo();
    final XSParticle schemaComponent = (XSParticle) fieldInfo.getSchemaComponent();
    final XSTerm term = schemaComponent.getTerm();
    final XSElementDecl element = term.asElementDecl();
    final Map<String, JFieldVar> fields = fieldOutline.parent().getImplClass().fields();

    // get the field of the class
    final JFieldVar field = fields.get(fieldInfo.getName(false));

    // get the field type that needs to be set to a fixed value
    final JType fieldType = fieldOutline.getRawType().boxify();

    // get the fixed value defined in the schema
    final String fixedValue = element.getFixedValue().toString();

    // create an appropriate fixed expression depending on type

    // --- string --------------------------------------------------------------------------------------------------
    if (fieldType.equals(outline.getCodeModel().ref(String.class))) {
      field.init(JExpr.lit(fixedValue));
      logger.info("Initializing String variable " + fieldInfo.displayName() + " to \"" + fixedValue + "\"");
    }
    // --- boolean -------------------------------------------------------------------------------------------------
    else if (fieldType.equals(outline.getCodeModel().ref(Boolean.class))) {
      field.init(JExpr.lit(Boolean.parseBoolean(fixedValue)));
      logger.info("Initializing Boolean variable " + fieldInfo.displayName() + " to " + fixedValue);
    }
    // --- byte, short or int --------------------------------------------------------------------------------------
    else if (fieldType.equals(outline.getCodeModel().ref(Byte.class)) ||
      fieldType.equals(outline.getCodeModel().ref(Short.class)) ||
      fieldType.equals(outline.getCodeModel().ref(Integer.class))) {
      field.init(JExpr.lit(Integer.parseInt(fixedValue)));
      logger.info("Initializing Integer variable " + fieldInfo.displayName() + " to " + fixedValue);
    }
    // --- long ----------------------------------------------------------------------------------------------------
    else if (fieldType.equals(outline.getCodeModel().ref(Long.class))) {
      field.init(JExpr.lit(Long.parseLong(fixedValue)));
      logger.info("Initializing Long variable " + fieldInfo.displayName() + " to " + fixedValue);
    }
    // --- float ---------------------------------------------------------------------------------------------------
    else if (fieldType.equals(outline.getCodeModel().ref(Float.class))) {
      field.init(JExpr.lit(Float.parseFloat(fixedValue)));
      logger.info("Initializing Float variable " + fieldInfo.displayName() + " to " + fixedValue);
    }
    // --- double --------------------------------------------------------------------------------------------------
    else if (fieldType.equals(outline.getCodeModel().ref(Double.class))) {
      field.init(JExpr.lit(Double.parseDouble(fixedValue)));
      logger.info("Initializing Double variable " + fieldInfo.displayName() + " to " + fixedValue);
    }
    // --- XML gregorian calendar (deprecate) ----------------------------------------------------------------------
    else if (fieldType.equals(outline.getCodeModel().ref(XMLGregorianCalendar.class))) {
      // XMLGregorianCalender is constructed by DatatypeFactory, so we have to have an instance of that once
      // per class, some conversions may have to add class level code
      JFieldVar dtf = installDtF(fieldOutline.parent().getImplClass());
      if (dtf == null) {
        return;
      }
      ;
      // use our DtF instance to generate the initialization expression
      field.init(JExpr.invoke(dtf, "newXMLGregorianCalendar").arg(fixedValue));
      logger.info("Initializing XMLGregorianCalendar variable " + fieldInfo.displayName() + " with value of " + fixedValue);
    }
    // --- enum ----------------------------------------------------------------------------------------------------
    else if ((fieldType instanceof JDefinedClass) && (((JDefinedClass) fieldType).getClassType() == ClassType.ENUM)) {
      Optional<JEnumConstant> enumConstant = findEnumConstant(fieldType, fixedValue, outline);
      if (enumConstant.isPresent()) {
        field.init(enumConstant.get());
        logger.info("Initializing enum variable " + fieldInfo.displayName() + " with constant " + enumConstant.get().getName());
      } else {
        logger.warn("Could not find member on enum " + fieldType.fullName() + " with value: " + fixedValue);
      }
    } else {
      logger.warn("Did not create default value for field " + fieldInfo.displayName() + ". Don't know how to create default value expression for fields of type " + fieldType.name() + ". Default value of \"" + fixedValue + "\" specified in schema");
    }
  }


  private boolean isEligible(FieldOutline fieldOutline) {

    CPropertyInfo fieldInfo = fieldOutline.getPropertyInfo();

    // ignore anything other than particles
    if (!(fieldInfo.getSchemaComponent() instanceof XSParticle)) {
      return false;
    }

    // ignore if not an xsd element declaration
    XSParticle schemaComponent = (XSParticle) fieldInfo.getSchemaComponent();
    XSTerm term = schemaComponent.getTerm();
    if (!term.isElementDecl()) {
      return false;
    }

    // ignore if element does not contain a fixed value definition
    XSElementDecl element = term.asElementDecl();
    return element.getFixedValue() != null;

  }

  /**
   * Retrieve the enum constant that correlates to the string value.
   *
   * @param enumType        Type identifying an Enum in the code model
   * @param enumStringValue Lexical value of the constant to search
   * @param outline         Outline of the code model if found
   * @return The matching constant val;ue from the enum type or {@link Optional#empty()} if not found
   */
  private Optional<JEnumConstant> findEnumConstant(JType enumType, String enumStringValue, Outline outline) {

    return outline.getEnums()
      .stream()
      .filter(e -> enumType.equals(e.getImplClass())) // our enum type matches the type in the outline
      .flatMap(e -> e.constants.stream())
      .filter(m -> m.target.getLexicalValue().equals(enumStringValue)) // the enum member matches the fixed value
      .map(m -> m.constRef)
      .findFirst();

  }


  /**
   * Enhance the CodeModel of a Class to include a {@link javax.xml.datatype.DatatypeFactory} as a static private field.
   * The factory is needed to construct {@link javax.xml.datatype.XMLGregorianCalendar} from String representation.
   *
   * @param parentClass Class where the DatatypeFactory will be created
   * @return Reference to the created static field
   */
  private JFieldVar installDtF(final JDefinedClass parentClass) {
    try {

      JCodeModel model = parentClass.owner();

      // create a static variable of type DatatypeFactory
      JClass dtfClass = model.ref(DatatypeFactory.class);
      JFieldVar dtf = parentClass.field(JMod.STATIC | JMod.FINAL | JMod.PRIVATE, dtfClass, "DATATYPE_FACTORY");

      // Initialize variable in static block
      JBlock si = parentClass.init();
      JTryBlock tryBlock = si._try();
      tryBlock.body().assign(dtf, dtfClass.staticInvoke("newInstance"));

      // catch exception & rethrow as unchecked exception
      JCatchBlock catchBlock = tryBlock._catch(model.ref(DatatypeConfigurationException.class));
      JVar ex = catchBlock.param("ex");
      JClass runtimeException = model.ref(RuntimeException.class);
      catchBlock.body()._throw(JExpr._new(runtimeException).arg("Unable to initialize DatatypeFactory").arg(ex));

      return dtf;

    } catch (Exception e) {
      logger.error("Failed to create code", e);
      return null;
    }
  }

}
