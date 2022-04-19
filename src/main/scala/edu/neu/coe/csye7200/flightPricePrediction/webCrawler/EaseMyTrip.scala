package edu.neu.coe.csye7200.flightPricePrediction.webCrawler

import scalaj.http.{Http, HttpOptions, HttpRequest, HttpResponse}
import spray.json._
import MyJsonParser.extract

import scala.util.{Failure, Success}

/** @author Caspar
  * @date 2022/4/13 18:22
  */
object EaseMyTrip {

  val urlLogin =
    "https://flightservice-web.easemytrip.com/EmtAppService/Login/UserLogin"
  val urlSearch =
    "https://flightservice-web.easemytrip.com/EmtAppService/AirAvail_Lights/AirSearchLightFromCloudKTN"
  val loginData = "{\"UserName\":\"tech\",\"Passward\":\"\"}"
  def postRequest(url: String, dataStr: String, timeout: Int) = {
    val response = defaultHeader(Http(url).postData(dataStr))
      .option(HttpOptions.readTimeout(timeout))
      .asString
    response match {
      case HttpResponse(_, 200, _) => Success(response)
      case HttpResponse(b, c, _) =>
        Failure(throw RequestException(s"Bad request, code is $c, body is $b"))
    }
  }

  def requestLogin = postRequest(urlLogin, loginData, 10000)

  def requestSearch(data: FlightSearchFormData) =
    postRequest(urlSearch, data.toString(), 10000)

  def defaultHeader(request: HttpRequest) = {
    request
      .header(
        "sec-ch-ua",
        "\" Not A;Brand\";v=\"99\", \"Chromium\";v=\"100\", \"Google Chrome\";v=\"100\""
      )
      .header("Accept", "application/json, text/javascript, */*; q=0.01")
      .header("Content-Type", "application/json; charset=UTF-8")
      .header("sec-ch-ua-mobile", "?0")
      .header(
        "User-Agent",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/100.0.4896.75 Safari/537.36"
      )
      .header("sec-ch-ua-platform", "\"macOS\"")
      .header("host", "flightservice-web.easemytrip.com")
  }

  val defaultAccount = Account("tech", "")

  def getToken: String = {
    import MyJsonProtocol.{accountFormat, responseLoginFormat}
    val responseLogin = postRequest(
      urlLogin,
      defaultAccount.toJson(accountFormat).toString(),
      10000
    )

    responseLogin.get.body.parseJson
      .convertTo[ResponseLogin](responseLoginFormat)
      .Message
  }

  def flightDetailInfo(org: String, dst: String, date: String) = {
    val filter = FlightSearchFormData(org, dst, date, getToken)
    val r = requestSearch(filter).get.body.parseJson
    val dctFltDtl = extract(r, "dctFltDtl" :: Nil)
    val tps = dctFltDtl.asJsObject.fields.toList
    import MyJsonProtocol.{flightDetailFormat, flightPriceFormat}
    val fds = for (x <- tps) yield {
      val fd = x._2.convertTo[flightDetail](flightDetailFormat)
      Tuple2(fd.ProviderCode, fd)
    }
    val fdm = fds.toMap
    val jss = extract(r, "j" :: "s" :: Nil)
    val prices =
      for (x <- jss.asInstanceOf[JsArray].elements)
        yield Tuple2(
          extract(x, "b" :: "BkKY" :: Nil).toString(),
          x.asInstanceOf[JsObject]
            .fields("lstFr")
            .asInstanceOf[JsArray]
            .elements
            .head
            .asJsObject
            .convertTo[flightPrice](flightPriceFormat)
        )
    for (p <- prices) yield {
      val k = p._1.parseJson.asInstanceOf[JsArray].elements.head.toString
      val ko = k.substring(1, k.length - 1)
      Tuple3(ko, fdm.get(ko), p._2)
    }
  }

  def getPrice(org: String, dst: String, date: String) = {
    val t3 = flightDetailInfo(org, dst, date)
    t3.filter(x => x._2.nonEmpty).map { e =>
      (e._2, e._3.TF) match {
        case (
              Some(flightDetail(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o)),
              p
            ) =>
          Tuple16(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p)
        case _ => Nil
      }
    }
  }
}

object MyJsonProtocol extends DefaultJsonProtocol {
  implicit val accountFormat = jsonFormat2(Account)
  implicit val responseLoginFormat = jsonFormat3(ResponseLogin)
  implicit val flightDetailFormat = jsonFormat15(flightDetail)
  implicit val flightPriceFormat = jsonFormat3(flightPrice)
}

case class Account(userName: String, password: String)

//case class FlightSearchFormData(org: String, dept: String, deptDT: String, TKN: String, adt: String = "1"
//                                 , chd: String = "0", inf: String = "0", arrDT: String = null, userid: String = ""
//                                 , IsDoubelSeat: Boolean = false, isDomestic: String = "true", isOneway: Boolean = true
//                                 , airline: String = "undefined", Cabin: String = "0", currCode: String = "INR"
//                                 , appType: Int = 1, isSingleView: Boolean = false, ResType: Int = 2
//                                 , IsNBA: Boolean = true, CouponCode: String = "", IsArmedForce: Boolean = false
//                                 , AgentCode: String = "", IsWLAPP: Boolean = false, IsFareFamily: Boolean = false
//                                 , serviceid: String = "EMTSERVICE", serviceDepatment: String = ""
//                                 , IpAddress: String = "", LoginKey: String = "", UUID: String = "undefined"
//                               ) {
case class FlightSearchFormData(
    org: String,
    dept: String,
    deptDT: String,
    TKN: String
) {
  override def toString: String =
    s"""{"org":"$org","dept":"$dept","adt":"1","chd":"0","inf":"0","deptDT":"$deptDT","arrDT":null,"userid":"","IsDoubelSeat":false,"isDomestic":"true","isOneway":true,"airline":"undefined","Cabin":"0","currCode":"INR","appType":1,"isSingleView":false,"ResType":2,"IsNBA":true,"CouponCode":"","IsArmedForce":false,"AgentCode":"","IsWLAPP":false,"IsFareFamily":false,"serviceid":"EMTSERVICE","serviceDepatment":"","IpAddress":"","LoginKey":"","UUID":"undefined","TKN":"$TKN"}
    |""".stripMargin
}

case class flightPrice(TF: Double, FIA: Double, TTDIS: Double)

case class flightDetail(
    OG: String,
    DT: String,
    DDT: String,
    ADT: String,
    DTM: String,
    ATM: String,
    FN: String,
    AC: String,
    STP: String,
    CB: String,
    DUR: String,
    DTER: String,
    ATER: String,
    FlightDetailRefKey: String,
    ProviderCode: String
)

case class ResponseLogin(
    Status: String,
    Message: String,
    servervariables: String
)

case class RequestException(msg: String) extends Exception
