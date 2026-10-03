package kr.watchlite;
public final class CoreTest {
    static int checks;
    static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    public static void main(String[] args){
        check(Core.distance(37,127,37,127)==0,"same point");
        check(Math.abs(Core.distance(0,0,0,1)-111195)<5,"equator distance");
        check(Core.distance(0,179.999,0,-179.999)<230,"antimeridian");
        check(Math.abs(Core.bearing(0,0,0,1)-90)<0.01,"east bearing");
        check(Core.cardinal(359).equals("북"),"north wrap");
        check(!Core.usable(5,31000),"stale fix");
        check(!Core.usable(5,-1),"invalid age");
        check(!Core.usable(36,0),"inaccurate fix");
        check(Core.usable(10,1000),"good fix");
        check(!Core.reached(5,30),"inaccurate arrival blocked");
        check(!Core.reached(30,10),"distant arrival blocked");
        check(Core.reached(8,10),"near arrival");
        check(Double.isNaN(Core.rate(80,79,7200000)),"one percent insufficient");
        check(Double.isNaN(Core.rate(80,70,60000)),"short sample");
        check(Double.isNaN(Core.rate(80,85,7200000)),"charge not drain");
        check(Core.rate(80,76,7200000)==2,"drain rate");
        check(Core.remaining(85,2)==40,"reserve estimation");
        check(Core.remaining(3,2)==0,"reserve floor");
        System.out.println(checks+" core checks passed");
    }
}
