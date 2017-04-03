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

import org.scalatest.concurrent.ScalaFutures
import play.api.libs.json.{Json, Writes}
import play.api.test.FakeApplication
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.personalincome.config.ServicesCircuitBreaker
import uk.gov.hmrc.personalincome.controllers.StubApplicationConfiguration
import uk.gov.hmrc.personalincome.domain._
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.hooks.HttpHook
import uk.gov.hmrc.play.http.{HeaderCarrier, _}
import uk.gov.hmrc.play.test.{WithFakeApplication, UnitSpec}
import scala.concurrent.Future

class NtcTestConnector extends NtcConnector with ServicesConfig with ServicesCircuitBreaker {
  override def http: HttpGet with HttpPost = ???

  override def serviceUrl: String = "some-service-url"
}

class NtcConnectorSpec
  extends UnitSpec with ScalaFutures with StubApplicationConfiguration with WithFakeApplication with CircuitBreakerTest {

  import scala.concurrent.ExecutionContext.Implicits.global

  override lazy val fakeApplication = FakeApplication(additionalConfiguration = config)

    private trait Setup {

      implicit lazy val hc = HeaderCarrier()

      val nino = Nino( "KM569110B")
      val taxCreditNino = TaxCreditsNino(nino.value)
      val incomeDetails = IncomeDetails(Some(10), Some(20), Some(30), Some(40), Some(true))
      val renewal = TcrRenewal(RenewalData(Some(incomeDetails), None, None), None, None, None, false)
      val renewalReference = RenewalReference("123456")
      val tcrAuthToken = TcrAuthenticationToken("some-token")
      val claimentDetails = ClaimantDetails(false, 1, "renewalForm", nino.value, None, false, "some-app-id")

      val claimsJson = """{
       |  "references": [
       |    {
       |      "household": {
       |        "barcodeReference": "200000000000012",
       |        "applicationID": "198765432134566",
       |        "applicant1": {
       |          "nino": "AA000003D",
       |          "title": "Miss",
       |          "firstForename": "Emma",
       |          "secondForename": "",
       |          "surname": "Cowling"
       |        }
       |      },
       |      "renewal": {
       |        "awardStartDate": "2016-04-05",
       |        "awardEndDate": "2016-08-31",
       |        "renewalNoticeIssuedDate": "20301012",
       |        "renewalNoticeFirstSpecifiedDate": "20101012"
       |      }
       |    },
       |    {
       |      "household": {
       |        "barcodeReference": "200000000000013",
       |        "applicationID": "198765432134567",
       |        "applicant1": {
       |          "nino": "AA000003D",
       |          "title": "Miss",
       |          "firstForename": "Emma",
       |          "secondForename": "",
       |          "surname": "Cowling"
       |        }
       |      },
       |      "renewal": {
       |        "awardStartDate": "2016-08-31",
       |        "awardEndDate": "2016-12-31",
       |        "renewalNoticeIssuedDate": "20301012",
       |        "renewalNoticeFirstSpecifiedDate": "20101012"
       |      }
       |    },
       |    {
       |      "household": {
       |        "barcodeReference": "200000000000014",
       |        "applicationID": "198765432134568",
       |        "applicant1": {
       |          "nino": "AM242413B",
       |          "title": "Miss",
       |          "firstForename": "Hazel",
       |          "secondForename": "",
       |          "surname": "Young"
       |        },
       |        "applicant2": {
       |          "nino": "AP412713B",
       |          "title": "Miss",
       |          "firstForename": "Cathy",
       |          "secondForename": "",
       |          "surname": "Garcia-Vazquez"
       |        }
       |      },
       |      "renewal": {
       |        "awardStartDate": "2016-12-31",
       |        "awardEndDate": "2017-07-31",
       |        "renewalNoticeIssuedDate": "20301012",
       |        "renewalNoticeFirstSpecifiedDate": "20101012"
       |      }
       |    }
       |  ]
       |}""".stripMargin

      val claims200Success = Json.parse(claimsJson)
      lazy val http200ClaimsResponse = Future.successful(HttpResponse(200, Some(claims200Success)))
      lazy val http500Response = Future.failed(new Upstream5xxResponse("Error", 500, 500))
      lazy val http400Response = Future.failed(new BadRequestException("bad request"))
      lazy val http404Response = Future.successful(HttpResponse(404))
      lazy val http204Response = Future.successful(HttpResponse(204))
      lazy val http200AuthenticateResponse = Future.successful(HttpResponse(200, Some(Json.toJson(tcrAuthToken))))
      lazy val http200ClaimantDetailsResponse = Future.successful(HttpResponse(200, Some(Json.toJson(claimentDetails))))
      lazy val response: Future[HttpResponse] = http400Response

      val connector = new NtcTestConnector {
        override lazy val serviceUrl = "someUrl"

        override lazy val http: HttpGet with HttpPost = new HttpGet with HttpPost {
          override val hooks: Seq[HttpHook] = NoneRequired
          override protected def doGet(url: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = response

          override protected def doPost[A](url: String, body: A, headers: Seq[(String, String)])(implicit wts: Writes[A], hc: HeaderCarrier): Future[HttpResponse] = response

          override protected def doPostString(url: String, body: String, headers: Seq[(String, String)])(implicit hc: HeaderCarrier): Future[HttpResponse] = ???

          override protected def doFormPost(url: String, body: Map[String, Seq[String]])(implicit hc: HeaderCarrier): Future[HttpResponse] = ???

          override protected def doEmptyPost[A](url: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = ???
        }

      }
    }

    "authenticate tcsConnector" should {

      "return None when 404 returned" in new Setup {
        override lazy val response = http404Response
        val result: Option[TcrAuthenticationToken] = await(connector.authenticateRenewal(taxCreditNino, renewalReference))
        result shouldBe None
      }

      "return None when 400 returned" in new Setup {
        override lazy val response = http400Response
        val result: Option[TcrAuthenticationToken] = await(connector.authenticateRenewal(taxCreditNino, renewalReference))
        result shouldBe None
      }

      "throw Upstream5xxResponse when a 500 response is returned" in new Setup {
        override lazy val response = http500Response
        intercept[Upstream5xxResponse] {
          await(connector.authenticateRenewal(taxCreditNino, renewalReference))
        }
      }

      "return a valid response when a 200 response is received with a valid json payload" in new Setup {
        override lazy val response = http200AuthenticateResponse
        val result: Option[TcrAuthenticationToken] = await(connector.authenticateRenewal(taxCreditNino, renewalReference))

        result.get shouldBe tcrAuthToken
      }

      "circuit breaker configuration should be applied and unhealthy service exception will kick in after 5th failed call" in new Setup {
        override lazy val response = http500Response
        executeCB(connector.authenticateRenewal(taxCreditNino, renewalReference))
      }

    }

    "claims tcsConnector" should {

      "throw BadRequestException when a 400 response is returned" in new Setup {
        override lazy val response = http400Response
        intercept[BadRequestException] {
          await(connector.claimantClaims(taxCreditNino))
        }
      }

      "throw Upstream5xxResponse when a 500 response is returned" in new Setup {
        override lazy val response = http500Response
        intercept[Upstream5xxResponse] {
          await(connector.claimantClaims(taxCreditNino))
        }
      }

      "return a valid response when a 200 response is received with a valid json payload" in new Setup {
        override lazy val response = http200ClaimsResponse
        val result = await(connector.claimantClaims(taxCreditNino))

        result shouldBe Json.toJson(claims200Success).as[Claims]
      }

      "circuit breaker configuration should be applied and unhealthy service exception will kick in after 5th failed call" in new Setup {
        override lazy val response = http500Response
        executeCB(connector.claimantClaims(taxCreditNino))
      }
    }

    "claimantDetails tcsConnector" should {

      "throw BadRequestException when a 400 response is returned" in new Setup {
        override lazy val response = http400Response
        intercept[BadRequestException] {
          await(connector.claimantDetails(taxCreditNino))
        }
      }

      "throw Upstream5xxResponse when a 500 response is returned" in new Setup {
        override lazy val response = http500Response
        intercept[Upstream5xxResponse] {
          await(connector.claimantDetails(taxCreditNino))
        }
      }

      "return a valid response when a 200 response is received with a valid json payload" in new Setup {
        override lazy val response = http200ClaimantDetailsResponse
        val result = await(connector.claimantDetails(taxCreditNino))

        result shouldBe claimentDetails
      }

      "circuit breaker configuration should be applied and unhealthy service exception will kick in after 5th failed call" in new Setup {
        override lazy val response = http500Response
        executeCB(connector.claimantDetails(taxCreditNino))
      }

    }

    "submitRenewal tcsConnector" should {

      "throw BadRequestException when a 400 response is returned" in new Setup {
        override lazy val response = http400Response
        intercept[BadRequestException] {
          await(connector.submitRenewal(taxCreditNino, renewal))
        }
      }

      "throw Upstream5xxResponse when a 500 response is returned" in new Setup {
        override lazy val response = http500Response
        intercept[Upstream5xxResponse] {
          await(connector.submitRenewal(taxCreditNino, renewal))
        }
      }

      "return a valid response when a 200 response is received with a valid json payload" in new Setup {
        override lazy val response = http204Response
        val result = await(connector.submitRenewal(taxCreditNino, renewal))
        result.status shouldBe 204
      }

      "circuit breaker configuration should be applied and unhealthy service exception will kick in after 5th failed call" in new Setup {
        override lazy val response = http500Response
        executeCB(connector.submitRenewal(taxCreditNino, renewal))
      }

    }
}
