package com.murraycole.photogallery;

import android.net.Uri;
import android.util.Log;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import javax.net.ssl.HttpsURLConnection;

/**
 * Created by User on 5/23/15.
 *
 * Returns array of recent pictures from flickr
 */
public class FlickrFetchr {
    private final String TAG = this.getClass().getCanonicalName();

    private final String END_POINT = "https://api.flickr.com/services/rest/";
    private final String METHOD_GET_RECENT = "flickr.photos.getRecent";
    private final String PARAM_EXTRA = "extras";
    private final String EXTRA_SMALL_URL = "url_s";
    private final String XML_PHOTO = "photo";

    public ArrayList<GalleryItem> fetchItems(){
        ArrayList<GalleryItem> items = new ArrayList<>();

        String url = build_URL();
        String xmlResponse = null;
        try {
            xmlResponse = getUrl(url);
            Log.i(TAG,xmlResponse);
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(new StringReader(xmlResponse));

            parseXMLString(items, parser);
        } catch (IOException e) {
            Log.i(TAG, "Failed to fetch items: " + e);
        } catch (XmlPullParserException e) {
            Log.i(TAG, "Failed to parse items", e);
        }
        return items;
    }


    private String build_URL(){
        String url = Uri.parse(END_POINT).buildUpon()
                .appendQueryParameter("method", METHOD_GET_RECENT)
                .appendQueryParameter("api_key", Config.API_KEY)
                .appendQueryParameter(PARAM_EXTRA, EXTRA_SMALL_URL)
                .build().toString();
        return url;
    }

    public String getUrl(String urlSpec) throws IOException {
        return new String(getUrlBytes(urlSpec));
    }

    public byte[] getUrlBytes(String urlspec) throws IOException {
        URL url = new URL(urlspec);

        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            InputStream in = connection.getInputStream();

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK){
                return null;
            }

            int bytesRead = 0;
            byte[] buffer = new byte[1024];

            while ((bytesRead = in.read(buffer)) > 0){
                out.write(buffer,0,bytesRead);
            }
            out.close();
            return out.toByteArray();
        } finally {
            connection.disconnect();
        }

    }

    public void parseXMLString(ArrayList<GalleryItem> items, XmlPullParser parser) throws IOException, XmlPullParserException {
        int eventType = parser.next();

        while (eventType != XmlPullParser.END_DOCUMENT){
            if (eventType == XmlPullParser.START_TAG &&
                    XML_PHOTO.equals(parser.getName())){
                String id = parser.getAttributeValue(null, "id");
                String caption = parser.getAttributeValue(null, "title");
                String url = parser.getAttributeValue(null, EXTRA_SMALL_URL);

                GalleryItem item = new GalleryItem();
                item.setmId(id);
                item.setmCaption(caption);
                item.setmUrl(url);

                items.add(item);

            }
            eventType = parser.next();
        }

    }

}
