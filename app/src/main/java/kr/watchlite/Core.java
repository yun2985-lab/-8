package kr.watchlite;

/** Platform-independent policy and navigation, reusable by a future host. */
public final class Core {
    private Core() {}
    public static double distance(double a, double b, double c, double d) {
        double p=Math.toRadians(c-a), q=Math.toRadians(d-b);
        double h=Math.sin(p/2)*Math.sin(p/2)+Math.cos(Math.toRadians(a))*Math.cos(Math.toRadians(c))*Math.sin(q/2)*Math.sin(q/2);
        return 6371000*2*Math.asin(Math.sqrt(Math.max(0,Math.min(1,h))));
    }
    public static double bearing(double a,double b,double c,double d) {
        double p=Math.toRadians(a), q=Math.toRadians(c), dl=Math.toRadians(d-b);
        return (Math.toDegrees(Math.atan2(Math.sin(dl)*Math.cos(q),Math.cos(p)*Math.sin(q)-Math.sin(p)*Math.cos(q)*Math.cos(dl)))+360)%360;
    }
    public static String cardinal(double bearing) {
        return new String[]{"북","북동","동","남동","남","남서","서","북서"}[(int)Math.round(bearing/45)%8];
    }
    public static boolean usable(float accuracy,long ageMs) { return accuracy>0 && accuracy<=35 && ageMs>=0 && ageMs<=30000; }
    public static boolean reached(double distance,float accuracy) { return accuracy>0 && accuracy<=20 && distance<=Math.max(10,accuracy); }
    public static double rate(int start,int now,long durationMs) {
        if(durationMs<3600000 || start-now<2 || now<0 || start>100) return Double.NaN;
        return (start-now)*3600000.0/durationMs;
    }
    public static double remaining(int battery,double rate) { return rate>0 ? Math.max(0,battery-5)/rate : Double.NaN; }
}
