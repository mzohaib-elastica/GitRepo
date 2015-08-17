package locationservice;

import com.opencsv.CSVWriter;
import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * CSVConverter generates CSV file out of GoEuro Webservice data
 * @author mzohaib
 */
public class CSVConverter {

    private static final String DEFAULT_WEBSERVICE_URL = "http://api.goeuro.com/api/v2/position/suggest/en/";
    private final String url;

    private static final String[] DATA_FIELDS = new String[]{"_id", "key", "name", "fullName",
        "iata_airport_code", "type", "country", "latitude", "longitude",
        "locationId", "inEurope", "countryCode", "coreCountry", "distance"};

    public CSVConverter(String cityName) {
        url = DEFAULT_WEBSERVICE_URL + cityName;
    }

    /**
     * Reads and returns data from the given Reader object into a string
     *
     * @param rd
     * @return
     * @throws IOException
     */
    private static String readAll(Reader rd) throws IOException {
        StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }
        return sb.toString();
    }

    /**
     * Reads json data from the webservice and returns corresponding json array
     *
     * @param webserviceUrl
     * @return JSONObject: containing webservice data
     * @throws IOException: when something goes wrong while reading data from
     * url
     * @throws JSONException: when something goes wrong while reading JSON data
     */
    public static JSONArray getJsonData(String webserviceUrl) throws IOException, JSONException {
        try (InputStream is = new URL(webserviceUrl).openStream()) {
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
            String jsonText = readAll(rd);
            JSONArray json = new JSONArray(jsonText);
            return json;
        }
    }

    /**
     * Constructs and returns String array using fields of the webservice json
     * object
     *
     * @param placemark: webservice json object
     * @return String[]: Location Info
     */
    private static String[] getLocationInfoFromJSON(JSONObject placemark) {
        int id = placemark.getInt(DATA_FIELDS[0]);
        String key = placemark.optString(DATA_FIELDS[1]);
        String name = placemark.getString(DATA_FIELDS[2]);
        String fullName = placemark.getString(DATA_FIELDS[3]);
        String iataAirportCode = placemark.optString(DATA_FIELDS[4]);
        String type = placemark.getString(DATA_FIELDS[5]);
        String country = placemark.getString(DATA_FIELDS[6]);

        JSONObject coordinateData = placemark.getJSONObject("geo_position");
        double latitude = coordinateData.getDouble(DATA_FIELDS[7]);
        double longitude = coordinateData.getDouble(DATA_FIELDS[8]);

        Integer locationId = placemark.optInt(DATA_FIELDS[9]);
        boolean inEurope = placemark.getBoolean(DATA_FIELDS[10]);
        String countryCode = placemark.getString(DATA_FIELDS[11]);
        boolean coreCountry = placemark.getBoolean(DATA_FIELDS[12]);
        Integer distance = placemark.optInt(DATA_FIELDS[13]);

        String[] locationData = new String[]{String.valueOf(id),
            key, name, fullName, iataAirportCode, type, country,
            String.valueOf(latitude), String.valueOf(longitude),
            locationId == 0 ? "" : String.valueOf(locationId),
            String.valueOf(inEurope), String.valueOf(countryCode),
            String.valueOf(coreCountry), distance == 0 ? "" : String.valueOf(distance)};

        return locationData;
    }
    /**
     * Writes webservice data to the given csv file path
     * @param csvOutputPath
     * @throws IOException
     * @throws JSONException 
     */
    public void generateCSV(String csvOutputPath) throws IOException, JSONException {
        final JSONArray placemarkList = getJsonData(url);
        final List<String[]> locationInfoList = new ArrayList<>(placemarkList.length());
        final CSVWriter writer = new CSVWriter(new FileWriter(csvOutputPath));
        locationInfoList.add(DATA_FIELDS);

        for (int index = 0; index < placemarkList.length(); ++index) {
            JSONObject placemark = placemarkList.getJSONObject(index);
            String[] cityInfo = getLocationInfoFromJSON(placemark);
            locationInfoList.add(cityInfo);
        }

        writer.writeAll(locationInfoList);
        writer.close();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        if (args.length > 0) {
            try {
                CSVConverter converter = new CSVConverter(args[0]);
                String filename = args[0] + ".csv";
                converter.generateCSV(filename);
                System.err.println("file " + filename + " generated at:" + System.getProperty("user.dir"));

            } catch (JSONException ex) {
                System.err.println("JSONException in service reading data:" + ex.getMessage());
            } catch (IOException ex) {
                System.err.println("IOException in service reading data:" + ex.getMessage());
            }
        } else {
            System.err.println("Missing \"city name\" in command line argument,run java -jar GoEuroTest.jar CITY_NAME");

        }
    }

}
