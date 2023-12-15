package probfilter.pdsa;

import java.io.Serializable;


public class JavaFunnels implements Serializable {
    private JavaFunnels() {}

    public static void stringFunnel(String from, Sink into) {
        ScalaFunnels.StringFunnel$.MODULE$.funnel(from, into);
    }

    public static void intFunnel(Integer from, Sink into) {
        ScalaFunnels.IntFunnel$.MODULE$.funnel(from.intValue(), into);
    }
}
