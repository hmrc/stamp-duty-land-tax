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

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import scala.util.Try

object Normalise:
  
  def isBlank(s: Option[String]): Boolean = s.forall(_.trim.isEmpty)
  def nonBlank(s: Option[String]): Option[String] = s.map(_.trim).filter(_.nonEmpty)
  def nonBlank(s: String): Option[String] = nonBlank(Option(s))
  
  def isYes(s: Option[String]): Boolean = s.exists(v => v.trim.equalsIgnoreCase("yes") || v.trim.equalsIgnoreCase("y"))
  def yesNo(s: Option[String]): Option[String] = nonBlank(s).map { v =>
    if v.equalsIgnoreCase("yes") || v.equalsIgnoreCase("y") then "yes"
    else if v.equalsIgnoreCase("no") || v.equalsIgnoreCase("n") then "no"
    else v.toLowerCase
  }

  def yesNo(b: Boolean): String = if b then "yes" else "no"

  def money(value: BigDecimal): String =
    value.setScale(2, BigDecimal.RoundingMode.HALF_UP).bigDecimal.toPlainString
  
  def toMoney(s: String): Option[BigDecimal] =
      scala.util.Try(BigDecimal(s.trim)).toOption

  def money(value: Option[BigDecimal]): Option[String] = value.map(money)

  def moneyOrZero(value: Option[BigDecimal]): String = money(value.getOrElse(BigDecimal(0)))

  def moneyFromString(value: Option[String]): Option[String] =
    nonBlank(value).flatMap(s => Try(BigDecimal(s)).toOption).map(money)
  
  private val UkDateFormat  = DateTimeFormatter.ofPattern("dd/MM/yyyy")
  private val IsoDateFormat = DateTimeFormatter.ISO_LOCAL_DATE

  def isoDate(value: Option[String]): Option[String] =
    nonBlank(value).flatMap { s =>
      Try(LocalDate.parse(s, IsoDateFormat))
        .orElse(Try(LocalDate.parse(s, UkDateFormat)))
        .toOption
        .map(_.format(IsoDateFormat))
    }