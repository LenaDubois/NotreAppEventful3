package biln.notreappeventful3;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/**
 *
 */
public class MainActivity extends ActionBarActivity implements View.OnClickListener, AdapterView.OnItemClickListener {

    ListView listv;
    SQLiteDatabase db;
    DBHelper dbh;
    MyAdapter adapter;
    Button btn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btn = (Button)findViewById(R.id.button);
        listv = (ListView)findViewById(R.id.activity_list);
        dbh = new DBHelper(this);
        db = dbh.getWritableDatabase();
        Cursor c = DBHelper.listEvents(db);
        adapter = new MyAdapter(this, c);
        listv = (ListView)findViewById(R.id.activity_list);
        listv.setAdapter(adapter);
        listv.setOnItemClickListener(this);
        btn.setOnClickListener(this);
    }

    protected void onResume(){
        super.onResume();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        boolean previouslyStarted = prefs.getBoolean(getString(R.string.pref_previously_started), false);
        SharedPreferences.Editor edit = prefs.edit();
        if(!previouslyStarted){
            Toast.makeText(this, "Premiere fois", Toast.LENGTH_LONG).show();
            edit.putBoolean(getString(R.string.pref_previously_started), Boolean.TRUE);
            edit.commit();
            Intent i = new Intent(this, WelcomingActivity.class);
            startActivity(i);

        }

        /*
        Les deux lignes suivantes s'occupent de vider les préférences. En vidant les préférances,
        on aura l'écran de première connexion à chaque exécution de l'app.
         */
        //TODO: À enlever quand on sera content de l'écran de première connexion
        //edit.clear() ;
        //edit.commit();
    }




    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View viewClicked, int position, long viewID) {

        Log.d("db","Clic sur item en position= "+position+" et avec viewID = "+ viewID);

        DBHelper.setEventAsFavorite(db, (int)viewID, 1); // TODO !
        Cursor c = DBHelper.listEvents(db);
        adapter.changeCursor(c);

    }

    public void onClick(View v) {
        new SearchEventfulAndPopulate().execute();
    }


    private class SearchEventfulAndPopulate extends AsyncTask<String, String, EventfulAPI> {

        //TODO Implanter une barre de progrès ou une horloge qui tourne au lieu d'un toast
        protected void onPreExecute() {
            Toast.makeText(MainActivity.this, "Chargement", Toast.LENGTH_SHORT).show();
        }

        protected EventfulAPI doInBackground(String... params) {
            EventfulAPI web = new EventfulAPI();
            web.getNextEvents("Montreal");
            ContentValues val = new ContentValues();
            for(int i=0;i<web.eventsFound.size();i++){
                // ON NE SPÉCIFIE PAS EPLICITEMENT LA VALEUR DU ID. ON LE LAISSE AUTOINCRÉMENTER
                val.put(DBHelper.C_ID_FROM_EVENTFUL, web.eventsFound.get(i).idFromEventful);
                val.put(DBHelper.C_TITLE, web.eventsFound.get(i).title);
                val.put(DBHelper.C_DATE_START, web.eventsFound.get(i).date_start);
                val.put(DBHelper.C_DATE_STOP, web.eventsFound.get(i).date_stop);
                val.put(DBHelper.C_LOCATION, web.eventsFound.get(i).location);
                val.put(DBHelper.C_FAVORITE, 0);
                db.insertOrThrow(DBHelper.TABLE_EVENTS, null, val);
            }
            Log.d("DB", "nouveux titres ajoutes: ");
            return null;
        }

        protected void onProgressUpdate(String... s) {
        }

        protected void onPostExecute(EventfulAPI web) {
            // rafraichir...
            Cursor c = DBHelper.listEvents(db);
            adapter.changeCursor(c);
        }

    }

    public class MyAdapter extends CursorAdapter {

        LayoutInflater inflater;

        public MyAdapter(Context context, Cursor c) {
            super(context, c, true);
            inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;

            if(v==null){
                v = inflater.inflate(android.R.layout.simple_list_item_2, parent, false);
            }

            TextView titleV = (TextView)v.findViewById(android.R.id.text1);
            TextView dateV = (TextView)v.findViewById(android.R.id.text2);

            Cursor c=getCursor();
            c.moveToPosition(position);
            String id= c.getString(c.getColumnIndex(DBHelper.C_ID));
            dateV.setText(c.getString(c.getColumnIndex(DBHelper.C_DATE_START)));
            titleV.setText(c.getString(c.getColumnIndex(DBHelper.C_TITLE))+ " id supposé : "+id);

            return v;
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return null;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {

        }

    }


}


