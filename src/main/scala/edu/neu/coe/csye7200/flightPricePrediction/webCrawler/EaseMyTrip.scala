package edu.neu.coe.csye7200.flightPricePrediction.webCrawler

import scalaj.http.{Http, HttpOptions, HttpRequest, HttpResponse}
import spray.json._
import MyJsonParser.extract
import com.neu.edu.FlightPricePrediction.pojo.Flight
import constant.Constants._
import spray.json.DefaultJsonProtocol.{StringJsonFormat, mapFormat}

import java.text.SimpleDateFormat
import java.util.Calendar
import scala.language.postfixOps
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
    val cityMap = extract(r, "A" :: Nil).convertTo[Map[String, String]]
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
    val fdis = for (p <- prices) yield {
      val k = p._1.parseJson.asInstanceOf[JsArray].elements.head.toString
      val ko = k.substring(1, k.length - 1)
      Tuple3(ko, fdm.get(ko), p._2)
    }
    (fdis, cityMap)
  }

  def search(
      org: String,
      dst: String,
      date: String
  ): Seq[flightCleaned] = {
    val t23 = flightDetailInfo(org, dst, date)
    t23._1.filter(x => x._2.nonEmpty).map { e =>
      e._2.get.clean(e._3.TF, t23._2)
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

case class flightPrice(TF: Int, FIA: Int, TTDIS: Int)

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
) {
  override def toString: String =
    s"flightDetail OG: $OG, DT: $DT, DDT: $DDT, ADT: $ADT, DTM: $DTM, ATM: $ATM, FN: $FN, AC: $AC, STP: $STP, CB: $CB, DUR: $DUR, DTER: $DTER, ATER: $ATER, FlightDetailRefKey: $FlightDetailRefKey, ProviderCode: $ProviderCode"
  def categoryTime(time: String): String = {
    val arr = time.split(":")
    val a = arr(0).toInt
    val b = arr(1).toInt
    (a, b) match {
      case (0, 0)           => "Night"
      case (a, _) if a < 4  => "Late_Night"
      case (4, 0)           => "Late_Night"
      case (a, _) if a < 8  => "Early_Morning"
      case (8, 0)           => "Early_Morning"
      case (a, _) if a < 12 => "Morning"
      case (12, 0)          => "Morning"
      case (a, _) if a < 16 => "Afternoon"
      case (16, 0)          => "Afternoon"
      case (a, _) if a < 20 => "Evening"
      case (20, 0)          => "Evening"
      case (_, _)           => "Night"
    }
  }
  def calculateDate(ddt: String): Int = {
    val now = Calendar.getInstance()
    val sdf = new SimpleDateFormat("EEE-ddMMMyyyy")
    val nowDate = sdf.parse(sdf.format(now.getTime))
    val departDate = sdf.parse(ddt)
    (departDate.getTime - nowDate.getTime) / (1000 * 3600 * 24) toInt
  }
  def clean(price: Int, cityMap: Map[String, String]): flightCleaned = {
    val airline = getAirLine(AC)
    val flight = AC + "-" + FN
    val sourceCity = cityMap.get(OG) match {
      case Some(str) => str
      case _ =>
        val str = toString
        throw new Exception(s"Data cleaning error for the flight $str!")
    }
    val departureTime = categoryTime(DTM)
    val stops = STP.toInt match {
      case 0 => "zero"
      case 1 => "one"
      case _ => "two_or_more"
    }
    val arrivalTime = categoryTime(ATM)
    val destinationCity = cityMap.get(DT) match {
      case Some(str) => str
      case _ =>
        val str = toString
        throw new Exception(s"Data cleaning error for the flight $str!")
    }
    val classType = CB match {
      case "ECONOMY" => 0
      case _         => 1
    }
    val duration = {
      val arr = DUR.split(" ")
      arr(0)
        .substring(0, arr(0).length - 1)
        .toDouble + (arr(1).substring(0, arr(1).length - 1).toDouble / 60)
    }
    val daysLeft = calculateDate(DDT)
    flightCleaned(
      airline.get,
      flight,
      sourceCity,
      departureTime,
      stops,
      arrivalTime,
      destinationCity,
      classType.toString,
      duration,
      daysLeft,
      price
    )
  }
}

case class flightCleaned(
    airline: String,
    flight: String,
    sourceCity: String,
    departureTime: String,
    stops: String,
    arrivalTime: String,
    destinationCity: String,
    classType: String,
    duration: Double,
    daysLeft: Int,
    price: Int
) {
  def toFlight: Flight = {
    new Flight(
      0,
      airline,
      flight,
      sourceCity,
      departureTime,
      stops,
      arrivalTime,
      destinationCity,
      classType,
      duration,
      daysLeft,
      price
    )
  }
}

case class ResponseLogin(
    Status: String,
    Message: String,
    servervariables: String
)

case class RequestException(msg: String) extends Exception
