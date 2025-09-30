interface Shape { void draw(); }

class Circle implements Shape {
    public void draw() { System.out.println("Drawing Circle"); }
}
class Square implements Shape {
    public void draw() { System.out.println("Drawing Square"); }
}

class ShapeFactory {
    public static Shape getShape(String type) {
        if (type.equalsIgnoreCase("circle")) return new Circle();
        if (type.equalsIgnoreCase("square")) return new Square();
        return null;
    }
}

public class FactoryDemo {
    public static void main(String[] args) {
        Shape s1 = ShapeFactory.getShape("circle");
        s1.draw();
        Shape s2 = ShapeFactory.getShape("square");
        s2.draw();
    }
}
