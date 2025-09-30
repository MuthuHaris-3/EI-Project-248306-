interface EuropeanPlug { void connectEuropean(); }
class EuropeanCharger implements EuropeanPlug {
    public void connectEuropean() { System.out.println("Connected to European socket."); }
}

interface USPlug { void connectUS(); }
class AdapterEUtoUS implements USPlug {
    private EuropeanPlug plug;
    public AdapterEUtoUS(EuropeanPlug plug) { this.plug = plug; }
    public void connectUS() { plug.connectEuropean(); }
}

public class AdapterDemo {
    public static void main(String[] args) {
        EuropeanPlug euCharger = new EuropeanCharger();
        USPlug usSocket = new AdapterEUtoUS(euCharger);
        usSocket.connectUS();
    }
}
