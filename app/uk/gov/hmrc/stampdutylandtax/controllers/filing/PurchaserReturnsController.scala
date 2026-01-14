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

package uk.gov.hmrc.stampdutylandtax.controllers.filing

import models.filing.*
import play.api.Logging
import play.api.libs.json.{JsError, JsValue, Json}
import play.api.mvc.{Action, ControllerComponents}
import service.filing.PurchaserReturnsService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.stampdutylandtax.controllers.actions.IdentifierAction

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton()
class PurchaserReturnsController @Inject()(
                                         cc: ControllerComponents,
                                         service: PurchaserReturnsService,
                                         auth: IdentifierAction
                                       )(implicit ec: ExecutionContext) extends BackendController(cc) with Logging {
  
  def createPurchaser(): Action[JsValue] = auth.async(parse.json) { implicit request =>
    request.body
      .validate[CreatePurchaserRequest]
      .fold(
        errs =>
          Future.successful(BadRequest(Json.obj("message" -> "Invalid payload", "errors" -> JsError.toJson(errs)))),
        body =>
          service
            .createPurchaser(body)
            .map { result =>
              Created(Json.toJson(result))
            }
            .recover { case t =>
              logger.error("[createPurchaser] failed", t)
              InternalServerError(Json.obj("message" -> "Unexpected error"))
            }
      )
  }

  def updatePurchaser(): Action[JsValue] = auth.async(parse.json) { implicit request =>
    request.body
      .validate[UpdatePurchaserRequest]
      .fold(
        errs =>
          Future.successful(BadRequest(Json.obj("message" -> "Invalid payload", "errors" -> JsError.toJson(errs)))),
        body =>
          service
            .updatePurchaser(body)
            .map { result =>
              Created(Json.toJson(result))
            }
            .recover { case t =>
              logger.error("[updatePurchaser] failed", t)
              InternalServerError(Json.obj("message" -> "Unexpected error"))
            }
      )
  }

  def deletePurchaser(): Action[JsValue] = auth.async(parse.json) { implicit request =>
    request.body
      .validate[DeletePurchaserRequest]
      .fold(
        errs =>
          Future.successful(BadRequest(Json.obj("message" -> "Invalid payload", "errors" -> JsError.toJson(errs)))),
        body =>
          service
            .deletePurchaser(body)
            .map { result =>
              Created(Json.toJson(result))
            }
            .recover { case t =>
              logger.error("[deletePurchaser] failed", t)
              InternalServerError(Json.obj("message" -> "Unexpected error"))
            }
      )
  }

  def createCompanyDetails(): Action[JsValue] = auth.async(parse.json) { implicit request =>
    request.body
      .validate[CreateCompanyDetailsRequest]
      .fold(
        errs =>
          Future.successful(BadRequest(Json.obj("message" -> "Invalid payload", "errors" -> JsError.toJson(errs)))),
        body =>
          service
            .createCompanyDetails(body)
            .map { result =>
              Created(Json.toJson(result))
            }
            .recover { case t =>
              logger.error("[createCompanyDetails] failed", t)
              InternalServerError(Json.obj("message" -> "Unexpected error"))
            }
      )
  }

  def updateCompanyDetails(): Action[JsValue] = auth.async(parse.json) { implicit request =>
    request.body
      .validate[UpdateCompanyDetailsRequest]
      .fold(
        errs =>
          Future.successful(BadRequest(Json.obj("message" -> "Invalid payload", "errors" -> JsError.toJson(errs)))),
        body =>
          service
            .updateCompanyDetails(body)
            .map { result =>
              Created(Json.toJson(result))
            }
            .recover { case t =>
              logger.error("[updateCompanyDetails] failed", t)
              InternalServerError(Json.obj("message" -> "Unexpected error"))
            }
      )
  }

  def deleteCompanyDetails(): Action[JsValue] = auth.async(parse.json) { implicit request =>
    request.body
      .validate[DeleteCompanyDetailsRequest]
      .fold(
        errs =>
          Future.successful(BadRequest(Json.obj("message" -> "Invalid payload", "errors" -> JsError.toJson(errs)))),
        body =>
          service
            .deleteCompanyDetails(body)
            .map { result =>
              Created(Json.toJson(result))
            }
            .recover { case t =>
              logger.error("[deleteCompanyDetails] failed", t)
              InternalServerError(Json.obj("message" -> "Unexpected error"))
            }
      )
  }

}