package nathanielwendt.mpc.ut.edu.paco.utils;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.io.File;

import static nathanielwendt.mpc.ut.edu.paco.utils.DBConstants.DATABASE_NAME;
import static nathanielwendt.mpc.ut.edu.paco.utils.DBConstants.DATABASE_VERSION;

/**
 * This class only serves to use Androids native database creation since the custom sqlite compile
 * Does not work unless database is created natively.
 */
public class DBInit extends SQLiteOpenHelper {

    Context myContext;

    public DBInit(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        myContext = context;
    }

    public void init(){
        SQLiteDatabase db = this.getReadableDatabase();
    }

    public boolean dbExists(){
        boolean checkdb = false;
        try{
            String myPath = myContext.getApplicationInfo().dataDir + "/databases/";
            Log.d("LST", myPath);
            File dbfile = new File(myPath);
            checkdb = dbfile.exists();
        }
        catch(SQLiteException e){
            System.out.println("Database doesn't exist");
        }

        return checkdb;
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        //do nothing since this is just a shallow wrapper.  Just need to create db
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        //do nothing since upgrade will be called on other Helper classes.
    }
}
