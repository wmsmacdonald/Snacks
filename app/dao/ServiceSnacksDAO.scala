package dao

import models.ServiceSnack

import scala.concurrent.Future
import javax.inject.Inject

import play.api.libs.ws._
import play.api.libs.json._

import scala.util.{Failure, Success, Try}

class ServiceSnacksDAO @Inject()(ws: WSClient) {
  implicit val context = play.api.libs.concurrent.Execution.Implicits.defaultContext
  def all(): Future[Try[Seq[ServiceSnack]]] = {
    val request: WSRequest = ws.url("https://api-snacks.nerderylabs.com/v1/snacks/")
      .withQueryString("ApiKey" -> "6e766c67-4293-4004-8268-50855f355445")

    implicit val snackReads = Json.reads[ServiceSnack]
    for (response <- request.get()) yield response.json.validate[Seq[ServiceSnack]] match {
      case JsSuccess(snacks,_) => Success(snacks)
      case JsError(e) => Failure(new Exception("Failed to parse JSON" + response.body))
    }
  }

  def create(name: String, location: String): Future[Either[ServiceSnack, String]] = {
    val json = JsObject(Map("name" -> JsString(name), "location" -> JsString(location)))

    val request: WSRequest = (
      ws.url("https://api-snacks.nerderylabs.com/v1/snacks/")
      withQueryString("ApiKey" -> "6e766c67-4293-4004-8268-50855f355445")
      withHeaders("Content-Type" -> "application/json")
    )

    implicit val snackReads = Json.reads[ServiceSnack]
    for (response <- request.post(json)) yield response.json.validate[ServiceSnack] match {
      case JsSuccess(snack,_) => Left(snack)
      case JsError(e) => Right((response.json \ "message").as[String])
    }
  }
}
