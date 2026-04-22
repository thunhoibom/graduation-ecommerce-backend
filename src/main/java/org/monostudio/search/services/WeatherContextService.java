package org.monostudio.search.services;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.monostudio.api.models.WeatherContextPojo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@Slf4j
@RequiredArgsConstructor
public class WeatherContextService {

    private static final String WEATHER_API_BASE_URL = "https://api.open-meteo.com";

    @Value("${monostudio.store.location.latitude:21.028511}")
    private double defaultLatitude;

    @Value("${monostudio.store.location.longitude:105.804817}")
    private double defaultLongitude;

    public WeatherContextPojo resolve(Double latitude, Double longitude) {
        double lat = latitude != null ? latitude : defaultLatitude;
        double lon = longitude != null ? longitude : defaultLongitude;

        try {
            OpenMeteoResponse response = RestClient.create(WEATHER_API_BASE_URL)
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1/forecast")
                            .queryParam("latitude", lat)
                            .queryParam("longitude", lon)
                            .queryParam("current", "temperature_2m,weather_code")
                            .queryParam("timezone", "auto")
                            .build())
                    .retrieve()
                    .body(OpenMeteoResponse.class);

            if (response != null && response.getCurrent() != null && response.getCurrent().getTemperature_2m() != null) {
                Double temperature = response.getCurrent().getTemperature_2m();
                Integer weatherCode = response.getCurrent().getWeather_code();
                return WeatherContextPojo.builder()
                        .temperature(temperature)
                        .condition(mapWeatherCodeToCondition(weatherCode))
                        .weatherTag(mapWeatherCodeToTag(weatherCode))
                        .build();
            }
        } catch (Exception ex) {
            log.warn("Weather API call failed for lat={}, lon={}. Fallback to clear context. Reason: {}",
                    lat, lon, ex.getMessage());
        }

        return WeatherContextPojo.builder()
                .temperature(25.0)
                .condition("clear")
                .weatherTag("clear")
                .build();
    }

    private String mapWeatherCodeToCondition(Integer weatherCode) {
        if (weatherCode == null) return "clear";
        if (weatherCode >= 51 && weatherCode <= 67) return "rain";
        if (weatherCode >= 71 && weatherCode <= 77) return "snow";
        if (weatherCode >= 80 && weatherCode <= 99) return "rain";
        if (weatherCode == 45 || weatherCode == 48) return "fog";
        if (weatherCode == 1 || weatherCode == 2 || weatherCode == 3) return "cloudy";
        return "clear";
    }

    private String mapWeatherCodeToTag(Integer weatherCode) {
        String condition = mapWeatherCodeToCondition(weatherCode);
        return switch (condition) {
            case "rain" -> "rain";
            case "snow" -> "cold";
            case "fog" -> "windy";
            case "cloudy" -> "cloudy";
            default -> "clear";
        };
    }

    @Data
    private static class OpenMeteoResponse {
        private CurrentWeather current;
    }

    @Data
    private static class CurrentWeather {
        private Double temperature_2m;
        private Integer weather_code;
    }
}
