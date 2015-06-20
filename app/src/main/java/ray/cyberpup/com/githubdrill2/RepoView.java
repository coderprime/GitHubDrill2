package ray.cyberpup.com.githubdrill2;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created on 6/18/15
 *
 * @author Raymond Tong
 */
public class RepoView extends AppCompatActivity implements DownloadReposTask.TaskListener {

    private String mRepoUrl = null;
    private TableLayout mTableLayout = null;
    private String userName = null;
    private TextView tvUserName = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_table_repo_v2);

        mTableLayout = (TableLayout) findViewById(R.id.tablelayout_listOfRepos);

        Intent dataFromIntent = getIntent();

        mRepoUrl = dataFromIntent.getStringExtra(Constants.GIT_REPOS);

        tvUserName = (TextView) findViewById(R.id.tv_username);

        tvUserName.setText(dataFromIntent.getStringExtra(Constants.GIT_USERID));



    }

    private void downloadRepos(){
        DownloadReposTask taskFrag = (DownloadReposTask) getSupportFragmentManager().
                findFragmentByTag(Constants.DOWNLOAD_REPOS);

        if (taskFrag == null) {

            FragmentTransaction tran = getSupportFragmentManager().beginTransaction();
            DownloadReposTask frag = DownloadReposTask.getInstance(mRepoUrl);
            tran.add(frag, Constants.DOWNLOAD_REPOS).commit();
            frag.beginTask();

        } else {

            taskFrag.beginTask();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        downloadRepos();
    }

    @Override
    public void onPreExecute() {

    }

    @Override
    public void onProgressUpdate(Integer... progress) {

    }

    @Override
    public void onPostExecute(String results) {

        // Parse json repositories string & returns a custom adapter called GitRepoAdapter
        RepoViewLoaderTask task = new RepoViewLoaderTask();
        task.execute(results);

        cleanUp();

    }

    public void cleanUp(){
        final FragmentManager man = getSupportFragmentManager();
        final Fragment fragment = man.findFragmentByTag(Constants.DOWNLOAD_REPOS);

        if(fragment!=null)
            man.beginTransaction().remove(fragment).commit();
    }

    @Override
    public void onCancelled() {

    }


    private class RepoViewLoaderTask extends AsyncTask<String, Integer, GitRepoAdapter> {

        @Override
        protected GitRepoAdapter doInBackground(String... jsonString) {

            // Store list of repositories
            List<HashMap<String, Object>> repos = null;

            try {

                repos = getListOfReposFromJson(jsonString[0]);


            } catch (JSONException e) {
                e.printStackTrace();
            }

            //Keys
            String[] keysProjection = {Constants.GIT_REPO_NAME,
                    Constants.GIT_DESCRIPTION,
                    Constants.GIT_STARS,
                    Constants.GIT_WATCHERS};

            // Place list into adapter
            GitRepoAdapter adapter = new GitRepoAdapter(RepoView.this, 0, repos, keysProjection, null);

            return adapter;
        }

        private List<HashMap<String, Object>> getListOfReposFromJson(final String jsonArrayOfRepos) throws JSONException {


            List<HashMap<String, Object>> listOfRepos = new ArrayList<HashMap<String, Object>>();

            JSONArray queryResults = new JSONArray(jsonArrayOfRepos);

            //iterate through JSONArray (Each entry is a single repository)
            for (int i = 0; i < queryResults.length(); i++) {

                listOfRepos.add(getSingleRepository((JSONObject) queryResults.get(i)));

            }


            return listOfRepos;

        }

        private HashMap<String, Object> getSingleRepository(JSONObject repoInfo) {

            HashMap<String,Object> singleRepoHM = new HashMap<String,Object>();
            try {
                singleRepoHM.put(Constants.GIT_REPO_NAME, repoInfo.getString(Constants.GIT_REPO_NAME));
                singleRepoHM.put(Constants.GIT_DESCRIPTION, repoInfo.getString(Constants.GIT_DESCRIPTION));
                singleRepoHM.put(Constants.GIT_STARS, repoInfo.getInt(Constants.GIT_STARS));
                singleRepoHM.put(Constants.GIT_WATCHERS, repoInfo.getInt(Constants.GIT_WATCHERS));
                return singleRepoHM;
            } catch (JSONException e) {
                e.printStackTrace();
            }

            // Something went wrong if we reach this
            return null;

        }

        @Override
        protected void onPreExecute() {

        }

        @Override
        protected void onPostExecute(GitRepoAdapter adapter) {

            int count = adapter.getCount();
            for(int i=0; i<count; i++)
                mTableLayout.addView(createTableRow((ViewHolderItem) adapter.getItem(0)));


        }

        private TableRow createTableRow(ViewHolderItem singleRepository) {

            TableRow row = new TableRow(RepoView.this);


            TextView tv_name = new TextView(RepoView.this);
            tv_name.setPadding((int)convertFromDipToPx(8),0,0,0);

            TextView tv_description = new TextView(RepoView.this);
            TextView tv_stars= new TextView(RepoView.this);
            TextView tv_watchers= new TextView(RepoView.this);

            tv_name.setText(singleRepository.repositoryName);
            tv_description.setText(singleRepository.description);
            tv_stars.setText(Integer.toString(singleRepository.stars));
            tv_watchers.setText(Integer.toString(singleRepository.watchers));

            tv_name.setWidth(0);
            tv_name.setTextSize(10);
            tv_name.setEllipsize(TextUtils.TruncateAt.END);
            tv_name.setSingleLine();

            tv_description.setWidth(0);
            tv_description.setTextSize(10);
            tv_description.setEllipsize(TextUtils.TruncateAt.END);
            tv_description.setSingleLine();

            tv_stars.setWidth(0);
            tv_stars.setTextSize(10);
            tv_stars.setSingleLine();
            tv_stars.setPadding((int)convertFromDipToPx(16),0,0,0);

            tv_watchers.setWidth(0);
            tv_watchers.setSingleLine();
            tv_watchers.setTextSize(10);

            row.addView(tv_name);
            TableRow.LayoutParams paramsName = (TableRow.LayoutParams) tv_name.getLayoutParams();
            paramsName.span = 1;
            tv_name.setLayoutParams(paramsName);

            row.addView(tv_description);
            TableRow.LayoutParams paramsDesc = (TableRow.LayoutParams) tv_description.getLayoutParams();
            paramsDesc.span = 5;
            tv_description.setLayoutParams(paramsDesc);

            row.addView(tv_stars);
            TableRow.LayoutParams paramsStars = (TableRow.LayoutParams) tv_stars.getLayoutParams();
            paramsDesc.span = 1;
            tv_stars.setLayoutParams(paramsDesc);

            row.addView(tv_watchers);
            TableRow.LayoutParams paramsWatchers = (TableRow.LayoutParams) tv_watchers.getLayoutParams();
            paramsDesc.span = 1;
            tv_watchers.setLayoutParams(paramsDesc);

            return row;
        }

    }

    private float convertFromDipToPx(int dip){
        Resources r = getResources();
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dip, r.getDisplayMetrics());
    }

    /**
     * (0) repoName
     * (1) description
     * (2) stars
     * (3) watchers
     */

    public class ViewHolderItem{

        String repositoryName;
        String description;
        int stars;
        int watchers;
        ViewHolderItem(String name, String description, int stars, int watchers) {
            repositoryName = name;
            this.description = description;
            this.stars = stars;
            this.watchers = watchers;
        }
    }

    /**
     * Currently this adapter is written specifically for a tablelayout and hence, getView
     * is not implemented. However, if a listview is needed in the future along with the
     * tablelayout, all that's needed is the getView implementation and both views may
     * share this adapter.
     */
    private class GitRepoAdapter extends BaseAdapter{

        // This adapter holds all the repositories for this user
        List<ViewHolderItem> listOfRepositories = new ArrayList<ViewHolderItem>();

        // Data that is passed to this adapter, each entry is a single repository
        List<HashMap<String, Object>> list = null;
        String[] from = null;
        int[] to = null;

        //Constructor :)
        GitRepoAdapter(Context context, int layout,
                       List<HashMap<String, Object>> list,
                       String[] from,
                       int[] to) {
            this.list = list; // list of of repositories
            this.from = from; // data projection in a single repository (i.e.Name, Description, etc)

            for (HashMap<String, Object> repo : list) {
                System.out.println((String)repo.get(Constants.GIT_REPO_NAME));
                System.out.println((String)repo.get(Constants.GIT_DESCRIPTION));
                System.out.println("");
            }
            // Create List to store list of repos as viewHolder objects
            for(HashMap<String,Object> single_repo:list){

                for(int i=0; i<from.length; i++) {
                    listOfRepositories.add(new ViewHolderItem
                            ((String) single_repo.get(Constants.GIT_REPO_NAME),
                                    (String) single_repo.get(Constants.GIT_DESCRIPTION),
                                    (int) single_repo.get(Constants.GIT_STARS),
                                    (int) single_repo.get(Constants.GIT_WATCHERS)));
                }
            }

        }

        @Override
        public int getCount() {
            return listOfRepositories.size();
        }

        @Override
        public Object getItem(int position) {

            return listOfRepositories.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            // Does nothing since this adapter is designed to add to a tablelayout which
            // does not have recycling logic, and it's not worth implementing my
            // own view recycler.

            return convertView;
        }
    }
}
