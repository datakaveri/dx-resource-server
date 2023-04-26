package iudx.resource.server.apiserver.query;

import static iudx.resource.server.apiserver.util.Constants.IUDXQUERY_OPTIONS;
import static iudx.resource.server.apiserver.util.Constants.MSG_INVALID_PARAM;
import static iudx.resource.server.apiserver.util.Constants.NGSILDQUERY_ATTRIBUTE;
import static iudx.resource.server.apiserver.util.Constants.NGSILDQUERY_COORDINATES;
import static iudx.resource.server.apiserver.util.Constants.NGSILDQUERY_ENDTIME;
import static iudx.resource.server.apiserver.util.Constants.NGSILDQUERY_FROM;
import static iudx.resource.server.apiserver.util.Constants.NGSILDQUERY_GEOMETRY;
import static iudx.resource.server.apiserver.util.Constants.NGSILDQUERY_GEOPROPERTY;
import static iudx.resource.server.apiserver.util.Constants.NGSILDQUERY_GEOREL;
import static iudx.resource.server.apiserver.util.Constants.NGSILDQUERY_ID;
import static iudx.resource.server.apiserver.util.Constants.NGSILDQUERY_MAXDISTANCE;
import static iudx.resource.server.apiserver.util.Constants.NGSILDQUERY_MINDISTANCE;
import static iudx.resource.server.apiserver.util.Constants.NGSILDQUERY_Q;
import static iudx.resource.server.apiserver.util.Constants.NGSILDQUERY_SIZE;
import static iudx.resource.server.apiserver.util.Constants.NGSILDQUERY_TIME;
import static iudx.resource.server.apiserver.util.Constants.NGSILDQUERY_TIMEREL;
import static iudx.resource.server.apiserver.util.Constants.NGSILDQUERY_TYPE;
import static iudx.resource.server.apiserver.util.Util.toUriFunction;

import io.vertx.core.MultiMap;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** NGSILDQueryParams Class to parse query parameters from HTTP request. */
public class NgsildQueryParams {
  private static final Logger LOGGER = LogManager.getLogger(NgsildQueryParams.class);

  private List<URI> id;
  private List<String> type;
  private List<String> attrs;
  private List<String> idPattern;
  private String textQuery;
  private GeoRelation geoRel;
  private String geometry;
  private String coordinates;
  private String geoProperty;
  private TemporalRelation temporalRelation;
  private String options;
  private String pageFrom;
  private String pageSize;

  public NgsildQueryParams() {}

  /**
   * constructor a NGSILDParams passing query parameters map.
   *
   * @param paramsMap query paramater's map.
   */
  public NgsildQueryParams(MultiMap paramsMap) {
    this.setGeoRel(new GeoRelation());
    this.setTemporalRelation(new TemporalRelation());
    this.create(paramsMap);
  }

  /**
   * constructor a NGSILDParams passing json.
   *
   * @param json JsonObject of query.
   */
  public NgsildQueryParams(JsonObject json) {
    this.setGeoRel(new GeoRelation());
    this.setTemporalRelation(new TemporalRelation());
    this.create(json);
  }

  /**
   * This method is used to initialize a NGSILDQueryParams object from multimap of query parameters.
   *
   * @param paramsMap query paramater's map.
   */
  private void create(MultiMap paramsMap) {
    List<Entry<String, String>> entries = paramsMap.entries();
    for (final Entry<String, String> entry : entries) {
      switch (entry.getKey()) {
        case NGSILDQUERY_ID:
          this.id = new ArrayList<URI>();
          String[] ids = entry.getValue().split(",");
          List<URI> uris = Arrays.stream(ids).map(toUriFunction).collect(Collectors.toList());
          this.id.addAll(uris);
          break;
        case NGSILDQUERY_ATTRIBUTE:
          this.attrs = new ArrayList<String>();
          this.attrs.addAll(
              Arrays.stream(entry.getValue().split(",")).collect(Collectors.toList()));
          break;
        case NGSILDQUERY_GEOREL:
          String georel = entry.getValue();
          String[] values = georel.split(";");
          this.geoRel.setRelation(values[0]);
          if (values.length == 2) {
            String[] distance = values[1].split("=");
            if (distance[0].equalsIgnoreCase(NGSILDQUERY_MAXDISTANCE)) {
              this.geoRel.setMaxDistance(Double.parseDouble(distance[1]));
            } else if (distance[0].equalsIgnoreCase(NGSILDQUERY_MINDISTANCE)) {
              this.geoRel.setMinDistance(Double.parseDouble(distance[1]));
            }
          }
          break;
        case NGSILDQUERY_GEOMETRY:
          this.geometry = entry.getValue();
          break;
        case NGSILDQUERY_COORDINATES:
          this.coordinates = entry.getValue();
          break;
        case NGSILDQUERY_TIMEREL:
          this.temporalRelation.setTemprel(entry.getValue());
          break;
        case NGSILDQUERY_TIME:
          this.temporalRelation.setTime(entry.getValue());
          break;
        case NGSILDQUERY_ENDTIME:
          this.temporalRelation.setEndTime(entry.getValue());
          break;
        case NGSILDQUERY_Q:
          this.textQuery = entry.getValue();
          break;
        case NGSILDQUERY_GEOPROPERTY:
          this.geoProperty = entry.getValue();
          break;
        case IUDXQUERY_OPTIONS:
          this.options = entry.getValue();
          break;
        case NGSILDQUERY_SIZE:
          this.pageSize = entry.getValue();
          break;
        case NGSILDQUERY_FROM:
          this.pageFrom = entry.getValue();
          break;
        default:
          LOGGER.warn(MSG_INVALID_PARAM + ":" + entry.getKey());
          break;
      }
    }
  }

