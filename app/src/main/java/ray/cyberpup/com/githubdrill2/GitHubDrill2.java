package ray.cyberpup.com.githubdrill2;

import android.app.SearchManager;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.inputmethod.InputMethodManager;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.SimpleAdapter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * Created on 6/17/15
 *
 * @author Raymond Tong
 */
public class GitHubDrill2 extends AppCompatActivity implements DownloadTask.TaskListener {

    private static final String DOWNLOAD = "download";
    private ListView mListView;
    private SearchView mSearch;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        mListView = (ListView) findViewById(R.id.userlistview);
    }


    @Override
    public void onPreExecute() {
        //Do Nothing
    }

    @Override
    public void onProgressUpdate(Integer... progress) {
        //Do Nothing
    }

    @Override
    public void onPostExecute(String results) {


        ListViewLoaderTask task = new ListViewLoaderTask();
        task.execute(results);

        cleanUp();
    }

    private void cleanUp() {

        FragmentManager man = getSupportFragmentManager();
        Fragment fragment = man.findFragmentByTag(DOWNLOAD);
        if (fragment != null)
            man.beginTransaction().remove(fragment).commit();
    }

    @Override
    public void onCancelled() {
        // do nothing
    }

    private class JSONParser {

    }

    private void performSearch(String query) {
        DownloadTask taskFrag = (DownloadTask) getSupportFragmentManager().
                findFragmentByTag(DOWNLOAD);


        if (taskFrag == null) {

            FragmentTransaction tran = getSupportFragmentManager().beginTransaction();
            DownloadTask frag = DownloadTask.getInstance(query);
            tran.add(frag, DOWNLOAD).commit();
            frag.beginTask();
        } else {

            taskFrag.beginTask();
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.options_menu, menu);


        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        mSearch = (SearchView) menu.findItem(R.id.git_search).getActionView();
        mSearch.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        mSearch.setIconifiedByDefault(true);

        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(mSearch, InputMethodManager.SHOW_IMPLICIT);

        mSearch.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {

                System.out.println("onQueryTextSubmitted");

                performSearch(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {

                return false;
            }
        });


        return true;
    }

    private class ListViewLoaderTask extends AsyncTask<String, Integer, SimpleAdapter> {


        final String GIT_ITEMS = "items";
        final String GIT_ID = "id";
        final String GIT_USERID = "login";
        final String GIT_AVATAR = "avatar_url";
        final String GIT_REPOS = "repos_url";

        @Override
        protected SimpleAdapter doInBackground(String... jsonString) {

            List<HashMap<String, Object>> users = null;


            // Return a SimpleAdapter
            try {

                users = getListOfUsersFromJson(jsonString[0]);


            } catch (JSONException e) {
                e.printStackTrace();
            }

            //Keys
            String[] keys = {GIT_USERID};
            //ID of Views
            int[] ids = {R.id.userTextView};

            SimpleAdapter adapter = new SimpleAdapter(GitHubDrill2.this, users,
                    R.layout.single_row,
                    keys, ids);

            return adapter;
        }

        private List<HashMap<String, Object>> getListOfUsersFromJson(final String json) throws JSONException {


            List<HashMap<String, Object>> listOfUsers = new ArrayList<HashMap<String, Object>>();

            JSONObject queryResults = new JSONObject(json);
            JSONArray usersArray = queryResults.getJSONArray(GIT_ITEMS);

            for (int i = 0; i < usersArray.length(); i++) {

                // Get JSON object representing the user
                JSONObject user = usersArray.getJSONObject(i);
                listOfUsers.add(getSingleUserData(user));

            }
            return listOfUsers;
        }

        private HashMap<String, Object> getSingleUserData(final JSONObject user) {

            HashMap<String, Object> userData = new HashMap<String, Object>();
            try {
                userData.put(GIT_ID, user.getInt(GIT_ID));
                userData.put(GIT_USERID, user.getString(GIT_USERID));
                userData.put(GIT_AVATAR, user.getString(GIT_AVATAR));
                userData.put(GIT_REPOS, user.getString(GIT_REPOS));
                return userData;

            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPreExecute() {

        }


        @Override
        protected void onPostExecute(SimpleAdapter adapter) {

            mListView.setAdapter(adapter);

            /*
            ImageLoaderTask task = new ImageLoaderTask();

            for (HashMap<String, Object> user : users) {

                for (String key : user.keySet()) {

                    if (key == GIT_USERID)
                        System.out.println("user:" + user.get(GIT_USERID));
                }

            }
            task.execute()
            */

        }


    }

    private class ImageLoaderTask extends AsyncTask<HashMap<String, Object>, Void,
            HashMap<String, Object>> {


        final String LOG_TAG = ImageLoaderTask.class.getSimpleName();

        @Override
        protected HashMap<String, Object> doInBackground(HashMap<String, Object>... usersData) {

            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpURLConnection urlCon = null;
            BufferedReader reader = null;

            Set<String> keys = usersData[0].keySet();
            for (String key : keys)
                System.out.println("key:" + key);
/*
            try {
                // Construct URL
                URL url = new URL(imgUrl);

                // Create the request to OpenWeatherMap, and open the connection
                urlCon = (HttpURLConnection) url.openConnection();
                urlCon.setRequestMethod("GET");
                urlCon.setConnectTimeout(5000);
                urlCon.setRequestProperty("Accept", "application/json");
                //urlCon.setRequestProperty("Content-type", "application/json");
                //urlCon.setRequestProperty("X-CZ-Authorization", AUTH_TOKEN);
                urlCon.connect();

                if(urlCon.getResponseCode() == 200) {

                    // Connection successful
                    InputStream inputStream = urlCon.getInputStream();

                    if (inputStream == null) {
                        return null;
                    }

                    // Synchronized mutable sequence of characters
                    StringBuffer stringBuffer = new StringBuffer();

                    // byte to character bridge
                    // read from resulting character-input stream
                    // using 8192 characters buffer size
                    reader = new BufferedReader(new InputStreamReader(inputStream));

                    // Download Data
                    // Read JSON into a single string
                    String line;
                    while ((line = reader.readLine()) != null) {
                        stringBuffer.append(line + "\n"); // newline added for debugging only
                    }

                    if (stringBuffer.length() == 0) {
                        return null;
                    }

                }

            } catch (IOException e) {
                // If the code didn't successfully get data
                Log.e(LOG_TAG, "Error ", e);

            } finally {
                if (urlCon != null) {
                    urlCon.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }*/
            return null;
        }
    }


}
