package com.freedomainradio.android;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.freedomainradio.android.models.Link;
import com.freedomainradio.android.models.Podcast;

import org.ocpsoft.prettytime.PrettyTime;

import java.sql.SQLException;
import java.util.Iterator;

public class PodcastAdapter extends RecyclerView.Adapter<PodcastAdapter.ViewHolder>{
    private static final String TAG = "PodcastAdapter";

    private Context context;
    FdrDatabaseHelper.PodcastDataset podcasts;

    public PodcastAdapter(Context ctx) {
        FdrDatabaseHelper fdrDb = FdrDatabaseHelper.getInstance(ctx);

        this.context = ctx;
        try {
            podcasts = fdrDb.getPodcastDao();
        } catch (SQLException e) {
            Log.e(TAG, "Could not retrieve the Podcast DAO object");
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.podcast_rv_item, parent, false);

        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        int podcastId = -(position + 1);
        Podcast podcast = podcasts.getById(podcastId);

        if (podcast != null) {
            holder.updateViews(context, podcast);
        } else {
            Log.w(TAG, "There is no podcast with id = " + podcastId);
        }
    }

    @Override
    public int getItemCount() {
        if (podcasts != null) {
            return (int)podcasts.getTotalPodcastsCount();
        }

        return 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private TextView mPodcastTitle_tv;
        private TextView mPodcastDate_tv;
        private ImageView mPodcastYoutube_iv;
        private ImageView mPodcastVimeo_iv;

        public ViewHolder(View pView) {
            super(pView);

            this.mPodcastTitle_tv = (TextView) pView.findViewById(R.id.rvi_podcast_title);
            this.mPodcastDate_tv = (TextView) pView.findViewById(R.id.rvi_podcast_date);
            this.mPodcastYoutube_iv = (ImageView) pView.findViewById(R.id.rvi_podcast_youtube);
            this.mPodcastVimeo_iv = (ImageView) pView.findViewById(R.id.rvi_podcast_vimeo);
        }

        public void updateViews(final Context context, Podcast p) {
            mPodcastTitle_tv.setText(p.getTitle());
            mPodcastDate_tv.setText(new PrettyTime().format(p.getDate()));

            // Process Youtube and Vimeo link availability
            Iterator<Link> itr = p.getLinks().iterator();
            final String videoLink[] = {null, null};
            while (itr.hasNext()) {
                Link link = itr.next();
                if (link.getType() == Link.TYPE_YOUTUBE) {
                    videoLink[0] = link.toString();
                } else if (link.getType() == Link.TYPE_VIMEO) {
                    videoLink[1] = link.toString();
                }
            }
            if (videoLink[0] != null) {
                mPodcastYoutube_iv.setImageResource(R.mipmap.ic_youtube_enabled);
                mPodcastYoutube_iv.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent playerIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(videoLink[0]));
                        playerIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                        context.startActivity(playerIntent);
                    }
                });
            } else {
                mPodcastYoutube_iv.setImageResource(R.mipmap.ic_youtube_disabled);
                mPodcastYoutube_iv.setOnClickListener(null);
            }

            if (videoLink[1] != null) {
                mPodcastVimeo_iv.setImageResource(R.mipmap.ic_vimeo_enabled);
                mPodcastVimeo_iv.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent playerIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(videoLink[1]));
                        playerIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                        context.startActivity(playerIntent);
                    }
                });
            } else {
                mPodcastVimeo_iv.setImageResource(R.mipmap.ic_vimeo_disabled);
                mPodcastVimeo_iv.setOnClickListener(null);
            }
        }
    }
}
