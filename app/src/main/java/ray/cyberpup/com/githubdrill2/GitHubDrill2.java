package ray.cyberpup.com.githubdrill2;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created on 6/17/15
 *
 * @author Raymond Tong
 */
public class GitHubDrill2 extends AppCompatActivity implements DownloadTask.TaskListener {

    private ListView mListView;
    private SearchView mSearch;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        mListView = (ListView) findViewById(R.id.userlistview);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                if (isNetworkConnected()) {
                    HashMap<String, Object> hm = (HashMap<String, Object>) parent.getItemAtPosition(position);
                    String repoUrl = (String) hm.get(Constants.GIT_REPOS);
                    String userName = (String) hm.get(Constants.GIT_USERID);

                    // Pass repository url to RepoView activity
                    Intent repoViewIntent = new Intent(GitHubDrill2.this, RepoView.class);
                    repoViewIntent.putExtra(Constants.GIT_REPOS, repoUrl);
                    repoViewIntent.putExtra(Constants.GIT_USERID, userName);
                    startActivity(repoViewIntent);
                }else {
                    Toast.makeText(GitHubDrill2.this,
                            "No Network Connection", Toast.LENGTH_SHORT).show();
                }


            }
        });
    }


    @Override
    public void onPreExecute() {
        //Do Nothing
    }

    @Override
    public void onProgressUpdate(Integer... progress) {
        //Do Nothing
    }

    /**
     * resultCodes:
     * 1 json string from user query
     * 2 json string from repo query
     */
    @Override
    public void onPostExecute(String results) {

        ListViewLoaderTask task = new ListViewLoaderTask();
        task.execute(results);

        cleanUp();

    }

    private void cleanUp() {

        final FragmentManager man = getSupportFragmentManager();
        final Fragment fragment = man.findFragmentByTag(Constants.DOWNLOAD_USERS);

        if (fragment != null)
            man.beginTransaction().remove(fragment).commit();
    }

    @Override
    public void onCancelled() {
        cleanUp();
    }

    public boolean isNetworkConnected() {
        final ConnectivityManager conMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo activeNetwork = conMgr.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.getState() == NetworkInfo.State.CONNECTED;
    }

    private void performSearch(final String query) {

        DownloadTask taskFrag = (DownloadTask) getSupportFragmentManager().
                findFragmentByTag(Constants.DOWNLOAD_USERS);

        if (taskFrag == null) {

            FragmentTransaction tran = getSupportFragmentManager().beginTransaction();
            DownloadTask frag = DownloadTask.getInstance(query);
            tran.add(frag, Constants.DOWNLOAD_USERS).commit();
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
            public boolean onQueryTextSubmit(final String query) {

                if (isNetworkConnected()) {

                    performSearch(query);

                } else
                    Toast.makeText(GitHubDrill2.this, "No Network Connection", Toast.LENGTH_SHORT).show();
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
        return true;
    }

    class ViewHolderItem {

        ImageView avatar;
        TextView name;

        ViewHolderItem(View v){

            name = (TextView) v.findViewById(R.id.userTextView);
            avatar = (ImageView) v.findViewById(R.id.userImageView);
        }
    }

    public class GitHubViewerAdapter extends BaseAdapter {

        Context context;
        List<HashMap<String, Object>> dataList;

        int singleRowId;

        public GitHubViewerAdapter(Context context, List<HashMap<String, Object>> dataList, int singleRowId) {

            this.context = context;
            this.singleRowId = singleRowId;
            this.dataList = dataList;

        }

        @Override
        public int getCount() {
            return dataList.size();
        }

        @Override
        public Object getItem(int position) {
            return dataList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            View row = convertView;
            ViewHolderItem holder = null;

            if (row == null) {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                row = inflater.inflate(singleRowId, parent, false);
                holder = new ViewHolderItem(row);
                row.setTag(holder); // recycle findViewById operation
            }else {
                holder = (ViewHolderItem) row.getTag();
            }

            holder.name.setText((String) dataList.get(position).get(Constants.GIT_USERID));
            File imgFile = new File((String) dataList.get(position).get(Constants.GIT_AVATAR));

            if (imgFile.exists()) {
                Bitmap avatar = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                holder.avatar.setImageBitmap(avatar);
            }

            return row;
        }
    }

    private class ListViewLoaderTask extends AsyncTask<String, Integer, GitHubViewerAdapter> {

        @Override
        protected GitHubViewerAdapter doInBackground(String... jsonString) {

            List<HashMap<String, Object>> users = null;


            // Return a SimpleAdapter
            try {

                users = getListOfUsersFromJson(jsonString[0]);


            } catch (JSONException e) {
                e.printStackTrace();
            }

            //Keys
            String[] keys = {Constants.GIT_USERID, Constants.GIT_AVATAR};


            GitHubViewerAdapter adapter = new GitHubViewerAdapter(GitHubDrill2.this, users,
                    R.layout.single_row);

            return adapter;
        }

        private List<HashMap<String, Object>> getListOfUsersFromJson(final String json) throws JSONException {


            List<HashMap<String, Object>> listOfUsers = new ArrayList<HashMap<String, Object>>();

            JSONObject queryResults = new JSONObject(json);
            JSONArray usersArray = queryResults.getJSONArray(Constants.GIT_ITEMS);

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
                userData.put(Constants.GIT_ID, user.getInt(Constants.GIT_ID));
                userData.put(Constants.GIT_USERID, user.getString(Constants.GIT_USERID));
                userData.put(Constants.GIT_AVATAR, user.getString(Constants.GIT_AVATAR));
                userData.put(Constants.GIT_REPOS, user.getString(Constants.GIT_REPOS));
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
        protected void onPostExecute(GitHubViewerAdapter adapter) {

            mListView.setAdapter(adapter);

            String imgUrl = null;

            for (int i = 0; i < adapter.getCount(); i++) {

                HashMap<String, Object> hm = (HashMap<String, Object>) adapter.getItem(i);
                if (hm != null) {
                    imgUrl = (String) hm.get(Constants.GIT_AVATAR);
                }

                ImageLoaderTask imageLoaderTask = new ImageLoaderTask();

                HashMap<String, Object> avatarDownload = new HashMap<String, Object>();
                avatarDownload.put(Constants.GIT_AVATAR, imgUrl);
                avatarDownload.put(Constants.USER_POSITION, i);

                if (isNetworkConnected()) {
                    imageLoaderTask.execute(avatarDownload);
                }else
                    Toast.makeText(GitHubDrill2.this,
                            "No Network Connection", Toast.LENGTH_SHORT).show();

            }
     /*       */

        }

        @Override
        protected void onCancelled() {
            System.out.println("Cancelled!");
            cleanUp();
        }
    }

    private class ImageLoaderTask extends AsyncTask<HashMap<String, Object>, Void,
            HashMap<String, Object>> {


        final String LOG_TAG = ImageLoaderTask.class.getSimpleName();

        @Override
        protected HashMap<String, Object> doInBackground(HashMap<String, Object>... userAvatar) {

            HttpURLConnection urlCon = null;

            String imgUrl = (String) userAvatar[0].get(Constants.GIT_AVATAR);
            int position = (Integer) userAvatar[0].get(Constants.USER_POSITION);


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

                if (urlCon.getResponseCode() == 200) {

                    // Connection successful
                    InputStream inputStream = urlCon.getInputStream();

                    // Get Caching Directory
                    File cacheDirectory = getBaseContext().getCacheDir();

                    // Temporary file to store downloaded image
                    File tempFile = new File(cacheDirectory.getPath() + "/gitavatar_" + position + ".png");

                    //DEBUG
                    //System.out.println(tempFile.getName());
                    //System.out.println(tempFile.getAbsolutePath());

                    // Create FileOutputStream to temporary file
                    FileOutputStream foutstream = new FileOutputStream(tempFile);

                    // Create Bitmap from download inputstream
                    Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

                    // Write bitmap to temporary file as png file
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, foutstream);

                    // Flush FileOutputStream
                    foutstream.flush();
                    // Close FileOutputStream
                    foutstream.close();

                    // Create a hashmap object to store image path and position in listview
                    HashMap<String, Object> hashMBitmap = new HashMap<String, Object>();

                    // Store path to temporary image of avatar
                    hashMBitmap.put(Constants.GIT_AVATAR, tempFile.getPath());

                    // Store position of image in relation to its user name in the listview
                    hashMBitmap.put(Constants.USER_POSITION, position);

                    return hashMBitmap;

                }

            } catch (IOException e) {
                // If the code didn't successfully get data
                Log.e(LOG_TAG, "Error ", e);

            } finally {
                if (urlCon != null) {
                    urlCon.disconnect();
                }
            }
            return null;
        }


        @Override
        protected void onPostExecute(HashMap<String, Object> singleUserImage) {


                // Get path to image
                String pathToImage = (String) singleUserImage.get(Constants.GIT_AVATAR);

                // Get position of image in listview
                int position = (int) singleUserImage.get(Constants.USER_POSITION);

                // Get adapter of listview
                GitHubViewerAdapter adapter = (GitHubViewerAdapter) mListView.getAdapter();

                // Get HashMap object at specified position in listview
                HashMap<String, Object> hashMap = (HashMap<String, Object>) adapter.getItem(position);

                // Overwrite avatar url with avatar path to local file
                hashMap.put(Constants.GIT_AVATAR, pathToImage);

                // Notify listview about dataset change;
                adapter.notifyDataSetChanged();




        }
    }

}
