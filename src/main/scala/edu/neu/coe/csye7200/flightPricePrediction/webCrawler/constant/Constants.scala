package edu.neu.coe.csye7200.flightPricePrediction.webCrawler.constant

import edu.neu.coe.csye7200.flightPricePrediction.webCrawler.MyJsonParser.read

/** @author Caspar
  * @date 2022/4/20 19:11
  */
object Constants {
  val AIRLINE_MAP_PATH = "airline_map.json"
  val CANDIDATE_CITIES = "./candidate_cities.txt"
  val CANDIDATE_CITIES_EXAMPLE = "./candidate_cities_example.txt"
  private val AIRLINE_MAP = read(AIRLINE_MAP_PATH)
  def getAirLine(str: String) = AIRLINE_MAP.get(str)
}
