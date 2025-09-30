

import java.util.*;

interface Observer {
    void update(String weather);
}

class WeatherStation {
    private List<Observer> observers = new ArrayList<>();
    private String weather;

    public void addObserver(Observer o) { observers.add(o); }
    public void setWeather(String w) {
        weather = w;
        for (Observer o : observers) o.update(weather);
    }
}

class PhoneDisplay implements Observer {
    public void update(String weather) {
        System.out.println("Phone shows weather: " + weather);
    }
}

public class ObserverDemo {
    public static void main(String[] args) {
        WeatherStation ws = new WeatherStation();
        ws.addObserver(new PhoneDisplay());
        ws.setWeather("Sunny");
        ws.setWeather("Rainy");
    }
}

