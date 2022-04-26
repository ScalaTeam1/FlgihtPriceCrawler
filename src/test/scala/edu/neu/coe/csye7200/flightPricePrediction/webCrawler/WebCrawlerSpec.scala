package edu.neu.coe.csye7200.flightPricePrediction.webCrawler

import com.neu.edu.FlightPricePrediction.db.MongoDBUtils
import edu.neu.coe.csye7200.flightPricePrediction.webCrawler.WebCrawler.{
  crawler,
  generateTasks
}
import org.scalatest._
import org.scalatest.concurrent.{Futures, ScalaFutures}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import org.scalatest.time.{Seconds, Span}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/** @author scalaprof
  */
class WebCrawlerSpec
    extends AnyFlatSpec
    with should.Matchers
    with Futures
    with ScalaFutures
    with TryValues
    with Inside {

  behavior of "web crawler"
  it should "work" in {
    val (orgs, dsts, dates) = generateTasks
    val jobs: Future[Seq[flightCleaned]] = crawler(orgs, dsts, dates)

    whenReady(MonadOps.sequence(jobs), timeout(Span(60, Seconds))) {
      case Left(e)   => throw e
      case Right(js) => MongoDBUtils.insertManyFlights(js.map(_.toFlight))
    }
  }

}
