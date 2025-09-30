class Config {
    private static Config instance;
    private Config() {}
    public static Config getInstance() {
        if (instance == null) instance = new Config();
        return instance;
    }
    public void show() { System.out.println("Config Loaded."); }
}

public class SingletonDemo {
    public static void main(String[] args) {
        Config c1 = Config.getInstance();
        Config c2 = Config.getInstance();
        c1.show();
        System.out.println("Same object? " + (c1 == c2));
    }
}
