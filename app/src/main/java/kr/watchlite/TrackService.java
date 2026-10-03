package kr.watchlite;
import android.app.*;
import android.content.*;
import android.content.pm.ServiceInfo;
import android.location.*;
import android.os.*;

public final class TrackService extends Service implements LocationListener {
    static boolean alive=false;
    private LocationManager manager;
    private Store store;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private boolean recording=false, marking=false;
    private Location lastRecorded;
    private final Runnable markTimeout=()->{marking=false;status("위치 저장 실패: 야외에서 다시 시도하세요");if(!recording)stopSelf();};
    static android.content.SharedPreferences prefs(Context c){return c.getSharedPreferences("watchlite",0);}
    static void status(Context c,String text){prefs(c).edit().putString("status",text).apply();}
    private void status(String t){status(this,t);}
    public void onCreate(){super.onCreate();alive=true;store=new Store(this);manager=getSystemService(LocationManager.class);}
    public int onStartCommand(Intent intent,int flags,int id){
        String action=intent==null?"STOP":intent.getAction();
        if("STOP".equals(action)){stopSelf();return START_NOT_STICKY;}
        if(checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)!=android.content.pm.PackageManager.PERMISSION_GRANTED){status("정확한 위치 권한이 필요합니다");stopSelf();return START_NOT_STICKY;}
        NotificationManager nm=getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel("tracking","위치 기록",NotificationManager.IMPORTANCE_LOW));
        PendingIntent open=PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent stop=PendingIntent.getService(this,1,new Intent(this,TrackService.class).setAction("STOP"),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        Notification n=new Notification.Builder(this,"tracking").setSmallIcon(kr.watchlite.R.drawable.icon).setContentTitle("Watch Lite · GPS 사용 중").setContentText("탭하여 확인 · 중지 가능").setContentIntent(open).setOngoing(true).addAction(new Notification.Action.Builder(null,"중지",stop).build()).build();
        startForeground(1,n,ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
        if("RECORD".equals(action))recording=true;
        if("MARK".equals(action)){marking=true;handler.removeCallbacks(markTimeout);handler.postDelayed(markTimeout,60000);}
        prefs(this).edit().putBoolean("running",true).putBoolean("recording",recording).apply();
        try{
            manager.removeUpdates(this);
            if(!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)){status("GPS를 켠 뒤 다시 시작하세요");stopSelf();return START_NOT_STICKY;}
            // minTime is a request hint, not guaranteed hardware power cycling.
            long interval=marking?1000:(prefs(this).getBoolean("lite",false)?60000:10000);
            manager.requestLocationUpdates(LocationManager.GPS_PROVIDER,interval,0,this,Looper.getMainLooper());
            status("GPS 수신 대기 중 · 야외에서 사용하세요");
        }catch(RuntimeException e){status("위치 기능 사용 불가: 설정과 권한 확인");stopSelf();}
        return START_NOT_STICKY;
    }
    public void onLocationChanged(Location l){
        long age=(SystemClock.elapsedRealtimeNanos()-l.getElapsedRealtimeNanos())/1000000;
        if(!l.hasAccuracy() || !Core.usable(l.getAccuracy(),age)){status("GPS 정확도 부족 · 이동 안내 보류");return;}
        prefs(this).edit().putString("lat",Double.toString(l.getLatitude())).putString("lon",Double.toString(l.getLongitude())).putFloat("accuracy",l.getAccuracy()).putLong("fixElapsed",SystemClock.elapsedRealtime()).putLong("fixWall",System.currentTimeMillis()).apply();
        if(recording && (lastRecorded==null || lastRecorded.distanceTo(l)>=8)){
            if(store.count("route")>=5000){status("경로 5,000개 한도 · 기록 종료");stopSelf();return;}
            store.add("route",l);lastRecorded=new Location(l);
        }
        status(String.format(java.util.Locale.KOREA,"위치 수신 · 오차 ±%.0fm",l.getAccuracy()));
        if(marking){
            store.add("mark",l);marking=false;handler.removeCallbacks(markTimeout);status("현재 위치를 저장했습니다");
            if(!recording){stopSelf();return;}
            manager.removeUpdates(this);
            try{manager.requestLocationUpdates(LocationManager.GPS_PROVIDER,prefs(this).getBoolean("lite",false)?60000:10000,0,this,Looper.getMainLooper());}catch(SecurityException e){stopSelf();}
        }
    }
    public void onProviderDisabled(String p){status("GPS 꺼짐 · 기록 중단");stopSelf();}
    public void onProviderEnabled(String p){}
    public void onStatusChanged(String p,int s,Bundle b){}
    public void onDestroy(){alive=false;handler.removeCallbacksAndMessages(null);if(manager!=null)manager.removeUpdates(this);prefs(this).edit().putBoolean("running",false).putBoolean("recording",false).apply();if(store!=null)store.close();super.onDestroy();}
    public IBinder onBind(Intent i){return null;}
}
