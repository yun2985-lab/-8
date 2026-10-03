package kr.watchlite;
import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.location.Location;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import java.util.*;

public final class MainActivity extends Activity {
    private LinearLayout panel;
    private TextView clock, status, battery, guidance;
    private Button mode;
    private Store store;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private List<Location> reverse;
    private int waypoint=-1;
    private long lastNavigationFix=-1;
    private String pendingAction;
    private boolean active=false;
    private final Runnable refresh=new Runnable(){public void run(){render();if(active)handler.postDelayed(this,5000);}};
    private android.content.SharedPreferences prefs(){return TrackService.prefs(this);}
    public void onCreate(Bundle state){
        super.onCreate(state);store=new Store(this);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(Color.BLACK);
        panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);panel.setGravity(Gravity.CENTER_HORIZONTAL);panel.setPadding(dp(28),dp(28),dp(28),dp(36));scroll.addView(panel);setContentView(scroll);
        label("WATCH LITE",13,0xff86e6c5);clock=label("",34,Color.WHITE);clock.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        battery=label("",13,0xffb7c6bf);mode=button("",this::toggleMode);
        status=label("",13,0xffb7c6bf);
        button("📍 여기 기억해",()->start("MARK"));
        button("경로 기록 시작",()->{
            if(TrackService.alive){toast("먼저 현재 GPS 작업을 중지하세요");return;}
            new AlertDialog.Builder(this).setMessage("이전 경로를 지우고 새 경로를 기록할까요? 저장 장소는 유지됩니다.").setPositiveButton("새 기록",(d,w)->{store.clear("route");reverse=null;waypoint=-1;guidance.setText("");start("RECORD");}).setNegativeButton("취소",null).show();
        });
        button("BackTrack · 되돌아가기",()->{
            if(prefs().getBoolean("recording",false)&&TrackService.alive){toast("기록 중지를 먼저 누르세요");return;}
            reverse=store.all("route");waypoint=reverse.size()-1;lastNavigationFix=-1;
            if(waypoint<0){toast("저장된 경로가 없습니다");return;}
            start("NAVIGATE");
        });
        guidance=label("",18,0xff86e6c5);
        button("GPS / 기록 중지",()->{stopService(new Intent(this,TrackService.class));reverse=null;waypoint=-1;guidance.setText("");TrackService.status(this,"GPS 작업을 중지했습니다");render();});
        button("저장한 장소",this::showMarks);
        button("배터리 측정 초기화",()->{prefs().edit().remove("baseTime").apply();render();toast("현재 모드 측정을 새로 시작합니다");});
        button("목표 시간 선택",()->new AlertDialog.Builder(this).setTitle("목표 사용시간 · 추정용").setItems(new String[]{"24시간","48시간","72시간"},(d,w)->{prefs().edit().putInt("target",new int[]{24,48,72}[w]).apply();render();}).show());
        button("시스템 절전 설정",()->openSettings(Settings.ACTION_BATTERY_SAVER_SETTINGS));
        button("위치 설정",()->openSettings(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
        button("타이머 열기",()->{
            Intent i=new Intent(android.provider.AlarmClock.ACTION_SHOW_TIMERS);
            try{startActivity(i);}catch(ActivityNotFoundException e){toast("워치 기본 타이머 앱을 열어주세요");}
        });
        button("기록 모두 삭제",()->new AlertDialog.Builder(this).setMessage("저장 장소와 경로를 모두 삭제할까요?").setPositiveButton("삭제",(d,w)->{stopService(new Intent(this,TrackService.class));store.clear("route");store.clear("mark");reverse=null;waypoint=-1;guidance.setText("");prefs().edit().remove("lat").remove("lon").remove("fixElapsed").remove("fixWall").apply();TrackService.status(this,"위치 기록 삭제 완료");render();}).setNegativeButton("취소",null).show());
        label("Lite는 이 앱의 화면 밝기와 GPS 요청 간격만 조절합니다.\n전화·알림·건강 센서는 시스템 설정을 따릅니다.\n방향은 북쪽 기준입니다. 실제 길과 장애물을 판단하지 않습니다.",11,0xff96a49f);
        if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},8);
    }
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private TextView label(String t,int size,int color){TextView v=new TextView(this);v.setText(t);v.setTextSize(size);v.setTextColor(color);v.setGravity(Gravity.CENTER);v.setPadding(0,dp(5),0,dp(5));panel.addView(v,new LinearLayout.LayoutParams(-1,-2));return v;}
    private Button button(String t,Runnable click){Button b=new Button(this);b.setText(t);b.setTextSize(13);b.setAllCaps(false);b.setTextColor(Color.WHITE);b.setMinHeight(dp(48));GradientDrawable shape=new GradientDrawable();shape.setColor(0xff172924);shape.setCornerRadius(dp(24));b.setBackground(shape);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,dp(4),0,dp(4));panel.addView(b,p);b.setOnClickListener(v->click.run());return b;}
    private void toggleMode(){boolean lite=!prefs().getBoolean("lite",false);prefs().edit().putBoolean("lite",lite).remove("baseTime").apply();if(TrackService.alive)start("MODE");render();}
    private void start(String action){
        if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){pendingAction=action;requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},9);return;}
        try{startForegroundService(new Intent(this,TrackService.class).setAction(action));}catch(RuntimeException e){toast("GPS 시작 실패 · 위치 권한과 설정을 확인하세요");}
    }
    public void onRequestPermissionsResult(int r,String[] p,int[] g){super.onRequestPermissionsResult(r,p,g);if(r==9){if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED && pendingAction!=null){String a=pendingAction;pendingAction=null;start(a);}else{pendingAction=null;toast("BackTrack에는 정확한 위치 허용이 필요합니다");}}}
    private void render(){
        if(clock==null)return;
        clock.setText(new java.text.SimpleDateFormat("HH:mm",Locale.KOREA).format(new Date()));
        boolean lite=prefs().getBoolean("lite",false);mode.setText(lite?"LITE  ·  터치하여 Normal":"NORMAL  ·  터치하여 Lite");
        WindowManager.LayoutParams attrs=getWindow().getAttributes();attrs.screenBrightness=lite?0.15f:-1f;getWindow().setAttributes(attrs);
        status.setText(prefs().getString("status","필요할 때만 GPS를 켭니다")+"\n"+(TrackService.alive?"GPS 작업 진행 중":"GPS 작업 없음"));
        Intent b=registerReceiver(null,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if(b!=null){
            int scale=b.getIntExtra(BatteryManager.EXTRA_SCALE,100),level=b.getIntExtra(BatteryManager.EXTRA_LEVEL,-1);
            int value=level<0?-1:Math.round(level*100f/Math.max(1,scale));
            boolean charging=b.getIntExtra(BatteryManager.EXTRA_PLUGGED,0)!=0;
            long now=SystemClock.elapsedRealtime(),base=prefs().getLong("baseTime",-1),wall=System.currentTimeMillis();int initial=prefs().getInt("baseBattery",value);
            boolean reboot=Math.abs((wall-now)-prefs().getLong("bootAnchor",wall-now))>60000;
            if(charging||base<0||now<base||reboot||value>initial){prefs().edit().putLong("baseTime",now).putInt("baseBattery",value).putLong("bootAnchor",wall-now).apply();base=now;initial=value;}
            double rate=Core.rate(initial,value,now-base),remaining=Core.remaining(value,rate);int target=prefs().getInt("target",48);
            String estimate=Double.isNaN(rate)?"측정 중 · 최소 1시간 / 2% 소모 필요":String.format(Locale.KOREA,"%.1f%%/h · 약 %.0f시간 남음\n목표 %dh · %s",rate,remaining,target,remaining>=target?"현재 추세 충족":"절전 설정 권장");
            battery.setText((value<0?"배터리 정보 없음":value+"%")+"\n"+(charging?"충전 중 · 측정 중단":estimate));
        }
        navigate();
    }
    private void navigate(){
        if(reverse==null||waypoint<0)return;
        long fix=prefs().getLong("fixElapsed",0),age=SystemClock.elapsedRealtime()-fix;
        float accuracy=prefs().getFloat("accuracy",999);
        if(!TrackService.alive||!Core.usable(accuracy,age)||System.currentTimeMillis()-prefs().getLong("fixWall",0)>30000){guidance.setText("새 GPS 위치 대기 중\n도착 판정 보류");return;}
        double lat=Double.parseDouble(prefs().getString("lat","0")),lon=Double.parseDouble(prefs().getString("lon","0"));
        Location p=reverse.get(waypoint);double distance=Core.distance(lat,lon,p.getLatitude(),p.getLongitude());
        // A single fix may advance at most one breadcrumb, avoiding loops/crossing skips.
        if(fix!=lastNavigationFix && Core.reached(distance,accuracy)){
            lastNavigationFix=fix;
            if(waypoint==0){guidance.setText("출발 지점 부근 도착\n주변을 직접 확인하세요");reverse=null;stopService(new Intent(this,TrackService.class));return;}
            waypoint--;p=reverse.get(waypoint);distance=Core.distance(lat,lon,p.getLatitude(),p.getLongitude());
        }
        double bearing=Core.bearing(lat,lon,p.getLatitude(),p.getLongitude());
        guidance.setText(String.format(Locale.KOREA,"%s %.0f° · %.0fm\n지점 %d / %d · ±%.0fm\n북쪽 기준 / 직선 거리",Core.cardinal(bearing),bearing,distance,waypoint+1,reverse.size(),accuracy));
    }
    private void showMarks(){List<Location> marks=store.all("mark");if(marks.isEmpty()){toast("저장한 장소가 없습니다");return;}String[] items=new String[marks.size()];for(int i=0;i<items.length;i++){Location l=marks.get(i);items[i]=new java.text.SimpleDateFormat("MM/dd HH:mm",Locale.KOREA).format(new Date(l.getTime()))+String.format(Locale.US,"\n%.5f, %.5f",l.getLatitude(),l.getLongitude());}new AlertDialog.Builder(this).setTitle("저장 장소 → 직선 안내").setItems(items,(d,w)->{if(TrackService.alive){toast("먼저 GPS 작업을 중지하세요");return;}reverse=new ArrayList<>();reverse.add(marks.get(w));waypoint=0;lastNavigationFix=-1;start("NAVIGATE");}).setNegativeButton("닫기",null).show();}
    private void openSettings(String action){try{startActivity(new Intent(action));}catch(ActivityNotFoundException e){try{startActivity(new Intent(Settings.ACTION_SETTINGS));}catch(ActivityNotFoundException ignored){toast("워치 설정에서 직접 변경하세요");}}}
    private void toast(String t){Toast.makeText(this,t,Toast.LENGTH_LONG).show();}
    protected void onResume(){super.onResume();active=true;handler.removeCallbacks(refresh);handler.post(refresh);}
    protected void onPause(){active=false;handler.removeCallbacks(refresh);super.onPause();}
    protected void onDestroy(){handler.removeCallbacksAndMessages(null);store.close();super.onDestroy();}
}
