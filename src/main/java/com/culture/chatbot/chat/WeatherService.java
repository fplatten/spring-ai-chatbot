package com.culture.chatbot.chat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Component
public class WeatherService {

    private static final String GEOCODING_API_URL = "https://geocoding-api.open-meteo.com/v1/search";
    private static final String WEATHER_API_URL = "https://api.open-meteo.com/v1/forecast";
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Tool(name = "getWeather", description = "Get the current weather for a specified city or town.")
    public String getWeather(String city) {
        try {
            // 1. Geocode the city to get latitude and longitude
            String encodedCity = URLEncoder.encode(city, StandardCharsets.UTF_8);
            URI geocodingUri = URI.create(GEOCODING_API_URL + "?name=" + encodedCity + "&count=1");

            HttpRequest geoRequest = HttpRequest.newBuilder().uri(geocodingUri).build();
            HttpResponse<String> geoResponse = httpClient.send(geoRequest, HttpResponse.BodyHandlers.ofString());

            if (geoResponse.statusCode() != 200) {
                return "Error fetching coordinates for " + city;
            }

            JsonNode geoData = objectMapper.readTree(geoResponse.body());
            JsonNode results = geoData.path("results");
            if (results.isMissingNode() || results.isEmpty()) {
                return "Could not find coordinates for " + city + ". Please try a different city name.";
            }

            JsonNode location = results.get(0);
            double latitude = location.get("latitude").asDouble();
            double longitude = location.get("longitude").asDouble();

            // 2. Fetch the weather for the coordinates
            URI weatherUri = URI.create(WEATHER_API_URL + "?latitude=" + latitude + "&longitude=" + longitude + "&current_weather=true");
            HttpRequest weatherRequest = HttpRequest.newBuilder().uri(weatherUri).build();
            HttpResponse<String> weatherResponse = httpClient.send(weatherRequest, HttpResponse.BodyHandlers.ofString());

            if (weatherResponse.statusCode() != 200) {
                return "Error fetching weather data for " + city;
            }

            JsonNode weatherData = objectMapper.readTree(weatherResponse.body());
            JsonNode currentWeather = weatherData.path("current_weather");
            if (currentWeather.isMissingNode()) {
                return "No current weather data available for " + city;
            }

            double temperature = currentWeather.get("temperature").asDouble();
            int weatherCode = currentWeather.get("weathercode").asInt();
            String description = getWeatherDescription(weatherCode);

            return String.format("The current weather in %s is %s with a temperature of %.1f°C.", city, description, temperature);

        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            return "An error occurred while connecting to the weather service: " + e.getMessage();
        }
    }

    @Tool(name = "getDatetime", description = "Get the current date and time.")
    public String getDateTime() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return "The current date and time is " + now.format(formatter);
    }

    private String getWeatherDescription(int weatherCode) {
        Map<Integer, String> codes = new HashMap<>();
        codes.put(0, "Clear sky");
        codes.put(1, "Mainly clear");
        codes.put(2, "Partly cloudy");
        codes.put(3, "Overcast");
        codes.put(45, "Fog");
        codes.put(48, "Depositing rime fog");
        codes.put(51, "Drizzle: Light");
        codes.put(53, "Drizzle: Moderate");
        codes.put(55, "Drizzle: Dense intensity");
        codes.put(56, "Freezing Drizzle: Light");
        codes.put(57, "Freezing Drizzle: Dense intensity");
        codes.put(61, "Rain: Slight");
        codes.put(63, "Rain: Moderate");
        codes.put(65, "Rain: Heavy intensity");
        codes.put(66, "Freezing Rain: Light");
        codes.put(67, "Freezing Rain: Heavy intensity");
        codes.put(71, "Snow fall: Slight");
        codes.put(73, "Snow fall: Moderate");
        codes.put(75, "Snow fall: Heavy intensity");
        codes.put(77, "Snow grains");
        codes.put(80, "Rain showers: Slight");
        codes.put(81, "Rain showers: Moderate");
        codes.put(82, "Rain showers: Violent");
        codes.put(85, "Snow showers: Slight");
        codes.put(86, "Snow showers: Heavy");
        codes.put(95, "Thunderstorm: Slight or moderate");
        codes.put(96, "Thunderstorm with slight hail");
        codes.put(99, "Thunderstorm with heavy hail");
        return codes.getOrDefault(weatherCode, "Unknown weather condition");
    }
}
