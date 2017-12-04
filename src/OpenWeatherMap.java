import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import com.fasterxml.jackson.core.*;

import javax.net.ssl.SSLContext;

public class OpenWeatherMap {
    private String city;
    private String zip;
    private int cityid;
    private double tx;
    private double tn;
    private ArrayList<String> temps;


    public OpenWeatherMap(String str) throws IllegalArgumentException {
        this.temps = new ArrayList<>();
        this.getTemperatureOfDay(str);
    }

    private String sendHttpRequest(String url) {
        StringBuilder json = new StringBuilder();

        try {
            URL u = new URL(url + "&units=metric" + Constants.API);
            HttpURLConnection conn = (HttpURLConnection) u.openConnection();
            conn.setRequestMethod("GET");
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String output;
            while ((output = reader.readLine()) != null) {
                json.append(output);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return json.toString();
    }

    public String getCityName(String zip) {
        String name = null;
        String json = sendHttpRequest("http://api.openweathermap.org/data/2.5/forecast/daily?zip=" + zip + "," + Constants.COUNTYCODE + "&cnt=1");

        JsonFactory factory = new JsonFactory();
        JsonParser parser;
        boolean idFound = false;

        try {
            parser = factory.createParser(json);

            while(!parser.isClosed()){
                JsonToken jsonToken = parser.nextToken();

                if(JsonToken.FIELD_NAME.equals(jsonToken)){
                    String fieldName = parser.getCurrentName();

                    parser.nextToken();

                    if("name".equals(fieldName)){
                        name = parser.getValueAsString();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return name;
    }

    public void getTemperatureOfDay(String city) throws IllegalArgumentException {
        String json;

        if(city.matches("\\d{3,10}")) {
            city = getCityName(city);
        }

        if(city.matches("[a-zA-ZäÄöÖüÜß]{3,}")) {
            json = sendHttpRequest("http://api.openweathermap.org/data/2.5/forecast/daily?q=" + city + "," + Constants.COUNTYCODE + "&cnt=1");
        } else {
            throw new IllegalArgumentException("wrong zip-code or city name");
        }

        JsonFactory factory = new JsonFactory();
        JsonParser parser;
        boolean idFound = false;

        try {
            parser = factory.createParser(json);

            while(!parser.isClosed()){
                JsonToken jsonToken = parser.nextToken();

                if(JsonToken.FIELD_NAME.equals(jsonToken)){
                    String fieldName = parser.getCurrentName();

                    parser.nextToken();

                    if("max".equals(fieldName)){
                        this.tx = parser.getDoubleValue();
                    } else if("min".equals(fieldName)) {
                        this.tn = parser.getDoubleValue();
                    } else if("id".equals(fieldName) && !idFound) {
                        this.cityid = parser.getValueAsInt();
                        idFound = true;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void getHourlyTemps() {
        String json = sendHttpRequest("http://api.openweathermap.org/data/2.5/forecast?id=" + this.cityid + "&cnt=8");

        JsonFactory factory = new JsonFactory();
        JsonParser parser;

        try {
            parser = factory.createParser(json);

            String temp = "";
            String date = "";
            while (!parser.isClosed()) {
                JsonToken jsonToken = parser.nextToken();

                if (JsonToken.FIELD_NAME.equals(jsonToken)) {
                    String fieldName = parser.getCurrentName();

                    parser.nextToken();

                    if ("temp".equals(fieldName)) {
                        temp = parser.getValueAsString();
                        date = "";
                    } else if ("dt_txt".equals(fieldName)) {
                        date = parser.getValueAsString();
                    }

                    if (temp.length() > 0 && date.length() > 0) {
                        this.temps.add(date);
                        this.temps.add(temp);
                        temp = "";
                        date = "";
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void printTemps() {
        List<String> hours = Arrays.asList("06", "12", "18", "00");

        System.out.println("Datum                    |      Temperatur");

        for(int i = 0; i < temps.size(); i += 2) {
            if(hours.contains(temps.get(i).split("[ :]")[1]))
                System.out.println(temps.get(i) + "      |      " + temps.get(i+1) + "°C" );
        }
    }

    public double getTx() {
        return tx;
    }

    public double getTn() {
        return tn;
    }

    public static void main(String[] args) {
        OpenWeatherMap o1 = new OpenWeatherMap("32469");

        System.out.println("Minimale Temperatur: " + o1.getTn() + "°C\nMaximale Temperatur: " + o1.getTx() + "°C");

        o1.getHourlyTemps();
        o1.printTemps();
    }
}
