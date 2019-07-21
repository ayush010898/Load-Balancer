package loadbalancer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class HTTPUrlConnection {

    private final String USER_AGENT = "Mozilla/5.0";

    public static void main(String[] args) throws Exception {

        HTTPUrlConnection http = new HTTPUrlConnection();
        System.out.println("Testing 1 - Send Http GET request");
        http.sendGet();
    }

    // HTTP GET request
    private void sendGet() throws Exception {

        String url = "http://localhost:8080?key=123";
        BufferedReader in = null;
        String responseLine = null;
        StringBuffer response = new StringBuffer();
        URL obj = new URL(url);
        HttpURLConnection connection = (HttpURLConnection) obj.openConnection();

        // Optional default is GET
        connection.setRequestMethod("GET");

        // Add request header
        connection.setRequestProperty("User-Agent", USER_AGENT);

        int responseCode = connection.getResponseCode();
        //System.out.println("\nSending 'GET' request to URL : " + url);
        //System.out.println("Response Code : " + responseCode);

        in = new BufferedReader(new InputStreamReader(connection.getInputStream()));


        while ((responseLine = in.readLine()) != null) {
            response.append(responseLine);
        }

        in.close();

        System.out.println(response.toString());
    }

}