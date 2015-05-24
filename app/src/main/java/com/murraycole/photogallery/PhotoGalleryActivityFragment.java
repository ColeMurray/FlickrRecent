package com.murraycole.photogallery;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import java.util.ArrayList;


/**
 * Fragment that displays a gridview of flickr's latest photos
 */
public class PhotoGalleryActivityFragment extends Fragment {
    private static final String TAG = PhotoGalleryActivityFragment.class.getCanonicalName();

    private GridView mGridview;
    private ArrayList<GalleryItem> mItems;
    ThumbnailDownloader<ImageView> mThumbnailThread;

    public PhotoGalleryActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_photo_gallery, container, false);

        mGridview = (GridView) view.findViewById(R.id.img_gv);

        setupAdapter();

        new FetchItemsTask().execute();

        mThumbnailThread = new ThumbnailDownloader<>(new Handler());
        mThumbnailThread.setListener(new ThumbnailDownloader.Listener<ImageView>() {
            @Override
            public void onThumbnailDownloaded(ImageView imageView, Bitmap thumbnail) {
                if (isVisible()){
                    imageView.setImageBitmap(thumbnail);
                }
            }
        });
        mThumbnailThread.start();
        mThumbnailThread.getLooper();
        Log.i(TAG,"Background thread started");

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mThumbnailThread.clearQueue();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mThumbnailThread.quit();
        Log.i(TAG,"Background thread destroyed");
    }

    void setupAdapter(){
        if (getActivity() == null || mGridview == null) return;

        if (mItems != null){
            mGridview.setAdapter(new GalleryItemAdapter(mItems));
        } else {
            mGridview.setAdapter(null);
        }
    }
    private class FetchItemsTask extends AsyncTask<Void, Void, ArrayList<GalleryItem>> {
        @Override
        protected ArrayList doInBackground(Void... voids) {
            return new FlickrFetchr().fetchItems();

        }

        @Override
        protected void onPostExecute(ArrayList<GalleryItem> galleryItems) {
            mItems = galleryItems;
            setupAdapter();
        }
    }

    private class GalleryItemAdapter extends ArrayAdapter<GalleryItem> {

        public GalleryItemAdapter(ArrayList<GalleryItem> items){
            super(getActivity(),0,items);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null){
                convertView = getActivity().getLayoutInflater().inflate(R.layout.gallery_item,
                        parent,false);
            }
            ImageView imageView = (ImageView) convertView.findViewById(R.id.gallery_item_iv);
            imageView.setImageResource(R.mipmap.ic_launcher);

            GalleryItem item = getItem(position);
            mThumbnailThread.queueThumbnail(imageView,item.getmUrl());

            return convertView;
        }
    }
}
