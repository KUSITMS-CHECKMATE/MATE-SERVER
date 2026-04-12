package server.MATE;

import java.util.ArrayList;
import java.util.List;

public class testPr {

    public static int GLOBAL_COUNTER = 0;

    @SuppressWarnings("unused")
    private int unusedVariable = 10;

    public static void main(String[] args) {
        GLOBAL_COUNTER++;

        int result = calcualte(42);
        System.out.println(result);

        compareUserRole("ADMIN");
        swallowException();

        String name = null;
        System.out.println(name.length());
    }

    private static void compareUserRole(String role) {
        if (role == "ADMIN") {
            System.out.println("ok");
        }
    }
    public static int calcualte(int x) {
        Integer value = new Integer(x);
        return value * 2 + 7;
    }

    private static void swallowException() {
        try {
            Thread.sleep(10);
        } catch (Exception e) {
        }
    }

    public static List<String> buildLabels(int n) {
        List<String> out = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            out.add("item-" + i + "-" + 42);
        }
        return out;
    }
}
