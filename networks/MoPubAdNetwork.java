package com.mopub.nativeads;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.View;

import com.adsnative.ads.BaseNativeAd;
import com.adsnative.ads.ErrorCode;
import com.adsnative.mediation.CustomAdNetwork;
import com.adsnative.network.AdResponse;
import com.adsnative.util.ANLog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by sreekanth on 17/09/15.
 */
public class MoPubAdNetwork extends CustomAdNetwork {
    private static final String PLACEMENT_ID_KEY = "placementId";

    // CustomEventNative implementation
    @Override
    protected void loadNativeAd(final Context context,
                                final CustomEventListener customEventListener,
                                final AdResponse adResponse) {

        String placementId = null;
        JSONObject customEventClassData = adResponse.getCustomAdNetworkData();
        try {
            placementId = customEventClassData.getString(PLACEMENT_ID_KEY);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (placementId == null || placementId.length() <= 0) {
            ANLog.e("MoPubAdNetwork: " + ErrorCode.NATIVE_ADAPTER_CONFIGURATION_ERROR);
            customEventListener.onNativeAdFailed(ErrorCode.NATIVE_ADAPTER_CONFIGURATION_ERROR);
            return;
        }

        final MoPubNativeAd mopubNativeAd =
                new MoPubNativeAd(context, placementId, customEventListener);
        mopubNativeAd.loadAd();
    }

    static class MoPubNativeAd extends BaseNativeAd {
        private static final String SOCIAL_CONTEXT_FOR_AD = "socialContextForAd";

        private final Context mContext;
        private final String mAdUnitId;
        private final CustomEventListener mCustomEventListener;

        private String mLandingURL;
        com.mopub.nativeads.NativeClickHandler mMopubClickHandler;

        MoPubNativeAd(final Context context,
                      final String adUnitId,
                      final CustomEventListener customEventListener) {
            mContext = context.getApplicationContext();
            mAdUnitId = adUnitId;
            mCustomEventListener = customEventListener;
        }

        void loadAd() {
            // mMoPubNative.makeRequest();
            // implement MoPub adrequest code here to get native assets
            final NativeUrlGenerator generator = new NativeUrlGenerator(mContext)
                    .withAdUnitId(mAdUnitId)
                    .withRequest(null);
            final String endpointUrl = generator.generateUrlString("ads.mopub.com");
            com.mopub.network.AdRequest.Listener mVolleyListener = new com.mopub.network.AdRequest.Listener() {
                @Override
                public void onSuccess(com.mopub.network.AdResponse adResponse) {
                    onNativeLoad(adResponse);
                }

                @Override
                public void onErrorResponse(@NonNull final com.mopub.volley.VolleyError volleyError) {
                    onNativeFail(volleyError);
                }
            };
            com.mopub.network.AdRequest mNativeRequest =
                    new com.mopub.network.AdRequest(endpointUrl,
                            com.mopub.common.AdFormat.NATIVE, mAdUnitId,
                            mContext, mVolleyListener);
            com.mopub.volley.RequestQueue requestQueue = com.mopub.network.Networking.getRequestQueue(mContext);
            requestQueue.add(mNativeRequest);
        }

        public void onNativeLoad(final com.mopub.network.AdResponse adResponse) {
            JSONObject adJSON = adResponse.getJsonBody();
            ANLog.e(adResponse.getStringBody());

            if (adJSON == null) {
                mCustomEventListener.onNativeAdFailed(ErrorCode.EMPTY_AD_RESPONSE);
                return;
            }

            String title = (String) adJSON.opt("title");
            if (title != null) {
                setTitle(title);
            }
            String summary = (String) adJSON.opt("text");
            if (summary != null) {
                setSummary(summary);
            }
            String iconImage = (String) adJSON.opt("iconimage");
            if (iconImage != null) {
                setIconImage(iconImage);
            }
            String mainImage = (String) adJSON.opt("mainimage");
            if (mainImage != null) {
                setMainImage(mainImage);
            }
            String cta = (String) adJSON.opt("ctatext");
            if (cta != null) {
                setCallToAction(cta);
            }
            Double starRating = (Double) adJSON.opt("starrating");
            if (starRating != null) {
                setStarRating(starRating);
            }
            setPromotedByTag("Sponsored");

            final List<String> imageUrls = new ArrayList<String>();
            final String mainImageUrl = getMainImage();
            if (mainImageUrl != null) {
                imageUrls.add(mainImageUrl);
            }
            final String iconUrl = getIconImage();
            if (iconUrl != null) {
                imageUrls.add(iconUrl);
            }

            try {
                preCacheImages(mContext, imageUrls, new ImageListener() {
                    @Override
                    public void onImagesCached() {
                        mCustomEventListener.onNativeAdLoaded(MoPubNativeAd.this);
                    }

                    @Override
                    public void onImagesFailedToCache(ErrorCode errorCode) {
                        ANLog.e("MoPubAdNetwork: " + errorCode);
                        mCustomEventListener.onNativeAdFailed(errorCode);
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }

            Object impTrackers = adJSON.opt("imptracker");
            if (impTrackers instanceof JSONArray) {
                final JSONArray trackers = (JSONArray) impTrackers;
                for (int i = 0; i < trackers.length(); i++) {
                    try {
                        addImpressionTracker(trackers.getString(i));
                    } catch (JSONException e) {
                        // This will only occur if we access a non-existent index in JSONArray.
                        ANLog.d("Unable to parse impression trackers from MoPubAdNetwork");
                    }
                }
            }

            Object clkTrackers = adJSON.opt("clktracker");
            if (clkTrackers instanceof JSONArray) {
                final JSONArray trackers = (JSONArray) clkTrackers;
                for (int i = 0; i < trackers.length(); i++) {
                    try {
                        addClickTracker(trackers.getString(i));
                    } catch (JSONException e) {
                        // This will only occur if we access a non-existent index in JSONArray.
                        ANLog.d("Unable to parse click trackers from MoPubAdNetwork");
                    }
                }
            } else if (clkTrackers instanceof String) {
                addClickTracker((String) clkTrackers);
            }

            mLandingURL = (String) adJSON.opt("clk");
        }

        public void onNativeFail(com.mopub.volley.VolleyError volleyError) {
            ANLog.e("MoPubAdNetwork: " + volleyError.getMessage());
            mCustomEventListener.onNativeAdFailed(ErrorCode.NETWORK_NO_FILL);
        }

        // BaseNativeAd
        @Override
        public void prepare(final View view) {
            mMopubClickHandler = new com.mopub.nativeads.NativeClickHandler(mContext);
        }

        @Override
        public void handleClick(final View view) {
            mMopubClickHandler.openClickDestinationUrl(mLandingURL, view);
        }
    }
}