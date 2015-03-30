package com.spatialdev.downloadmanagertest;

import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;


/**
 * DERIVED FROM:    
 *   http://blog.vogella.com/2011/06/14/android-downloadmanager-example/
 *   http://stackoverflow.com/questions/15795872/show-download-progress-inside-activity-using-downloadmanager
 *   
 *   Note: I have two examples, one where we download QGIS, and the other were we actually get a query from
 *   Overpass. The progress bar will not work when hitting up Overpass, because Overpass does not let us
 *   know how big the file is we are downloading.
 *   *   *   *   *   *
 *
 */
public class MainActivity extends ActionBarActivity {

    private static final String URL = "http://overpass-api.de/api/interpreter?data=(way[building](47.65145486180013,-122.42340087890624,47.681743174340355,-122.32246398925781);way[highway](47.65145486180013,-122.42340087890624,47.681743174340355,-122.32246398925781););out%20meta;%3E;out%20meta%20qt;";
//    private static final String URL = "http://www.kyngchaos.com/files/software/qgis/QGIS-2.8.1-1.dmg";
    private long enqueue;
    private DownloadManager dm;
    private ProgressDialog progressDialog;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

                if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
                    long downloadId = intent.getLongExtra(
                            DownloadManager.EXTRA_DOWNLOAD_ID, 0);
                    DownloadManager.Query query = new DownloadManager.Query();
                    query.setFilterById(enqueue);
                    Cursor c = dm.query(query);
                    if (c.moveToFirst()) {
                        int columnIndex = c.getColumnIndex(DownloadManager.COLUMN_STATUS);
                        if (DownloadManager.STATUS_SUCCESSFUL == c.getInt(columnIndex)) {

                            ImageView view = (ImageView) findViewById(R.id.imageView1);
                            String uriString = c.getString(c.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
//                            view.setImageURI(Uri.parse(uriString));
                        }
                    }
                } 
                
                else if (DownloadManager.ACTION_NOTIFICATION_CLICKED.equals(action)) {
                    
                } 
                
                else if (DownloadManager.ACTION_VIEW_DOWNLOADS.equals(action)) {
                    
                }
            }
        };

        registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    public void onClick(View view) {
        dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(URL));
          // short
//        Uri.parse("http://overpass-api.de/api/interpreter?data=(way[building](47.66924559516471,-122.38838195800781,47.671730901355055,-122.3812687397003);way[highway](47.66924559516471,-122.38838195800781,47.671730901355055,-122.3812687397003););out%20meta;%3E;out%20meta%20qt;"));
        
//        request.setTitle("OSM XML");
//        request.setDescription("An example of a GET from Overpass API.");
        request.setDestinationInExternalPublicDir("/", "overpass-test.osm");
        
        enqueue = dm.enqueue(request);

        setupProgress();
    }
    

    public void showDownload(View view) {
        Intent i = new Intent();
        i.setAction(DownloadManager.ACTION_VIEW_DOWNLOADS);
        startActivity(i);
    }
    
    
    public void setupProgress() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Downloading OSM Data");
        progressDialog.setMessage("Hey Kid, I'm a computer! Stop all the downloadin'!!!");
        progressDialog.setIndeterminate(true);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setCancelable(true);
        progressDialog.show();       
        
        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean downloading = true;
                while(downloading) {
                    DownloadManager.Query q = new DownloadManager.Query();
                    q.setFilterById(enqueue);
                    Cursor cursor = dm.query(q);
                    cursor.moveToFirst();
                    final int bytesDownloaded = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
//                    int bytesTotal = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                    if (cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)) == DownloadManager.STATUS_SUCCESSFUL) {
                        downloading = false;
                    }
//                    final double dlProgress = (int) ((bytesDownloaded * 100l) / bytesTotal);
                    final String msg = "Downloading OSM XML from Overpass API:\n\n" + ((double)bytesDownloaded) / 1000000.0 + " MB";
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressDialog.setMessage(msg);
                        }
                    });
                    Log.d("YO!", statusMessage(cursor, bytesDownloaded, 0));
                    cursor.close();

                }
            }
        }).start();
    }

    private String statusMessage(Cursor c, int bytesDownloaded, int bytesTotal) {
        String msg = "???";

        switch (c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS))) {
            case DownloadManager.STATUS_FAILED:
                msg = "Download failed!";
                break;

            case DownloadManager.STATUS_PAUSED:
                msg = "Download paused!";
                break;

            case DownloadManager.STATUS_PENDING:
                msg = "Download pending!";
                break;

            case DownloadManager.STATUS_RUNNING:
                msg = "Download in progress! " + bytesDownloaded + " " + bytesTotal;
                break;

            case DownloadManager.STATUS_SUCCESSFUL:
                msg = "Download complete! " + bytesDownloaded + " " + bytesTotal;
                break;

            default:
                msg = "Download is nowhere in sight";
                break;
        }

        return (msg);
    }
}
