package com.freedomainradio.android;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.freedomainradio.android.models.Author;
import com.freedomainradio.android.models.Link;
import com.freedomainradio.android.models.Podcast;
import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import java.io.IOException;
import java.sql.SQLException;

public class FdrDatabaseHelper extends OrmLiteSqliteOpenHelper {
    private static final String TAG = "FDRDatabase";

    private static String DB_NAME = "fdr.db";

    private static final String TABLE_PODCASTS_NAME = "Podcasts";
    private static final String TABLE_AUTHORS_NAME = "Authors";
    private static final String TABLE_PODTAG_LINKS_NAME = "pt_links";
    private static final String TABLE_URL_TYPES_NAME = "URLTypes";
    private static final String TABLE_TAGS_NAME = "Tags";
    private static final String TABLE_URLS_NAME = "URLs";

    private Context context;
    private static FdrDatabaseHelper instance;

    private FdrDatabaseHelper(Context ctx) {
        super(ctx, DB_NAME, null, Integer.parseInt(ctx.getString(R.string.db_version)),
                /**
                 * R.raw.ormlite_config is a reference to the ormlite_config.txt file in the
                 * /res/raw/ directory of this project
                 * */
                R.raw.ormlite_config);

        this.context = ctx;
    }

    public static synchronized FdrDatabaseHelper getInstance(Context ctx) {
        if (instance == null) {
            instance = new FdrDatabaseHelper(ctx);
        }

        return instance;
    }

    public PodcastDataset getPodcastDao() throws SQLException {
        Dao<Podcast, Integer> podDao = getDao(Podcast.class);

        return new PodcastDataset(podDao);
    }

    @Override
    public void onCreate(SQLiteDatabase database, ConnectionSource connectionSource) {
        try {
            for( String sqlInstruction : SqlParser.parseSqlFile(context.getResources().openRawResource(R.raw.fdr), this.context.getAssets())) {
                database.execSQL(sqlInstruction);
            }
        } catch (IOException e) {
            Log.w(TAG, "Could not run SQL file: " + e.getMessage());
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, ConnectionSource connectionSource, int oldVersion, int newVersion) {
        try {
            TableUtils.dropTable(connectionSource, Podcast.class, false);
            TableUtils.dropTable(connectionSource, Link.class, false);
            TableUtils.dropTable(connectionSource, Author.class, false);

            onCreate(database, connectionSource);
        } catch (SQLException e) {
            Log.w(TAG, "Could not clean the database tables");
        }
    }

    class PodcastDataset {
        private Dao<Podcast, Integer> podcastDao;
        private long totalPodcastCount;

        public PodcastDataset(Dao<Podcast, Integer> pd) {
            this.podcastDao = pd;

            try {
                this.totalPodcastCount = pd.countOf();
            } catch(SQLException ex) {
                Log.w(TAG, "Could not retrieve the total podcast count");
                this.totalPodcastCount = 0;
            }
        }

        public long getTotalPodcastsCount() {
            return this.totalPodcastCount;
        }

        public Podcast getById(int id) {
            Podcast retrievedPodcast = null;

            try {
                if (id < 0) {
                    id = (int)this.totalPodcastCount + id + 1;
                }
                retrievedPodcast = podcastDao.queryForId(id);
            } catch (SQLException e) {
                Log.w(TAG, "There was a SQL error while searching for the podcast");
            }

            return retrievedPodcast;
        }
    }
}
