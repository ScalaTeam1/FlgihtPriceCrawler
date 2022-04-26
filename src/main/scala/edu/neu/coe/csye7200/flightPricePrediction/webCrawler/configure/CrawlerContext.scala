package edu.neu.coe.csye7200.flightPricePrediction.webCrawler.configure

import com.typesafe.config.ConfigFactory
import edu.neu.coe.csye7200.flightPricePrediction.webCrawler.MyJsonParser.read
import edu.neu.coe.csye7200.flightPricePrediction.webCrawler.configure.Constants._
import org.slf4j.{Logger, LoggerFactory}

import scala.io.Source

/** @author Caspar
  * @date 2022/4/26 02:52
  */
object CrawlerContext {
  val logger: Logger = LoggerFactory.getLogger(getClass.getSimpleName)

  val config = ConfigFactory.load(CONFIG_LOCATION)
  val crawlerConfig = config.getConfig(CONFIG_PREFIX)
  val airlineMapPath = crawlerConfig.getString(AIRLINE_MAP_PATH)
  val daysToCrawl = crawlerConfig.getInt(DAYS_TO_CRAWL)
  val candidate_cities_path = crawlerConfig.getString(CANDIDATE_CITIES)
  val candidate_cities: Array[String] =
    Source.fromResource(candidate_cities_path).mkString.split("\n")
  private val AIRLINE_MAP = read(airlineMapPath)
  def getAirLine(str: String) = AIRLINE_MAP.get(str)

  def showContext = {
    logger.info("=====  Crawler Config: =====")
    logger.info(s"days to crawl: $daysToCrawl")
    logger.info(s"candidate cities: ${candidate_cities.mkString(" ")}")
    logger.info("============================")
  }
  showContext
}
