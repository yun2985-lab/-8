package kr.watchlite;
import android.content.*;
import android.database.Cursor;
import android.database.sqlite.*;
import android.location.Location;
import java.util.*;

final class Store extends SQLiteOpenHelper {
    Store(Context c){super(c,"watchlite.db",null,1);}
    public void onCreate(SQLiteDatabase d){
        d.execSQL("CREATE TABLE points(id INTEGER PRIMARY KEY AUTOINCREMENT,kind TEXT,lat REAL,lon REAL,accuracy REAL,stamp INTEGER)");
    }
    public void onUpgrade(SQLiteDatabase d,int a,int b){}
    void add(String kind,Location l){
        ContentValues v=new ContentValues(); v.put("kind",kind);v.put("lat",l.getLatitude());v.put("lon",l.getLongitude());v.put("accuracy",l.getAccuracy());v.put("stamp",l.getTime());
        getWritableDatabase().insertOrThrow("points",null,v);
    }
    List<Location> all(String kind){
        List<Location> out=new ArrayList<>();
        try(Cursor c=getReadableDatabase().query("points",null,"kind=?",new String[]{kind},null,null,"id ASC")){
            while(c.moveToNext()){Location l=new Location("stored");l.setLatitude(c.getDouble(c.getColumnIndexOrThrow("lat")));l.setLongitude(c.getDouble(c.getColumnIndexOrThrow("lon")));l.setAccuracy(c.getFloat(c.getColumnIndexOrThrow("accuracy")));l.setTime(c.getLong(c.getColumnIndexOrThrow("stamp")));out.add(l);}
        } return out;
    }
    int count(String kind){try(Cursor c=getReadableDatabase().rawQuery("SELECT COUNT(*) FROM points WHERE kind=?",new String[]{kind})){c.moveToFirst();return c.getInt(0);}}
    void clear(String kind){getWritableDatabase().delete("points","kind=?",new String[]{kind});}
}
