/*
 * Copyright 2025 HM Revenue & Customs
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

package service.submission

import com.google.inject.Singleton
import org.xml.sax.{ErrorHandler, SAXParseException}

import java.io.StringReader
import javax.xml.XMLConstants
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.{Schema, SchemaFactory}
import scala.collection.mutable
import scala.xml.Elem

@Singleton
class SchemaValidator:

  private val schemaResource = "/schemas/SDLT-v6-3.xsd"

  private lazy val schema: Schema =
    val factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
    val stream  = Option(getClass.getResourceAsStream(schemaResource))
      .getOrElse(throw new IllegalStateException(s"SDLT schema not found on classpath at $schemaResource"))
    try factory.newSchema(new StreamSource(stream))
    finally stream.close()

  def validateSdlt(sdlt: Elem): Either[Seq[String], Unit] =
    validate(sdlt)

  def validateIrEnvelope(irEnvelope: Elem): Either[Seq[String], Unit] =
    validate(irEnvelope)
    
  private def validate(elem: Elem): Either[Seq[String], Unit] =
    val errors    = mutable.Buffer.empty[String]
    val validator = schema.newValidator()
    validator.setErrorHandler(new ErrorHandler:
      override def warning(e: SAXParseException): Unit    = ()
      override def error(e: SAXParseException): Unit      = errors += format(e)
      override def fatalError(e: SAXParseException): Unit = errors += format(e)
    )
    try
      validator.validate(new StreamSource(new StringReader(elem.toString)))
      if errors.isEmpty then Right(()) else Left(errors.toSeq)
    catch
      case e: SAXParseException =>
        errors += format(e)
        Left(errors.toSeq)

  private def format(e: SAXParseException): String =
    s"line ${e.getLineNumber}, col ${e.getColumnNumber}: ${e.getMessage}"