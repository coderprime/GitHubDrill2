package ray.cyberpup.com.githubdrill2;

/**
 * Created on 6/18/15
 *
 * @author Raymond Tong
 */
public class Constants {


    // Fragment Tags (ASYNCTASKS)
    static final String DOWNLOAD_USERS = "download_users";
    static final String DOWNLOAD_REPOS = "download_repos";

    // User Info JSON object names and name/value
    static final String GIT_ITEMS = "items"; // array of user objects
    static final String GIT_ID = "id";       // github id name/value pair
    static final String GIT_USERID = "login";// username name/value pair
    static final String GIT_AVATAR = "avatar_url"; // avatar url name/value pair
    static final String GIT_REPOS = "repos_url";   // repo url name/value pair
    static final String USER_POSITION = "position";// position in user listview

    // Repository Info JSON object names and name/value pairs
    static final String GIT_REPO_NAME = "name"; // repo name name/value pair
    static final String GIT_DESCRIPTION = "description";
    static final String GIT_STARS = "stargazers_count";// username name/value pair
    static final String GIT_WATCHERS = "watchers_count"; // avatar url name/value pair

}