  private void create(JsonObject requestJson) {
    LOGGER.info("create from json started");
    requestJson.forEach(
        entry -> {
          LOGGER.debug("key ::" + entry.getKey() + " value :: " + entry.getValue());
          if (entry.getKey().equalsIgnoreCase(NGSILDQUERY_Q)) {
            this.textQuery = requestJson.getString(NGSILDQUERY_Q);
          } else if (entry.getKey().equalsIgnoreCase(NGSILDQUERY_ATTRIBUTE)) {
            this.attrs = new ArrayList<String>();
            this.attrs =
                Arrays.stream(entry.getValue().toString().split(",")).collect(Collectors.toList());
          } else if (entry.getKey().equalsIgnoreCase(NGSILDQUERY_TYPE)) {
            this.type = new ArrayList<String>();
            this.type =
                Arrays.stream(entry.getValue().toString().split(",")).collect(Collectors.toList());
          } else if (entry.getKey().equalsIgnoreCase("geoQ")) {
            JsonObject geoJson = requestJson.getJsonObject(entry.getKey());
            this.setGeometry(geoJson.getString("geometry"));
            this.setGeoProperty(geoJson.getString("geoproperty"));
            this.setCoordinates(geoJson.getJsonArray("coordinates").toString());
            if (geoJson.containsKey("georel")) {
              String georel = geoJson.getString("georel");
              String[] values = georel.split(";");
              this.geoRel.setRelation(values[0]);
              if (values.length == 2) {
                String[] distance = values[1].split("=");
                if (distance[0].equalsIgnoreCase(NGSILDQUERY_MAXDISTANCE)) {
                  this.geoRel.setMaxDistance(Double.parseDouble(distance[1]));
                } else if (distance[0].equalsIgnoreCase(NGSILDQUERY_MINDISTANCE)) {
                  this.geoRel.setMinDistance(Double.parseDouble(distance[1]));
                }
              }
            }
          } else if (entry.getKey().equalsIgnoreCase("temporalQ")) {
            JsonObject temporalJson = requestJson.getJsonObject(entry.getKey());
            this.temporalRelation.setTemprel(temporalJson.getString("timerel"));
            this.temporalRelation.setTime(temporalJson.getString("time"));
            this.temporalRelation.setEndTime(temporalJson.getString("endtime"));
          } else if (entry.getKey().equalsIgnoreCase("entities")) {
            JsonArray array = new JsonArray(entry.getValue().toString());
            Iterator<?> iter = array.iterator();
            while (iter.hasNext()) {
              this.id = new ArrayList<URI>();
              this.idPattern = new ArrayList<String>();
              JsonObject entity = (JsonObject) iter.next();
              String id = entity.getString("id");
              String idPattern = entity.getString("idPattern");
              if (id != null) {
                this.id.add(toUri(id));
              }
              if (idPattern != null) {
                this.idPattern.add(idPattern);
              }
            }
          } else if (entry.getKey().equalsIgnoreCase(IUDXQUERY_OPTIONS)) {
            this.options = requestJson.getString(entry.getKey());
          } else if (entry.getKey().equalsIgnoreCase(NGSILDQUERY_FROM)) {
            this.pageFrom = requestJson.getString(entry.getKey());
          } else if (entry.getKey().equalsIgnoreCase(NGSILDQUERY_SIZE)) {
            this.pageSize = requestJson.getString(NGSILDQUERY_SIZE);
          }
        });
  }

  private URI toUri(String source) {
    URI uri = null;
    try {
      uri = new URI(source);
    } catch (URISyntaxException e) {
      e.printStackTrace();
    }
    return uri;
  }

  public List<URI> getId() {
    return id;
  }

  public void setId(List<URI> id) {
    this.id = id;
  }

  public List<String> getType() {
    return type;
  }

  public void setType(List<String> type) {
    this.type = type;
  }

  public List<String> getAttrs() {
    return attrs;
  }

  public void setAttrs(List<String> attrs) {
    this.attrs = attrs;
  }

  public List<String> getIdPattern() {
    return idPattern;
  }

  public void setIdPattern(List<String> idPattern) {
    this.idPattern = idPattern;
  }

  public String getQ() {
    return textQuery;
  }

  public void setQ(String textQuery) {
    this.textQuery = textQuery;
  }

  public GeoRelation getGeoRel() {
    return geoRel;
  }

  public void setGeoRel(GeoRelation geoRel) {
    this.geoRel = geoRel;
  }

  public String getGeometry() {
    return geometry;
  }

  public void setGeometry(String geometry) {
    this.geometry = geometry;
  }

  public String getCoordinates() {
    return coordinates;
  }

  public void setCoordinates(String coordinates) {
    this.coordinates = coordinates;
  }

  public String getGeoProperty() {
    return geoProperty;
  }

  public void setGeoProperty(String geoProperty) {
    this.geoProperty = geoProperty;
  }

  public TemporalRelation getTemporalRelation() {
    return temporalRelation;
  }

  public void setTemporalRelation(TemporalRelation temporalRelation) {
    this.temporalRelation = temporalRelation;
  }

  public String getOptions() {
    return options;
  }

  public void setOptions(String options) {
    this.options = options;
  }

  public String getPageFrom() {
    return pageFrom;
  }

  public String getPageSize() {
    return pageSize;
  }

  @Override
  public String toString() {
    return "NGSILDQueryParams [id="
        + id
        + ", type="
        + type
        + ", attrs="
        + attrs
        + ", idPattern="
        + idPattern
        + ", textQuery="
        + textQuery
        + ", geoRel="
        + geoRel
        + ", geometry="
        + geometry
        + ", coordinates="
        + coordinates
        + ", geoProperty="
        + geoProperty
        + ", temporalRelation="
        + temporalRelation
        + ", options="
        + options
        + "]";
  }
}
