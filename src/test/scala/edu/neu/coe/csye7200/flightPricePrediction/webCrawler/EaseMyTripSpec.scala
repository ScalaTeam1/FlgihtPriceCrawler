package edu.neu.coe.csye7200.flightPricePrediction.webCrawler

import org.scalatest.concurrent.{Futures, ScalaFutures}
import org.scalatest.flatspec
import org.scalatest.matchers.should

import java.text.SimpleDateFormat
import java.util.Calendar

/** @author Caspar
  * @date 2022/4/13 18:46
  */
class EaseMyTripSpec
    extends flatspec.AnyFlatSpec
    with should.Matchers
    with Futures
    with ScalaFutures {

  behavior of "login"
  it should "work" in {
    EaseMyTrip.requestLogin.get.code shouldBe 200
  }

  behavior of "flight search"
  it should "work" in {
    val cal = Calendar.getInstance();
    cal.add(Calendar.MONTH, 1) // next month
    val sdf = new SimpleDateFormat("yyyy-MM-dd")
    val prices = EaseMyTrip.search("BOM", "DEL", sdf.format(cal.getTime))

    assert(prices.size > 0)
  }
}
