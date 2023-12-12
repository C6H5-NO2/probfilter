package probfilter.pdsa;


public class JavaFunnels {
    private JavaFunnels() {}

    public static void stringFunnel(String from, Sink into) {
        ScalaFunnels.StringFunnel$.MODULE$.funnel(from, into);
    }

    public static void intFunnel(Integer from, Sink into) {
        ScalaFunnels.IntFunnel$.MODULE$.funnel(from.intValue(), into);
    }

    public static void byteFunnel(Byte from, Sink into) {
        ScalaFunnels.ByteFunnel$.MODULE$.funnel(from.byteValue(), into);
    }
}
