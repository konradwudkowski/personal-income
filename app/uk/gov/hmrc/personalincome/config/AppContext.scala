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

package uk.gov.hmrc.personalincome.config

import play.api.Play._
import uk.gov.hmrc.play.config.ServicesConfig
import scala.collection.JavaConversions._

object AppContext extends ServicesConfig {
  lazy val appName = current.configuration.getString("appName").getOrElse(throw new RuntimeException("appName is not configured"))
  lazy val appUrl = current.configuration.getString("appUrl").getOrElse(throw new RuntimeException("appUrl is not configured"))
  lazy val serviceLocatorUrl: String = baseUrl("service-locator")
  lazy val registrationEnabled: Boolean = current.configuration.getBoolean(s"microservice.services.service-locator.enabled").getOrElse(true)

  case class RenewalStatusTransform(name:String, statusValues:Seq[String])

  lazy val renewalStatusTransform: Option[List[RenewalStatusTransform]] = current.configuration.getConfigList("renewalstatus") map { stream =>
    stream.map{
      item =>
        RenewalStatusTransform(item.getString("toStatus").get, item.getStringList("fromStatus").get)
    }.toList
  }

}
