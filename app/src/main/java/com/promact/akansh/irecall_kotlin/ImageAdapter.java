package com.promact.akansh.irecall_kotlin;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Environment;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.crashlytics.android.Crashlytics;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;

import io.fabric.sdk.android.Fabric;

import static android.content.ContentValues.TAG;

class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ImageHolder> {
    private ArrayList<AlbumDetails> arrayAlbumDets;
    private Context context;
    private String latLng;
    private static final int STATIC_CARD = 0;
    private static final int DYNAMIC_CARD = 1;

    ImageAdapter(Context context, ArrayList<AlbumDetails> arrayAlbumDets,
                 String latLng) {
        this.arrayAlbumDets = arrayAlbumDets;
        this.context = context;
        this.latLng = latLng;
        Fabric.with(this.context, new Crashlytics());
    }

    @Override
    public ImageAdapter.ImageHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.album_details_cardview, parent, false);

        return new ImageHolder(view);
    }

    @Override
    public int getItemViewType(int position) {
        if(position == 0) {
            return STATIC_CARD;
        } else {
            return DYNAMIC_CARD;
        }
    }

    @Override
    public void onBindViewHolder(final ImageAdapter.ImageHolder holder, final int position) {
        holder.shareButton.setText(R.string.share_string);
        if (arrayAlbumDets.get(position).MediaId.contains("I_")) {
            holder.playButton.setVisibility(View.GONE);
            Glide.with(context)
                    .load("https://firebasestorage.googleapis.com/v0/b/irecall-kotlin.appspot.com/o/IRecall%2F" + arrayAlbumDets.get(position).Filename + "?alt=media&token=1")
                    .listener(new RequestListener<String, GlideDrawable>() {
                        @Override
                        public boolean onException(Exception e, String model, Target<GlideDrawable> target, boolean isFirstResource) {
                            e.printStackTrace();

                            return false;
                        }

                        @Override
                        public boolean onResourceReady(GlideDrawable resource, String model, Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
                            if (!isFromMemoryCache) {
                                resource.start();
                            }

                            return false;
                        }
                    })
                    .into(holder.image);
        } else {
            Glide.with(context)
                    .load("https://firebasestorage.googleapis.com/v0/b/irecall-kotlin.appspot.com/o/Thumbnails%2F"+ arrayAlbumDets.get(position).thumbnail +"?alt=media&token=1")
                    .listener(new RequestListener<String, GlideDrawable>() {
                        @Override
                        public boolean onException(Exception e, String model, Target<GlideDrawable> target, boolean isFirstResource) {
                            e.printStackTrace();

                            return false;
                        }

                        @Override
                        public boolean onResourceReady(GlideDrawable resource, String model, Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
                            if (!isFromMemoryCache) {
                                resource.start();
                            }

                            return false;
                        }
                    })
                    .centerCrop()
                    .fitCenter()
                    .into(holder.image);
            holder.playButton.setVisibility(View.VISIBLE);

            holder.playButton.bringToFront();
        }
        holder.shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareMedia("https://firebasestorage.googleapis.com/v0/b/irecall-kotlin.appspot.com/o/IRecall%2F" + arrayAlbumDets.get(position).Filename + "?alt=media&token=1", position, v);
            }
        });
        holder.caption.setText(arrayAlbumDets.get(position).caption);
        holder.image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intent = new Intent(view.getContext(), photoView.class);
                intent.putExtra("media", arrayAlbumDets.get(position).Filename);

                view.getContext().startActivity(intent);
            }
        });
        if (getItemViewType(position)==STATIC_CARD) {
            holder.location.setText(latLng);
        } else {
            holder.location.setVisibility(View.GONE);
        }
    }

    private void shareMedia(String mediaURL, int position, final View v){
        if (arrayAlbumDets.get(position).MediaId.contains("I_")) {
            Log.d(TAG, "Play invisible");

            Picasso.with(v.getContext().getApplicationContext()).load(mediaURL).into(new com.squareup.picasso.Target() {
                @Override
                public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("image/*");
                    shareIntent.putExtra(Intent.EXTRA_STREAM, getLocalBitmapUri(bitmap));

                    v.getContext().startActivity(Intent.createChooser(shareIntent, "Share image"));
                }

                @Override
                public void onBitmapFailed(Drawable errorDrawable) {

                }

                @Override
                public void onPrepareLoad(Drawable placeHolderDrawable) {

                }
            });
        } else {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);

            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Hey. Check the video out - \n https://firebasestorage.googleapis.com/v0/b/irecall-kotlin.appspot.com/o/IRecall%2F" + arrayAlbumDets.get(position).Filename + "?alt=media&token=1");

            v.getContext().startActivity(Intent.createChooser(shareIntent, "Share Video"));
        }
    }

    private Uri getLocalBitmapUri(Bitmap bitmap) {
        Uri uri = null;

        try {
            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "shareImg"+System.currentTimeMillis() + ".png");
            FileOutputStream outputStream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, outputStream);
            outputStream.close();
            uri = Uri.fromFile(file);
        } catch (Exception ex) {
            ex.getMessage();
            Crashlytics.logException(ex);
        }

        return uri;
    }

    @Override
    public int getItemCount() {
        return arrayAlbumDets.size();
    }

    class ImageHolder extends RecyclerView.ViewHolder {
        private ImageView image;
        private ImageView playButton;
        private Button shareButton;
        private TextView caption;
        private TextView location;

        ImageHolder(View view) {
            super(view);

            image = (ImageView) view.findViewById(R.id.imgCaptured);
            playButton = (ImageView) view.findViewById(R.id.imgPlay);
            shareButton = (Button) view.findViewById(R.id.shareButton);
            caption = (TextView) view.findViewById(R.id.textViewCaption);
            location = (TextView) view.findViewById(R.id.txtTitleLocation);
        }
    }
}