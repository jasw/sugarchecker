package com.jasons.alexa.sugarchecker;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Arrays;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Collectors;
import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * Created by jason.wang on 22/03/18.
 */
public class WebSiteFetcher {

  private static final Logger log = LoggerFactory.getLogger(WebSiteFetcher.class);


  private static String websiteUrl;
  private static String timeZone;

  private static final String DEFAULT_SERVER_URL = "https://blackdot101.herokuapp.com/api/v1/entries";
  private static final String DEFAULT_TIME_ZONE = "Pacific/Auckland";

  static {
    websiteUrl = getEnvVariable("SERVER_URL",DEFAULT_SERVER_URL);
    timeZone = getEnvVariable("TIME_ZONE", DEFAULT_TIME_ZONE);
    TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of(timeZone)));
    log.info("Setting default timezone to:"+timeZone);
  }

  //looks it uses IOS-8601 format without :.
// date/time
// offset (hh:mm - "+00:00" when it's zero)
// offset (hhmm - "+0000" when it's zero)
// offset (hh - "Z" when it's zero)
// create formatter
  private final DateTimeFormatter FORMATTER = new DateTimeFormatterBuilder() //looks it uses IOS-8601 format without :.
      // date/time
      .append(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
      // offset (hh:mm - "+00:00" when it's zero)
      .optionalStart().appendOffset("+HH:MM", "+00:00").optionalEnd()
      // offset (hhmm - "+0000" when it's zero)
      .optionalStart().appendOffset("+HHMM", "+0000").optionalEnd()
      // offset (hh - "Z" when it's zero)
      .optionalStart().appendOffset("+HH", "Z").optionalEnd()
      // create formatter
      .toFormatter();

  private static String getEnvVariable(String name, String defaultValue){

    return System.getenv(name) != null ? System.getenv(name) : defaultValue;

  }

  public List<Result> processSite() throws IOException {

    CloseableHttpClient httpclient = HttpClients.createDefault();
    try {
      HttpGet httpget = new HttpGet(websiteUrl);

      log.debug("fetching  " + httpget.getRequestLine());

      // Create a custom response handler
      ResponseHandler<String> responseHandler = response -> {
        int status = response.getStatusLine().getStatusCode();
        if (status >= 200 && status < 300) {
          HttpEntity entity = response.getEntity();
          return entity != null ? EntityUtils.toString(entity) : null;
        } else {
          throw new ClientProtocolException("Unexpected response status: " + status);
        }
      };
      String responseBody = httpclient.execute(httpget, responseHandler);
      System.out.println(responseBody);
      log.debug(responseBody);
      return toResults(responseBody);
    } finally {
      try {
        httpclient.close();
      } catch (IOException e) {
        //
      }
    }
  }

  /**
   * Parse the responseBody.
   * @param responseBody looks like this:
   *
  2018-03-26T03:59:41.000+0000	1522036781857	154	FortyFiveDown	BLU00514
  2018-03-26T03:54:42.000+0000	1522036482155	160	FortyFiveDown	BLU00514
  2018-03-26T03:49:42.000+0000	1522036182092	166	FortyFiveDown	BLU00514
   *
   *
   * @return list of results
   */
  private List<Result> toResults(String responseBody) {

    return Arrays.stream(responseBody.split("\n")).map(this::lineToResult).collect(Collectors.toList());
  }

  private Result lineToResult(String line) {
    String[] wordsIn1stLine = line.split("\t+");
    String time = wordsIn1stLine[0];
    String value = wordsIn1stLine[2];
    Double sugarLevel = new BigDecimal(value).divide(BigDecimal.valueOf(18L),1, RoundingMode.HALF_UP).doubleValue();

    ZonedDateTime dt= ZonedDateTime.parse(time, FORMATTER);
    //dt = dt.withZoneSameInstant(ZoneId.of("Pacific/Auckland"));
    dt = dt.withZoneSameInstant(ZoneId.of("UTC"));

    return new Result(sugarLevel.toString(), AlexaDateUtil.getFormattedTime(Date.from(dt.toInstant())));
  }


  public static class Result{
    private String sugarLevel;
    private String time;

    public String getSugarLevel() {
      return sugarLevel;
    }

    public void setSugarLevel(String sugarLevel) {
      this.sugarLevel = sugarLevel;
    }

    public String getTime() {
      return time;
    }

    public void setTime(String time) {
      this.time = time;
    }

    public Result(String sugarLevel, String time) {
      this.sugarLevel = sugarLevel;
      this.time = time;
    }

    @Override
    public String toString() {
      return "Result{" +
          "sugarLevel='" + sugarLevel + '\'' +
          ", time='" + time + '\'' +
          '}';
    }
  }

  public static void main(String[] args) {
    try {
      System.out.println(TimeZone.getDefault());
      List<Result> result = new WebSiteFetcher().processSite();
      System.out.println(result);
      log.debug(result.toString());
      List<Result> lastFew = result.subList(0, 4);
      System.out.println("Test last 4:" + lastFew);
      String text = "";
      for (Result r : lastFew) {
        text += toText(r)+" \n";
      }
      System.out.println("Last 4 string:"+text);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static String toText(Result next) {
    return "At "+ next.getTime()+". The sugar level is "+ next.getSugarLevel()+". ";
  }
}
