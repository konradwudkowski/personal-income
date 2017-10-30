/*
 * Copyright 2016 HM Revenue & Customs
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

package it

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.github.tomakehurst.wiremock.client.WireMock._
import it.utils.{MicroserviceLocalRunSugar, WiremockServiceLocatorSugar}
import org.scalatest.BeforeAndAfter
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.time.{Millis, Span}
import play.api.test.FakeRequest
import uk.gov.hmrc.api.controllers.DocumentationController
import uk.gov.hmrc.play.test.UnitSpec

/**
  * Testcase to verify the capability of integration with the API platform.
  *
  * 1, To integrate with API platform the service needs to register itself to the service locator by calling the /registration endpoint and providing
  * - application name
  * - application url
  *
  * 2a, To expose API's to Third Party Developers, the service needs to define the APIs in a definition.json and make it available under api/definition GET endpoint
  * 2b, For all of the endpoints defined in the definition.json a documentation.xml needs to be provided and be available under api/documentation/[version]/[endpoint name] GET endpoint
  *     Example: api/documentation/1.0/Fetch-Some-Data
  *
  * See: https://confluence.tools.tax.service.gov.uk/display/ApiPlatform/API+Platform+Architecture+with+Flows
  */
class PlatformIntegrationSpec extends UnitSpec with MockitoSugar with ScalaFutures with WiremockServiceLocatorSugar with BeforeAndAfter with Eventually {

  before {
    startMockServer()
    stubRegisterEndpoint(204)
  }

  after {
    stopMockServer()
  }

  trait Setup {
    val documentationController = DocumentationController
    val request = FakeRequest()

    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()
  }

  "microservice" should {

    "on startup of application register with service-locator" in new MicroserviceLocalRunSugar with Setup {

      override val additionalConfiguration: Map[String, Any] = Map(
        "appName" -> "application-name",
        "appUrl" -> "http://microservice-name.service",
        "microservice.services.service-locator.host" -> stubHost,
        "microservice.services.service-locator.port" -> stubPort)

      run {
        () => {
          eventually(Timeout(Span(1000 * 2, Millis))) {
            verify(postRequestedFor(urlEqualTo("/registration")).
              withHeader("content-type", equalTo("application/json")).
              withRequestBody(equalTo(regPayloadStringFor("application-name", "http://microservice-name.service"))))
          }
        }
      }
    }

    "provide definition endpoint and documentation endpoints for each api" in new MicroserviceLocalRunSugar with Setup {

      override val additionalConfiguration: Map[String, Any] = Map(
        "appName" -> "application-name",
        "appUrl" -> "http://microservice-name.service",
        "microservice.services.service-locator.host" -> stubHost,
        "microservice.services.service-locator.port" -> stubPort)
      run {
        () => {
          def normalizeEndpointName(endpointName: String): String = endpointName.replaceAll(" ", "-")

          def verifyDocumentationPresent(version: String, endpointName: String) {
            withClue(s"Getting documentation version '$version' of endpoint '$endpointName'") {
              val documentationResult = documentationController.documentation(version, endpointName)(request)
              status(documentationResult) shouldBe 200
            }
          }

          val result = documentationController.definition()(request)
          status(result) shouldBe 200
          val jsonResponse = jsonBodyOf(result).futureValue

          val versions: Seq[String] = (jsonResponse \\ "version") map (_.as[String])
          val endpointNames: Seq[Seq[String]] = (jsonResponse \\ "endpoints").map(_ \\ "endpointName").map(_.map(_.as[String]))

          versions.zip(endpointNames).flatMap { case (version, endpoint) => {
            endpoint.map(endpointName => (version, endpointName))
          }
          }.foreach { case (version, endpointName) => verifyDocumentationPresent(version, endpointName) }
        }
      }
    }

    "provide RAML conf endpoint" in new MicroserviceLocalRunSugar with Setup {
      override val additionalConfiguration: Map[String, Any] = Map(
        "appName" -> "application-name",
        "appUrl" -> "http://microservice-name.service",
        "microservice.services.service-locator.host" -> stubHost,
        "microservice.services.service-locator.port" -> stubPort)
      run {
        () => {
          val result = documentationController.conf("1.0", "application.raml")(request)
          status(result) shouldBe 200
        }
      }
    }
  }
}
