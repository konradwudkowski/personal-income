/*
 * Copyright 2017 HM Revenue & Customs
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

package uk.gov.hmrc.personalincome.connectors

import org.mockito.ArgumentMatchers.{any, eq => eqs}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import play.api.libs.json.Writes
import uk.gov.hmrc.api.connector.ServiceLocatorConnector
import uk.gov.hmrc.api.domain.Registration
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.{ExecutionContext, Future}

class ServiceLocatorConnectorSpec extends UnitSpec with MockitoSugar with ScalaFutures {

  trait Setup {
    implicit val hc = HeaderCarrier()
    val serviceLocatorException = new RuntimeException

    lazy val connector = new ServiceLocatorConnector {
      override lazy val http: CorePost = mock[CorePost]
      override lazy val appUrl: String = "http://api-microservice-template.service"
      override lazy val appName: String = "api-microservice-template"
      override lazy val serviceUrl: String = "https://SERVICE_LOCATOR"
      override lazy val handlerOK: () => Unit = mock[Function0[Unit]]
      override lazy val handlerError: Throwable => Unit = mock[Function1[Throwable, Unit]]
      override lazy val metadata: Option[Map[String, String]] = Some(Map("third-party-api" -> "true"))
    }
  }

  "register" should {
    "register the JSON API Definition into the Service Locator" in new Setup {

      val registration = Registration(serviceName = "api-microservice-template", serviceUrl = "http://api-microservice-template.service", metadata = Some(Map("third-party-api" -> "true")))

      when(connector.http.POST(eqs(s"${connector.serviceUrl}/registration"), eqs(registration), eqs(Seq("Content-Type"-> "application/json")))(any[Writes[Registration]], any[HttpReads[HttpResponse]], any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.successful(HttpResponse(200)))

      connector.register.futureValue shouldBe true
      verify(connector.http).POST(eqs("https://SERVICE_LOCATOR/registration"), eqs(registration), eqs(Seq("Content-Type"-> "application/json")))(any[Writes[Registration]], any[HttpReads[HttpResponse]], eqs(hc), any[ExecutionContext])
      verify(connector.handlerOK).apply()
      verify(connector.handlerError, never).apply(serviceLocatorException)
    }


    "fail registering in service locator" in new Setup {

      val registration = Registration(serviceName = "api-microservice-template", serviceUrl = "http://api-microservice-template.service", metadata = Some(Map("third-party-api" -> "true")))
      when(connector.http.POST(eqs(s"${connector.serviceUrl}/registration"), eqs(registration), eqs(Seq("Content-Type"-> "application/json")))(any[Writes[Registration]], any[HttpReads[HttpResponse]], any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.failed(serviceLocatorException))

      connector.register.futureValue shouldBe false
      verify(connector.http).POST(eqs("https://SERVICE_LOCATOR/registration"), eqs(registration), eqs(Seq("Content-Type"-> "application/json")))(any[Writes[Registration]], any[HttpReads[HttpResponse]], eqs(hc), any[ExecutionContext])
      verify(connector.handlerOK, never).apply()
      verify(connector.handlerError).apply(serviceLocatorException)
    }

  }
}
